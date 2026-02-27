package ko.dh.goot.payment.persistence;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record PaymentRecord(
        String paymentId,
        Long orderId,
        String paymentProvider,   // KAKAOPAY
        String paymentMethodType, // MONEY, CARD, etc
        String paymentStatus,
        String pgTxId,
        LocalDateTime paidAt,
        Integer amount,
        Integer cancelAmount
) {
}