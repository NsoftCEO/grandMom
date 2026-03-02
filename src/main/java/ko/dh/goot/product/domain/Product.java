package ko.dh.goot.product.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    private String productName;
    private int price;
    private Integer salePrice;
    private String description;
    private Long categoryId;
    
    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProductOption> options = new ArrayList<>();

    private Product(String productName,
                    int price,
                    Integer salePrice,
                    String description,
                    Long categoryId) {

        if (price < 0) throw new IllegalArgumentException("price must be >= 0");

        this.productName = productName;
        this.price = price;
        this.salePrice = salePrice;
        this.description = description;
        this.categoryId = categoryId;
        this.productStatus = ProductStatus.ACTIVE;
    }

    public static Product create(String productName,
                                 int price,
                                 Integer salePrice,
                                 String description,
                                 Long categoryId) {
        return new Product(productName, price, salePrice, description, categoryId);
    }

    /* =========================
       연관관계 편의 메서드
       ========================= */

    public void addOption(ProductOption option) {
        if (option == null) throw new IllegalArgumentException("option is null");
        if (options.contains(option)) return;

        option.assignTo(this);
        options.add(option);
    }

    public void removeOption(ProductOption option) {
        if (options.remove(option)) {
            option.removeProduct();
        }
    }

    public boolean isOnSale() {
        return salePrice != null;
    }

    public int calculatePrice() {
        return isOnSale() ? salePrice : price;
    }
    
    /* =========================
       상태 변경 도메인 로직
       ========================= */

    public void hide() {
        this.productStatus = ProductStatus.HIDDEN;
    }

    public void stop() {
        this.productStatus = ProductStatus.STOP;
    }

    public void activate() {
        this.productStatus = ProductStatus.ACTIVE;
    }

}