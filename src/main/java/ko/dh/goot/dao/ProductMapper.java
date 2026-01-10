package ko.dh.goot.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.dto.Product;
import ko.dh.goot.dto.ProductListItem;

@Mapper
public interface ProductMapper {

	List<ProductListItem> selectProductList(Map<String, Object> param);
	Product selectProductById(long productId);

}
