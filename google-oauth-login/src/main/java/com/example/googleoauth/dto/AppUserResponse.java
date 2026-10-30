package com.example.googleoauth.dto;

import java.util.List;

import com.example.googleoauth.entity.AppUser;

public record AppUserResponse(
		Long id,
		String name,
		String email,
		String authProvider,
		String status,
		List<String> roles) {

	public static AppUserResponse from(AppUser user) {
		List<String> authorities = user.getAuthorities().stream()
				.map(authority -> authority.getAuthority())
				.sorted()
				.toList();
		return new AppUserResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getAuthProvider(),
				user.getStatus().name(),
				authorities);
	}
	
	
}
