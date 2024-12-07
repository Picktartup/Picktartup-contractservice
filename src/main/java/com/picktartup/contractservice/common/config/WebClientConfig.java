package com.picktartup.contractservice.common.config;

import com.picktartup.contractservice.exception.BusinessException;
import com.picktartup.contractservice.exception.ErrorCode;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Value("${service.user.url}")
    private String userServiceUrl;

    @Value("${service.startup.url}")
    private String startupServiceUrl;

    @Value("${service.wallet.url}")
    private String walletServiceUrl; // Wallet 서비스 URL 추가

    @Bean
    public WebClient userServiceWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)  // 추가
                .filter(loggingFilter())
                .filter(errorHandler())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .followRedirect(true)
                        .responseTimeout(Duration.ofSeconds(20))))  // 추가
                .build();
    }

    @Bean
    public WebClient startupServiceWebClient() {
        return WebClient.builder()
                .baseUrl(startupServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)  // 추가
                .filter(loggingFilter())
                .filter(errorHandler())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .followRedirect(true)
                        .responseTimeout(Duration.ofSeconds(20))))  // 추가
                .build();
    }

    @Bean
    public WebClient walletServiceWebClient() {
        HttpClient httpClient = HttpClient.create()
                .secure(sslSpec -> {
                    try {
                        sslSpec.sslContext(
                                SslContextBuilder.forClient()
                                        .trustManager(InsecureTrustManagerFactory.INSTANCE) // 신뢰할 수 없는 인증서 허용
                                        .build()
                        );
                    } catch (SSLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .followRedirect(true);

        return WebClient.builder()
                .baseUrl(walletServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(loggingFilter()) // 로깅 필터 추가
                .filter(errorHandler()) // 에러 핸들러 추가
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // HttpClient 연결
                .build();
    }

    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is5xxServerError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> Mono.error(new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR)));
            }
            return Mono.just(clientResponse);
        });
    }
}
