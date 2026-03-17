package ko.dh.goot.auth.oauth;

public interface OAuthUserInfo {
    String getProvider();    // ex: "GOOGLE", "KAKAO"
    String getProviderId();  // ex: "123456789" (소셜 제공자의 고유 PK)
    String getEmail();
    String getName();
    String getPhone();       // 전화번호 수집을 동의한 경우
}