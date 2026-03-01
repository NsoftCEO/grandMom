package ko.dh.goot.product.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListItem {

    private Long productId;
    private String productName;
    private int price;      // 정가
    private Integer salePrice;  // 할인가
    private String productStatus;
    private LocalDateTime createdAt;
    private int totalStock;
    private String mainImageUrl;

    public int getDisplayPrice() {
        return salePrice != null ? salePrice : price;
    }

    /** 할인 여부 */
    public boolean isOnSale() {
        return salePrice != null? true : false; // salePrice값이 있으면 isSale값 true, ProductListItem객체에 해당필드가 없어도 getter 기준으로 직렬화 됨 
    }

    /** 할인율 (옵션) */
    public Integer getDiscountRate() {
        if (salePrice == null) return null;
        return (price - salePrice) * 100 / price;
    }

}
