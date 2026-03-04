package com.stratos.unit.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.UserRequest;
import com.stratos.payload.response.UserResponse;
import com.stratos.role.ERole;
import com.stratos.role.Role;
import com.stratos.role.RoleRepository;
import com.stratos.user.User;
import com.stratos.user.UserRepository;
import com.stratos.user.UserServiceImpl;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private Role userRole;
    private Role adminRole;
    private Role managerRole;

    @BeforeEach
    void setUp() {
        userRole = new Role(1, ERole.ROLE_USER);
        adminRole = new Role(2, ERole.ROLE_ADMIN);
        managerRole = new Role(3, ERole.ROLE_MANAGER);

        user = new User("testuser", "test@stratos.com", "hashed_password");
        user.setId(1L);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
    }

    // ========================================================================
    // getAllUsers
    // ========================================================================

    @Nested
    class GetAllUsers {

        @Test
        void shouldReturnMappedList() {
            User user2 = new User("user2", "user2@stratos.com", "hashed");
            user2.setId(2L);
            user2.setRoles(Set.of(adminRole));

            when(userRepository.findAll()).thenReturn(List.of(user, user2));

            List<UserResponse> responses = userService.getAllUsers();

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getUsername()).isEqualTo("testuser");
            assertThat(responses.get(1).getUsername()).isEqualTo("user2");
        }

        @Test
        void shouldReturnEmptyList() {
            when(userRepository.findAll()).thenReturn(Collections.emptyList());

            List<UserResponse> responses = userService.getAllUsers();

            assertThat(responses).isEmpty();
        }
    }

    // ========================================================================
    // getUserById
    // ========================================================================

    @Nested
    class GetUserById {

        @Test
        void shouldReturnMappedResponse() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserById(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@stratos.com");
            assertThat(response.getRoles()).containsExactly("ROLE_USER");
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // createUser
    // ========================================================================

    @Nested
    class CreateUser {

        @Test
        void shouldCreateUserWithDefaultRole() {
            UserRequest request = new UserRequest();
            request.setUsername("newuser");
            request.setEmail("new@stratos.com");
            request.setPassword("password123");
            // role is null — defaults to USER

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@stratos.com")).thenReturn(false);
            when(encoder.encode("password123")).thenReturn("encoded");
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            UserResponse response = userService.createUser(request);

            assertThat(response.getUsername()).isEqualTo("newuser");
            assertThat(response.getRoles()).containsExactly("ROLE_USER");
        }

        @Test
        void shouldResolveAdminRole() {
            UserRequest request = new UserRequest();
            request.setUsername("admin");
            request.setEmail("admin@stratos.com");
            request.setPassword("password");
            request.setRole(Set.of("admin"));

            when(userRepository.existsByUsername("admin")).thenReturn(false);
            when(userRepository.existsByEmail("admin@stratos.com")).thenReturn(false);
            when(encoder.encode(anyString())).thenReturn("encoded");
            when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User saved = i.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            UserResponse response = userService.createUser(request);

            assertThat(response.getRoles()).containsExactly("ROLE_ADMIN");
        }

        @Test
        void shouldResolveManagerRole() {
            UserRequest request = new UserRequest();
            request.setUsername("manager");
            request.setEmail("mgr@stratos.com");
            request.setPassword("password");
            request.setRole(Set.of("mod"));

            when(userRepository.existsByUsername("manager")).thenReturn(false);
            when(userRepository.existsByEmail("mgr@stratos.com")).thenReturn(false);
            when(encoder.encode(anyString())).thenReturn("encoded");
            when(roleRepository.findByName(ERole.ROLE_MANAGER)).thenReturn(Optional.of(managerRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User saved = i.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            UserResponse response = userService.createUser(request);

            assertThat(response.getRoles()).containsExactly("ROLE_MANAGER");
        }

        @Test
        void shouldThrowForDuplicateUsername() {
            UserRequest request = new UserRequest();
            request.setUsername("testuser");
            request.setEmail("unique@stratos.com");
            request.setPassword("password");

            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username is already taken");
        }

        @Test
        void shouldThrowForDuplicateEmail() {
            UserRequest request = new UserRequest();
            request.setUsername("unique");
            request.setEmail("test@stratos.com");
            request.setPassword("password");

            when(userRepository.existsByUsername("unique")).thenReturn(false);
            when(userRepository.existsByEmail("test@stratos.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email is already in use");
        }
    }

    // ========================================================================
    // updateUser
    // ========================================================================

    @Nested
    class UpdateUser {

        @Test
        void shouldUpdateEmailOnly() {
            UserRequest request = new UserRequest();
            request.setEmail("new@stratos.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("new@stratos.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = userService.updateUser(1L, request);

            assertThat(response.getEmail()).isEqualTo("new@stratos.com");
        }

        @Test
        void shouldSkipEmailCheckIfUnchanged() {
            UserRequest request = new UserRequest();
            request.setEmail("test@stratos.com"); // Same as current email

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            // existsByEmail should NOT be called since email is unchanged
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = userService.updateUser(1L, request);

            assertThat(response.getEmail()).isEqualTo("test@stratos.com");
        }

        @Test
        void shouldUpdatePasswordOnly() {
            UserRequest request = new UserRequest();
            request.setPassword("newpassword");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(encoder.encode("newpassword")).thenReturn("new_hashed");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            userService.updateUser(1L, request);

            assertThat(user.getPassword()).isEqualTo("new_hashed");
        }

        @Test
        void shouldUpdateRolesOnly() {
            UserRequest request = new UserRequest();
            request.setRole(Set.of("admin"));

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = userService.updateUser(1L, request);

            assertThat(response.getRoles()).containsExactly("ROLE_ADMIN");
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            UserRequest request = new UserRequest();
            request.setEmail("new@stratos.com");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldThrowForDuplicateEmailOnUpdate() {
            UserRequest request = new UserRequest();
            request.setEmail("taken@stratos.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("taken@stratos.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email is already in use");
        }
    }

    // ========================================================================
    // deleteUser
    // ========================================================================

    @Nested
    class DeleteUser {

        @Test
        void shouldDeleteUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            userService.deleteUser(1L);

            verify(userRepository).delete(user);
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
