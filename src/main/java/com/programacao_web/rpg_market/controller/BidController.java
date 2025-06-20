package com.programacao_web.rpg_market.controller;

import com.programacao_web.rpg_market.model.*;
import com.programacao_web.rpg_market.service.ProductService;
import com.programacao_web.rpg_market.service.UserService;
import com.programacao_web.rpg_market.service.BidService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping({"/bid", "/lance"})
public class BidController {

    private static final Logger log = LoggerFactory.getLogger(BidController.class);    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BidService bidService;    /**
     * Prepara o modal de confirmação de lance
     */
    @PostMapping("/prepare")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> prepareBid(
            @RequestParam String productId,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal UserDetails currentUser) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("=== PREPARANDO MODAL DE LANCE ===");
            log.info("ProductId: {}, Amount: {}, User: {}", productId, amount, currentUser.getUsername());
            
            // Buscar usuário
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Usuário não encontrado");
                return ResponseEntity.ok(response);
            }
            User bidder = userOpt.get();
            
            // Buscar produto
            Optional<Product> productOpt = productService.findById(productId);
            if (productOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Produto não encontrado");
                return ResponseEntity.ok(response);
            }
            Product product = productOpt.get();
            
            // Validações de negócio
            String validationError = validateBid(product, bidder, amount);
            if (validationError != null) {
                response.put("success", false);
                response.put("error", validationError);
                return ResponseEntity.ok(response);
            }
              // Verificar saldo
            boolean hasSufficientFunds = bidder.getGoldCoins().compareTo(amount) >= 0;
            
            // Calcular lance mínimo
            BigDecimal currentPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal minIncrement = product.getMinBidIncrement() != null ? product.getMinBidIncrement() : BigDecimal.ONE;
            BigDecimal minBid = currentPrice.add(minIncrement);
            
            // VERIFICAÇÃO CRÍTICA: Se não há saldo suficiente, retornar erro
            if (!hasSufficientFunds) {
                BigDecimal needed = amount.subtract(bidder.getGoldCoins());
                response.put("success", false);
                response.put("error", String.format("💰 Saldo insuficiente! Você tem %.2f moedas e precisa de %.2f moedas (faltam %.2f moedas)", 
                    bidder.getGoldCoins(), amount, needed));
                return ResponseEntity.ok(response);
            }
              // Preparar dados do modal (apenas se passou nas validações)
            response.put("success", true);
            response.put("productId", productId);
            response.put("productName", product.getName());
            response.put("bidAmount", amount);
            response.put("currentPrice", currentPrice);
            response.put("minBid", minBid);
            response.put("userBalance", bidder.getGoldCoins());
            response.put("hasSufficientFunds", hasSufficientFunds);
            response.put("seller", product.getSeller().getUsername());
            response.put("minIncrement", product.getMinBidIncrement());
            response.put("message", String.format("Lance de %.2f moedas será registrado. Só será debitado se você vencer o leilão!", amount));
            
            log.info("✅ Modal preparado com sucesso");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Erro ao preparar modal de lance: ", e);
            response.put("success", false);
            response.put("error", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }    /**
     * Confirma e processa o lance
     */
    @PostMapping("/confirm")
    public String confirmBid(
            @RequestParam String productId,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            log.info("=== CONFIRMANDO LANCE ===");
            log.info("ProductId: {}, Amount: {}, User: {}", productId, amount, currentUser.getUsername());
            
            // Buscar usuário
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Usuário não encontrado");
                return "redirect:/item/" + productId;
            }
            User bidder = userOpt.get();
            
            // Buscar produto
            Optional<Product> productOpt = productService.findById(productId);
            if (productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Produto não encontrado");
                return "redirect:/mercado";
            }
            Product product = productOpt.get();
            
            // Validações finais
            String validationError = validateBid(product, bidder, amount);
            if (validationError != null) {
                redirectAttributes.addFlashAttribute("error", validationError);
                return "redirect:/item/" + productId;
            }
            
            // Verificar saldo novamente (importante!)
            if (bidder.getGoldCoins().compareTo(amount) < 0) {
                BigDecimal needed = amount.subtract(bidder.getGoldCoins());
                redirectAttributes.addFlashAttribute("error", 
                    String.format("Saldo insuficiente! Você precisa de mais %.2f moedas de ouro", needed));
                return "redirect:/item/" + productId;
            }
            
            // Registrar lance usando o BidService (SEM DÉBITO)
            bidService.placeBid(product, bidder, amount);
            
            log.info("=== LANCE CONFIRMADO COM SUCESSO ===");
            redirectAttributes.addFlashAttribute("success", 
                String.format("🎯 Lance de %.2f moedas registrado com sucesso! " +
                             "Seu ouro será debitado apenas se você vencer o leilão. " +
                             "Boa sorte, aventureiro!", amount));
            
            return "redirect:/item/" + productId;
            
        } catch (Exception e) {
            log.error("❌ Erro ao confirmar lance: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Erro ao processar lance: " + e.getMessage());
            return "redirect:/item/" + productId;
        }
    }

    /**
     * Processar lance - Rota simplificada para compatibilidade
     */
    @PostMapping("/dar")
    public String makeBid(
            @RequestParam String productId,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Buscar usuário
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Usuário não encontrado");
                return "redirect:/item/" + productId;
            }
            
            // Processar lance usando o serviço
            bidService.placeBidSimple(productId, userOpt.get(), amount);
            
            // Sucesso
            redirectAttributes.addFlashAttribute("success", 
                String.format("Lance de $%.2f registrado com sucesso!", amount));
            
            return "redirect:/item/" + productId;
            
        } catch (Exception e) {
            log.error("Erro ao processar lance: ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Erro ao processar lance: " + e.getMessage());
            return "redirect:/item/" + productId;
        }
    }    /**
     * Valida se o lance é válido
     */
    private String validateBid(Product product, User bidder, BigDecimal amount) {
        // Verificar se é um leilão ativo
        if (product.getType() != ProductType.AUCTION || 
            product.getStatus() != ProductStatus.AUCTION_ACTIVE) {
            return "Este item não está em leilão ativo";
        }
        
        // Verificar se não é o próprio vendedor
        if (bidder.getId().equals(product.getSeller().getId())) {
            return "Você não pode dar lances no seu próprio item";
        }
        
        // VALIDAÇÃO CRUCIAL: Verificar se o usuário já está liderando o leilão
        if (bidService.isUserWinning(product, bidder)) {
            return "🏆 Você já está liderando este leilão! Aguarde outros aventureiros darem seus lances.";
        }
        
        // Verificar valor mínimo
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Valor de lance inválido";
        }
        
        // Verificar se o lance é maior que o atual
        BigDecimal currentPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        BigDecimal minIncrement = product.getMinBidIncrement() != null ? 
                                 product.getMinBidIncrement() : BigDecimal.ONE;
        BigDecimal minBid = currentPrice.add(minIncrement);
        
        if (amount.compareTo(minBid) < 0) {
            return String.format("O lance deve ser pelo menos %.2f moedas", minBid);
        }
        
        return null; // Tudo válido
    }
}
