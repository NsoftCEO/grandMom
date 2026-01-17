package ko.dh.goot.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderProductView{

	private Long optionId;
    private Long productId;
    private String productName;
    private int displayPrice;
    private Integer stockQuantity;
    private String color;
    private String size;   
    private String thumbnailUrl;
    private int quantity;
    private int totalPrice;
}

