package ko.dh.goot.auth.service;

import org.springframework.stereotype.Service;

import ko.dh.goot.auth.oauth.OAuthUserInfo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final GoogleOAuthService googleOAuthService;
    private final KakaoOAuthService kakaoOAuthService;

    public OAuthUserInfo getUserInfo(String provider, String accessToken) {
        return switch (provider.toLowerCase()) {
            case "google" -> googleOAuthService.getUserInfo(accessToken);
            case "kakao" -> kakaoOAuthService.getUserInfo(accessToken);
            default -> throw new IllegalArgumentException("unsupported provider: " + provider);
        };
    }
}