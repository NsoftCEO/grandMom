package ko.dh.goot.order.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderEntity {

    private Long orderId;
    private String userId;
    private String orderName;
    private int totalAmount;
    private String orderStatus;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String deliveryMemo;
}

