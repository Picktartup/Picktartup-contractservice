package com.picktartup.contractservice.mock;

import com.picktartup.contractservice.entity.Startup;
import com.picktartup.contractservice.entity.Wallet;

import java.time.LocalDateTime;
import java.util.HashSet;

public class StartupMock {
    public static Startup createMockStartup() {
        Wallet wallet = Wallet.builder()
                .walletId(2L)
                .address("mock_startup_wallet_address")
                .balance(5000.0)
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
