package ko.dh.goot.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.order.dao.OrderItemMapper;
import ko.dh.goot.order.dao.OrderMapper;
import ko.dh.goot.order.dto.Order;
import ko.dh.goot.order.dto.OrderItem;
import ko.dh.goot.order.service.OrderService;
import ko.dh.goot.payment.dao.PaymentMapper;
import ko.dh.goot.payment.dto.PortOnePaymentResponse;
import ko.dh.goot.payment.dto.WebhookPayload;
import ko.dh.goot.product.service.ProductOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final ProductOptionService productOptionService;
	private final OrderService orderService;
	private final WebhookService webhookService;
	private final PortoneApiService portoneApiService;	
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
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
        	System.out.println(payloadData.toString());
        	log.info("{}", payloadData);
        	       	
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

	// todo: portone 외부연동은 트랜잭션밖으로 빼야됨
	@Transactional
    public void confirmPaymentAndCompleteOrder(String paymentId) {

        /* ===== 1. 멱등성 ===== */
        if (paymentMapper.existsByPaymentId(paymentId) > 0) {
        	log.info("이미 처리된 결제. paymentId={}", paymentId);
            return;
        }

        /* ===== 2. PG 결제 조회(외부 연동) ===== */
        PortOnePaymentResponse portonePaymentDetails = portoneApiService.portonePaymentDetails(paymentId);
        
        if (portonePaymentDetails == null) {
            throw new BusinessException(ErrorCode.PG_PAYMENT_NOT_FOUND);
        }
  
        Long orderId = portonePaymentDetails.getOrderId();

        /* ===== 3. 주문 조회 ===== */
        Order order = orderMapper.selectOrder(orderId);
        if (order == null) {
        	throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        /* ===== 4. 금액 검증 ===== */
        Long paidAmount = portonePaymentDetails.getAmount().getTotal();
        if (!paidAmount.equals(Long.valueOf(order.getTotalAmount()))) {
        	throw new BusinessException(
                    ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "주문금액=" + order.getTotalAmount() + ", 결제금액=" + paidAmount
                );
        }

        // ===== 5. 결제 저장 =====
        paymentMapper.insertPayment(portonePaymentDetails);
        
        /* ===== 6. 주문상품 조회 (단일 옵션) ===== */
        OrderItem orderItem = orderItemMapper.selectOrderItemByOrderId(orderId);
        if (orderItem == null) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND);
        }
        System.out.println("orderItem:::::");
        System.out.println(orderItem.toString());
        // ===== 7. 재고 차감 =====
        // decreaseStock(Long optionId, int orderQuantity)
        productOptionService.decreaseStock(orderItem.getOptionId(), orderItem.getQuantity());

        // ===== 8. 주문 상태 변경 =====
        int resultCount = orderService.changeOrderStatus(orderId,"PAYMENT_READY", portonePaymentDetails.getStatus());
        
        if(resultCount != 1) {
        	throw new BusinessException(ErrorCode.ORDER_STATUS_UPDATE_FAILED,
                    "orderId=" + orderId
                );
        }

        
    }
	
    
}
