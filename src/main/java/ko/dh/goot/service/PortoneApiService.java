package ko.dh.goot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ko.dh.goot.dto.PortOnePaymentResponse;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.time.OffsetDateTime;
import java.util.HashMap;

/**
 * PortOne API 호출을 담당하는 서비스입니다. (V2 API 규격 적용)
 * V2 공식 문서를 기반으로, API Secret을 'Authorization: PortOne <SECRET>' 형식으로 
 * 직접 사용하여 결제 정보를 조회합니다. (별도의 access-token 발급 단계 불필요)
 */
@Log4j2
@Service
public class PortoneApiService {

    @Value("${portone.api-secret}")
    private String apiSecret;
    
    // PortOne API 기본 URL
    @Value("${portone.pay-detail-url}")
    private String payDetailURL;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PortoneApiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = JsonMapper.builder()
        	    .addModule(new JavaTimeModule())
        	    .build();
    }
    
    public PortOnePaymentResponse portonePaymentDetails(String paymentId) {

        String paymentUrl = payDetailURL + paymentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "PortOne " + apiSecret);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
        	ResponseEntity<String> rawResponse =
        		    restTemplate.exchange(paymentUrl, HttpMethod.GET, entity, String.class);

    		if (!rawResponse.getStatusCode().is2xxSuccessful()) {
    		    log.error("PortOne API 실패. status={}, body={}", rawResponse.getStatusCode(), rawResponse.getBody());
    		    throw new IllegalStateException("PortOne API HTTP 실패");
    		}
    		
    		System.out.println("rawResponse:");
    		System.out.println(rawResponse);

    		String rawBody = rawResponse.getBody();
    		if (rawBody == null || rawBody.isBlank()) {
    		    throw new IllegalStateException("PortOne API 응답 body 없음");
    		}

    		System.out.println("dto맵핑::::::::::::");
    		/* ===== 1. 기본 DTO 매핑 ===== */
    		PortOnePaymentResponse body =
    		        objectMapper.readValue(rawBody, PortOnePaymentResponse.class);
    		
    		
    		System.out.println("body:::::::::::::");
    		System.out.println(body);
    		
    		
    		/* ===== 2. JsonNode로 추가 필드 추출 ===== */
    		JsonNode root = objectMapper.readTree(rawBody);

    		/*
    		//method.provider
    		JsonNode providerNode = root.path("method").path("provider");
    		if (!providerNode.isMissingNode()) {
    		    body.applyProvider(providerNode.asText());
    		}

    		//paidAt
    		JsonNode paidAtNode = root.path("paidAt");
    		if (!paidAtNode.isMissingNode()) {
    		    body.applyPaidAt(
    		        OffsetDateTime.parse(paidAtNode.asText()).toLocalDateTime()
    		    );
    		}
            System.out.println("사용한 paymentId:");
            System.out.println(paymentId);
            System.out.println("body::");
            System.out.println(body);
            System.out.println(" Node::");
            System.out.println(providerNode.asText());
            System.out.println(paidAtNode.asText());
            
      
            if (!paymentId.equals(body.getId())) {
                throw new IllegalStateException(
                    "결제 ID 불일치. request paymentId=" + paymentId
                    + ", 포트원 response=" + body.getId()
                );
            }
            
            // ===== 1. 상태 검증 =====
            if (!"PAID".equals(body.getStatus())) {
                throw new IllegalStateException(
                    "결제 완료 상태 아님. status=" + body.getStatus()
                );
            }

            // ===== 2. 금액 검증 =====
            PortOnePaymentResponse.Amount amount = body.getAmount();

            if (amount == null || amount.getTotal() == null || amount.getPaid() == null) {
                throw new IllegalStateException("amount 정보 누락");
            }

            if (!amount.getTotal().equals(amount.getPaid())) {
                throw new IllegalStateException(
                    "전액 결제 아님. total=" + amount.getTotal()
                        + ", paid=" + amount.getPaid()
                );
            }*/

            // ===== 3. orderId 검증 =====		
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