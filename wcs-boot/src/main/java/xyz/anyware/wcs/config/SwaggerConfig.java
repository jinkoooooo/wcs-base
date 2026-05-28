package xyz.anyware.wcs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * 특정 REST API 패키지만 노출되도록 그룹 구성
     */
    @Bean
    public GroupedOpenApi wcsRestApi() {
        return GroupedOpenApi.builder()
                .group("WES 전용 API")
                .packagesToScan("xyz.anyware.wcs.rest")
                .pathsToMatch("/rest/**")
                .addOpenApiCustomizer(securityOpenApiCustomizer())
                .build();
    }

    /**
     * Swagger 문서의 기본 정보 및 인증 설정
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WES I/F Document")
                        .description("WES I/F Document")
                        .version("1.0.0"))
                .components(securityComponents())
                .addSecurityItem(new SecurityRequirement().addList("Authorization"));
    }

    /**
     * 인증 스키마 정의
     */
    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("Authorization",
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("JWT 토큰 또는 사용자 정의 토큰을 입력하세요. 예: Bearer {token}"));
    }

    /**
     * 모든 API 요청에 Authorization 헤더 자동 적용
     */
    private OpenApiCustomizer securityOpenApiCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation ->
                        operation.addSecurityItem(new SecurityRequirement().addList("Authorization"))
                )
        );
    }
}