package com.programacao_web.rpg_market.config;

import com.programacao_web.rpg_market.model.*;
import com.programacao_web.rpg_market.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository, 
                          ProductRepository productRepository,
                          PasswordEncoder passwordEncoder) {        return args -> {
            System.out.println("=== Inicializando dados do MongoDB ===");
            long userCount = userRepository.count();
            long productCount = productRepository.count();
            System.out.println("Usuários existentes: " + userCount);
            System.out.println("Produtos existentes: " + productCount);
            
            // Vamos listar os usuários existentes para debug
            if (userCount > 0) {
                System.out.println("=== Usuários existentes no banco ===");
                userRepository.findAll().forEach(user -> {
                    System.out.println("Username: " + user.getUsername() + ", Email: " + user.getEmail() + ", Role: " + user.getRole());
                });
            }            // Verifica se o usuário admin existe, independente da quantidade de usuários
            if (!userRepository.existsByUsername("admin")) {
                System.out.println("Criando usuário admin...");
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@rpgmarket.com");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setCharacterClass("Mago");
                admin.setLevel(99);
                admin.setExperience(1000);
                admin.setGoldCoins(new BigDecimal("10000"));
                admin.setRole(UserRole.ROLE_MESTRE);
                userRepository.save(admin);
                
                System.out.println("✅ Usuário admin criado com sucesso!");
            } else {
                System.out.println("Usuário admin já existe no banco.");
            }
            
            if (userCount == 0) {
                System.out.println("Criando usuários iniciais adicionais...");
                
                // Create test buyer user
                User buyer = new User();
                buyer.setUsername("testuser");
                buyer.setEmail("test@rpgmarket.com");
                buyer.setPassword(passwordEncoder.encode("password"));
                buyer.setCharacterClass("Guerreiro");
                buyer.setLevel(10);
                buyer.setExperience(500);                buyer.setGoldCoins(new BigDecimal("1000"));
                buyer.setRole(UserRole.ROLE_AVENTUREIRO);
                userRepository.save(buyer);                System.out.println("✅ Usuário testuser criado:");
                System.out.println("   - Teste: username='testuser', password='password'");
            } else {
                System.out.println("Outros usuários já existem, pulando criação de usuário teste...");
            }
            
            // Verificar se o usuário admin foi salvo corretamente
            User savedAdmin = userRepository.findByUsername("admin").orElse(null);
            if (savedAdmin != null) {
                System.out.println("✅ Admin confirmado no banco: " + savedAdmin.getUsername());
                System.out.println("   Password hash: " + savedAdmin.getPassword().substring(0, Math.min(10, savedAdmin.getPassword().length())) + "...");
                System.out.println("   Role: " + savedAdmin.getRole());
            } else {
                System.out.println("❌ Erro: Admin não encontrado no banco!");
            }
            
            if (productRepository.count() == 0) {
                System.out.println("Criando produtos iniciais...");
                User admin = userRepository.findByUsername("admin").orElse(null);
                if (admin != null) {
                    // Create direct sale product
                    Product product1 = new Product();
                    product1.setName("Espada Longa Encantada");
                    product1.setDescription("Uma espada longa e afiada, forjada pelos anões das montanhas. Aumenta +10 de ataque.");
                    product1.setPrice(new BigDecimal("150.00"));
                    product1.setCategory(ProductCategory.ARMAS);
                    product1.setType(ProductType.DIRECT_SALE);
                    product1.setStatus(ProductStatus.AVAILABLE);
                    product1.setSeller(admin);
                    product1.setRarity(ItemRarity.RARO);
                    productRepository.save(product1);

                    // Create auction product
                    Product product2 = new Product();
                    product2.setName("Poção de Vida Suprema");
                    product2.setDescription("Restaura 100 pontos de vida instantaneamente. Feita com ervas raras da floresta élfica.");
                    product2.setPrice(new BigDecimal("25.00")); // Starting bid
                    product2.setBuyNowPrice(new BigDecimal("75.00"));
                    product2.setMinBidIncrement(new BigDecimal("5.00"));
                    product2.setCategory(ProductCategory.POCOES_ELIXIRES);
                    product2.setType(ProductType.AUCTION);
                    product2.setStatus(ProductStatus.AUCTION_ACTIVE);
                    product2.setSeller(admin);                    product2.setRarity(ItemRarity.LENDARIO);
                    product2.setAuctionEndDate(LocalDateTime.now().plusDays(7));
                    productRepository.save(product2);
                    
                    System.out.println("✅ Produtos criados:");
                    System.out.println("   - Espada Longa Encantada (Venda Direta): R$ 150,00");
                    System.out.println("   - Poção de Vida Suprema (Leilão): Lance inicial R$ 25,00");
                } else {
                    System.out.println("❌ Admin não encontrado para criar produtos!");
                }
            }
            
            System.out.println("=== Inicialização concluída ===");
        };
    }
}