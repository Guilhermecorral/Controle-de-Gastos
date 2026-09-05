package com.controledegastos.backend.security;

import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceRestartTest {
    @Test
    void refreshTokenRemainsValidAfterServiceIsCreatedAgainWithTheSameEnvironmentSecret() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("jwt.secret", "uma-chave-de-jwt-fixa-com-mais-de-trinta-e-dois-caracteres")
                .withProperty("jwt.refresh-expiration", "2592000000");
        User user = User.builder().email("restart@example.com").name("Restart").role(User.Role.USER).build();

        JwtService beforeRestart = new JwtService(environment);
        beforeRestart.validateConfiguration();
        String refreshToken = beforeRestart.generateRefreshToken(user);

        JwtService afterRestart = new JwtService(environment);
        afterRestart.validateConfiguration();

        assertThat(afterRestart.isRefreshTokenValid(refreshToken, user)).isTrue();
        assertThat(afterRestart.getRefreshExpiration()).isEqualTo(2_592_000_000L);
    }
}
