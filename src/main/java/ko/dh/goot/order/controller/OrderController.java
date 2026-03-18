package ko.dh.goot.order.controller;


import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import ko.dh.goot.order.dto.OrderProductView;
import ko.dh.goot.order.dto.OrderRequest;
import ko.dh.goot.order.dto.OrderResponse;
import ko.dh.goot.order.service.OrderService;
import ko.dh.goot.payment.dto.PaymentParamRequest;
import ko.dh.goot.payment.dto.PaymentParamResponse;
import ko.dh.goot.security.principal.SecurityUserDetails;
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

	private final OrderService orderService;

	
	 // 주문 페이지로 이동
    @GetMapping("/detail")
    public String orderDetail(@RequestParam("optionId") long optionId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            Model model) throws NotFoundException {
    	
        OrderProductView orderProduct = orderService.selectOrderProduct(optionId, quantity); // 수정해야됨
        model.addAttribute("product", orderProduct);
        model.addAttribute("quantity", quantity);
        model.addAttribute("storeId", storeId);
        model.addAttribute("kakaoChannelKey", kakaoChannelKey);
        System.out.println("orderProduct::");
        System.out.println(orderProduct);
        return "order/orderDetail"; // order.html 템플릿 렌더링
    }

    @PostMapping("/prepareOrder")
    public ResponseEntity<OrderResponse> prepareOrder(@Valid @RequestBody OrderRequest orderRequest, @AuthenticationPrincipal SecurityUserDetails userDetails) {
    
    	String userId = userDetails.getUserId();
       
        OrderResponse response = orderService.prepareOrder(orderRequest, userId); // 💡 Service 호출: 금액 재계산, DB 저장, orderId 반환

        return ResponseEntity.ok(response);

    }
    
    /* ===============================
     * 2️. 결제 파라미터 생성
     * =============================== */
    @PostMapping("/createPaymentParams")
    public ResponseEntity<PaymentParamResponse> requestPayment(@Valid @RequestBody PaymentParamRequest request) {
        PaymentParamResponse response = orderService.createPaymentParams(request.getOrderId());

        return ResponseEntity.ok(response);
    }

    
 
}
