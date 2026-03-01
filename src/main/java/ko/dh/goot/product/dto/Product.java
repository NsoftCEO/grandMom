package ko.dh.goot.product.dto;

import java.time.LocalDateTime;
import java.util.List;

import ko.dh.goot.product.persistence.ProductImageRecord;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product {
    private Long productId;
	private String productName;
    private Integer price;
    private String category;
    private Integer stock;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String mainImage;
    private List<ProductImageRecord> images;


}