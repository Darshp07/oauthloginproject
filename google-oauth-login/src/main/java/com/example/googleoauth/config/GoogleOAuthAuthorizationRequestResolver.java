
package com.example.googleoauth.config;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthAuthorizationRequestResolver
        implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public GoogleOAuthAuthorizationRequestResolver(
            ClientRegistrationRepository repository) {

        this.defaultResolver =
            new DefaultOAuth2AuthorizationRequestResolver(
                repository,
                "/oauth2/authorization"
            );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request) {

        OAuth2AuthorizationRequest authorizationRequest =
            defaultResolver.resolve(request);

        return customize(authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request,
            String clientRegistrationId) {

        OAuth2AuthorizationRequest authorizationRequest =
            defaultResolver.resolve(request, clientRegistrationId);

        return customize(authorizationRequest);
    }

    private OAuth2AuthorizationRequest customize(
            OAuth2AuthorizationRequest request) {
//
//        if (request == null ||
//            !"google".equals(request.getAttribute(
//            		OAuth2ParameterNames
//                    .REGISTRATION_ID))) {
//            return request;
//        }
        if (request == null) {
            return request;
        }
        return OAuth2AuthorizationRequest
            .from(request)
            .additionalParameters(params -> {
                params.put("access_type", "offline");
                params.put("prompt", "consent");
            })
            .build();
    }
}
