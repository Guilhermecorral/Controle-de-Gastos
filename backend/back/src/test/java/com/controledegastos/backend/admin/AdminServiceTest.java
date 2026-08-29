package com.controledegastos.backend.admin;

import com.controledegastos.backend.admin.dto.AdminOverviewResponseDTO;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @InjectMocks
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminService, "allowedAdminEmails", "");
        ReflectionTestUtils.setField(adminService, "bootstrapAdminEmail", "admin@farolfinanceiro.online");
    }

    @Test
    void shouldUseBootstrapEmailAsSecureWhitelistFallback() {
        User currentAdmin = adminUser("ADMIN@farolfinanceiro.online");
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(currentAdmin);
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
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(adminUser("intruso@farolfinanceiro.online"));

        assertThatThrownBy(adminService::getOverview)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("whitelist");
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
