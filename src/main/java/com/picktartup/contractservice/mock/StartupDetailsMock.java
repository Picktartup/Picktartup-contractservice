package com.picktartup.contractservice.mock;

import com.picktartup.contractservice.entity.StartupDetails;

import java.time.LocalDateTime;
import java.util.HashSet;

public class StartupDetailsMock {
    public static StartupDetails createMockStartupDetails() {

        return StartupDetails.builder()
                .startupId(1L)
                .description("목업스타트업입니달라.")
                .investmentStatus("비상장")
                .investmentRound("Seed")
                .address("서울 마포구 상암")
                .ceoName("김목업")
                .registrationNum("123-45-67890")
                .contractPeriod(12)
                .page("https://www.naver.com/")
                .establishmentDate("2020.10.1")
                .expectedRoi(150.0)
                .roi(null)
                .signature("https://contract-image.s3.ap-northeast-2.amazonaws.com/signature/dummyStartup.png")
                .build();
    }
}
