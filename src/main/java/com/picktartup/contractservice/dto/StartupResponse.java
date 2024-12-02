package com.picktartup.contractservice.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StartupResponse {
    private Long startupId;
    private String name;
    private String description;
    private String category;
    private String progress;
    private String investmentStatus;
    private String address;
    private String ceoName;
    private String logoUrl;
    private String registrationNum;
    private Integer contractPeriod;
    private String signature;
    private Long ceoUserId;
    private Integer campaignId;
    private Double expectedRoi;
    private Double roi;
}
