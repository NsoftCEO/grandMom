package ko.dh.goot.order.domain;

import java.time.LocalDateTime;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.payment.domain.RefundStatus;
import lombok.Getter;

@Getter
public class OrderItem {

    private Long orderItemId;

    private final Long productId;
    private final Long optionId;

    private final String productName;
    private final int unitPrice;
    private final int quantity;
    private final int totalPrice;

    private final String color;
    private final String size;
    
    private final RefundStatus refundStatus;
    private final LocalDateTime createdAt;

    private OrderItem(
            Long productId,
            Long optionId,
            String productName,
            int unitPrice,
            int quantity,
            String color,
            String size
    ) {
        if (quantity <= 0) throw new BusinessException(ErrorCode.ORDER_INVALID_QUANTITY);
        if (unitPrice < 0) throw new BusinessException(ErrorCode.ORDER_INVALID_UNIT_PRICE);

        this.productId = productId;
        this.optionId = optionId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = unitPrice * quantity;
        this.color = color;
        this.size = size;
        this.refundStatus = RefundStatus.NONE;
        this.createdAt = LocalDateTime.now();
    }

    public static OrderItem create(Long productId,
            Long optionId,
            String productName,
            int unitPrice,
            int quantity,
            String color,
            String size) {
    	
    	return new OrderItem(productId, optionId, productName,
    	unitPrice, quantity, color, size);
    }
    
    	



    /*
    public void changeQuantity(int newQuantity) {
        if (newQuantity <= 0) throw new BusinessException(ErrorCode.ORDER_INVALID_QUANTITY);
        this.quantity = newQuantity;
        this.totalPrice = this.unitPrice * newQuantity;
    }

    // ===== 환불 로직은 나중에 주석 처리 =====
    
    public void requestRefund() {
        if (this.refundStatus != RefundStatus.NONE) throw new BusinessException(ErrorCode.INVALID_REFUND_STATE);
        this.refundStatus = RefundStatus.REQUESTED;
    }
    */
}
