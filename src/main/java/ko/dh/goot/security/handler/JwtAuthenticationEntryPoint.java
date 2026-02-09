package ko.dh.goot.security.handler;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.common.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        ErrorCode errorCode =
                (ErrorCode) request.getAttribute("authError");

        // 토큰이 아예 없는 경우
        if (errorCode == null) {
            errorCode = ErrorCode.UNAUTHORIZED;
        }

        log.warn("Authentication failed. uri={}, errorCode={}, message={}",
                request.getRequestURI(),
                errorCode.name(),
                authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(errorCode);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
