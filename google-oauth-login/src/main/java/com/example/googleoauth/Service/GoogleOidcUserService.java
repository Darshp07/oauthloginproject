package com.example.googleoauth.Service;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import com.example.googleoauth.entity.AppUser;

@Service
@RequiredArgsConstructor
public class GoogleOidcUserService extends OidcUserService
		implements OAuth2UserService<OidcUserRequest, OidcUser>  {

	private final AppUserService appUserService;
	private final OidcUserService delegate = new OidcUserService();

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest)
			throws OAuth2AuthenticationException {
		OidcUser googleUser = delegate.loadUser(userRequest);
		String email = googleUser.getEmail();

		if (email == null
				|| !Boolean.TRUE.equals(googleUser.getClaimAsBoolean("email_verified"))) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error("invalid_user_info"),
					"Google did not provide a verified email address.");
		}

		AppUser appUser = appUserService.findOrCreateGoogleUser(
				googleUser.getSubject(),
				email,
				googleUser.getFullName());
		if (!appUser.isEnabled() || !appUser.isAccountNonLocked()) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error("access_denied"),
					"This account is suspended.");
		}

		return new DefaultOidcUser(
				googleUser.getAuthorities(),
				googleUser.getIdToken(),
				googleUser.getUserInfo(),
				"email");
	}
}
