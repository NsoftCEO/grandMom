package ko.dh.goot.order.entity;

import java.time.LocalDateTime;

import ko.dh.goot.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}

