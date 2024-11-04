package com.picktartup.contractservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Setter
@Getter
@Table(name = "contractdetails")
@Entity
public class ContractDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "details_seq_generator")
    @SequenceGenerator(name = "details_seq_generator", sequenceName = "details_seq", allocationSize = 1)
    @Column(name = "details_id")
    private Long detailsId;

    @OneToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    // tokenAmount 이 자주 변경될 것이 예상되어 ContractDetails를 연관관계의 주인으로
    @Column(nullable = false)
    private Double tokenAmount;

    @Column(nullable = false)
    private String imgUrl;

    @Column(nullable = false)
    private String contractAddress;

    // 거래 생성일
    @Column(nullable = false)
    private LocalDateTime contract_at;

    @Column(nullable = false)
    private String investorSignature;

    @Column(nullable = false)
    private String startupSignature;

}
