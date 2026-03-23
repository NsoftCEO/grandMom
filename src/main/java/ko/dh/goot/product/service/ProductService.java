package ko.dh.goot.product.service;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.stereotype.Service;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.product.dao.ProductMapper;
import ko.dh.goot.product.dto.ProductDetail;
import ko.dh.goot.product.dto.ProductList;
import ko.dh.goot.product.persistence.ProductOptionRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProductService {
	
	private final ProductMapper productMapper;

	public List<ProductList> selectProductList(Map<String, Object> param) {

		return productMapper.selectProductList(param);
		
	}

	public ProductDetail selectProductDetail(long productId) throws NotFoundException {

		ProductDetail product = productMapper.selectProductDetail(productId);
	    if (product == null) {
	    	throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,"상품번호: " + productId);
	    }

	    List<ProductOptionRecord> options = productMapper.selectProductOptions(productId);
	    if (options == null || options.isEmpty()) {
	        throw new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND, 
	            "옵션이 존재하지 않는 상품입니다. productId=" + productId);
	    }

	    product.setOptions(options);
	    product.setImages(productMapper.selectProductImages(productId));

	    return product;
	}

	
}
