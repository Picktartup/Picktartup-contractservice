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
    private LocalDateTime contractAt;
    private Double tokenAmount;
    private ContractStatus contractStatus;
    private String startupName;
    private Integer startupGoalProgress;
}
