package ko.dh.goot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderProduct {

    private Long optionId;
    private Long productId;

    private String productName;
    private String color;
    private String size;

    private String thumbnailUrl;

    private int price;
    private int quantity;
    private int stockQuantity;

    private int totalPrice;

    public void calculateTotalPrice() {
        this.totalPrice = price * quantity;
    }
}
