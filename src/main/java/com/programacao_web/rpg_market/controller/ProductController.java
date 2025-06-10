package com.programacao_web.rpg_market.controller;

import com.programacao_web.rpg_market.model.*;
import com.programacao_web.rpg_market.service.FileStorageService;
import com.programacao_web.rpg_market.service.ProductService;
import com.programacao_web.rpg_market.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/item")
public class ProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserService userService;
      @Autowired
    private FileStorageService fileStorageService;
    
    // Exibe formulário para criar um novo produto com dados enriquecidos
    @GetMapping("/novo")
    public String showCreateProductForm(Model model) {
        try {
            model.addAttribute("product", new Product());
            model.addAttribute("categories", ProductCategory.values());
            model.addAttribute("rarities", ItemRarity.values());
            model.addAttribute("types", ProductType.values());
            
            // Add information about magic properties if used in template
            model.addAttribute("magicProperties", MagicProperty.values());
              return "product/create";
        } catch (Exception e) {
            throw e; // Rethrow to see the error in browser
        }
    }
    
    // Processa a criação de um novo produto com melhor validação e feedback
    @PostMapping("/novo")
    public String createProduct(
            @ModelAttribute Product product,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String[] magicProperties,
            @RequestParam(value = "directSalePrice", required = false) BigDecimal directSalePrice,
            @RequestParam(value = "startingBid", required = false) BigDecimal startingBid,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Busca o usuário atual para definir como vendedor
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Usuário não encontrado no reino.");
                return "redirect:/item/novo";
            }
            
            // Verifica o tipo de anúncio e atribui o preço adequado
            if (product.getType() == ProductType.DIRECT_SALE) {
                if (directSalePrice == null || directSalePrice.compareTo(BigDecimal.ZERO) <= 0) {
                    redirectAttributes.addFlashAttribute("error", "Por favor, informe um preço válido para venda direta.");
                    return "redirect:/item/novo";
                }
                product.setPrice(directSalePrice);
            } else if (product.getType() == ProductType.AUCTION) {
                if (startingBid == null || startingBid.compareTo(BigDecimal.ZERO) <= 0) {
                    redirectAttributes.addFlashAttribute("error", "Por favor, informe um lance inicial válido para leilão.");
                    return "redirect:/item/novo";
                }
                  // Garantir que o preço é definido para leilões
                product.setPrice(startingBid);
            }
            
            // Processa a imagem do item
            if (image != null && !image.isEmpty()) {
                String imageFilename = fileStorageService.storeFile(image);
                product.setImageUrl(imageFilename);
            }
            
            // Define propriedades mágicas se fornecidas
            if (magicProperties != null && magicProperties.length > 0) {
                for (String property : magicProperties) {
                    product.addMagicProperty(MagicProperty.valueOf(property));
                }
            }
            
            // Define o status baseado no tipo de produto
            if (product.getType() == ProductType.AUCTION) {
                product.setStatus(ProductStatus.AUCTION_ACTIVE);
                
                // Configure auction end date if not set
                if (product.getAuctionEndDate() == null) {
                    product.setAuctionEndDate(LocalDateTime.now().plusDays(7));
                }
            } else {
                product.setStatus(ProductStatus.AVAILABLE);
            }
            
            // Data de criação
            product.setCreatedAt(LocalDateTime.now());
            
            // Salva o produto
            productService.create(product, userOpt.get());
            
            redirectAttributes.addFlashAttribute("success", 
                "Seu item foi colocado à venda no mercado! Os aventureiros já podem vê-lo.");
            return "redirect:/aventureiro/inventario";
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erro ao processar a imagem do item. Os orcs devem ter interferido na transmissão.");
            return "redirect:/item/novo";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Erro ao criar item: " + e.getMessage());
            return "redirect:/item/novo";
        }
    }
      // Exibe detalhes de um produto
    @GetMapping("/{id}")
    public String showProduct(@PathVariable String id, Model model, 
                            @AuthenticationPrincipal UserDetails currentUser) {
        Optional<Product> productOpt = productService.findById(id);
        if (productOpt.isEmpty()) {
            return "error/404"; // Página não encontrada
        }
        
        Product product = productOpt.get();
        model.addAttribute("product", product);
        
        // Verifica se o produto foi vendido e se o usuário atual é o vendedor
        if (product.getStatus() == ProductStatus.SOLD || 
            product.getStatus() == ProductStatus.AUCTION_ENDED) {
            
            // Se há usuário logado, verifica se é o vendedor
            if (currentUser != null) {
                // Permite que o vendedor veja seus próprios itens vendidos
                Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
                if (userOpt.isPresent() && 
                    product.getSeller().getId().equals(userOpt.get().getId())) {
                    model.addAttribute("isOwner", true);
                }
            }
            
            // Para produtos vendidos, ainda mostra a página mas com indicação clara
            model.addAttribute("isSold", true);
        }
        
        // Se for leilão, busca os lances
        if (product.getType() == ProductType.AUCTION) {
            model.addAttribute("bids", productService.getProductBids(product));
        }
        
        return "product/details";
    }
    
    /**
     * Inicia processo de compra - redireciona para checkout
     */
    @PostMapping("/{id}/comprar")
    public String startPurchase(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            Optional<Product> productOpt = productService.findById(id);
            
            if (userOpt.isEmpty() || productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Produto ou usuário não encontrado");
                return "redirect:/item/" + id;
            }
            
            Product product = productOpt.get();
            User buyer = userOpt.get();
            
            // Verificações básicas
            if (buyer.getId().equals(product.getSeller().getId())) {
                redirectAttributes.addFlashAttribute("error", "Você não pode comprar seu próprio item");
                return "redirect:/item/" + id;
            }
            
            if (product.getType() == ProductType.DIRECT_SALE) {
                return "redirect:/checkout/comprar/" + id;
            } else if (product.getType() == ProductType.AUCTION && product.getBuyNowPrice() != null) {
                return "redirect:/checkout/comprar-agora/" + id;
            } else {
                redirectAttributes.addFlashAttribute("error", "Este item não está disponível para compra direta");
                return "redirect:/item/" + id;
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao processar compra: " + e.getMessage());
            return "redirect:/item/" + id;
        }
    }
    
    /**
     * Processa lance em leilão - redireciona para checkout
     */
    @PostMapping("/{id}/lance")
    public String placeBid(
            @PathVariable String id,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        return "redirect:/checkout/lance/" + id + "?bidAmount=" + amount;
    }
    
    // Exibe formulário de edição de produto
    @GetMapping("/{id}/editar")
    public String showEditForm(@PathVariable String id, Model model, @AuthenticationPrincipal UserDetails currentUser) {
        Optional<Product> productOpt = productService.findById(id);
        Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
        
        if (productOpt.isEmpty() || userOpt.isEmpty()) {
            return "error/404";
        }
        
        // Verifica se o produto pertence ao usuário
        if (!productOpt.get().getSeller().getId().equals(userOpt.get().getId())) {
            return "error/403"; // Acesso negado
        }
        
        model.addAttribute("product", productOpt.get());
        model.addAttribute("categories", ProductCategory.values());
        
        return "product/edit";
    }
    
    // Processa a edição de um produto
    @PostMapping("/{id}/editar")
    public String editProduct(
            @PathVariable String id,
            @ModelAttribute Product product,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
        if (userOpt.isEmpty()) {
            return "error/403";
        }
        
        try {
            productService.update(id, product, userOpt.get());
            redirectAttributes.addFlashAttribute("success", "Item atualizado com sucesso!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/item/" + id;
    }
    
    /**
     * Handles request to delete a product
     */
    @PostMapping("/{id}/excluir")
    public String deleteProduct(@PathVariable String id, 
                              @AuthenticationPrincipal UserDetails currentUser,  // Mudado de User para UserDetails
                              RedirectAttributes redirectAttributes) {
        try {
            // Primeiro, obtenha o usuário atual do UserService usando o nome de usuário do UserDetails
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Usuário não encontrado");
                return "redirect:/aventureiro/inventario";
            }
              User user = userOpt.get();
            
            Optional<Product> productOpt = productService.findById(id);
            
            if (productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Item não encontrado");
                return "redirect:/aventureiro/inventario";
            }
            
            Product product = productOpt.get();
              // Check if current user is the seller
            if (!product.getSeller().getId().equals(user.getId())) {  // Use user do userService
                redirectAttributes.addFlashAttribute("errorMessage", "Você não tem permissão para excluir este item");
                return "redirect:/item/" + id;
            }
            
            boolean result = productService.deleteProduct(product, user);  // Passa o user do userService
            
            if (result) {
                String messageType = "Anúncio removido com sucesso";                if (product.getType() == ProductType.AUCTION && product.getStatus() == ProductStatus.AUCTION_ACTIVE) {
                    messageType = "Leilão encerrado e removido com sucesso";
                }
                redirectAttributes.addFlashAttribute("successMessage", messageType);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Não foi possível remover o item");
            }
              return "redirect:/aventureiro/inventario";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao processar a exclusão: " + e.getMessage());
            return "redirect:/aventureiro/inventario";
        }
    }
}
