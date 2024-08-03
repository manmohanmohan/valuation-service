package com.example.valuation_service.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Position {
    private String assetId;
    private int quantity;
}
