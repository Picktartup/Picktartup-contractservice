package com.picktartup.contractservice.service;

import com.picktartup.contractservice.dto.*;
import com.picktartup.contractservice.entity.ContractStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.IOException;
import java.util.List;

public interface ContractService {

    String generatePdf(ContractPdfRequest contractPdfRequest, @RequestHeader(HttpHeaders.AUTHORIZATION) String authToken);
    ContractResponse createContract(ContractRequest contractRequest, @RequestHeader(HttpHeaders.AUTHORIZATION) String authToken);
    List<ContractListResponse> getContractList(Long userId, String contractStatus);
    ContractDetailResponse getContractDetail(Long contractId);

}
