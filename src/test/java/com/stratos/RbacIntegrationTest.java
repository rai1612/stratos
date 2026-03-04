package com.stratos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class RbacIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldAccessPublicEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/all"))
                .andExpect(status().isOk())
                .andExpect(content().string("Public Content."));
    }

    @Test
    void shouldAccessUserEndpointAsUser() throws Exception {
        String token = registerAndLogin("rbac_user", "rbac_user@stratos.com");

        mockMvc.perform(get("/api/test/user")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("User Content."));
    }

    @Test
    void shouldDenyModEndpointAsUser() throws Exception {
        String token = registerAndLogin("rbac_user_deny", "rbac_deny@stratos.com");

        mockMvc.perform(get("/api/test/mod")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAccessModEndpointAsMod() throws Exception {
        Set<String> roles = new HashSet<>(Collections.singletonList("mod"));
        String token = registerAndLogin("rbac_mod", "rbac_mod@stratos.com", roles);

        mockMvc.perform(get("/api/test/mod")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Moderator Board."));
    }

    @Test
    void shouldAccessAdminEndpointAsAdmin() throws Exception {
        Set<String> roles = new HashSet<>(Collections.singletonList("admin"));
        String token = registerAndLogin("rbac_admin", "rbac_admin@stratos.com", roles);

        mockMvc.perform(get("/api/test/admin")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin Board."));
    }
}
