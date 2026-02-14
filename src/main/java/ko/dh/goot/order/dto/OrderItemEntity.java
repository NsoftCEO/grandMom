package ko.dh.goot.order.dto;

import java.time.LocalDateTime;

import ko.dh.goot.order.domain.OrderItem;
import ko.dh.goot.payment.domain.RefundStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderItemEntity {

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
    private String optionInfo;
    private RefundStatus refundStatus;
    private LocalDateTime createdAt;

    @Builder
    private OrderItemEntity(
            Long orderItemId,
            Long orderId,
            Long productId,
            Long optionId,
            String productName,
            int unitPrice,
            int quantity,
            int totalPrice,
            String color,
            String size,
            String optionInfo,
            RefundStatus refundStatus,
            LocalDateTime createdAt
    ) {
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.productId = productId;
        this.optionId = optionId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.color = color;
        this.size = size;
        this.optionInfo = optionInfo;
        this.refundStatus = refundStatus;
        this.createdAt = createdAt;
    }

    public static OrderItemEntity from(OrderItem orderItem, ProductOptionForOrder product, String optionInfoJson) {
        return OrderItemEntity.builder()
                .orderId(orderItem.getOrderId())
                .productId(orderItem.getProductId())
                .optionId(product.getOptionId())
                .productName(orderItem.getProductName())
                .unitPrice(orderItem.getUnitPrice())
                .quantity(orderItem.getQuantity())
                .totalPrice(orderItem.getTotalPrice())
                .color(orderItem.getColor())
                .size(orderItem.getSize())
                .optionInfo(optionInfoJson)
                .refundStatus(orderItem.getRefundStatus())
                .createdAt(orderItem.getCreatedAt())
                .build();
    }
}
