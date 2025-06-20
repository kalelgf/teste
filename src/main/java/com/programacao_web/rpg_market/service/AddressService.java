package com.programacao_web.rpg_market.service;

import com.programacao_web.rpg_market.model.DeliveryAddress;
import com.programacao_web.rpg_market.repository.DeliveryAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    @Autowired
    private DeliveryAddressRepository deliveryAddressRepository;

    /**
     * Get all addresses for a specific user
     */
    public List<DeliveryAddress> getAddressesByUserId(String userId) {
        return deliveryAddressRepository.findByUserId(userId);
    }

    /**
     * Get a specific address by ID and user ID for security
     */
    public Optional<DeliveryAddress> getAddressByIdAndUserId(String addressId, String userId) {
        return deliveryAddressRepository.findByIdAndUserId(addressId, userId);
    }

    /**
     * Get user's default address
     */
    public Optional<DeliveryAddress> getDefaultAddress(String userId) {
        return deliveryAddressRepository.findByUserIdAndIsDefaultTrue(userId);
    }

    /**
     * Save a new address or update an existing one
     */
    public DeliveryAddress saveAddress(DeliveryAddress address) {
        // If this address is being set as default, remove default from other addresses
        if (address.getIsDefault() != null && address.getIsDefault()) {
            setOtherAddressesAsNonDefault(address.getUserId(), address.getId());
        }
        
        return deliveryAddressRepository.save(address);
    }    /**
     * Update an existing address
     */
    public DeliveryAddress updateAddress(String addressId, String userId, DeliveryAddress updatedAddress) {
        Optional<DeliveryAddress> existingAddress = getAddressByIdAndUserId(addressId, userId);
        
        if (existingAddress.isPresent()) {
            DeliveryAddress address = existingAddress.get();
            
            // Update fields
            address.setStreet(updatedAddress.getStreet());
            address.setNumber(updatedAddress.getNumber());
            address.setComplement(updatedAddress.getComplement());
            address.setDistrict(updatedAddress.getDistrict());
            address.setCity(updatedAddress.getCity());
            address.setState(updatedAddress.getState());
            address.setPostalCode(updatedAddress.getPostalCode());
            address.setLatitude(updatedAddress.getLatitude());
            address.setLongitude(updatedAddress.getLongitude());
            address.setDescription(updatedAddress.getDescription());
            
            // Handle default status
            if (updatedAddress.getIsDefault() != null && updatedAddress.getIsDefault()) {
                setOtherAddressesAsNonDefault(userId, addressId);
                address.setIsDefault(true);
            } else if (updatedAddress.getIsDefault() != null) {
                address.setIsDefault(updatedAddress.getIsDefault());
            }
            
            return deliveryAddressRepository.save(address);
        }
        
        throw new RuntimeException("Endereço não encontrado");
    }

    /**
     * Delete an address
     */
    public boolean deleteAddress(String addressId, String userId) {
        Optional<DeliveryAddress> address = getAddressByIdAndUserId(addressId, userId);
        
        if (address.isPresent()) {
            deliveryAddressRepository.delete(address.get());
            
            // If deleted address was default, set another address as default if available
            if (address.get().getIsDefault()) {
                List<DeliveryAddress> remainingAddresses = getAddressesByUserId(userId);
                if (!remainingAddresses.isEmpty()) {
                    DeliveryAddress newDefault = remainingAddresses.get(0);
                    newDefault.setIsDefault(true);
                    deliveryAddressRepository.save(newDefault);
                }
            }
            
            return true;
        }
        
        return false;
    }

    /**
     * Set an address as default
     */
    public boolean setAsDefault(String addressId, String userId) {
        Optional<DeliveryAddress> address = getAddressByIdAndUserId(addressId, userId);
        
        if (address.isPresent()) {
            // Remove default from other addresses
            setOtherAddressesAsNonDefault(userId, addressId);
            
            // Set this address as default
            DeliveryAddress defaultAddress = address.get();
            defaultAddress.setIsDefault(true);
            deliveryAddressRepository.save(defaultAddress);
            
            return true;
        }
        
        return false;
    }

    /**
     * Remove default status from all other addresses of the user
     */
    private void setOtherAddressesAsNonDefault(String userId, String excludeAddressId) {
        List<DeliveryAddress> userAddresses = getAddressesByUserId(userId);
        
        for (DeliveryAddress address : userAddresses) {
            if (!address.getId().equals(excludeAddressId) && Boolean.TRUE.equals(address.getIsDefault())) {
                address.setIsDefault(false);
                deliveryAddressRepository.save(address);
            }
        }
    }

    /**
     * Count user addresses
     */
    public long countAddressesByUserId(String userId) {
        return deliveryAddressRepository.countByUserId(userId);
    }
}
