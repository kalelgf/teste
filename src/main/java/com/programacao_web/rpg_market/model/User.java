package com.programacao_web.rpg_market.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Data;

import java.math.BigDecimal;

@Data
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    
    private String characterClass;
    
    private int level;
    
    private int experience;
    
    private BigDecimal goldCoins;
    
    private UserRole role;

    private String profileImageUrl;

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // Método alias para compatibilidade com o sistema de checkout
    public BigDecimal getGoldBalance() {
        return goldCoins;
    }
    
    public void setGoldBalance(BigDecimal goldBalance) {
        this.goldCoins = goldBalance;
    }
}
