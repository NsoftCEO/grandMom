package ko.dh.goot.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;
    private String userId;
    private String orderName;
    private int totalAmount;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String deliveryMemo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> orderItems = new ArrayList<>();

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
        this.updatedAt = this.createdAt;
    }

    public static Order create(String userId,
                               String orderName,
                               String receiverName,
                               String receiverPhone,
                               String receiverAddress,
                               String deliveryMemo) {

        return new Order(userId,
                orderName,
                receiverName,
                receiverPhone,
                receiverAddress,
                deliveryMemo);
    }

    public void addItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
        this.totalAmount += item.getTotalPrice();
    }

    public void completePayment() {
        if (this.orderStatus != OrderStatus.PAYMENT_READY) {
            throw new IllegalStateException("결제 불가 상태");
        }
        this.orderStatus = OrderStatus.PAID;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.orderStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소됨");
        }
        this.orderStatus = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
}