package ko.dh.goot.order.entity;

import java.time.LocalDateTime;

import ko.dh.goot.order.domain.Order;
import ko.dh.goot.order.domain.OrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderEntity {

    private Long orderId;
    private String userId;
    private String orderName;

    private int totalAmount;
    private OrderStatus orderStatus;

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String deliveryMemo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private OrderEntity(
            Long orderId,
            String userId,
            String orderName,
            int totalAmount,
            OrderStatus orderStatus,
            String receiverName,
            String receiverPhone,
            String receiverAddress,
            String deliveryMemo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderName = orderName;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.deliveryMemo = deliveryMemo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrderEntity from(Order order) {
        return OrderEntity.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .orderName(order.getOrderName())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddress(order.getReceiverAddress())
                .deliveryMemo(order.getDeliveryMemo())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

