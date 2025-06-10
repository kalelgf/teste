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
                          PasswordEncoder passwordEncoder) {
        return args -> {            System.out.println("=== Inicializando dados do MongoDB ===");
            System.out.println("Usuários existentes: " + userRepository.count());
            System.out.println("Produtos existentes: " + productRepository.count());

            if (userRepository.count() == 0) {
                System.out.println("Criando usuários iniciais...");
                // Create admin user first
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
                
                // Create test buyer user
                User buyer = new User();
                buyer.setUsername("testuser");
                buyer.setEmail("test@rpgmarket.com");
                buyer.setPassword(passwordEncoder.encode("password"));
                buyer.setCharacterClass("Guerreiro");
                buyer.setLevel(10);
                buyer.setExperience(500);
                buyer.setGoldCoins(new BigDecimal("1000"));
                buyer.setRole(UserRole.ROLE_AVENTUREIRO);                userRepository.save(buyer);
                
                System.out.println("✅ Usuários criados:");
                System.out.println("   - Admin: username='admin', password='admin'");
                System.out.println("   - Teste: username='testuser', password='password'");
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
                    product2.setSeller(admin);
                    product2.setRarity(ItemRarity.LENDARIO);
                    product2.setAuctionEndDate(LocalDateTime.now().plusDays(7));                    productRepository.save(product2);
                    
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