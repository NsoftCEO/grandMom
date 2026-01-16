package ko.dh.goot.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private Long orderItemId;

    private Long orderId;
    private Long productId;
    private Long optionId;

    private String productName;
    private int unitPrice; 
    private int quantity;
    private int totalPrice;

    private String color;
    private String size;
    /**
     * JSON 문자열 그대로 저장
     * 예: {"color":"black","size":"L"}
     */
    private String optionInfo;

    /**
     * NONE, REQUESTED, PARTIAL, REFUNDED, FAILED
     */
    private String refundStatus;

}
