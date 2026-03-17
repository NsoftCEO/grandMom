package ko.dh.goot.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenResponse {
    private String grantType;     // 보통 "Bearer"
    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpiresIn; // 만료 시간(ms)
}