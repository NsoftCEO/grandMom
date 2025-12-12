package ko.dh.goot.controller;


import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ko.dh.goot.dto.OrderRequest;
import ko.dh.goot.dto.OrderResponse;
import ko.dh.goot.dto.Product;
import ko.dh.goot.service.OrderService;
import ko.dh.goot.service.ProductService;
import ko.dh.goot.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
@Log4j2
@Controller
@RequiredArgsConstructor
@RequestMapping("order")
public class OrderController {
	
	@Value("${portone.store-id}")
    private String storeId;
	
	@Value("${portone.channel-key}")
    private String kakaoChannelKey;
	
	@Value("${portone.webhook-secret}")
    private String webhookSecret;
	
	private final ProductService productService;
	private final OrderService orderService;
	private final WebhookService webhookService;
	//private final PaymentService paymentService;

	 // 주문 페이지로 이동
    @GetMapping("/detail")
    public String orderPage(@RequestParam("productId") int productId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            Model model) {
    	System.out.println("주문상세로 이동");
        Product product = productService.selectProductById(productId);
        model.addAttribute("product", product);
        model.addAttribute("quantity", quantity);
        model.addAttribute("storeId", storeId);
        model.addAttribute("kakaoChannelKey", kakaoChannelKey);
        System.out.println("product::");
        System.out.println(product);
        return "order/orderDetail"; // order.html 템플릿 렌더링
    }

    @PostMapping("/prepareOrder")
    public ResponseEntity<Map<String, Object>> prepareOrder(@RequestBody OrderRequest orderRequest) {
        // ⚠️ [보안 필수] 실제로는 세션이나 Spring Security를 통해 userId를 가져와야 함
        String currentUserId = "user-1234"; // 임시 사용자 ID

        try {
            // 💡 Service 호출: 금액 재계산, DB 저장, orderId 반환
        	OrderResponse response = orderService.prepareOrder(orderRequest, currentUserId);

            // 클라이언트에게 orderId와 서버 확정 금액을 반환
            return ResponseEntity.ok(Map.of(
                "orderId", response.getOrderId(),
                "expectedAmount", response.getExpectedAmount() 
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 재고 부족, 상품 없음 등의 비즈니스 로직 에러
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            // 기타 서버 에러
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "주문 생성 중 알 수 없는 서버 오류가 발생했습니다."
            ));
        }
    }
    
 // ✅ 포트원에서 결제 완료 후 호출
   /* @PostMapping("/completePaymentOrgin")
    public ResponseEntity<?> completePayment(@RequestBody Map<String, Object> payload) {
        try {
        	System.out.println("/complete 호출");
            String paymentId = (String) payload.get("paymentId");
            Object orderIdObj = payload.get("orderId");

            if(paymentId == null || orderIdObj == null) {
            	System.out.println("completePayment null오류");
            }
            
            Long orderId;
            if (orderIdObj instanceof Integer) {
                orderId = ((Integer) orderIdObj).longValue();
            
            // 2. JSON 파서가 Long으로 파싱한 경우 (값이 클 때)
            } else if (orderIdObj instanceof Long) {
                orderId = (Long) orderIdObj;

            // 3. String 등 예상치 못한 타입으로 온 경우 (매우 드물지만 안전 대비)
            } else {
                throw new IllegalArgumentException("주문 ID의 형식이 올바르지 않습니다.");
            }
            
            orderService.verifyPayment(paymentId, orderId);
            return ResponseEntity.ok().body(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", e.getMessage()));
        }
    }*/
    
