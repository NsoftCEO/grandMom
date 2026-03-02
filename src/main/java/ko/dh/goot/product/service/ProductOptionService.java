package ko.dh.goot.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.product.dao.ProductOptionMapper;
import ko.dh.goot.product.dao.ProductOptionRepository;
import ko.dh.goot.product.domain.ProductOption;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductOptionService {
	
	private final ProductOptionMapper productOptionMapper;
	
	public void decreaseStock(Long optionId, int orderQuantity) {

		if (orderQuantity <= 0) {
			throw new BusinessException(ErrorCode.ORDER_INVALID_QUANTITY, "주문 수량 :"+ orderQuantity);
	    }
		
        int updatedCount = productOptionMapper.decreaseStock(optionId, orderQuantity);

        // 👉 조건 불일치 = 재고 부족 또는 존재하지 않는 옵션
        if (updatedCount != 1) {
        	throw new BusinessException(ErrorCode.OUT_OF_STOCK, 
        			"optionId=" + optionId + ", quantity=" + orderQuantity);
        }
    }

	@Transactional
	public void increaseStock(Long optionId, int quantity) {

		if (quantity <= 0) {
			throw new BusinessException(ErrorCode.PRODUCT_STOCK_UPDATE_FAILED, "상품 수량 :"+ quantity);
	    }

	    int updatedCount = productOptionMapper.increaseStock(optionId, quantity);

	    if (updatedCount != 1) {
	        throw new BusinessException(ErrorCode.PRODUCT_STOCK_UPDATE_FAILED,
	        		"optionId=" + optionId + ", quantity=" + quantity);
	    }
	}
	
}
