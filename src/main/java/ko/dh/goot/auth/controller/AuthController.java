package ko.dh.goot.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ko.dh.goot.auth.dto.LoginRequest;
import ko.dh.goot.auth.dto.SignupRequest;
import ko.dh.goot.auth.dto.TokenRefreshRequest;
import ko.dh.goot.auth.dto.TokenResponse;
import ko.dh.goot.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
	
	private final AuthService authService;
    /**
     * 회원가입 API
     */
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청: email={}", request.getEmail());
        authService.signup(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 로그인 API (JWT Access/Refresh 토큰 발급)
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 요청: email={}", request.getEmail());
        
        // 아이디/비밀번호 검증 후 JWT 토큰 DTO 반환
        TokenResponse tokenResponse = authService.login(request);
        
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 토큰 재발급 API (Refresh Token을 이용해 새로운 Access Token 발급)
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        log.info("토큰 재발급 요청");
        
        TokenResponse tokenResponse = authService.refreshToken(request.getRefreshToken());
        
        return ResponseEntity.ok(tokenResponse);
    }
    
    /**
     * 로그아웃 API (Refresh Token 삭제 및 Access Token 블랙리스트 처리 등)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody TokenRefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }
}

