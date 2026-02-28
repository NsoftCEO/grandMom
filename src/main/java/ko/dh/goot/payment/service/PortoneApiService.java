package ko.dh.goot.payment.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.payment.dto.PortOnePaymentResponse;
import lombok.extern.log4j.Log4j2;

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

    public PortoneApiService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(10));

        this.restTemplate = restTemplateBuilder
                .requestFactorySettings(settings)
                .build();
        this.objectMapper = objectMapper;
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
                log.error("🚨 PortOne API HTTP 실패. status={}, body={}", rawResponse.getStatusCode(), rawResponse.getBody());
                throw new BusinessException(ErrorCode.PG_API_FAILED, "status=" + rawResponse.getStatusCode());
            }
    		
    		System.out.println("rawResponse:");
    		System.out.println(rawResponse);

    		String rawBody = rawResponse.getBody();
    		if (rawBody == null || rawBody.isBlank()) {
    			throw new BusinessException(ErrorCode.PG_EMPTY_RESPONSE);
    		}

    		/* ===== 1. 기본 DTO 매핑 ===== */
    		PortOnePaymentResponse body =
    		        objectMapper.readValue(rawBody, PortOnePaymentResponse.class);

            // ===== 3. orderId 검증 =====		
			Long extractOrderId = extractOrderId(body.getCustomData());
			body.applyOrderId(extractOrderId);
			 
            return body;

        } catch (BusinessException e) {
            // 이미 의도된 예외 → 그대로 전달
            throw e;

        } catch (JsonProcessingException e) {
            log.error("PG 응답 JSON 파싱 실패. paymentId={}", paymentId, e);
            throw new BusinessException(ErrorCode.PG_PARSE_FAILED, e);

        } catch (Exception e) {
            log.error("PG 통신 중 예외 발생. paymentId={}", paymentId, e);
            throw new BusinessException(ErrorCode.PG_API_FAILED, e);
        }
    }
    
    // 나중에 유틸클래스 만들어서 옮길수도있음
    private Long extractOrderId(String customData) {

    	if (customData == null || customData.isBlank()) {
    		throw new BusinessException(ErrorCode.PG_INVALID_DATA, "customData empty");
        }

	    try {
	        PortOnePaymentResponse.CustomData data =
	            objectMapper.readValue(customData, PortOnePaymentResponse.CustomData.class);
	        
	        if (data.getOrderId() == null) {
	        	throw new BusinessException(ErrorCode.PG_INVALID_DATA, "customData.orderId is null");
	        }
	        
	        return data.getOrderId();

	    } catch (BusinessException e) {
	        throw e;
	    } catch (JsonProcessingException e) {
	        throw new BusinessException(ErrorCode.PG_PARSE_FAILED, "customData parse error", e);
	    } catch (Exception e) {
	        throw new BusinessException(ErrorCode.PG_INVALID_RESPONSE,"customData parsing failed: " + customData);
	    }
    }
    

}