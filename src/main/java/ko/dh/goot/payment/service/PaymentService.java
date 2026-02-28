package ko.dh.goot.payment.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.common.exception.WebhookException;
import ko.dh.goot.order.dao.OrderRepository;
import ko.dh.goot.order.domain.Order;
import ko.dh.goot.payment.dao.PaymentRepository;
import ko.dh.goot.payment.domain.Payment;
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
	private final WebhookService webhookService;
	private final PortoneApiService portoneApiService;	
    private final PaymentRepository paymentRepository; 
    private final OrderRepository orderRepository ;
    private final ObjectMapper objectMapper;
    
    
    @Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.api-secret}")
    private String apiSecret;
    
    private static final String TRANSACTION_PAID = "Transaction.Paid";
    private static final String TRANSACTION_FAILED = "Transaction.Failed";
    private static final String TRANSACTION_CANCELLED = "Transaction.Cancelled";

    // 웹훅에서 500을 return하면 웹훅 수백 번 재전송 따라서 실패했어도 DB에 남기고, 200을 준다.
	public void handlePaymentWebhook(String payload, String webhookId, String webhookSignature, String webhookTimestamp) {

		try {
			webhookService.verifyWebhook(payload, webhookId, webhookSignature, webhookTimestamp);
		}catch (WebhookException e) {
			throw e;
		}
				
    	try {   	
    		
    		WebhookPayload payloadData = parseWebhookPayload(payload);
    		log.info("[Webhook] payload={}", payloadData);
        	       	
    		String webhookType = payloadData.getType();
    		String portonePaymentId = extractPaymentId(payloadData);

            if (TRANSACTION_PAID.equals(webhookType)) {
                processSuccessfulPayment(portonePaymentId);
            } else if (TRANSACTION_FAILED.equals(webhookType) || TRANSACTION_CANCELLED.equals(webhookType)) {
                processFailedOrCancelledPayment(portonePaymentId);
            } else {
                log.info("[Webhook] 무시하는 이벤트 타입입니다. type={}", webhookType);
            }   	
        	
    	} catch (WebhookException | BusinessException e) {
            // WebhookException(파싱/데이터오류)과 BusinessException은 200 반환으로 무한 재시도 방어
            log.warn("[Webhook] warning error (ignored). message={}", e.getMessage(), e);
            return;
        } catch (Exception e) {
            // 그 외 DB Connection 등의 시스템 에러는 500 반환해서 재시도 요청
            log.error("[Webhook] unexpected error", e);
            throw e; 
        }
    }
	
	public void processSuccessfulPayment(String portonePaymentId) {
        // 1. 멱등성 검증 (DB 조회: 이미 처리된 pgTxId인지 확인)
        if (paymentRepository.existsByPgTxId(portonePaymentId)) {
            log.info("[Webhook] 이미 처리된 결제입니다. pgTxId(portonePaymentId)={}", portonePaymentId);
            return;
        }

        // 2. 외부 API 연동 (포트원에 실제 결제 내역 단건 조회 - 위변조 방지)
        PortOnePaymentResponse pgPayment = portoneApiService.portonePaymentDetails(portonePaymentId);
        if (pgPayment == null) {
            throw new BusinessException(ErrorCode.PG_PAYMENT_NOT_FOUND, "portonePaymentId=" + portonePaymentId);
        }

        // 3. 내부 트랜잭션 진입 (도메인 상태 변경 및 DB 반영)
        processSuccessfulPaymentTx(pgPayment);
    }

	@Transactional
    public void processSuccessfulPaymentTx(PortOnePaymentResponse pgPayment) {
        Long orderId = pgPayment.getOrderId(); // 포트원 customData 등에서 추출한 내부 주문 ID
        
        // 1. Aggregate Root(Order) 조회
        Order order = getOrderAggregate(orderId);

        // 2. 금액 검증 (위변조 검증의 핵심)
        int paidAmount = pgPayment.getAmount().getTotal();
        if (order.getTotalAmount() != paidAmount) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "주문금액=" + order.getTotalAmount() + ", 결제금액=" + paidAmount);
        }

        // 3. 결제 도메인(Payment) 생성 및 영속화
        try {
        	// 안전한 값 추출 (Null Safe)
        	String provider = pgPayment.getMethod() != null ? pgPayment.getMethod().getProvider() : "unknown";
            String methodType = pgPayment.getMethod() != null ? pgPayment.getMethod().getType() : "unknown";
            String pgTxId = pgPayment.getPgTxId() != null ? pgPayment.getPgTxId() : pgPayment.getId();
            LocalDateTime paidAt = pgPayment.getPaidAt() != null ? pgPayment.getPaidAt().toLocalDateTime() : LocalDateTime.now();

            Payment payment = Payment.create(
                pgPayment.getId(),
                orderId,
                provider,
                methodType,
                pgTxId,
                paidAt,
                paidAmount,
                pgPayment.getStatus()
            );
            
            paymentRepository.save(payment); 
        } catch (DataIntegrityViolationException e) {
            // 동시성 문제로 Insert가 충돌난 경우 이미 처리된 것으로 간주 (멱등성 보장)
            log.info("[Webhook] 동시 요청으로 인한 중복 결제 처리 방어. pgTxId={}", pgPayment.getId());
            return; 
        }
        
        // 4. 주문 도메인 상태 변경 (JPA Dirty Checking으로 자동 UPDATE)
        order.completePayment();

        // 5. 부가 로직: 재고 차감 (Aggregate 내부 컬렉션 활용)
        order.getOrderItems().forEach(item -> 
            productOptionService.decreaseStock(item.getOptionId(), item.getQuantity())
        );
        
        log.info("[Webhook] 결제 성공 처리 완료. orderId={}", orderId);
    }

    /* =========================================================================
     * 유스케이스 2: 결제 실패 및 취소 동기화
     * ========================================================================= */
    
    public void processFailedOrCancelledPayment(String portonePaymentId) {
        // 1. 외부 연동
        PortOnePaymentResponse pgPayment = portoneApiService.portonePaymentDetails(portonePaymentId);
        if (pgPayment == null) {
            log.warn("[Webhook] 포트원 결제 내역을 찾을 수 없습니다. portonePaymentId={}", portonePaymentId);
            return;
        }

        // 2. 내부 트랜잭션
        processFailedOrCancelledPaymentTx(pgPayment);
    }

    @Transactional
    public void processFailedOrCancelledPaymentTx(PortOnePaymentResponse pgPayment) {
        Long orderId = pgPayment.getOrderId();
        Order order = getOrderAggregate(orderId);

        String currentStatus = order.getOrderStatus().name();
        String pgStatus = pgPayment.getStatus(); 
        
        // 1. 취소 이벤트 (CANCELLED)
        if ("CANCELLED".equals(pgStatus)) {
            if ("CANCELLED".equals(currentStatus)) {
                log.info("[Webhook] 이미 취소 처리된 주문입니다. orderId={}", orderId);
                return;
            }

            // [도메인 1] 주문 취소 상태로 변경
            order.cancel(); 
            
            // [도메인 2] 결제 정보 취소 상태로 변경 (부분 취소/전액 취소 로직 적용)
            paymentRepository.findById(pgPayment.getId()).ifPresent(payment -> {
                int cancelAmount = (pgPayment.getAmount() != null && pgPayment.getAmount().getCancelled() != null)
                        ? (int) pgPayment.getAmount().getCancelled() : 0;
                payment.cancel(cancelAmount); // Payment 엔티티 내부에서 상태 및 금액 검증
            });

            // [부가 로직] 재고 복구
            order.getOrderItems().forEach(item -> 
                productOptionService.increaseStock(item.getOptionId(), item.getQuantity())
            );
            log.info("[Webhook] 결제 취소 동기화 완료 및 재고 복구. orderId={}", orderId);
            
        } 
        // 2. 실패 이벤트 (FAILED)
        else {
            if ("PAYMENT_FAILED".equals(currentStatus)) return; // 이미 실패 처리됨
            
            order.cancel(); // 또는 order.failPayment() 등 명확한 상태 도입 권장
            
            paymentRepository.findByPgTxId(pgPayment.getId()).ifPresent(Payment::fail);
            log.info("[Webhook] 결제 실패 동기화 완료. orderId={}", orderId);
        }
    }

    /* =========================================================================
     * Private Helper Methods 
     * ========================================================================= */

    private WebhookPayload parseWebhookPayload(String payload) {
        try {
            return objectMapper.readValue(payload, WebhookPayload.class);
        } catch (JsonProcessingException e) {
            throw new WebhookException(ErrorCode.WEBHOOK_INVALID_PAYLOAD, "JSON Parsing Error");
        }
    }

    private String extractPaymentId(WebhookPayload payloadData) {
        String paymentId = payloadData.getData() != null ? payloadData.getData().getPaymentId() : null;
        if (paymentId == null) {
            throw new WebhookException(ErrorCode.WEBHOOK_INVALID_PAYLOAD, "paymentId is null in payload");
        }
        return paymentId; // 포트원 V2에서는 이게 imp_uid 역할
    }

    private Order getOrderAggregate(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "orderId=" + orderId));
    }
}
/*
	public void confirmPaymentAndCompleteOrder(String paymentId) {

	    // ===== 2. PG 결제 조회 (외부 연동은 트랜잭션 포함x) ===== 
	    PortOnePaymentResponse pgPayment = portoneApiService.portonePaymentDetails(paymentId);

	    if (pgPayment == null) {
	        throw new BusinessException(ErrorCode.PG_PAYMENT_NOT_FOUND, "paymentId=" + paymentId);
	    }

	    // ===== 3. 내부 트랜잭션 =====
	    confirmPaymentInternal(pgPayment);
	}

	@Transactional
    public void confirmPaymentInternal(PortOnePaymentResponse pgPayment) {

        Long orderId = pgPayment.getOrderId();

        //===== 4. 주문 조회 =====
        OrderRecord order = orderMapper.selectOrder(orderId);
        if (order == null) {
        	throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "orderId=" + orderId);
        }

        // ===== 5. 금액 검증 =====
        Long paidAmount = pgPayment.getAmount().getTotal();
        if (!paidAmount.equals(Long.valueOf(order.totalAmount()))) {
        	throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "주문금액=" + order.totalAmount() + ", 결제금액=" + paidAmount
                );
        }

        // ===== 6. 결제 저장 =====
        try {
            paymentMapper.insertPayment(pgPayment);
        } catch (DuplicateKeyException e) {
            log.info("[Webhook] 이미 처리된 결제 (DB unique). paymentId={}", pgPayment.getId());
            return; // 200 OK 리턴
        }
        
        // ===== 7. 주문상품 조회 (단일 옵션) =====
        OrderItemRecord orderItem = orderItemMapper.selectOrderItemByOrderId(orderId);
        if (orderItem == null) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND, "orderId=" + orderId);
        }

        // ===== 8. 재고 차감 =====
        productOptionService.decreaseStock(orderItem.optionId(), orderItem.quantity());

        // ===== 9. 주문 상태 변경 =====
        int resultCount = orderService.changeOrderStatus(orderId,"PAYMENT_READY", pgPayment.getStatus());
       
        if(resultCount != 1) {
        	throw new BusinessException(ErrorCode.ORDER_STATUS_UPDATE_FAILED, "orderId=" + orderId);                    
        }

        
    }
	*/

