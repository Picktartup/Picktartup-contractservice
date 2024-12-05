package com.picktartup.contractservice.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picktartup.contractservice.dto.CampaignDto;
import com.picktartup.contractservice.dto.ContractRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ContractLoggingAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Controller 로깅 추가
    @Around("execution(* com.picktartup.contractservice.controller.*.*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        LocalDateTime requestTime = LocalDateTime.now();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", requestTime.format(DateTimeFormatter.ISO_DATE_TIME));
        logData.put("request_id", UUID.randomUUID().toString());
        logData.put("http_method", request.getMethod());
        logData.put("uri", request.getRequestURI());
        logData.put("api_path", request.getRequestURI().replaceAll("/\\d+", "/{id}"));
        logData.put("client_ip", request.getRemoteAddr());
        logData.put("api_name", ((MethodSignature) joinPoint.getSignature()).getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName());

        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            try {
                logData.put("request_params", objectMapper.writeValueAsString(args));
            } catch (Exception e) {
                logData.put("request_params", "Failed to serialize request parameters");
            }
        }

        return logWithCommonData(joinPoint, logData);
    }

    // 캠페인 생성 모니터링
    @Around("execution(* com.picktartup.contractservice.service.StartupFundingService.createCampaign(..))")
    public Object monitorCampaignCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        LocalDateTime createTime = LocalDateTime.now();
        Object[] args = joinPoint.getArgs();
        CampaignDto.Create.Request request = (CampaignDto.Create.Request) args[0];

        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", "campaign_creation");
        logData.put("campaign_name", request.getName());
        logData.put("target_amount", request.getTargetAmount());
        logData.put("timestamp", createTime.format(DateTimeFormatter.ISO_DATE_TIME));
        logData.put("request_id", UUID.randomUUID().toString());

        return logWithCommonData(joinPoint, logData);
    }

    // 투자 진행 모니터링
    @Around("execution(* com.picktartup.contractservice.service.StartupFundingService.invest(..))")
    public Object monitorInvestment(ProceedingJoinPoint joinPoint) throws Throwable {
        LocalDateTime investTime = LocalDateTime.now();
        Object[] args = joinPoint.getArgs();
        Long startupId = (Long) args[0];
        CampaignDto.Investment.Request request = (CampaignDto.Investment.Request) args[1];

        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", "investment");
        logData.put("startup_id", startupId);
        logData.put("user_id", request.getUserId());
        logData.put("amount", request.getAmount());
        logData.put("timestamp", investTime.format(DateTimeFormatter.ISO_DATE_TIME));
        logData.put("request_id", UUID.randomUUID().toString());

        return logWithCommonData(joinPoint, logData);
    }

    // 계약 생성 모니터링
    @Around("execution(* com.picktartup.contractservice.service.StartupFundingService.createContract(..))")
    public Object monitorContractCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        LocalDateTime contractTime = LocalDateTime.now();
        Object[] args = joinPoint.getArgs();
        ContractRequest request = (ContractRequest) args[0];

        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", "contract_creation");
        logData.put("startup_id", request.getStartupId());
        logData.put("user_id", request.getUserId());
        logData.put("amount", request.getAmount());
        logData.put("timestamp", contractTime.format(DateTimeFormatter.ISO_DATE_TIME));
        logData.put("request_id", UUID.randomUUID().toString());

        return logWithCommonData(joinPoint, logData);
    }

    // 공통 로깅 데이터 추가
    private Object logWithCommonData(ProceedingJoinPoint joinPoint, Map<String, Object> logData) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // 성공 로그
            logData.put("response_time_ms", executionTime);
            logData.put("success", true);
            logData.put("http_status", 200);

            // API 호출인 경우에만 응답 데이터 기록
            if (!logData.containsKey("event_type")) {
                try {
                    logData.put("response", objectMapper.writeValueAsString(result));
                } catch (Exception e) {
                    logData.put("response", "Failed to serialize response");
                }
            }

            log.info(objectMapper.writeValueAsString(logData));
            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // 실패 로그
            logData.put("response_time_ms", executionTime);
            logData.put("success", false);
            logData.put("http_status", 500);
            logData.put("error_message", e.getMessage());
            log.error(objectMapper.writeValueAsString(logData));
            throw e;
        }
    }
}