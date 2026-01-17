package ko.dh.goot.payment.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    private String paymentId;
    private Long orderId;
    private String paymentProvider;      // KAKAOPAY
    private String paymentMethodType;    // MONEY, CARD, etc
    private String paymentStatus;
    private String pgTxId;
    private LocalDateTime paidAt;
    private Integer amount;
    private Integer cancelAmount;
}