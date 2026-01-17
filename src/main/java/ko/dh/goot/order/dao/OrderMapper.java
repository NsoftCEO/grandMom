package ko.dh.goot.order.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ko.dh.goot.order.dto.Order;
import ko.dh.goot.order.dto.OrderProductView;

@Mapper
public interface OrderMapper {

	Order selectOrder(Long orderId);
	int insertOrder(Order order);
	int selectOrderExpectedAmount(Long orderId);
	int changeOrderStatus(
	        @Param("orderId") Long orderId,
	        @Param("beforeStatus") String beforeStatus,
	        @Param("afterStatus") String afterStatus
	    );
	OrderProductView selectOrderProduct(Long optionId);
	


}
