package com.picktartup.contractservice.service;

import com.picktartup.contractservice.dto.*;
import com.picktartup.contractservice.entity.ContractStatus;

import java.io.IOException;
import java.util.List;

public interface ContractService {

    ContractResponse createContract(ContractRequest contractRequest);

    ContractImageResponse getContractImage(Long contractId);

    ContractDetailResponse getContractDetail(Long contractId);

    List<ContractListResponse> getContractList(Long userId, ContractStatus contractStatus);
}
