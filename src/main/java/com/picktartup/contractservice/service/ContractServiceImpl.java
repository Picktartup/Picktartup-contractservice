package com.picktartup.contractservice.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.itextpdf.html2pdf.HtmlConverter;
import com.picktartup.contractservice.dto.*;
import com.picktartup.contractservice.entity.*;
import com.picktartup.contractservice.exception.BusinessException;
import com.picktartup.contractservice.exception.ErrorCode;
import com.picktartup.contractservice.repository.ContractDetailsRepository;
import com.picktartup.contractservice.repository.ContractRepository;
import com.picktartup.contractservice.webclient.StartupServiceClient;
import com.picktartup.contractservice.webclient.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RequiredArgsConstructor
@Service
public class ContractServiceImpl implements ContractService{

    private final StartupFundingService startupFundingService;
    private final UserServiceClient userServiceClient;
    private final StartupServiceClient startupServiceClient;
    private final ContractRepository contractRepository;
    private final ContractDetailsRepository contractDetailsRepository;
    private final TemplateEngine templateEngine;
    private final AmazonS3 s3Client;

    // TODO: 모금 완료되어 계약 최종 체결 시
    // 1) contract.signedAt 컬럼 업데이트 -> now()
    // 2) contract.status 컬럼 업데이트 -> BEGIN to ACTIVE
    // how to: 스마트 컨트랙트 컴파일된 파일에서 해당 함수 import

