package ko.dh.goot.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류입니다."),

    // auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A003", "접근 권한이 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A004", "토큰이 만료되었습니다."),
	
    // order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문이 존재하지 않습니다."),
    ORDER_INVALID_STATUS(HttpStatus.BAD_REQUEST, "O002", "주문 상태가 올바르지 않습니다."),
    ORDER_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "O003", "주문 생성을 실패했습니다."),
    ORDER_STATUS_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "O004", "주문 상태 변경을 실패했습니다."),
    
    // orderItem
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "OI001", "주문상품이 존재하지 않습니다."),
    
    
    // product
    PRODUCT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품 옵션이 존재하지 않습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P002", "상품 재고가 부족합니다."),

    // payment
    PAYMENT_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "PAY001", "결제 요청이 유효하지 않습니다."),
	PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY002", "결제가 존재하지 않습니다."),	
	PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAY003", "결제 금액이 일치하지 않습니다."),
	
	/* ================= Webhook ================= */
    WEBHOOK_SIGNATURE_INVALID(HttpStatus.BAD_REQUEST, "WH001", "웹훅 시그니처 검증 실패"),
    WEBHOOK_TIMESTAMP_EXPIRED(HttpStatus.BAD_REQUEST, "WH002", "웹훅 타임스탬프 만료"),
    WEBHOOK_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "WH003", "웹훅 요청 형식 오류"),

    /* ================= PG 통신 ================= */
    PG_API_FAILED(HttpStatus.BAD_GATEWAY, "PG001", "PG 서버 호출 실패"),
    PG_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "PG002", "PG 서버 응답 지연"),

    /* ================= PG 응답 ================= */
    PG_EMPTY_RESPONSE(HttpStatus.BAD_GATEWAY, "PG010", "PG 응답 바디 없음"),
    PG_PARSE_FAILED(HttpStatus.BAD_GATEWAY, "PG011", "PG 응답 JSON 파싱 실패"),
    PG_INVALID_DATA(HttpStatus.BAD_GATEWAY, "PG012", "PG 응답 데이터 구조 오류"),
    PG_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "PG013", "PG 응답 오류"),

    /* ================= PG 비즈니스 ================= */
    PG_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PG020", "PG 결제 정보 없음");
	
	
	
	;	
    private final HttpStatus status;
    private final String code;
    private final String message;
}
