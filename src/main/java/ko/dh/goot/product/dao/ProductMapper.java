package ko.dh.goot.product.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.product.dto.ProductDetail;
import ko.dh.goot.product.dto.ProductList;
import ko.dh.goot.product.persistence.ProductImageRecord;
import ko.dh.goot.product.persistence.ProductOptionRecord;

@Mapper
public interface ProductMapper {

	List<ProductList> selectProductList(Map<String, Object> param);
	ProductDetail selectProductDetail(long productId);
	List<ProductOptionRecord> selectProductOptions(long productId);
	List<ProductImageRecord> selectProductImages(long productId);
	

}
