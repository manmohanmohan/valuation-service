package com.example.valuation_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FXRate {
    private String currency;
    private double multiplier;
}

