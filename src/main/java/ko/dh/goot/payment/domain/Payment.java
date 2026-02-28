package ko.dh.goot.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    private String paymentId; // merchant_uid
    private Long orderId;
    private String paymentProvider;
    private String paymentMethodType;
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    private String pgTxId;
    private LocalDateTime paidAt;
    private int amount;
    private int cancelAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Payment(String paymentId,
                    Long orderId,
                    String paymentProvider,
                    String paymentMethodType,
                    String pgTxId,
                    LocalDateTime paidAt,
                    int amount,
                    PaymentStatus paymentStatus) {

        this.paymentId = paymentId;
        this.orderId = orderId;
        this.paymentProvider = paymentProvider;
        this.paymentMethodType = paymentMethodType;
        this.pgTxId = pgTxId;
        this.paidAt = paidAt;
        this.amount = amount;
        this.cancelAmount = 0;
        this.paymentStatus = paymentStatus;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static Payment create(String paymentId,
                                 Long orderId,
                                 String paymentProvider,
                                 String paymentMethodType,
                                 String pgTxId,
                                 LocalDateTime paidAt,
                                 int amount,
                                 String pgStatus) {

        return new Payment(
                paymentId,
                orderId,
                paymentProvider,
                paymentMethodType,
                pgTxId,
                paidAt,
                amount,
                PaymentStatus.from(pgStatus)
        );
    }

    public void cancel(int cancelAmount) {
        if (this.paymentStatus == PaymentStatus.CANCELLED
                && this.cancelAmount == this.amount) {
            throw new IllegalStateException("이미 전액 취소된 결제입니다.");
        }

        int newCancelAmount = this.cancelAmount + cancelAmount;

        if (newCancelAmount > this.amount) {
            throw new IllegalArgumentException("취소 금액이 결제 금액을 초과할 수 없습니다.");
        }

        this.cancelAmount = newCancelAmount;

        if (this.cancelAmount == this.amount) {
            this.paymentStatus = PaymentStatus.CANCELLED;
        }

        this.updatedAt = LocalDateTime.now();
    }

    public void fail() {
        if (this.paymentStatus == PaymentStatus.FAILED) {
            throw new IllegalStateException("이미 실패 처리된 결제입니다.");
        }

        this.paymentStatus = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }
}