package com.example.googleoauth.config;

import java.util.List;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;

import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.googleoauth.Service.GoogleOidcUserService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

	private final AuthenticationProvider authenticationProvider;
	private final AuthenticationEntryPoint authenticationEntryPoint;
	private final AccessDeniedHandler accessDeniedHandler;
	private final GoogleOidcUserService googleOidcUserService;
	private final AppUserJwtAuthenticationConverter appUserJwtAuthenticationConverter;
	private static final String[] WHITE_LIST_URL = { "/swagger-ui/**",
			"/v3/api-docs/**",
			"/swagger-resources/**",
			"/api/auth/appUsers/register" };


@Bean
@Order(1)
public SecurityFilterChain authorizationServerFilterChain(
        HttpSecurity http,
        OAuth2AuthorizationService authorizationService,
        OAuth2AuthorizationConsentService authorizationConsentService)
        throws Exception {

    OAuth2AuthorizationServerConfigurer authorizationServer =
            OAuth2AuthorizationServerConfigurer.authorizationServer();

    http
        .securityMatcher(authorizationServer.getEndpointsMatcher())
        .with(authorizationServer, server -> server
            .authorizationService(authorizationService)
            .authorizationConsentService(authorizationConsentService)
        )
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        )
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(
                new LoginUrlAuthenticationEntryPoint(
                    "http://localhost:5173/?oauthLogin"
                )
            )
        );

    return http.build();
}

	
		@Bean
		@Order(2)
		public SecurityFilterChain defaultSecurityFilterChain(
		        HttpSecurity http) throws Exception {

		    SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();

		    successHandler.setDefaultTargetUrl("http://localhost:5173/?oauthLogin");
		    successHandler.setAlwaysUseDefaultTargetUrl(false);
		    SimpleUrlAuthenticationSuccessHandler googleSuccessHandler = new SimpleUrlAuthenticationSuccessHandler("http://localhost:5173/?oauthLogin&google=success");
		    googleSuccessHandler.setAlwaysUseDefaultTargetUrl(true);

		    http
		        .cors(Customizer.withDefaults())
		        .csrf(csrf -> csrf.disable())
		        .authenticationProvider(authenticationProvider)
		        .authorizeHttpRequests(auth -> auth
		            .requestMatchers(WHITE_LIST_URL).permitAll()
		            .anyRequest().authenticated())
		        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
		                .jwtAuthenticationConverter(appUserJwtAuthenticationConverter)))
		        .formLogin(form -> form
		            .loginPage("http://localhost:5173/?oauthLogin")
		            .loginProcessingUrl("/login")
		            .usernameParameter("username")
		            .passwordParameter("password")
		            .failureUrl("http://localhost:5173/?oauthLogin&error")
		            .successHandler(successHandler)
		            .permitAll())
		        .oauth2Login(oauth2 -> oauth2
		            .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
		            .successHandler(googleSuccessHandler)
		            .failureUrl("http://localhost:5173/?oauthLogin&error=google"))
		        .exceptionHandling(exception -> exception
	        		    .authenticationEntryPoint(authenticationEntryPoint)
	        		    .accessDeniedHandler(accessDeniedHandler))
		        .logout(logout -> logout
		            .logoutUrl("/logout")
		            .invalidateHttpSession(true)
		            .clearAuthentication(true)
		            .deleteCookies("JSESSIONID")
		            .logoutSuccessUrl("http://localhost:5173/?loggedOut"));

		    return http.build();
	}
		
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of("http://localhost:5173"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}