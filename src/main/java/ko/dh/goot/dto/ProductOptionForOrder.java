package ko.dh.goot.dto;

import lombok.Getter;

@Getter
public class ProductOptionForOrder{
	private Long optionId;
    private Long productId;
    private String productName;
    private int unitPrice;
    private Integer stockQuantity;
    private String color;
    private String size;
	
}