    // 계약서 PDF 생성
    @Override
    public String generatePdf(ContractPdfRequest contractPdfRequest) {
        // User 정보 조회
        UserDto.UserInfo userInfo = userServiceClient.getUserInfo(contractPdfRequest.getUserId()).block();

        // Startup 정보 조회
        StartupResponse startupInfo = startupServiceClient.getStartupInfo(contractPdfRequest.getStartupId()).block();

        // CEO 지갑 정보 조회
        UserDto.UserInfo ceoInfo = userServiceClient.getUserInfo(startupInfo.getCeoUserId()).block();

        // 프리뷰 계약서 처리 (트랜잭션 해시, 투자자 서명)
        String transactionHash = (contractPdfRequest.getTransactionHash() != null)
                ? contractPdfRequest.getTransactionHash()
                : "[트랜잭션 해시]";
        String investorSignature = (contractPdfRequest.getInvestorSignature() != null)
                ? contractPdfRequest.getInvestorSignature()
                : "https://contract-image.s3.ap-northeast-2.amazonaws.com/signature/investor_null.png";

        // 계약서 PDF 생성 로직
        // Step 1: Thymeleaf로 HTML 생성
        Context context = new Context();
        context.setVariable("investorName", userInfo.getUsername());
        context.setVariable("investorWallet", userInfo.getWalletAddress());
        context.setVariable("companyName", startupInfo.getName());
        context.setVariable("companyAddress", startupInfo.getAddress());
        context.setVariable("ceoName", startupInfo.getCeoName());
        context.setVariable("companyRegistrationNumber", startupInfo.getRegistrationNum());
        context.setVariable("companyWallet", ceoInfo.getWalletAddress());
        context.setVariable("contractPeriod", startupInfo.getContractPeriod());
        context.setVariable("investmentAmount", contractPdfRequest.getAmount());
        context.setVariable("contractAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        context.setVariable("transactionHash", transactionHash);
        context.setVariable("investorSignatureUrl", investorSignature);
        context.setVariable("companySignatureUrl", startupInfo.getSignature());

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

        // 프리뷰 계약서 파일명 처리
        String prefix;
        if (!StringUtils.hasText(contractPdfRequest.getTransactionHash())) {
            prefix = "tmp_" + contractPdfRequest.getUserId() + "_" + contractPdfRequest.getStartupId();
        } else {
            prefix = contractPdfRequest.getUserId() + "_" + contractPdfRequest.getStartupId();
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

    @Override
    public ContractResponse createContract(ContractRequest contractRequest) {
        // Startup 정보 조회
        StartupResponse startupInfo = startupServiceClient.getStartupInfo(contractRequest.getStartupId()).block();

        // 스마트 컨트랙트 로직
        CampaignDto.Investment.Request investRequest = CampaignDto.Investment.Request.builder()
                .userId(contractRequest.getUserId())
                .walletPassword(contractRequest.getWalletPassword())
                .amount(contractRequest.getAmount())
                .build();
        CampaignDto.Investment.Response investResponse = startupFundingService.invest(Long.valueOf(startupInfo.getCampaignId()), investRequest);

        // Contract 등록
        Contract contract = Contract.builder()
                .status(ContractStatus.BEGIN)
                .startupId(contractRequest.getStartupId())
                .userId(contractRequest.getUserId())
                .build();
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

        // 프리뷰 계약서 삭제
        String bucketName = "contract-image";
        deletePreviewPdf(bucketName, contractRequest.getUserId().toString(), contractRequest.getStartupId().toString());

        return new ContractResponse(s3Url);
    }

    // 계약 상태에 따른 투자 리스트 조회
    @Override
    public List<ContractListResponse> getContractList(Long userId, String contractStatus) {
        // contractStatus 값에 따라 상태 리스트를 설정
        List<ContractStatus> statuses;
        if ("active".equalsIgnoreCase(contractStatus)) {
            statuses = Arrays.asList(ContractStatus.BEGIN, ContractStatus.ACTIVE);
        } else if ("completed".equalsIgnoreCase(contractStatus)) {
            statuses = Arrays.asList(ContractStatus.COMPLETED, ContractStatus.CANCELLED);
        } else {
            throw new BusinessException(ErrorCode.INVALID_CONTRACT_STATUS);
        }

        // 계약 리스트 조회
        List<Contract> contracts = contractRepository.findContractsByUserIdAndStatuses(userId, statuses);

        // 응답 리스트 생성
        List<ContractListResponse> response = new ArrayList<>();
        for (Contract contract : contracts) {
            // 연관된 ContractDetails 조회
            ContractDetails details = contractDetailsRepository.findByContract_ContractId(contract.getContractId());
            if (details == null) {
                continue;
            }

            // Startup 정보 조회
            StartupResponse startupInfo = startupServiceClient.getStartupInfo(contract.getStartupId()).block();

            // ROI와 반환 토큰 계산
            Double returnToken = startupInfo.getRoi() != null && details.getTokenAmount() != null
                    ? details.getTokenAmount() * (1 + startupInfo.getRoi() / 100)
                    : null;

            LocalDateTime contractDate = "active".equalsIgnoreCase(contractStatus)
                    ? details.getContractAt()
                    : contract.getSignedAt().plusMonths(startupInfo.getContractPeriod());

            Double tokenAmount = "active".equalsIgnoreCase(contractStatus)
                    ? details.getTokenAmount()
                    : returnToken;

            Double progress = "active".equalsIgnoreCase(contractStatus)
                    ? Double.valueOf(startupInfo.getProgress())
                    : startupInfo.getRoi();

            // 응답 객체 생성 및 추가
            ContractListResponse contractResponse = ContractListResponse.builder()
                    .contractId(contract.getContractId())
                    .contractDate(contractDate)
                    .startupName(startupInfo.getName())
                    .tokenAmount(tokenAmount)
                    .contractStatus(contract.getStatus())
                    .progress(progress)
                    .build();

            response.add(contractResponse);
        }

        return response;
    }

    // 계약서 상세 조회
    @Override
    public ContractDetailResponse getContractDetail(Long contractId) {
        Contract contract = contractRepository.findByIdWithDetails(contractId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND));

        ContractDetails contractDetails = contract.getContractDetails();
        StartupResponse startupInfo = startupServiceClient.getStartupInfo(contract.getStartupId()).block();

        // NULL인 필드 처리
        LocalDateTime contractBeginAt = contract.getSignedAt();
        LocalDateTime contractEndAt = contractBeginAt != null
                ? contractBeginAt.plusMonths(startupInfo.getContractPeriod())
                : null;

        Double returnToken = startupInfo.getRoi() != null && contractDetails.getTokenAmount() != null
                ? contractDetails.getTokenAmount() * (1 + startupInfo.getRoi() / 100)
                : null;

        // 투자 진행률을 Integer로 변환
        Integer process = null;
        try {
            process = startupInfo.getProgress() != null ? Integer.valueOf(startupInfo.getProgress()) : null;
        } catch (NumberFormatException e) {
            log.error("Failed to parse progress value: {}", startupInfo.getProgress());
        }

        return ContractDetailResponse.builder()
                .contractStatus(contract.getStatus().toString())
                .investAt(contractDetails.getContractAt())
                .contractBeginAt(contractBeginAt)
                .contractEndAt(contractEndAt)
                .investToken(contractDetails.getTokenAmount())
                .returnToken(returnToken)
                .startupName(startupInfo.getName())
                .process(process)
                .roi(startupInfo.getRoi())
                .startupLogo(startupInfo.getLogoUrl())
                .startupDescription(startupInfo.getDescription())
                .startupCategory(startupInfo.getCategory())
                .investStatus(startupInfo.getInvestmentStatus())
                .investRound(null)  // startupInfo에서 제공하지 않는 정보
                .expectedRoi(startupInfo.getExpectedRoi())
                .contractPdfUrl(contractDetails.getImgUrl())
                .build();
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
