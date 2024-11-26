package com.picktartup.contractservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Setter
@Getter
@Table(name = "startup")
public class Startup {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "startup_seq_generator")
    @SequenceGenerator(name = "startup_seq_generator", sequenceName = "startup_seq", allocationSize = 1)
    @Column(name = "startup_id")
    private Long startupId;

    @OneToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(length = 100, nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer progress;

    @Column(name = "investment_start_date", nullable = false)
    private LocalDateTime investmentStartDate;

    @Column(name = "investment_target_deadline", nullable = false)
    private LocalDateTime investmentTargetDeadline;

    @Column(name = "goal_coin", nullable = false)
    private Integer goalCoin;

    @Column(name = "current_coin", nullable = false)
    private Double currentCoin;

    @Column(name = "funding_progress", nullable = false)
    private Integer fundingProgress;

    @Column(name = "logo_url", length = 100, nullable = false)
    private String logoUrl;

    @OneToMany(mappedBy = "startup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Contract> contracts;
}
