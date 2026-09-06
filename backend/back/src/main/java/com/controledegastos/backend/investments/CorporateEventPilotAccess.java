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
    private final Set<String> allowedEmails;

    public CorporateEventPilotAccess(
            @Value("${app.investments.corporate-events.pilot-enabled:false}") boolean enabled,
            @Value("${app.investments.corporate-events.pilot-emails:}") String rawEmails
    ) {
        this.enabled = enabled;
        this.allowedEmails = Arrays.stream(rawEmails.split(","))
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean canUse(User user) {
        return enabled && user != null && (user.getRole() == User.Role.ADMIN
                || allowedEmails.contains(user.getEmail().trim().toLowerCase(Locale.ROOT)));
    }

    public void require(User user) {
        if (!canUse(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A Agenda de Proventos Experimental ainda não está disponível para esta conta");
        }
    }
}
