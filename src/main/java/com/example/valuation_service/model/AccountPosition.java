package com.example.valuation_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AccountPosition {
    private String accountId;
    private List<Position> position;
}
