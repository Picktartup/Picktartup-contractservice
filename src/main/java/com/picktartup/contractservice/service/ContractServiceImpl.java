package com.picktartup.contractservice.service;

import com.picktartup.contractservice.dto.ContractRequest;
import com.picktartup.contractservice.entity.*;
import com.picktartup.contractservice.mock.StartupMock;
import com.picktartup.contractservice.mock.UserMock;
import com.picktartup.contractservice.repository.ContractDetailsRepository;
import com.picktartup.contractservice.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ContractServiceImpl implements ContractService{

    private final ContractRepository contractRepository;
    private final ContractDetailsRepository contractDetailsRepository;

    // 계약생성
    @Override
    public String createContract(ContractRequest contractRequest) {
        Contract contract = new Contract();

        // user api에 user 정보 요청 (contractRequest.getUserId)
        Users userMock = UserMock.createMockUser();

        // startup api에 startup 정보 요청 (contractRequest.getStartupId)
        Startup startupMock = StartupMock.createMockStartup();

        // 계약정보 등록
        contract.setUserId(contractRequest.getUserId());
        contract.setStartupId(contractRequest.getStartupId());
        contract.setStatus(ContractStatus.ACTIVE);

        contract = contractRepository.save(contract);

        // 계약상세정보 등록
        ContractDetails contractDetails = getContractDetails(contractRequest, contract);
        contractDetailsRepository.save(contractDetails);

        return "계약 생성 완료";
    }

    private static ContractDetails getContractDetails(ContractRequest contractRequest, Contract contract) {
        ContractDetails contractDetails = new ContractDetails();
        contractDetails.setContract(contract);
        contractDetails.setContractAddress(contractRequest.getContractAddress());
        contractDetails.setTokenAmount(contractRequest.getAmount());
        contractDetails.setImgUrl(contractRequest.getImgUrl());
        contractDetails.setInvestorSignature(contractRequest.getInvestorSignature());
        contractDetails.setStartupSignature(contractRequest.getStartupSignature());
        contractDetails.setContract_at(contractRequest.getContractAt());
        return contractDetails;
    }
}
