package com.controledegastos.backend.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CaptchaVerificationService.class);

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

        String normalizedSecretKey = normalizeSecretKey(secretKey);

        if (normalizedSecretKey.isBlank()) {
            log.warn("Captcha habilitado sem APP_CAPTCHA_SECRET_KEY configurada para a acao={}", action);
            throw new IllegalStateException("A chave secreta do captcha precisa estar configurada");
        }

        if (captchaToken == null || captchaToken.isBlank()) {
            log.warn("Captcha ausente para a acao={} e ip={}", action, remoteIp);
            throw new IllegalArgumentException("A verificacao anti-bot e obrigatoria para continuar");
        }

        String requestBody = "secret=" + encode(normalizedSecretKey)
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

            if (response.statusCode() >= 400) {
                if (payload.errorCodes() != null && payload.errorCodes().contains("invalid-input-secret")) {
                    log.warn(
                            "APP_CAPTCHA_SECRET_KEY invalida para a acao={}. Revise a Secret Key do widget Turnstile no Render. " +
                                    "O valor do frontend deve ficar em VITE_TURNSTILE_SITE_KEY e o valor do backend em APP_CAPTCHA_SECRET_KEY.",
                            action
                    );
                }
                log.warn(
                        "Captcha respondeu HTTP {} para a acao={} com body={}",
                        response.statusCode(),
                        action,
                        abbreviate(response.body(), 300)
                );
                throw new IllegalStateException("Nao foi possivel validar o captcha no momento");
            }

            if (!payload.success()) {
                log.warn(
                        "Captcha recusado para a acao={} ip={} errorCodes={}",
                        action,
                        remoteIp,
                        payload.errorCodes()
                );
                throw new IllegalArgumentException("Nao foi possivel validar o desafio anti-bot para " + action);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Captcha interrompido para a acao={} ip={}", action, remoteIp, exception);
            throw new IllegalStateException("Nao foi possivel validar o captcha no momento");
        } catch (IOException exception) {
            log.warn("Falha de IO ao validar captcha para a acao={} ip={}", action, remoteIp, exception);
            throw new IllegalStateException("Nao foi possivel validar o captcha no momento");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizeSecretKey(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();

        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }

        return normalized;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }

    /**
     * Mapeia apenas os campos relevantes da resposta da plataforma anti-bot.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CaptchaVerifyResponse(
            boolean success,
            @JsonProperty("error-codes")
            List<String> errorCodes
    ) {
    }
}
