package com.programacao_web.rpg_market.service;

import com.programacao_web.rpg_market.model.*;
import com.programacao_web.rpg_market.repository.BidRepository;
import com.programacao_web.rpg_market.repository.ProductRepository;
import com.programacao_web.rpg_market.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private TransactionRepository transactionRepository;    @Autowired
    private BidService bidService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    /**
     * Cria um novo produto
     */
    public Product create(Product product, User seller) {
        // Definir o vendedor
        product.setSeller(seller);
        
        // Garantir que leilões sempre tenham um preço inicial > 0
        if (product.getType() == ProductType.AUCTION && 
            (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Leilões devem ter um lance inicial válido maior que zero");
        }
        
        // Se for leilão, verifica se tem data de término
        if (product.getType() == ProductType.AUCTION && product.getAuctionEndDate() == null) {
            // Define um padrão de 7 dias se não for especificado
            product.setAuctionEndDate(LocalDateTime.now().plusDays(7));
            product.setStatus(ProductStatus.AUCTION_ACTIVE);
        }
        
        return productRepository.save(product);    }
    
    /**
     * Busca um produto pelo ID
     */
    public Optional<Product> findById(String id) {  // Changed from Long to String
        return productRepository.findById(id);
    }
    
    /**
     * Busca produtos disponíveis para venda direta
     */
    public Page<Product> findAvailable(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.AVAILABLE, pageable);
    }
    
    /**
     * Busca leilões ativos
     */
    public Page<Product> findActiveAuctions(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.AUCTION_ACTIVE, pageable);
    }
    
    /**
     * Busca produtos por categoria
     */
    public Page<Product> findByCategory(ProductCategory category, Pageable pageable) {
        return productRepository.findByCategoryAndStatus(category, ProductStatus.AVAILABLE, pageable);
    }
    
    /**
     * Busca produtos por palavra-chave
     */
    public Page<Product> search(String keyword, ProductStatus status, Pageable pageable) {
        return productRepository.searchByNameAndStatus(keyword, status, pageable);
    }
    
    /**
     * Busca os lances de um produto
     */
    public List<Bid> getProductBids(Product product) {
        return bidRepository.findByProductOrderByAmountDesc(product);
    }    /**
     * Realiza um lance em um leilão (delegado para BidService)
     */    @Transactional
    public void makeBid(Product product, User bidder, BigDecimal amount) {
        log.info("Delegando lance para BidService: produtoId={}, licitante={}, valor={}", 
                 product.getId(), bidder.getUsername(), amount);
        
        // Delegar para o BidService que tem a lógica correta
        bidService.placeBid(product, bidder, amount);
        
        // Verificar se é uma compra imediata (buy now)
        if (product.getBuyNowPrice() != null && 
            amount.compareTo(product.getBuyNowPrice()) >= 0) {
            log.info("Preço de compra imediata atingido, finalizando leilão");
            endAuction(product);
        }
        
        log.info("makeBid concluído com sucesso");
    }
    
    /**
     * Realiza uma compra direta
     */
    @Transactional
    public void buyNow(Product product, User buyer) {
        // Verifica se o comprador não é o vendedor
        if (buyer.getId().equals(product.getSeller().getId())) {
            throw new IllegalArgumentException("Você não pode comprar seu próprio item");
        }
        
        if (product.getStatus() != ProductStatus.AVAILABLE && 
            product.getStatus() != ProductStatus.AUCTION_ACTIVE) {
            throw new IllegalArgumentException("Este produto não está disponível para compra");
        }
        
        BigDecimal price = product.getType() == ProductType.AUCTION ? 
            product.getBuyNowPrice() : product.getPrice();
            
        if (price == null) {
            throw new IllegalArgumentException("Preço não definido para este produto");
        }
        
        product.setStatus(ProductStatus.SOLD);
        productRepository.save(product);
        
        // Cria uma transação
        transactionService.createTransaction(product, buyer, product.getSeller(), price);
    }
      /**
     * Verifica periodicamente leilões que terminaram
     */
    @Scheduled(fixedRate = 60000) // Executa a cada minuto
    @Transactional
    public void checkEndedAuctions() {
        List<Product> endedAuctions = productRepository.findByStatusAndAuctionEndDateLessThanEqual(
            ProductStatus.AUCTION_ACTIVE, LocalDateTime.now());
            
        log.info("Verificando leilões terminados. Encontrados: {}", endedAuctions.size());
            
        for (Product auction : endedAuctions) {
            endAuction(auction);
        }
    }
    
    /**
     * Finaliza um leilão - delega para o BidService a lógica de débito
     */
    private void endAuction(Product product) {
        log.info("=== FINALIZANDO LEILÃO ===");
        log.info("Produto: {} ({})", product.getName(), product.getId());
        
        // Usar o BidService para processar o fim do leilão corretamente
        bidService.processAuctionEnd(product);
        
        log.info("✅ Leilão finalizado para o produto: {}", product.getId());
    }

    /**
     * Atualiza um produto existente
     */
    @Transactional
    public Product updateProduct(String id, Product updatedProduct) {  // Changed from Long to String
        Optional<Product> productOpt = findById(id);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Produto não encontrado");
        }
        
        Product product = productOpt.get();
        // Update properties
        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setCategory(updatedProduct.getCategory());
        
        // Se for um leilão, permite atualizar o preço de compra imediata e incremento mínimo
        if (product.getType() == ProductType.AUCTION && 
            product.getStatus() != ProductStatus.AUCTION_ENDED) {
            product.setBuyNowPrice(updatedProduct.getBuyNowPrice());
            product.setMinBidIncrement(updatedProduct.getMinBidIncrement());
        }
        
        // Se houver nova imagem, atualiza ela
        if (updatedProduct.getImageUrl() != null && !updatedProduct.getImageUrl().isEmpty()) {
            product.setImageUrl(updatedProduct.getImageUrl());
        }
        
        return productRepository.save(product);
    }

    /**
     * Update product with string ID for MongoDB
     */
    @Transactional
    public Product update(String id, Product updatedProduct, User user) {
        Optional<Product> productOpt = findById(id);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Produto não encontrado");
        }
        
        Product product = productOpt.get();
        
        // Verify ownership
        if (!product.getSeller().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Você não tem permissão para editar este produto");
        }
        
        // Update properties
        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setCategory(updatedProduct.getCategory());
        
        // If it's an auction, allow updating buy now price and minimum bid increment
        if (product.getType() == ProductType.AUCTION && 
            product.getStatus() != ProductStatus.AUCTION_ENDED) {
            product.setBuyNowPrice(updatedProduct.getBuyNowPrice());
            product.setMinBidIncrement(updatedProduct.getMinBidIncrement());
        }
        
        // If there's a new image, update it
        if (updatedProduct.getImageUrl() != null && !updatedProduct.getImageUrl().isEmpty()) {
            product.setImageUrl(updatedProduct.getImageUrl());
        }
        
        return productRepository.save(product);
    }

    /**
     * Retorna ranking de vendedores com mais vendas
     */
    public List<User> getTopSellers() {
        return transactionRepository.findTopSellers();
    }

    /**
     * Retorna ranking de compradores com mais compras
     */
    public List<User> getTopBuyers() {
        return transactionRepository.findTopBuyers();
    }

    /**
     * Find all products owned by a user
     */
    public List<Product> findByUser(User user) {
        return productRepository.findBySeller(user);
    }
    
    /**
     * Find active products (available or auction) owned by a user
     */
    public List<Product> findActiveByUser(User user) {
        return productRepository.findBySellerAndStatusIn(
            user, Arrays.asList(ProductStatus.AVAILABLE, ProductStatus.AUCTION_ACTIVE));
    }
    
    /**
     * Find sold products owned by a user
     */
    public List<Product> findSoldByUser(User user) {
        return productRepository.findBySellerAndStatusIn(
            user, Arrays.asList(ProductStatus.SOLD, ProductStatus.AUCTION_ENDED));
    }
    
    /**
     * Delete a product
     * Returns true if successful, false otherwise
     */
    @Transactional
    public boolean deleteProduct(Product product, User currentUser) {
        try {
            // Verify user is the seller
            if (!product.getSeller().getId().equals(currentUser.getId())) {
                return false;
            }
            
            // For active auctions, cancel all bids
            if (product.getType() == ProductType.AUCTION && 
                product.getStatus() == ProductStatus.AUCTION_ACTIVE) {
                
                cancelAllBidsForProduct(product);
                
                // Update product status before deletion to reflect it was canceled
                product.setStatus(ProductStatus.CANCELED);
                productRepository.save(product);
            }
            
            // Delete the product
            productRepository.delete(product);
            
            // Handle image deletion if needed
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                fileStorageService.deleteFile(product.getImageUrl());
            }
              return true;
        } catch (Exception e) {
            log.error("Erro ao excluir produto: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Cancel all bids for a product
     */
    private void cancelAllBidsForProduct(Product product) {
        List<Bid> bids = bidRepository.findByProductOrderByAmountDesc(product);
        if (!bids.isEmpty()) {
            // Notify bidders about cancellation if needed
            // For now, just delete all bids
            bidRepository.deleteAll(bids);
        }
    }

    /**
     * Busca produtos de venda direta com filtros
     */    public Page<Product> findDirectSalesWithFilters(
            ProductCategory category,
            ItemRarity rarity,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        
        // Criar critérios
        Criteria criteria = new Criteria();
        
        // Critérios básicos para vendas diretas
        criteria = Criteria.where("type").is(ProductType.DIRECT_SALE)
                .and("status").is(ProductStatus.AVAILABLE);
        
        // Adicionar filtros opcionais
        if (category != null) {
            criteria = criteria.and("category").is(category);
        }
        
        if (rarity != null) {
            criteria = criteria.and("rarity").is(rarity);
        }
        
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) > 0) {
            criteria = criteria.and("price").gte(minPrice);
        }
        
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
            criteria = criteria.and("price").lte(maxPrice);
        }
        
        // Criar consulta
        Query query = new Query(criteria);
        
        // Obter contagem total para paginação
        long total = mongoTemplate.count(query, Product.class);
        
        // Adicionar paginação
        query.with(pageable);
        
        // Executar consulta
        List<Product> products = mongoTemplate.find(query, Product.class);
        
        // Retornar Page
        return new PageImpl<>(products, pageable, total);
    }

    /**
     * Busca leilões ativos com filtros
     */    public Page<Product> findAuctionsWithFilters(
            ProductCategory category,
            ItemRarity rarity,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean endingSoon,
            Pageable pageable) {
        
        // Criar critérios
        Criteria criteria = new Criteria();
        
        // Critérios básicos para leilões ativos
        criteria = Criteria.where("type").is(ProductType.AUCTION)
                .and("status").is(ProductStatus.AUCTION_ACTIVE);
        
        // Adicionar filtros opcionais
        if (category != null) {
            criteria = criteria.and("category").is(category);
        }
        
        if (rarity != null) {
            criteria = criteria.and("rarity").is(rarity);
        }
        
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) > 0) {
            criteria = criteria.and("price").gte(minPrice);
        }
        
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
            criteria = criteria.and("price").lte(maxPrice);
        }
        
        // Filtro especial para leilões terminando em breve (próximas 24 horas)
        if (endingSoon != null && endingSoon) {
            LocalDateTime nextDay = LocalDateTime.now().plusHours(24);
            criteria = criteria.and("auctionEndDate").lte(nextDay);
        }
        
        // Criar consulta
        Query query = new Query(criteria);
        
        // Obter contagem total para paginação
        long total = mongoTemplate.count(query, Product.class);
        
        // Adicionar paginação
        query.with(pageable);
        
        // Executar consulta
        List<Product> auctions = mongoTemplate.find(query, Product.class);
        
        // Retornar Page
        return new PageImpl<>(auctions, pageable, total);
    }

    /**
     * Salva um produto
     */
    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }
}
