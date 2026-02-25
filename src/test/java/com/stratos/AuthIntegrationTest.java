package com.stratos;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stratos.payload.request.LoginRequest;
import com.stratos.payload.request.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.stratos.user.UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void shouldRegisterUser() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("reg_test_user");
        signupRequest.setEmail("reg@stratos.com");
        signupRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("User registered successfully")));
    }

    @Test
    void shouldLoginUser() throws Exception {
        // Setup: Create user directly in DB
        com.stratos.user.User user = new com.stratos.user.User();
        user.setUsername("login_test_user");
        user.setEmail("login@stratos.com");
        user.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("login_test_user");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("login_test_user"));
    }

    @Test
    void shouldFailLoginWithBadCreds() throws Exception {
        // Setup: Create user
        com.stratos.user.User user = new com.stratos.user.User();
        user.setUsername("fail_test_user");
        user.setEmail("fail@stratos.com");
        user.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("fail_test_user");
        loginRequest.setPassword("wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAccessProtectedResourceWithToken() throws Exception {
        // Setup: Create user
        com.stratos.user.User user = new com.stratos.user.User();
        user.setUsername("access_test_user");
        user.setEmail("access@stratos.com");
        user.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(user);

        // Login to get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("access_test_user");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        // Access protected resource
        mockMvc.perform(get("/api/random/protected")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // Verified: Not 401
    }

    @Test
    void shouldDetermineUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/random/protected"))
                .andExpect(status().isUnauthorized());
    }
}
