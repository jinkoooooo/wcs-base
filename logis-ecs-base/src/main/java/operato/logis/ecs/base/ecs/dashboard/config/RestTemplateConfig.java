package operato.logis.ecs.base.ecs.dashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 설정
 *
 * ECS API 호출을 위한 RestTemplate Bean 설정
 */
@Configuration
public class RestTemplateConfig {

    @Value("${ecs.api.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${ecs.api.read-timeout:10000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .readTimeout(Duration.ofMillis(readTimeout))
                .build();
    }
}
