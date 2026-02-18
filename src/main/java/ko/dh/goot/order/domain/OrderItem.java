package ko.dh.goot.order.domain;

import java.time.LocalDateTime;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.order.dto.ProductOptionForOrder;
import ko.dh.goot.order.entity.OrderItemEntity;
import ko.dh.goot.payment.domain.RefundStatus;
import lombok.Getter;

@Getter
public class OrderItem {

    private Long orderItemId;
    private Long orderId;
    private Long productId;
    private Long optionId;

    private String productName;
    private int unitPrice;
    private int quantity;
    private int totalPrice;

    private String color;
    private String size;
    private RefundStatus refundStatus;
    private LocalDateTime createdAt;

    private OrderItem(
            Long orderId,
            Long productId,
            int unitPrice,
            int quantity,
            String productName,
            Long optionId,
            String color,
            String size
    ) {
        if (quantity <= 0) throw new BusinessException(ErrorCode.ORDER_INVALID_QUANTITY);
        if (unitPrice < 0) throw new BusinessException(ErrorCode.ORDER_INVALID_UNIT_PRICE);

        this.orderId = orderId;
        this.productId = productId;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = unitPrice * quantity;

        this.productName = productName;
        this.optionId = optionId;
        this.color = color;
        this.size = size;

        this.refundStatus = RefundStatus.NONE;
        this.createdAt = LocalDateTime.now();
    }

    public static OrderItem create(
            Long orderId,
            Long productId,
            int unitPrice,
            int quantity,
            String productName,
            Long optionId,
            String color,
            String size
    ) {
        return new OrderItem(orderId, productId, unitPrice, quantity, productName, optionId, color, size);
    }

    public static OrderItem from(OrderItemEntity entity) {
        OrderItem item = new OrderItem(
                entity.getOrderId(),
                entity.getProductId(),
                entity.getUnitPrice(),
                entity.getQuantity(),
                entity.getProductName(),
                entity.getOptionId(),
                entity.getColor(),
                entity.getSize()
        );
        item.orderItemId = entity.getOrderItemId();
        item.refundStatus = entity.getRefundStatus();
        item.createdAt = entity.getCreatedAt();
        return item;
    }

    public void changeQuantity(int newQuantity) {
        if (newQuantity <= 0) throw new BusinessException(ErrorCode.ORDER_INVALID_QUANTITY);
        this.quantity = newQuantity;
        this.totalPrice = this.unitPrice * newQuantity;
    }

    // ===== 환불 로직은 나중에 주석 처리 =====
    /*
    public void requestRefund() {
        if (this.refundStatus != RefundStatus.NONE) throw new BusinessException(ErrorCode.INVALID_REFUND_STATE);
        this.refundStatus = RefundStatus.REQUESTED;
    }
    */
}
