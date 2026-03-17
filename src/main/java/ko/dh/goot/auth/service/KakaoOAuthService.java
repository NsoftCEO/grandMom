package ko.dh.goot.auth.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ko.dh.goot.auth.oauth.KakaoUserInfo;
import ko.dh.goot.auth.oauth.OAuthUserInfo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

	private final RestTemplate restTemplate;
    private final String userInfoUri = "https://kapi.kakao.com/v2/user/me";

    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> resp = restTemplate.exchange(userInfoUri, HttpMethod.GET, entity, Map.class);
        Map<String, Object> body = resp.getBody();
        return new KakaoUserInfo(body);
    }
}