    /**
     * [웹훅 엔드포인트 역할] PG사로부터 결제 성공 알림(Webhook)을 받아 주문을 최종 확정합니다.
     * 클라이언트(브라우저)가 아닌 PG사 서버가 호출하도록 설계합니다.
     * 이 엔드포인트는 PG사 콘솔에 웹훅 URL로 등록되어야 합니다. (예: https://yourdomain.com/api/v1/orders/completePayment)
     * @param payload PG사에서 전달한 결제 정보 (paymentId, orderId 등이 포함됨)
     * @param portoneSignature PG사에서 보낸 시그니처 (요청 헤더 'X-Portone-Signature' 또는 'Authorization' 등에 포함되어 있다고 가정)
     */
    /* @PostMapping("/completePayment")
    public ResponseEntity<?> handlePaymentWebhook(
    		@RequestBody String rawPayload,
            @RequestHeader(value = "webhook-id") String webhookId,
            @RequestHeader(value = "webhook-signature") String webhookSignature,
            @RequestHeader(value = "webhook-timestamp") String webhookTimestamp) {
        
        try {
        	log.debug("completePayment ::");
        	System.out.println(" 웹훅 요청 헤더 전체 목록:");

            System.out.println("-----------------------------------------------------------");
            
        	System.out.println("handlePaymentWebhook (웹훅 역할) 호출");
            System.out.println("payload::");
            System.out.println(rawPayload);
            System.out.println("webhookSignature::");
            System.out.println(webhookSignature);
            // -----------------------------------------------------------
            // 0. 웹훅 시크릿 키 검증 (보안 강화)
            // -----------------------------------------------------------

            if (!verifyWebhookSignature(rawPayload, webhookSignature, webhookTimestamp, webhookId)) {
                log.error("🚨 [Webhook] 시그니처 검증 실패. 위조 요청 가능성.");
                return ResponseEntity.status(403).body(Map.of("message", "Invalid Webhook Signature. Access Denied."));
            }
            
            // 1. 필수 데이터 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(rawPayload, Map.class);
            
            String paymentId = (String) payload.get("paymentId");
            Object orderIdObj = payload.get("orderId");
            
            if (paymentId == null || orderIdObj == null) {
                // 데이터 누락 시 400 Bad Request 대신, 200 OK를 반환하여 PG사의 재시도를 막고 로그를 남겨 수동 처리 유도 가능
                return ResponseEntity.badRequest().body(Map.of("message", "필수 데이터 (paymentId 또는 orderId)가 누락되었습니다."));
            }

            // 2. orderId 타입 변환 (JSON 파싱 시 Int/Long 혼용 방지)
            Long orderId;
            if (orderIdObj instanceof Integer) {
                orderId = ((Integer) orderIdObj).longValue();
            } else if (orderIdObj instanceof Long) {
                orderId = (Long) orderIdObj;
            } else {
                 throw new IllegalArgumentException("주문 ID의 형식이 올바르지 않습니다.");
            }
            
            // -----------------------------------------------------------
            // 3. 핵심: OrderService의 트랜잭션 메서드 호출 (검증, DB 업데이트, 재고 차감)
            // -----------------------------------------------------------
            orderService.completeOrderTransaction(paymentId, orderId);
            
            // 4. 웹훅 응답: PG사에 "정상적으로 처리했음"을 알리기 위해 200 OK를 반환합니다.
            return ResponseEntity.ok(Map.of("message", "PG사 웹훅 처리 성공 및 주문 완료", "orderId", orderId));

        } catch (IllegalArgumentException e) {
            // PG사로부터 받은 데이터 형식 오류
            System.err.println("웹훅 데이터 형식 오류: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "웹훅 데이터 형식 오류: " + e.getMessage()));
        } catch (IllegalStateException e) {
            // 금액 불일치, 이미 처리된 주문 등의 비즈니스 로직 오류 -> PG사에 재시도 요청을 막기 위해 200 OK 반환
            System.err.println("결제 검증/확정 비즈니스 오류 (웹훅): " + e.getMessage());
            return ResponseEntity.ok(Map.of("message", "비즈니스 로직 오류로 처리 실패 (환불 처리 필요): " + e.getMessage()));
        } catch (Exception e) {
            // 기타 서버 에러 -> PG사에 재시도를 유도하기 위해 500 Internal Server Error 반환
            System.err.println("결제 완료 처리 중 서버 오류 발생 (웹훅): " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "웹훅 처리 중 서버 오류가 발생했습니다. PG사가 재시도할 것입니다."
            ));
        }
    }*/
    
    @PostMapping("/completePayment")
    public ResponseEntity<?> handlePaymentWebhook(
    		@RequestBody String payload,
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestHeader("webhook-timestamp") String webhookTimestamp){
    	
    	webhookService.verifyWebhook(payload, webhookId, webhookSignature, webhookTimestamp);
		return null;
    }
    
