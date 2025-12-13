package ko.dh.goot.service;

import java.time.Instant;
import java.nio.charset.StandardCharsets;

import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class WebhookService {

    // 환경 설정 파일 (application.yml 등)에서 웹훅 비밀 키를 주입받습니다.
    @Value("${portone.webhook-secret}")
    private String webhookSecret;
    
    @Value("${portone.webhook-prefix}")
    private String webhookPrefix;
    
    private final String HMAC_SHA256 = "HmacSHA256";
    
    
    
    public boolean verifyWebhook(String payload, String webhookId, String webhookSignature, String webhookTimestamp) {
    	System.out.println("ㅁㅁㅁㅁㅁ ㅁㅁ");
    	try{   		
    		boolean verifyTimestamp = this.verifyTimestamp(webhookTimestamp);
    		
    		String selfSigniture = this.selfSigniture(webhookId, webhookTimestamp, payload);
    		
    		if(webhookSignature.equals(selfSigniture)) {
    			System.out.println("시그니처 같음");
    			System.out.println(webhookSignature);
    			System.out.println(selfSigniture);
    			return true;
    		}
			System.out.println("시그니처 다름");
			return false;


    	}catch (Exception e) {
    		return false;
		}
    	
    }
    
    private boolean verifyTimestamp(String webhookTimestamp) {
    	long timestamp = Long.parseLong(webhookTimestamp);
        long currentTime = Instant.now().getEpochSecond();
        long toleranceSeconds = 300; 
        
        if (Math.abs(currentTime - timestamp) > toleranceSeconds) {
            log.warn("웹훅 검즘 타임스태프 오류: {}", webhookTimestamp);
            return false;
        }
        return true;
    }
    
    private String selfSigniture(String webhookId, String timestamp, String payload) {
        try {
            String sec = webhookSecret;
            
            if (sec.startsWith(webhookPrefix)) {
                sec = sec.substring(webhookPrefix.length());
            }

            byte[] decodedKey = Base64.getDecoder().decode(sec);
            SecretKeySpec keySpec = new SecretKeySpec(decodedKey, HMAC_SHA256);

            String toSign = webhookId + "." + timestamp + "." + payload;

            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(keySpec);
            byte[] macData = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));

            return "v1," + Base64.getEncoder().encodeToString(macData);
        } catch (Exception e) {
            throw new RuntimeException("시그니처 생성 중 오류 발생", e);
        }
    }
    
    

}