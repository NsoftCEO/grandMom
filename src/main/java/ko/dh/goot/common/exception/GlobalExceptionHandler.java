package ko.dh.goot.common.exception;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.security.auth.message.AuthException;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthException.class)
    public ResponseEntity<?> handleAuth(AuthException e) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", e.getMessage()));
    }
	
    /**
     * 비즈니스 예외 (의도된 실패)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("BusinessException 발생. code={}, message={}",
                errorCode.getCode(), e.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(WebhookException.class)
    public ResponseEntity<Void> handleWebhookException(WebhookException e) {
    	ErrorCode errorCode = e.getErrorCode();
    	
    	log.warn("WebhookException 발생. code={}, message={}",
                errorCode.getCode(), e.getMessage(), e);

    	return ResponseEntity
                .status(errorCode.getStatus())
                .build(); // PG에 리턴이므로 로그기록하고, status만 보내면됨
    }
    
    /**
     * Validation 예외 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {

        List<FieldErrorResponse> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new FieldErrorResponse(
                        err.getField(),
                        err.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        log.warn("ValidationException: {}", errors);

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errors));
    }

    /**
     * 시스템 예외 (예상 못 한 오류)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception", e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
