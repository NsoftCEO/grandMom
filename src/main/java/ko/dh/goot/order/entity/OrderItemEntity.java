package ko.dh.goot.order.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private String refundStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
