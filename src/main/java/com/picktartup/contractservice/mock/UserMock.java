package com.picktartup.contractservice.mock;

import com.picktartup.contractservice.entity.Role;
import com.picktartup.contractservice.entity.Users;
import com.picktartup.contractservice.entity.Wallet;
import com.picktartup.contractservice.entity.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;

public class UserMock {
    public static Users createMockUser() {
        Wallet wallet = Wallet.builder()
                .userId(1L)
                .keystoreFilename("keystore1.json")
                .address("0x1234567890abcdef1234567890abcdef12345678")
                .balance(new BigDecimal("5000.0"))
                .status(WalletStatus.ACTIVE)
                .build();

        return Users.builder()
                .userId(1L)
                .username("mockUser")
                .email("mockUser@example.com")
                .encryptedPwd("mockEncryptedPassword")
                .role(Role.USER)
                .isActivated(true)
                .createdAt(LocalDateTime.now())
                .contracts(new HashSet<>()) // 초기 빈 계약 세트
                .transactions(new HashSet<>()) // 초기 빈 거래 세트
                .build();
    }
}
