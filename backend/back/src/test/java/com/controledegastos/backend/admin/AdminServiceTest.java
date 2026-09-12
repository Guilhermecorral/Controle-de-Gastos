package com.controledegastos.backend.admin;

import com.controledegastos.backend.admin.dto.AdminOverviewResponseDTO;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminAccessPolicy adminAccessPolicy;

    @Mock
    private UserFinancialDataResetService financialDataResetService;

    @InjectMocks
    private AdminService adminService;

    @Test
    void shouldUseBootstrapEmailAsSecureWhitelistFallback() {
        User currentAdmin = adminUser("ADMIN@farolfinanceiro.online");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(currentAdmin);
        when(adminAccessPolicy.canAccess(currentAdmin)).thenReturn(true);
        when(adminAccessPolicy.configuredEmails()).thenReturn(java.util.Set.of("admin@farolfinanceiro.online"));
        when(adminAccessPolicy.isExplicitlyConfigured()).thenReturn(true);
        when(adminAccessPolicy.accessMode()).thenReturn("WHITELIST");
        when(userRepository.count()).thenReturn(2L);
        when(userRepository.countByActiveTrue()).thenReturn(2L);
        when(userRepository.countByRole(User.Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByTwoFactorEnabledTrue()).thenReturn(1L);
        when(transactionRepository.sumAmountByType(Transaction.TransactionType.RECEITA)).thenReturn(new BigDecimal("150.00"));
        when(transactionRepository.sumAmountByType(Transaction.TransactionType.DESPESA)).thenReturn(new BigDecimal("50.00"));

        AdminOverviewResponseDTO overview = adminService.getOverview();

        assertThat(overview.adminWhitelist()).containsExactly("admin@farolfinanceiro.online");
        assertThat(overview.emailsPermitidosParaAdmin()).isEqualTo(1);
        assertThat(overview.saldoGlobal()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRejectAdminOutsideExplicitWhitelist() {
        User intruder = adminUser("intruso@farolfinanceiro.online");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(intruder);
        when(adminAccessPolicy.canAccess(intruder)).thenReturn(false);

        assertThatThrownBy(adminService::getOverview)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("whitelist");
    }

    @Test
    void shouldResetFinancialDataWithoutChangingAccountIdentityOrSecurity() {
        User currentAdmin = adminUser("admin@farolfinanceiro.online");
        User targetUser = User.builder()
                .id(2L)
                .name("Conta de Homologacao")
                .email("teste@farolfinanceiro.online")
                .password("encoded-password")
                .role(User.Role.ADMIN)
                .active(true)
                .twoFactorEnabled(true)
                .twoFactorSecretEncrypted("encrypted-secret")
                .build();
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(currentAdmin);
        when(adminAccessPolicy.canAccess(currentAdmin)).thenReturn(true);
        when(adminAccessPolicy.canPromote(targetUser.getEmail())).thenReturn(true);
        when(userRepository.findById(targetUser.getId())).thenReturn(java.util.Optional.of(targetUser));
        when(transactionRepository.countByUser(targetUser)).thenReturn(0L);

        var response = adminService.resetUserData(targetUser.getId());

        verify(financialDataResetService).reset(targetUser);
        verify(userRepository, never()).save(targetUser);
        verify(userRepository, never()).delete(targetUser);
        assertThat(response.id()).isEqualTo(targetUser.getId());
        assertThat(response.email()).isEqualTo(targetUser.getEmail());
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.active()).isTrue();
        assertThat(response.twoFactorEnabled()).isTrue();
        assertThat(response.totalTransactions()).isZero();
        assertThat(response.lastTransactionDate()).isNull();
        assertThat(targetUser.getPassword()).isEqualTo("encoded-password");
        assertThat(targetUser.getTwoFactorSecretEncrypted()).isEqualTo("encrypted-secret");
    }

    @Test
    void shouldRejectFinancialResetWhenAdminIsOutsideWhitelist() {
        User intruder = adminUser("intruso@farolfinanceiro.online");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(intruder);
        when(adminAccessPolicy.canAccess(intruder)).thenReturn(false);

        assertThatThrownBy(() -> adminService.resetUserData(2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("whitelist");
        verifyNoInteractions(financialDataResetService);
    }

    private User adminUser(String email) {
        return User.builder()
                .id(1L)
                .name("Administrador")
                .email(email)
                .password("encoded")
                .role(User.Role.ADMIN)
                .active(true)
                .build();
    }
}
