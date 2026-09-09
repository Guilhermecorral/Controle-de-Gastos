package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Keeps an unfinished market-data integration unavailable to regular accounts. */
@Component
public class CorporateEventPilotAccess {
    private final boolean enabled;
    private final String provider;
    private final Set<String> allowedEmails;

    public CorporateEventPilotAccess(
            @Value("${app.investments.corporate-events.pilot-enabled:false}") boolean enabled,
            @Value("${app.investments.corporate-events.pilot-emails:}") String rawEmails,
            @Value("${app.investments.corporate-events.provider:mock}") String provider
    ) {
        this.enabled = enabled;
        this.provider = provider.trim().toLowerCase(Locale.ROOT);
        this.allowedEmails = Arrays.stream(rawEmails.split(","))
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean canUse(User user) {
        return isB3ProviderConfigured() && enabled && user != null && (user.getRole() == User.Role.ADMIN
                || allowedEmails.contains(user.getEmail().trim().toLowerCase(Locale.ROOT)));
    }

    public void require(User user) {
        if (!isB3ProviderConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A Agenda de Proventos Experimental requer o provedor B3 habilitado pelo ambiente");
        }
        if (!canUse(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A Agenda de Proventos Experimental ainda não está disponível para esta conta");
        }
    }

    private boolean isB3ProviderConfigured() {
        return "b3".equals(provider);
    }
}
