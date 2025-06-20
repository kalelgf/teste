package com.programacao_web.rpg_market.model;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {
    
    @Id
    private String id;
    
    @Field("name")
    private String name;
    
    @Field("description")
    private String description;
    
    @Field("price")
    private BigDecimal price;
    
    @Field("category")
    private ProductCategory category;
    
    @Field("status")
    private ProductStatus status = ProductStatus.AVAILABLE;
    
    @Field("type")
    private ProductType type = ProductType.DIRECT_SALE;
    
    @Field("image_url")
    private String imageUrl;
    
    @DBRef
    private User seller;
    
    @DBRef
    private List<Bid> bids = new ArrayList<>();
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("auction_end_date")
    private LocalDateTime auctionEndDate;
    
    @Field("buy_now_price")
    private BigDecimal buyNowPrice;
    
    @Field("min_bid_increment")
    private BigDecimal minBidIncrement;
    
    // Novos campos para elementos RPG
    @Field("rarity")
    private ItemRarity rarity = ItemRarity.COMUM;
    
    @Field("level_required")
    private Integer levelRequired = 1;
    
    @Field("magic_properties")
    private Set<MagicProperty> magicProperties = new HashSet<>();
    
    @Field("quantity")
    private Integer quantity = 1;
    
    @Field("weight")
    private BigDecimal weight = BigDecimal.ONE;
    
    @Field("durability")
    private Integer durability = 100;
    
    @Field("history")
    private String history;
    
    @Field("experience_gained")
    private Integer experienceGained = 0;
    
    // Getters e Setters manuais para resolver problema do Lombok
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }
    
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    
    public ProductType getType() { return type; }
    public void setType(ProductType type) { this.type = type; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
    
    public List<Bid> getBids() { return bids; }
    public void setBids(List<Bid> bids) { this.bids = bids; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getAuctionEndDate() { return auctionEndDate; }
    public void setAuctionEndDate(LocalDateTime auctionEndDate) { this.auctionEndDate = auctionEndDate; }
    
    public BigDecimal getBuyNowPrice() { return buyNowPrice; }
    public void setBuyNowPrice(BigDecimal buyNowPrice) { this.buyNowPrice = buyNowPrice; }
    
    public BigDecimal getMinBidIncrement() { return minBidIncrement; }
    public void setMinBidIncrement(BigDecimal minBidIncrement) { this.minBidIncrement = minBidIncrement; }
    
    public ItemRarity getRarity() { return rarity; }
    public void setRarity(ItemRarity rarity) { this.rarity = rarity; }
    
    public Set<MagicProperty> getMagicProperties() { return magicProperties; }
    public void setMagicProperties(Set<MagicProperty> magicProperties) { this.magicProperties = magicProperties; }
    
    public void addMagicProperty(MagicProperty property) {
        if (this.magicProperties == null) {
            this.magicProperties = new HashSet<>();
        }
        this.magicProperties.add(property);
    }
}
