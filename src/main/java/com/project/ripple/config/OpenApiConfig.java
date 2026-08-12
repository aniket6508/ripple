package com.project.ripple.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI rippleOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ripple API")
                        .version("0.0.1")
                        .description("API for parsing Java repositories into source and call graph metadata."));
    }
}
