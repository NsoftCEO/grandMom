package ko.dh.goot.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OrderRequest {
	@NotNull
	private Long optionId;
	@NotNull
    private Long productId;
    @NotNull
    @Positive
    private Integer quantity;
    @NotBlank
    @Size(max = 100)
    private String receiver; 
    @NotBlank
    @Pattern(regexp = "^[0-9\\-]{10,13}$",
             message = "전화번호 형식이 올바르지 않습니다.")
    private String phone;
    @NotBlank
    @Size(max = 255)
    private String address;
    @Size(max = 255)
    private String memo;
    @NotBlank
    @Size(max = 255)
    private String orderName;  
    // totalAmount는 서버에서 재계산해야 안전하지만, 클라이언트가 보내주는 값도 받아서 참고할 수 있음
    @NotNull
    @Min(0)
    private Integer clientTotalAmount;
    
}