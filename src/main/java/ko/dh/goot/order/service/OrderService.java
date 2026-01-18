package ko.dh.goot.order.service;

import java.util.Map;

import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.order.dao.OrderItemMapper;
import ko.dh.goot.order.dao.OrderMapper;
import ko.dh.goot.order.dto.Order;
import ko.dh.goot.order.dto.OrderItem;
import ko.dh.goot.order.dto.OrderProduct;
import ko.dh.goot.order.dto.OrderProductView;
import ko.dh.goot.order.dto.OrderRequest;
import ko.dh.goot.order.dto.OrderResponse;
import ko.dh.goot.order.dto.ProductOptionForOrder;
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
	
	public OrderResponse prepareOrder(OrderRequest req, String userId) {

		ProductOptionForOrder product = productOptionMapper.selectProductOptionDetail(req.getOptionId());
        
        if (product == null) {
        	throw new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND); // todo :: Validation 패키지 새로 만들기
        }
        
        if (product.getStockQuantity() < req.getQuantity()) {
        	throw new BusinessException(ErrorCode.OUT_OF_STOCK,
                    "현재 재고: " + product.getStockQuantity()
            );
        }
        
        int unitPrice = product.getUnitPrice();
        int quantity = req.getQuantity();
        int serverCalculatedAmount = unitPrice * quantity;
        
        
        Order order = Order.builder()
                .userId(userId)
                .orderName(req.getOrderName())
                .totalAmount(serverCalculatedAmount)
                .orderStatus("PAYMENT_READY")
                .receiverName(req.getReceiver())
                .receiverPhone(req.getPhone())
                .receiverAddress(req.getAddress())
                .deliveryMemo(req.getMemo())
                .build();
        
        int orderInsertCount = orderMapper.insertOrder(order);
        
        if (orderInsertCount != 1) {
        	throw new BusinessException(ErrorCode.ORDER_CREATE_FAILED);
        }
        
        Long orderId = order.getOrderId();

        String optionInfoJson = null;
        try {
            if (req.getOptionInfo() != null) {
                optionInfoJson = objectMapper.writeValueAsString(req.getOptionInfo());
            }
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "옵션 정보 직렬화 실패"
            );
        }

        OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(product.getProductId())
            .productName(product.getProductName())
            .optionId(product.getOptionId())
            .unitPrice(unitPrice)
            .quantity(quantity)
            .totalPrice(serverCalculatedAmount)
            .color(product.getColor())
            .size(product.getSize())
            .optionInfo(optionInfoJson) // JSON 그대로 저장
            .refundStatus("NONE")
            .build();

        int orderItemInsertCount = orderItemMapper.insertOrderItem(orderItem);
        
        if (orderItemInsertCount != 1) {
            throw new BusinessException(ErrorCode.ORDER_CREATE_FAILED);
        }
 
		return new OrderResponse(orderId, serverCalculatedAmount);
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
