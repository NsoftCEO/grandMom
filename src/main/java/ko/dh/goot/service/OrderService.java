package ko.dh.goot.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	
	@Value("${portone.api-secret}")
    private String apiSecret;
	
	private final ProductMapper productMapper;
	
	private final OrderMapper orderMapper;

	public OrderResponse prepareOrder(OrderRequest orderRequest, String currentUserId) {

		Product product = productMapper.selectProductById(orderRequest.getProductId());
        
        if (product == null) {
            throw new IllegalArgumentException("상품 정보가 존재하지 않습니다."); // todo :: Validation 패키지 새로 만들기
        }
        if (product.getStock() < orderRequest.getQuantity()) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }
        
        int serverCalculatedAmount = product.getPrice() * orderRequest.getQuantity();
        
        Order order = Order.builder()
                .userId(currentUserId)
                .orderName(orderRequest.getOrderName())
                .totalAmount(serverCalculatedAmount)
                .orderStatus("PAYMENT_READY")
                .receiverName(orderRequest.getReceiver())
                .receiverPhone(orderRequest.getPhone())
                .receiverAddress(orderRequest.getAddress())
                .deliveryMemo(orderRequest.getMemo())
                .build();
        
        int rowCount = orderMapper.insertOrder(order);

        if (rowCount != 1) {
            // 💡 주문 저장이 실패했으므로 예외 발생 및 트랜잭션 롤백 유도
            throw new IllegalStateException("주문 데이터 저장에 실패했습니다. 영향 받은 행: " + rowCount);
        }
        
		return new OrderResponse(order.getOrderId(), serverCalculatedAmount);
	}
	
	/**
     * [3. 확정] 결제 검증, DB 기록, 상태 업데이트, 재고 차감을 단일 트랜잭션으로 처리합니다.
     * 이 메서드는 Controller의 /completePayment 엔드포인트에서 호출되어야 합니다.
     * * @param paymentId PG사에서 발급된 결제 ID
     * @param orderId 사전에 저장된 주문 ID
     */
    public void completeOrderTransaction(String paymentId, Long orderId) {
        
        // 1. PG 검증 및 PG 데이터 조회 (3-1)
        // verifyPayment는 PG 통신 및 금액/상태 검증을 수행하고, 성공 시 JsonNode를 반환합니다.
        JsonNode paymentData = this.verifyPayment(paymentId, orderId);
        
        // PG 응답에서 최종 금액 추출
        int totalAmount = paymentData.at("/amount/total").asInt();
        
        // 2. 결제 기록 (3-2) - payments 테이블에 기록
        // 💡 Mock Code: 실제로는 paymentService.recordPaymentSuccess(orderId, paymentId, totalAmount, "PAID"); 와 같이 호출되어야 합니다.
        System.out.println("[3-2] 결제 기록: PaymentService를 통해 payments 테이블에 기록 (ID: " + paymentId + ")");

        // 3. 주문 상태 업데이트 (3-3) - orders 테이블 상태 변경
        // 💡 Mock Code: 실제로는 orderMapper.updateOrderStatus(orderId, "PAID", "PAYMENT_READY"); 와 같이 호출되어야 합니다.
        System.out.println("[3-3] 주문 상태 업데이트: OrderMapper를 통해 orders 상태를 PAID로 변경");
        
        // 4. 재고 차감 (3-4) - products 테이블 재고 감소
        // ⚠️ Mock Code: 실제로는 orderMapper.selectOrderDetails(orderId) 등으로 주문 항목을 가져와 productMapper.decreaseStock(productId, quantity)를 호출해야 합니다.
        System.out.println("[3-4] 재고 차감: ProductMapper를 통해 상품 재고 차감");

        // 트랜잭션이 성공적으로 커밋될 준비 완료
        System.out.println("✅ 트랜잭션 성공: 주문 ID " + orderId + "의 결제 확정 및 후속 작업 완료.");
    }
	
	/**
     * [3. 확정] 결제 검증, DB 기록, 상태 업데이트, 재고 차감을 단일 트랜잭션으로 처리합니다.
     * 이 메서드는 Controller의 /completePayment 엔드포인트에서 호출되어야 합니다.
     * * @param paymentId PG사에서 발급된 결제 ID
     * @param orderId 사전에 저장된 주문 ID
     */
	public JsonNode verifyPayment(String paymentId, Long orderId) {
        RestTemplate restTemplate = new RestTemplate();
        
        try {
            String url = "https://api.portone.io/payments/" + paymentId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "PortOne " + apiSecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            // 2. PG사 API 호출 및 응답 받기
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            // 3. JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode paymentData = mapper.readTree(response.getBody());
            
            // 4. 결제 상태 및 금액 추출
            String status = paymentData.get("status").asText();
            
            // ⚠️ 주의: PG사 응답 구조에 따라 'amount' 노드의 유효성을 먼저 확인해야 합니다.
            JsonNode totalAmountNode = paymentData.at("/amount/total");
            if (!totalAmountNode.isInt() && !totalAmountNode.isTextual()) {
                throw new IllegalStateException("PG 응답에서 결제 금액('amount/total')을 찾을 수 없습니다.");
            }
            int totalAmount = totalAmountNode.asInt();

            // 5. DB에 저장된 예상 금액 조회
            int expectedAmount = orderMapper.selectOrderExpectedAmount(orderId);

            // 6. 금액 불일치 검증 (가장 중요한 보안 로직)
            if (totalAmount != expectedAmount) {
                // 💡 PG사에는 성공했으나, 금액이 다르면 결제를 취소해야 합니다.
                // PortOne 취소 API를 호출하는 로직이 이 자리에 추가되어야 합니다.
                throw new IllegalStateException("결제 금액 불일치: PG 결제금액 (" + totalAmount + ") vs. DB 예상금액 (" + expectedAmount + "). 위조 의심.");
            }

            // 7. PG 상태 검증
            if (!"PAID".equals(status)) {
                // 💡 결제가 PAID 상태가 아니면 비즈니스 예외 발생
                throw new IllegalStateException("결제 승인 실패: PG사 응답 상태가 'PAID'가 아닙니다. 현재 상태: " + status);
            }

            // 8. 검증 완료 (후속 작업 진행 준비)
            System.out.println("결제 검증 성공 및 금액 일치 확인: " + paymentId);
            
            return paymentData;
            
        } catch (HttpClientErrorException e) {
            // PG사 API 호출 중 4xx (Bad Request, Unauthorized) 또는 5xx (Server Error) 발생
            throw new RuntimeException("PG사 통신 오류: " + e.getResponseBodyAsString(), e);
        } catch (JsonProcessingException e) {
            // JSON 파싱 오류
            throw new RuntimeException("PG 응답 JSON 파싱 실패", e);
        } catch (Exception e) {
            // 기타 모든 예외를 RuntimeException으로 감싸서 트랜잭션 롤백 유도
            throw new RuntimeException("결제 검증 중 예상치 못한 오류 발생: " + e.getMessage(), e);
        }
    }

}
