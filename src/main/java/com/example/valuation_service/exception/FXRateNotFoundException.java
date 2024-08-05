package com.example.valuation_service.exception;

public class FXRateNotFoundException extends RuntimeException{
    public FXRateNotFoundException(String message){
        super(message);
    }
}


