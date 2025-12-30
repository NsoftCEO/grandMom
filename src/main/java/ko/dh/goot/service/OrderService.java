package ko.dh.goot.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ko.dh.goot.dao.OrderMapper;
import ko.dh.goot.dao.ProductMapper;
import ko.dh.goot.dto.Order;
import ko.dh.goot.dto.OrderRequest;
import ko.dh.goot.dto.OrderResponse;
import ko.dh.goot.dto.Product;
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

	public OrderResponse prepareOrder(OrderRequest req, String userId) {

		Product product = productMapper.selectProductById(req.getProductId());
        
        if (product == null) {
            throw new IllegalArgumentException("상품 정보가 존재하지 않습니다."); // todo :: Validation 패키지 새로 만들기
        }
        if (product.getStock() < req.getQuantity()) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }
        
        int serverCalculatedAmount = product.getPrice() * req.getQuantity();
        
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
        
        int rowCount = orderMapper.insertOrder(order);

        // 여기서 order_item에 저장
        
        if (rowCount != 1) {
            // 💡 주문 저장이 실패했으므로 예외 발생 및 트랜잭션 롤백 유도
            throw new IllegalStateException("주문 데이터 저장에 실패했습니다. 영향 받은 행: " + rowCount);
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
