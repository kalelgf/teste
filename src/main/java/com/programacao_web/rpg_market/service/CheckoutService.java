package com.programacao_web.rpg_market.service;

import com.programacao_web.rpg_market.dto.CheckoutRequest;
import com.programacao_web.rpg_market.dto.CheckoutSummary;
import com.programacao_web.rpg_market.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {
    
    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private DeliveryAddressService deliveryAddressService;
    
    @Autowired
    private TransactionService transactionService;
    
    /**
     * Prepara o resumo do checkout antes da confirmação
     */
    public CheckoutSummary prepareCheckout(String productId, User buyer, CheckoutRequest request) {
        // Busca o produto
        Optional<Product> productOpt = productService.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Produto não encontrado");
        }
        
        Product product = productOpt.get();
        
        // Verifica se o comprador não é o vendedor
        if (buyer.getId().equals(product.getSeller().getId())) {
            throw new IllegalArgumentException("Você não pode comprar seu próprio item");
        }
        
        // Verifica se o produto está disponível
        if (product.getStatus() != ProductStatus.AVAILABLE && 
            product.getStatus() != ProductStatus.AUCTION_ACTIVE) {
            throw new IllegalArgumentException("Este produto não está disponível para compra");
        }
        
        CheckoutSummary summary = new CheckoutSummary();
        summary.setProduct(product);
        summary.setBuyer(buyer);
        summary.setSeller(product.getSeller());
        summary.setGoldBalance(buyer.getGoldBalance());
        summary.setNotes(request.getNotes());
          // Determina o tipo de compra e valor
        if (product.getType() == ProductType.DIRECT_SALE) {
            summary.setPurchaseType("DIRECT_SALE");
            summary.setTotalAmount(product.getPrice());
        } else if (product.getType() == ProductType.AUCTION) {
            if (request.getBidAmount() != null && request.getBidAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Lance em leilão
                summary.setPurchaseType("AUCTION_BID");
                summary.setBidAmount(request.getBidAmount());
                summary.setTotalAmount(request.getBidAmount());
                
                // Calcula lance mínimo
                BigDecimal currentPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                BigDecimal increment = product.getMinBidIncrement() != null ? product.getMinBidIncrement() : BigDecimal.ONE;
                summary.setCurrentBid(currentPrice);
                summary.setMinBidAmount(currentPrice.add(increment));
                
                // Valida o lance
                if (request.getBidAmount().compareTo(summary.getMinBidAmount()) < 0) {
                    throw new IllegalArgumentException("O lance deve ser de pelo menos " + 
                        summary.getMinBidAmount() + " moedas de ouro");
                }
            } else if (product.getBuyNowPrice() != null && request.getBidAmount() == null) {
                // Compra imediata em leilão
                summary.setPurchaseType("AUCTION_BUY_NOW");
                summary.setTotalAmount(product.getBuyNowPrice());
            } else {
                throw new IllegalArgumentException("Lance inválido ou tipo de compra não especificado para leilão");
            }
        }
          // Verifica saldo
        summary.setHasSufficientFunds(buyer.getGoldBalance().compareTo(summary.getGrandTotal()) >= 0);
        
        // Processa endereço de entrega
        if (request.getDeliveryAddressId() != null) {
            Optional<DeliveryAddress> addressOpt = deliveryAddressService.findById(request.getDeliveryAddressId());
            if (addressOpt.isPresent() && addressOpt.get().getUserId().equals(buyer.getId())) {
                summary.setDeliveryAddress(addressOpt.get());
            } else {
                throw new IllegalArgumentException("Endereço de entrega não encontrado");
            }
        } else if (request.getStreet() != null && !request.getStreet().trim().isEmpty()) {
            // Criar endereço temporário
            DeliveryAddress tempAddress = new DeliveryAddress();
            tempAddress.setStreet(request.getStreet());
            tempAddress.setNumber(request.getNumber());
            tempAddress.setComplement(request.getComplement());
            tempAddress.setDistrict(request.getDistrict());
            tempAddress.setCity(request.getCity());
            tempAddress.setState(request.getState());
            tempAddress.setPostalCode(request.getPostalCode());
            tempAddress.setLatitude(request.getLatitude());
            tempAddress.setLongitude(request.getLongitude());
            tempAddress.setDescription(request.getAddressDescription());
            summary.setDeliveryAddress(tempAddress);
        } else {
            // Usar endereço padrão se disponível
            Optional<DeliveryAddress> defaultAddress = deliveryAddressService.findDefaultByUserId(buyer.getId());
            if (defaultAddress.isPresent()) {
                summary.setDeliveryAddress(defaultAddress.get());
            }
        }
        
        return summary;
    }
    
    /**
     * Confirma e processa a compra
     */
    @Transactional
    public Transaction confirmPurchase(String productId, User buyer, CheckoutRequest request) {
        // Prepara o checkout novamente para validar
        CheckoutSummary summary = prepareCheckout(productId, buyer, request);
        
        if (!summary.getHasSufficientFunds()) {
            throw new IllegalArgumentException("Saldo insuficiente. Você precisa de " + 
                summary.getGrandTotal() + " moedas de ouro, mas possui apenas " + 
                summary.getGoldBalance());
        }
        
        if (summary.getDeliveryAddress() == null) {
            throw new IllegalArgumentException("Endereço de entrega é obrigatório");
        }
          Product product = summary.getProduct();
        
        // Ensure delivery address is saved before creating transaction
        // because Transaction uses @DBRef for deliveryAddress
        if (summary.getDeliveryAddress() != null && summary.getDeliveryAddress().getId() == null) {
            DeliveryAddress newAddress = summary.getDeliveryAddress();
            newAddress.setUserId(buyer.getId());
            
            // Save address permanently if user requested, otherwise save as temporary
            if (request.getSaveAddress() != null && request.getSaveAddress()) {
                newAddress.setIsDefault(false);
            }
            
            deliveryAddressService.save(newAddress);
        }

        Transaction transaction;// Processa baseado no tipo de compra
        if ("DIRECT_SALE".equals(summary.getPurchaseType()) || "AUCTION_BUY_NOW".equals(summary.getPurchaseType())) {
            // Compra direta ou compra imediata em leilão
            
            // Deduz o valor do saldo do comprador
            userService.deductGold(buyer, summary.getGrandTotal());
            
            // Atualiza status do produto para SOLD usando buyNow que já faz isso corretamente
            // mas sem criar transação duplicada, usando apenas a lógica de status
            if (product.getStatus() != ProductStatus.AVAILABLE && 
                product.getStatus() != ProductStatus.AUCTION_ACTIVE) {
                throw new IllegalArgumentException("Este produto não está disponível para compra");
            }
            
            product.setStatus(ProductStatus.SOLD);
            
            // Se for leilão com compra imediata, também marca como encerrado
            if ("AUCTION_BUY_NOW".equals(summary.getPurchaseType())) {
                product.setStatus(ProductStatus.AUCTION_ENDED);
            }
            
            // Salva as alterações do produto diretamente via ProductService
            productService.save(product);
              // Cria a transação com endereço de entrega e observações
            transaction = transactionService.createTransaction(
                product, buyer, summary.getSeller(), summary.getTotalAmount(),
                summary.getDeliveryAddress(), summary.getNotes());
            
            // Para vendas diretas e compras imediatas de leilão, marca como completa
            transaction = transactionService.updateStatus(transaction.getId(), TransactionStatus.COMPLETED);        } else if ("AUCTION_BID".equals(summary.getPurchaseType())) {
            // Lance em leilão
            log.info("Processando lance em leilão: produtoId={}, licitante={}, valor={}", 
                     productId, buyer.getUsername(), summary.getBidAmount());
            
            productService.makeBid(product, buyer, summary.getBidAmount());
            
            log.info("Lance em leilão processado com sucesso");
            
            // Para lances, não criamos transação imediatamente
            // A transação será criada quando o leilão terminar
            return null; // Indica que foi um lance, não uma compra
            
        } else {
            throw new IllegalArgumentException("Tipo de compra inválido");
        }
        
        return transaction;
    }
    
    /**
     * Busca endereços de entrega do usuário
     */
    public List<DeliveryAddress> getUserAddresses(User user) {
        return deliveryAddressService.findByUserId(user.getId());
    }
    
    /**
     * Busca endereço padrão do usuário
     */
    public Optional<DeliveryAddress> getUserDefaultAddress(User user) {
        return deliveryAddressService.findDefaultByUserId(user.getId());
    }
}
