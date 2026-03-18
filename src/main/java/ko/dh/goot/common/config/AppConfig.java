package ko.dh.goot.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public Clock clock() {
        // 시스템의 기본 시간대와 UTC를 기준으로 하는 Clock을 빈으로 등록합니다.
        // JWT 생성 시 일관된 시간 기준을 위해 systemUTC()를 권장합니다.
        return Clock.systemUTC();
    }
}
