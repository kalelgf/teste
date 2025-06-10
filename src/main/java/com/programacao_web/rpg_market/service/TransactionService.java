package com.programacao_web.rpg_market.service;

import com.programacao_web.rpg_market.model.DeliveryAddress;
import com.programacao_web.rpg_market.model.Product;
import com.programacao_web.rpg_market.model.Transaction;
import com.programacao_web.rpg_market.model.TransactionStatus;
import com.programacao_web.rpg_market.model.User;
import com.programacao_web.rpg_market.model.DeliveryAddress;
import com.programacao_web.rpg_market.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserService userService;
    
    /**
     * Cria uma nova transação para uma compra ou lance vencedor
     */
    @Transactional
    public Transaction createTransaction(Product product, User buyer, User seller, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setProduct(product);
        transaction.setBuyer(buyer);
        transaction.setSeller(seller);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        
        transaction = transactionRepository.save(transaction);
        
        // Dá experiência para comprador e vendedor
        userService.addExperience(buyer, 10);
        userService.addExperience(seller, 15);
        
        return transaction;
    }
    
    /**
     * Cria uma nova transação com endereço de entrega e observações
     */
    @Transactional
    public Transaction createTransaction(Product product, User buyer, User seller, BigDecimal amount, 
                                       DeliveryAddress deliveryAddress, String notes) {
        Transaction transaction = new Transaction();
        transaction.setProduct(product);
        transaction.setBuyer(buyer);
        transaction.setSeller(seller);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDeliveryAddress(deliveryAddress);
        transaction.setNotes(notes);
        
        transaction = transactionRepository.save(transaction);
        
        // Dá experiência para comprador e vendedor
        userService.addExperience(buyer, 10);
        userService.addExperience(seller, 15);
        
        return transaction;
    }
    
    /**
     * Atualiza o status de uma transação (versão simplificada)
     */
    @Transactional
    public Transaction updateStatus(String id, TransactionStatus newStatus) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transação não encontrada");
        }
        
        Transaction transaction = transactionOpt.get();
        transaction.setStatus(newStatus);
          if (newStatus == TransactionStatus.COMPLETED) {
            transaction.setCompletedAt(LocalDateTime.now());
            
            // Experiência extra ao completar - com verificação de null
            if (transaction.getBuyer() != null) {
                userService.addExperience(transaction.getBuyer(), 5);
            }
            if (transaction.getSeller() != null) {
                userService.addExperience(transaction.getSeller(), 5);
            }
        }
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Busca transações de um comprador
     */
    public List<Transaction> getBuyerTransactions(User buyer) {
        return transactionRepository.findByBuyer(buyer);
    }
    
    /**
     * Busca transações de um vendedor
     */
    public List<Transaction> getSellerTransactions(User seller) {
        return transactionRepository.findBySeller(seller);
    }
    
    /**
     * Busca uma transação por ID
     */
    public Optional<Transaction> findById(String id) {
        return transactionRepository.findById(id);
    }
    
    /**
     * Atualiza o status de uma transação, com verificação de permissões
     */
    @Transactional
    public Transaction updateStatus(String id, TransactionStatus newStatus, User requestingUser) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transação não encontrada");
        }
        
        Transaction transaction = transactionOpt.get();
          // Verificar permissões
        boolean isSeller = transaction.getSeller() != null && 
                          transaction.getSeller().getId() != null && 
                          transaction.getSeller().getId().equals(requestingUser.getId());
        boolean isBuyer = transaction.getBuyer() != null && 
                         transaction.getBuyer().getId() != null && 
                         transaction.getBuyer().getId().equals(requestingUser.getId());
        
        if (!isSeller && !isBuyer) {
            throw new IllegalArgumentException("Você não tem permissão para atualizar esta transação");
        }
        
        // Validar transições de status
        validateStatusTransition(transaction.getStatus(), newStatus, isSeller, isBuyer);
        
        transaction.setStatus(newStatus);
        
        // Se for concluído, atualiza data de conclusão
        if (newStatus == TransactionStatus.COMPLETED) {
            transaction.setCompletedAt(LocalDateTime.now());
        }
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Adiciona código de rastreio (para vendedor)
     */
    @Transactional
    public Transaction addTrackingCode(String id, String trackingCode, User seller) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transação não encontrada");
        }
        
        Transaction transaction = transactionOpt.get();
          // Verifica se o usuário é o vendedor
        if (transaction.getSeller() == null || 
            transaction.getSeller().getId() == null || 
            !transaction.getSeller().getId().equals(seller.getId())) {
            throw new IllegalArgumentException("Apenas o vendedor pode adicionar código de rastreio");
        }
        
        transaction.setTrackingCode(trackingCode);
        transaction.setStatus(TransactionStatus.SHIPPED);
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Confirma recebimento (para comprador)
     */
    @Transactional
    public Transaction confirmReceipt(String id, User buyer) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transação não encontrada");
        }
        
        Transaction transaction = transactionOpt.get();
          // Verifica se o usuário é o comprador
        if (transaction.getBuyer() == null || 
            transaction.getBuyer().getId() == null || 
            !transaction.getBuyer().getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("Apenas o comprador pode confirmar o recebimento");
        }
        
        // Verifica se o status atual é compatível
        if (transaction.getStatus() != TransactionStatus.SHIPPED && 
            transaction.getStatus() != TransactionStatus.DELIVERED) {
            throw new IllegalArgumentException("Não é possível confirmar o recebimento neste momento");
        }
        
        // Atualiza o status e completa a transação
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Abre uma disputa (para comprador)
     */
    @Transactional
    public Transaction openDispute(String id, String reason, User buyer) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transação não encontrada");
        }
        
        Transaction transaction = transactionOpt.get();
          // Verifica se o usuário é o comprador
        if (transaction.getBuyer() == null || 
            transaction.getBuyer().getId() == null || 
            !transaction.getBuyer().getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("Apenas o comprador pode abrir uma disputa");
        }
        
        // Verifica se o status permite abrir disputa
        if (transaction.getStatus() == TransactionStatus.COMPLETED || 
            transaction.getStatus() == TransactionStatus.CANCELED ||
            transaction.getStatus() == TransactionStatus.DISPUTED ||
            transaction.getStatus() == TransactionStatus.REFUNDED) {
            throw new IllegalArgumentException("Não é possível abrir uma disputa neste momento");
        }
        
        // Atualiza para status de disputa
        transaction.setStatus(TransactionStatus.DISPUTED);
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Atualiza o status de uma transação (nova versão)
     */
    @Transactional
    public Transaction updateTransactionStatus(String id, TransactionStatus status) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transação não encontrada");
        }
        
        Transaction transaction = transactionOpt.get();
        transaction.setStatus(status);
        
        if (status == TransactionStatus.COMPLETED) {
            transaction.setCompletedAt(LocalDateTime.now());
        }
        
        return transactionRepository.save(transaction);
    }
    
    /**
     * Validates if the transition from current status to new status is allowed
     * based on user's role in the transaction
     */
    private boolean validateStatusTransition(TransactionStatus currentStatus, TransactionStatus newStatus, 
                                             boolean isSeller, boolean isBuyer) {
        // Basic validation rules
        switch (currentStatus) {
            case PENDING:
                // Pending -> Shipped: Only seller can ship
                if (newStatus == TransactionStatus.SHIPPED) {
                    return isSeller;
                }
                // Pending -> Canceled: Both buyer and seller can cancel
                if (newStatus == TransactionStatus.CANCELED) {
                    return true;
                }
                break;
                
            case SHIPPED:
                // Shipped -> Delivered: Only buyer can confirm receipt
                if (newStatus == TransactionStatus.DELIVERED) {
                    return isBuyer;
                }
                // Shipped -> Disputed: Only buyer can open dispute
                if (newStatus == TransactionStatus.DISPUTED) {
                    return isBuyer;
                }
                break;
                
            case DISPUTED:
                // Disputed -> Resolved/Refunded: Admin only (not handled here)
                break;
                
            default:
                // Other statuses generally don't allow transitions
                return false;
        }
        
        return false; // Default: transition not allowed
    }
}
