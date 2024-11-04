package com.picktartup.contractservice.controller;

import com.picktartup.contractservice.common.dto.ApiResponse;
import com.picktartup.contractservice.dto.ContractRequest;
import com.picktartup.contractservice.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ContractController {

    private final ContractService contractService;

    @PostMapping("/api/v1//contract/transaction")
    public ApiResponse<String> createContract(@RequestBody ContractRequest contractRequest) {
        return ApiResponse.ok(contractService.createContract(contractRequest));
    }



}
