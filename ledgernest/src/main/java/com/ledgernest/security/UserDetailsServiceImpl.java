package com.ledgernest.security;

import com.ledgernest.user.User;
import com.ledgernest.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service    
@RequiredArgsConstructor

public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring Security calls this method — passing in whoever's "username" is
    // In our system, email IS the username
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Find user in DB by email
        // If not found → throw UsernameNotFoundException
        // Spring Security catches this and returns 401 automatically
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));

        // Convert your Role enum → Spring Security's GrantedAuthority
        // "ROLE_" prefix is mandatory — Spring's @PreAuthorize("hasRole('ADMIN')")
        // strips this prefix when checking, so ROLE_ADMIN matches hasRole('ADMIN')
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        // Return Spring's own User object (NOT your entity)
        // Spring uses this to verify the password and check if account is active
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),  // enabled — false means login is rejected
                true,             // accountNonExpired
                true,             // credentialsNonExpired
                true,             // accountNonLocked
                List.of(authority)
        );
    }
}

