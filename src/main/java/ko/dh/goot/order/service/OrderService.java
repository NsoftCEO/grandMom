package ko.dh.goot.order.service;

import java.util.Map;
import java.util.UUID;

import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.order.dao.OrderItemMapper;
import ko.dh.goot.order.dao.OrderMapper;
import ko.dh.goot.order.domain.Order;
import ko.dh.goot.order.domain.OrderItem;
import ko.dh.goot.order.domain.OrderStatus;
import ko.dh.goot.order.dto.OrderProductView;
import ko.dh.goot.order.dto.OrderRequest;
import ko.dh.goot.order.dto.OrderResponse;
import ko.dh.goot.order.dto.ProductOptionForOrder;
import ko.dh.goot.order.entity.OrderEntity;
import ko.dh.goot.order.entity.OrderItemEntity;
import ko.dh.goot.payment.dto.PaymentParamResponse;
import ko.dh.goot.product.dao.ProductOptionMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
	
	@Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.channel-key}")
    private String channelKey;
	
	private final OrderMapper orderMapper;
	private final OrderItemMapper orderItemMapper;
	private final ProductOptionMapper productOptionMapper;	
	public OrderProductView selectOrderProduct(Long optionId, int quantity) throws NotFoundException {

		OrderProductView orderProduct = orderMapper.selectOrderProduct(optionId);

        if (orderProduct == null) {
        	throw new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND);
        } 
        
        if (orderProduct.getStockQuantity() < quantity) {
        	throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }

        orderProduct.setQuantity(quantity);
        orderProduct.setTotalPrice(orderProduct.getDisplayPrice() * quantity);

        return orderProduct;
    }
	
	@Transactional
	public OrderResponse prepareOrder(OrderRequest orderRequest, String userId) {
		
		if (orderRequest.getQuantity() == null || orderRequest.getQuantity() <= 0) {
		    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "주문 수량: " + orderRequest.getQuantity());
		}
		
	    ProductOptionForOrder product = productOptionMapper.selectProductOptionDetail(orderRequest.getOptionId());

	    if (product == null) {
	    	throw new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND);
	    }
	    
	    if (product.getStockQuantity() < orderRequest.getQuantity())
	        throw new BusinessException(ErrorCode.OUT_OF_STOCK, "현재 재고: " + product.getStockQuantity());

	    Order order = Order.create(
	            userId,
	            orderRequest.getOrderName(),
	            orderRequest.getReceiver(),
	            orderRequest.getPhone(),
	            orderRequest.getAddress(),
	            orderRequest.getMemo()
	    );
	    
	    OrderItem item = OrderItem.create(
	            product.getProductId(),
	            product.getOptionId(),
	            product.getProductName(),
	            product.getUnitPrice(),
	            orderRequest.getQuantity(),
	            product.getColor(),
	            product.getSize()
	    );
	    
	    order.addItem(item);
	    
	    OrderEntity orderEntity = OrderEntity.builder()
	            .userId(order.getUserId())
	            .orderName(order.getOrderName())
	            .totalAmount(order.getTotalAmount())
	            .orderStatus(order.getOrderStatus())
	            .receiverName(order.getReceiverName())
	            .receiverPhone(order.getReceiverPhone())
	            .receiverAddress(order.getReceiverAddress())
	            .deliveryMemo(order.getDeliveryMemo())
	            .build();
	    
	    int insertOrder = orderMapper.insertOrder(orderEntity);
	    
	    if(insertOrder != 1 || orderEntity.getOrderId() == null) {
	    	throw new BusinessException(ErrorCode.ORDER_CREATE_FAILED);
	    }
	    
	    order.assignId(orderEntity.getOrderId());
	    
	    OrderItemEntity itemEntity = OrderItemEntity.builder()
	            .orderId(order.getOrderId())
	            .productId(item.getProductId())
	            .optionId(item.getOptionId())
	            .productName(item.getProductName())
	            .unitPrice(item.getUnitPrice())
	            .quantity(item.getQuantity())
	            .totalPrice(item.getTotalPrice())
	            .color(item.getColor())
	            .size(item.getSize())
	            .refundStatus(item.getRefundStatus().name())
	            .build();

	    int insertOrderItem = orderItemMapper.insertOrderItem(itemEntity);
	    
	    if(insertOrderItem != 1) {
	    	throw new BusinessException(ErrorCode.ORDER_ITEM_CREATE_FAILED);
	    }
	    
	    return new OrderResponse(order.getOrderId(), order.getTotalAmount());
	}


	/* ===============================
     * 결제 파라미터 생성
     * =============================== */
    public PaymentParamResponse createPaymentParams(Long orderId) {

        Order order = orderMapper.selectOrder(orderId);

        if (order == null) {
        	throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (OrderStatus.PAYMENT_READY != order.getOrderStatus()) {
        	throw new BusinessException(ErrorCode.ORDER_INVALID_STATUS);
        }

        return PaymentParamResponse.builder()
                .storeId(storeId)
                .channelKey(channelKey)
                .paymentId("payment-" + UUID.randomUUID())
                .orderName(order.getOrderName())
                .totalAmount(order.getTotalAmount())
                .currency("KRW")
                .payMethod("EASY_PAY")
                .isTestChannel(true)
                .customData(Map.of("orderId", orderId.toString()))
                .build();
    }

	public int changeOrderStatus(Long orderId, String beforeStatus, String afterStatus) {
		return orderMapper.changeOrderStatus(orderId, beforeStatus, afterStatus);
	}

	


}
