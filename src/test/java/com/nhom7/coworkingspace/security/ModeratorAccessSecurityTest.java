package com.nhom7.coworkingspace.security;

import com.nhom7.coworkingspace.config.SecurityConfig;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ModeratorAccessSecurityTest.ModeratorProbeController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthErrorHandler.class,
        ModeratorAccessSecurityTest.ModeratorProbeController.class
})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:8081")
class ModeratorAccessSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void adminCanAccessModeratorApiAndPage() throws Exception {
        assertAllowedFor("ADMIN");
    }

    @Test
    void moderatorCanAccessModeratorApiAndPage() throws Exception {
        assertAllowedFor("MODERATOR");
    }

    @Test
    void regularUserCannotAccessModeratorApiOrPage() throws Exception {
        authenticateTokenAs("user-token", "USER");

        mockMvc.perform(get("/api/moderator/probe")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/moderator/probe")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserCannotAccessModeratorApiOrPage() throws Exception {
        mockMvc.perform(get("/api/moderator/probe"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/moderator/probe"))
                .andExpect(status().is3xxRedirection());
    }

    private void assertAllowedFor(String role) throws Exception {
        String token = role.toLowerCase() + "-token";
        authenticateTokenAs(token, role);

        mockMvc.perform(get("/api/moderator/probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("allowed"));

        mockMvc.perform(get("/moderator/probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("allowed"));
    }

    private void authenticateTokenAs(String token, String role) {
        String username = role.toLowerCase();
        Date issuedAt = new Date();

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(token)).thenReturn(true);
        when(jwtTokenProvider.extractUsername(token)).thenReturn(username);
        when(jwtTokenProvider.extractIssuedAt(token)).thenReturn(issuedAt);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(tokenBlacklistService.isUserTokenRevoked(username, issuedAt)).thenReturn(false);
        when(customUserDetailsService.loadUserByUsername(username)).thenReturn(
                org.springframework.security.core.userdetails.User
                        .withUsername(username)
                        .password("not-used")
                        .roles(role)
                        .build());
    }

    @RestController
    @ModeratorOrAdmin
    static class ModeratorProbeController {

        @GetMapping({"/api/moderator/probe", "/moderator/probe"})
        String probe() {
            return "allowed";
        }
    }
}
