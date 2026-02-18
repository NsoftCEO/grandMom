package ko.dh.goot.order.domain;

import java.util.ArrayList;
import java.util.List;

import ko.dh.goot.order.entity.OrderEntity;
import lombok.Getter;

@Getter
public class Order {

    private Long orderId;
    private String userId;
    private String orderName;
    private int totalAmount;
    private OrderStatus orderStatus;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String deliveryMemo;

    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(String userId, String orderName, String receiverName,
                  String receiverPhone, String receiverAddress, String deliveryMemo) {
        this.userId = userId;
        this.orderName = orderName;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.deliveryMemo = deliveryMemo;
        this.orderStatus = OrderStatus.PAYMENT_READY;
        this.totalAmount = 0;
    }

    public static Order create(String userId, String orderName, String receiverName,
                               String receiverPhone, String receiverAddress, String deliveryMemo) {
        return new Order(userId, orderName, receiverName, receiverPhone, receiverAddress, deliveryMemo);
    }

    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        recalculateTotalAmount();
    }

    private void recalculateTotalAmount() {
        totalAmount = orderItems.stream().mapToInt(OrderItem::getTotalPrice).sum();
    }

    public void completePayment() {
        if (orderStatus != OrderStatus.PAYMENT_READY) throw new IllegalStateException("결제 불가 상태");
        orderStatus = OrderStatus.PAID;
    }

    public void cancel() {
        if (orderStatus == OrderStatus.CANCELLED) throw new IllegalStateException("이미 취소됨");
        orderStatus = OrderStatus.CANCELLED;
    }

    public static Order from(OrderEntity entity) {
        Order order = new Order(
        		entity.getUserId(), entity.getOrderName(),
                entity.getReceiverName(), entity.getReceiverPhone(),
                entity.getReceiverAddress(), entity.getDeliveryMemo());
       
        order.orderId = entity.getOrderId();
        order.totalAmount = entity.getTotalAmount();
        order.orderStatus = entity.getOrderStatus();
        
        return order;
    }
}
