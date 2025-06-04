package com.example.globalradioapp.network;

/**
 * Enhanced API response wrapper for better error handling and loading states
 */
public class ApiResponse<T> {
    
    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }
    
    private final Status status;
    private final T data;
    private final String message;
    private final int code;
    
    private ApiResponse(Status status, T data, String message, int code) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.code = code;
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Status.SUCCESS, data, null, 200);
    }
    
    public static <T> ApiResponse<T> error(String message, int code) {
        return new ApiResponse<>(Status.ERROR, null, message, code);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(Status.ERROR, null, message, -1);
    }
    
    public static <T> ApiResponse<T> loading() {
        return new ApiResponse<>(Status.LOADING, null, null, -1);
    }
    
    public Status getStatus() {
        return status;
    }
    
    public T getData() {
        return data;
    }
    
    public String getMessage() {
        return message;
    }
    
    public int getCode() {
        return code;
    }
    
    public boolean isSuccessful() {
        return status == Status.SUCCESS && data != null;
    }
    
    public boolean isError() {
        return status == Status.ERROR;
    }
    
    public boolean isLoading() {
        return status == Status.LOADING;
    }
}