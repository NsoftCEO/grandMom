package ko.dh.goot.product.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    private int stockQuantity;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ProductOption(String color,
                          String size,
                          int stockQuantity) {

        if (stockQuantity < 0) throw new IllegalArgumentException("stockQuantity must be >= 0");

        this.color = color;
        this.size = size;
        this.stockQuantity = stockQuantity;
    }

    public static ProductOption create(String color,
                                       String size,
                                       int stockQuantity) {
        return new ProductOption(color, size, stockQuantity);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    protected void assignTo(Product product) {
        this.product = product;
    }

    protected void removeProduct() {
        this.product = null;
    }
    
    public int calculateOrderUnitPrice() {
        return this.product.calculatePrice();
    }
    
    public void decreaseStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        if (this.stockQuantity < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        this.stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        this.stockQuantity += quantity;
    }
}