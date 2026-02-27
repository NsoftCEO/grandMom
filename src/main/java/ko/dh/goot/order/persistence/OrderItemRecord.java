package ko.dh.goot.order.persistence;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record OrderItemRecord(
        Long orderItemId,
        Long orderId,
        Long productId,
        Long optionId,
        String productName,
        long unitPrice,
        int quantity,
        long totalPrice,
        String color,
        String size,
        String refundStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}