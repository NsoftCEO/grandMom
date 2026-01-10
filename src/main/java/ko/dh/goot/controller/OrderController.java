package ko.dh.goot.controller;


import java.util.Map;

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
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.dto.OrderRequest;
import ko.dh.goot.dto.OrderResponse;
import ko.dh.goot.dto.Product;
import ko.dh.goot.dto.ProductDetail;
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
        ProductDetail product = productService.selectProductDetail(productId); // 수정해야됨
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

    
 
}
