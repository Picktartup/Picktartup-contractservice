package com.picktartup.contractservice.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Setter
@Getter
@Table(name ="contract")
@Entity
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_seq_generator")
    @SequenceGenerator(name = "contract_seq_generator", sequenceName = "contract_seq", allocationSize = 1)
    @Column(name = "contract_id")
    private Long contractId;

    // 계약 체결일자
    private LocalDateTime signedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long startupId;

    @OneToOne(mappedBy = "contract", cascade = CascadeType.ALL)
    private ContractDetails contractDetails;

}
