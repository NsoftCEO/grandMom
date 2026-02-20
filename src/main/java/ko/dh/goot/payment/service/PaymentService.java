package ko.dh.goot.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.common.exception.WebhookException;
import ko.dh.goot.order.dao.OrderItemMapper;
import ko.dh.goot.order.dao.OrderMapper;
import ko.dh.goot.order.domain.Order;
import ko.dh.goot.order.domain.OrderItem;
import ko.dh.goot.order.entity.OrderItemEntity;
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

    // 웹훅에서 500을 return하면 웹훅 수백 번 재전송 따라서 실패했어도 DB에 남기고, 200을 준다.
	public void handlePaymentWebhook(String payload, String webhookId, String webhookSignature, String webhookTimestamp) {
		
		// try밖에 둬서 에러 catch안되고 403에러 던지게 함.
		webhookService.verifyWebhook(payload, webhookId, webhookSignature, webhookTimestamp);
		
    	try {   	
    		
    		WebhookPayload payloadData = objectMapper.readValue(payload, WebhookPayload.class);

    		log.info("[Webhook] payload={}", payloadData);
        	       	
        	if (!TRANSACTION_PAID.equals(payloadData.getType())) {
                log.info("[Webhook] Ignore type={}", payloadData.getType());
                return; // 정상 return, 200리턴하여 웹훅요청 막음
            }
        	
        	String paymentId = payloadData.getData().getPaymentId();
        	
        	if (payloadData.getData() == null || paymentId == null) {
        		throw new WebhookException(ErrorCode.WEBHOOK_INVALID_PAYLOAD, "paymentId=" + paymentId + "payloadData.getData()" + payloadData.getData());
            } // 500에러 반환
 	
        	/* ===== 1. 멱등성 (가장 먼저) ===== */
    	    if (paymentMapper.existsByPaymentId(paymentId) > 0) {
    	        log.info("이미 처리된 결제. paymentId={}", paymentId);
    	        return;
    	    }
    	    
        	confirmPaymentAndCompleteOrder(paymentId);        	
        	
    	} catch (JsonProcessingException e) {
            log.error("🚨 [Webhook] JSON 파싱 실패. payload={}", payload, e);
            return;
    	} catch (BusinessException e) {
    	    log.warn("[Webhook] business error. code={}, message={}", e.getErrorCode().getCode(), e.getMessage(), e);
    	    return;
    	} catch (Exception e) {
    	    log.error("[Webhook] unexpected error", e);
    	    throw e; // 서버에러는 500반환해서 재시도 요청
    	}
    	
        
	}

	public void confirmPaymentAndCompleteOrder(String paymentId) {

	    /* ===== 2. PG 결제 조회 (외부 연동은 트랜잭션 포함x) ===== */
	    PortOnePaymentResponse pgPayment =
	            portoneApiService.portonePaymentDetails(paymentId);

	    if (pgPayment == null) {
	        throw new BusinessException(ErrorCode.PG_PAYMENT_NOT_FOUND, "paymentId=" + paymentId);
	    }

	    /* ===== 3. 내부 트랜잭션 ===== */
	    confirmPaymentInternal(pgPayment);
	}

	@Transactional
    public void confirmPaymentInternal(PortOnePaymentResponse pgPayment) {

        Long orderId = pgPayment.getOrderId();

        /* ===== 4. 주문 조회 ===== */
        Order order = orderMapper.selectOrder(orderId);
        if (order == null) {
        	throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "orderId=" + orderId);
        }

        /* ===== 5. 금액 검증 ===== */
        Long paidAmount = pgPayment.getAmount().getTotal();
        if (!paidAmount.equals(Long.valueOf(order.getTotalAmount()))) {
        	throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "주문금액=" + order.getTotalAmount() + ", 결제금액=" + paidAmount
                );
        }

        // ===== 6. 결제 저장 =====
        try {
            paymentMapper.insertPayment(pgPayment);
        } catch (DuplicateKeyException e) {
            log.info("[Webhook] 이미 처리된 결제 (DB unique). paymentId={}", pgPayment.getId());
            return; // 200 OK 리턴
        }
        
        /* ===== 7. 주문상품 조회 (단일 옵션) ===== */
        OrderItemEntity orderItem = orderItemMapper.selectOrderItemByOrderId(orderId);
        if (orderItem == null) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND, "orderId=" + orderId);
        }

        // ===== 8. 재고 차감 =====
        productOptionService.decreaseStock(orderItem.getOptionId(), orderItem.getQuantity());

        // ===== 9. 주문 상태 변경 =====
        int resultCount = orderService.changeOrderStatus(orderId,"PAYMENT_READY", pgPayment.getStatus());
       
        if(resultCount != 1) {
        	throw new BusinessException(ErrorCode.ORDER_STATUS_UPDATE_FAILED, "orderId=" + orderId);                    
        }

        
    }
	
    
}
