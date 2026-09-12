package com.controledegastos.backend.admin;

import com.controledegastos.backend.auth.AuthService;
import com.controledegastos.backend.auth.dto.LoginRequestDTO;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.TaxProfileType;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void shouldRemoveTaxObligationsButKeepTheUserTaxProfile() throws Exception {
        regularUser.setTaxProfileType(TaxProfileType.PF);
        userRepository.saveAndFlush(regularUser);
        jdbcTemplate.update("""
                INSERT INTO tax_obligations (id, user_id, name, category, due_date, estimated_amount,
                    status, document_stage, recurrence, competence_year, origin, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), regularUser.getId(), "IPVA de teste", "IPVA", LocalDate.now(),
                100, "A_PAGAR", "ESTIMATIVA", "ANUAL", LocalDate.now().getYear(), "MANUAL",
                LocalDateTime.now(), LocalDateTime.now());

        mockMvc.perform(post("/api/admin/users/{userId}/reset-data", regularUser.getId())
                        .cookie(accessCookie(admin)))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tax_obligations WHERE user_id = ?",
                Integer.class, regularUser.getId())).isZero();
        assertThat(userRepository.findById(regularUser.getId()).orElseThrow().getTaxProfileType())
                .isEqualTo(TaxProfileType.PF);
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
