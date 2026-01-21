package ko.dh.goot.common.exception;

import lombok.Getter;

@Getter
public class WebhookException extends RuntimeException {
	
    private final ErrorCode errorCode;

    public WebhookException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public WebhookException(ErrorCode errorCode, String detailMessage) {
    	super(String.format("%s (%s)", errorCode.getMessage(), detailMessage));
        this.errorCode = errorCode;
    }
}
