package ko.dh.goot.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WebhookPayload {

    private String type;
    private String timestamp;
    private Data data;

    @Getter
    @NoArgsConstructor
    public static class Data {
        private String paymentId;
        private String transactionId;
        private String storeId;
        
        @Override
        public String toString() {
            return "Data{" +
                   "paymentId='" + paymentId + '\'' +
                   ", transactionId='" + transactionId + '\'' +
                   ", storeId='" + storeId + '\'' +
                   '}';
        }
    }

	@Override
	public String toString() {
		return "WebhookPayload [type=" + type + ", timestamp=" + timestamp + ", data=" + data + "]";
	}
    
    
}
