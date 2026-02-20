package ko.dh.goot.auth;

import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.security.auth.message.AuthException;
import ko.dh.goot.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
/*
    private final AuthMapper authMapper;
    private final BCryptPasswordEncoder encoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public Map<String, String> login(String email, String rawPassword) {

        UserVO user = authMapper.findByEmail(email);
        if (user == null || !encoder.matches(rawPassword, user.getPassword())) {
            throw new AuthException("INVALID_CREDENTIALS");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new AuthException("USER_DISABLED");
        }

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken();

        authMapper.saveRefreshToken(
            user.getUserId(),
            refreshToken,
            new Date(System.currentTimeMillis() + jwtProvider.getRefreshExpireMs())
        );

        return Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken
        );
    }

    public String refresh(String refreshToken) {
        String userId = authMapper.findValidUserIdByToken(refreshToken);
        if (userId == null) {
            throw new AuthException("INVALID_REFRESH_TOKEN");
        }

        UserVO user = authMapper.findByUserId(userId);
        return jwtProvider.createAccessToken(user.getUserId(), user.getRole());
    }*/
}
