package ko.dh.goot.order.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ko.dh.goot.order.dto.OrderItem;

@Mapper
public interface OrderItemMapper {
	OrderItem selectOrderItemByOrderId(@Param("orderId") Long orderId);
	int insertOrderItem(OrderItem orderItem);



}
