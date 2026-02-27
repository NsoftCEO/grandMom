package ko.dh.goot.order.persistence;

import java.time.LocalDateTime;
import ko.dh.goot.order.domain.OrderStatus;

public record OrderRecord(
        Long orderId,
        String userId,
        String orderName,
        long totalAmount,
        OrderStatus orderStatus,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        String deliveryMemo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}