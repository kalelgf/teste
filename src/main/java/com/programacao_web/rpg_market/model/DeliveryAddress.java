package com.programacao_web.rpg_market.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Data;

@Data
@Document(collection = "delivery_addresses")
public class DeliveryAddress {
    
    @Id
    private String id;
    
    @Field("user_id")
    private String userId;
    
    @Field("street")
    private String street;
    
    @Field("number")
    private String number;
    
    @Field("complement")
    private String complement;
    
    @Field("district")
    private String district;
    
    @Field("city")
    private String city;
    
    @Field("state")
    private String state;
    
    @Field("postal_code")
    private String postalCode;
    
    @Field("latitude")
    private Double latitude;
    
    @Field("longitude")
    private Double longitude;
    
    @Field("is_default")
    private Boolean isDefault = false;
    
    @Field("description")
    private String description; // ex: "Casa", "Trabalho", "Torre do Mago"
}
