package com.programacao_web.rpg_market.controller;

import com.programacao_web.rpg_market.dto.CheckoutRequest;
import com.programacao_web.rpg_market.dto.CheckoutSummary;
import com.programacao_web.rpg_market.model.*;
import com.programacao_web.rpg_market.service.CheckoutService;
import com.programacao_web.rpg_market.service.ProductService;
import com.programacao_web.rpg_market.service.TransactionService;
import com.programacao_web.rpg_market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    @Autowired
    private CheckoutService checkoutService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private TransactionService transactionService;

    /**
     * Inicia o processo de checkout para venda direta
     */
    @GetMapping("/comprar/{productId}")
    public String startDirectSaleCheckout(
            @PathVariable String productId,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            Optional<Product> productOpt = productService.findById(productId);
            
            if (userOpt.isEmpty() || productOpt.isEmpty()) {
                return "error/404";
            }
            
            User buyer = userOpt.get();
            Product product = productOpt.get();
            
            // Verificações básicas
            if (product.getStatus() != ProductStatus.AVAILABLE) {
                return "redirect:/item/" + productId + "?error=unavailable";
            }
            
            if (buyer.getId().equals(product.getSeller().getId())) {
                return "redirect:/item/" + productId + "?error=own-product";
            }
            
            // Prepara checkout inicial
            CheckoutRequest request = new CheckoutRequest();
            request.setProductId(productId);
            
            CheckoutSummary summary = checkoutService.prepareCheckout(productId, buyer, request);
            
            model.addAttribute("summary", summary);
            model.addAttribute("request", request);
            model.addAttribute("userAddresses", checkoutService.getUserAddresses(buyer));
            model.addAttribute("checkoutType", "DIRECT_SALE");
            
            return "checkout/review";
            
        } catch (Exception e) {
            return "redirect:/item/" + productId + "?error=" + e.getMessage();
        }
    }
    
    /**
     * Inicia o processo de checkout para compra imediata em leilão
     */
    @GetMapping("/comprar-agora/{productId}")
    public String startAuctionBuyNowCheckout(
            @PathVariable String productId,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            Optional<Product> productOpt = productService.findById(productId);
            
            if (userOpt.isEmpty() || productOpt.isEmpty()) {
                return "error/404";
            }
            
            User buyer = userOpt.get();
            Product product = productOpt.get();
            
            // Verificações para leilão
            if (product.getType() != ProductType.AUCTION || 
                product.getStatus() != ProductStatus.AUCTION_ACTIVE ||
                product.getBuyNowPrice() == null) {
                return "redirect:/item/" + productId + "?error=invalid-auction";
            }
            
            if (buyer.getId().equals(product.getSeller().getId())) {
                return "redirect:/item/" + productId + "?error=own-product";
            }
            
            // Prepara checkout para compra imediata
            CheckoutRequest request = new CheckoutRequest();
            request.setProductId(productId);
            
            CheckoutSummary summary = checkoutService.prepareCheckout(productId, buyer, request);
            
            model.addAttribute("summary", summary);
            model.addAttribute("request", request);
            model.addAttribute("userAddresses", checkoutService.getUserAddresses(buyer));
            model.addAttribute("checkoutType", "AUCTION_BUY_NOW");
            
            return "checkout/review";
            
        } catch (Exception e) {
            return "redirect:/item/" + productId + "?error=" + e.getMessage();
        }
    }
    
    /**
     * Processa lance em leilão
     */
    @PostMapping("/lance/{productId}")
    public String placeBid(
            @PathVariable String productId,
            @RequestParam BigDecimal bidAmount,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Usuário não encontrado");
                return "redirect:/item/" + productId;
            }
            
            User bidder = userOpt.get();
            CheckoutRequest request = new CheckoutRequest();
            request.setProductId(productId);
            request.setBidAmount(bidAmount);
            request.setNotes(notes);
            
            // Processa o lance
            checkoutService.confirmPurchase(productId, bidder, request);
            
            redirectAttributes.addFlashAttribute("success", 
                "Lance de " + bidAmount + " moedas de ouro registrado com sucesso!");
            
            return "redirect:/item/" + productId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/item/" + productId;
        }
    }
    
    /**
     * Confirma a compra após revisão
     */
    @PostMapping("/confirmar")
    public String confirmPurchase(
            @ModelAttribute CheckoutRequest request,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
          try {
            log.info("Confirm purchase request: productId={}, bidAmount={}, confirmPurchase={}", 
                     request.getProductId(), request.getBidAmount(), request.getConfirmPurchase());
            
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Usuário não encontrado");
                return "redirect:/mercado";
            }
            
            User buyer = userOpt.get();
            request.setConfirmPurchase(true);
            
            Transaction transaction = checkoutService.confirmPurchase(
                request.getProductId(), buyer, request);
            
            if (transaction != null) {
                // Compra realizada com sucesso
                log.info("Purchase successful, redirecting to success page: transactionId={}", transaction.getId());
                return "redirect:/checkout/sucesso/" + transaction.getId();
            } else {
                // Foi um lance (retorna null)
                log.info("Bid successful, redirecting to product page");
                redirectAttributes.addFlashAttribute("success", "Lance registrado com sucesso!");
                return "redirect:/item/" + request.getProductId() + "?success=bid";
            }
            
        } catch (Exception e) {
            log.error("Error confirming purchase: ", e);
            redirectAttributes.addFlashAttribute("error", "Erro ao processar: " + e.getMessage());
            
            // Redirect back to appropriate page based on purchase type
            if (request.getBidAmount() != null) {
                return "redirect:/checkout/lance/" + request.getProductId() + "?error=" + e.getMessage();
            } else {
                return "redirect:/checkout/comprar/" + request.getProductId() + "?error=" + e.getMessage();
            }
        }
    }
    
    /**
     * Endpoint para buscar endereços via AJAX
     */
    @GetMapping("/enderecos")
    @ResponseBody
    public Object getUserAddresses(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                return "[]";
            }
            
            return checkoutService.getUserAddresses(userOpt.get());
            
        } catch (Exception e) {
            return "[]";
        }
    }
    
    /**
     * Inicia o processo de lance em leilão
     */
    @GetMapping("/lance/{productId}")
    public String startBidCheckout(
            @PathVariable String productId,
            @RequestParam(required = false) BigDecimal bidAmount,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            Optional<Product> productOpt = productService.findById(productId);
            
            if (userOpt.isEmpty() || productOpt.isEmpty()) {
                return "error/404";
            }
            
            User bidder = userOpt.get();
            Product product = productOpt.get();
            
            // Verificações para leilão
            if (product.getType() != ProductType.AUCTION || 
                product.getStatus() != ProductStatus.AUCTION_ACTIVE) {
                return "redirect:/item/" + productId + "?error=invalid-auction";
            }
            
            if (bidder.getId().equals(product.getSeller().getId())) {
                return "redirect:/item/" + productId + "?error=own-product";
            }
              // Prepara o checkout para lance
            CheckoutRequest request = new CheckoutRequest();
            request.setProductId(productId);
            if (bidAmount != null && bidAmount.compareTo(BigDecimal.ZERO) > 0) {
                request.setBidAmount(bidAmount);
            } else {
                // Set default minimum bid if none provided
                BigDecimal currentPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                BigDecimal increment = product.getMinBidIncrement() != null ? product.getMinBidIncrement() : BigDecimal.ONE;
                request.setBidAmount(currentPrice.add(increment));
            }
            
            CheckoutSummary summary = checkoutService.prepareCheckout(productId, bidder, request);
            
            model.addAttribute("summary", summary);
            model.addAttribute("request", request);
            model.addAttribute("checkoutType", "AUCTION_BID");
            
            return "checkout/bid";
            
        } catch (Exception e) {
            return "redirect:/item/" + productId + "?error=" + e.getMessage();
        }
    }
    
    /**
     * Página de confirmação de lance - endpoint alternativo para /bid
     */
    @GetMapping("/bid")
    public String bidPage(
            @RequestParam String productId,
            @RequestParam(required = false) BigDecimal bidAmount,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model) {
        
        log.info("Accessing bid page: productId={}, bidAmount={}, user={}", 
                 productId, bidAmount, currentUser.getUsername());
        
        // Redireciona para o endpoint padrão
        return startBidCheckout(productId, bidAmount, currentUser, model);
    }
    
    /**
     * Exibe página de sucesso após compra
     */
    @GetMapping("/sucesso/{transactionId}")
    public String showPurchaseSuccess(
            @PathVariable String transactionId,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            Optional<Transaction> transactionOpt = transactionService.findById(transactionId);
            
            if (userOpt.isEmpty() || transactionOpt.isEmpty()) {
                return "error/404";
            }
            
            Transaction transaction = transactionOpt.get();
            User user = userOpt.get();
            
            // Verifica se o usuário é o comprador desta transação
            if (!transaction.getBuyer().getId().equals(user.getId())) {
                return "error/403";
            }
            
            model.addAttribute("transaction", transaction);
            
            return "checkout/success";
            
        } catch (Exception e) {
            return "error/500";
        }
    }
}
