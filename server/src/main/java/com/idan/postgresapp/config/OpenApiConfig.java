package com.idan.postgresapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("PostgreSQL Live Hub Enterprise API")
                        .version("2.0.0")
                        .description("מערכת ניהול מלאה (Spring Boot 3 + PostgreSQL 16 + JWT Security) כולל מוצרים, הזמנות, משתמשים, אנליטיקה ויומן פעילות (Audit Log).")
                        .contact(new Contact().name("Idan").email("idan@example.com")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("הזן את טוקן ה-JWT שלך (ללא המילה Bearer)")));
    }
}
