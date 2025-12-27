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

            System.out.println("사용한 paymentId:");
            System.out.println(paymentId);
            System.out.println("포트원 respose::");
            System.out.println(response);
            System.out.println("body::");
            System.out.println(response.getBody());
            
            PortOnePaymentResponse body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw new IllegalStateException("PortOne API 응답 실패");
            }

            if (!paymentId.equals(body.getId())) {
                throw new IllegalStateException(
                    "결제 ID 불일치. request paymentId=" + paymentId
                    + ", 포트원 response=" + body.getId()
                );
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
			Long extractOrderId = extractOrderId(body.getCustomData());
			body.applyOrderId(extractOrderId);
			 
            return body;

        } catch (Exception e) {
            log.error("🚨 PortOne 결제 조회 실패. paymentId={}", paymentId, e);
            throw new RuntimeException("PortOne 결제 조회 실패", e);
        }
    }
    
    // 나중에 유틸클래스 만들어서 옮길수도있음
    private Long extractOrderId(String customData) {

	    if (customData == null || customData.isBlank()) {
	    	throw new IllegalStateException("extractOrderId중 customData 없습니다.");
	    }

	    try {
	        PortOnePaymentResponse.CustomData data =
	            objectMapper.readValue(
	                customData,
	                PortOnePaymentResponse.CustomData.class
	            );
	        
	        if (data.getOrderId() == null) {
	            throw new IllegalStateException("customData.orderId 누락");
	        }
	        
	        return data.getOrderId();

	    } catch (Exception e) {
	        throw new IllegalStateException(
	            "customData 파싱 실패: " + customData, e
	        );
	    }
	}


}