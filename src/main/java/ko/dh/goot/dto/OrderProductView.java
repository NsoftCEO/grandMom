package ko.dh.goot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderProductView extends ProductOptionBase {

    
    private int displayPrice;
    private String thumbnailUrl;
    private int quantity;
    private int totalPrice;
}

