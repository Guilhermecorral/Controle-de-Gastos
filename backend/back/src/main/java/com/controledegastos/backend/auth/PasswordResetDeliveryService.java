package com.controledegastos.backend.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Entrega o link de redefinição por e-mail usando a infraestrutura configurada no ambiente.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetDeliveryService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.security.password-reset.mail-from:no-reply@farolfinanceiro.local}")
    private String mailFrom;

    /**
     * Encaminha o link de redefinição por e-mail e falha explicitamente quando o SMTP não estiver pronto.
     */
    public void deliverResetLink(String email, String resetLink) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            throw new IllegalStateException("O envio de e-mail não está configurado no ambiente atual");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Redefinição de senha - Farol Financeiro");
        message.setText("""
                Recebemos um pedido para redefinir sua senha no Farol Financeiro.

                Use o link abaixo para concluir a troca com segurança:
                %s

                Se você não pediu essa troca, ignore este e-mail.
                """.formatted(resetLink));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Não foi possível enviar o e-mail de redefinição para {}", email, exception);
            throw new IllegalStateException("Não foi possível enviar o e-mail de recuperação agora");
        }
    }
}
