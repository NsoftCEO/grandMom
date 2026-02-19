package ko.dh.goot.order.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class Order {

    private Long orderId;

    private final String userId;
    private final String orderName;
    private final String receiverName;
    private final String receiverPhone;
    private final String receiverAddress;
    private final String deliveryMemo;

    private int totalAmount;
    private OrderStatus orderStatus;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private final List<OrderItem> orderItems = new ArrayList<>();

    private Order(String userId,
                  String orderName,
                  String receiverName,
                  String receiverPhone,
                  String receiverAddress,
                  String deliveryMemo) {

        this.userId = userId;
        this.orderName = orderName;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.deliveryMemo = deliveryMemo;

        this.totalAmount = 0;
        this.orderStatus = OrderStatus.PAYMENT_READY;       
        this.createdAt = LocalDateTime.now();
    }

    public static Order create(String userId,
                               String orderName,
                               String receiverName,
                               String receiverPhone,
                               String receiverAddress,
                               String deliveryMemo) {

        return new Order(userId, orderName,
                receiverName, receiverPhone,
                receiverAddress, deliveryMemo);
    }

    public void assignId(Long orderId) {
        this.orderId = orderId;
    }

    public void addItem(OrderItem item) {
        this.orderItems.add(item);
        this.totalAmount += item.getTotalPrice();
    }

    /*
    public void completePayment() {
        if (orderStatus != OrderStatus.PAYMENT_READY) {
            throw new IllegalStateException("결제 불가 상태");
        }
        this.orderStatus = OrderStatus.PAID;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (orderStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소됨");
        }
        this.orderStatus = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }*/

}
