package com.example.valuation_service.service;

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


    public List<Account> calculateValuation(List<String> accountIds, String currencyCode) {
        List<Account> result = new ArrayList<>();

        List<AccountPosition> accountPositions = positionService.getPositions(accountIds);
        if (!CollectionUtils.isEmpty(accountPositions)) {
            List<String> assetIds = accountPositions.stream()
                    .flatMap(accountPosition -> accountPosition.getPosition().stream())
                    .map(Position::getAssetId)
                    .distinct()
                    .collect(Collectors.toList());

            List<Eligibility> eligibilityList = eligibilityService.getEligibility(accountIds, assetIds);
            Map<String, Price> priceMap = priceService.getPrices(assetIds).stream()
                    .collect(Collectors.toMap(Price::getAssetId, price -> price));

            Map<String, Double> fxRates = fxService.getFXRates().stream()
                    .collect(Collectors.toMap(FXRate::getCurrency, FXRate::getMultiplier));

            for (AccountPosition accountPosition : accountPositions) {
                double collateralValue = 0;
                double marketValue = 0;

                for (Position position : accountPosition.getPosition()) {
                    Price price = priceMap.get(position.getAssetId());
                    double discount = eligibilityList.stream()
                            .filter(eligibility -> eligibility.getAssetIDs().contains(position.getAssetId())
                                    && eligibility.getAccountIDs().contains(accountPosition.getAccountId()))
                            .mapToDouble(Eligibility::getDiscount)
                            .findAny().orElse(0);

                    if (price != null) {
                        double usdPrice = getUsdPrice(fxRates, price.getCurrency(), price.getPrice());
                        collateralValue += usdPrice * position.getQuantity() * discount;
                        marketValue += usdPrice * position.getQuantity();
                    }
                }

                result.add(new Account(accountPosition.getAccountId(),
                        formatValue(getValueBasedOnCurrency(fxRates, currencyCode, collateralValue)),
                        formatValue(getValueBasedOnCurrency(fxRates, currencyCode, marketValue))));
            }
        }
        return result;
    }

    /**
     * Converts the given value to USD based on the currency and FX rates.
     *
     * @param fxRates Map of currency to USD conversion rates
     * @param currency The currency code
     * @param value The value to be converted
     * @return The value in USD
     */

    private double getUsdPrice(Map<String, Double> fxRates, String currency, double value) {
        return fxRates.getOrDefault(currency, 0.0) * value;
    }

    /**
     * Converts the given USD amount to the specified currency based on FX rates.
     *
     * @param fxRates Map of currency to USD conversion rates
     * @param currency The target currency code
     * @param usdAmount The amount in USD
     * @return The value in the specified currency
     */
    private double getValueBasedOnCurrency(Map<String, Double> fxRates, String currency, double usdAmount) {
        Double rate = fxRates.get(currency);
        return (rate != null && rate != 0) ? usdAmount / rate : 0;
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
