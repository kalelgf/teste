package com.programacao_web.rpg_market.repository;

import com.programacao_web.rpg_market.model.DeliveryAddress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAddressRepository extends MongoRepository<DeliveryAddress, String> {
    
    List<DeliveryAddress> findByUserId(String userId);
    
    Optional<DeliveryAddress> findByUserIdAndIsDefault(String userId, Boolean isDefault);
    
    Optional<DeliveryAddress> findByUserIdAndIsDefaultTrue(String userId);
    
    Optional<DeliveryAddress> findByIdAndUserId(String id, String userId);
    
    long countByUserId(String userId);
    
    void deleteByUserId(String userId);
}
