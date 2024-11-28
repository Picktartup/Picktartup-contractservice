package com.picktartup.contractservice.service;

import com.picktartup.contractservice.dto.*;
import com.picktartup.contractservice.entity.ContractStatus;

import java.io.IOException;
import java.util.List;

public interface ContractService {

    String generatePdf(ContractPdfRequest contractPdfRequest);
    ContractResponse createContract(ContractRequest contractRequest);
    List<ContractListResponse> getContractList(Long userId, String contractStatus);
    ContractDetailResponse getContractDetail(Long contractId);

}
