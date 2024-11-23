package com.picktartup.contractservice.mock;

import com.picktartup.contractservice.entity.Wallet;
import com.picktartup.contractservice.entity.WalletStatus;

import java.math.BigDecimal;

public class WalletMock {
    public static Wallet createWalletMock() {

        return Wallet.builder()
                .userId(1L)
                .keystoreFilename("UTC--2024-11-22T03-32-17.47022000Z--f019f5d7948c9af927560a60ad4eb4b4739aff9d.json")
                .address("0xf019f5d7948c9af927560a60ad4eb4b4739aff9d")
                .balance(new BigDecimal("0"))
                .status(WalletStatus.ACTIVE)
                .build();
    }
}
