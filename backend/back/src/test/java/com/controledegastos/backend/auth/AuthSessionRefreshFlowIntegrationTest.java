package com.controledegastos.backend.auth;

import com.controledegastos.backend.auth.dto.LoginRequestDTO;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthSessionRefreshFlowIntegrationTest {
    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private AuthService authService;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        user = userRepository.save(User.builder()
                .name("Refresh flow")
                .email("refresh-flow@example.com")
                .password(passwordEncoder.encode("SenhaForte#2026"))
                .role(User.Role.USER)
                .build());
    }

    private MockMvc mockMvc;

    @Test
    void refreshesPersistedSessionWhenTheAccessCookieIsUnavailableAfterReload() throws Exception {
        AuthenticationSession initialSession = authService.login(
                new LoginRequestDTO(user.getEmail(), "SenhaForte#2026", null, null),
                "127.0.0.1"
        ).session();

        assertThat(refreshTokenRepository.findAll())
                .filteredOn(token -> token.getUser().getId().equals(user.getId()))
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.getTokenHash()).isNotBlank();
                    assertThat(token.getRevokedAt()).isNull();
                });

        // A reload without a usable access cookie must make the browser try /refresh.
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        String refreshedAccessToken = cookieValue(mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("cg_refresh_token", initialSession.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andReturn()
                .getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE), "cg_access_token");

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("cg_access_token", refreshedAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    private String cookieValue(java.util.List<String> headers, String cookieName) {
        String prefix = cookieName + "=";
        return headers.stream()
                .filter(header -> header.startsWith(prefix))
                .map(header -> header.substring(prefix.length(), header.indexOf(';')))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cookie " + cookieName + " não foi emitido"));
    }
}
