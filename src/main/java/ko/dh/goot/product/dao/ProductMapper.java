package ko.dh.goot.product.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.product.dto.ProductDetail;
import ko.dh.goot.product.dto.ProductImage;
import ko.dh.goot.product.dto.ProductListItem;
import ko.dh.goot.product.dto.ProductOption;

@Mapper
public interface ProductMapper {

	List<ProductListItem> selectProductList(Map<String, Object> param);
	ProductDetail selectProductDetail(long productId);
	List<ProductOption> selectProductOptions(long productId);
	List<ProductImage> selectProductImages(long productId);
	

}
