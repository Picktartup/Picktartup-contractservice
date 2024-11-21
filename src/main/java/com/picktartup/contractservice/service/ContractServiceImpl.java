package com.picktartup.contractservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.itextpdf.html2pdf.HtmlConverter;
import com.picktartup.contractservice.dto.*;
import com.picktartup.contractservice.entity.*;
import com.picktartup.contractservice.mock.StartupDetailsMock;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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

    // TODO: 모금 완료되어 계약 최종 체결 시
    // 1) contract.signedAt 컬럼 업데이트 -> now()
    // 2) contract.status 컬럼 업데이트 -> BEGIN to ACTIVE
    // how to: 스마트 컨트랙트 컴파일된 파일에서 해당 함수 import

    private static ContractDetails getContractDetails(ContractRequest contractRequest, Contract contract) {
        ContractDetails contractDetails = new ContractDetails();
        contractDetails.setContract(contract);
        contractDetails.setContractAddress(contractRequest.getContractAddress());
        contractDetails.setTokenAmount(contractRequest.getAmount());
        contractDetails.setImgUrl(null);
        contractDetails.setInvestorSignature(contractRequest.getInvestorSignature());
        contractDetails.setStartupSignature(contractRequest.getStartupSignature());
        contractDetails.setContractAt(contractRequest.getContractAt());

        return contractDetails;
    }

    // 임시 계약서 PDF 삭제
    private void deleteTemporaryPdfs(String bucketName, String userId, String startupId) {
        String prefix = "contracts/tmp_" + userId + "_" + startupId; // tmp_로 시작하는 파일 패턴

        // S3 버킷에서 객체 나열 및 삭제
        ObjectListing objectListing = s3Client.listObjects(bucketName);
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            String key = objectSummary.getKey();

            // 파일명이 prefix로 시작하면 삭제
            if (key.startsWith(prefix)) {
                System.out.println("Deleting: " + key);
                s3Client.deleteObject(bucketName, key);
            }
        }
    }

    // 계약서 PDF 생성
    @Override
    public String generatePdf(ContractRequest contractRequest) {
        URL s3Url;

        // user api에 user 정보 요청 (contractRequest.getUserId)
        Users userMock = UserMock.createMockUser();

        // startup api에 startup 정보 요청 (contractRequest.getStartupId)
        Startup startupMock = StartupMock.createMockStartup();
        StartupDetails startupDetailsMock = StartupDetailsMock.createMockStartupDetails();

        // 특정 필드가 null이면 []로 텍스트 변환 else 값 그대로
        String contractAddress = (contractRequest.getContractAddress() != null)
                ? contractRequest.getContractAddress()
                : "[스마트 컨트랙트 주소]";
        String transactionHash = (contractRequest.getTransactionHash() != null)
                ? contractRequest.getTransactionHash()
                : "[트랜잭션 해시]";

        // pdf 생성
        // Step 1: Generate HTML from Thymeleaf
        Context context = new Context();
        context.setVariable("investorName", userMock.getUsername());
        context.setVariable("investorWallet", userMock.getWallet().getAddress());
        context.setVariable("companyName", startupMock.getName());
        context.setVariable("companyAddress", startupDetailsMock.getAddress());
        context.setVariable("ceoName", startupDetailsMock.getCeoName());
        context.setVariable("companyRegistrationNumber", startupDetailsMock.getRegistrationNum());
        context.setVariable("companyWallet", startupMock.getWallet().getAddress());
        context.setVariable("contractPeriod", startupDetailsMock.getContractPeriod());
        context.setVariable("investmentAmount", contractRequest.getAmount());
        context.setVariable("smartContractAddress", contractAddress); // tmp -> NULL
        context.setVariable("contractAt", contractRequest.getContractAt().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        context.setVariable("transactionHash", transactionHash); // tmp -> NULL

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

        // 조건문 적용 -> 특정 필드가 null이면 이름 앞에 tmp_ 붙이기 else 그대로
        String prefix;
        if (contractRequest.getTransactionHash() == null) {
            prefix = "tmp_" + userMock.getUserId() + "_" + startupMock.getStartupId();
        } else {
            prefix = userMock.getUserId() + "_" + startupMock.getStartupId();
        }
        String pdfFileName = "contracts/" + prefix + "_" + System.currentTimeMillis() + ".pdf";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/pdf");
        metadata.setContentLength(pdfBytes.length);

        s3Client.putObject(bucketName, pdfFileName, new ByteArrayInputStream(pdfBytes), metadata);

        // Step 4: Get S3 URL
        s3Url = s3Client.getUrl(bucketName, pdfFileName);

        return s3Url.toString();
    }

    // 계약 생성
    @Override
    public ContractResponse createContract(ContractRequest contractRequest) {
        Contract contract = new Contract();

        // user api에 user 정보 요청 (contractRequest.getUserId)
        Users userMock = UserMock.createMockUser();

        // startup api에 startup 정보 요청 (contractRequest.getStartupId)
        Startup startupMock = StartupMock.createMockStartup();
        StartupDetails startupDetailsMock = StartupDetailsMock.createMockStartupDetails();

        // TODO: 스마트 컨트랙트 로직

        // 계약정보 등록
        contract.setUserId(contractRequest.getUserId());
        contract.setStartupId(contractRequest.getStartupId());
        contract.setStatus(ContractStatus.ACTIVE);

        contract = contractRepository.save(contract);

        // 계약상세정보 등록 + 최종 PDF 생성
        String s3Url = generatePdf(contractRequest);

        ContractDetails contractDetails = getContractDetails(contractRequest, contract);
        contractDetails.setImgUrl(s3Url);

        contractDetailsRepository.save(contractDetails);

        // 임시 pdf 삭제 로직 : 이름이 tmp_로 시작하면 삭제
        String bucketName = "contract-image";
        deleteTemporaryPdfs(bucketName, userMock.getUserId().toString(), startupMock.getStartupId().toString());

        return new ContractResponse(s3Url); // 컨트랙트 주소, 해시값 포함된 새로운 pdf
    }

    // 계약 상태에 따른 투자 리스트 조회
    @Override
    public List<ContractListResponse> getContractList(Long userId, String contractStatus) {
        List<ContractStatus> statuses;

        // contractStatus 값에 따라 상태 리스트를 설정
        if ("active".equalsIgnoreCase(contractStatus)) {
            statuses = Arrays.asList(ContractStatus.BEGIN, ContractStatus.ACTIVE);
        } else if ("completed".equalsIgnoreCase(contractStatus)) {
            statuses = Arrays.asList(ContractStatus.COMPLETED, ContractStatus.CANCELLED);
        } else {
            throw new IllegalArgumentException("Invalid contract status: " + contractStatus);
        }

        // 계약 리스트 조회
        List<Contract> contracts = contractRepository.findContractsByUserIdAndStatuses(userId, statuses);

        // 응답 리스트 생성
        List<ContractListResponse> response = new ArrayList<>();
        for (Contract contract : contracts) {
            // 연관된 ContractDetails 조회
            ContractDetails details = contractDetailsRepository.findByContract_ContractId(contract.getContractId());
            if (details == null) {
                continue; // contractDetails가 없는 경우 건너뜀
            }

            // Mock 데이터 (나중에 실제 데이터로 변경 필요)
            Startup startupMock = StartupMock.createMockStartup();
            StartupDetails startupDetailsMock = StartupDetailsMock.createMockStartupDetails();

            Double roi = (startupDetailsMock.getRoi() != null) ? startupDetailsMock.getRoi() : null;

            Double returnToken = (roi != null)
                    ? details.getTokenAmount() * (1 + roi / 100)
                    : null;

            // 반환 데이터 설정
            LocalDateTime contractDate = "active".equalsIgnoreCase(contractStatus)
                    ? details.getContractAt()
                    : contract.getSignedAt().plusMonths(startupDetailsMock.getContractPeriod());

            Double tokenAmount = "active".equalsIgnoreCase(contractStatus)
                    ? details.getTokenAmount()
                    : returnToken;
            Double progress = "active".equalsIgnoreCase(contractStatus)
                    ? Double.valueOf(startupMock.getProgress())
                    : roi;

            // ContractListResponse 객체 생성
            ContractListResponse contractResponse = new ContractListResponse(
                    contract.getContractId(),
                    contractDate,
                    startupMock.getName(),
                    tokenAmount,
                    contract.getStatus(),
                    progress
            );
            response.add(contractResponse);
        }
        return response;
    }

    // 계약서 상세 조회
    @Override
    public ContractDetailResponse getContractDetail(Long contractId) {
        // contractId로 Contract와 ContractDetails 동시에 가져오기
        Contract contract = contractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new RuntimeException("해당 계약을 찾을 수 없습니다."));
        ContractDetails contractDetails = contract.getContractDetails();

        // Mock 대체 외부 API 호출하여 스타트업 정보 가져오기
        Startup startupMock = StartupMock.createMockStartup();
        StartupDetails startupDetailsMock = StartupDetailsMock.createMockStartupDetails();

        // NULL인 필드 처리
        LocalDateTime contractBeginAt = (contract.getSignedAt() != null) ? contract.getSignedAt() : null;
        LocalDateTime contractEndAt = (contractBeginAt != null)
                ? contract.getSignedAt().plusMonths(startupDetailsMock.getContractPeriod())
                : null;
        Double roi = (startupDetailsMock.getRoi() != null) ? startupDetailsMock.getRoi() : null;
        Double returnToken = (roi != null)
                ? contractDetails.getTokenAmount() * (1 + roi / 100)
                : null;

        // ContractDetailResponseDto 생성 및 반환
        return new ContractDetailResponse(
                contract.getStatus().toString(),
                contractDetails.getContractAt(),
                contractBeginAt,
                contractEndAt,
                contractDetails.getTokenAmount(),
                returnToken,
                startupMock.getName(),
                startupMock.getProgress(),
                roi,
                startupMock.getLogoUrl(),
                startupDetailsMock.getDescription(),
                startupMock.getCategory(),
                startupDetailsMock.getInvestmentStatus(),
                startupDetailsMock.getInvestmentRound(),
                startupDetailsMock.getExpectedRoi(),
                startupMock.getLogoUrl(),
                contractDetails.getImgUrl()
        );
    }
}
