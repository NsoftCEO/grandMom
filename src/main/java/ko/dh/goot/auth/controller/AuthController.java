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

import jakarta.validation.Valid;
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
    

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        
        authService.signup(request);
        return ResponseEntity.ok().build();
    }

    /**
     * ✅ 일반 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return createAuthResponse(tokenResponse);
    }

    /**
     * ✅ 토큰 재발급 (쿠키에서 Refresh Token을 읽어옴)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).body("Refresh token is missing");
        }

        TokenResponse tokenResponse = authService.refreshToken(refreshToken);
        return createAuthResponse(tokenResponse);
    }

    /**
     * ✅ 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // 쿠키 삭제를 위해 빈 쿠키 설정
        ResponseCookie emptyCookie = jwtProvider.createEmptyRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, emptyCookie.toString())
                .body(Map.of("message", "Successfully logged out"));
    }

    /**
     * [공통 유틸] Access Token은 Body로, Refresh Token은 쿠키로 분리하여 응답
     */
    private ResponseEntity<?> createAuthResponse(TokenResponse tokenResponse) {
        // 1. RT를 쿠키로 생성
        ResponseCookie refreshCookie = jwtProvider.createRefreshTokenCookie(tokenResponse.getRefreshToken());

        // 2. 프론트엔드에게는 RT를 숨기고 AT만 전달하기 위한 응답 맵 구성
        Map<String, Object> responseBody = Map.of(
                "accessToken", tokenResponse.getAccessToken(),
                "accessTokenExpiresIn", tokenResponse.getAccessTokenExpiresIn()
        );

        // 3. Header에 쿠키 삽입 후 Body 리턴
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(responseBody);
    }
}