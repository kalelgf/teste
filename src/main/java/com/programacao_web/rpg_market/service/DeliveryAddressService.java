package com.programacao_web.rpg_market.service;

import com.programacao_web.rpg_market.model.DeliveryAddress;
import com.programacao_web.rpg_market.repository.DeliveryAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DeliveryAddressService {
    
    @Autowired
    private DeliveryAddressRepository deliveryAddressRepository;
    
    /**
     * Busca endereços de um usuário
     */
    public List<DeliveryAddress> findByUserId(String userId) {
        return deliveryAddressRepository.findByUserId(userId);
    }
    
    /**
     * Busca endereço padrão de um usuário
     */
    public Optional<DeliveryAddress> findDefaultByUserId(String userId) {
        return deliveryAddressRepository.findByUserIdAndIsDefault(userId, true);
    }
    
    /**
     * Busca um endereço por ID
     */
    public Optional<DeliveryAddress> findById(String id) {
        return deliveryAddressRepository.findById(id);
    }
    
    /**
     * Salva um novo endereço
     */
    @Transactional
    public DeliveryAddress save(DeliveryAddress address) {
        // Se está marcando como padrão, remove o padrão anterior
        if (address.getIsDefault() != null && address.getIsDefault()) {
            Optional<DeliveryAddress> currentDefault = findDefaultByUserId(address.getUserId());
            if (currentDefault.isPresent()) {
                DeliveryAddress oldDefault = currentDefault.get();
                oldDefault.setIsDefault(false);
                deliveryAddressRepository.save(oldDefault);
            }
        }
        
        return deliveryAddressRepository.save(address);
    }
    
    /**
     * Atualiza um endereço existente
     */
    @Transactional
    public DeliveryAddress update(String id, DeliveryAddress addressData) {
        Optional<DeliveryAddress> existingOpt = deliveryAddressRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("Endereço não encontrado");
        }
        
        DeliveryAddress existing = existingOpt.get();
        
        // Atualiza os campos
        existing.setStreet(addressData.getStreet());
        existing.setNumber(addressData.getNumber());
        existing.setComplement(addressData.getComplement());
        existing.setDistrict(addressData.getDistrict());
        existing.setCity(addressData.getCity());
        existing.setState(addressData.getState());
        existing.setPostalCode(addressData.getPostalCode());
        existing.setLatitude(addressData.getLatitude());
        existing.setLongitude(addressData.getLongitude());
        existing.setDescription(addressData.getDescription());
        existing.setIsDefault(addressData.getIsDefault());
        
        return save(existing);
    }
    
    /**
     * Busca um endereço por ID e ID do usuário para segurança
     */
    public Optional<DeliveryAddress> findByIdAndUserId(String id, String userId) {
        return deliveryAddressRepository.findByIdAndUserId(id, userId);
    }
    
    /**
     * Conta endereços de um usuário
     */
    public long countByUserId(String userId) {
        return deliveryAddressRepository.countByUserId(userId);
    }
    
    /**
     * Remove um endereço verificando se pertence ao usuário
     */
    @Transactional
    public boolean deleteByIdAndUserId(String id, String userId) {
        Optional<DeliveryAddress> addressOpt = findByIdAndUserId(id, userId);
        if (addressOpt.isPresent()) {
            DeliveryAddress address = addressOpt.get();
            
            // Se era o endereço padrão, definir outro como padrão
            if (Boolean.TRUE.equals(address.getIsDefault())) {
                List<DeliveryAddress> remainingAddresses = findByUserId(userId);
                remainingAddresses.removeIf(addr -> addr.getId().equals(id));
                if (!remainingAddresses.isEmpty()) {
                    DeliveryAddress newDefault = remainingAddresses.get(0);
                    newDefault.setIsDefault(true);
                    deliveryAddressRepository.save(newDefault);
                }
            }
            
            deliveryAddressRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    /**
     * Remove um endereço
     */
    @Transactional
    public void delete(String id) {
        deliveryAddressRepository.deleteById(id);
    }
    
    /**
     * Define um endereço como padrão
     */
    @Transactional
    public void setAsDefault(String id, String userId) {
        Optional<DeliveryAddress> addressOpt = deliveryAddressRepository.findById(id);
        if (addressOpt.isEmpty()) {
            throw new RuntimeException("Endereço não encontrado");
        }
        
        DeliveryAddress address = addressOpt.get();
        
        // Verifica se o endereço pertence ao usuário
        if (!address.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Você não tem permissão para modificar este endereço");
        }
        
        // Remove o padrão anterior
        Optional<DeliveryAddress> currentDefault = findDefaultByUserId(userId);
        if (currentDefault.isPresent()) {
            DeliveryAddress oldDefault = currentDefault.get();
            oldDefault.setIsDefault(false);
            deliveryAddressRepository.save(oldDefault);
        }
        
        // Define o novo padrão
        address.setIsDefault(true);
        deliveryAddressRepository.save(address);
    }
}
