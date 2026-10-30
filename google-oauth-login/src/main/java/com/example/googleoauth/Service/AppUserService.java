	package com.example.googleoauth.Service;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.googleoauth.Repository.AppUserRepository;
import com.example.googleoauth.Repository.RoleRepository;
import com.example.googleoauth.Utility.ApiResponse;
import com.example.googleoauth.Utility.ResponseBuilder;
import com.example.googleoauth.controller.AppUserController.RefreshTokenRequest;
import com.example.googleoauth.controller.AppUserController.UserLogInRequest;
import com.example.googleoauth.dto.AppUserResponse;
import com.example.googleoauth.entity.AccountStatus;
import com.example.googleoauth.entity.AppUser;
import com.example.googleoauth.entity.Role;
import com.google.api.client.auth.oauth2.TokenResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppUserService {

	private final AppUserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtServices jwtServices;
	private final UserDetailsService userDetailsService;
	@Transactional
	public ApiResponse<String> registerLocalUser(String name, String email, String password) {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

		if (userRepository.existsByEmail(normalizedEmail)) {
			return ResponseBuilder.error("Email is already registered");
		}

		AppUser user = new AppUser();
		user.setName(name);
		user.setEmail(normalizedEmail);
		user.setPassword(passwordEncoder.encode(password));
		user.setAuthProvider("LOCAL");
		user.setRoles(Set.of(findUserRole()));
		userRepository.save(user);
		return ResponseBuilder.successResponse("Registration completed successfully.");
	}

//	@Transactional
//	public AppUser findOrCreateGoogleUser(String googleId, String email, String name) {
//		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
//		Optional<AppUser> existingGoogleUser = userRepository.findByGoogleId(googleId);
//		if (existingGoogleUser.isPresent()) {
//			AppUser linkedUser = existingGoogleUser.get();
//			if (!linkedUser.getEmail().equalsIgnoreCase(normalizedEmail)) {
//				throw new IllegalArgumentException(
//						"This Google account is already linked to a different email.");
//			}
//			return linkedUser;
//		}
//
//		AppUser user = userRepository.findByEmail(normalizedEmail);
//		if (user == null) {
//			user = new AppUser();
//			user.setName(name == null || name.isBlank()
//					? normalizedEmail.substring(0, normalizedEmail.indexOf('@'))
//					: name.trim());
//			user.setEmail(normalizedEmail);
//			user.setGoogleId(googleId);
//			user.setAuthProvider("GOOGLE");
//			user.setRoles(Set.of(findUserRole()));
//			return userRepository.save(user);
//		}
//
//		if (user.getGoogleId() != null && !user.getGoogleId().equals(googleId)) {
//			throw new IllegalArgumentException(
//					"This email is linked to another Google account.");
//		}
//		user.setGoogleId(googleId);
//		user.setAuthProvider("GOOGLE");
//		if (user.getRoles().isEmpty()) {
//			user.setRoles(Set.of(findUserRole()));
//		}
//		return userRepository.save(user);
//	}

@Transactional
public AppUser findOrCreateGoogleUser(
        String googleId, String email, String name) {

    if (googleId == null || googleId.isBlank()
            || email == null || email.isBlank()) {
        throw new IllegalArgumentException(
                "Google ID and email are required.");
    }

    String normalizedEmail =
            email.trim().toLowerCase(Locale.ROOT);

    // 1. Find an existing Google-linked account
    Optional<AppUser> existingGoogleUser =
            userRepository.findByGoogleId(googleId);

    if (existingGoogleUser.isPresent()) {
        AppUser user = existingGoogleUser.get();

        if (!normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException(
                    "This Google account is linked to a different email.");
        }

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new IllegalStateException(
                    "This account is suspended.");
        }

        return user;
    }

    // 2. Do not automatically link an existing account by email
    AppUser existingEmailUser =
            userRepository.findByEmail(normalizedEmail);
    
    Optional<AppUser> emailAccount =
            userRepository.findByEmailIgnoreCase(normalizedEmail);


    if (emailAccount.isPresent()) {
        throw new IllegalArgumentException(
                "An account with this email already exists. "
                + "Sign in using your existing method.");
    }

    // 3. Create a new Google user
    AppUser user = new AppUser();

    user.setName(
            name == null || name.isBlank()
                    ? normalizedEmail.substring(
                            0, normalizedEmail.indexOf('@'))
                    : name.trim());

    user.setEmail(normalizedEmail);
    user.setGoogleId(googleId);
    user.setAuthProvider("GOOGLE");
    user.setStatus(AccountStatus.ACTIVE);

    // Assign the default USER role
    user.setRoles(Set.of(findUserRole()));

    // 4. Save and return the new user
    return userRepository.save(user);
}


	@Transactional
	public ApiResponse<TokenResponse> googleLogin(String googleId, String email, String name) {
		AppUser user;
		try {
			user = findOrCreateGoogleUser(googleId, email, name);
		} catch (IllegalArgumentException e) {
			return ResponseBuilder.error(e.getMessage());
		}

		String token = jwtServices.buildToken(user);
		String refeshTone= jwtServices.refreshToken(user);
		TokenResponse response = new TokenResponse();
		response.setAccessToken(token);
		response.setRefreshToken(refeshTone);
		return ResponseBuilder.success(response);
	}

