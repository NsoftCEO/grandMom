package ko.dh.goot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PortOnePaymentResponse {

    private String id;
    private String status;
    private Amount amount;

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

	@Override
	public String toString() {
		return "PortOnePaymentResponse [id=" + id + ", status=" + status + ", amount=" + amount + ", customData="
				+ customData + "]";
	}
    
}
