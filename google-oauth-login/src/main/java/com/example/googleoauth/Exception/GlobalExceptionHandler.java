package com.example.googleoauth.Exception;

import java.io.IOException;


import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import com.example.googleoauth.Utility.ApiResponse;
import com.example.googleoauth.Utility.ResponseBuilder;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
	  private static final Logger log =
	            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<String>> handleStatusException(
            ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(ResponseBuilder.error(exception.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataConflict(
            DataIntegrityViolationException exception) {
        log.warn("A database constraint prevented an account change.");
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                .body(ResponseBuilder.error(
                        "An account with this email or Google identity already exists."));
    }

//	@ExceptionHandler(Exception.class)
//	public ApiResponse<String> handleException(Exception e) throws IOException{
//		return ResponseBuilder.error("INTERNAL_SERVER_ERROR");
//	}
	@ExceptionHandler(RuntimeException.class)
	public ApiResponse<String> RuntimeException(Exception e) throws IOException{
		return ResponseBuilder.error("INTERNAL_SERVER_ERROR");
	}

	@ExceptionHandler(ClassNotFoundException.class)
	public ApiResponse<String> ClassNotFoundException(Exception e) throws IOException {
		return ResponseBuilder.error("INTERNAL_SERVER_ERROR");
	}
//	@ExceptionHandler(NoResourceFoundException.class)
//	public  ApiResponse<String> handleNotFound(
//	        NoResourceFoundException ex) {
//
//		return ResponseBuilder.successResponse(ex.getMessage());
//	}
	@ExceptionHandler(AuthorizationDeniedException.class)
	public ApiResponse<String> AuthorizationDeniedException(Exception e,HttpServletResponse response){
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		return ResponseBuilder.error("You don't have permission to access this resource.");
	}
	
	  @ExceptionHandler(NoResourceFoundException.class)
	    public ResponseEntity<String> handleNotFound(
	            NoResourceFoundException ex) {

	        log.warn("Resource not found: {}", ex.getMessage());

	        return ResponseEntity.status(HttpStatus.SC_NOT_FOUND)
	                .body(ex.getMessage());
	    }
}
