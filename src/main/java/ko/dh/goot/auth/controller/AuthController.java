package ko.dh.goot.auth.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ko.dh.goot.auth.dto.ClientMetadata;
import ko.dh.goot.auth.dto.LoginRequest;
import ko.dh.goot.auth.dto.SignupRequest;
import ko.dh.goot.auth.dto.TokenResponse;
import ko.dh.goot.auth.service.AuthService;
import ko.dh.goot.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProvider jwtProvider;
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        authService.signup(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // 💡 헬퍼 메서드를 통해 Record로 한 번에 추출
        ClientMetadata clientMetadata = extractClientMetadata(httpRequest);

        TokenResponse tokenResponse = authService.login(request, clientMetadata);
        return createAuthResponse(tokenResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        
        if (refreshToken == null) {
            return ResponseEntity.status(401).body("Refresh token is missing");
        }

        // 💡 재발급 시에도 동일하게 기기 정보 추출
        ClientMetadata clientMetadata = extractClientMetadata(httpRequest);

        TokenResponse tokenResponse = authService.refreshToken(refreshToken, clientMetadata);
        return createAuthResponse(tokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie emptyCookie = jwtProvider.createEmptyRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, emptyCookie.toString())
                .body(Map.of("message", "Successfully logged out"));
    }

    private ResponseEntity<?> createAuthResponse(TokenResponse tokenResponse) {
        ResponseCookie refreshCookie = jwtProvider.createRefreshTokenCookie(tokenResponse.getRefreshToken());

        Map<String, Object> responseBody = Map.of(
                "accessToken", tokenResponse.getAccessToken(),
                "accessTokenExpiresIn", tokenResponse.getAccessTokenExpiresIn()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(responseBody);
    }
    
    // ==========================================
    // 🛠️ 유틸리티 메서드 
    // ==========================================

    /**
     * Request에서 클라이언트 정보를 추출하여 Record로 반환
     */
    private ClientMetadata extractClientMetadata(HttpServletRequest request) {
        return new ClientMetadata(
                getClientIp(request),
                request.getHeader("User-Agent"),
                request.getHeader("X-Device-Id"),
                request.getHeader("X-Device-Type"),
                request.getHeader("X-Device-Name")
        );
    }

    /**
     * 💡 프록시 환경에서 다중 IP가 들어올 경우 첫 번째(실제 클라이언트) IP만 추출하도록 개선
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim(); 
        }
        return ip;
    }
}