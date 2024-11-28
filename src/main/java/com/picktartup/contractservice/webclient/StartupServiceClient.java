package com.picktartup.contractservice.webclient;

import com.picktartup.contractservice.dto.BaseResponse;
import com.picktartup.contractservice.dto.StartupResponse;
import com.picktartup.contractservice.exception.BusinessException;
import com.picktartup.contractservice.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupServiceClient {
    private final WebClient startupServiceWebClient;

    public Mono<StartupResponse> getStartupInfo(Long startupId) {
        return startupServiceWebClient.get()
                .uri("/api/v1/startups/" + startupId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                    if (clientResponse.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new BusinessException(ErrorCode.STARTUP_NOT_FOUND));
                    }
                    return Mono.error(new BusinessException(ErrorCode.STARTUP_SERVICE_ERROR));
                })
                .onStatus(status -> status.is5xxServerError(), clientResponse ->
                        Mono.error(new BusinessException(ErrorCode.STARTUP_SERVICE_ERROR)))
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<StartupResponse>>() {
                })
                .map(response -> {
                    if (response == null || response.getData() == null) {
                        throw new BusinessException(ErrorCode.STARTUP_SERVICE_ERROR);
                    }
                    return response.getData();
                });
    }
}