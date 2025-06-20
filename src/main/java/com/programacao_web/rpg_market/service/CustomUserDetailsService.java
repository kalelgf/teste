package com.programacao_web.rpg_market.service;

import com.programacao_web.rpg_market.model.User;
import com.programacao_web.rpg_market.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
          List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority(user.getRole().toString()));
        }
        
        // Criar nossa implementação personalizada de UserDetails
        return new CustomUserDetails(
            user.getUsername(),
            user.getPassword(),
            true, true, true, true,
            authorities,
            user.getProfileImageUrl()
        );
    }
    
    // Classe interna que estende User do Spring Security
    public static class CustomUserDetails extends org.springframework.security.core.userdetails.User {
        
        private static final long serialVersionUID = 1L;
        private final String profileImageUrl;
        
        public CustomUserDetails(String username, String password, 
                              boolean enabled, boolean accountNonExpired,
                              boolean credentialsNonExpired, boolean accountNonLocked,
                              Collection<? extends GrantedAuthority> authorities,
                              String profileImageUrl) {
            super(username, password, enabled, accountNonExpired, 
                  credentialsNonExpired, accountNonLocked, authorities);
            this.profileImageUrl = profileImageUrl;
        }
        
        // Método getter para o atributo personalizado
        public String getProfileImageUrl() {
            return profileImageUrl;
        }
    }
}