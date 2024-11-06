package com.picktartup.contractservice.service;

import com.picktartup.contractservice.dto.ContractDetailResponse;
import com.picktartup.contractservice.dto.ContractImageResponse;
import com.picktartup.contractservice.dto.ContractListResponse;
import com.picktartup.contractservice.dto.ContractRequest;
import com.picktartup.contractservice.entity.ContractStatus;

import java.util.List;

public interface ContractService {

    String createContract(ContractRequest contractRequest);

    ContractImageResponse getContractImage(Long contractId);

    ContractDetailResponse getContractDetail(Long contractId);

    List<ContractListResponse> getContractList(Long userId, ContractStatus contractStatus);
}
