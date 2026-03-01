package ko.dh.goot.product.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductOption {

    private Long optionId;
    private Long productId;
    private String color;
    private String size;
    private int stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
