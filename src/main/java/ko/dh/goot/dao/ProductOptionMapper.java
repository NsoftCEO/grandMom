package ko.dh.goot.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductOptionMapper {

    int decreaseStock(@Param("optionId") Long optionId,
                      @Param("quantity") int quantity);
}

