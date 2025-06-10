package com.programacao_web.rpg_market.controller;

import com.programacao_web.rpg_market.model.Transaction;
import com.programacao_web.rpg_market.model.TransactionStatus;
import com.programacao_web.rpg_market.model.User;
import com.programacao_web.rpg_market.service.TransactionService;
import com.programacao_web.rpg_market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/transacao")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private UserService userService;
    
    // Exibe detalhes de uma transação
    @GetMapping("/{id}")
    public String showTransaction(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model) {
        
        Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
        if (userOpt.isEmpty()) {
            return "error/403";
        }
        
        User user = userOpt.get();
        Optional<Transaction> transactionOpt = transactionService.findById(id);
        
        if (transactionOpt.isEmpty()) {
            return "error/404";
        }
          Transaction transaction = transactionOpt.get();
        
        // Verifica se o usuário é parte da transação (com segurança para DBRef)
        boolean isBuyer = transaction.getBuyer() != null && 
                         transaction.getBuyer().getId() != null && 
                         transaction.getBuyer().getId().equals(user.getId());
        boolean isSeller = transaction.getSeller() != null && 
                          transaction.getSeller().getId() != null && 
                          transaction.getSeller().getId().equals(user.getId());
        
        if (!isBuyer && !isSeller) {
            return "error/403";
        }
        
        model.addAttribute("transaction", transaction);
        return "transaction/details";
    }
    
    // Atualiza o status de uma transação
    @PostMapping("/{id}/atualizar")
    public String updateTransactionStatus(
            @PathVariable String id,
            @RequestParam TransactionStatus status,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("Usuário não encontrado");
            }
            
            Transaction updated = transactionService.updateStatus(id, status, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "Status atualizado para: " + updated.getStatus().name());
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/transacao/" + id;
    }
    
    // Adiciona código de rastreio (para vendedor)
    @PostMapping("/{id}/rastreio")
    public String addTrackingCode(
            @PathVariable String id,
            @RequestParam String trackingCode,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("Usuário não encontrado");
            }
            
            transactionService.addTrackingCode(id, trackingCode, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "Código de rastreio adicionado");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/transacao/" + id;
    }
    
    // Confirma recebimento (para comprador)
    @PostMapping("/{id}/confirmar-recebimento")
    public String confirmReceipt(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("Usuário não encontrado");
            }
            
            transactionService.confirmReceipt(id, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "Recebimento confirmado! O pagamento foi liberado para o vendedor.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/transacao/" + id;
    }
    
    // Abre uma disputa (para comprador)
    @PostMapping("/{id}/abrir-disputa")
    public String openDispute(
            @PathVariable String id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("Usuário não encontrado");
            }
            
            transactionService.openDispute(id, reason, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "Disputa aberta. Um mestre irá analisar o caso.");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/transacao/" + id;
    }
}
