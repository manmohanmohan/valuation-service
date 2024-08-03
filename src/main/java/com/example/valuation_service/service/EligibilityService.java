package com.example.valuation_service.service;

import com.example.valuation_service.model.Eligibility;

import java.util.List;

public interface EligibilityService {
    List<Eligibility> getEligibility(List<String> accountIds, List<String> assetIds);
}
