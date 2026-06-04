package com.eprint.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class SpringDocConfig {

    public static final String BASIC_AUTH_SCHEME = "basicAuth";

    @Value("${app.base-uri}")
    private String appBaseUri;

    @Bean
    public OpenAPI openAPI() {
        log.info("Initializing SpringDoc OpenAPI configuration");

        OpenAPI openApi = new OpenAPI()
                .servers(List.of(new Server().url(appBaseUri)))
                .info(new Info()
                        .title("E-PRINT-SERVER API")
                        .description("E-PRINT-SERVER API")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME));

        log.info("SpringDoc OpenAPI configuration initialized successfully");
        return openApi;
    }
}