//	public ApiResponse<List<AppUserResponse>> getUsers() {
//		List<AppUser> all = userRepository.findAll();
//		List<AppUserResponse> users = userRepository.findAll().stream()
//				.map(AppUserResponse::from)
//				.toList();
//		return ResponseBuilder.success(users);
//	}
	
	public ApiResponse<List<AppUserResponse>> getUsers() {

	    Authentication authentication = SecurityContextHolder
	            .getContext()
	            .getAuthentication();

	    boolean isAdmin = authentication.getAuthorities().stream()
	            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

	    List<AppUser> allUsers = userRepository.findAll();

	    List<AppUserResponse> users = allUsers.stream()
	            .filter(user -> isAdmin ||
	                    user.getRoles().stream()
	                            .noneMatch(role ->
	                                    role.getRoleName().equals("ROLE_ADMIN")))
	            .map(AppUserResponse::from)
	            .toList();

	    return ResponseBuilder.success(users);
	}

	public ApiResponse<AppUserResponse> getUserByEmail(String email) {
		AppUser user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found.");
		}
		return ResponseBuilder.success(AppUserResponse.from(user));
	}

	@Transactional
	public ApiResponse<AppUserResponse> updateAccountStatus(
			Long userId,
			AccountStatus status,
			String requesterEmail) {
		AppUser user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "User account was not found."));
		if (user.getEmail().equalsIgnoreCase(requesterEmail)
				&& status != AccountStatus.ACTIVE) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "You cannot suspend your own account.");
		}
		user.setStatus(status);
		return ResponseBuilder.success(AppUserResponse.from(userRepository.save(user)));
	}

	private Role findUserRole() {
		return roleRepository.findByRoleName("ROLE_USER")
				.orElseThrow(() -> new IllegalStateException(
						"ROLE_USER has not been initialized."));
	}

	public ApiResponse<TokenResponse> UserLogin(UserLogInRequest req) {
		String normalizedEmail = req.userName() == null ? "" : req.userName().trim().toLowerCase(Locale.ROOT);
		var userdata = userRepository.findByEmail(normalizedEmail);
		UserDetails userDetails = userDetailsService.loadUserByUsername(normalizedEmail);
		if (userDetails == null || userdata == null) {
			return ResponseBuilder.DataNotFound();
		}
		if (!userdata.isEnabled() || !userdata.isAccountNonLocked()) {
			return ResponseBuilder.error("This account is suspended.");
		}
		if (!passwordEncoder.matches(
		        req.passWord(),
		        userDetails.getPassword())) {
			return ResponseBuilder.error("User Password is not valid..!!");
		}
		String token = jwtServices.buildToken(userdata);
		String refeshTone= jwtServices.refreshToken(userdata);
		TokenResponse response = new TokenResponse();
		response.setAccessToken(token);
		response.setRefreshToken(refeshTone);
		return ResponseBuilder.success(response);
	}

	public ApiResponse<TokenResponse> refreshToken(RefreshTokenRequest request) {
		 String refreshToken = request.refresh_token();


		    if (refreshToken == null || refreshToken.isBlank()) {
		        return ResponseBuilder.error(
		                "Refresh token is required");
		    }

		    // 2. Refresh token validate
		    if (!jwtServices.isRefreshTokenValid(refreshToken)) {
		        return ResponseBuilder.error(
		        		"Refresh token is invalid or expired. Please login again.");
		    }

		    // 3. Token mathi username extract
		    String username = jwtServices.extractUsername(refreshToken);

		    // 4. Database mathi user find
		    AppUser user = userRepository.findByEmail(username);

		    if (user == null) {
		        return ResponseBuilder.error("User not found");
		    }

		    // 5. Navo access token generate
		    String newAccessToken = jwtServices.buildToken(user);

		    TokenResponse response = new TokenResponse();
			response.setAccessToken(newAccessToken);
		return ResponseBuilder.success(response);
	}
}
