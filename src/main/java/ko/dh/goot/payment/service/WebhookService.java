package ko.dh.goot.payment.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Service
public class WebhookService {

    @Value("${portone.webhook-secret}")
    private String webhookSecret;
    
    @Value("${portone.webhook-prefix}")
    private String webhookPrefix;
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    
    
    public boolean verifyWebhook(String payload, String webhookId, String webhookSignature, String webhookTimestamp) {
    	
    	if (!StringUtils.hasText(payload) || !StringUtils.hasText(webhookId) || 
	            !StringUtils.hasText(webhookSignature) || !StringUtils.hasText(webhookTimestamp)) {
	            log.error("🚨 [Webhook Check] 필수 헤더 또는 데이터가 누락되었거나 비어있습니다.");
	            return false;
	        }
    	
    	try{   
	 
    		if (!verifyTimestamp(webhookTimestamp)) {
    		    return false;
    		}
    		
    		String selfSignature = this.selfSignature(webhookId, webhookTimestamp, payload);
    		
    		if(webhookSignature.equals(selfSignature) && selfSignature != null) {
    			return true;
    		}

			return false;


    	}catch (Exception e) {
    		log.warn("[verifyWebhook] 검증 중 예외 발생", e);
    		return false;
		}
    	
    }
    
    private boolean verifyTimestamp(String webhookTimestamp) {
    	try {
    		long timestamp = Long.parseLong(webhookTimestamp);
            long currentTime = Instant.now().getEpochSecond();
            long toleranceSeconds = 300; 
            
            if (Math.abs(currentTime - timestamp) > toleranceSeconds) {
                log.warn("웹훅 검즘 타임스태프 오류: {}", webhookTimestamp);
                return false;
            }
            
            return true;
            
    	} catch (NumberFormatException e) {
    	    log.warn("웹훅 타임스탬프 형식 오류: {}", webhookTimestamp);
    	    return false;
    	}      
    }
    
    private String selfSignature(String webhookId, String timestamp, String payload) {
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
        	log.error("[Webhook Check] 시그니처 생성 실패", e);
        	return null;
        }
    }
    
}