package ko.dh.goot.order.service;

import java.util.Map;

import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.order.dao.OrderItemMapper;
import ko.dh.goot.order.dao.OrderMapper;
import ko.dh.goot.order.domain.Order;
import ko.dh.goot.order.domain.OrderItem;
import ko.dh.goot.order.dto.OrderEntity;
import ko.dh.goot.order.dto.OrderItemEntity;
import ko.dh.goot.order.dto.OrderProduct;
import ko.dh.goot.order.dto.OrderProductView;
import ko.dh.goot.order.dto.OrderRequest;
import ko.dh.goot.order.dto.OrderResponse;
import ko.dh.goot.order.dto.ProductOptionForOrder;
import ko.dh.goot.payment.domain.RefundStatus;
import ko.dh.goot.product.dao.ProductMapper;
import ko.dh.goot.product.dao.ProductOptionMapper;
import ko.dh.goot.product.dto.ProductDetail;
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
	private final ObjectMapper objectMapper;

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
	    ProductOptionForOrder product = productOptionMapper.selectProductOptionDetail(orderRequest.getOptionId());

	    if (product == null) throw new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND);
	    if (product.getStockQuantity() < orderRequest.getQuantity())
	        throw new BusinessException(ErrorCode.OUT_OF_STOCK, "현재 재고: " + product.getStockQuantity());

	    int unitPrice = product.getUnitPrice();
	    int quantity = orderRequest.getQuantity();

	    // 도메인 객체 생성
	    OrderItem orderItem = OrderItem.create(
	            null,  // orderId는 DB 저장 후 세팅
	            product.getProductId(),
	            unitPrice,
	            quantity,
	            product.getProductName(),
	            product.getOptionId(),
	            product.getColor(),
	            product.getSize()
	    );

	    // Order 생성
	    Order order = Order.create(
	            userId,
	            orderRequest.getOrderName(),
	            orderRequest.getReceiver(),
	            orderRequest.getPhone(),
	            orderRequest.getAddress(),
	            orderRequest.getMemo()
	    );
	    order.addOrderItem(orderItem);

	    int orderInsertCount = orderMapper.insertOrder(OrderEntity.from(order));
	    if (orderInsertCount != 1) throw new BusinessException(ErrorCode.ORDER_CREATE_FAILED);

	    Long orderId = order.getOrderId();
	    orderItem = OrderItem.create(
	            orderId,
	            orderItem.getProductId(),
	            orderItem.getUnitPrice(),
	            orderItem.getQuantity(),
	            orderItem.getProductName(),
	            orderItem.getOptionId(),
	            orderItem.getColor(),
	            orderItem.getSize()
	    );

	    // 옵션 info JSON 처리
	    String optionInfoJson = null;
	    try {
	        if (orderRequest.getOptionInfo() != null) {
	            optionInfoJson = objectMapper.writeValueAsString(orderRequest.getOptionInfo());
	        }
	    } catch (Exception e) {
	        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "옵션 정보 직렬화 실패");
	    }

	    OrderItemEntity orderItemEntity = OrderItemEntity.from(orderItem, product, optionInfoJson);
	    int orderItemInsertCount = orderItemMapper.insertOrderItem(orderItemEntity);
	    if (orderItemInsertCount != 1) throw new BusinessException(ErrorCode.ORDER_CREATE_FAILED);

	    return new OrderResponse(orderId, order.getTotalAmount());
	}


	/* ===============================
     * 결제 파라미터 생성
     * =============================== */
    public Map<String, Object> createPaymentParams(Long orderId) {

        Order order = orderMapper.selectOrder(orderId);

        if (order == null) {
        	throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (!"PAYMENT_READY".equals(order.getOrderStatus())) {
        	throw new BusinessException(ErrorCode.ORDER_INVALID_STATUS);
        }

        return Map.of(
                "storeId", storeId,
                "channelKey", channelKey,
                "paymentId", "payment-" + java.util.UUID.randomUUID(),
                "orderName", order.getOrderName(),
                "totalAmount", order.getTotalAmount(),
                "currency", "KRW",
                "payMethod", "EASY_PAY",
                "isTestChannel", true,
                "customData", Map.of("orderId", orderId.toString())
        );
    }

	public int changeOrderStatus(Long orderId, String beforeStatus, String afterStatus) {
		return orderMapper.changeOrderStatus(orderId, beforeStatus, afterStatus);
	}

	


}
