package com.picktartup.contractservice.service;

import com.picktartup.contractservice.dto.ContractDetailResponse;
import com.picktartup.contractservice.dto.ContractImageResponse;
import com.picktartup.contractservice.dto.ContractRequest;

public interface ContractService {

    String createContract(ContractRequest contractRequest);

    ContractImageResponse getContractImage(Long contractId);

    ContractDetailResponse getContractDetail(Long contractId);
}
