package ko.dh.goot.common.exception;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 표준화된 에러 응답
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private String code;            // 애플리케이션 수준 에러 코드 (ex: ORDER_NOT_FOUND)
    private String message;         // 사용자에게 보여줄 메시지 (간단히)
    private String traceId;         // 요청 추적용 ID (운영 시 필수)
    private String path;            // 요청 URI
    private List<FieldErrorResponse> errors; // 필드별 상세 에러 (validation)

    // ErrorCode
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    // ErrorCode + traceId
    public static ErrorResponse of(ErrorCode errorCode, String traceId) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .traceId(traceId)
                .build();
    }

    // ErrorCode + message + traceId + path
    public static ErrorResponse of(ErrorCode errorCode, String message, String traceId, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .code(errorCode.getCode())
                .message(message)
                .traceId(traceId)
                .path(path)
                .build();
    }

    // ErrorCode + validation errors + traceId
    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorResponse> errors, String traceId) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .traceId(traceId)
                .errors(errors)
                .build();
    }
}