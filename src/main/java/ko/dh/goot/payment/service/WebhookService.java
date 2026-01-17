package ko.dh.goot.payment.service;

import java.time.Instant;
import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.order.dao.OrderMapper;
import ko.dh.goot.payment.dao.PaymentMapper;
import ko.dh.goot.payment.dto.WebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Service
public class WebhookService {

    // 환경 설정 파일 (application.yml 등)에서 웹훅 비밀 키를 주입받습니다.
    @Value("${portone.webhook-secret}")
    private String webhookSecret;
    
    @Value("${portone.webhook-prefix}")
    private String webhookPrefix;
    
    private final String HMAC_SHA256 = "HmacSHA256";
    
    private final ObjectMapper objectMapper;
    
    
    public boolean verifyWebhook(String payload, String webhookId, String webhookSignature, String webhookTimestamp) {
    	
    	if (!StringUtils.hasText(payload) || !StringUtils.hasText(webhookId) || 
	            !StringUtils.hasText(webhookSignature) || !StringUtils.hasText(webhookTimestamp)) {
	            log.error("🚨 [Webhook Check] 필수 헤더 또는 데이터가 누락되었거나 비어있습니다.");
	            return false;
	        }
    	
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
        	log.warn("시그니처 생성 중 오류 발생");
            throw new RuntimeException("시그니처 생성 중 오류 발생", e);
        }
    }
    
    /*
    WebhookPayload로 대체
    public Map<String, Object> extractWebhookData(String payload){
    	Map<String, Object> parsedPayload;
    	try {
            parsedPayload = objectMapper.readValue(payload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) parsedPayload.get("data");
            if (dataMap == null) {
            	log.info("payload가 data 구조 아님");
                dataMap = parsedPayload;
            }
    	
            String paymentId = (String) dataMap.get("paymentId");
            
            if (paymentId == null) {
                throw new IllegalArgumentException("페이로드에 paymentId가 누락되었습니다.");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("paymentId", paymentId);
            return result;
            
    	} catch (JsonProcessingException e) {
            log.error("🚨 [Webhook] JSON 파싱 실패. payload={}", payload, e);
            throw new IllegalArgumentException("유효하지 않은 JSON 페이로드입니다.", e);
        }
    }*/
    
    

}