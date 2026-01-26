package ko.dh.goot.payment.controller;


import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import ko.dh.goot.payment.dto.PaymentReadyResponse;
import ko.dh.goot.payment.service.PaymentService;

import org.springframework.ui.Model;


@Controller
@RequiredArgsConstructor
@RequestMapping("payment")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/payment")
    public String paymentPage() {
    	System.out.println("paymentPage:::ㄴㄴㄴㄴ");
        return "payment/payment";
    }

    @PostMapping("/handlePaymentWebhook")
    public ResponseEntity<?> handlePaymentWebhook(
    		@RequestBody String payload,
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestHeader("webhook-timestamp") String webhookTimestamp){
    	
    	// 시그니처 오류 시 Exception 발생 -> GlobalHandler가 401 응답
        // 그 외 비즈니스 오류는 서비스 내부 catch 후 정상 종료 (200 응답 유도)
        paymentService.handlePaymentWebhook(payload, webhookId, webhookSignature, webhookTimestamp);
        
        
        return ResponseEntity.ok().build(); // 항상 200
    }
}