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
    
    /** 카테고리 */
    //private Long categoryId;
    //private String categoryName;
    private ProductCategory category;
    
    /** 옵션 / 이미지 */
    private List<ProductOption> options;
    private List<ProductImage> images;

    public Integer getDisplayPrice() {
        return salePrice != null ? salePrice : price;
    }

    public boolean isOnSale() {
        return salePrice != null;
    }
}
