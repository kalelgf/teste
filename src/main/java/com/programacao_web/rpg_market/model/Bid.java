package com.programacao_web.rpg_market.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bids")
public class Bid {
    
    @Id
    private String id;
    
    @DBRef
    private Product product;
    
    @DBRef
    private User bidder;
    
    @Field("amount")
    private BigDecimal amount;
    
    @Field("bid_time")
    private LocalDateTime bidTime = LocalDateTime.now();
    
    @Field("winning")
    private boolean winning;
    
    // Getters e Setters manuais para resolver problema do Lombok
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public User getBidder() { return bidder; }
    public void setBidder(User bidder) { this.bidder = bidder; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }
    
    public boolean isWinning() { return winning; }
    public void setWinning(boolean winning) { this.winning = winning; }
}
