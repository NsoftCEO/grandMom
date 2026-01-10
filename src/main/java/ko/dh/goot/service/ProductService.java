package ko.dh.goot.service;

import java.util.List;
import java.util.Map;

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

	public ProductDetail selectProductDetail(int productId) {

		return productMapper.selectProductDetail(productId);
	}

	
}
