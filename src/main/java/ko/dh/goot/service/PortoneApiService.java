package ko.dh.goot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;

/**
 * PortOne API 호출을 담당하는 서비스입니다. (V2 API 규격 적용)
 * V2 공식 문서를 기반으로, API Secret을 'Authorization: PortOne <SECRET>' 형식으로 
 * 직접 사용하여 결제 정보를 조회합니다. (별도의 access-token 발급 단계 불필요)
 */
@Service
public class PortoneApiService {

    // PortOne API Secret Key (PortOne 콘솔에서 발급받은 V2 API Secret)
    // 이 값이 PortOne 공식 문서의 'MY_API_SECRET'에 해당합니다.
    @Value("${portone.api-secret}")
    private String apiSecret;
    
    // PortOne API 기본 URL
    @Value("${pay-detail-url}")
    private String payDetailURL;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PortoneApiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    

    /**
     * PortOne API를 통해 paymentId로 결제 상세 정보를 조회합니다. (V2 API 사용)
     * V2 인증 방식: Authorization: PortOne <API_SECRET>
     * @param paymentId 웹훅으로부터 수신한 PG사 결제 ID
     * @return PortOne API 응답에서 핵심 정보를 추출한 Map (merchantUid, totalAmount, status 등)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> portonePaymentDetails(String paymentId) {
        // 1. HTTP 헤더 설정 (Authorization Secret 직접 사용)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 🚨 V2 공식 문서에 따른 인증 방식 적용
        headers.set(HttpHeaders.AUTHORIZATION, "PortOne " + apiSecret); 
        
        System.out.println("🚨 [PortoneApiService] PortOne API (V2 Secret) 결제 상세 조회 시작: PaymentId=" + paymentId);

        // 2. HTTP 요청 엔티티 (GET 요청이므로 바디는 null)
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            // 3. PortOne 결제 상세 조회 API 호출 (V2 엔드포인트: https://api.portone.io/payments/{payment_id})
            String paymentUrl = payDetailURL + paymentId;
            
            // API 호출 및 응답 처리
            ResponseEntity<Map> response = restTemplate.exchange(
                paymentUrl,
                HttpMethod.GET,
                entity,
                Map.class
            );

            System.out.println("포트원 response::");
            System.out.println(response);
            
            // 4. 응답 검증 및 데이터 추출
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // 필수 데이터 추출: merchant_uid, amount, status
                String merchantUid = (String) responseBody.get("merchant_uid");
                Long amount = ((Number) responseBody.get("amount")).longValue(); 
                String status = (String) responseBody.get("status");

                if (merchantUid == null || amount == null || status == null) {
                    throw new IllegalStateException("PortOne API 응답에서 필수 데이터 (merchant_uid, amount, status)가 누락되었습니다.");
                }
                
                Map<String, Object> details = new HashMap<>();
                details.put("merchantUid", merchantUid);
                details.put("totalAmount", amount);
                details.put("status", status); 
                
                System.out.println("✅ [PortoneApiService] API 조회 성공. 주문 ID (" + merchantUid + ") 확보 및 금액 검증 준비 완료.");
                return details;

            } else {
                throw new RuntimeException("PortOne API 서버 통신 실패. 상태 코드: " + response.getStatusCodeValue());
            }

        } catch (Exception e) {
            System.err.println("🚨 PortOne API 결제 상세 조회 중 치명적인 오류 발생: " + e.getMessage());
            throw new RuntimeException("API 결제 상세 조회 실패: " + e.getMessage(), e);
        }
    }
}