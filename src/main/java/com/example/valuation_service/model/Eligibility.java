package com.example.valuation_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Eligibility {
    private boolean eligible;
    private List<String> assetIDs;
    private List<String> accountIDs;
    private double discount;
}

