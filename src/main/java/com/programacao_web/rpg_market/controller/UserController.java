package com.programacao_web.rpg_market.controller;

import com.programacao_web.rpg_market.model.Product;
import com.programacao_web.rpg_market.model.User;
import com.programacao_web.rpg_market.model.DeliveryAddress;
import com.programacao_web.rpg_market.service.ProductService;
import com.programacao_web.rpg_market.service.UserService;
import com.programacao_web.rpg_market.service.DeliveryAddressService;
import com.programacao_web.rpg_market.dto.PasswordChangeRequest;
import com.programacao_web.rpg_market.service.FileStorageService;
import com.programacao_web.rpg_market.service.CustomUserDetailsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/aventureiro") // Prefixo RPG para todas as rotas de usuário
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private FileStorageService fileStorageService;
      @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private DeliveryAddressService deliveryAddressService;
    
    // Adicionar este método na classe UserController
    private void refreshAuthentication(User user) {
        UserDetails updatedUserDetails = customUserDetailsService.loadUserByUsername(user.getUsername());
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
            updatedUserDetails, null, updatedUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }
    
    // Exibe formulário de registro
    @GetMapping("/registrar")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "user/register"; // Caminho para o template Thymeleaf
    }
    
    // Processa o registro de novos usuários
    @PostMapping("/registrar")
    public String registerUser(
            @ModelAttribute User user, 
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            RedirectAttributes redirectAttributes) {
        try {
            // Processar upload de imagem se fornecido
            if (profileImage != null && !profileImage.isEmpty()) {
                String imageFilename = fileStorageService.storeAndResizeImage(profileImage, 200, 200);
                user.setProfileImageUrl(imageFilename);
            }
            
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("success", "Conta criada com sucesso!");            return "redirect:/login";
        } catch (Exception e) {
            // Provide a user-friendly error message
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Username já existe")) {
                errorMessage = "Este nome de aventureiro já está em uso. Por favor, escolha outro.";
            } else if (errorMessage.contains("Email já está em uso")) {
                errorMessage = "Este email já está cadastrado. Tente fazer login ou recuperar sua senha.";
            } else {
                errorMessage = "Ocorreu um erro ao criar sua conta. Por favor, tente novamente.";
            }
            
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/aventureiro/registrar";
        }
    }
    
    // Exibe o perfil do usuário logado
    @GetMapping("/perfil")
    public String showProfile(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
        if (userOpt.isEmpty()) {
            return "error/403";
        }
        
        User user = userOpt.get();
        // Adicione todos os atributos necessários
        model.addAttribute("user", user);
        model.addAttribute("activeItems", productService.findByUser(user)); // Ou outra lógica
        model.addAttribute("soldItems", productService.findSoldByUser(user)); // Ou outra lógica
        model.addAttribute("userPurchases", userService.getUserPurchases(user)); // Ou outra lógica
        
        return "user/profile"; // Retorno direto para a página, sem usar "content"
    }
    
    // Exibe o inventário (produtos) do usuário
    @GetMapping("/inventario")
    public String showInventory(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
        if (userOpt.isEmpty()) {
            return "error/403";
        }
        
        User user = userOpt.get();
        List<Product> products = productService.findByUser(user);
        model.addAttribute("products", products);
        
        // Retorne diretamente para a página inventory
        return "user/inventory";
    }
      // Exibe as compras realizadas pelo usuário
    @GetMapping("/compras")
    public String showUserPurchases(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        userService.findByUsername(currentUser.getUsername()).ifPresent(user -> {
            model.addAttribute("transactions", userService.getUserPurchases(user));
        });
        return "user/purchases";
    }
    
    // Exibe as vendas realizadas pelo usuário
    @GetMapping("/vendas")
    public String showUserSales(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        userService.findByUsername(currentUser.getUsername()).ifPresent(user -> {
            model.addAttribute("transactions", userService.getUserSales(user));
        });
        return "user/sales";
    }
    
    // Exibe formulário para alterar senha
    @GetMapping("/senha")
    public String showChangePasswordForm(Model model) {
        model.addAttribute("passwordRequest", new PasswordChangeRequest());
        return "user/change-password"; // Return direct template path
    }
    
    // Processa a alteração de senha
    @PostMapping("/senha")
    public String changePassword(
            @ModelAttribute PasswordChangeRequest passwordRequest, 
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                return "error/403";
            }
            
            userService.changePassword(userOpt.get(), 
                    passwordRequest.getCurrentPassword(), 
                    passwordRequest.getNewPassword(), 
                    passwordRequest.getConfirmPassword());
            
            redirectAttributes.addFlashAttribute("success", "Senha alterada com sucesso!");
            return "redirect:/aventureiro/perfil"; // Only redirect to profile on success
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // Return to the change password form on error
            return "redirect:/aventureiro/senha";
        }
    }
    
    // Exibe formulário para editar perfil
    @GetMapping("/editar-perfil")
    public String showEditProfileForm(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
        if (userOpt.isEmpty()) {
            return "error/403";
        }
        
        model.addAttribute("user", userOpt.get());
        return "user/edit-profile"; // Return direct template path
    }
    
    // Processa a edição do perfil
    @PostMapping("/editar-perfil")
    public String updateProfile(
            @ModelAttribute User updatedUser,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "croppedImage", required = false) String croppedImageData,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                return "error/403";
            }
            
            User user = userOpt.get();
            
            // Processar o upload da imagem do perfil
            if (croppedImageData != null && !croppedImageData.isEmpty()) {
                // Processar a imagem recortada do data URL
                String imageFilename = fileStorageService.storeBase64Image(croppedImageData, "profile");
                
                // Remover imagem anterior se existir
                if (user.getProfileImageUrl() != null) {
                    fileStorageService.deleteFile(user.getProfileImageUrl());
                }
                
                user.setProfileImageUrl(imageFilename);
            } else if (profileImage != null && !profileImage.isEmpty()) {
                // Caso não tenha recortado, apenas redimensionar e salvar
                String imageFilename = fileStorageService.storeAndResizeImage(profileImage, 200, 200);
                
                // Remover imagem anterior se existir
                if (user.getProfileImageUrl() != null) {
                    fileStorageService.deleteFile(user.getProfileImageUrl());
                }
                
                user.setProfileImageUrl(imageFilename);
            }
            
            // Atualiza os outros campos
            userService.updateProfile(user, updatedUser);
            
            // Atualiza a autenticação para refletir as mudanças no perfil
            refreshAuthentication(user);
            
            redirectAttributes.addFlashAttribute("success", "Perfil atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
          return "redirect:/aventureiro/perfil";
    }
    
    // === ADDRESS MANAGEMENT ENDPOINTS ===
      // Display user addresses
    @GetMapping("/enderecos")
    public String showAddresses(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
        if (userOpt.isEmpty()) {
            return "error/403";
        }
        
        User user = userOpt.get();
        List<DeliveryAddress> addresses = deliveryAddressService.findByUserId(user.getId());
        model.addAttribute("addresses", addresses);
        model.addAttribute("user", user);
        
        return "user/addresses";
    }
    
    // Show form to add new address
    @GetMapping("/enderecos/novo")
    public String showAddAddressForm(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
        if (userOpt.isEmpty()) {
            return "error/403";
        }
        
        model.addAttribute("address", new DeliveryAddress());
        model.addAttribute("isEdit", false);
        
        return "user/address-form";
    }
    
    // Process new address creation
    @PostMapping("/enderecos/novo")
    public String createAddress(
            @ModelAttribute DeliveryAddress address,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
          try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                return "error/403";
            }
            
            User user = userOpt.get();
            address.setUserId(user.getId());
            
            // If this is the user's first address, make it default
            if (deliveryAddressService.countByUserId(user.getId()) == 0) {
                address.setIsDefault(true);
            }
            
            deliveryAddressService.save(address);
            redirectAttributes.addFlashAttribute("success", "Endereço adicionado com sucesso!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao salvar endereço: " + e.getMessage());
        }
        
        return "redirect:/aventureiro/enderecos";
    }
    
    // Show form to edit existing address
    @GetMapping("/enderecos/{addressId}/editar")
    public String showEditAddressForm(
            @PathVariable String addressId,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model) {
        
        Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
        if (userOpt.isEmpty()) {
            return "error/403";
        }
        
        User user = userOpt.get();
        Optional<DeliveryAddress> addressOpt = deliveryAddressService.findByIdAndUserId(addressId, user.getId());
        
        if (addressOpt.isEmpty()) {
            return "error/404";
        }
        
        model.addAttribute("address", addressOpt.get());
        model.addAttribute("isEdit", true);
        
        return "user/address-form";
    }
    
    // Process address update
    @PostMapping("/enderecos/{addressId}/editar")
    public String updateAddress(
            @PathVariable String addressId,
            @ModelAttribute DeliveryAddress updatedAddress,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
          try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {
                return "error/403";
            }
            
            User user = userOpt.get();
            deliveryAddressService.update(addressId, updatedAddress);
            redirectAttributes.addFlashAttribute("success", "Endereço atualizado com sucesso!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao atualizar endereço: " + e.getMessage());
        }
        
        return "redirect:/aventureiro/enderecos";
    }
    
    // Delete address
    @PostMapping("/enderecos/{addressId}/deletar")
    public String deleteAddress(
            @PathVariable String addressId,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {            return "error/403";
            }
            
            User user = userOpt.get();
            boolean deleted = deliveryAddressService.deleteByIdAndUserId(addressId, user.getId());
            
            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Endereço removido com sucesso!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Endereço não encontrado.");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao remover endereço: " + e.getMessage());
        }
        
        return "redirect:/aventureiro/enderecos";
    }
    
    // Set address as default
    @PostMapping("/enderecos/{addressId}/padrao")
    public String setAsDefaultAddress(
            @PathVariable String addressId,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<User> userOpt = userService.findByUsername(currentUser.getUsername());
            if (userOpt.isEmpty()) {            return "error/403";
            }
            
            User user = userOpt.get();
            deliveryAddressService.setAsDefault(addressId, user.getId());
            redirectAttributes.addFlashAttribute("success", "Endereço definido como padrão!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao definir endereço padrão: " + e.getMessage());
        }
        
        return "redirect:/aventureiro/enderecos";
    }
}
