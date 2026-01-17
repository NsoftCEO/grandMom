package ko.dh.goot.order.dto;

public class OrderResponse {
    private Long orderId;
    private int expectedAmount; // 서버가 최종 확정한 금액

    public OrderResponse(Long orderId, int expectedAmount) {
        this.orderId = orderId;
        this.expectedAmount = expectedAmount;
    }

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public int getExpectedAmount() {
		return expectedAmount;
	}

	public void setExpectedAmount(int expectedAmount) {
		this.expectedAmount = expectedAmount;
	}

    
}