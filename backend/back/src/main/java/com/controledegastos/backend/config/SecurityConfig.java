package com.controledegastos.backend.config;

import com.controledegastos.backend.security.AuthRateLimitFilter;
import com.controledegastos.backend.security.ApiCorsFilter;
import com.controledegastos.backend.security.SecurityRequestDebugFilter;
import com.controledegastos.backend.security.CorsHeaderService;
import com.controledegastos.backend.security.JwtAuthenticationFilter;
import com.controledegastos.backend.security.TrustedOriginFilter;
import com.controledegastos.backend.security.AllowedOriginPatterns;
import com.controledegastos.backend.user.Repository.UserRepository;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Centraliza a configuracao de seguranca, JWT e Swagger da aplicacao.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    @Value("${app.security.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173,https://farolfinanceiro.online,https://www.farolfinanceiro.online}")
    private String allowedOrigins;

    private final AuthRateLimitFilter authRateLimitFilter;
    private final ApiCorsFilter apiCorsFilter;
    private final SecurityRequestDebugFilter securityRequestDebugFilter;
    private final CorsHeaderService corsHeaderService;
    private final TrustedOriginFilter trustedOriginFilter;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserRepository userRepository;

    /**
     * Define as rotas publicas, habilita o filtro JWT e mantem a API sem sessao de servidor.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/me",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password/redirect",
                                "/api/auth/reset-password",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(apiCorsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityRequestDebugFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(trustedOriginFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Restringe quais origens do frontend podem conversar com a API em ambientes reais.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> configuredOrigins = AllowedOriginPatterns.expand(allowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(configuredOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Origin", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Registra no boot quais origens de frontend ficaram liberadas para acelerar diagnosticos em producao.
     */
    @Bean
    public org.springframework.boot.CommandLineRunner corsOriginsStartupLog() {
        return args -> log.info(
                "[SECURITY] CORS allowed origins configured={} expanded={}",
                allowedOrigins,
                AllowedOriginPatterns.expand(allowedOrigins)
        );
    }

    private static String normalizeOriginValue(String origin) {
        return AllowedOriginPatterns.normalize(origin);
    }

    /**
     * Disponibiliza o encoder usado para armazenar senhas com hash BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Expoe o AuthenticationManager configurado pelo Spring Security.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Ajusta a documentacao OpenAPI para indicar o uso de autenticacao Bearer JWT.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("Farol Financeiro API")
                        .version("1.0.0")
                        .description("API REST para controle financeiro pessoal"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }

    /**
     * Carrega usuarios pelo email, que funciona como username da aplicacao.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + username));
    }

    /**
     * Padroniza a resposta 401 para requisicoes nao autenticadas.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
                corsHeaderService.applyIfAllowed(request, response);
                log.warn(
                        "[SECURITY] 401 method={} uri={} origin={} message={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getHeader("Origin"),
                        authException.getMessage()
                );
                response.sendError(401, "Autenticacao necessaria para acessar este recurso");
        };
    }

    /**
     * Padroniza a resposta 403 para usuarios autenticados sem permissao.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
                corsHeaderService.applyIfAllowed(request, response);
                log.warn(
                        "[SECURITY] 403 method={} uri={} origin={} message={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getHeader("Origin"),
                        accessDeniedException.getMessage()
                );
                response.sendError(403, "Voce nao tem permissao para acessar este recurso");
        };
    }
}
