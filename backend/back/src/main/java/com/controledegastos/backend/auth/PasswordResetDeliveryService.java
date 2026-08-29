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
 * Entrega o link de redefinicao por e-mail usando a infraestrutura configurada no ambiente.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetDeliveryService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.security.password-reset.mail-from:no-reply@farolfinanceiro.local}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.host:}")
    private String mailHost;

    /**
     * Encaminha o link de redefinicao por e-mail e falha explicitamente quando o SMTP nao estiver pronto.
     */
    public void deliverResetLink(String email, String resetLink) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            throw new IllegalStateException("O envio de e-mail não está configurado no ambiente atual");
        }

        String senderAddress = resolveSenderAddress();

        try {
            mailSender.send(buildMessage(email, resetLink, senderAddress));
            log.info("E-mail de redefinição enviado com sucesso para {} via host={}", maskEmail(email), normalize(mailHost));
        } catch (MailException exception) {
            String fallbackSender = normalize(mailUsername);

            if (!fallbackSender.isBlank() && !fallbackSender.equalsIgnoreCase(senderAddress)) {
                log.warn(
                        "Falha ao enviar o e-mail de redefinição com remetente {}. Tentando novamente com o usuário SMTP.",
                        senderAddress,
                        exception
                );

                try {
                    mailSender.send(buildMessage(email, resetLink, fallbackSender));
                    log.info("E-mail de redefinição enviado com sucesso para {} usando o usuário SMTP como remetente", maskEmail(email));
                    return;
                } catch (MailException fallbackException) {
                    log.error("Não foi possível enviar o e-mail de redefinição nem com o remetente alternativo", fallbackException);
                }
            } else {
                log.error("Não foi possível enviar o e-mail de redefinição via host={}", normalize(mailHost), exception);
            }

            throw new IllegalStateException("Não foi possível enviar o e-mail de recuperação agora");
        }
    }

    private SimpleMailMessage buildMessage(String email, String resetLink, String senderAddress) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(email);
        message.setSubject("Redefinição de senha - Farol Financeiro");
        message.setText("""
                Recebemos um pedido para redefinir sua senha no Farol Financeiro.

                Use o link abaixo para concluir a troca com segurança:
                %s

                Se você não pediu essa troca, ignore este e-mail.
                """.formatted(resetLink));
        return message;
    }

    private String resolveSenderAddress() {
        String configuredSender = normalize(mailFrom);
        String smtpUsername = normalize(mailUsername);
        String smtpHost = normalize(mailHost);

        // O Gmail rejeita remetentes que não sejam a própria conta ou um alias verificado.
        if (smtpHost.equalsIgnoreCase("smtp.gmail.com") && !smtpUsername.isBlank()) {
            return smtpUsername;
        }

        if (configuredSender.isBlank() || configuredSender.endsWith("@farolfinanceiro.local")) {
            return smtpUsername.isBlank() ? "no-reply@farolfinanceiro.local" : smtpUsername;
        }

        return configuredSender;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');

        if (atIndex <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
