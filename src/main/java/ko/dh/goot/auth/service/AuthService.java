// ko/dh/goot/auth/service/AuthService.java
package ko.dh.goot.auth.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ko.dh.goot.auth.dao.RefreshTokenRepository;
import ko.dh.goot.auth.domain.RefreshToken;
import ko.dh.goot.auth.dto.LoginRequest;
import ko.dh.goot.auth.dto.SignupRequest;
import ko.dh.goot.auth.dto.TokenResponse;
import ko.dh.goot.auth.oauth.OAuthUserInfo;
import ko.dh.goot.security.jwt.JwtProvider;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ✅ 회원가입 (LOCAL)
     */
    @Transactional
    public void signup(SignupRequest request) {

        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("email already exists");
        });

        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName() == null ? "no-name" : request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .loginType("LOCAL")
                .provider(null)
                .providerId(null)
                .role("ROLE_USER")
                .status("ACTIVE")
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

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken();

        saveRefreshToken(user.getUserId(), refreshToken);

        return TokenResponse.of(
                accessToken,
                refreshToken,
                jwtProvider.getAccessExpireMs()
        );
    }

    /**
     * ✅ 소셜 로그인
     */
    @Transactional
    public TokenResponse socialLogin(String provider, String accessTokenFromClient) {

        OAuthUserInfo userInfo = oAuthService.getUserInfo(provider, accessTokenFromClient);

        Optional<User> optUser = Optional.empty();

        if (userInfo.getEmail() != null) {
            optUser = userRepository.findByEmail(userInfo.getEmail());
        }

        if (optUser.isEmpty()) {
            optUser = userRepository.findByProviderAndProviderId(
                    userInfo.getProvider(),
                    userInfo.getProviderId()
            );
        }

        User user = optUser.orElseGet(() -> register(userInfo));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BadCredentialsException("inactive user");
        }

        user.updateLastLogin();

        String access = jwtProvider.createAccessToken(user.getUserId(), user.getRole());
        String refresh = jwtProvider.createRefreshToken();

        saveRefreshToken(user.getUserId(), refresh);

        return TokenResponse.of(
                access,
                refresh,
                jwtProvider.getAccessExpireMs()
        );
    }

    /**
     * ✅ 소셜 회원가입
     */
    @Transactional
    protected User register(OAuthUserInfo userInfo) {

        User user = User.builder()
                .email(userInfo.getEmail() != null
                        ? userInfo.getEmail()
                        : userInfo.getProvider() + "_" + userInfo.getProviderId())
                .name(userInfo.getName() == null ? "no-name" : userInfo.getName())
                .loginType("SOCIAL")
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .role("ROLE_USER")
                .status("ACTIVE")
                .build();

        return userRepository.save(user);
    }

    /**
     * ✅ 토큰 재발급
     */
    @Transactional
    public TokenResponse refreshToken(String refreshToken) {

        RefreshToken rt = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("invalid refresh token"));

        // ✅ 만료 or 폐기 체크
        if (rt.getExpiredAt().isBefore(Instant.now()) || rt.isRevoked()) {
            refreshTokenRepository.delete(rt);
            throw new BadCredentialsException("refresh token expired or revoked");
        }

        String userId = rt.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + userId));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BadCredentialsException("inactive user");
        }

        String newAccess = jwtProvider.createAccessToken(user.getUserId(), user.getRole());
        String newRefresh = jwtProvider.createRefreshToken();

        // ✅ 기존 토큰 제거 후 재발급
        refreshTokenRepository.delete(rt);
        saveRefreshToken(user.getUserId(), newRefresh);

        return TokenResponse.of(
                newAccess,
                newRefresh,
                jwtProvider.getAccessExpireMs()
        );
    }

    /**
     * ✅ 로그아웃 (RefreshToken 폐기)
     */
    @Transactional
    public void logout(String refreshToken) {

        refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
            rt.revoke(); // ✅ setter 대신 도메인 메서드
        });
    }

    /**
     * ✅ RefreshToken 저장 (단일 토큰 정책)
     */
    private void saveRefreshToken(String userId, String refreshToken) {

        // 기존 토큰 제거
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken rt = RefreshToken.builder()
                .userId(userId)
                .token(refreshToken)
                .expiredAt(Instant.now().plusMillis(jwtProvider.getRefreshExpireMs()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(rt);
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

        return new java.util.HashMap<String, Object>() {{
            put("id", user.getUserId());
            put("email", user.getEmail());
            put("name", user.getName());
            put("provider", user.getProvider());
            put("role", user.getRole());
        }};
    }
}