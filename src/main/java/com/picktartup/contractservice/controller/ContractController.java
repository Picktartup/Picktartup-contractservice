package com.picktartup.contractservice.controller;

import com.picktartup.contractservice.common.dto.ApiResponse;
import com.picktartup.contractservice.dto.ContractDetailResponse;
import com.picktartup.contractservice.dto.ContractImageResponse;
import com.picktartup.contractservice.dto.ContractRequest;
import com.picktartup.contractservice.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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






}
