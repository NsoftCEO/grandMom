package ko.dh.goot.order.domain;

import jakarta.persistence.*;
import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
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
        if (item == null) throw new IllegalArgumentException("OrderItem이 없습니다.");
        if (orderItems.contains(item)) return;
        
        item.setOrder(this);
        orderItems.add(item);
        recalculateTotal();
    }
    
    public void removeItem(OrderItem item) {
        if (orderItems.remove(item)) {
            item.setOrder(null);
            recalculateTotal();
        }
    }

    private void recalculateTotal() {
        this.totalAmount = orderItems.stream()
            .mapToInt(OrderItem::getTotalPrice)
            .sum();
    }

    public void completePayment(int paidAmount) {
        // 1. 금액 검증 책임 수행
        if (this.totalAmount != paidAmount) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "주문금액=" + this.totalAmount + ", 결제금액=" + paidAmount);
        }
        
        // 2. 상태 검증
        if (this.orderStatus != OrderStatus.PAYMENT_READY) {
            throw new IllegalStateException("결제 불가 상태");
        }
        
        // 3. 상태 변경
        this.orderStatus = OrderStatus.PAID;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
    	if (this.orderStatus != OrderStatus.PAYMENT_READY) {
    	    throw new IllegalStateException("취소 불가 상태" + this.orderStatus);
    	}
        this.orderStatus = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /* =========================
    상태 쿼리 / 전이 헬퍼
    ========================= */

	 public boolean isPaid() {
	     return this.orderStatus == OrderStatus.PAID;
	 }
	
	 public boolean isCancelled() {
	     return this.orderStatus == OrderStatus.CANCELLED;
	 }
	 
	 public boolean isPaymentFailed() {
	        return this.orderStatus == OrderStatus.PAYMENT_FAILED;
	 }
	 
	 public void markPaymentFailed() {
        if (this.orderStatus == OrderStatus.PAID || this.orderStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 완료/취소된 주문입니다: " + this.orderStatus);
        }
        this.orderStatus = OrderStatus.PAYMENT_FAILED;
        this.updatedAt = LocalDateTime.now();
    }
	 
	 
	 
}