package ko.dh.goot.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListItem {

    private Long productId;
    private String productName;
    private Integer price;      // 정가, int의 기본값은 0이므로 INTEGER로
    private Integer salePrice;  // 할인가 (nullable)
    private String status;
    private LocalDateTime createdAt;
    private Integer totalStock;
    private String mainImageUrl;

    /** 실제 노출 가격 */
    public int getDisplayPrice() {
        return salePrice != null ? salePrice : price;
    }

    /** 할인 여부 */
    public boolean isOnSale() {
        return salePrice != null;
    }

    /** 할인율 (옵션) */
    public Integer getDiscountRate() {
        if (salePrice == null) return null;
        return (price - salePrice) * 100 / price;
    }
}
