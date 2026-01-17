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
    	
        paymentService.handlePaymentWebhook(payload, webhookId, webhookSignature, webhookTimestamp);
        
        
        return ResponseEntity.ok(Map.of("message", "PG사 웹훅 처리 성공 및 주문 완료"));
    }
}