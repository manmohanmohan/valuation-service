package com.example.valuation_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Account {
    private String accountId;
    private double collateralValue;
    private double marketValue;
}
