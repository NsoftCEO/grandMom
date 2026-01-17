package ko.dh.goot.order.dao;

import org.apache.ibatis.annotations.Mapper;

import ko.dh.goot.order.dto.OrderItem;

@Mapper
public interface OrderItemMapper {

	int insertOrderItem(OrderItem orderItem);



}
