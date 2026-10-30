
package com.example.googleoauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;

import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;

import com.example.googleoauth.Service.CustomUserPrincipal;
import com.example.googleoauth.entity.AppUser;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class OAuthPersistenceConfig {

	@Bean
	public OAuth2AuthorizationService authorizationService(
	        JdbcOperations jdbcOperations,
	        RegisteredClientRepository registeredClientRepository) {

	    JdbcOAuth2AuthorizationService service =
	            new JdbcOAuth2AuthorizationService(
	                    jdbcOperations,
	                    registeredClientRepository
	            );

	    ObjectMapper objectMapper = new ObjectMapper();

	    ClassLoader classLoader = getClass().getClassLoader();

	    objectMapper.registerModules(
	            SecurityJackson2Modules.getModules(classLoader)
	    );

	    SecurityJackson2Modules.enableDefaultTyping(objectMapper);

	    objectMapper.registerModule(
	            new OAuth2AuthorizationServerJackson2Module()
	    );

	    // Allow CustomUserPrincipal
	    objectMapper.addMixIn(
	            CustomUserPrincipal.class,
	            CustomUserPrincipalMixin.class
	    );

	    JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper rowMapper =
	            new JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper(
	                    registeredClientRepository
	            );

	    rowMapper.setObjectMapper(objectMapper);

	    service.setAuthorizationRowMapper(rowMapper);

	    return service;
	}

	private abstract static class CustomUserPrincipalMixin {
	}
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationConsentService(
                jdbcOperations,
                registeredClientRepository);
    }

    // Explicit Jackson mixin for AppUser
    private abstract static class AppUserMixin {
    }
}
