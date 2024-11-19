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

    @Column(nullable = false, length = 20)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(length = 100)
    private String category;

    private Integer progress;

    @Column(name = "investment_start_date", nullable = false)
    private LocalDateTime investmentStartDate;

    @Column(name = "investment_target_deadline", nullable = false)
    private LocalDateTime investmentTargetDeadline;

    @Column(name = "goal_coin", nullable = false)
    private Integer goalCoin;

    @Column(name = "expected_roi", nullable = false)
    private Double expectedRoi;

    @Column(name = "current_coin", nullable = false)
    private Double currentCoin;

    @Column(name = "investment_status", length = 10, nullable = false)
    private String investmentStatus;

    @Column(name = "investment_round", length = 20)
    private String investmentRound;

    private Double roi;

    @Column(length = 100)
    private String address;

    @Column(name = "ceo_name", length = 100)
    private String ceoName;

    @Column(name = "registration_num", length = 30)
    private String registrationNum;

    @Column(name = "contract_period", nullable = false)
    private Integer contractPeriod;

    @OneToMany(mappedBy = "startup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Contract> contracts;
}
