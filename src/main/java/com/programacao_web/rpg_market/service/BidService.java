package com.programacao_web.rpg_market.service;

import com.programacao_web.rpg_market.model.*;
import com.programacao_web.rpg_market.repository.BidRepository;
import com.programacao_web.rpg_market.repository.ProductRepository;
import com.programacao_web.rpg_market.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BidService {

    private static final Logger log = LoggerFactory.getLogger(BidService.class);

    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TransactionService transactionService;/**
     * Registra um lance no leilão (SEM DÉBITO - apenas verifica saldo)
     */
    @Transactional
    public Bid placeBid(Product product, User bidder, BigDecimal amount) {
        log.info("=== INICIANDO REGISTRO DE LANCE ===");
        log.info("Produto: {} ({})", product.getName(), product.getId());
        log.info("Usuário: {} (saldo: {} moedas)", bidder.getUsername(), bidder.getGoldCoins());
        log.info("Valor do lance: {} moedas", amount);
        
        // VALIDAÇÕES IMPORTANTES
        if (product.getType() != ProductType.AUCTION || product.getStatus() != ProductStatus.AUCTION_ACTIVE) {
            throw new IllegalArgumentException("Este item não está em leilão ativo");
        }
        
        if (bidder.getId().equals(product.getSeller().getId())) {
            throw new IllegalArgumentException("Você não pode dar lances no seu próprio item");
        }
        
        // Verificar se o lance é maior que o atual + incremento mínimo
        BigDecimal currentPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        BigDecimal minIncrement = product.getMinBidIncrement() != null ? product.getMinBidIncrement() : BigDecimal.ONE;
        BigDecimal minBid = currentPrice.add(minIncrement);
        
        if (amount.compareTo(minBid) < 0) {
            throw new IllegalArgumentException(String.format("O lance deve ser pelo menos %.2f moedas", minBid));
        }
        
        // VERIFICAÇÃO CRUCIAL: O usuário tem saldo suficiente?
        if (bidder.getGoldCoins().compareTo(amount) < 0) {
            BigDecimal needed = amount.subtract(bidder.getGoldCoins());
            throw new IllegalArgumentException(String.format("Saldo insuficiente! Você precisa de mais %.2f moedas de ouro", needed));
        }
        
        log.info("✅ Todas as validações passaram");
        
        // Marcar lances anteriores como não vencedores
        List<Bid> previousBids = bidRepository.findByProductOrderByAmountDesc(product);
        for (Bid oldBid : previousBids) {
            if (oldBid.isWinning()) {
                oldBid.setWinning(false);
                bidRepository.save(oldBid);
                log.info("Lance anterior desativado: usuário {}, valor {}", 
                         oldBid.getBidder().getUsername(), oldBid.getAmount());
            }
        }
        
        // Criar o novo lance
        Bid newBid = new Bid();
        newBid.setProduct(product);
        newBid.setBidder(bidder);        newBid.setAmount(amount);
        newBid.setBidTime(LocalDateTime.now());
        newBid.setWinning(true); // Este é o lance vencedor atual
        
        // Salvar o lance (SEM DÉBITO - só registra)
        Bid savedBid = bidRepository.save(newBid);
        log.info("✅ Novo lance registrado: ID={}", savedBid.getId());
        
        // Atualizar o preço atual do produto para refletir o lance mais alto
        product.setPrice(amount);
        productRepository.save(product);
        log.info("✅ Preço do produto atualizado para: {} moedas", amount);
        
        // IMPORTANTE: O ouro NÃO é debitado aqui!
        // Será debitado apenas quando o leilão terminar e este for o lance vencedor
        log.info("=== LANCE REGISTRADO COM SUCESSO (SEM DÉBITO) ===");
        
        return savedBid;
    }

    /**
     * Busca todos os lances de um produto
     */
    public List<Bid> getProductBids(Product product) {
        return bidRepository.findByProductOrderByAmountDesc(product);
    }

    /**
     * Busca o lance vencedor atual de um produto
     */
    public Bid getCurrentWinningBid(Product product) {
        List<Bid> bids = bidRepository.findByProductOrderByAmountDesc(product);
        return bids.stream()
                   .filter(Bid::isWinning)
                   .findFirst()
                   .orElse(null);
    }

    /**
     * Verifica se um usuário tem o lance vencedor em um produto
     */
    public boolean isUserWinning(Product product, User user) {
        Bid winningBid = getCurrentWinningBid(product);
        return winningBid != null && winningBid.getBidder().getId().equals(user.getId());
    }

    /**
     * Finaliza um leilão processando o lance vencedor
     * (Aqui que acontece o débito do ouro)
     */
    @Transactional
    public void processAuctionEnd(Product product) {
        log.info("Finalizando leilão do produto: {}", product.getId());
        
        Bid winningBid = getCurrentWinningBid(product);
        if (winningBid != null) {
            User winner = winningBid.getBidder();
            BigDecimal amount = winningBid.getAmount();
            
            // Verificar se o vencedor ainda tem saldo suficiente
            if (winner.getGoldCoins().compareTo(amount) >= 0) {
                // Debitar o valor do vencedor
                winner.setGoldCoins(winner.getGoldCoins().subtract(amount));
                
                // Creditar o valor para o vendedor
                User seller = product.getSeller();
                seller.setGoldCoins(seller.getGoldCoins().add(amount));
                
                // Atualizar status do produto
                product.setStatus(ProductStatus.SOLD);
                
                log.info("Leilão finalizado com sucesso. Vencedor: {}, Valor: {}", 
                         winner.getUsername(), amount);
            } else {
                // Vencedor não tem mais saldo - cancelar leilão
                log.warn("Vencedor {} não tem saldo suficiente. Cancelando leilão.", 
                         winner.getUsername());
                product.setStatus(ProductStatus.AUCTION_ENDED);
            }
        } else {
            // Sem lances - leilão termina sem venda
            product.setStatus(ProductStatus.AUCTION_ENDED);
            log.info("Leilão terminado sem lances para o produto: {}", product.getId());
        }
        
        productRepository.save(product);
    }
    
    /**
     * Método simplificado para dar lances (compatível com o controlador)
     */
    @Transactional
    public Bid placeBidSimple(String productId, User bidder, BigDecimal amount) {
        log.info("=== PROCESSANDO LANCE SIMPLES ===");
        log.info("ProductId: {}, Amount: {}, User: {}", productId, amount, bidder.getUsername());
        
        // Buscar produto
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
        
        // Validações básicas
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor de lance inválido");
        }
        
        // Verificar se é um leilão ativo
        if (!isValidAuction(product)) {
            throw new IllegalArgumentException("Este item não está em leilão ativo");
        }
        
        // Verificar se não é o próprio vendedor
        if (bidder.getId().equals(product.getSeller().getId())) {
            throw new IllegalArgumentException("Você não pode dar lances no seu próprio item");
        }
        
        // Verificar se o usuário já está liderando o leilão
        if (isUserWinning(product, bidder)) {
            throw new IllegalArgumentException("Você já está liderando este leilão!");
        }
        
        // Verificar se o lance é válido (maior que o atual)
        BigDecimal currentPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        BigDecimal minBid = currentPrice.add(BigDecimal.ONE); // Incremento mínimo de 1
        
        if (amount.compareTo(minBid) < 0) {
            throw new IllegalArgumentException(
                String.format("O lance deve ser pelo menos $%.2f", minBid));
        }
        
        // Marcar lances anteriores como não vencedores
        bidRepository.findByProductOrderByAmountDesc(product).forEach(oldBid -> {
            if (oldBid.isWinning()) {
                oldBid.setWinning(false);
                bidRepository.save(oldBid);
                log.info("Lance anterior marcado como não vencedor: {}", oldBid.getId());
            }
        });
        
        // Criar e salvar o novo lance
        Bid bid = new Bid();
        bid.setProduct(product);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bid.setBidTime(LocalDateTime.now());
        bid.setWinning(true); // Este será o lance vencedor
        
        Bid savedBid = bidRepository.save(bid);
        log.info("Lance salvo com sucesso: {}", savedBid.getId());
        
        // Atualizar o preço do produto
        product.setPrice(amount);
        productRepository.save(product);
        log.info("Preço do produto atualizado: {}", product.getPrice());
        
        log.info("=== LANCE PROCESSADO COM SUCESSO ===");
        return savedBid;
    }
    
    /**
     * Verificar se o produto é um leilão válido
     */
    private boolean isValidAuction(Product product) {
        return product.getType() != null && 
               product.getType().name().equals("AUCTION") &&
               product.getStatus() != null &&
               (product.getStatus().name().equals("AUCTION_ACTIVE") || 
                product.getStatus().name().equals("AVAILABLE"));
    }
}
