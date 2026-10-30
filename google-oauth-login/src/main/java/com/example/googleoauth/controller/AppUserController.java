package com.example.googleoauth.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.googleoauth.Service.AppUserService;
import com.example.googleoauth.Utility.ApiResponse;
import com.example.googleoauth.Utility.ResponseBuilder;
import com.example.googleoauth.dto.AppUserResponse;
import com.example.googleoauth.entity.AccountStatus;
import com.google.api.client.auth.oauth2.TokenResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/appUsers")
@CrossOrigin("*")
@RequiredArgsConstructor
@Tag(name = "User API")
public class AppUserController {

	private final AppUserService authService;

	
	
	@GetMapping("GetAllUsers")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<List<AppUserResponse>> getUsers(){
		return authService.getUsers();
	}

	@GetMapping("/me")
	public ApiResponse<AppUserResponse> getCurrentUser(
			@AuthenticationPrincipal Jwt jwt) {
		return authService.getUserByEmail(jwt.getSubject());
	}

	@PutMapping("/{userId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<AppUserResponse> updateAccountStatus(
			@PathVariable Long userId,
			@Valid @RequestBody AccountStatusRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return authService.updateAccountStatus(userId, request.status(), jwt.getSubject());
	}
	
	
	@PostMapping("/register")
	public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request) {

		try {
			return authService.registerLocalUser(request.name().trim(), request.email().trim(),
					request.password());

		} catch (IllegalArgumentException e) {
			return ResponseBuilder.error("Email is already registered");
		}
	}
	
	
	
	public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email,
			@NotBlank @Size(min = 8, max = 72) String password) {
	}

	public record GoogleLoginRequest(@NotBlank String credential) {
	}
	
	public record UserLogInRequest(String userName, String passWord ) {
		
	}
	
	
	public record RefreshTokenRequest(@NotBlank String refresh_token) {}

	public record AccountStatusRequest(@jakarta.validation.constraints.NotNull AccountStatus status) {}

	@PostMapping("/LogInUser")
	public ApiResponse<TokenResponse> UserLogin(@Valid @RequestBody UserLogInRequest req) {
		return authService.UserLogin(req);
	}

	@Operation(security = @SecurityRequirement(name = "oauth2"))
	@GetMapping("/test")
	public String test() {
		return "OAuth token working!";
	}
	
//	@PostMapping("/google")
//	public ApiResponse<TokenResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
//		try {
//			GoogleIdToken.Payload payload = googleTokenService.verifyToken(request.credential());
//
//			if(payload==null) {
//				return ResponseBuilder.error("Invalid Google ID Token");
//			}
//			String googleId = payload.getSubject();
//			String email = payload.getEmail();
//			String name = (String) payload.get("name");
//
//			if (email == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
//				return ResponseBuilder.error("Google email is not verified");
//			}
//
//			return authService.googleLogin(googleId, email, name);
//
//		} catch (Exception e) {
//			return ResponseBuilder.error("Google login failed: " + e.getMessage());
//		}
//	}

//	@PostMapping("/refresh")
//	public ApiResponse<TokenResponse> refreshToken(
//			@Valid @RequestBody RefreshTokenRequest request) {
//		return authService.refreshToken(request);
//	}
}
