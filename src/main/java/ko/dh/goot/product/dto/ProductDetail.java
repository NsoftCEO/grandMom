package ko.dh.goot.product.dto;

import java.time.LocalDateTime;
import java.util.List;

import ko.dh.goot.product.domain.ProductStatus;
import ko.dh.goot.product.persistence.ProductImageRecord;
import ko.dh.goot.product.persistence.ProductOptionRecord;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProductDetail {

    private Long productId;
    private String productName;
    private Integer price; // 정가
    private Integer salePrice; // 할인 적용가
    private String description;
    private ProductStatus productStatus;
    private LocalDateTime createdAt;
    
	// 카테고리 (1:1)
    private ProductCategory category;
    
    // 옵션 (1:N)
    private List<ProductOptionRecord> options;
    // 이미지 (1:N)
    private List<ProductImageRecord> images;

    public Integer getDisplayPrice() {
        return salePrice != null ? salePrice : price;
    }

    public boolean isOnSale() {
        return salePrice != null;
    }
}
