package ko.dh.goot.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenResponse {

    private String grantType; // "Bearer"
    private String accessToken;
    private String refreshToken;
    private Long accessTokenExpiresIn;

    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return TokenResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(expiresIn)
                .build();
    }
}