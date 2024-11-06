package com.picktartup.contractservice.controller;

import com.picktartup.contractservice.common.dto.ApiResponse;
import com.picktartup.contractservice.dto.ContractDetailResponse;
import com.picktartup.contractservice.dto.ContractImageResponse;
import com.picktartup.contractservice.dto.ContractListResponse;
import com.picktartup.contractservice.dto.ContractRequest;
import com.picktartup.contractservice.entity.ContractStatus;
import com.picktartup.contractservice.entity.Users;
import com.picktartup.contractservice.mock.UserMock;
import com.picktartup.contractservice.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RequiredArgsConstructor
@RestController
public class ContractController {

    private final ContractService contractService;

    // 계약생성
    @PostMapping("/api/v1//contract/transaction")
    public ApiResponse<String> createContract(@RequestBody ContractRequest contractRequest) {
        return ApiResponse.ok(contractService.createContract(contractRequest));
    }

    // 계약서 이미지 조회
    @GetMapping("/api/v1/contracts/{contractId}/image")
    public ApiResponse<ContractImageResponse> getContractImage(@PathVariable("contractId") Long contractId) {
        return ApiResponse.ok(contractService.getContractImage(contractId));
    }

    // 계약서 상세 조회
    @GetMapping("/api/v1/investments/transactions/{contractId}")
    public ApiResponse<ContractDetailResponse> getTransactionDetails(@PathVariable Long contractId) {
        return ApiResponse.ok(contractService.getContractDetail(contractId));
    }

    // 계약 상태에 따른 투자 리스트 조회
    @GetMapping("/api/v1/contracts/{contract_status}")
    public ApiResponse<List<ContractListResponse>> getContractList(@PathVariable ContractStatus contract_status) {
        // 통합 테스트 때 jwt 토큰으로 userId 받아올 예정
        Users mockUser = UserMock.createMockUser();
        Long userId = mockUser.getUserId();
        return ApiResponse.ok(contractService.getContractList(userId, contract_status));
    }










}
