package ko.dh.goot.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.controller.OrderController;
import ko.dh.goot.dao.OrderMapper;
import ko.dh.goot.dao.PaymentMapper;
import ko.dh.goot.dto.Order;
import ko.dh.goot.dto.Payment;
import ko.dh.goot.dto.PortOnePaymentResponse;
import ko.dh.goot.dto.WebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final OrderService orderService;
	private final WebhookService webhookService;
	private final PortoneApiService portoneApiService;	
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    
    
    @Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.api-secret}")
    private String apiSecret;
    
    private static final String TRANSACTION_PAID = "Transaction.Paid";

    
	public void handlePaymentWebhook(String payload, String webhookId, String webhookSignature, String webhookTimestamp) {
		boolean verifyWebhook = webhookService.verifyWebhook(payload, webhookId, webhookSignature, webhookTimestamp);
	
    	if(!verifyWebhook) {
    		log.error("🚨 [Webhook] 시그니처 검증 실패. 위조 요청 가능성. payload={}", payload); 
    		throw new IllegalArgumentException("Invalid Webhook Signature.");
    	}
    	
    	try {
    		WebhookPayload payloadData = objectMapper.readValue(payload, WebhookPayload.class);
    		
    		System.out.println("payloadData::");
        	System.out.println(payloadData);
        	       	
        	if (!TRANSACTION_PAID.equals(payloadData.getType())) {
                log.info("[Webhook] Ignore type={}", payloadData.getType());
                return;
            }
        	
        	if (payloadData.getData() == null || payloadData.getData().getPaymentId() == null) {
                log.error("🚨 [Webhook] paymentId 누락. payload={}", payload);
                return;
            }
	
        	String paymentId = payloadData.getData().getPaymentId();
        	
        	confirmPaymentAndCompleteOrder(paymentId);
        	
        	
    	} catch (JsonProcessingException e) {
            log.error("🚨 [Webhook] JSON 파싱 실패. payload={}", payload, e);
            return;
        } catch (Exception e) {
            log.error("🚨 [Webhook] 처리 중 예외 발생", e);
            return;
        }
    	
        
	}

	@Transactional
    public void confirmPaymentAndCompleteOrder(String paymentId) {

        /* ===== 1. 멱등성 ===== */
        if (paymentMapper.existsByPaymentId(paymentId) > 0) {
            log.info("이미 존재하는 주문번호. paymentId={}", paymentId);
            return;
        }

        /* ===== 2. PG 결제 조회 ===== */
        PortOnePaymentResponse portonePaymentDetails = portoneApiService.portonePaymentDetails(paymentId);

        Long orderId = extractOrderId(portonePaymentDetails.getCustomData());

        /* ===== 3. 주문 조회 ===== */
        Order order = orderMapper.selectOrder(orderId);
        if (order == null) {
            throw new IllegalStateException("주문 없음. orderId=" + orderId);
        }

        /* ===== 4. 금액 검증 ===== */
        Long paidAmount = portonePaymentDetails.getAmount().getTotal();
        if (!paidAmount.equals(Long.valueOf(order.getTotalAmount()))) {
            throw new IllegalStateException(
                "결제금액 불일치. order=" + order.getTotalAmount()
                    + ", paid=" + paidAmount
            );
        }
/*
        // ===== 5. 결제 저장 =====
        paymentMapper.insertPayment(paymentId, orderId, paidAmount);

        // ===== 6. 주문 상태 변경 =====
        orderService.changeOrderStatus(
            orderId,
            "PAYMENT_READY",
            "PAID"
        );

        // ===== 7. 재고 차감 =====
        orderService.decreaseStockByOrder(orderId);*/
    }
	
	private Long extractOrderId(String customData) {

	    if (customData == null || customData.isBlank()) {
	        return null;
	    }

	    try {
	        PortOnePaymentResponse.CustomData data =
	            objectMapper.readValue(
	                customData,
	                PortOnePaymentResponse.CustomData.class
	            );

	        return data.getOrderId();

	    } catch (Exception e) {
	        throw new IllegalStateException(
	            "customData 파싱 실패: " + customData, e
	        );
	    }
	}

    
}
