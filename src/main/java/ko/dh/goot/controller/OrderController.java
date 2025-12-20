package ko.dh.goot.controller;


import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
import ko.dh.goot.service.PortoneApiService;
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
	private final PortoneApiService portoneApiService;

	private final ObjectMapper objectMapper;
	
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
        String userId = "user-1234"; // 임시 사용자 ID

        try {           
        	OrderResponse response = orderService.prepareOrder(orderRequest, userId); // 💡 Service 호출: 금액 재계산, DB 저장, orderId 반환

        	return ResponseEntity.ok(
                    Map.of("orderId", response.getOrderId())
                );
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
    
    /* ===============================
     * 2️. 결제 파라미터 생성
     * =============================== */
    @PostMapping("/createPaymentParams")
    public ResponseEntity<?> requestPayment(@RequestBody Map<String, Long> body) {
        Long orderId = body.get("orderId");

        Map<String, Object> paymentParams = orderService.createPaymentParams(orderId);

        return ResponseEntity.ok(paymentParams);
    }

    @PostMapping("/completePayment")
    public ResponseEntity<?> handlePaymentWebhook(
    		@RequestBody String payload,
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestHeader("webhook-timestamp") String webhookTimestamp){
    	
    	boolean verifyWebhook = webhookService.verifyWebhook(payload, webhookId, webhookSignature, webhookTimestamp);
		
    	System.out.println("payload::");
    	System.out.println(payload);
    	
    	if(!verifyWebhook) {
    		log.error("🚨 [Webhook] 시그니처 검증 실패. 위조 요청 가능성. payload={}", payload);
            // 403 Forbidden 대신 200 OK를 반환하여 PG사가 재시도를 멈추게 하는 경우도 있지만, 
            // 보안상으로는 실패 응답이 더 명확합니다. 여기서는 403을 반환합니다.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid Webhook Signature.");
    	}
    	
    	Map<String, Object> parsedPayload;
        try {
            parsedPayload = objectMapper.readValue(payload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("🚨 [Webhook] JSON 파싱 실패. payload={}", payload, e);
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid JSON payload."));
        }
    	
        try {
            // -----------------------------------------------------------
            // 💡 dataMap 변수 선언 및 초기화 (parsedPayload 사용)
            // -----------------------------------------------------------
            
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) parsedPayload.get("data"); 

            // V2 'data' 필드가 없으면, V1/최상위 구조로 폴백
            if (dataMap == null) {
                dataMap = parsedPayload; 
                System.out.println("⚠️ V2 'data' 필드 누락. V1/최상위 구조로 폴백하여 데이터 추출 시도.");
            }
            
            // 1. paymentId 추출 시도 (가장 중요한 값)
            String paymentId = (String) dataMap.get("paymentId"); 
            if (paymentId == null) {
                paymentId = (String) dataMap.get("id"); // 폴백 ID
            }
            
            // 2. 필수 데이터 (paymentId) 확인
            if (paymentId == null) { 
                log.error("필수 데이터 (paymentId) 추출 실패.");
                return ResponseEntity.badRequest().body(Map.of("message", "필수 데이터 (paymentId) 누락."));
            }
       
            System.out.println("결제 상세요청을 위한 paymentId ::");
            System.out.println(paymentId);
            // 🚨 여기서 paymentId를 사용하여 API 서비스 호출
            Map<String, Object> apiDetails = portoneApiService.portonePaymentDetails(paymentId);
            
            System.out.println("apiDetails::::::");
            System.out.println(apiDetails);

            Long orderId = (Long) apiDetails.get("orderId");

            try {
                
                System.out.println("✅ 최종 확보된 주문 ID (orderId): " + orderId);
            } catch (NumberFormatException e) {
                 throw new IllegalArgumentException("orderId 값이 유효한 숫자 형식이 아닙니다: " + orderId);
            }
            
            System.out.println("✅ 웹훅 시그니처 검증 및 API 데이터 확보 통과. 결제 확정 트랜잭션 시작.");
           
            orderService.completeOrderTransaction(paymentId, orderId);
            
            // 4. 웹훅 응답: 200 OK를 반환합니다.
            return ResponseEntity.ok(Map.of("message", "PG사 웹훅 처리 성공 및 주문 완료"));

        } catch (IllegalArgumentException e) {
            log.error("웹훅 데이터 형식 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "웹훅 데이터 형식 오류: " + e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("결제 검증/확정 비즈니스 오류 (웹훅): {}", e.getMessage());
            return ResponseEntity.ok(Map.of("message", "비즈니스 로직 오류로 처리 실패: " + e.getMessage()));
        } catch (Exception e) {
            log.error("결제 완료 처리 중 서버 오류 발생 (웹훅): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "웹훅 처리 중 서버 오류가 발생했습니다. PG사가 재시도할 것입니다."
            ));
        }
    	
    	
    	
    	
    }
    
 
}
