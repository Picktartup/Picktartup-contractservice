package com.picktartup.contractservice.webclient;

import com.picktartup.contractservice.dto.BaseResponse;
import com.picktartup.contractservice.dto.UserDto;
import com.picktartup.contractservice.exception.BusinessException;
import com.picktartup.contractservice.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {
    private final WebClient userServiceWebClient;

    public Mono<UserDto.UserInfo> getUserInfo(Long userId) {
        return userServiceWebClient.get()
                .uri("/api/v1/users/auth/" + userId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                    if (clientResponse.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND));
                    }
                    return Mono.error(new BusinessException(ErrorCode.USER_SERVICE_ERROR));
                })
                .onStatus(status -> status.is5xxServerError(), clientResponse ->
                        Mono.error(new BusinessException(ErrorCode.USER_SERVICE_ERROR)))
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<UserDto.UserInfo>>() {
                })
                .map(response -> {
                    if (response == null || response.getData() == null) {
                        throw new BusinessException(ErrorCode.USER_SERVICE_ERROR);
                    }
                    return UserDto.UserInfo.builder()
                            .username(response.getData().getUsername())
                            .walletAddress(response.getData().getWalletAddress())
                            .build();
                });
    }

    public UserDto.ValidationResponse validateUserExists(Long userId) {
        try {
            BaseResponse<UserDto.ValidationResponse> response = userServiceWebClient.get()
                    .uri("/api/v1/users/public/" + userId + "/validation")
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                        if (clientResponse.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new BusinessException(ErrorCode.USER_NOT_FOUND));
                        }
                        return Mono.error(new BusinessException(ErrorCode.USER_SERVICE_ERROR));
                    })
                    .onStatus(status -> status.is5xxServerError(), clientResponse ->
                            Mono.error(new BusinessException(ErrorCode.USER_SERVICE_ERROR)))
                    .bodyToMono(new ParameterizedTypeReference<BaseResponse<UserDto.ValidationResponse>>() {
                    })
                    .block();

            if (response == null || response.getData() == null) {
                throw new BusinessException(ErrorCode.USER_SERVICE_ERROR);
            }

            if (!"ACTIVE".equals(response.getData().getStatus())) {
                throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
            }

            return response.getData();

        } catch (WebClientResponseException e) {
            log.error("User service error: {}", e.getMessage());
            throw new BusinessException(ErrorCode.USER_SERVICE_ERROR);
        } catch (Exception e) {
            log.error("Unexpected error while validating user: {}", e.getMessage());
            throw new BusinessException(ErrorCode.USER_SERVICE_ERROR);
        }
    }

}