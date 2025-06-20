package com.programacao_web.rpg_market.dto;

import java.math.BigDecimal;

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

    // Getters e Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public String getDeliveryAddressId() { return deliveryAddressId; }
    public void setDeliveryAddressId(String deliveryAddressId) { this.deliveryAddressId = deliveryAddressId; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Boolean getConfirmPurchase() { return confirmPurchase; }
    public void setConfirmPurchase(Boolean confirmPurchase) { this.confirmPurchase = confirmPurchase; }
    
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    
    public String getComplement() { return complement; }
    public void setComplement(String complement) { this.complement = complement; }
    
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public String getAddressDescription() { return addressDescription; }
    public void setAddressDescription(String addressDescription) { this.addressDescription = addressDescription; }
    
    public Boolean getSaveAddress() { return saveAddress; }
    public void setSaveAddress(Boolean saveAddress) { this.saveAddress = saveAddress; }
    
    public BigDecimal getBidAmount() { return bidAmount; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }
}
