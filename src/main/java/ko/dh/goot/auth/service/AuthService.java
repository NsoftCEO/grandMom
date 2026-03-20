package ko.dh.goot.auth.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ko.dh.goot.auth.dao.RefreshTokenMapper;
import ko.dh.goot.auth.domain.RefreshToken;
import ko.dh.goot.auth.domain.UserRole;
import ko.dh.goot.auth.dto.ClientMetadata;
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
    private final PasswordEncoder passwordEncoder;
    private final TokenHasher tokenHasher;

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

    @Transactional
    public TokenResponse login(LoginRequest request, ClientMetadata metadata) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + email));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BadCredentialsException("inactive user");
        }

        user.updateLastLogin();
        ClientMetadata normalized = normalizeMetadata(metadata);

        if (hasText(normalized.deviceId())) {
            refreshTokenMapper.revokeByUserIdAndDeviceIdAndDeviceType(
                    user.getUserId(),
                    normalized.deviceId(),
                    normalized.deviceType(),
                    "RELOGIN_SAME_DEVICE"
            );
        }

        String familyId = UUID.randomUUID().toString();
        return issueTokens(user, familyId, normalized);
    }

    @Transactional
    public TokenResponse socialLogin(String provider, String accessTokenFromClient, ClientMetadata metadata) {
        OAuthUserInfo userInfo = oAuthService.getUserInfo(provider, accessTokenFromClient);

        Optional<User> optUser = (userInfo.getEmail() != null)
                ? userRepository.findByEmail(userInfo.getEmail())
                : Optional.empty();

        User user = optUser
                .or(() -> userRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId()))
                .orElseGet(() -> register(userInfo));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BadCredentialsException("inactive user");
        }

        user.updateLastLogin();
        ClientMetadata normalized = normalizeMetadata(metadata);

        if (hasText(normalized.deviceId())) {
            refreshTokenMapper.revokeByUserIdAndDeviceIdAndDeviceType(
                    user.getUserId(),
                    normalized.deviceId(),
                    normalized.deviceType(),
                    "RELOGIN_SAME_DEVICE"
            );
        }

        String familyId = UUID.randomUUID().toString();
        return issueTokens(user, familyId, normalized);
    }

    @Transactional(noRollbackFor = RefreshTokenException.class)
    public TokenResponse refreshToken(String refreshToken, ClientMetadata currentMetadata) {
        ClientMetadata normalized = normalizeMetadata(currentMetadata);
        String hashedToken = tokenHasher.hash(refreshToken);

        RefreshToken rt = refreshTokenMapper.findByTokenHash(hashedToken)
                .orElseThrow(() -> new RefreshTokenException("invalid refresh token"));

        if (rt.isRevoked()) {
            refreshTokenMapper.revokeFamily(rt.getTokenFamilyId(), "TOKEN_REUSE_DETECTED");
            throw new RefreshTokenException("COMPROMISED_TOKEN_DETECTED");
        }

        if (rt.isExpired()) {
            refreshTokenMapper.revokeFamily(rt.getTokenFamilyId(), "TOKEN_EXPIRED");
            throw new RefreshTokenException("TOKEN_EXPIRED");
        }

        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BadCredentialsException("inactive user");
        }

        // 💡 [핵심] 원자적 처리: 동시에 2번의 갱신 요청이 들어올 때의 Race Condition 방어
        int updatedRows = refreshTokenMapper.revokeToken(hashedToken, "ROTATED");
        if (updatedRows == 0) {
            // WHERE revoked = false 조건에 의해 0건이 수정되었다면, 
            // 찰나의 순간에 누군가 이미 이 토큰을 썼다는 뜻입니다.
            refreshTokenMapper.revokeFamily(rt.getTokenFamilyId(), "CONCURRENT_REUSE_DETECTED");
            throw new RefreshTokenException("COMPROMISED_TOKEN_DETECTED");
        }

        // 기존 기기 정보는 DB 값을 유지하고 접속 IP/UserAgent 등만 최신화
        ClientMetadata merged = new ClientMetadata(
                normalized.ipAddress(),
                normalized.userAgent(),
                rt.getDeviceId(),
                rt.getDeviceType(),
                rt.getDeviceName()
        );

        return issueTokens(user, rt.getTokenFamilyId(), merged);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!hasText(refreshToken)) return;
        refreshTokenMapper.revokeToken(tokenHasher.hash(refreshToken), "LOGOUT");
    }

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

    private TokenResponse issueTokens(User user, String familyId, ClientMetadata metadata) {
        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken();

        saveRefreshToken(user.getUserId(), refreshToken, familyId, metadata);

        return TokenResponse.of(accessToken, refreshToken, jwtProvider.getAccessExpireMs());
    }

    private void saveRefreshToken(String userId, String refreshToken, String familyId, ClientMetadata metadata) {
        RefreshToken rt = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHasher.hash(refreshToken))
                .tokenFamilyId(familyId)
                .expiredAt(Instant.now().plusMillis(jwtProvider.getRefreshExpireMs()))
                .revoked(false)
                .deviceId(metadata.deviceId())
                .deviceType(metadata.deviceType())
                .deviceName(hasText(metadata.deviceName()) ? metadata.deviceName() : metadata.userAgent())
                .userAgent(metadata.userAgent())
                .ipAddress(metadata.ipAddress())
                .build();

        refreshTokenMapper.insertToken(rt);
    }

    public Object me(String accessToken) {
        if (!jwtProvider.validateToken(accessToken)) {
            throw new BadCredentialsException("invalid access token");
        }

        String userId = jwtProvider.getUserId(accessToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("user not found: " + userId));

        return Map.of(
        	    "id", user.getUserId(),
        	    "email", user.getEmail(),
        	    "name", user.getName(),
        	    "provider", user.getProvider(),
        	    "role", user.getRole()
        	);
    }

    /**
     * 💡 헤더 누락 시의 Null 방어 및 기본값 세팅
     */
    private ClientMetadata normalizeMetadata(ClientMetadata metadata) {
        if (metadata == null) {
            return new ClientMetadata(null, null, null, "WEB", null);
        }

        String deviceType = hasText(metadata.deviceType()) ? metadata.deviceType().trim().toUpperCase() : "WEB";
        return new ClientMetadata(
                metadata.ipAddress(),
                metadata.userAgent(),
                metadata.deviceId(),
                deviceType,
                metadata.deviceName()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}