package ko.dh.goot.order.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ko.dh.goot.order.domain.OrderItem;
import ko.dh.goot.order.dto.OrderItemEntity;

@Mapper
public interface OrderItemMapper {
	OrderItemEntity selectOrderItemByOrderId(@Param("orderId") Long orderId);
	int insertOrderItem(OrderItemEntity orderItem);



}
