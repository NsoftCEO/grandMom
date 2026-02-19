package ko.dh.goot.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OrderRequest {
	private Long optionId;
    private Long productId;
    private Integer quantity;
    private String receiver; 
    private String phone;
    private String address;
    private String memo;
    private String orderName;  
    // totalAmount는 서버에서 재계산해야 안전하지만, 클라이언트가 보내주는 값도 받아서 참고할 수 있음
    private Integer clientTotalAmount;
    
}