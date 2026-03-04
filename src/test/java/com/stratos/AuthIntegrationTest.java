package com.stratos;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stratos.payload.request.LoginRequest;
import com.stratos.payload.request.SignupRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private com.stratos.user.UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // ========================================================================
    // Happy path
    // ========================================================================

    @Nested
    class HappyPath {

        @Test
        void shouldRegisterUser() throws Exception {
            SignupRequest request = new SignupRequest();
            request.setUsername("reg_user");
            request.setEmail("reg@stratos.com");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("User registered successfully")));
        }

        @Test
        void shouldLoginUser() throws Exception {
            com.stratos.user.User user = new com.stratos.user.User();
            user.setUsername("login_user");
            user.setEmail("login@stratos.com");
            user.setPassword(passwordEncoder.encode("password123"));
            userRepository.save(user);

            LoginRequest request = new LoginRequest();
            request.setUsername("login_user");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.username").value("login_user"));
        }

        @Test
        void shouldAccessProtectedResourceWithToken() throws Exception {
            String token = registerAndLogin("access_user", "access@stratos.com");

            mockMvc.perform(get("/api/random/protected")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound()); // Not 401 — proves auth worked
        }
    }

    // ========================================================================
    // Edge cases
    // ========================================================================

    @Nested
    class EdgeCases {

        @Test
        void shouldRejectDuplicateUsername() throws Exception {
            // Register first user
            registerAndLogin("dup_user", "first@stratos.com");

            // Try to register with same username
            SignupRequest request = new SignupRequest();
            request.setUsername("dup_user");
            request.setEmail("second@stratos.com");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Username is already taken")));
        }

        @Test
        void shouldRejectDuplicateEmail() throws Exception {
            registerAndLogin("email_user1", "dup@stratos.com");

            SignupRequest request = new SignupRequest();
            request.setUsername("email_user2");
            request.setEmail("dup@stratos.com");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Email is already in use")));
        }

        @Test
        void shouldFailLoginWithWrongPassword() throws Exception {
            registerAndLogin("wrong_pw_user", "wrong@stratos.com");

            LoginRequest request = new LoginRequest();
            request.setUsername("wrong_pw_user");
            request.setPassword("wrong_password");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectAccessWithoutToken() throws Exception {
            mockMvc.perform(get("/api/workspaces"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectMalformedToken() throws Exception {
            mockMvc.perform(get("/api/workspaces")
                    .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectBlankUsername() throws Exception {
            SignupRequest request = new SignupRequest();
            request.setUsername("");
            request.setEmail("blank@stratos.com");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectTooShortUsername() throws Exception {
            SignupRequest request = new SignupRequest();
            request.setUsername("ab"); // min 3
            request.setEmail("short@stratos.com");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectInvalidEmail() throws Exception {
            SignupRequest request = new SignupRequest();
            request.setUsername("invalid_email");
            request.setEmail("not-an-email");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectTooShortPassword() throws Exception {
            SignupRequest request = new SignupRequest();
            request.setUsername("short_pw");
            request.setEmail("shortpw@stratos.com");
            request.setPassword("12345"); // min 6

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
