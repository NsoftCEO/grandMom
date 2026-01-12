package ko.dh.goot.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.dto.ProductDetail;
import ko.dh.goot.dto.ProductImage;
import ko.dh.goot.dto.ProductListItem;
import ko.dh.goot.dto.ProductOption;

@Mapper
public interface ProductMapper {

	List<ProductListItem> selectProductList(Map<String, Object> param);
	ProductDetail selectProductDetail(long productId);
	List<ProductOption> selectProductOptions(long productId);
	List<ProductImage> selectProductImages(long productId);
	

}
