package com.programacao_web.rpg_market.controller;

import com.programacao_web.rpg_market.model.User;
import com.programacao_web.rpg_market.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               Model model) {
        log.info("GET /login requested");
        if (error != null) {
            model.addAttribute("error", "Credenciais inválidas!");
        }
        if (logout != null) {
            model.addAttribute("message", "Logout realizado com sucesso!");
        }
        return "user/login";    }
    
    /*
    // Custom authentication method - not needed when using Spring Security form login
    @PostMapping("/authenticate")
    public String processAuthentication(@RequestParam String username,
                                       @RequestParam String password,
                                       HttpServletRequest request,
                                       jakarta.servlet.http.HttpSession session,
                                       Model model) {
        log.info("POST /authenticate requested for user: {}", username);
        try {
            // Verificar se o usuário existe no banco
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("User found in database: {}", user.getUsername());
                log.info("User password hash (first 10 chars): {}...", user.getPassword().substring(0, Math.min(10, user.getPassword().length())));
                log.info("User role: {}", user.getRole());
                
                // Testar se a senha fornecida bate com o hash
                boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
                log.info("Password matches: {}", passwordMatches);
                
                if (!passwordMatches) {
                    log.error("Password does not match for user: {}", username);
                    model.addAttribute("error", "Senha incorreta!");
                    return "user/login";
                }
            } else {
                log.error("User not found in database: {}", username);
                model.addAttribute("error", "Usuário não encontrado!");
                return "user/login";
            }
            
            // Create authentication token
            UsernamePasswordAuthenticationToken authRequest = 
                new UsernamePasswordAuthenticationToken(username, password);
            
            // Add request details
            authRequest.setDetails(new WebAuthenticationDetails(request));
            
            log.info("Attempting authentication for user: {}", username);
            
            // Authenticate
            Authentication authResult = authenticationManager.authenticate(authRequest);
              log.info("Authentication successful for user: {}", username);
            
            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authResult);
            
            // Store authentication in session
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            log.info("Authentication stored in session for user: {}", username);
            
            // Redirect to market page
            return "redirect:/mercado";
            
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", username, e);
            model.addAttribute("error", "Credenciais inválidas: " + e.getMessage());
            return "user/login";
        }
    }
    */
}