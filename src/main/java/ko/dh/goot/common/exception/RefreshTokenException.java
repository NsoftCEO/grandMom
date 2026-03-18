package ko.dh.goot.common.exception;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * 토큰 재발급 실패 시 던지는 예외
 * (noRollbackFor 옵션을 위해 별도 클래스로 분리)
 */
public class RefreshTokenException extends BadCredentialsException {

	private static final long serialVersionUID = 1L;

	public RefreshTokenException(String msg) {
        super(msg);
    }
}