package ko.dh.goot.controller;


import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

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
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.dto.OrderRequest;
import ko.dh.goot.dto.OrderResponse;
import ko.dh.goot.dto.Product;
import ko.dh.goot.service.OrderService;
import ko.dh.goot.service.ProductService;
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
    @PostMapping("/completePaymentOrgin")
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
    }
    
    /**
     * [웹훅 엔드포인트 역할] PG사로부터 결제 성공 알림(Webhook)을 받아 주문을 최종 확정합니다.
     * 클라이언트(브라우저)가 아닌 PG사 서버가 호출하도록 설계합니다.
     * 이 엔드포인트는 PG사 콘솔에 웹훅 URL로 등록되어야 합니다. (예: https://yourdomain.com/api/v1/orders/completePayment)
     * @param payload PG사에서 전달한 결제 정보 (paymentId, orderId 등이 포함됨)
     * @param portoneSignature PG사에서 보낸 시그니처 (요청 헤더 'X-Portone-Signature' 또는 'Authorization' 등에 포함되어 있다고 가정)
     */
    @PostMapping("/completePayment")
    public ResponseEntity<?> handlePaymentWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader Map<String, String> headers, 
            @RequestHeader(value = "webhook-id") String webhookId,
            @RequestHeader(value = "webhook-signature") String webhookSignature,
            @RequestHeader(value = "webhook-timestamp") String webhookTimestamp) {
        
        try {

        	System.out.println(" 웹훅 요청 헤더 전체 목록:");
            headers.forEach((key, value) -> {
                System.out.println(String.format("Header '%s' = %s", key, value));
            });
            System.out.println("-----------------------------------------------------------");
            
        	System.out.println("handlePaymentWebhook (웹훅 역할) 호출");
            System.out.println("payload::");
            System.out.println(payload);
            System.out.println("webhookSignature::");
            System.out.println(webhookSignature);
            // -----------------------------------------------------------
            // 0. 웹훅 시크릿 키 검증 (보안 강화)
            // -----------------------------------------------------------

            if (!verifyWebhookSignature(payload, webhookSignature, webhookTimestamp)) {
                System.err.println("🚨 경고: 웹훅 시그니처 검증 실패. 위조 요청 가능성.");
                return ResponseEntity.status(403).body(Map.of("message", "Invalid Webhook Signature. Access Denied."));
            }
            
            // 1. 필수 데이터 추출
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
    }
    
    /**
     * 웹훅 시그니처를 검증하는 Mock 메서드입니다.
     * 실제 구현 시 PG사의 정확한 해시 알고리즘(예: HMAC-SHA256)을 사용하여 구현해야 합니다.
     * ⚠️ PortOne은 'Authorization' 헤더에 토큰 형식으로 시그니처를 제공할 수 있으므로, 실제 PG사 문서를 참고하여 구현해야 합니다.
     * * @param payload 웹훅 요청 본문 데이터
     * @param webhookSignature PG사에서 보낸 시그니처
     * @return 시그니처가 일치하면 true
     */
    private boolean verifyWebhookSignature(Map<String, Object> payload, String webhookSignature, String webhookTimestamp) {
    	if (webhookSignature == null || webhookSignature.isEmpty() || webhookTimestamp == null || webhookTimestamp.isEmpty()) {
            System.err.println("시그니처 또는 타임스탬프가 누락되었습니다.");
            return false;
        }

    	// 1. 타임스탬프 유효성 검사 (Replay Attack 방지) - 5분 이내의 요청만 허용한다고 가정
        try {
            long timestamp = Long.parseLong(webhookTimestamp);
            long currentTime = Instant.now().getEpochSecond();
            long toleranceSeconds = 300; // 5분 허용 오차
            
            System.out.println("timestamp 유효성검사::");
            System.out.println(timestamp);
            System.out.println(currentTime);
            System.out.println(Math.abs(currentTime - timestamp));
            
            if (Math.abs(currentTime - timestamp) > toleranceSeconds) {
                System.err.println("타임스탬프 검증 실패: 요청이 너무 오래되었거나 미래의 요청입니다. Timestamp: " + webhookTimestamp);
                return false;
            }
        } catch (NumberFormatException e) {
            System.err.println("타임스탬프 형식이 올바르지 않습니다.");
            return false;
        }

        // 2. 시그니처 생성 및 비교
        try {
            // PG사는 보통 타임스탬프와 Payload를 결합한 문자열을 해시합니다.
            // Mock 예시: timestamp + "." + payload JSON string
            String payloadString = new ObjectMapper().writeValueAsString(payload);
            String signedPayload = webhookTimestamp + "." + payloadString; 
            
            System.out.println("payload::");
            System.out.println(payload);
            System.out.println("payloadString::");
            System.out.println(payloadString);
            System.out.println("signedPayload::");
            System.out.println(signedPayload);
            
            // HMAC-SHA256 해시 생성
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(webhookSecret.getBytes("UTF-8"), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            
            String selfSignature = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(signedPayload.getBytes("UTF-8")));

            // 3. 생성된 시그니처와 PG사 시그니처 비교
            // ⚠️ 실제 PG사 시그니처는 'v1,해시값' 형태일 수 있습니다. (예시: Stripe)
            String signatureToCompare = webhookSignature.startsWith("v1,") ? webhookSignature.substring(3) : webhookSignature;
            
            if (!selfSignature.equals(signatureToCompare)) {
                System.err.println("시그니처 불일치: Self=" + selfSignature + ", PG=" + signatureToCompare);
                return false;
            }
            
            System.out.println("웹훅 시그니처 및 타임스탬프 검증 성공.");
            return true;

        } catch (NoSuchAlgorithmException | InvalidKeyException | JsonProcessingException e) {
            System.err.println("시그니처 생성 오류: " + e.getMessage());
            return false;
        } catch (Exception e) {
             System.err.println("일반 오류: " + e.getMessage());
             return false;
        }
    }
}
