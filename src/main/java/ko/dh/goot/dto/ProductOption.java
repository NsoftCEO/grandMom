package ko.dh.goot.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductOption {

    private Long optionId;
    private Long productId;
    private String color;
    private String size;
    private Integer stockQuantity;
}
