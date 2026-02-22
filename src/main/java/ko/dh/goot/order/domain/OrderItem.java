package ko.dh.goot.order.domain;

import jakarta.persistence.*;
import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.payment.domain.RefundStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long optionId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int totalPrice;

    private String color;
    private String size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus refundStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private OrderItem(Long productId,
                      Long optionId,
                      String productName,
                      int unitPrice,
                      int quantity,
                      String color,
                      String size) {

    	if (quantity <= 0) throw new BusinessException(ErrorCode.ORDER_INVALID_QUANTITY);
        if (unitPrice < 0) throw new BusinessException(ErrorCode.ORDER_INVALID_UNIT_PRICE);

        this.productId = productId;
        this.optionId = optionId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = unitPrice * quantity;
        this.color = color;
        this.size = size;
        this.refundStatus = RefundStatus.NONE;
        this.createdAt = LocalDateTime.now();
    }

    public static OrderItem create(Long productId,
                                   Long optionId,
                                   String productName,
                                   int unitPrice,
                                   int quantity,
                                   String color,
                                   String size) {

        return new OrderItem(productId,
                optionId,
                productName,
                unitPrice,
                quantity,
                color,
                size);
    }

    protected void setOrder(Order order) {
        this.order = order;
    }
}