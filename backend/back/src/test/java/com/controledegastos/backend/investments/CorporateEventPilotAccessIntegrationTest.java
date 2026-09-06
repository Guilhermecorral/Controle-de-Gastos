package com.controledegastos.backend.investments;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.investments.corporate-events.pilot-enabled=true",
        "app.investments.corporate-events.pilot-emails=pilot@example.com"
})
@Transactional
class CorporateEventPilotAccessIntegrationTest {
    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private User regularUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        regularUser = userRepository.save(User.builder()
                .name("Pilot blocked")
                .email("blocked@example.com")
                .password(passwordEncoder.encode("SenhaForte#2026"))
                .role(User.Role.USER)
                .build());
    }

    @Test
    void rejectsPilotPreviewWithHttp403ForAnUnlistedNonAdminUser() throws Exception {
        String accessToken = authService.login(
                new LoginRequestDTO(regularUser.getEmail(), "SenhaForte#2026", null, null),
                "127.0.0.1"
        ).session().accessToken();

        mockMvc.perform(post("/api/investments/wallet-earnings/preview")
                        .cookie(new Cookie("cg_access_token", accessToken)))
                .andExpect(status().isForbidden());
    }
}
