package com.example.valuation_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Price {

    private final String assetId;
    private final double price;
    private String currency = "GBP";
}
