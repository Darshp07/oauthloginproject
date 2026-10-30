package com.example.googleoauth.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.example.googleoauth.Repository.AppUserRepository;
import com.example.googleoauth.entity.AppUser;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppUserJwtAuthenticationConverter
		implements Converter<Jwt, JwtAuthenticationToken> {

	private final AppUserRepository userRepository;

	@Override
	public JwtAuthenticationToken convert(Jwt jwt) {
		String email = jwt.getSubject();
		if (email == null || email.isBlank()) {
			throw new BadCredentialsException("The access token has no user identity.");
		}

		AppUser user = userRepository.findByEmail(email);
		if (user == null) {
			throw new BadCredentialsException("No application account is linked to this token.");
		}
		if (!user.isEnabled() || !user.isAccountNonLocked()) {
			throw new DisabledException("This account is suspended.");
		}

		return new JwtAuthenticationToken(jwt, user.getAuthorities(), user.getEmail());
	}
}
