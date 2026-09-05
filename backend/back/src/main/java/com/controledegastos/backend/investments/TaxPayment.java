package com.controledegastos.backend.investments;
import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "tax_payments", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period", "revenue_code"}))
@Getter @Setter @NoArgsConstructor
public class TaxPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 7) private String period;
    @Column(name = "revenue_code", nullable = false, length = 4) private String revenueCode;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private LocalDate paidAt;
    @Column(nullable = false) private LocalDate dueDate;
    @Column(nullable = false, length = 255) private String accountLabel;
    @Column(nullable = false, length = 255) private String note;
}
