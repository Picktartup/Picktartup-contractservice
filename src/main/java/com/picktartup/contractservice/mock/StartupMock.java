package com.picktartup.contractservice.mock;

import com.picktartup.contractservice.entity.Startup;
import com.picktartup.contractservice.entity.Wallet;
import com.picktartup.contractservice.entity.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;

public class StartupMock {
    public static Startup createMockStartup() {
        Wallet wallet = Wallet.builder()
                .userId(1L)
                .keystoreFilename("keystore1.json")
                .address("0x1234567890abcdef1234567890abcdef12345678")
                .balance(new BigDecimal("5000.0"))
                .status(WalletStatus.ACTIVE)
                .build();

        return Startup.builder()
                .startupId(1L)
                .wallet(wallet)
                .name("목업스타트업")
                .category("테크")
                .progress(40)
                .investmentStartDate(LocalDateTime.now())
                .investmentTargetDeadline(LocalDateTime.now().plusMonths(2))
                .goalCoin(1000)
                .currentCoin(200.0)
                .fundingProgress(10)
                .logoUrl("https://logo-resources.thevc.kr/organizations/200x200/e116f9f95794fd8c56dcf39b82836c36da4f8f41862543d32600cab91989d5ab_1646663882349973.jpg")
                .build();
    }
}
