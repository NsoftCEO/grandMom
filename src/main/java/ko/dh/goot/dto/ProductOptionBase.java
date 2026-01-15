package ko.dh.goot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductOptionBase {

    private Long optionId;
    private Long productId;
    private String productName;
    private Integer stockQuantity;
    private String color;
    private String size;
}

