package com.example.googleoauth.Utility;

public class ResponseBuilder {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(1, "successFull", data);
	}

	public static <T> ApiResponse<T> successResponse(String massage) {
		return new ApiResponse<>(1, massage);
	}

	public static <T> ApiResponse<T> error(String massage) {
		return new ApiResponse<>(0, massage);
	}

	public static <T> ApiResponse<T> DataNotFound() {
		return new ApiResponse<>(0, "Data Not Found..!!");
	}
}
