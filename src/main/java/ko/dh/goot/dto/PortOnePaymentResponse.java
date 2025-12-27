package ko.dh.goot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class PortOnePaymentResponse {

    private String id;
    private String status;
    private Amount amount;
    
    private Long orderId;

    /** ⚠️ 문자열로 수신 */
    private String customData;

    @Getter
    @NoArgsConstructor
    public static class Amount {
        private Long total;
        private Long paid;
        private Long cancelled;
    }

    /** customData 내부 JSON 전용 DTO */
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
    
	@Override
	public String toString() {
		return "PortOnePaymentResponse [id=" + id + ", status=" + status + ", amount=" + amount + ", customData="
				+ customData + "]";
	}
    
}
