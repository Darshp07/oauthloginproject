package com.example.googleoauth.Service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.example.googleoauth.entity.AppUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleOidcUserService extends OidcUserService {

    private final AppUserService appUserService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {

        OidcUser googleUser = super.loadUser(userRequest);

        String email = googleUser.getEmail();

        if (email == null ||
            !Boolean.TRUE.equals(
                googleUser.getClaimAsBoolean("email_verified"))) {

            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_user_info"),
                "Google did not provide a verified email address."
            );
        }

        AppUser appUser = appUserService.findOrCreateGoogleUser(
            googleUser.getSubject(),
            email,
            googleUser.getFullName()
        );

        if (!appUser.isEnabled() || !appUser.isAccountNonLocked()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("access_denied"),
                "This account is suspended."
            );
        }

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        googleUser.getAuthorities().forEach(authority ->
            authorities.add(
                new SimpleGrantedAuthority(authority.getAuthority())
            )
        );

        // Add roles from your database
        appUser.getRoles().forEach(role ->
            authorities.add(
                new SimpleGrantedAuthority(role.getRoleName())
            )
        );

        return new DefaultOidcUser(
            authorities,
            googleUser.getIdToken(),
            googleUser.getUserInfo(),
            "email"
        );
    }
}