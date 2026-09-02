package com.controledegastos.backend.investments;

import com.controledegastos.backend.investments.InvestmentDtos.ProjectionResponse;
import com.controledegastos.backend.security.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class InvestmentServiceTest {

    private final InvestmentService service = new InvestmentService(
            mock(InvestmentPositionRepository.class),
            mock(AuthenticatedUserService.class),
            mock(MarketQuoteService.class),
            mock(InvestmentMovementRepository.class),
            mock(com.controledegastos.backend.transactions.Repository.TransactionRepository.class)
    );

    @Test
    void shouldProjectTwelvePercentWithCompoundInterest() {
        ReflectionTestUtils.setField(service, "defaultAnnualRate", new BigDecimal("12.0"));

        ProjectionResponse result = service.projection(
                new BigDecimal("10000"),
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1)
        );

        assertThat(result.months()).isEqualTo(12);
        assertThat(result.projectedBalance()).isEqualByComparingTo("11200.00");
        assertThat(result.projectedEarnings()).isEqualByComparingTo("1200.00");
        assertThat(result.timeline()).hasSize(12);
    }

    @Test
    void shouldRejectInvalidProjectionPeriod() {
        assertThatThrownBy(() -> service.projection(
                new BigDecimal("1000"), new BigDecimal("12"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("posterior");
    }
}
