package ko.dh.goot.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private long expectedAmount; // 서버가 최종 확정한 금액

}