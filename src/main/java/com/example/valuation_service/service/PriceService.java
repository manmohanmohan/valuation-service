package com.example.valuation_service.service;

import com.example.valuation_service.model.Price;

import java.util.List;

public interface PriceService {
    List<Price> getPrices(List<String> assetIds);
}
