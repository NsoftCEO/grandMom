package ko.dh.goot.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.dao.OrderItemMapper;
import ko.dh.goot.dao.OrderMapper;
import ko.dh.goot.dao.ProductMapper;
import ko.dh.goot.dto.Order;
import ko.dh.goot.dto.OrderItem;
import ko.dh.goot.dto.OrderRequest;
import ko.dh.goot.dto.OrderResponse;
import ko.dh.goot.dto.Product;
import ko.dh.goot.dto.ProductDetail;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
	
	@Value("${portone.store-id}")
    private String storeId;

    @Value("${portone.channel-key}")
    private String channelKey;
	
	private final ProductMapper productMapper;	
	private final OrderMapper orderMapper;
	private final OrderItemMapper orderItemMapper;
	
	private final ObjectMapper objectMapper;

	public OrderResponse prepareOrder(OrderRequest req, String userId) {

		ProductDetail product = productMapper.selectProductDetail(req.getProductId()); // 수정해야됨
        
        if (product == null) {
            throw new IllegalArgumentException("상품 정보가 존재하지 않습니다."); // todo :: Validation 패키지 새로 만들기
        }
       // if (product.getStock() < req.getQuantity()) {
       //    throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + product.getStock());
       // }
        
        int unitPrice = product.getPrice();
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
            throw new IllegalStateException("주문 저장 실패");
        }

        Long orderId = order.getOrderId();
		System.out.println("optionInfo::");
		System.out.println(req.getOptionInfo());
        String optionInfoJson = null;
        if (req.getOptionInfo() != null) {
            try {
                optionInfoJson = objectMapper.writeValueAsString(req.getOptionInfo());
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("옵션 정보 직렬화 실패", e);
            }
        }
        
        /* ===== 4. order_item 스냅샷 저장 (⭐ 핵심) ===== */
        OrderItem orderItem = OrderItem.builder()
            .orderId(orderId)
            .productId(product.getProductId())
            .productName(product.getProductName())
            .productPrice(unitPrice)
            .quantity(quantity)
            .totalPrice(serverCalculatedAmount)
            .optionInfo(optionInfoJson) // JSON 그대로 저장
            .refundStatus("NONE")
            .build();

        int orderIemInsertCount = orderItemMapper.insertOrderItem(orderItem);
        
        if (orderIemInsertCount != 1) {
            throw new IllegalStateException("order_item 저장 실패");
        }
        
		return new OrderResponse(order.getOrderId(), serverCalculatedAmount);
	}
	
	/* ===============================
     * 결제 파라미터 생성
     * =============================== */
    public Map<String, Object> createPaymentParams(Long orderId) {

        Order order = orderMapper.selectOrder(orderId);

        if (order == null) {
            throw new IllegalArgumentException("주문 없음");
        }

        if (!"PAYMENT_READY".equals(order.getOrderStatus())) {
            throw new IllegalStateException("이미 처리된 주문");
        }

        String orderIdStr = String.valueOf(orderId);
        
        return Map.of(
            "storeId", storeId,
            "channelKey", channelKey,
            "paymentId", "payment-" + java.util.UUID.randomUUID(),
            "orderName", order.getOrderName(),
            "totalAmount", order.getTotalAmount(),
            "currency", "KRW",
            "payMethod", "EASY_PAY",
            "isTestChannel", true,
            "customData", Map.of("orderId", orderIdStr)
        );
    }

	public int changeOrderStatus(Long orderId, String beforeStatus, String afterStatus) {
		return orderMapper.changeOrderStatus(orderId, beforeStatus, afterStatus);
	}


}
