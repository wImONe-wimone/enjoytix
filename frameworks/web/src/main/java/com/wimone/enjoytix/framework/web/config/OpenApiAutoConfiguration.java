package com.wimone.enjoytix.framework.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;

@AutoConfiguration
@ConditionalOnClass({OpenAPI.class, GroupedOpenApi.class})
public class OpenApiAutoConfiguration {

    private static final String BEARER_AUTH_SECURITY_SCHEME = "BearerAuth";
    private static final String USER_ID_SECURITY_SCHEME = "X-User-Id";

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI enjoyTixOpenApi(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name", "enjoytix-service");
        return new OpenAPI()
                .info(new Info()
                        .title(toTitle(applicationName))
                        .version(environment.getProperty("info.app.version", "0.1.0-SNAPSHOT"))
                        .description("EnjoyTix service API documentation."))
                .servers(List.of(new Server()
                        .url("/")
                        .description("Current Swagger UI origin")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(BEARER_AUTH_SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("DevToken")
                                .description("Gateway development token. Enter dev-1 to send Authorization: Bearer dev-1."))
                        .addSecuritySchemes(USER_ID_SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(USER_ID_SECURITY_SCHEME)
                                .description("User identity header propagated by the gateway.")));
    }

    @Bean
    @ConditionalOnMissingBean(name = "enjoyTixApiGroup")
    public GroupedOpenApi enjoyTixApiGroup() {
        return GroupedOpenApi.builder()
                .group("enjoytix-api")
                .pathsToMatch("/api/**")
                .build();
    }

    private String toTitle(String applicationName) {
        String normalized = applicationName
                .replace("enjoytix-", "")
                .replace("-", " ");
        return "EnjoyTix " + normalized + " API";
    }
}
