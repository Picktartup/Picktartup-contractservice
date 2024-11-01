package com.picktartup.contractservice.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private HttpStatus status;
    private String message;
    private T data;

    // 반환 데이터 없이 성공했을 때
    public static <T> ApiResponse<T> ok() {
        return (ApiResponse<T>) ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("성공")
                .data(null)
                .build();
    }

    // 반환 데이터 있고 성공했을 때
    public static <T> ApiResponse<T> ok(T data) {
        return (ApiResponse<T>) ApiResponse.builder()
                .status(HttpStatus.OK)
                .message("성공")
                .data(data)
                .build();
    }

    // 생성 완료
    public static <T> ApiResponse<T> created(T data) {
        return (ApiResponse<T>) ApiResponse.builder()
                .status(HttpStatus.CREATED)
                .message("생성 완료")
                .data(data)
                .build();
    }

    // 실패했을 때
    public static <T> ApiResponse<T> error(T errorCode) {
        return (ApiResponse<T>) ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message("실패")
                .data(errorCode)
                .build();
    }

}
