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
                .description("목업스타트업입니달라.")
                .category("테크")
                .progress(40)
                .investmentStartDate(LocalDateTime.now())
                .investmentTargetDeadline(LocalDateTime.now().plusMonths(2))
                .goalCoin(1000)
                .expectedRoi(150.0)
                .currentCoin(200.0)
                .investmentStatus("비상장")
                .investmentRound("Seed")
                .roi(null)
                .address("서울 마포구 상암")
                .ceoName("김목업")
                .registrationNum("123-45-67890")
                .contractPeriod(12)
                .contracts(new HashSet<>())
                .build();
    }
}
