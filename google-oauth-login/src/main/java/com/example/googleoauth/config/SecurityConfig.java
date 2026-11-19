
package com.example.googleoauth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
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

    private final GoogleOAuthAuthorizationRequestResolver googleOAuthAuthorizationRequestResolver;

    private final AuthenticationProvider authenticationProvider;

    private final AuthenticationEntryPoint authenticationEntryPoint;

    private final AccessDeniedHandler accessDeniedHandler;

    private final GoogleOidcUserService googleOidcUserService;

    private static final String[] WHITE_LIST_URL = {
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/swagger-resources/**",
        "/api/auth/appUsers/register"
    };

    // ==========================================
    // 1. Authorization Server Configuration
    // ==========================================

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
                        "http://localhost:5173/"
                    )
                )
            );

        return http.build();
    }

    // ==========================================
    // 2. Default Security Configuration
    // ==========================================

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http) throws Exception {

        // Email / Password Login Success Handler
        SavedRequestAwareAuthenticationSuccessHandler formSuccessHandler =
                new SavedRequestAwareAuthenticationSuccessHandler();

        formSuccessHandler.setDefaultTargetUrl(
                "http://localhost:5173/?login=success"
        );

        formSuccessHandler.setAlwaysUseDefaultTargetUrl(true);

        // Google Login Success Handler
        SimpleUrlAuthenticationSuccessHandler googleSuccessHandler =
                new SimpleUrlAuthenticationSuccessHandler(
                        "http://localhost:5173/?google=success"
                );

        googleSuccessHandler.setAlwaysUseDefaultTargetUrl(true);

        http

            // CORS
            .cors(Customizer.withDefaults())

            // CSRF - Development only
            .csrf(csrf -> csrf.disable())

            // Authentication Provider
            .authenticationProvider(authenticationProvider)

            // Session Management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            // Authorization
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(WHITE_LIST_URL).permitAll()
                .anyRequest().authenticated()
            )

            // Email / Password Login
            .formLogin(form -> form
                .loginPage("http://localhost:5173/")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .failureUrl("http://localhost:5173/?error=login")
                .successHandler(formSuccessHandler)
                .permitAll()
            )

            // Google OAuth2 / OIDC Login
            .oauth2Login(oauth2 -> oauth2

                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestResolver(
                        googleOAuthAuthorizationRequestResolver
                    )
                )

                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(googleOidcUserService)
                )

                .successHandler(googleSuccessHandler)

                .failureUrl(
                    "http://localhost:5173/?error=google"
                )
            )

            // Exception Handling
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessUrl(
                    "http://localhost:5173/?loggedOut"
                )
            );

        return http.build();
    }

    // ==========================================
    // 3. CORS Configuration
    // ==========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
            List.of("http://localhost:5173")
        );

        config.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
            )
        );

        config.setAllowedHeaders(
            List.of("*")
        );

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            config
        );

        return source;
    }
}
