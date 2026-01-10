package ko.dh.goot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImage {

    private Long imageId;
    private String imageUrl;
    private String imageType;
    private Integer sortOrder;
}
