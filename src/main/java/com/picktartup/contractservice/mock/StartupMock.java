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
                .name("Mock Startup Inc.")
                .description("This is a mock startup for testing purposes.")
                .category("Technology")
                .progress(40)
                .ssi(4.5)
                .contractStartDate(LocalDateTime.now())
                .contractTargetDeadline(LocalDateTime.now().plusMonths(2))
                .goalCoin(1000)
                .expectedReturn(150.0)
                .currentCoin(200)
                .contracts(new HashSet<>())
                .build();
    }
}
