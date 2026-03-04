package com.stratos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stratos.payload.request.UserRequest;
import com.stratos.role.ERole;
import com.stratos.role.Role;
import com.stratos.role.RoleRepository;
import com.stratos.user.User;
import com.stratos.user.UserRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testAdmin;
    private User testUser;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();

        adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow();
        userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow();

        testAdmin = new User();
        testAdmin.setUsername("admin_user");
        testAdmin.setEmail("admin@stratos.com");
        testAdmin.setPassword(passwordEncoder.encode("password123"));
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        testAdmin.setRoles(adminRoles);
        testAdmin = userRepository.save(testAdmin);

        testUser = new User();
        testUser.setUsername("normal_user");
        testUser.setEmail("user@stratos.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        testUser.setRoles(userRoles);
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "admin_user", roles = { "ADMIN" })
    void adminCanGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "normal_user", roles = { "USER" })
    void normalUserCannotGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = { "ADMIN" })
    void adminCanGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = { "ADMIN" })
    void adminCanCreateUser() throws Exception {
        UserRequest request = new UserRequest();
        request.setUsername("new_user");
        request.setEmail("new@stratos.com");
        request.setPassword("password123");
        request.setRole(Set.of("user"));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_user"));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = { "ADMIN" })
    void adminCanUpdateUser() throws Exception {
        UserRequest request = new UserRequest();
        request.setEmail("updated@stratos.com");
        request.setRole(Set.of("admin"));

        mockMvc.perform(put("/api/users/" + testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@stratos.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = { "ADMIN" })
    void adminCanDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUser.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + testUser.getId()))
                .andExpect(status().isNotFound());
    }
}
