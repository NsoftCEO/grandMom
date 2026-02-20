package ko.dh.goot.payment.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOnePaymentResponse {

    private String id;
    private String status;
    private String pgTxId;
    
    private String customData;// 문자열로 수신
    private Long orderId;

    private Method method;
    private Amount amount;
    private OffsetDateTime paidAt;

    @Getter
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Method {
        private String type;
        private String provider;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private Long total;
        private Long paid;
        private Long cancelled;
    }
    
    @Getter
    @NoArgsConstructor
    public static class CustomData {
        private Long orderId;
    }
    
    public void applyOrderId(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("세팅할 orderId가 없습니다.");
        }
        this.orderId = orderId;
    }
}
