package com.controledegastos.backend.admin;

import com.controledegastos.backend.auth.AuthService;
import com.controledegastos.backend.auth.dto.LoginRequestDTO;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.admin.allowed-emails=admin-reset@farolfinanceiro.online")
@Transactional
class AdminResetDataSecurityIntegrationTest {

    private static final String PASSWORD = "SenhaForte#2026";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private User admin;
    private User regularUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        admin = saveUser("Admin", "admin-reset@farolfinanceiro.online", User.Role.ADMIN);
        regularUser = saveUser("Usuario", "user-reset@farolfinanceiro.online", User.Role.USER);
    }

    @Test
    void shouldAllowWhitelistedAdminToResetAnotherAccount() throws Exception {
        mockMvc.perform(post("/api/admin/users/{userId}/reset-data", regularUser.getId())
                        .cookie(accessCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(regularUser.getId()))
                .andExpect(jsonPath("$.email").value(regularUser.getEmail()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldRejectResetFromRegularUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/{userId}/reset-data", regularUser.getId())
                        .cookie(accessCookie(regularUser)))
                .andExpect(status().isForbidden());
    }

    private User saveUser(String name, String email, User.Role role) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .role(role)
                .build());
    }

    private Cookie accessCookie(User user) {
        String accessToken = authService.login(
                new LoginRequestDTO(user.getEmail(), PASSWORD, null, null),
                "127.0.0.1"
        ).session().accessToken();
        return new Cookie("cg_access_token", accessToken);
    }
}
