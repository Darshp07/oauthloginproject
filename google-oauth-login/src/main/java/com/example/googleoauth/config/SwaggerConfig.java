package com.example.googleoauth.config;

import org.springframework.context.annotation.Bean;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("oauth2",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows()
                            .authorizationCode(new OAuthFlow()
                                .authorizationUrl(
                                    "http://localhost:8080/oauth2/authorize")
                                .tokenUrl(
                                    "http://localhost:8080/oauth2/token")
                                .scopes(new Scopes()
                                    .addString("read", "Read access"))
                            )
                        )
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("oauth2"));
    }
}
