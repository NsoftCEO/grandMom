package ko.dh.goot.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class PaymentParamRequest {
    @NotNull
    @Positive
    private Long orderId;
}
