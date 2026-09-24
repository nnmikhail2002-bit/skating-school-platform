package com.skating.platform.backend.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String message){
        super(message);
    }
}