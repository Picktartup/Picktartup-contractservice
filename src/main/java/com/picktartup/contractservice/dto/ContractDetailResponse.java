package com.picktartup.contractservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContractDetailResponse {

    private String contractStatus;
    private LocalDateTime investAt;        // 투자 등록일
    private LocalDateTime contractBeginAt; // 계약 체결일
    private LocalDateTime contractEndAt;   // 계약 완료일
    private Double investToken;  // 투자 토큰
    private Double returnToken;  // 반환 토큰

    private String startupName;
    private Integer process;
    private Double roi;

    private String startupLogo;
    private String startupDescription;
    private String startupCategory;
    private String investStatus;
    private String investRound;
    private Double expectedRoi;
    private String contractPdfUrl;

}