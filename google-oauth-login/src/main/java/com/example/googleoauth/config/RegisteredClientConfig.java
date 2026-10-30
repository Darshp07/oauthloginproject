
package com.example.googleoauth.config;

import java.time.Duration;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@Configuration
public class RegisteredClientConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            JdbcOperations jdbcOperations,
            PasswordEncoder passwordEncoder) {

        JdbcRegisteredClientRepository repository =
                new JdbcRegisteredClientRepository(jdbcOperations);

        // Swagger OAuth client
        RegisteredClient swaggerClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId("google-oauth-client")
                .clientSecret(passwordEncoder.encode("test-secret"))
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(
                        AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(
                        "http://localhost:8080/swagger-ui/oauth2-redirect.html")
                .scope("read")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .build())
                .build();

        // React public client with PKCE
        RegisteredClient reactClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId("darshak-react-client")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.NONE)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(
                        AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:5173/callback")
                .scope("read")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        // Register only when the client does not already exist
        registerIfMissing(repository, swaggerClient);
        registerIfMissing(repository, reactClient);

        return repository;
    }

    private void registerIfMissing(
            RegisteredClientRepository repository,
            RegisteredClient client) {

        if (repository.findByClientId(client.getClientId()) == null) {
            repository.save(client);
        }
    }
}
