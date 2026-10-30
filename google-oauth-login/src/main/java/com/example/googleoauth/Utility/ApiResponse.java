package com.example.googleoauth.Utility;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

	  public Integer code;
	    public String message;
	    public T data;

	    public ApiResponse(Integer code, String message) {
	        this.code = code;
	        this.message = message;

	    }

}
