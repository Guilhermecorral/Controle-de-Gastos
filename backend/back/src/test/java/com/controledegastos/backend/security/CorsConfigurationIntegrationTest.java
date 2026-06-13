package com.controledegastos.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que o preflight CORS das rotas publicas de autenticacao responda com os headers exigidos pelo frontend.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "app.security.cors.allowed-origins=https://farolfinanceiro.duckdns.org,https://project-niqqo-farolfinanceiro.vercel.app",
        "app.security.request-debug.enabled=true"
})
class CorsConfigurationIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void shouldReturnCorsHeadersForRegisterPreflightFromProductionFrontend() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/auth/register"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "https://farolfinanceiro.duckdns.org")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .build();

        HttpResponse<Void> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin").orElse(null))
                .isEqualTo("https://farolfinanceiro.duckdns.org");
        assertThat(response.headers().firstValue("Access-Control-Allow-Credentials").orElse(null))
                .isEqualTo("true");
    }
}
