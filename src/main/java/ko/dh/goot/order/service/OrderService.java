package ko.dh.goot.order.service;

import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.order.dao.OrderItemMapper;
import ko.dh.goot.order.dao.OrderMapper;
import ko.dh.goot.order.dao.OrderRepository;
import ko.dh.goot.order.domain.Order;
import ko.dh.goot.order.domain.OrderItem;
import ko.dh.goot.order.domain.OrderStatus;
import ko.dh.goot.order.dto.OrderProductView;
import ko.dh.goot.order.dto.OrderRequest;
import ko.dh.goot.order.dto.OrderResponse;
import ko.dh.goot.order.dto.ProductOptionForOrder;
import ko.dh.goot.order.persistence.OrderRecord;
import ko.dh.goot.payment.dto.PaymentParamResponse;
import ko.dh.goot.product.dao.ProductOptionMapper;
import ko.dh.goot.product.dao.ProductOptionRepository;
import ko.dh.goot.product.domain.ProductOption;
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
	private final OrderRepository orderRepository ;
	private final ProductOptionMapper productOptionMapper;	
	private final ProductOptionRepository productOptionRepository;	
	
	
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

	    ProductOption option = productOptionRepository.findById(orderRequest.getOptionId())
	            .orElseThrow(() -> 
	                    new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

	    option.decreaseStock(orderRequest.getQuantity());

	    Order order = Order.create(
	            userId,
	            orderRequest.getOrderName(),
	            orderRequest.getReceiver(),
	            orderRequest.getPhone(),
	            orderRequest.getAddress(),
	            orderRequest.getMemo()
	    );

	    OrderItem item = OrderItem.create(
	    		option.getProduct().getProductId(),
	    		option.getOptionId(),
	    		option.getProduct().getProductName(),
	    		option.calculateOrderUnitPrice(),
	            orderRequest.getQuantity(),
	            option.getColor(),
	            option.getSize()
	    );

	    order.addItem(item);

	    orderRepository.save(order);

	    return new OrderResponse(order.getOrderId(), order.getTotalAmount());
	}


	/* ===============================
     * 결제 파라미터 생성
     * =============================== */
    public PaymentParamResponse createPaymentParams(Long orderId) {

    	OrderRecord order = orderMapper.selectOrder(orderId);

        if (order == null) {
        	throw new BusinessException(ErrorCode.ORDER_NOT_FOUND,"주문번호: " + orderId);
        }

        if (OrderStatus.PAYMENT_READY != order.orderStatus()) {
        	throw new BusinessException(ErrorCode.ORDER_INVALID_STATUS, "현재 주문상태: " + order.orderStatus());
        }

        return PaymentParamResponse.of(
                storeId,
                channelKey,
                order.orderName(),
                order.totalAmount(),
                orderId
        );
    }

	public int changeOrderStatus(Long orderId, String beforeStatus, String afterStatus) {
		return orderMapper.changeOrderStatus(orderId, beforeStatus, afterStatus);
	}

	


}
