package com.programacao_web.rpg_market.dto;

import com.programacao_web.rpg_market.model.DeliveryAddress;
import com.programacao_web.rpg_market.model.Product;
import com.programacao_web.rpg_market.model.User;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CheckoutSummary {
    
    private Product product;
    private User buyer;
    private User seller;
    private BigDecimal totalAmount;
    private BigDecimal goldBalance;
    private Boolean hasSufficientFunds;
    private DeliveryAddress deliveryAddress;
    private String notes;
    
    // Informações específicas do tipo de compra
    private String purchaseType; // "DIRECT_SALE" ou "AUCTION_BID" ou "AUCTION_BUY_NOW"
    private BigDecimal bidAmount; // Para lances em leilão
    private BigDecimal currentBid; // Lance atual (para leilões)
    private BigDecimal minBidAmount; // Lance mínimo necessário
    
    // Taxas e custos adicionais (para futuras expansões)
    private BigDecimal shippingCost = BigDecimal.ZERO;
    private BigDecimal serviceFee = BigDecimal.ZERO;
    
    public BigDecimal getGrandTotal() {
        return totalAmount.add(shippingCost).add(serviceFee);
    }
}
