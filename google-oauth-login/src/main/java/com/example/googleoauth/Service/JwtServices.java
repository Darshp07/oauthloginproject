package com.example.googleoauth.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.example.googleoauth.entity.AppUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtServices {


	@Value("${application.security.jwt.access-token-expiration}")
	private String JWT_TOKEN_VALIDITY;
	
	@Value("${application.security.jwt.refresh-token-expiration}")
	private  String JWT_REFRESH_TOKEN_VALIDITY;

	@Value("${application.security.jwt.secret-key}")
	private String key;
//	@Value("${application.security.jwt.issuer}")
//	private String issuer;
//
//	@Value("${application.security.jwt.audience}")
//	private String audience;

	public String buildToken(UserDetails userdata) {

	    Map<String, Object> claims = new HashMap<>();
		AppUser users = (AppUser) userdata;
		claims.put(
		        "role",
		        userdata.getAuthorities()
		            .stream()
		            .map(GrantedAuthority::getAuthority)
		            .toList()
		    );
		claims.put("token_type", "ACCESS");
		return Jwts.builder().claims(claims)
				.subject(users.getUsername())
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis()+Long.parseLong(JWT_TOKEN_VALIDITY)))
				.signWith(getSignInKey()).compact();
		
	}

	public  String refreshToken(UserDetails userdata) {

	    Map<String, Object> claims = new HashMap<>();
		AppUser users = (AppUser) userdata;
		claims.put(
		        "role",
		        userdata.getAuthorities()
		            .stream()
		            .map(GrantedAuthority::getAuthority)
		            .toList()
		    );
		claims.put("token_type", "REFRESH");
		return Jwts.builder().claims(claims)
				.subject(users.getUsername())
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis()+Long.parseLong(JWT_REFRESH_TOKEN_VALIDITY)))
				.signWith(getSignInKey())
				.compact();
		
	}
	private Key getSignInKey() {
		byte[] decode = Decoders.BASE64.decode(key.trim());
		return Keys.hmacShaKeyFor(decode);
	}

	public boolean isTokenValid(String jwt, UserDetails userDetails) {
		String username =extractUsername(jwt);
		return  (username.split(",")[0].equals(userDetails.getUsername()))&& !isTokenExpired(jwt);
	}

	private boolean isTokenExpired(String jwt) {
	
		return extractExpiration(jwt).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token,Claims::getExpiration);
		
	}
	private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
		Claims claims = extractAllClaims(token);
		return claimResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		
		 return Jwts.parser()
		            .verifyWith((SecretKey) getSignInKey())
		          //  .requireIssuer(issuer)
		          //  .requireAudience(audience)
		            .clockSkewSeconds(30)
		            .build()
		            .parseSignedClaims(token)
		            .getPayload();
	}

	public String extractUsername(String token) {
		return extractClaim(token,Claims::getSubject);
	}
	

	public boolean isRefreshTokenValid(String token) {
	    try {
	        Claims claims = Jwts.parser()
	                .verifyWith( (SecretKey)getSignInKey())
	                .clockSkewSeconds(30)
	                .build()
	                .parseSignedClaims(token)
	                .getPayload();

	        return "REFRESH".equals(
	                claims.get("token_type", String.class))
	                && claims.getSubject() != null  && claims.getExpiration().after(new Date());

	    } catch (JwtException | IllegalArgumentException e) {
	        return false;
	    }
	}

//	public String buildToken(UserDetails userDetails, String ipAddress) {
//		AppUser users = (AppUser) userDetails;
//	    Map<String, Object> claims = new HashMap<>();
//	    claims.put("ip", ipAddress);
//		 claims.put(
//			        "role",
//			        userDetails.getAuthorities()
//			                   .stream()
//			                   .map(GrantedAuthority::getAuthority)
//			                   .toList()
//			    );
//		return Jwts.builder().claims(claims).id(UUID.randomUUID().toString())
//				.subject(users.getUsername())
//				//.issuer(issuer)
//				//.audience()
//				//.add(audience)
//	            //.and()
//				.issuedAt(new Date(System.currentTimeMillis()))
//				.expiration(new Date(System.currentTimeMillis()+Long.parseLong(JWT_TOKEN_VALIDITY)))
//				.signWith(getSignInKey()).compact();
//		
//	}

	
}
