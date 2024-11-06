package com.picktartup.contractservice.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContractDetailResponse {
    private String startupName;
    private String startupCategory;
    private String startupDescription;
    private String progressStatus; // contractStatus 상태에 따라 % 또는 취소, 완료
    private Integer goalCoin;
    private Integer currentCoin;
    private String contractStatus;
    private Double investedTokenAmount; // 투자토큰
    private LocalDateTime transactionCreatedAt; // 거래 생성일
    private String contractAddress; // 계약주소
}
