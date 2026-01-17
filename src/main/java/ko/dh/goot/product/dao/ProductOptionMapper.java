package ko.dh.goot.product.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ko.dh.goot.order.dto.ProductOptionForOrder;

@Mapper
public interface ProductOptionMapper {

    int decreaseStock(@Param("optionId") Long optionId,
                      @Param("quantity") int quantity);

	ProductOptionForOrder selectProductOptionDetail(Long optionId);
}

