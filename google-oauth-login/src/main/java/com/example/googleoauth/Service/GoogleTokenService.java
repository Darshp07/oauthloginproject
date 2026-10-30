package com.example.googleoauth.Service;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
@Service
@RequiredArgsConstructor
public class GoogleTokenService {

	  private final GoogleIdTokenVerifier verifier;

	    public GoogleTokenService() {
	        this.verifier = new GoogleIdTokenVerifier.Builder(
	                new NetHttpTransport(),
	                GsonFactory.getDefaultInstance())
	                .setAudience(Collections.singletonList(
	                    "207867314906-6v3otfb7n03viaopsthhmkvmigamj92g.apps.googleusercontent.com"))
	                .build();
	    }

	    public GoogleIdToken.Payload verifyToken(String token)
	            throws Exception {

	        GoogleIdToken idToken = verifier.verify(token);

	        if (idToken == null) {
	            return null;
	        }

	        return idToken.getPayload();
	    }
}
