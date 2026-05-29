package com.controledegastos.backend.auth;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Centraliza a persistencia dos tokens temporarios de redefinicao de senha.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);

    void deleteByExpiresAtBefore(OffsetDateTime threshold);
}
