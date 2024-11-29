package com.picktartup.contractservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractPdfRequest {

    private Long userId;
    private Long startupId;

    private Double amount;
    private String transactionHash;
    private String investorSignature;

}
