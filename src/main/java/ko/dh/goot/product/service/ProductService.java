package ko.dh.goot.product.service;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.stereotype.Service;

import ko.dh.goot.product.dao.ProductMapper;
import ko.dh.goot.product.dto.ProductDetail;
import ko.dh.goot.product.dto.ProductList;
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
	        throw new NotFoundException("상품 없음");
	    }

	    product.setOptions(productMapper.selectProductOptions(productId));
	    product.setImages(productMapper.selectProductImages(productId));

	    return product;
	}

	
}
