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
@Table(name = "startup_details")
public class StartupDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "startup_seq_generator")
    @SequenceGenerator(name = "startup_seq_generator", sequenceName = "startup_seq", allocationSize = 1)
    @Column(name = "startup_id")
    private Long startupId;

    @Column(length = 200, nullable = false)
    private String description;

    @Column(name = "investment_status", length = 10, nullable = false)
    private String investmentStatus;

    @Column(name = "investment_round", length = 20, nullable = false)
    private String investmentRound;

    @Column(length = 100, nullable = false)
    private String address;

    @Column(name = "ceo_name", length = 10, nullable = false)
    private String ceoName;

    @Column(name = "registration_num", length = 30, nullable = false)
    private String registrationNum;

    @Column(name = "contract_period", nullable = false)
    private Integer contractPeriod;

    @Column(length = 30, nullable = false)
    private String page;

    @Column(name = "establishment_date", length = 30, nullable = false)
    private String establishmentDate;

    @Column(name = "expected_roi", nullable = false)
    private Double expectedRoi;

    private Double roi;

}
