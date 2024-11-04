package com.picktartup.contractservice.mock;

import com.picktartup.contractservice.entity.Role;
import com.picktartup.contractservice.entity.Users;
import com.picktartup.contractservice.entity.Wallet;

import java.time.LocalDateTime;
import java.util.HashSet;

public class UserMock {
    public static Users createMockUser() {
        Wallet wallet = Wallet.builder()
                .walletId(1L)
                .address("mock_address_123")
                .balance(1000.0)
                .build();

        return Users.builder()
                .userId(1L)
                .username("mockUser")
                .email("mockUser@example.com")
                .encryptedPwd("mockEncryptedPassword")
                .role(Role.USER)
                .isActivated(true)
                .createdAt(LocalDateTime.now())
                .wallet(wallet)
                .contracts(new HashSet<>()) // 초기 빈 계약 세트
                .transactions(new HashSet<>()) // 초기 빈 거래 세트
                .build();
    }
}
