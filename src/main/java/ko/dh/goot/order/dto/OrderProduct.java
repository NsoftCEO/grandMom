package ko.dh.goot.order.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrderProduct {

    private Long optionId;
    private Long productId;
    private String productName;    
    private int displayPrice;  
    private String color;
    private String size; 
    private int stockQuantity;
    private String thumbnailUrl;
    
    private int quantity;   
    private int totalPrice;

}
