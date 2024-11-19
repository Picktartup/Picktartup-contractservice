package com.picktartup.contractservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractRequest {

    private Long userId;
    private Long startupId;

    private LocalDateTime contractAt;
    private String contractAddress;
    private Double amount;

    private String investorSignature;
    private String startupSignature;

}
