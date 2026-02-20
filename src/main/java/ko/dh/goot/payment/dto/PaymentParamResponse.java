package ko.dh.goot.payment.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentParamResponse {

    private String storeId;
    private String channelKey;
    private String paymentId;
    private String orderName;
    private Integer totalAmount;
    private String currency;
    private String payMethod;
    private boolean isTestChannel;
    private Map<String, String> customData;
}