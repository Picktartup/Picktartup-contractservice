package com.picktartup.contractservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.itextpdf.html2pdf.HtmlConverter;
import com.picktartup.contractservice.dto.*;
import com.picktartup.contractservice.entity.*;
import com.picktartup.contractservice.mock.StartupMock;
import com.picktartup.contractservice.mock.UserMock;
import com.picktartup.contractservice.repository.ContractDetailsRepository;
import com.picktartup.contractservice.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ContractServiceImpl implements ContractService{

    @Autowired
    private final ContractRepository contractRepository;
    @Autowired
    private final ContractDetailsRepository contractDetailsRepository;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private AmazonS3 s3Client;

    private static ContractDetails getContractDetails(ContractRequest contractRequest, Contract contract) {
        ContractDetails contractDetails = new ContractDetails();
        contractDetails.setContract(contract);
        contractDetails.setContractAddress(contractRequest.getContractAddress());
        contractDetails.setTokenAmount(contractRequest.getAmount());
        contractDetails.setImgUrl(null);
        contractDetails.setInvestorSignature(contractRequest.getInvestorSignature());
        contractDetails.setStartupSignature(contractRequest.getStartupSignature());
        contractDetails.setContract_at(contractRequest.getContractAt());
        return contractDetails;
    }

    // 계약생성
    @Override
    public ContractResponse createContract(ContractRequest contractRequest) {
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

        // 스마트 컨트랙트 요청

        // pdf 생성
        // Step 1: Generate HTML from Thymeleaf
        Context context = new Context();
        context.setVariable("investorName", userMock.getUsername());
        context.setVariable("investorWallet", userMock.getWallet().getAddress());
        context.setVariable("companyName", startupMock.getName());
        context.setVariable("companyAddress", startupMock.getAddress());
        context.setVariable("ceoName", startupMock.getCeoName());
        context.setVariable("companyRegistrationNumber", startupMock.getRegistrationNum());
        context.setVariable("companyWallet", startupMock.getWallet().getAddress());
        context.setVariable("contractPeriod", startupMock.getContractPeriod());
        context.setVariable("investmentAmount", contractRequest.getAmount());
        context.setVariable("smartContractAddress", contractRequest.getContractAddress());
        context.setVariable("contractAt", contractRequest.getContractAt().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        context.setVariable("transactionHash", "0x6fcb8e3d34f8e67c98a31d7a8b1f045d2e9e9b3a2b1c3d8f7e9a67b9c8d5e4f6\n");

        String htmlContent = templateEngine.process("contract-template", context);

        // Step 2: Convert HTML to PDF
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        try {
            HtmlConverter.convertToPdf(new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8)), pdfOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] pdfBytes = pdfOutputStream.toByteArray();

        // Step 3: Upload PDF to S3
        String bucketName = "contract-image";
        String pdfFileName = "contracts/" + System.currentTimeMillis() + ".pdf";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/pdf");
        metadata.setContentLength(pdfBytes.length);

        s3Client.putObject(bucketName, pdfFileName, new ByteArrayInputStream(pdfBytes), metadata);

        // Step 4: Get S3 URL
        URL s3Url = s3Client.getUrl(bucketName, pdfFileName);

        // 계약상세정보 등록
        // Step 5: Save S3 URL
        ContractDetails contractDetails = getContractDetails(contractRequest, contract);
        contractDetails.setImgUrl(s3Url.toString());
        contractDetailsRepository.save(contractDetails);

        // Step 6: Return S3 URL in response
        return new ContractResponse(s3Url.toString());
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
        if (contract.getStatus() == ContractStatus.ACTIVE) {
            // ACTIVE 상태일 때만 목표달성도 표시
            progressStatus = mockStartup.getProgress() + "%";
        } else {
            // 다른 상태일 때는 progress 대신 상태에 맞는 메시지 표시
            switch (contract.getStatus()) {
                case BEGIN:
                    progressStatus = "진행 전";
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


    // 계약 상태에 따른 투자 리스트 조회
    @Override
    public List<ContractListResponse> getContractList(Long userId, ContractStatus contractStatus) {
        List<Contract> contracts = contractRepository.findByUserIdAndStatus(userId, contractStatus);

        List<ContractListResponse> response = new ArrayList<>();
        for (Contract contract : contracts) {
            ContractDetails details = contractDetailsRepository.findByContract_ContractId(contract.getContractId());

            // Startup 정보 요청
            Long startupId = contract.getStartupId();
            Startup mockStartup = StartupMock.createMockStartup();

            // ContractResponseDTO 생성
            ContractListResponse contractResponse = new ContractListResponse(
                    contract.getContractId(),
                    details.getContract_at(),
                    details.getTokenAmount(),
                    contract.getStatus(),
                    mockStartup.getName()
            );
            response.add(contractResponse);
        }
        return response;
    }
}
