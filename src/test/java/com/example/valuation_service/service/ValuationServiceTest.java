package com.example.valuation_service.service;


import com.example.valuation_service.model.*;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ValuationServiceTest {

    @Mock
    PositionService positionService;

    @Mock
    EligibilityService eligibilityService;

    @Mock
    PriceService priceService;

    @Mock
    FXService fxService;

    @InjectMocks
    ValuationService valuationService;

    private static List<FXRate> fxRates;
    private static Map<String, List<Position>> accountPositions;

    @BeforeAll
    public static void setUpOnce() {
        fxRates = List.of(
                new FXRate("GBP", 1.28),
                new FXRate("JPY", 0.0062),
                new FXRate("USD", 1),
                new FXRate("INR", 0.012),
                new FXRate("EUR", 1.10)
        );

        accountPositions = new HashMap<>();
        accountPositions.put("E1", getPositionA());
        accountPositions.put("E2", getPositionB());
        accountPositions.put("E3", getPositionC());
        accountPositions.put("E4", getPositionD());
    }

    @BeforeEach
    public void setUp() {
        when(fxService.getFXRates()).thenReturn(fxRates);
    }



    @Test
    @DisplayName("Empty Account List should return empty result")
    public void testCalculateValuationWithEmptyAccountList() {
        List<Account> result = valuationService.calculateValuation(Collections.emptyList(), "USD");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("No positions for the account should return empty result")
    public void testCalculateValuationWithNoPositions() {
        when(positionService.getPositions(anyList())).thenReturn(Collections.emptyList());
        List<Account> result = valuationService.calculateValuation(Collections.singletonList("E6"), "USD");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("With eligible and ineligible positions, single account, currency: USD")
    void testCalculateValuationWithMixedPositionsSingleAccountUSD() {
        List<String> accountIds = List.of("E1");
        List<String> assetIds = List.of("S1", "S3", "S4");
        List<Price> priceList = List.of(new Price("S1", 50.5), new Price("S3", 10.4), new Price("S4", 15.5, "USD"));
        List<String> eligibleAssets = List.of("S1", "S3");
        String currency = "USD";
        when(positionService.getPositions(accountIds)).thenReturn(getAccountPositions(accountIds));
        List<Eligibility> eligibilityList = new ArrayList<>();

        eligibilityList.add(new Eligibility(true, eligibleAssets, List.of("E1"), 0.9));
        eligibilityList.add(new Eligibility(false, List.of("S4"), List.of("E1"), 0));
        when(eligibilityService.getEligibility(accountIds, assetIds)).thenReturn(eligibilityList);
        when(priceService.getPrices(assetIds)).thenReturn(priceList);
        when(fxService.getFXRates()).thenReturn(getFxRates());

        List<Account> result = valuationService.calculateValuation(accountIds, currency);

        assertEquals(1, result.size());
        assertEquals("E1", result.get(0).getAccountId());
        assertEquals(7015.68, result.get(0).getCollateralValue());
        assertEquals(9345.2, result.get(0).getMarketValue());
    }

    @Test
    @DisplayName("With eligible and ineligible positions, single account, currency: INR")
    void testCalculateValuationWithMixedPositionsSingleAccountINR() {
        List<String> accountIds = List.of("E1");
        List<String> assetIds = List.of("S1", "S3", "S4");
        List<Price> priceList = List.of(new Price("S1", 50.5), new Price("S3", 10.4), new Price("S4", 15.5, "USD"));
        List<String> eligibleAssets = List.of("S1", "S3");
        String currency = "INR";
        when(positionService.getPositions(accountIds)).thenReturn(getAccountPositions(accountIds));
        List<Eligibility> eligibilityList = new ArrayList<>();

        eligibilityList.add(new Eligibility(true, eligibleAssets, List.of("E1"), 0.9));
        eligibilityList.add(new Eligibility(false, List.of("S4"), List.of("E1"), 0));
        when(eligibilityService.getEligibility(accountIds, assetIds)).thenReturn(eligibilityList);
        when(priceService.getPrices(assetIds)).thenReturn(priceList);
        when(fxService.getFXRates()).thenReturn(getFxRates());

        List<Account> result = valuationService.calculateValuation(accountIds, currency);

        assertEquals(1, result.size());
        assertEquals("E1", result.get(0).getAccountId());
        assertEquals(584640, result.get(0).getCollateralValue());//584640
        assertEquals(778766.67, result.get(0).getMarketValue());//778766.67
    }

    @Test
    @DisplayName("With eligible and ineligible positions, single account, currency: INR")
    void testCalculateValuationWithInvalidCurrency() {
        List<String> accountIds = List.of("E1");
        List<String> assetIds = List.of("S1", "S3", "S4");
        List<Price> priceList = List.of(new Price("S1", 50.5), new Price("S3", 10.4), new Price("S4", 15.5, "USD"));
        List<String> eligibleAssets = List.of("S1", "S3");
        String currency = "InvalidCurrency";
        when(positionService.getPositions(accountIds)).thenReturn(getAccountPositions(accountIds));
        List<Eligibility> eligibilityList = new ArrayList<>();

        eligibilityList.add(new Eligibility(true, eligibleAssets, List.of("E1"), 0.9));
        eligibilityList.add(new Eligibility(false, List.of("S4"), List.of("E1"), 0));
        when(eligibilityService.getEligibility(accountIds, assetIds)).thenReturn(eligibilityList);
        when(priceService.getPrices(assetIds)).thenReturn(priceList);
        when(fxService.getFXRates()).thenReturn(getFxRates());

        List<Account> result = valuationService.calculateValuation(accountIds, currency);

        assertEquals(1, result.size());
        assertEquals("E1", result.get(0).getAccountId());
        assertEquals(0, result.get(0).getCollateralValue());//584640
        assertEquals(0, result.get(0).getMarketValue());//778766.67
    }


    @Test
    @DisplayName("With eligible positions only, single account, currency: USD")
    void testCalculateValuationWithEligiblePositionsSingleAccountUSD() {
        List<String> accountIds = List.of("E3");
        List<String> assetIds = List.of("S7", "S8", "S9");
        List<Price> priceList = List.of(new Price("S7", 50.5), new Price("S8", 10.4), new Price("S9", 15.5, "USD"));
        List<String> eligibleAssets = List.of("S7", "S8", "S9");
        String currency = "USD";
        when(positionService.getPositions(accountIds)).thenReturn(getAccountPositions(accountIds));
        List<Eligibility> eligibilityList = new ArrayList<>();

        eligibilityList.add(new Eligibility(true, eligibleAssets, List.of("E3"), 0.9));
        when(eligibilityService.getEligibility(accountIds, assetIds)).thenReturn(eligibilityList);
        when(priceService.getPrices(assetIds)).thenReturn(priceList);
        when(fxService.getFXRates()).thenReturn(getFxRates());

        List<Account> result = valuationService.calculateValuation(accountIds, currency);

        assertEquals(1, result.size());
        assertEquals("E3", result.get(0).getAccountId());
        assertEquals(841.07, result.get(0).getCollateralValue());
        assertEquals(934.52, result.get(0).getMarketValue());
    }

    @Test
    @DisplayName("With ineligible positions only, single account, currency: USD")
    void testCalculateValuationWithIneligiblePositionsSingleAccountUSD() {
        List<String> accountIds = List.of("E4");
        List<String> assetIds = List.of("S10", "S11", "S12");
        List<Price> priceList = List.of(new Price("S10", 50.5), new Price("S11", 10.4), new Price("S12", 15.5, "USD"));
        List<String> inEligibleAssets = List.of("S10", "S11", "S12");
        String currency = "USD";
        when(positionService.getPositions(accountIds)).thenReturn(getAccountPositions(accountIds));
        List<Eligibility> eligibilityList = new ArrayList<>();

        eligibilityList.add(new Eligibility(false, inEligibleAssets, List.of("E4"), 0));
        when(eligibilityService.getEligibility(accountIds, assetIds)).thenReturn(eligibilityList);
        when(priceService.getPrices(assetIds)).thenReturn(priceList);
        when(fxService.getFXRates()).thenReturn(getFxRates());

        List<Account> result = valuationService.calculateValuation(accountIds, currency);

        assertEquals(1, result.size());
        assertEquals("E4", result.get(0).getAccountId());
        assertEquals(0, result.get(0).getCollateralValue());
        assertEquals(934.52, result.get(0).getMarketValue());
    }

    @Test
    @DisplayName("With eligible and ineligible positions, multiple accounts, currency: USD")
    void testCalculateValuationWithMixedPositionsMultipleAccountsUSD() {
        List<String> accountIds = List.of("E1", "E2");
        List<String> assetIds = List.of("S1", "S3", "S4", "S2", "S5");
        List<Price> priceList = List.of(
                new Price("S1", 50.5),
                new Price("S3", 10.4),
                new Price("S4", 15.5, "USD"),
                new Price("S2", 20.2, "JPY"),
                new Price("S5", 15.5, "EUR")
        );
        List<String> eligibleAssets = List.of("S1", "S3", "S2");
        List<String> inEligibleAssets = List.of("S4", "S5");
        String currency = "USD";
        when(positionService.getPositions(accountIds)).thenReturn(getAccountPositions(accountIds));
        List<Eligibility> eligibilityList = new ArrayList<>();

        eligibilityList.add(new Eligibility(true, eligibleAssets, accountIds, 0.9));
        eligibilityList.add(new Eligibility(false, inEligibleAssets, accountIds, 0));
        when(eligibilityService.getEligibility(accountIds, assetIds)).thenReturn(eligibilityList);
        when(priceService.getPrices(assetIds)).thenReturn(priceList);
        when(fxService.getFXRates()).thenReturn(getFxRates());

        List<Account> result = valuationService.calculateValuation(accountIds, currency);

        assertEquals(2, result.size());
        assertEquals("E1", result.get(0).getAccountId());
        assertEquals(7015.68, result.get(0).getCollateralValue());
        assertEquals(9345.2, result.get(0).getMarketValue());
        assertEquals("E2", result.get(1).getAccountId());
        assertEquals(22.54, result.get(1).getCollateralValue());
        assertEquals(1730.05, result.get(1).getMarketValue());
    }

    @Test
    @DisplayName("No prices for assets should return zero valuation")
    void testCalculateValuationWithNoPrices() {
        List<String> accountIds = List.of("E1", "E2");
        List<String> assetIds = List.of("S1", "S3", "S4", "S2", "S5");
        List<String> eligibleAssets = List.of("S1", "S3", "S2");
        List<String> inEligibleAssets = List.of("S4", "S5");
        String currency = "USD";
        when(positionService.getPositions(accountIds)).thenReturn(getAccountPositions(accountIds));
        List<Eligibility> eligibilityList = new ArrayList<>();

        eligibilityList.add(new Eligibility(true, eligibleAssets, accountIds, 0.9));
        eligibilityList.add(new Eligibility(false, inEligibleAssets, accountIds, 0));
        when(eligibilityService.getEligibility(accountIds, assetIds)).thenReturn(eligibilityList);
        when(priceService.getPrices(assetIds)).thenReturn(Collections.emptyList());
        when(fxService.getFXRates()).thenReturn(getFxRates());

        List<Account> result = valuationService.calculateValuation(accountIds, currency);

        assertEquals(2, result.size());
        assertEquals("E1", result.get(0).getAccountId());
        assertEquals(0, result.get(0).getCollateralValue());
        assertEquals(0, result.get(0).getMarketValue());
        assertEquals("E2", result.get(1).getAccountId());
        assertEquals(0, result.get(1).getCollateralValue());
        assertEquals(0, result.get(1).getMarketValue());
    }


    private List<AccountPosition> getAccountPositions(List<String> accountIds) {
        List<AccountPosition> accountPositionList = new ArrayList<>();
        accountIds.forEach(accountId -> accountPositionList.add(
                new AccountPosition(accountId, accountPositions.getOrDefault(accountId, Collections.emptyList()))
        ));
        return accountPositionList;
    }

    private static List<Position> getPositionA() {
        List<Position> positionA = new ArrayList<>();
        positionA.add(Position.builder().assetId("S1").quantity(100).build());
        positionA.add(Position.builder().assetId("S3").quantity(100).build());
        positionA.add(Position.builder().assetId("S4").quantity(100).build());
        return positionA;
    }

    private static List<Position> getPositionB() {
        List<Position> positionB = new ArrayList<>();
        positionB.add(Position.builder().assetId("S2").quantity(200).build());
        positionB.add(Position.builder().assetId("S5").quantity(100).build());
    //    positionB.add(Position.builder().assetId("S6").quantity(300).build());
        return positionB;
    }

    private static List<Position> getPositionC() {
        List<Position> positionA = new ArrayList<>();
        positionA.add(Position.builder().assetId("S7").quantity(10).build());
        positionA.add(Position.builder().assetId("S8").quantity(10).build());
        positionA.add(Position.builder().assetId("S9").quantity(10).build());
        return positionA;
    }

    private static List<Position> getPositionD() {
        List<Position> positionA = new ArrayList<>();
        positionA.add(Position.builder().assetId("S10").quantity(10).build());
        positionA.add(Position.builder().assetId("S11").quantity(10).build());
        positionA.add(Position.builder().assetId("S12").quantity(10).build());
        return positionA;
    }


    private  List<FXRate> getFxRates(){
        return List.of(new FXRate("GBP",1.28),
                new FXRate("JPY",0.0062),
                new FXRate("USD",1),
                new FXRate("INR", 0.012),
                new FXRate("EUR",1.10)
                );
    }


}
