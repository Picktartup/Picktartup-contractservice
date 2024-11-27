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
import com.picktartup.contractservice.mock.WalletMock;
import com.picktartup.contractservice.repository.ContractDetailsRepository;
import com.picktartup.contractservice.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

    private final StartupFundingService startupFundingService;

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

    // 계약서 PDF 생성
    @Override
    public String generatePdf(ContractPdfRequest contractpdfRequest) {
        // user api에 user 정보 요청 (contractRequest.getUserId)
        Users userMock = UserMock.createMockUser();
        Wallet walletMock = WalletMock.createWalletMock();

        // startup api에 startup 정보 요청 (contractRequest.getStartupId)
        Startup startupMock = StartupMock.createMockStartup();
        StartupDetails startupDetailsMock = StartupDetailsMock.createMockStartupDetails();

        // (프리뷰 계약서일 경우) 트랜잭션 해시, 투자자 서명 필드를 기본 텍스트/이미지로 처리
        String transactionHash = (contractpdfRequest.getTransactionHash() != null)
                ? contractpdfRequest.getTransactionHash()
                : "[트랜잭션 해시]";
        String investorSignature = (contractpdfRequest.getInvestorSignature() != null)
                ? contractpdfRequest.getInvestorSignature()
                : "https://contract-image.s3.ap-northeast-2.amazonaws.com/signature/investor_null.png";

        // 계약서 PDF 생성 로직
        // Step 1: Thymeleaf로 HTML 생성 
        Context context = new Context();
        context.setVariable("investorName", userMock.getUsername());
        context.setVariable("investorWallet", walletMock.getAddress());

        context.setVariable("companyName", startupMock.getName());
        context.setVariable("companyAddress", startupDetailsMock.getAddress());
        context.setVariable("ceoName", startupDetailsMock.getCeoName());
        context.setVariable("companyRegistrationNumber", startupDetailsMock.getRegistrationNum());
        context.setVariable("companyWallet", startupMock.getWallet().getAddress());
        context.setVariable("contractPeriod", startupDetailsMock.getContractPeriod());

        context.setVariable("investmentAmount", contractpdfRequest.getAmount());
        context.setVariable("contractAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        context.setVariable("transactionHash", transactionHash);
        context.setVariable("investorSignatureUrl", investorSignature);
        context.setVariable("companySignatureUrl", startupDetailsMock.getSignature());

        String htmlContent = templateEngine.process("contract-template", context);

        // Step 2: HTML를 PDF로 변환
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        try {
            HtmlConverter.convertToPdf(new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8)), pdfOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] pdfBytes = pdfOutputStream.toByteArray();

        // Step 3: S3 버킷에 PDF 업로드
        String bucketName = "contract-image";

        // (프리뷰 계약서일 경우) 파일 이름 앞에 tmp_ 추가
        String prefix;
        if (!StringUtils.hasText(contractpdfRequest.getTransactionHash())) {
            prefix = "tmp_" + userMock.getUserId() + "_" + startupMock.getStartupId();
        } else {
            prefix = userMock.getUserId() + "_" + startupMock.getStartupId();
        }
        String pdfFileName = "contracts/" + prefix + "_" + System.currentTimeMillis() + ".pdf";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/pdf");
        metadata.setContentLength(pdfBytes.length);

        s3Client.putObject(bucketName, pdfFileName, new ByteArrayInputStream(pdfBytes), metadata);

        // Step 4: S3 버킷에 생성된 계약서 URL 가져오기
        URL s3Url = s3Client.getUrl(bucketName, pdfFileName);
        return s3Url.toString();
    }

    // 투자 등록
    @Override
    public ContractResponse createContract(ContractRequest contractRequest) {
        // user api에 user 정보 요청 (contractRequest.getUserId)
        Users userMock = UserMock.createMockUser();

        // startup api에 startup 정보 요청 (contractRequest.getStartupId)
        Startup startupMock = StartupMock.createMockStartup();

        // 스마트 컨트랙트 로직
        Long campaignId = startupMock.getCampaignId();
        CampaignDto.Investment.Request investRequest = CampaignDto.Investment.Request.builder()
                        .userId(userMock.getUserId())
                        .walletPassword(contractRequest.getWalletPassword())
                        .amount(contractRequest.getAmount())
                        .build();
        CampaignDto.Investment.Response investResponse = startupFundingService.invest(campaignId, investRequest);

        // Contract 등록
        Contract contract = new Contract();
        contract.setStatus(ContractStatus.BEGIN);
        contract.setStartupId(contractRequest.getStartupId());
        contract.setUserId(contractRequest.getUserId());
        contract = contractRepository.save(contract);

        // 최종 계약서 PDF 생성
        ContractPdfRequest contractPdfRequest = ContractPdfRequest.builder()
                        .userId(contractRequest.getUserId())
                        .startupId(contractRequest.getStartupId())
                        .amount(contractRequest.getAmount())
                        .investorSignature(contractRequest.getInvestorSignature())
                        .transactionHash(investResponse.getTransactionHash())
                        .build();
        String s3Url = generatePdf(contractPdfRequest);

        // ContractDetails 등록
        ContractDetails contractDetails = generateCDs(contractRequest, contract);
        contractDetails.setImgUrl(s3Url);
        contractDetailsRepository.save(contractDetails);

        // 프리뷰 계약서 삭제 : S3 버킷에서 이름이 tmp_로 시작하는 파일을 삭제
        String bucketName = "contract-image";
        deletePreviewPdf(bucketName, userMock.getUserId().toString(), startupMock.getStartupId().toString());

        return new ContractResponse(s3Url);
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

            // startup api에 startup 정보 요청
            Startup startupMock = StartupMock.createMockStartup();
            StartupDetails startupDetailsMock = StartupDetailsMock.createMockStartupDetails();

            // NULL일 가능성이 있는 필드 처리
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
                contractDetails.getImgUrl()
        );
    }

    // 투자 등록 시 ContractDetails 객체를 생성하는 메서드
    private static ContractDetails generateCDs(ContractRequest contractRequest, Contract contract) {
        ContractDetails contractDetails = new ContractDetails();

        contractDetails.setContract(contract);
        contractDetails.setTokenAmount(contractRequest.getAmount());
        contractDetails.setContractAt(LocalDateTime.now());

        return contractDetails;
    }

    // S3 버킷에서 프리뷰 계약서 PDF를 삭제하는 메서드
    private void deletePreviewPdf(String bucketName, String userId, String startupId) {
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
}
