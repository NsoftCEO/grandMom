package ko.dh.goot.payment.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.common.exception.WebhookException;
import ko.dh.goot.payment.dao.PaymentRepository;
import ko.dh.goot.payment.dto.PortOnePaymentResponse;
import ko.dh.goot.payment.dto.WebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final WebhookService webhookService;
    private final PortoneApiService portoneApiService;
    private final PaymentTxService paymentTxService;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    

    private static final String TRANSACTION_PAID = "Transaction.Paid";
    private static final String TRANSACTION_FAILED = "Transaction.Failed";
    private static final String TRANSACTION_CANCELLED = "Transaction.Cancelled";

    /**
     * 웹훅 처리 메인 로직
     */
    public void handlePaymentWebhook(String payload, String webhookId, String webhookSignature, String webhookTimestamp) {
        // 1. 웹훅 시그니처 검증
        webhookService.verifyWebhook(payload, webhookId, webhookSignature, webhookTimestamp);
                
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
            // 파싱/데이터 오류 등 비즈니스 예외는 200 반환으로 무한 재시도 방어
            log.warn("[Webhook] warning error (ignored). message={}", e.getMessage(), e);
        } catch (Exception e) {
            // 시스템 에러는 500을 반환해서 포트원이 재시도하게 둠
            log.error("[Webhook] unexpected error", e);
            throw e; 
        }
    }
    
    public void processSuccessfulPayment(String portonePaymentId) {
        // 1. 멱등성 검증 (DB 조회: 이미 처리된 pgTxId인지 확인)
        if (paymentRepository.existsByPgTxId(portonePaymentId)) {
            log.info("[Webhook] 이미 처리된 결제입니다. pgTxId={}", portonePaymentId);
            return;
        }

        // 2. 외부 API 연동 (포트원에 실제 결제 내역 단건 조회 - 위변조 방지)
        PortOnePaymentResponse pgPayment = portoneApiService.portonePaymentDetails(portonePaymentId);
        if (pgPayment == null) {
            throw new BusinessException(ErrorCode.PG_PAYMENT_NOT_FOUND, "portonePaymentId=" + portonePaymentId);
        }

        // 3. 내부 트랜잭션 진입 (외부 객체의 메서드를 호출하므로 @Transactional 완벽 동작!)
        paymentTxService.processSuccessfulPaymentTx(pgPayment);
    }

    public void processFailedOrCancelledPayment(String portonePaymentId) {
        // 1. 외부 API 연동 (결제 내역 조회)
        PortOnePaymentResponse pgPayment = portoneApiService.portonePaymentDetails(portonePaymentId);
        if (pgPayment == null) {
            log.warn("[Webhook] 포트원 결제 내역을 찾을 수 없습니다. portonePaymentId={}", portonePaymentId);
            return;
        }

        // 2. 내부 트랜잭션 진입
        paymentTxService.processFailedOrCancelledPaymentTx(pgPayment);
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
        return paymentId;
    }
}