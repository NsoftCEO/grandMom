package ko.dh.goot.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class ProductDetail {

    private Long productId;
    private String productName;
    private Integer price; // 정가
    private Integer salePrice; // 할인 적용가
    private String description;
    private String status;
    private LocalDateTime createdAt;
    
	// 카테고리 (1:1)
    private ProductCategory category;
    
    // 옵션 (1:N)
    private List<ProductOption> options;
    // 이미지 (1:N)
    private List<ProductImage> images;

    public Integer getDisplayPrice() {
        return salePrice != null ? salePrice : price;
    }

    public boolean isOnSale() {
        return salePrice != null;
    }
}
