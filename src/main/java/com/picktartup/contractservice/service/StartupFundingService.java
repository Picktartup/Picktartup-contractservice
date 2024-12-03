package com.picktartup.contractservice.service;

import com.picktartup.contractservice.contracts.PickenToken;
import com.picktartup.contractservice.contracts.StartupFunding;
import com.picktartup.contractservice.dto.*;
import com.picktartup.contractservice.entity.TokenTransferTransaction;
import com.picktartup.contractservice.entity.TransactionStatus;
import com.picktartup.contractservice.entity.TransactionType;
import com.picktartup.contractservice.exception.BusinessException;
import com.picktartup.contractservice.exception.ErrorCode;
import com.picktartup.contractservice.repository.TokenTransferTransactionRepository;
import com.picktartup.contractservice.utils.TokenUtils;
import com.picktartup.contractservice.webclient.StartupServiceClient;
import com.picktartup.contractservice.webclient.UserServiceClient;
import com.picktartup.contractservice.webclient.WalletServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class StartupFundingService {

    private final Web3j web3j;
    private final Credentials adminCredentials;
    private final String fundingContractAddress;
    private final String tokenContractAddress;
    private final KeystoreService keystoreService;
    private final MailService mailService;
    private final ContractGasProvider gasProvider;
    private final TokenTransferTransactionRepository tokenTransferTransactionRepository;
    private final StartupServiceClient startupServiceClient;
    private final UserServiceClient userServiceClient;
    private final WalletServiceClient walletServiceClient;

    // Campaign 생성
    @Transactional
    public CampaignDto.Create.Response createCampaign(CampaignDto.Create.Request request) {
        try {
            StartupFunding contract = loadFundingContract(adminCredentials);

            // PICKEN -> Wei 단위로 변환
            BigInteger targetAmountInWei = TokenUtils.toWei(request.getTargetAmount());

            // duration을 초 단위로 변환 (request에서는 일 단위로 받음)
            long durationInSeconds = request.getDurationInDays() * 24 * 60 * 60;

            // 컨트랙트 호출 전 로깅
            log.debug("컨트랙트 호출 파라미터 - targetAmount: {} Wei, duration: {} seconds",
                    targetAmountInWei, durationInSeconds);

            TransactionReceipt receipt = contract.createCampaign(
                    request.getName(),
                    request.getDescription(),
                    request.getStartupWallet(),
                    targetAmountInWei,  // Wei 단위로 변환된 값 사용
                    BigInteger.valueOf(durationInSeconds)
            ).send();

            List<StartupFunding.CampaignCreatedEventResponse> events =
                    contract.getCampaignCreatedEvents(receipt);

            if (events.isEmpty()) {
                throw new BusinessException(ErrorCode.CAMPAIGN_CREATION_FAILED);
            }

            StartupFunding.CampaignCreatedEventResponse event = events.get(0);

            // Wei -> PICKEN 단위로 변환하여 로깅
            log.info("캠페인 생성 완료 - campaignId: {}, targetAmount: {} PICKEN, txHash: {}",
                    event.campaignId,
                    TokenUtils.fromWei(event.targetAmount),
                    receipt.getTransactionHash());

            //campaignid startupdb 에 저장
            if (event.campaignId != null) {
                Integer campaignIdInt = event.campaignId.intValueExact();

                startupServiceClient.saveCampaignId(request.getStartupId(), campaignIdInt)
                        .subscribe(
                                unused -> log.info("Successfully saved campaign ID {} for startup {}",
                                        event.campaignId, request.getStartupId()),
                                throwable -> log.error("Failed to save campaign ID for startup {}",
                                        request.getStartupId(), throwable)
                        );
            } else {
                log.error("Campaign ID is null");
            }

            return CampaignDto.Create.Response.builder()
                    .campaignId(event.campaignId.intValue())
                    .name(request.getName())
                    .description(request.getDescription())
                    .startupWallet(request.getStartupWallet())
                    .targetAmount(request.getTargetAmount())  // 원래 PICKEN 단위 유지
                    .startTime(Instant.ofEpochSecond(event.startTime.longValue())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime())
                    .endTime(Instant.ofEpochSecond(event.endTime.longValue())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime())
                    .transactionHash(receipt.getTransactionHash())
                    .build();

        } catch (Exception e) {
            log.error("캠페인 생성 실패 - name: {}, targetAmount: {} PICKEN",
                    request.getName(), request.getTargetAmount(), e);
            throw new BusinessException(ErrorCode.CAMPAIGN_CREATION_FAILED, e.getMessage());
        }
    }

    @Transactional
    public CampaignDto.Investment.Response invest(
            Long startupId,
            CampaignDto.Investment.Request request) {

        // 스타트업 정보 조회
        StartupResponse startupInfo = startupServiceClient.getStartupInfo(startupId)
                .blockOptional()
                .orElseThrow(() -> new BusinessException(ErrorCode.STARTUP_NOT_FOUND));

        Long campaignId = startupInfo.getCampaign_id().longValue();

        log.info("투자 시작 - userId: {}, campaignId: {}, amount: {} PICKEN",
                request.getUserId(), startupInfo.getCampaign_id(), request.getAmount());

        // 캠페인 상태 확인
        CampaignDto.Status.Response campaignStatus = getCampaignStatus(campaignId);
        validateCampaignStatus(campaignStatus);

        // 투자자 지갑 정보 조회 및 자격증명
        WalletDto.WalletInfo investorWallet = findAndValidateInvestorWallet(request.getUserId());
        log.info("조회된 투자자 지갑 주소 조회: {} ", investorWallet.getAddress());
        log.info("조회된 투자자 키스토어 정보 조회: {} ", investorWallet.getKeystoreFilename());
        Credentials investorCredentials = loadInvestorCredentials(
                investorWallet,
                request.getWalletPassword()
        );

        // 트랜잭션 기록 생성
        TokenTransferTransaction transaction = TokenTransferTransaction.builder()
                .userId(request.getUserId())
                .campaignId(campaignId)
                .walletAddress(investorWallet.getAddress())
                .tokenAmount(BigDecimal.valueOf(request.getAmount()))
                .status(TransactionStatus.PENDING)
                .type(TransactionType.INVESTMENT)
                .build();

        TokenTransferTransaction savedTransaction = tokenTransferTransactionRepository.save(transaction);

        //토큰 승인 및 투자 실행(smart-contract)
        try {
            BigInteger amountInWei = TokenUtils.toWei(request.getAmount());
            log.debug("토큰 승인 요청 - amount: {} Wei", amountInWei);

            validateAndApproveTokens(investorCredentials, amountInWei);

            StartupFunding contract = loadFundingContract(investorCredentials);
            log.debug("투자 실행 - campaignId: {}, amount: {} Wei",
                    campaignId, amountInWei);

            TransactionReceipt receipt = executeInvestment(
                    contract,
                    campaignId,
                    amountInWei
            );

            //투자 성공 시 이벤트 조회
            List<StartupFunding.InvestmentMadeEventResponse> events =
                    contract.getInvestmentMadeEvents(receipt);

            if (events.isEmpty()) {
                // 트랜잭션 실패 처리
                updateTransactionFailure(savedTransaction, "Investment event not found");
                throw new BusinessException(ErrorCode.INVESTMENT_FAILED, "Investment event not found");
            }

            StartupFunding.InvestmentMadeEventResponse event = events.get(0);

            Double totalRaisedPICKEN = TokenUtils.fromWei(event.totalRaised);

            updateTransactionSuccess(savedTransaction, receipt.getTransactionHash());

            // CEO 정보 조회 후 투자 알림 이메일 알림
            UserDto.ValidationResponse ceoInfo = userServiceClient.validateUserExists(startupInfo.getCeo_user_id());

            sendInvestmentNotification(
                    ceoInfo.getEmail(),
                    request.getAmount(),
                    investorWallet.getAddress(),
                    campaignId,
                    totalRaisedPICKEN
            );

            log.info("투자 완료 - userId: {}, campaignId: {}, amount: {} PICKEN, totalRaised: {} PICKEN, txHash: {}",
                    request.getUserId(),
                    campaignId,
                    request.getAmount(),
                    totalRaisedPICKEN,
                    receipt.getTransactionHash());

            return CampaignDto.Investment.Response.builder()
                    .campaignId(campaignId)
                    .investorAddress(investorWallet.getAddress())
                    .amount(request.getAmount())  // 원래 PICKEN 단위 유지
                    .totalRaised(totalRaisedPICKEN)  // Wei에서 PICKEN으로 변환
                    .transactionHash(receipt.getTransactionHash())
                    .build();

        } catch (Exception e) {
            // 트랜잭션 실패 처리
            updateTransactionFailure(savedTransaction, e.getMessage());

            log.error("투자 실패 - userId: {}, campaignId: {}, amount: {} PICKEN",
                    request.getUserId(), campaignId, request.getAmount(), e);
            throw new BusinessException(ErrorCode.INVESTMENT_FAILED, e.getMessage());
        }
    }

    // 트랜잭션 성공 처리
    private void updateTransactionSuccess(TokenTransferTransaction transaction, String txHash) {
        transaction.setTransactionHash(txHash);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        tokenTransferTransactionRepository.save(transaction);
    }

    // 트랜잭션 실패 처리
    private void updateTransactionFailure(TokenTransferTransaction transaction, String errorMessage) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(errorMessage);
        tokenTransferTransactionRepository.save(transaction);
    }

    // 캠페인 상태 조회
    public CampaignDto.Status.Response getCampaignStatus(Long campaignId) {
        try {
            StartupFunding contract = loadFundingContract(createReadOnlyCredentials());

            Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> status =
                    contract.getCampaignStatus(BigInteger.valueOf(campaignId)).send();

            return CampaignDto.Status.Response.builder()
                    .campaignId(campaignId)
                    .status(convertToCampaignStatus(status.component1().intValue()))
                    .startTime(Instant.ofEpochSecond(status.component2().longValue())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime())
                    .endTime(Instant.ofEpochSecond(status.component3().longValue())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime())
                    .timeRemaining(String.valueOf(Duration.ofSeconds(status.component4().longValue())))
                    .build();

        } catch (Exception e) {
            log.error("캠페인 상태 조회 실패 - campaignId: {}", campaignId, e);
            throw new BusinessException(ErrorCode.CAMPAIGN_STATUS_CHECK_FAILED, e.getMessage());
        }
    }

    // 환불 처리
    @Transactional
    public CampaignDto.Refund.Response refund(Long campaignId, Long userId) {
        log.info("환불 시작 - userId: {}, campaignId: {}", userId, campaignId);

        WalletDto.WalletInfo investorWallet = findAndValidateInvestorWallet(userId);

        try {
            StartupFunding contract = loadFundingContract(
                    loadInvestorCredentials(investorWallet, "")
            );

            TransactionReceipt receipt = contract.refund(
                    BigInteger.valueOf(campaignId)
            ).send();

            List<StartupFunding.RefundProcessedEventResponse> events =
                    contract.getRefundProcessedEvents(receipt);

            if (events.isEmpty()) {
                throw new BusinessException(ErrorCode.REFUND_FAILED);
            }

            StartupFunding.RefundProcessedEventResponse event = events.get(0);

            log.info("환불 완료 - userId: {}, campaignId: {}, amount: {}",
                    userId, campaignId, event.amount);

            return CampaignDto.Refund.Response.builder()
                    .campaignId(campaignId)
                    .investorAddress(investorWallet.getAddress())
                    .amount(event.amount.doubleValue())
                    .transactionHash(receipt.getTransactionHash())
                    .build();

        } catch (Exception e) {
            log.error("환불 실패 - userId: {}, campaignId: {}", userId, campaignId, e);
            throw new BusinessException(ErrorCode.REFUND_FAILED, e.getMessage());
        }
    }

    // Private helper methods
    private void validateCampaignStatus(CampaignDto.Status.Response status) {
        if (status.getStatus() != CampaignDto.CampaignStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_CAMPAIGN_STATUS);
        }

        if (LocalDateTime.now().isBefore(status.getStartTime())) {
            throw new BusinessException(ErrorCode.CAMPAIGN_NOT_STARTED);
        }

        if (LocalDateTime.now().isAfter(status.getEndTime())) {
            throw new BusinessException(ErrorCode.CAMPAIGN_ENDED);
        }
    }

    private CampaignDto.CampaignStatus convertToCampaignStatus(int statusCode) {
        return switch (statusCode) {
            case 0 -> CampaignDto.CampaignStatus.ACTIVE;
            case 1 -> CampaignDto.CampaignStatus.SUCCESSFUL;
            case 2 -> CampaignDto.CampaignStatus.FAILED;
            case 3 -> CampaignDto.CampaignStatus.CANCELLED;
            default -> throw new BusinessException(ErrorCode.INVALID_CAMPAIGN_STATUS);
        };
    }

    public CampaignDto.Detail.Response getCampaignDetails(Long campaignId) {
        log.info("캠페인 상세 조회 시작 - campaignId: {}", campaignId);

        try {
            StartupFunding contract = loadFundingContract(createReadOnlyCredentials());

            Tuple3<BigInteger, BigInteger, BigInteger> balance =
                    contract.getCampaignBalance(BigInteger.valueOf(campaignId)).send();

            log.info("캠페인 상세 조회 완료 - campaignId: {}, currentBalance: {}",
                    campaignId, balance.component2());

            return CampaignDto.Detail.Response.builder()
                    .campaignId(campaignId)
                    .targetAmount(balance.component1().doubleValue())
                    .currentBalance(balance.component2().doubleValue())
                    .remainingAmount(balance.component3().doubleValue())
                    .build();

        } catch (Exception e) {
            log.error("캠페인 상세 조회 실패 - campaignId: {}", campaignId, e);
            throw new BusinessException(
                    ErrorCode.CAMPAIGN_DETAIL_FETCH_FAILED,
                    String.format("캠페인 상세 조회 중 오류 발생 - campaignId: %d", campaignId),
                    e
            );
        }
    }

    public CampaignDto.Investor.StatusResponse getInvestorStatus(Long campaignId, Long userId) {
        log.info("투자자 상태 조회 시작 - campaignId: {}, userId: {}", campaignId, userId);

        WalletDto.WalletInfo investorWallet = findAndValidateInvestorWallet(userId);

        try {
            StartupFunding contract = loadFundingContract(createReadOnlyCredentials());

            Tuple3<BigInteger, BigInteger, BigInteger> status =
                    contract.getInvestorStatus(
                            BigInteger.valueOf(campaignId),
                            investorWallet.getAddress()
                    ).send();

            log.info("투자자 상태 조회 완료 - campaignId: {}, userId: {}, investedAmount: {}",
                    campaignId, userId, status.component1());

            return CampaignDto.Investor.StatusResponse.builder()
                    .campaignId(campaignId)
                    .investorAddress(investorWallet.getAddress())
                    .investedAmount(status.component1().doubleValue())
                    .campaignTotal(status.component2().doubleValue())
                    .sharePercentage(status.component3().doubleValue())
                    .build();

        } catch (Exception e) {
            log.error("투자자 상태 조회 실패 - campaignId: {}, userId: {}", campaignId, userId, e);
            throw new BusinessException(
                    ErrorCode.INVESTOR_STATUS_FETCH_FAILED,
                    String.format("투자자 상태 조회 중 오류 발생 - campaignId: %d, userId: %d",
                            campaignId, userId),
                    e
            );
        }
    }


    public TokenDto.Balance.Response getTotalHeldTokens() {
        log.info("총 보유 토큰 조회 시작");

        try {
            StartupFunding contract = loadFundingContract(createReadOnlyCredentials());
            BigInteger balance = contract.getTotalHeldTokens().send();

            // BigInteger를 BigDecimal로 변환 (Wei to Ether)
            BigDecimal balanceInEther = Convert.fromWei(new BigDecimal(balance), Convert.Unit.ETHER);

            log.info("총 보유 토큰 조회 완료 - balance: {}", balance);

            return TokenDto.Balance.Response.builder()
                    .address(fundingContractAddress)
                    .balance(balanceInEther)
                    .build();

        } catch (Exception e) {
            log.error("총 보유 토큰 조회 실패", e);
            throw new BusinessException(
                    ErrorCode.BALANCE_CHECK_FAILED,
                    "총 보유 토큰 조회 중 오류 발생",
                    e
            );
        }
    }

    @Transactional
    public CampaignDto.Emergency.Response emergencyWithdraw(
            Long campaignId,
            CampaignDto.Emergency.Request request
    ) {
        log.info("긴급 출금 시작 - campaignId: {}, adminUserId: {}",
                campaignId, request.getAdminUserId());

        try {
            StartupFunding contract = loadFundingContract(adminCredentials);

            TransactionReceipt receipt = contract.emergencyWithdraw(
                    BigInteger.valueOf(campaignId),
                    request.getToAddress()
            ).send();

            log.info("긴급 출금 완료 - campaignId: {}, toAddress: {}, txHash: {}",
                    campaignId, request.getToAddress(), receipt.getTransactionHash());

            return CampaignDto.Emergency.Response.builder()
                    .campaignId(campaignId)
                    .toAddress(request.getToAddress())
                    .transactionHash(receipt.getTransactionHash())
                    .build();

        } catch (Exception e) {
            log.error("긴급 출금 실패 - campaignId: {}", campaignId, e);
            throw new BusinessException(
                    ErrorCode.EMERGENCY_WITHDRAW_FAILED,
                    String.format("긴급 출금 중 오류 발생 - campaignId: %d", campaignId),
                    e
            );
        }
    }

    // Private helper methods
    private StartupFunding loadFundingContract(Credentials credentials) {
        return StartupFunding.load(
                fundingContractAddress,
                web3j,
                credentials,
                gasProvider
        );
    }

    private WalletDto.WalletInfo findAndValidateInvestorWallet(Long userId) {
        log.info("유저 아이디로 지갑 찾기 userid:{}", userId);
        return walletServiceClient.getWalletInfo(userId)
                .blockOptional()
                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
    }

    private Credentials loadInvestorCredentials(WalletDto.WalletInfo wallet, String password) {
        try {
            WalletFile walletFile = keystoreService.getWalletFile(wallet.getKeystoreFilename());
            String privateKey = keystoreService.decryptPrivateKey(walletFile, password);
            return Credentials.create(privateKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("자격 증명 로드 실패 - walletAddress: {}", wallet.getAddress(), e);
            throw new BusinessException(
                    ErrorCode.CONTRACT_INTERACTION_FAILED,
                    "자격 증명 로드 중 오류가 발생했습니다: " + e.getMessage(),
                    e
            );
        }
    }

    private void validateAndApproveTokens(Credentials credentials, BigInteger amountInWei) {
        if (!checkAndApproveTokens(credentials, amountInWei)) {
            throw new BusinessException(ErrorCode.TOKEN_APPROVAL_FAILED);
        }
    }

    private TransactionReceipt executeInvestment(
            StartupFunding contract,
            Long campaignId,
            BigInteger request
    ) throws Exception {
        return contract.invest(
                BigInteger.valueOf(campaignId),
                request
        ).send();
    }

    private BigInteger extractCampaignId(StartupFunding contract, TransactionReceipt receipt) {
        return contract.getCampaignCreatedEvents(receipt)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_CREATION_FAILED))
                .campaignId;
    }

    private Credentials createReadOnlyCredentials() {
        return Credentials.create("0x0");
    }

    private boolean checkAndApproveTokens(Credentials credentials, BigInteger amount) {
        try {
            PickenToken tokenContract = loadTokenContract(credentials);

            // 1. 토큰 잔액 검증
            BigInteger balance = tokenContract.balanceOf(credentials.getAddress()).send();
            if (balance.compareTo(amount) < 0) {
                throw new BusinessException(
                        ErrorCode.INSUFFICIENT_TOKEN_BALANCE,
                        String.format("필요한 토큰: %d PICKEN, 현재 잔액: %d PICKEN",
                                amount,  // 원본 PICKEN 단위
                                TokenUtils.fromWei(balance)  // Wei -> PICKEN 변환
                        )
                );
            }

            // 2. 현재 승인액 확인
            BigInteger allowance = tokenContract.allowance(
                    credentials.getAddress(),
                    fundingContractAddress
            ).send();

            // 3. 필요한 경우에만 승인 진행
            if (allowance.compareTo(amount) < 0) {
                return handleTokenApproval(tokenContract, allowance, amount, credentials);
            }

            return true;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("토큰 승인 처리 중 오류 발생", e);
            throw new BusinessException(
                    ErrorCode.TOKEN_APPROVAL_FAILED,
                    "토큰 승인 처리 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    private boolean handleTokenApproval(
            PickenToken tokenContract,
            BigInteger currentAllowance,
            BigInteger requiredAmount,
            Credentials credentials
    ) throws Exception {
        // 기존 승인액 초기화
        if (currentAllowance.compareTo(BigInteger.ZERO) > 0) {
            TransactionReceipt resetReceipt = tokenContract.approve(
                    fundingContractAddress,
                    BigInteger.ZERO
            ).send();

            if (!resetReceipt.isStatusOK()) {
                throw new BusinessException(
                        ErrorCode.TOKEN_APPROVAL_FAILED,
                        "토큰 승인 초기화 실패"
                );
            }
        }

        // 새로운 승인
        TransactionReceipt approvalReceipt = tokenContract.approve(
                fundingContractAddress,
                requiredAmount
        ).send();

        if (!approvalReceipt.isStatusOK()) {
            throw new BusinessException(
                    ErrorCode.TOKEN_APPROVAL_FAILED,
                    "토큰 승인 실패"
            );
        }

        // 승인 결과 확인
        BigInteger newAllowance = tokenContract.allowance(
                credentials.getAddress(),
                fundingContractAddress
        ).send();

        if (newAllowance.compareTo(requiredAmount) < 0) {
            throw new BusinessException(
                    ErrorCode.TOKEN_APPROVAL_FAILED,
                    "토큰 승인 확인 실패"
            );
        }

        return true;
    }

    private PickenToken loadTokenContract(Credentials credentials) {
        return PickenToken.load(
                tokenContractAddress,
                web3j,
                credentials,
                gasProvider
        );
    }

    public void sendInvestmentNotification(
            String toEmail,
            Double amount,
            String investorAddress,
            Long campaignId,
            Double totalRaised
    ) {
        String title = "[PICKTARTUP] 신규 투자 알림 - ";

        StringBuilder emailContent = new StringBuilder();
        emailContent.append("<!DOCTYPE html>")
                .append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>")
                .append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>")

                // 로고 및 헤더
                .append("<div style='text-align: center; margin-bottom: 30px;'>")
                .append("<h1 style='color: #2C3E50;'>PICKTARTUP</h1>")
                .append("<h2 style='color: #34495E;'>신규 투자 알림</h2>")
                .append("</div>")

                // 인사말
                .append("<p>안녕하세요,<br>")
                .append("귀사의 프로젝트에 새로운 투자가 완료되어 안내드립니다.</p>")

                // 투자 정보 섹션
                .append("<div style='background-color: #F8F9FA; padding: 20px; border-radius: 5px; margin: 20px 0;'>")
                .append("<h3 style='color: #2C3E50; margin-top: 0;'>📊 투자 상세 정보</h3>")
                .append("<table style='width: 100%; border-collapse: collapse;'>")
                .append(String.format("<tr><td style='padding: 8px 0;'><strong>캠페인 ID:</strong></td><td>%d</td></tr>", campaignId))
                .append(String.format("<tr><td style='padding: 8px 0;'><strong>신규 투자 금액:</strong></td><td><span style='color: #27AE60;'>%.2f PICKEN</span></td></tr>", amount))
                .append(String.format("<tr><td style='padding: 8px 0;'><strong>투자자 주소:</strong></td><td><code style='background-color: #E8E8E8; padding: 2px 4px; border-radius: 3px;'>%s</code></td></tr>", investorAddress))
                .append("</table>")
                .append("</div>")

                // 펀딩 현황 섹션
                .append("<div style='background-color: #F8F9FA; padding: 20px; border-radius: 5px; margin: 20px 0;'>")
                .append("<h3 style='color: #2C3E50; margin-top: 0;'>🎯 펀딩 현황</h3>")
                .append("<table style='width: 100%; border-collapse: collapse;'>")
                .append(String.format("<tr><td style='padding: 8px 0;'><strong>총 모집액:</strong></td><td>%.2f PICKEN</td></tr>", totalRaised))
                .append("</table>")
                .append("</div>")

                // 안내사항
                .append("<div style='margin: 20px 0;'>")
                .append("<h3 style='color: #2C3E50;'>💡 안내사항</h3>")
                .append("<ul style='padding-left: 20px;'>")
                .append("<li>상세한 투자 내역은 PICKTARTUP 대시보드에서 확인하실 수 있습니다.</li>")
                .append("<li>투자자와의 소통은 플랫폼 내 메시지 기능을 이용해 주시기 바랍니다.</li>")
                .append("<li>추가 문의사항이 있으시면 고객센터로 연락 부탁드립니다.</li>")
                .append("</ul>")
                .append("</div>")

                // 푸터
                .append("<div style='margin-top: 40px; padding-top: 20px; border-top: 1px solid #E8E8E8; text-align: center; font-size: 0.9em; color: #666;'>")
                .append("<p>본 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해 주시기 바랍니다.<br>")
                .append("© 2024 YageumYageum. All rights reserved.</p>")
                .append("</div>")

                .append("</div>")
                .append("</body></html>");


        mailService.sendEmail(toEmail, title, emailContent.toString()); // HTML 이메일 발송
    }

}