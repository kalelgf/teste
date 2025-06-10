package com.programacao_web.rpg_market.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(collection = "transactions")
public class Transaction {
    
    @Id
    private String id;
    
    @DBRef
    private Product product;
    
    @DBRef
    private User buyer;
    
    @DBRef
    private User seller;
    
    @Field("amount")
    private BigDecimal amount;
    
    @Field("status")
    private TransactionStatus status;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("completed_at")
    private LocalDateTime completedAt;
      @Field("tracking_code")
    private String trackingCode;
    
    @DBRef
    private DeliveryAddress deliveryAddress;
    
    @Field("notes")
    private String notes; // Observações da compra
}
