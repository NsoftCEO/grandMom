package ko.dh.goot.order.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ko.dh.goot.order.persistence.OrderItemRecord;

@Mapper
public interface OrderItemMapper {
	OrderItemRecord selectOrderItemByOrderId(@Param("orderId") Long orderId);
	int insertOrderItem(OrderItemRecord orderItem);



}
