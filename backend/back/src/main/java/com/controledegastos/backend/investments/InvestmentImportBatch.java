package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "investment_import_batches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentImportBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    @Column(name = "source_format", nullable = false, length = 20)
    private String sourceFormat;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.EM_REVISAO;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }

    public enum Status { EM_REVISAO, CONFIRMADO, CANCELADO }
}
