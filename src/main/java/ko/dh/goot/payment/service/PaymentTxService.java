package ko.dh.goot.payment.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.order.dao.OrderRepository;
import ko.dh.goot.order.domain.Order;
import ko.dh.goot.payment.dao.PaymentRepository;
import ko.dh.goot.payment.domain.Payment;
import ko.dh.goot.payment.dto.PortOnePaymentResponse;
import ko.dh.goot.product.service.ProductOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentTxService {

    private final ProductOptionService productOptionService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void processSuccessfulPaymentTx(PortOnePaymentResponse pgPayment) {
        Long orderId = pgPayment.getOrderId();

        // 1. 주문 조회
        Order order = getOrderAggregate(orderId);

        // 2. 금액 검증 책임 위임
        int paidAmount = (pgPayment.getAmount() != null && pgPayment.getAmount().getTotal() != null) 
                ? pgPayment.getAmount().getTotal() : 0;
        
        order.completePayment(paidAmount);

        // 3. 결제 내역 저장
        try {
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
            // 동시 요청 멱등성 방어
            log.info("[Webhook] 동시 요청으로 인한 중복 결제 처리 방어. pgTxId={}", pgPayment.getId());
            return; 
        }
        
        log.info("[Webhook] 결제 성공 처리 완료. orderId={}", orderId);
    }

    @Transactional
    public void processFailedOrCancelledPaymentTx(PortOnePaymentResponse pgPayment) {
        Long orderId = pgPayment.getOrderId();
        Order order = getOrderAggregate(orderId);

        String currentStatus = order.getOrderStatus().name();
        String pgStatus = pgPayment.getStatus(); 
        
        if ("CANCELLED".equals(pgStatus)) {
            if ("CANCELLED".equals(currentStatus)) {
                log.info("[Webhook] 이미 취소 처리된 주문입니다. orderId={}", orderId);
                return;
            }

            order.cancel(); 
            
            paymentRepository.findByPgTxId(pgPayment.getId()).ifPresent(payment -> {
                int cancelAmount = (pgPayment.getAmount() != null && pgPayment.getAmount().getCancelled() != null)
                        ? pgPayment.getAmount().getCancelled() : 0;
                payment.cancel(cancelAmount);
            });

            order.getOrderItems().forEach(item -> 
                productOptionService.increaseStock(item.getOptionId(), item.getQuantity())
            );
            log.info("[Webhook] 결제 취소 동기화 완료 및 재고 복구. orderId={}", orderId);
            
        } else {
            if ("PAYMENT_FAILED".equals(currentStatus)) return;
            
            order.cancel(); 
            paymentRepository.findByPgTxId(pgPayment.getId()).ifPresent(Payment::fail);
            log.info("[Webhook] 결제 실패 동기화 완료. orderId={}", orderId);
        }
    }

    private Order getOrderAggregate(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "orderId=" + orderId));
    }
}