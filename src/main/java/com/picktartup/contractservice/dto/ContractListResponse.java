package com.picktartup.contractservice.dto;

import com.picktartup.contractservice.entity.ContractStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContractListResponse {

    private Long contractId;

    private LocalDateTime contractDate;
    private String startupName;
    private Double tokenAmount;
    private ContractStatus contractStatus;
    private Double progress;

}
