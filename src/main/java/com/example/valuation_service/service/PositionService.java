package com.example.valuation_service.service;

import com.example.valuation_service.model.AccountPosition;


import java.util.List;

public interface PositionService {
    List<AccountPosition> getPositions(List<String> accountIds);
}
