package ko.dh.goot.auth.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ko.dh.goot.auth.dao.RefreshTokenMapper;
import ko.dh.goot.auth.dao.RefreshTokenRepository;
import ko.dh.goot.auth.domain.RefreshToken;
import ko.dh.goot.auth.domain.UserRole;
import ko.dh.goot.auth.dto.LoginRequest;
import ko.dh.goot.auth.dto.SignupRequest;
import ko.dh.goot.auth.dto.TokenResponse;
import ko.dh.goot.auth.oauth.OAuthUserInfo;
import ko.dh.goot.common.exception.RefreshTokenException;
import ko.dh.goot.security.jwt.JwtProvider;
import ko.dh.goot.security.jwt.TokenHasher;
import ko.dh.goot.user.dao.UserRepository;
import ko.dh.goot.user.domain.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final OAuthService oAuthService;
    private final RefreshTokenMapper refreshTokenMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHasher tokenHasher;

    /**
     * ✅ 회원가입 (LOCAL)
     */
    @Transactional
    public void signup(SignupRequest request) {

        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("email already exists");
        });

        User user = User.builder()                
                .name(request.getName() == null ? "no-name" : request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(UserRole.ROLE_USER)
                .status("ACTIVE")
                .loginType("LOCAL")
                .provider(null)
                .providerId(null)               
                .build();

        userRepository.save(user);
    }

    /**
     * ✅ 로컬 로그인
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + email));

        // ✅ 상태 체크
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BadCredentialsException("inactive user");
        }

        // ✅ 마지막 로그인 갱신
        user.updateLastLogin();

        return issueTokens(user);
    }

    /**
     * ✅ 소셜 로그인
     */
    @Transactional
    public TokenResponse socialLogin(String provider, String accessTokenFromClient) {
        OAuthUserInfo userInfo = oAuthService.getUserInfo(provider, accessTokenFromClient);

        User user = userRepository.findByEmail(userInfo.getEmail())
                .or(() -> userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId()))
                .orElseGet(() -> register(userInfo));

        if (!"ACTIVE".equals(user.getStatus())) throw new BadCredentialsException("inactive user");

        user.updateLastLogin();
        return issueTokens(user); // 🔥 로직 공통화
    }

    /**
     * ✅ 소셜 회원가입
     */
    @Transactional
    protected User register(OAuthUserInfo userInfo) {
        User user = User.builder()
                .email(userInfo.getEmail() != null ? userInfo.getEmail() : userInfo.getProvider() + "_" + userInfo.getProviderId())
                .name(userInfo.getName() == null ? "no-name" : userInfo.getName())
                .loginType("SOCIAL")
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .role(UserRole.ROLE_USER)
                .status("ACTIVE")
                .build();
        return userRepository.save(user);
    }

    /**
     * ✅ 토큰 재발급 (보안 강화)
     */
    @Transactional(noRollbackFor = {RefreshTokenException.class}) // 🔥 특정 예외 시 롤백 방지
    public TokenResponse refreshToken(String refreshToken) {
        String hashedToken = tokenHasher.hash(refreshToken);

        RefreshToken rt = refreshTokenMapper.findByToken(hashedToken)
                .orElseThrow(() -> new RefreshTokenException("invalid refresh token"));

        // 만료/폐기 체크 시 즉시 해당 유저의 모든 토큰 삭제 (RTR 및 보안 정책)
        if (rt.getExpiredAt().isBefore(Instant.now()) || rt.isRevoked()) {
            refreshTokenMapper.deleteByUserId(rt.getUserId()); 
            throw new RefreshTokenException("refresh token expired or revoked");
        }

        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));

        if (!"ACTIVE".equals(user.getStatus())) throw new BadCredentialsException("inactive user");

        return issueTokens(user);
    }

    /**
     * ✅ 로그아웃 (RefreshToken 폐기)
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        refreshTokenMapper.revokeToken(tokenHasher.hash(refreshToken));
    }

    /**
     * ✅ RefreshToken 저장 (단일 토큰 정책)
     */
    private void saveRefreshToken(String userId, String refreshToken) {
        refreshTokenMapper.deleteByUserId(userId); // 기존 토큰 삭제

        RefreshToken rt = RefreshToken.builder()
                .userId(userId)
                .token(tokenHasher.hash(refreshToken))
                .expiredAt(Instant.now().plusMillis(jwtProvider.getRefreshExpireMs()))
                .revoked(false)
                .build();

        refreshTokenMapper.insertToken(rt);
    }

    /**
     * ✅ 내 정보 조회
     */
    public Object me(String accessToken) {

        if (!jwtProvider.validateToken(accessToken)) {
            throw new BadCredentialsException("invalid access token");
        }

        String userId = jwtProvider.getUserId(accessToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + userId));

        return new HashMap<String, Object>() {{
            put("id", user.getUserId());
            put("email", user.getEmail());
            put("name", user.getName());
            put("provider", user.getProvider());
            put("role", user.getRole());
        }};
    }
    
    /**
     * [공통 로직] 신규 AT, RT 발급 및 저장
     */
    private TokenResponse issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken();

        saveRefreshToken(user.getUserId(), refreshToken);

        return TokenResponse.of(accessToken, refreshToken, jwtProvider.getAccessExpireMs());
    }
}