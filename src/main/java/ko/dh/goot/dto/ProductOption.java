package ko.dh.goot.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOption {

    private Long optionId;
    private Long productId;
    private String color; //(기본값: DEFAULT)
    private String size; //사이즈 (기본값: FREE)   
    private Integer additionalPrice; // 옵션 추가 금액 (없으면 0), 최종가격 = basePrice + additionalPrice로
    private Integer stockQuantity; //현재 재고 수량
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
