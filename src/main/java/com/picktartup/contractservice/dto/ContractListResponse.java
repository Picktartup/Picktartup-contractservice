package com.picktartup.contractservice.dto;

import com.picktartup.contractservice.entity.ContractStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContractListResponse {

    private Long contractId;

    private LocalDateTime contractDate;
    private String startupName;
    private Double tokenAmount;
    private ContractStatus contractStatus;
    private Double progress;

}
