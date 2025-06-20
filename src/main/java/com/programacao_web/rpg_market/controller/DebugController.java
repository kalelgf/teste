package com.programacao_web.rpg_market.controller;

import com.programacao_web.rpg_market.model.*;
import com.programacao_web.rpg_market.repository.BidRepository;
import com.programacao_web.rpg_market.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/debug")
public class DebugController {

    private static final Logger log = LoggerFactory.getLogger(DebugController.class);    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private com.programacao_web.rpg_market.repository.ProductRepository productRepository;

    @Autowired
    private org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping requestMappingHandlerMapping;

    @GetMapping("/product/{productId}")
    @ResponseBody
    public Map<String, Object> debugProduct(@PathVariable String productId, 
                                           @AuthenticationPrincipal UserDetails currentUser) {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            Optional<Product> productOpt = productService.findById(productId);
            if (productOpt.isEmpty()) {
                debug.put("error", "Product not found");
                debug.put("timestamp", java.time.LocalDateTime.now());
                return debug;
            }
            
            Product product = productOpt.get();
            debug.put("product", Map.of(
                "id", product.getId(),
                "name", product.getName(),
                "type", product.getType(),
                "status", product.getStatus(),
                "price", product.getPrice(),
                "seller", product.getSeller().getUsername(),
                "minBidIncrement", product.getMinBidIncrement(),
                "buyNowPrice", product.getBuyNowPrice()
            ));
            
            if (currentUser != null) {
                Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    debug.put("currentUser", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "goldBalance", user.getGoldBalance(),
                        "isOwner", user.getId().equals(product.getSeller().getId())
                    ));
                }
            }
            
            List<Bid> bids = bidRepository.findByProductOrderByAmountDesc(product);
            debug.put("bidCount", bids.size());
            debug.put("bids", bids.stream().limit(5).map(bid -> Map.of(
                "id", bid.getId(),
                "amount", bid.getAmount(),
                "bidder", bid.getBidder().getUsername(),
                "bidTime", bid.getBidTime(),
                "winning", bid.isWinning()
            )).toList());
            
            Optional<Bid> highestBid = bidRepository.findHighestBidForProduct(product);
            if (highestBid.isPresent()) {
                debug.put("highestBid", Map.of(
                    "amount", highestBid.get().getAmount(),
                    "bidder", highestBid.get().getBidder().getUsername()
                ));
            }
            
            debug.put("timestamp", java.time.LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Debug error: ", e);
            debug.put("error", e.getMessage());
            debug.put("exception", e.getClass().getSimpleName());
            debug.put("timestamp", java.time.LocalDateTime.now());
        }
        
        return debug;
    }

    /**
     * Debug endpoint para verificar mapeamentos de endpoints
     */
    @GetMapping("/mappings")
    @ResponseBody
    public Map<String, Object> debugMappings() {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            List<String> mappings = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .map(entry -> entry.getKey().toString() + " -> " + entry.getValue().getMethod().getName())
                .sorted()
                .toList();
                
            debug.put("total_mappings", mappings.size());
            debug.put("mappings", mappings);
            debug.put("lance_mappings", mappings.stream()
                .filter(mapping -> mapping.contains("lance"))
                .toList());
            debug.put("timestamp", java.time.LocalDateTime.now());
            
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            debug.put("timestamp", java.time.LocalDateTime.now());
        }
        
        return debug;
    }    @GetMapping("/images")
    @ResponseBody
    public Map<String, Object> debugProductImages() {
        Map<String, Object> debug = new HashMap<>();
          try {
            List<Product> allProducts = productRepository.findAll();
            
            debug.put("totalProducts", allProducts.size());
            debug.put("products", allProducts.stream().map(product -> {
                Map<String, Object> productInfo = new HashMap<>();
                productInfo.put("id", product.getId());
                productInfo.put("name", product.getName());
                productInfo.put("imageUrl", product.getImageUrl());
                productInfo.put("status", product.getStatus());
                productInfo.put("createdAt", product.getCreatedAt());
                return productInfo;
            }).toList());
            
            debug.put("status", "success");
            
        } catch (Exception e) {
            debug.put("status", "error");
            debug.put("message", e.getMessage());
            log.error("Erro ao debugar imagens dos produtos", e);
        }
        
        return debug;
    }
}
