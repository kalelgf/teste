package com.programacao_web.rpg_market.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CheckoutRequest {
    
    private String productId;
    private String deliveryAddressId;
    private String notes; // Observações da compra
    private Boolean confirmPurchase = false; // Para confirmação final
    
    // Campos para novo endereço (se não usar um existente)
    private String street;
    private String number;
    private String complement;
    private String district;
    private String city;
    private String state;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String addressDescription;
    private Boolean saveAddress = false; // Se deve salvar o endereço para uso futuro
    
    // Campo para lance (em caso de leilão)
    private BigDecimal bidAmount;
}
