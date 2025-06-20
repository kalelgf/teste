package com.programacao_web.rpg_market.config;

import com.programacao_web.rpg_market.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {        
        http
            .authenticationProvider(authenticationProvider())            .authorizeHttpRequests(auth -> auth                // Páginas públicas
                .requestMatchers("/", "/mercado/**", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/error/**").permitAll()
                .requestMatchers("/login", "/authenticate").permitAll() // Explicitly permit login page and processing (GET and POST)                .requestMatchers("/debug/**").permitAll() // Debug endpoints
                .requestMatchers("/item/{id}").permitAll() // Visualização de itens é pública
                .requestMatchers("/aventureiro/registrar").permitAll()
                  // Páginas que requerem autenticação
                .requestMatchers("/item/novo").authenticated()
                .requestMatchers("/item/{id}/editar").authenticated()
                .requestMatchers("/item/{id}/comprar").authenticated()                .requestMatchers("/item/{id}/lance").authenticated()
                .requestMatchers("/lance/**").authenticated() // Endpoints de lance
                .requestMatchers("/bid/**").authenticated() // Endpoints de lance (alternativos)
                .requestMatchers("/item/{id}/excluir").authenticated() // Adicionar permissão explícita
                .requestMatchers("/checkout/**").authenticated() // Adicionar checkout
                .requestMatchers("/aventureiro/**").authenticated()
                .requestMatchers("/transacao/**").authenticated()
                
                // Páginas administrativas (apenas para role MESTRE)
                .requestMatchers("/admin/**").hasRole("MESTRE")
                .anyRequest().authenticated()            )            .csrf(csrf -> csrf
                .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/authenticate")
                .defaultSuccessUrl("/mercado", true)
                .failureUrl("/login?error")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/error/403")
            );
            
        return http.build();
    }
      @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}