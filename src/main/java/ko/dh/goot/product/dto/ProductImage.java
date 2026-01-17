package ko.dh.goot.product.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImage {

    private Long imageId;
    private Long productId;
    private String fileName;
    private String imageType;
    private Integer sortOrder;
}
