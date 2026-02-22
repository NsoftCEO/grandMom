package ko.dh.goot.common.exception;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
     * JPA 엔티티 조회 실패 시 발생합니다. (404 Not Found)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException e, HttpServletRequest request) {
        log.error("JPA 엔티티 조회 실패: [URI={}] [Message={}]", 
                request.getRequestURI(), 
                e.getMessage());

        // ErrorCode.ENTITY_NOT_FOUND 라는 공용 코드가 있다고 가정 (status: 404)
        return ResponseEntity
                .status(ErrorCode.ENTITY_NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.ENTITY_NOT_FOUND, e.getMessage()));
    }
    
    /**
     * DB 제약 조건 위반 시 발생합니다. (400 Bad Request)
     * 중복 키, 외래키 위반 등을 포괄하며 입력값이 올바르지 않음을 알립니다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.error("데이터 무결성 위반 발생: [URI={}] [Detail={}]", 
                request.getRequestURI(), 
                e.getMessage());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "입력 데이터가 유효하지 않거나 제약 조건을 위반했습니다."));
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
