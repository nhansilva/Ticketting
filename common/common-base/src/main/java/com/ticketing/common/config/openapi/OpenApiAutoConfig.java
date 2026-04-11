package com.ticketing.common.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration OpenAPI/Swagger — dùng chung toàn bộ services.
 *
 * Activate khi springdoc có trên classpath (service tự thêm vào pom).
 * Service configure title/description qua application.yml:
 * <pre>
 * common:
 *   openapi:
 *     title: "Event Catalog Service API"
 *     description: "Concert and movie event management"
 * </pre>
 */
@Configuration
@ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class OpenApiAutoConfig {

    @Value("${common.openapi.title:${spring.application.name:Service} API}")
    private String title;

    @Value("${common.openapi.description:API documentation}")
    private String description;

    @Value("${common.openapi.version:1.0.0}")
    private String version;

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI commonOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version)
                        .contact(new Contact()
                                .name("Ticketing Team")
                                .email("dev@ticketing.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token (không cần prefix 'Bearer')")));
    }
}
