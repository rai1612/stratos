package com.stratos.unit.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.stratos.role.ERole;
import com.stratos.role.Role;
import com.stratos.security.services.UserDetailsImpl;
import com.stratos.security.services.UserDetailsServiceImpl;
import com.stratos.user.User;
import com.stratos.user.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Nested
    class LoadUserByUsername {

        @Test
        void shouldReturnUserDetailsWithAuthorities() {
            Role userRole = new Role(1, ERole.ROLE_USER);
            Role adminRole = new Role(2, ERole.ROLE_ADMIN);

            User user = new User("testuser", "test@stratos.com", "hashed");
            user.setId(1L);
            user.setRoles(Set.of(userRole, adminRole));

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            UserDetails details = userDetailsService.loadUserByUsername("testuser");

            assertThat(details).isInstanceOf(UserDetailsImpl.class);
            assertThat(details.getUsername()).isEqualTo("testuser");
            assertThat(details.getPassword()).isEqualTo("hashed");
            assertThat(details.getAuthorities()).hasSize(2);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("nonexistent");
        }
    }
}
