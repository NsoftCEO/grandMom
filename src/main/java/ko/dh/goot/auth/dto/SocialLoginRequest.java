package ko.dh.goot.auth.dto;

import lombok.Getter;

@Getter
public class SocialLoginRequest {
    private String accessToken; // 클라이언트에서 전달되는 소셜 엑세스 토큰
}