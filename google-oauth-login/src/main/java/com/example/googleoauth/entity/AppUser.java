package com.example.googleoauth.entity;

import java.time.LocalDateTime;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "app_users")
public class AppUser implements UserDetails {

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @Column(nullable = false, length = 100)
	    private String name;

	    @Column(nullable = false, unique = true, length = 190)
	    private String email;

	    @Column(length = 255)
	    private String password;

	    @Column(name = "google_id", unique = true, length = 255)
	    private String googleId;

	    @Column(name = "auth_provider", nullable = false, length = 20)
	    private String authProvider;

	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false, length = 20)
	    private AccountStatus status = AccountStatus.ACTIVE;

	    @Column(name = "created_at", insertable = false, updatable = false)
	    private LocalDateTime createdAt;

	    
	    @ManyToMany(fetch = FetchType.EAGER)
	    @JoinTable(
	        name = "user_roles",
	        joinColumns = @JoinColumn(name = "user_id"),
	        inverseJoinColumns = @JoinColumn(name = "role_id")
	    )
	    private Set<Role> roles = new HashSet<>();
	    
	    
		@Override
		public Collection<? extends GrantedAuthority> getAuthorities() {
		    return roles.stream()
		            .map(role -> role.getRoleName().startsWith("ROLE_")
		                    ? role.getRoleName()
		                    : "ROLE_" + role.getRoleName())
		            .map(SimpleGrantedAuthority::new)
		            .toList();
		}

		@Override
		public String getUsername() {
			return email;
		}
		
		@Override
		public String getPassword() {
		    return password;
		}

		@Override
		public boolean isAccountNonExpired() {
		    return true;
		}

		@Override
		public boolean isAccountNonLocked() {
		    return status == AccountStatus.ACTIVE;
		}

		@Override
		public boolean isCredentialsNonExpired() {
		    return true;
		}

		@Override
		public boolean isEnabled() {
		    return status == AccountStatus.ACTIVE;
		}

}
