package com.picktartup.contractservice.service;

import com.picktartup.contractservice.dto.ContractDetailResponse;
import com.picktartup.contractservice.dto.ContractImageResponse;
import com.picktartup.contractservice.dto.ContractRequest;
import com.picktartup.contractservice.entity.*;
import com.picktartup.contractservice.mock.StartupMock;
import com.picktartup.contractservice.mock.UserMock;
import com.picktartup.contractservice.repository.ContractDetailsRepository;
import com.picktartup.contractservice.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    // 계약서 이미지 조회
    @Override
    public ContractImageResponse getContractImage(Long contractId) {
        // Contract와 ContractDetails를 한 번의 쿼리로 조회
        Contract contract = contractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new RuntimeException("해당 계약서를 찾을 수 없습니다."));

        // imgUrl을 DTO로 반환
        return new ContractImageResponse(contract.getContractDetails().getImgUrl());
    }

    // 계약서 상세 조회
    @Override
    public ContractDetailResponse getContractDetail(Long contractId) {
        // contractId로 Contract와 ContractDetails 동시에 가져오기
        Contract contract = contractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new RuntimeException("해당 계약을 찾을 수 없습니다."));
        ContractDetails contractDetails = contract.getContractDetails();

        // Mock 대체 외부 API 호출하여 스타트업 정보 가져오기
        Long startupId = contract.getStartupId();
        Startup mockStartup = StartupMock.createMockStartup();

        // 계약 상태에 따른 progressStatus 설정
        int progress = mockStartup.getProgress();
        String progressStatus;
        switch (contract.getStatus()) {
            case BEGIN:
            case ACTIVE:
                progressStatus = progress + "%";
                break;
            case CANCELLED:
                progressStatus = "취소";
                break;
            case COMPLETED:
                progressStatus = "완료";
                break;
            default:
                throw new RuntimeException("알 수 없는 계약 상태입니다.");
        }

        // ContractDetailResponseDto 생성 및 반환
        return new ContractDetailResponse(
                mockStartup.getName(),
                mockStartup.getCategory(),
                mockStartup.getDescription(),
                progressStatus,
                mockStartup.getGoalCoin(),
                mockStartup.getCurrentCoin(),
                contract.getStatus().toString(),
                contractDetails.getTokenAmount(),
                contractDetails.getContract_at(),
                contractDetails.getContractAddress()
        );
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
