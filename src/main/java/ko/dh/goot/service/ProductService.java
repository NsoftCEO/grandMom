package ko.dh.goot.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ko.dh.goot.dao.ProductMapper;
import ko.dh.goot.dto.Product;
import ko.dh.goot.dto.ProductListItem;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
	
	private final ProductMapper productMapper;

	public List<ProductListItem> selectProductList(Map<String, Object> param) {

		return productMapper.selectProductList(param);
	}

	public Product selectProductById(int productId) {
		Product product = productMapper.selectProductById(productId);
		
		if (product != null && !product.getImages().isEmpty() && product.getImages().get(0).getImagePath() != null) {
            // 정렬되어 있으므로 첫 번째 이미지를 메인 이미지로 사용
			System.out.println("메인이미지 등록");
            product.setMainImage(product.getImages().get(0).getImagePath());
        }
		return product;
	}

}
