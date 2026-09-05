package com.controledegastos.backend.auth;

import com.controledegastos.backend.auth.dto.LoginRequestDTO;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RefreshTokenPersistenceIntegrationTest {
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void refreshTokenIsPersistedAndRotatedInsteadOfLivingOnlyInMemory() {
        User user = userRepository.save(User.builder()
                .name("Persistent session")
                .email("persistent-session@example.com")
                .password(passwordEncoder.encode("SenhaForte#2026"))
                .role(User.Role.USER)
                .build());

        AuthenticationSession firstSession = authService.login(
                new LoginRequestDTO(user.getEmail(), "SenhaForte#2026", null, null), "127.0.0.1").session();
        RefreshToken firstStored = refreshTokenRepository.findAll().stream()
                .filter(token -> token.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow();
        assertThat(firstStored.getRevokedAt()).isNull();

        AuthenticationSession refreshedSession = authService.refreshSession(firstSession.refreshToken());

        assertThat(firstStored.getRevokedAt()).isNotNull();
        assertThat(refreshedSession.refreshToken()).isNotEqualTo(firstSession.refreshToken());
        assertThat(refreshTokenRepository.findAll()).filteredOn(token -> token.getUser().getId().equals(user.getId()))
                .hasSize(2).filteredOn(token -> token.getRevokedAt() == null).hasSize(1);
    }
}
