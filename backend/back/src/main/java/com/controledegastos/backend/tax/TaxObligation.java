package com.controledegastos.backend.tax;

import com.controledegastos.backend.investments.TaxPayment;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tax_obligations")
@Getter @Setter
public class TaxObligation {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 160)
    private String name;
    @Column(length = 120)
    private String issuingAuthority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Category category;
    @Column(nullable = false)
    private LocalDate dueDate;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedAmount;
    @Column(precision = 19, scale = 2)
    private BigDecimal paidAmount;
    private LocalDate paidDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DocumentStage documentStage;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Recurrence recurrence;
    @Column(nullable = false)
    private Integer competenceYear;
    private Integer competenceMonth;
    @Column(length = 1000)
    private String notes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Origin origin;
    @Column(length = 255)
    private String paymentAccountDescription;
    @Column(length = 255)
    private String receiptReference;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_transaction_id", unique = true)
    private Transaction linkedTransaction;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_darf_id", unique = true)
    private TaxPayment linkedDarf;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Category { IRPF, IPVA, IPTU, ISS, INSS, LICENCIAMENTO, DARF, PERSONALIZADO }
    public enum Status { A_PAGAR, PAGA, ATRASADA, ISENTA, EM_REVISAO }
    public enum DocumentStage { ESTIMATIVA, GUIA_EMITIDA }
    public enum Recurrence { UNICA, MENSAL, ANUAL, PERSONALIZADA }
    public enum Origin { MANUAL, INVESTIMENTO, IMPORTACAO }
}