    /**
     * [보안 필수 메서드] PG사 웹훅 시그니처를 검증합니다.
     * Standard Webhooks V2의 Canonical JSON 생성 방식을 따르며, 
     * 안전한 검증을 위해 로컬 ObjectMapper를 사용하도록 개선했습니다.
     */
   /* private boolean verifyWebhookSignature(
            Map<String, Object> payload,
            String webhookSignature,
            String webhookTimestamp
    ) {
        // 1. 필수 헤더 및 타임스탬프 유효성 검사 (Replay Attack 방지)
        if (webhookSignature == null || webhookSignature.isEmpty()
                || webhookTimestamp == null || webhookTimestamp.isEmpty()) {
            log.error("[Webhook] Missing signature or timestamp.");
            return false;
        }

        try {
            // 타임스탬프 검증 (5분 오차 허용)
            long timestamp = Long.parseLong(webhookTimestamp);
            long currentTime = Instant.now().getEpochSecond();
            long toleranceSeconds = 300; 
            
            if (Math.abs(currentTime - timestamp) > toleranceSeconds) {
                log.warn("[Webhook] Timestamp validation failed: Request is too old or in the future. Timestamp: {}", webhookTimestamp);
                return false;
            }
            
            // ---- 1) Canonical JSON 생성 및 설정 (V2 표준) ----
            
            // 💡 웹훅 검증을 위해 엄격하게 설정된 ObjectMapper를 로컬에서 새로 생성하여 사용합니다.
            // 이는 전역 ObjectMapper 설정의 영향을 받지 않고 Canonical JSON의 규칙을 강제하기 위함입니다.
            ObjectMapper canonicalMapper = new ObjectMapper();
            
            // 필수 Canonical JSON 설정 
            canonicalMapper.configure(SerializationFeature.INDENT_OUTPUT, false); // No pretty printing (공백 제거)
            
            // PortOne Canonical JSON V2 표준: Non-ASCII 및 슬래시 이스케이프 방지
            canonicalMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), false);
            canonicalMapper.getFactory().configure(JsonWriteFeature.ESCAPE_FORWARD_SLASHES.mappedFeature(), false);
            
            // Canonical JSON은 키를 알파벳 순으로 정렬합니다.
            Map<String, Object> sortedPayload = new TreeMap<>(payload);

            // Canonical JSON 문자열 생성
            String payloadJson = canonicalMapper.writeValueAsString(sortedPayload);
            log.debug("[Webhook Debug] Canonical JSON: {}", payloadJson);

            // ---- 2) signedPayload 구성 (V2 표준) ----
            // 형식: timestamp + . + Canonical JSON String
            String signedPayload = webhookTimestamp + "." + payloadJson;
            log.debug("[Webhook Debug] Signed Payload: {}", signedPayload);

            // ---- 3) Secret Key 처리 및 HMAC 계산 ----
            String secret = webhookSecret;
            if (secret != null) {
                String trimmedSecret = secret.trim();
                secret = trimmedSecret;
            }
            
            // Secret Key에서 'whsec_' 접두사 제거
            if (secret.startsWith("whsec_")) {
                secret = secret.substring("whsec_".length());
                log.debug("[Webhook Debug] Secret Key prefix removed. Key segment length: {}", secret.length());
            } else {
                 log.debug("[Webhook Debug] Secret Key used (no prefix removed). Key segment length: {}", secret.length());
            }
            
            // V2 표준: Secret Key는 US_ASCII로 바이트 변환
            byte[] keyBytes = secret.getBytes(StandardCharsets.US_ASCII);
            
            SecretKeySpec signingKey = new SecretKeySpec(
                    keyBytes, 
                    "HmacSHA256"
            );

            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(signingKey);

            // HMAC 계산 시, Signed Payload 문자열은 UTF-8 바이트로 변환
            byte[] hash = hmac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)); 
            String selfSignature = Base64.getEncoder().encodeToString(hash);

            // ---- 4) PortOne signature 의 v1 제거 ----
            // PortOne은 웹훅 시그니처에 'v1,' 접두사를 붙여서 보냅니다.
            String pgSignature = webhookSignature;
            if (pgSignature.startsWith("v1,")) {
                pgSignature = pgSignature.substring(3);
            }

            // ---- 5) 최종 비교 ----
            if (!selfSignature.equals(pgSignature)) {
                log.error("[Webhook] Signature mismatch. self={} | pg={}",
                        selfSignature, pgSignature);
                return false;
            }
            
            log.info("[Webhook] Signature verification successful!");
            return true;

        } catch (JsonProcessingException e) {
            log.error("[Webhook] Error processing JSON for Canonical format: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("[Webhook] Error verifying signature: {}", e.getMessage());
            return false;
        }
    }*/
    
