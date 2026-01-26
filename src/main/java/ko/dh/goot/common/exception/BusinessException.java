package ko.dh.goot.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public BusinessException(ErrorCode errorCode, String detailMessage) {
    	super(String.format("%s (%s)", errorCode.getMessage(), detailMessage));
        this.errorCode = errorCode;
    }
    
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(String.format("%s (%s)", errorCode.getMessage(), detailMessage), cause);
        this.errorCode = errorCode;
    }
}
