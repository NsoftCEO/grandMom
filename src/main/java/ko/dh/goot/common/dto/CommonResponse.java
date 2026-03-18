package ko.dh.goot.common.dto;

import ko.dh.goot.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access=AccessLevel.PROTECTED)
public class CommonResponse <T> {

	private final String code;
	private final String message;
	private final T data;
	
	public static <T> CommonResponse <T> of (ErrorCode errorCode) {
		return new CommonResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
	}
	
	public static <T> CommonResponse <T> of (ErrorCode errorCode, String Message) {
		return new CommonResponse<>(errorCode.getCode(), Message, null);
	}
	
	public static <T> CommonResponse <T> of (ErrorCode errorCode, T data) {
		return new CommonResponse<>(errorCode.getCode(), errorCode.getMessage(), data);
	}
	
	public T getData() {
		return this.data;
	}
}
