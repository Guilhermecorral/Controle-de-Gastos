package com.controledegastos.backend.admin;

import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAccessPolicyTest {

    @Test
    void shouldAllowOnlyExistingAdminsInRecoveryMode() {
        AdminAccessPolicy policy = policy("", "");

        assertThat(policy.canAccess(user(User.Role.ADMIN, "admin@farolfinanceiro.online"))).isTrue();
        assertThat(policy.canAccess(user(User.Role.USER, "user@farolfinanceiro.online"))).isFalse();
        assertThat(policy.canPromote("admin@farolfinanceiro.online")).isFalse();
        assertThat(policy.accessMode()).isEqualTo("RECUPERACAO");
    }

    @Test
    void shouldEnforceConfiguredWhitelist() {
        AdminAccessPolicy policy = policy("admin@farolfinanceiro.online", "bootstrap@farolfinanceiro.online");

        assertThat(policy.canAccess(user(User.Role.ADMIN, "ADMIN@farolfinanceiro.online"))).isTrue();
        assertThat(policy.canAccess(user(User.Role.ADMIN, "intruso@farolfinanceiro.online"))).isFalse();
        assertThat(policy.canPromote("bootstrap@farolfinanceiro.online")).isTrue();
        assertThat(policy.accessMode()).isEqualTo("WHITELIST");
    }

    private AdminAccessPolicy policy(String allowed, String bootstrap) {
        AdminAccessPolicy policy = new AdminAccessPolicy();
        ReflectionTestUtils.setField(policy, "allowedAdminEmails", allowed);
        ReflectionTestUtils.setField(policy, "bootstrapAdminEmail", bootstrap);
        return policy;
    }

    private User user(User.Role role, String email) {
        return User.builder().id(1L).name("Teste").email(email).password("encoded").role(role).active(true).build();
    }
}
