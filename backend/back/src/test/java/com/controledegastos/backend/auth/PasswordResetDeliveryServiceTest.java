package com.controledegastos.backend.auth;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetDeliveryServiceTest {

    @Test
    void shouldFallbackToSmtpUsernameWhenCustomMailFromIsPlaceholder() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(mailSender);

        PasswordResetDeliveryService service = new PasswordResetDeliveryService(provider);
        ReflectionTestUtils.setField(service, "mailFrom", "no-reply@farolfinanceiro.local");
        ReflectionTestUtils.setField(service, "mailUsername", "guilhermecorral.01@gmail.com");

        service.deliverResetLink("jorge@example.com", "https://farolfinanceiro.online/redefinir-senha?token=abc");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        assertEquals("guilhermecorral.01@gmail.com", messageCaptor.getValue().getFrom());
        assertEquals("jorge@example.com", messageCaptor.getValue().getTo()[0]);
    }
}
