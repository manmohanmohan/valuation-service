package com.example.valuation_service.service;

import com.example.valuation_service.exception.CurrencyNotFoundException;
import com.example.valuation_service.exception.FXRateNotFoundException;
import com.example.valuation_service.model.*;
import lombok.AllArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ValuationService {

    private final PositionService positionService;
    private final EligibilityService eligibilityService;
    private final PriceService priceService;
    private final FXService fxService;

    /**
     * Calculates the valuation for a list of accounts in the specified currency.
     *
     * @param accountIds   List of account IDs
     * @param currencyCode Target currency code
     * @return List of Account objects with calculated collateral and market values
     */
    public List<Account> calculateValuation(List<String> accountIds, String currencyCode) {
        List<Account> result = new ArrayList<>();

        List<AccountPosition> accountPositions = positionService.getPositions(accountIds);
        if (CollectionUtils.isEmpty(accountPositions)) {
            return result;
        }
        List<String> assetIds = extractAssetIds(accountPositions);

        List<Eligibility> eligibilityList = eligibilityService.getEligibility(accountIds, assetIds);
        Map<String, Price> priceMap = getPriceMap(assetIds);

        Map<String, Double> fxRates = getFxRatesMap();

        for (AccountPosition accountPosition : accountPositions) {
            double collateralValue = 0;
            double marketValue = 0;

            for (Position position : accountPosition.getPosition()) {
                Price price = priceMap.get(position.getAssetId());

                if (price != null) {
                    double discountFactor = getDiscountFactor(eligibilityList, accountPosition.getAccountId(),
                            position.getAssetId());
                    double usdPrice = getUsdPrice(fxRates, price.getCurrency(), price.getPrice());
                    collateralValue += usdPrice * position.getQuantity() * discountFactor;
                    marketValue += usdPrice * position.getQuantity();
                }
            }

            result.add(new Account(accountPosition.getAccountId(),
                    formatValue(getValueBasedOnCurrency(fxRates, currencyCode, collateralValue)),
                    formatValue(getValueBasedOnCurrency(fxRates, currencyCode, marketValue))));
        }
        return result;
    }

    private Map<String, Double> getFxRatesMap() {
        List<FXRate> fxRateList = fxService.getFXRates();

        if (fxRateList == null) {
            throw new FXRateNotFoundException("FX rates could not be retrieved from FX service.");
        }

        return fxRateList.stream()
                .collect(Collectors.toMap(FXRate::getCurrency, FXRate::getMultiplier));
    }

    private Map<String, Price> getPriceMap(List<String> assetIds) {
        Map<String, Price> priceMap = Optional.ofNullable(priceService.getPrices(assetIds))
                .orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(Price::getAssetId, price -> price));
        return priceMap;
    }

    private double getDiscountFactor(List<Eligibility> eligibilityList, String accountId, String assetId) {
        return eligibilityList.stream()
                .filter(eligibility -> eligibility.isEligible())
                .filter(eligibility -> eligibility.getAssetIDs().contains(assetId)
                        && eligibility.getAccountIDs().contains(accountId))
                .mapToDouble(Eligibility::getDiscount)
                .findAny().orElse(0);
    }

    private List<String> extractAssetIds(List<AccountPosition> accountPositions) {
        return accountPositions.stream()
                .flatMap(accountPosition -> accountPosition.getPosition().stream())
                .map(Position::getAssetId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Converts the given value to USD based on the currency and FX rates.
     *
     * @param fxRates  Map of currency to USD conversion rates
     * @param currency The currency code
     * @param value    The value to be converted
     * @return The value in USD
     */
    private double getUsdPrice(Map<String, Double> fxRates, String currency, double value) {
        Double rate = fxRates.get(currency);
        if (rate == null) {
            throw new CurrencyNotFoundException("Currency code '" + currency + "' not found in FX rates.");
        }
        return rate * value;
    }

    /**
     * Converts the given USD amount to the specified currency based on FX rates.
     *
     * @param fxRates   Map of currency to USD conversion rates
     * @param currency  The target currency code
     * @param usdAmount The amount in USD
     * @return The value in the specified currency
     */
    private double getValueBasedOnCurrency(Map<String, Double> fxRates, String currency, double usdAmount) {
        Double rate = fxRates.get(currency);
        if (rate == null || rate == 0) {
            throw new CurrencyNotFoundException("Currency code '" + currency + "' not found in FX rates or rate is zero.");
        }
        return usdAmount / rate;
    }

    /**
     * Formats the value to have a maximum of two decimal points.
     *
     * @param value The value to be formatted
     * @return The value formatted to two decimal points
     */
    private double formatValue(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
