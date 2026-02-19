package ko.dh.goot.order.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
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
}
