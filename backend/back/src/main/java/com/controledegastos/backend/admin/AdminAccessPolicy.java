package com.controledegastos.backend.admin;

import com.controledegastos.backend.user.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AdminAccessPolicy {

    private static final Logger log = LoggerFactory.getLogger(AdminAccessPolicy.class);

    @Value("${app.admin.allowed-emails:}")
    private String allowedAdminEmails;

    @Value("${app.admin.bootstrap.email:}")
    private String bootstrapAdminEmail;

    @PostConstruct
    void reportConfiguration() {
        if (!isExplicitlyConfigured()) {
            log.warn("Whitelist administrativa ausente. Modo de recuperacao ativo somente para contas que ja possuem role ADMIN; novas promocoes permanecem bloqueadas.");
        }
    }

    public boolean canAccess(User user) {
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return false;
        }
        return !isExplicitlyConfigured() || contains(user.getEmail());
    }

    public boolean canPromote(String email) {
        return isExplicitlyConfigured() && contains(email);
    }

    public boolean isProtected(User user) {
        return user != null
                && user.getRole() == User.Role.ADMIN
                && (!isExplicitlyConfigured() || contains(user.getEmail()));
    }

    public boolean isExplicitlyConfigured() {
        return !configuredEmails().isEmpty();
    }

    public String accessMode() {
        return isExplicitlyConfigured() ? "WHITELIST" : "RECUPERACAO";
    }

    public Set<String> configuredEmails() {
        String allowed = allowedAdminEmails == null ? "" : allowedAdminEmails;
        String bootstrap = bootstrapAdminEmail == null ? "" : bootstrapAdminEmail;
        return Arrays.stream((allowed + "," + bootstrap).split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean contains(String email) {
        return email != null && configuredEmails().contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
