package com.picktartup.contractservice.service;

import com.picktartup.contractservice.dto.ContractRequest;

public interface ContractService {

    String createContract(ContractRequest contractRequest);
}
