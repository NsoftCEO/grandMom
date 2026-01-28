package ko.dh.goot.payment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.common.exception.WebhookException;
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
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;
    
    
    
    public void verifyWebhook(String payload, String webhookId, String webhookSignature, String webhookTimestamp) {
    	
    	if (!StringUtils.hasText(payload) || !StringUtils.hasText(webhookId) || 
	            !StringUtils.hasText(webhookSignature) || !StringUtils.hasText(webhookTimestamp)) {
	            log.error("🚨 [Webhook Check] 필수 헤더 또는 데이터가 누락되었거나 비어있습니다.");
	            throw new WebhookException(ErrorCode.WEBHOOK_INVALID_REQUEST);
	        }
    	
    	verifyTimestamp(webhookTimestamp);

        String selfSignature = generateSignature(webhookId, webhookTimestamp, payload);

        if (!MessageDigest.isEqual(
                selfSignature.getBytes(StandardCharsets.UTF_8),
                webhookSignature.getBytes(StandardCharsets.UTF_8)               
        	)) {
            log.warn("[Webhook] 시그니처 불일치. expected={}, actual={}",
            		selfSignature, webhookSignature);
            throw new WebhookException(ErrorCode.WEBHOOK_SIGNATURE_INVALID);
        }
    }
    
    private void verifyTimestamp(String webhookTimestamp) {
        long timestamp;

        try {
            timestamp = Long.parseLong(webhookTimestamp);
        } catch (NumberFormatException e) {
            throw new WebhookException(ErrorCode.WEBHOOK_INVALID_REQUEST);
        }

        long now = Instant.now().getEpochSecond();

        if (Math.abs(now - timestamp) > TIMESTAMP_TOLERANCE_SECONDS) {
            log.warn("[Webhook] 타임스탬프 만료. timestamp={}", webhookTimestamp);
            throw new WebhookException(ErrorCode.WEBHOOK_TIMESTAMP_EXPIRED);
        }
    }
    
    private String generateSignature(String webhookId, String timestamp, String payload) {
        try {
            String secret = webhookSecret;

            if (secret.startsWith(webhookPrefix)) {
                secret = secret.substring(webhookPrefix.length());
            }

            byte[] decodedKey = Base64.getDecoder().decode(secret);
            SecretKeySpec keySpec = new SecretKeySpec(decodedKey, HMAC_SHA256);

            String toSign = webhookId + "." + timestamp + "." + payload;

            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(keySpec);
            byte[] macData = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));

            return "v1," + Base64.getEncoder().encodeToString(macData);

        } catch (Exception e) {
            log.error("[Webhook] 시그니처 생성 실패", e);
            throw new WebhookException(ErrorCode.INTERNAL_SERVER_ERROR, "시그니처 생성 중 오류 발생"); // 유일한 500에러, 웹훅 재시도 요청
        }
    }
    
}