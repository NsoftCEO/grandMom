package ko.dh.goot.payment.dto;

import java.util.Map;
import java.util.UUID;

public record PaymentParamResponse(
        String storeId,
        String channelKey,
        String paymentId,
        String orderName,
        Long totalAmount,
        String currency,
        String payMethod,
        boolean isTestChannel,
        Map<String, String> customData
) {
    public static PaymentParamResponse of(
            String storeId,
            String channelKey,
            String orderName,
            Long totalAmount,
            Long orderId
    ) {
        return new PaymentParamResponse(
                storeId,
                channelKey,
                "payment-" + UUID.randomUUID(),
                orderName,
                totalAmount,
                "KRW",
                "EASY_PAY",
                true,
                Map.of("orderId", orderId.toString())
        );
    }
}