package ko.dh.goot.payment.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/*
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOnePaymentResponse {

    private String id; // paymentId와 동일 포트원에서 id로 줌
    private String status;
    private Amount amount;
    private String pgTxId; 
    
    // apply하는 필드들
    private String provider;
    private LocalDateTime paidAt;
    private Long orderId;

    //⚠️ 문자열로 수신
    private String customData;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private Long total;
        private Long paid;
        private Long cancelled;
    }

    //customData 내부 JSON 전용 DTO
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

    public void applyProvider(String provider) {
    	if (provider == null) {
            throw new IllegalArgumentException("세팅할 provider가 없습니다.");
        }
        this.provider = provider;
    }

    public void applyPaidAt(LocalDateTime paidAt) {
    	if (paidAt == null) {
            throw new IllegalArgumentException("세팅할 paidAt가 없습니다.");
        }
        this.paidAt = paidAt;
    }
    
	@Override
	public String toString() {
		return "PortOnePaymentResponse [id=" + id + ", status=" + status + ", amount=" + amount + ", pgTxId=" + pgTxId
				+ ", orderId=" + orderId + ", customData=" + customData + "]";
	}
    
	
    
}*/
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
