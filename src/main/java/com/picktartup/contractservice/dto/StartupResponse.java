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
    private String registration_num;
    private Integer contract_period;
    private String signature;
    private Long ceo_user_id;
    private Integer campaign_id;
    private Double expected_roi;
    private Double roi;
}
