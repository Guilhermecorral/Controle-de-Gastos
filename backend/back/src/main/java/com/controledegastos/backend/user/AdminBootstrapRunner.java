package com.controledegastos.backend.user;

import com.controledegastos.backend.user.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria ou promove a conta administradora inicial apenas quando o bootstrap foi ativado via ambiente.
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    @Value("${app.admin.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.bootstrap.email:}")
    private String adminEmail;

    @Value("${app.admin.bootstrap.password:}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            return;
        }

        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("Bootstrap de administrador ativado sem credenciais completas. A conta master nao foi criada.");
            return;
        }

        User adminUser = userRepository.findByEmail(adminEmail.trim())
                .map(existingUser -> {
                    existingUser.setRole(User.Role.ADMIN);
                    existingUser.setPassword(passwordEncoder.encode(adminPassword));
                    return existingUser;
                })
                .orElseGet(() -> User.builder()
                        .name("Guilherme Corral Morais")
                        .email(adminEmail.trim())
                        .password(passwordEncoder.encode(adminPassword))
                        .role(User.Role.ADMIN)
                        .build());

        userRepository.save(adminUser);
        log.info("Bootstrap da conta administradora concluido com seguranca.");
    }
}
