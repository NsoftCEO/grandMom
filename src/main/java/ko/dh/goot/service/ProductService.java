package ko.dh.goot.service;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.stereotype.Service;

import ko.dh.goot.dao.ProductMapper;
import ko.dh.goot.dto.Product;
import ko.dh.goot.dto.ProductDetail;
import ko.dh.goot.dto.ProductListItem;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
	
	private final ProductMapper productMapper;

	public List<ProductListItem> selectProductList(Map<String, Object> param) {

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
