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

    // order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "주문이 존재하지 않습니다."),
    ORDER_INVALID_STATUS(HttpStatus.BAD_REQUEST, "O002", "주문 상태가 올바르지 않습니다."),
    ORDER_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "O003", "주문 생성을 실패했습니다."),
    ORDER_STATUS_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "O003", "주문 상태 변경을 실패했습니다."),
    
    // orderItem
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "OI001", "주문상품이 존재하지 않습니다."),
    
    
    // product
    PRODUCT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품 옵션이 존재하지 않습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "P002", "상품 재고가 부족합니다."),

    // payment
    PAYMENT_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "PAY001", "결제 요청이 유효하지 않습니다."),
	PG_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY002", "결제가 존재하지 않습니다."),
	PAYMENT_AMOUNT_MISMATCH(HttpStatus.NOT_FOUND, "PAY003", "결제 금액이 일치하지 않습니다.")
	
	
	
	
	
	;	
    private final HttpStatus status;
    private final String code;
    private final String message;
}
