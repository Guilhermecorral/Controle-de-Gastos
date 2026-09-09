package com.controledegastos.backend.investments;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Raw public-source response retained with a hash so parsers can be audited and replayed. */
@Entity
@Table(name = "market_data_snapshots", indexes = @Index(name = "idx_market_snapshot_lookup", columnList = "source,request_key,fetched_at"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "request_key", nullable = false, length = 160)
    private String requestKey;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    // PostgreSQL stores large text directly; @Lob would map this field to an OID instead of TEXT.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
