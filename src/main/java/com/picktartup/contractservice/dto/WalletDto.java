package com.picktartup.contractservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class WalletDto {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletInfo {
        private String address;
        private String keystoreFilename;
        private String status;
    }
}