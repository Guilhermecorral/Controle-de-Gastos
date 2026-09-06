package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorporateEventPilotAccessTest {
    @Test
    void allowsConfiguredPilotEmailAndAdministratorsOnlyWhenEnabled() {
        CorporateEventPilotAccess access = new CorporateEventPilotAccess(true, "guilhermecorral.01@gmail.com");
        User pilot = User.builder().email("guilhermecorral.01@gmail.com").role(User.Role.USER).build();
        User admin = User.builder().email("admin@example.com").role(User.Role.ADMIN).build();
        User regular = User.builder().email("regular@example.com").role(User.Role.USER).build();

        assertThat(access.canUse(pilot)).isTrue();
        assertThat(access.canUse(admin)).isTrue();
        assertThatThrownBy(() -> access.require(regular)).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(403));
    }
}
