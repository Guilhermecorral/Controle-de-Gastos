package com.controledegastos.backend.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Valida desafios anti-bot no servidor para impedir que o frontend seja a unica barreira.
 */
@Service
public class CaptchaVerificationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${app.security.captcha.enabled:false}")
    private boolean enabled;

    @Value("${app.security.captcha.secret-key:}")
    private String secretKey;

    @Value("${app.security.captcha.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String verifyUrl;

    /**
     * Exige captcha valido quando a feature estiver habilitada e falha com mensagem curta quando nao passar.
     */
    public void assertValid(String captchaToken, String remoteIp, String action) {
        if (!enabled) {
            return;
        }

        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("A chave secreta do captcha precisa estar configurada");
        }

        if (captchaToken == null || captchaToken.isBlank()) {
            throw new IllegalArgumentException("A verificacao anti-bot e obrigatoria para continuar");
        }

        String requestBody = "secret=" + encode(secretKey)
                + "&response=" + encode(captchaToken)
                + "&remoteip=" + encode(remoteIp == null ? "" : remoteIp);

        HttpRequest request = HttpRequest.newBuilder(URI.create(verifyUrl))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            CaptchaVerifyResponse payload = objectMapper.readValue(response.body(), CaptchaVerifyResponse.class);

            if (!payload.success()) {
                throw new IllegalArgumentException("Nao foi possivel validar o desafio anti-bot para " + action);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Nao foi possivel validar o captcha no momento");
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel validar o captcha no momento");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Mapeia apenas os campos relevantes da resposta da plataforma anti-bot.
     */
    private record CaptchaVerifyResponse(
            boolean success,
            @JsonProperty("error-codes")
            List<String> errorCodes
    ) {
    }
}
