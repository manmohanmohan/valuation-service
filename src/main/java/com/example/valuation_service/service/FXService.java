package com.example.valuation_service.service;

import com.example.valuation_service.model.FXRate;

import java.util.List;

public interface FXService {
    List<FXRate> getFXRates();
}
