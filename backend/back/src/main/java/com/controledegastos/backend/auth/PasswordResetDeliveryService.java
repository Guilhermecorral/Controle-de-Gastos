package com.controledegastos.backend.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Entrega o link de redefinicao por email quando houver SMTP e cai para log seguro em ambiente local.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetDeliveryService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.security.password-reset.mail-from:no-reply@controledegastos.local}")
    private String mailFrom;

    /**
     * Encaminha o link de redefinicao no canal disponivel para o ambiente atual.
     */
    public boolean deliverResetLink(String email, String resetLink) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            log.info("Link de redefinicao gerado para {}: {}", email, resetLink);
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Redefinicao de senha - Controle de Gastos");
        message.setText("""
                Recebemos um pedido para redefinir sua senha.

                Use o link abaixo para concluir a troca com seguranca:
                %s

                Se voce nao pediu essa troca, ignore este email.
                """.formatted(resetLink));

        mailSender.send(message);
        return true;
    }
}
