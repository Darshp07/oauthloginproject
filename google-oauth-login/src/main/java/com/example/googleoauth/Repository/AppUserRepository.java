package com.example.googleoauth.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.googleoauth.entity.AppUser;

public interface  AppUserRepository   extends JpaRepository<AppUser, Long>{



	//Optional<AppUser> findByEmail(String email);

	AppUser findByEmail(String emial);
    Optional<AppUser> findByGoogleId(String googleId);

    boolean existsByEmail(String email);
   // AppUser findByUsername(String username);
	Optional<AppUser> findByEmailIgnoreCase(String normalizedEmail);
}