    /**
     * [보안 필수 메서드] PG사 웹훅 시그니처를 검증합니다.
     * 타임스탬프 유효성 및 HMAC-SHA256 해시 비교를 통해 요청의 위변조 여부를 확인합니다.
     * * @param payload 웹훅 요청 본문 데이터
     * @param webhookSignature PG사에서 보낸 시그니처 (헤더: 'webhook-signature')
     * @param webhookTimestamp PG사에서 보낸 타임스탬프 (헤더: 'webhook-timestamp')
     * @return 시그니처가 일치하면 true
     */
    /*private boolean verifyWebhookSignature(
            String rawPayload,
            String webhookSignature,
            String webhookTimestamp,
            String webhookId
    ) {
    	try {
    		String dataToSign = String.join(".", webhookId, webhookTimestamp, rawPayload); // 데이터 조합
            Mac mac = Mac.getInstance("HmacSHA256"); // HMAC-SHA256 알고리즘 사용
            mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256")); // Secret 키 설정
            String a = Base64.getEncoder().encodeToString(mac.doFinal(dataToSign.getBytes())); // 시그니처 생성
            System.out.println("a값:");
            System.out.println(a);
            System.out.println("webhookSignature 값::");
            System.out.println(webhookSignature);
            if(a.equals(webhookSignature)) {
            	System.out.println("검증성공");
            	return true;
            }else {
            	System.out.println("검증실패");
            	 return false;
            }
    	}catch (Exception e) {
			System.out.println("verifyWebhookSignature에서오류:: ");
		}   	
    	 return false;
       
    }*/
        /* // 1. 필수 헤더 및 타임스탬프 유효성 검사 (Replay Attack 방지)
        if (webhookSignature == null || webhookSignature.isEmpty()
                || webhookTimestamp == null || webhookTimestamp.isEmpty()
                || webhookId == null || webhookId.isEmpty()) {
            log.error("[Webhook] Missing signature, timestamp, or webhook ID.");
            return false;
        }

        try {
            // 타임스탬프 검증 (5분 오차 허용)
            long timestamp = Long.parseLong(webhookTimestamp);
            long currentTime = Instant.now().getEpochSecond();
            long toleranceSeconds = 300; 
            
            if (Math.abs(currentTime - timestamp) > toleranceSeconds) {
                log.warn("[Webhook] Timestamp validation failed: Request is too old or in the future. Timestamp: {}", webhookTimestamp);
                return false;
            }
            
            // ---- 1) Signed Payload 구성 (Webhook ID 기반) ----
            // 형식: webhookId.webhookTimestamp.rawPayload
            String signedPayload = String.join(".", webhookId, webhookTimestamp, rawPayload);
            log.debug("[Webhook Debug] Signed Payload (ID based): {}", signedPayload);

            // ---- 2) Secret Key 처리 및 HMAC 계산 ----
            String secret = webhookSecret;
            System.out.println("webhookSecret：：");
            System.out.println(webhookSecret);
            if (secret != null) {
                // 🚨 공백 문자 제거 및 길이 로깅
                String trimmedSecret = secret.trim();
                if (trimmedSecret.length() != secret.length()) {
                    log.warn("[Webhook Debug] Secret Key was trimmed. Original length: {} | Trimmed length: {}", secret.length(), trimmedSecret.length());
                }
                secret = trimmedSecret;
            }
            
            if (secret.startsWith("whsec_")) {
                secret = secret.substring("whsec_".length());
                log.debug("[Webhook Debug] Secret Key prefix removed. Key segment length: {}", secret.length());
            } else {
                 log.debug("[Webhook Debug] Secret Key used (no prefix removed). Key segment length: {}", secret.length());
            }
            
            // 🚨 Secret Key는 US_ASCII로 바이트 변환
            byte[] keyBytes = secret.getBytes(StandardCharsets.US_ASCII);
            
            SecretKeySpec signingKey = new SecretKeySpec(
                    keyBytes, 
                    "HmacSHA256"
            );

            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(signingKey);

            // HMAC 계산 시, Signed Payload 문자열은 UTF-8 바이트로 변환
            byte[] hash = hmac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)); 
            String selfSignature = Base64.getEncoder().encodeToString(hash);

            // ---- 3) PortOne signature 의 v1 제거 ----
            String pgSignature = webhookSignature;
            if (pgSignature.startsWith("v1,")) {
                pgSignature = pgSignature.substring(3);
            }

            // ---- 4) 최종 비교 ----
            if (!selfSignature.equals(pgSignature)) {
                log.error("[Webhook] Signature mismatch. self={} | pg={}",
                        selfSignature, pgSignature);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("[Webhook] Error verifying signature: {}", e.getMessage());
            return false;
        }
     }*/
    
}
