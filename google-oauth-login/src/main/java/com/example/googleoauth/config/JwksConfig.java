package com.example.googleoauth.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Locale;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import com.example.googleoauth.Repository.AppUserRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Configuration
public class JwksConfig {
	@Bean
	public JWKSource<SecurityContext> jwkSource(
	        @Value("${application.security.oauth2.keystore-path}") String keyStorePath,
	        @Value("${application.security.oauth2.keystore-password}") String keyStorePassword,
	        @Value("${application.security.oauth2.key-alias}") String keyAlias
	) throws Exception {

	    KeyStore keyStore = KeyStore.getInstance("PKCS12");

	    try (InputStream inputStream =
	            new FileInputStream(keyStorePath)) {

	        keyStore.load(inputStream, keyStorePassword.toCharArray());
	    }

	    PrivateKey privateKey = (PrivateKey) keyStore.getKey(
	            keyAlias, keyStorePassword.toCharArray());

	    X509Certificate certificate =
	            (X509Certificate) keyStore.getCertificate(keyAlias);

	    RSAPublicKey publicKey =
	            (RSAPublicKey) certificate.getPublicKey();

	    RSAPrivateKey rsaPrivateKey =
	            (RSAPrivateKey) privateKey;

	    RSAKey rsaKey = new RSAKey.Builder(publicKey)
	            .privateKey(rsaPrivateKey)
	            .keyID("oauth-signing-key-v1")
	            .build();

	    JWKSet jwkSet = new JWKSet(rsaKey);

	    return (jwkSelector, securityContext) ->
	            jwkSelector.select(jwkSet);
	}
//    @Bean
//    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
//        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
//    }
    @Bean
    public JwtDecoder jwtDecoder(
            JWKSource<SecurityContext> jwkSource,
            @Value("${application.security.jwt.secret-key}") String configuredSecret) {
        JwtDecoder oauthJwtDecoder =
                OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
        byte[] secretBytes = Decoders.BASE64.decode(configuredSecret.trim());
        SecretKey secretKey = Keys.hmacShaKeyFor(secretBytes);
        JwtDecoder appJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        return token -> {
            final JWSAlgorithm algorithm;
            try {
                algorithm = SignedJWT.parse(token).getHeader().getAlgorithm();
            } catch (java.text.ParseException exception) {
                throw new JwtException("Invalid JWT format.", exception);
            }

            if (JWSAlgorithm.HS256.equals(algorithm)) {
                var decodedToken = appJwtDecoder.decode(token);
                if (!"ACCESS".equals(decodedToken.getClaimAsString("token_type"))) {
                    throw new JwtException("JWT is not an access token.");
                }
                return decodedToken;
            }
            if (JWSAlgorithm.RS256.equals(algorithm)) {
                return oauthJwtDecoder.decode(token);
            }
            throw new JwtException("Unsupported JWT signing algorithm.");
        };
        
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8080")
                .build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> appUserTokenCustomizer(
            AppUserRepository userRepository) {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                return;
            }
            String email = context.getPrincipal().getName()
                    .trim()
                    .toLowerCase(Locale.ROOT);
            var user = userRepository.findByEmail(email);
            if (user != null) {
                if (!user.isEnabled() || !user.isAccountNonLocked()) {
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("access_denied"),
                            "This account is suspended.");
                }
                context.getClaims().subject(user.getEmail());
                context.getClaims().claim("email", user.getEmail());
            }
        };
    }
}