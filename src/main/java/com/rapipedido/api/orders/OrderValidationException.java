package com.rapipedido.api.orders;

public class OrderValidationException extends RuntimeException {
    private final String errorCode;

    public OrderValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
