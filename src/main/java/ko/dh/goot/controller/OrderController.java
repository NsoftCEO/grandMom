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
        String currentUserId = "user-1234"; // 임시 사용자 ID

        try {           
        	OrderResponse response = orderService.prepareOrder(orderRequest, currentUserId); // 💡 Service 호출: 금액 재계산, DB 저장, orderId 반환

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
    
 
    @PostMapping("/completePayment")
    public ResponseEntity<?> handlePaymentWebhook(
    		@RequestBody String payload,
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestHeader("webhook-timestamp") String webhookTimestamp){
    	
    	boolean verifyWebhook = webhookService.verifyWebhook(payload, webhookId, webhookSignature, webhookTimestamp);
		
    	if(!verifyWebhook) {
    		log.error("🚨 [Webhook] 시그니처 검증 실패. 위조 요청 가능성. payload={}", payload);
            // 403 Forbidden 대신 200 OK를 반환하여 PG사가 재시도를 멈추게 하는 경우도 있지만, 
            // 보안상으로는 실패 응답이 더 명확합니다. 여기서는 403을 반환합니다.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid Webhook Signature.");
    	}
    	
    	return null;
    }
    
 
}
