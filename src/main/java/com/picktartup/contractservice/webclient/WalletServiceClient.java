package com.picktartup.contractservice.webclient;

import com.picktartup.contractservice.dto.BaseResponse;
import com.picktartup.contractservice.dto.WalletDto;
import com.picktartup.contractservice.exception.BusinessException;
import com.picktartup.contractservice.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletServiceClient {
    private final WebClient walletServiceWebClient;

    @CircuitBreaker(name = "walletService", fallbackMethod = "getWalletInfoFallback")
    public Mono<WalletDto.WalletInfo> getWalletInfo(Long userId) {
        return walletServiceWebClient.get()
                .uri("/api/v1/wallets/user/" + userId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                    if (clientResponse.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new BusinessException(ErrorCode.WALLET_NOT_FOUND));
                    }
                    return Mono.error(new BusinessException(ErrorCode.WALLET_SERVICE_ERROR));
                })
                .onStatus(status -> status.is5xxServerError(), clientResponse ->
                        Mono.error(new BusinessException(ErrorCode.WALLET_SERVICE_ERROR)))
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<WalletDto.WalletInfo>>() {})
                .map(response -> {
                    if (response == null || response.getData() == null) {
                        throw new BusinessException(ErrorCode.WALLET_SERVICE_ERROR);
                    }
                    return response.getData();
                })
                .doOnError(e -> log.error("지갑 정보 조회 실패 - userId: {}", userId, e));
    }

    private Mono<WalletDto.WalletInfo> getWalletInfoFallback(Long userId, Exception ex) {
        log.warn("지갑 정보 조회 폴백 호출 - userId: {}", userId, ex);
        throw new BusinessException(ErrorCode.WALLET_SERVICE_ERROR);
    }


}
