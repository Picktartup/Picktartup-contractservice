package com.picktartup.contractservice.controller;

import com.picktartup.contractservice.common.dto.ApiResponse;
import com.picktartup.contractservice.dto.*;
import com.picktartup.contractservice.entity.Users;
import com.picktartup.contractservice.mock.UserMock;
import com.picktartup.contractservice.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final ContractService contractService;

    // 계약서 PDF 생성
    @PostMapping("/pdf")
    public ApiResponse<String> generatePdf(@RequestBody ContractPdfRequest contractPdfRequest) {
        return ApiResponse.ok(contractService.generatePdf(contractPdfRequest));
    }

    // 계약 생성
    @PostMapping("/transaction")
    public ApiResponse<ContractResponse> createContract(@RequestBody ContractRequest contractRequest) {
        log.info("투자 요청 - startupId: {}, amount: {}, userId: {}",
                contractRequest.getStartupId(), contractRequest.getAmount(), contractRequest.getUserId());

        return ApiResponse.ok(contractService.createContract(contractRequest));
    }

    // 계약 상태에 따른 투자 리스트 조회 : 진행 중 - active / 완료 - completed
    @GetMapping("/status/{contractStatus}")
    public ApiResponse<List<ContractListResponse>> getContractList(@PathVariable String contractStatus, @RequestParam Long userId) {
        // 통합 테스트 때 jwt 토큰으로 userId 받아올 예정
        return ApiResponse.ok(contractService.getContractList(userId, contractStatus));
    }

    // 계약서 상세 조회
    @GetMapping("/details/{contractId}")
    public ApiResponse<ContractDetailResponse> getTransactionDetails(@PathVariable Long contractId) {
        return ApiResponse.ok(contractService.getContractDetail(contractId));
    }

}
