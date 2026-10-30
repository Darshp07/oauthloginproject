
package com.example.googleoauth.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.googleoauth.Repository.AppUserRepository;
import com.example.googleoauth.Repository.RoleRepository;
import com.example.googleoauth.Service.CustomUserPrincipal;
import com.example.googleoauth.entity.AppUser;
import com.example.googleoauth.entity.Role;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

	 private final AppUserRepository repository ;
	
	 
	 @Bean
	 public UserDetailsService userDetailsService() {

	     return email -> {

	         AppUser user = repository.findByEmail(email);

	         if (user == null) {
	             throw new UsernameNotFoundException("User not found");
	         }

	         List<String> roles = user.getRoles()
	        	        .stream()
	        	        .map(role -> role.getRoleName().startsWith("ROLE_")
	        	                ? role.getRoleName()
	        	                : "ROLE_" + role.getRoleName())
	        	        .collect(Collectors.toCollection(ArrayList::new)); 
	         return new CustomUserPrincipal(
	                 user.getId(),
	                 user.getEmail(),
	                 user.getPassword(),
	                 roles
	                 
	         );
	         
	     };
	 }
	 @Bean
	 public AuthenticationProvider authenticationProvider(
			 UserDetailsService userDetailsService,
			 PasswordEncoder passwordEncoder) {
		 
		 DaoAuthenticationProvider authProvider =
				 new DaoAuthenticationProvider(userDetailsService);
		 
		 authProvider.setPasswordEncoder(passwordEncoder);
		 return authProvider;
	 }
//
//	    @Bean
//	    public AuthenticationProvider authenticationProvider() {
//	        DaoAuthenticationProvider authProvider =new DaoAuthenticationProvider(userDetailsService());
//	        authProvider.setPasswordEncoder(passwordEncoder());
//	        return authProvider;
//	    }


	    @Bean
	    public PasswordEncoder passwordEncoder() {
	        return new BCryptPasswordEncoder();
	    }

	    @Bean
	    public ApplicationRunner initializeRoles(RoleRepository roleRepository) {
	        return args -> {
	            for (String roleName : new String[] { "ROLE_USER", "ROLE_ADMIN" }) {
	                if (roleRepository.findByRoleName(roleName).isEmpty()) {
	                    Role role = new Role();
	                    role.setRoleName(roleName);
	                    roleRepository.save(role);
	                }
	            }
	        };
	    }

//	    @Bean
//	    public AuditorAware<Integer> auditorAware() {
//	        return new ApplicationAuditAware();
//	    }

	    @Bean
	    public AuthenticationManager authenticationManager(
	            AuthenticationConfiguration config)
	            throws Exception {

	        return config.getAuthenticationManager();
	    }
}
