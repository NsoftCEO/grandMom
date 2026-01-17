package ko.dh.goot.product.service;

import org.springframework.stereotype.Service;

import ko.dh.goot.product.dao.ProductOptionMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductOptionService {
	
	private final ProductOptionMapper productOptionMapper;
	
	public void decreaseStock(Long optionId, int orderQuantity) {

        int updatedCount = productOptionMapper.decreaseStock(optionId, orderQuantity);

        // 👉 조건 불일치 = 재고 부족 또는 존재하지 않는 옵션
        if (updatedCount != 1) {
            throw new IllegalStateException(
                "재고 부족 또는 옵션 없음. optionId=" + optionId
                    + ", quantity=" + orderQuantity
            );
        }
    }
	
}
