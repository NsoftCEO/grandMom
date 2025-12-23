package ko.dh.goot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ko.dh.goot.dto.PortOnePaymentResponse;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.HashMap;

/**
 * PortOne API 호출을 담당하는 서비스입니다. (V2 API 규격 적용)
 * V2 공식 문서를 기반으로, API Secret을 'Authorization: PortOne <SECRET>' 형식으로 
 * 직접 사용하여 결제 정보를 조회합니다. (별도의 access-token 발급 단계 불필요)
 */
@Log4j2
@Service
public class PortoneApiService {

    // PortOne API Secret Key (PortOne 콘솔에서 발급받은 V2 API Secret)
    // 이 값이 PortOne 공식 문서의 'MY_API_SECRET'에 해당합니다.
    @Value("${portone.api-secret}")
    private String apiSecret;
    
    // PortOne API 기본 URL
    @Value("${portone.pay-detail-url}")
    private String payDetailURL;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PortoneApiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public PortOnePaymentResponse portonePaymentDetails(String paymentId) {

        String paymentUrl = payDetailURL + paymentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "PortOne " + apiSecret);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<PortOnePaymentResponse> response =
                restTemplate.exchange(
                    paymentUrl,
                    HttpMethod.GET,
                    entity,
                    PortOnePaymentResponse.class
                );

            PortOnePaymentResponse body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw new IllegalStateException("PortOne API 응답 실패");
            }

            /* ===== 1. 상태 검증 ===== */
            if (!"PAID".equals(body.getStatus())) {
                throw new IllegalStateException(
                    "결제 완료 상태 아님. status=" + body.getStatus()
                );
            }

            /* ===== 2. 금액 검증 ===== */
            PortOnePaymentResponse.Amount amount = body.getAmount();

            if (amount == null || amount.getTotal() == null || amount.getPaid() == null) {
                throw new IllegalStateException("amount 정보 누락");
            }

            if (!amount.getTotal().equals(amount.getPaid())) {
                throw new IllegalStateException(
                    "전액 결제 아님. total=" + amount.getTotal()
                        + ", paid=" + amount.getPaid()
                );
            }

            /* ===== 3. orderId 검증 ===== */
			/*
			 * if (body.getCustomData() == null || body.getCustomData().getOrderId() ==
			 * null) { throw new IllegalStateException("customData.orderId 누락"); }
			 */

            return body;

        } catch (Exception e) {
            log.error("🚨 PortOne 결제 조회 실패. paymentId={}", paymentId, e);
            throw new RuntimeException("PortOne 결제 조회 실패", e);
        }
    }


    /**
     * PortOne API를 통해 paymentId로 결제 상세 정보를 조회합니다. (V2 API 사용)
     * V2 인증 방식: Authorization: PortOne <API_SECRET>
     * @param paymentId 웹훅으로부터 수신한 PG사 결제 ID
     * @return PortOne API 응답에서 핵심 정보를 추출한 Map (merchantUid, totalAmount, status 등)
     
    @SuppressWarnings("unchecked")
    public Map<String, Object> portonePaymentDetails(String paymentId) {
    	
        String paymentUrl = payDetailURL + paymentId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "PortOne " + apiSecret); 
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                paymentUrl,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("포트원 API 응답 전체: {}", responseBody);
                
                System.out.println("1111");         
                Object amountObj = responseBody.get("amount");
                Long totalAmount = 0L;
                
                if (amountObj instanceof Map) {
                    // V2 방식: amount가 {total: 189000, ...} 형태의 Map인 경우
                    @SuppressWarnings("unchecked")
                    Map<String, Object> amountMap = (Map<String, Object>) amountObj;
                    Object totalVal = amountMap.get("total");
                    if (totalVal instanceof Number) {
                        totalAmount = ((Number) totalVal).longValue();
                    }
                } else if (amountObj instanceof Number) {
                    // V1 혹은 단순 숫자 방식 대응
                    totalAmount = ((Number) amountObj).longValue();
                }
                
                System.out.println("2222");
                String status = (String) responseBody.get("status");
                System.out.println("333");
                Object customDataObj = responseBody.get("customData");
                
                Long orderId = null;

                if (customDataObj != null) {
                    try {
                        Map<String, Object> customDataMap = null;
                        
                        if (customDataObj instanceof Map) {
                            customDataMap = (Map<String, Object>) customDataObj;
                        } else if (customDataObj instanceof String) {
                            // JSON 문자열인 경우 파싱 시도
                            String customDataStr = (String) customDataObj;
                            if (!customDataStr.isEmpty() && customDataStr.startsWith("{")) {
                                customDataMap = objectMapper.readValue(customDataStr, new TypeReference<Map<String, Object>>() {});
                            }
                        }

                        if (customDataMap != null) {
                            Object oId = customDataMap.get("orderId");
                            if (oId != null) {
                                // 문자열이든 숫자든 Long으로 변환
                            	orderId = Long.valueOf(oId.toString());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ customData 파싱 중 오류 발생: {}", e.getMessage());
                    }
                }
                
                if (orderId == null || totalAmount == null || status == null) {
                    throw new IllegalStateException("PortOne API 응답에서 필수 데이터 (orderId, amount, status)가 누락되었습니다.");
                }
                
                Map<String, Object> details = new HashMap<>();
                details.put("totalAmount", totalAmount);
                details.put("status", status); 
                details.put("orderId", orderId); 
                
                return details;

            } else {
                throw new RuntimeException("PortOne API 서버 통신 실패. 상태 코드: " + response.getStatusCodeValue());
            }

        } catch (Exception e) {
            System.err.println("🚨 PortOne API 결제 상세 조회 중 치명적인 오류 발생: " + e.getMessage());
            throw new RuntimeException("API 결제 상세 조회 실패: " + e.getMessage(), e);
        }
    }*/
}