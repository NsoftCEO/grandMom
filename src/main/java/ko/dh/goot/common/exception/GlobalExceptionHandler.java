package ko.dh.goot.common.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.security.auth.message.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

	private String traceId() {
	    String traceId = MDC.get("traceId");
	    return (traceId != null) ? traceId : "NO_TRACE";
	}

    private String requestInfo(HttpServletRequest request) {
        if (request == null) return "";
        return String.format("uri=%s, method=%s, query=%s, remote=%s",
                request.getRequestURI(),
                request.getMethod(),
                request.getQueryString(),
                request.getRemoteAddr());
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException e, HttpServletRequest request) {
        String trace = traceId();
        log.warn("[{}] AuthException: {}, msg={}", trace, requestInfo(request), e.getMessage());

        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getStatus())
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, trace));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String trace = traceId();
        ErrorCode errorCode = e.getErrorCode();

        log.warn("[{}] BusinessException: code={}, {}, msg={}",
                trace,
                errorCode.getCode(),
                requestInfo(request),
                e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, trace));
    }

    @ExceptionHandler(WebhookException.class)
    public ResponseEntity<Void> handleWebhookException(WebhookException e, HttpServletRequest request) {
        String trace = traceId();
        ErrorCode errorCode = e.getErrorCode();

        log.warn("[{}] WebhookException: code={}, {}, msg={}",
                trace,
                errorCode.getCode(),
                requestInfo(request),
                e.getMessage());

        // PG에게 바디 없이 상태만 돌려주기 때문에 Void 반환 (기존 의도 유지)
        return ResponseEntity
                .status(errorCode.getStatus())
                .build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException e, HttpServletRequest request) {
        String trace = traceId();

        log.warn("[{}] EntityNotFound: {}, msg={}",
                trace,
                requestInfo(request),
                e.getMessage());

        return ResponseEntity
                .status(ErrorCode.ENTITY_NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.ENTITY_NOT_FOUND, e.getMessage(), trace, request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        String trace = traceId();

        // 데이터 무결성은 원인 파악을 위해 ERROR 레벨 + stacktrace
        log.error("[{}] DataIntegrityViolation: {}, msg={}", trace, requestInfo(request), e.getMessage(), e);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "입력 데이터가 유효하지 않거나 제약 조건을 위반했습니다.", trace, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String trace = traceId();

        List<FieldErrorResponse> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new FieldErrorResponse(err.getField(), err.getDefaultMessage()))
                .collect(Collectors.toList());

        // validation은 INFO/다만 개발환경에서는 DEBUG로 더 자세히 볼 수 있음
        log.info("[{}] ValidationError: {}, errors={}", trace, requestInfo(request), errors);

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errors, trace));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        String trace = traceId();

        // 시스템 예외는 ERROR + stacktrace
        log.error("[{}] Unhandled Exception: {}, msg={}", trace, requestInfo(request), e.getMessage(), e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, trace));
    }
    
    
    @ExceptionHandler({
        BadCredentialsException.class,
        UsernameNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleAuthException(Exception e) {

        log.warn("Login failed: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
    }
    
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            Exception e,
            HttpServletRequest request
    ) {
        String traceId = MDC.get("traceId");

        log.debug("[{}] NoResourceFound: uri={}",
                traceId,
                request.getRequestURI());

        return ResponseEntity
                .status(404)
                .body(ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, traceId));
    }
    
    
    
    
    
    
    
    
}