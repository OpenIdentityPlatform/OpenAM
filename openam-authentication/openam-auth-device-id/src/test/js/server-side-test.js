/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS.
 */

describe("ComparisonResult test suite", function() {

    it("should create comparison result with zero penalty points", function() {
        // Given
        // When
        var comparisonResult = new ComparisonResult();
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should create comparison result with zero penalty points using constant", function() {
        // Given
        // When
        var comparisonResult = ComparisonResult.ZERO_PENALTY_POINTS;
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should create comparison result with penalty points and additional info", function() {
        // Given
        // When
        var comparisonResult = new ComparisonResult(11, true);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(11);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should create comparison result with penalty points", function() {
        // Given
        // When
        var comparisonResult = new ComparisonResult(111);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(111);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should create comparison result with additional info", function() {
        // Given
        // When
        var comparisonResult = new ComparisonResult(true);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should add comparison result", function() {
        // Given
        var comparisonResult = new ComparisonResult(111);
        var anotherComparisonResult = new ComparisonResult(321);
        // When
        comparisonResult.addComparisonResult(anotherComparisonResult);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(432);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should get comparison result successful", function() {
        // Given
        var comparisonResult = new ComparisonResult(0);
        // When
        var isSuccessful = comparisonResult.isSuccessful();
        //Then
        expect(isSuccessful).toBe(true);
    });

    it("should get comparison result unsuccessful", function() {
        // Given
        var comparisonResult = new ComparisonResult(1);
        // When
        var isSuccessful = comparisonResult.isSuccessful();
        //Then
        expect(isSuccessful).toBe(false);
    });

    it("should compare comparison results equally", function() {
        // Given
        var comparisonResult = new ComparisonResult(111);
        var anotherComparisonResult = new ComparisonResult(111);
        // When
        var result = ComparisonResult.compare(comparisonResult, anotherComparisonResult);
        //Then
        expect(result).toEqual(0);
    });

    it("should compare comparison results not equal penalty points", function() {
        // Given
        var comparisonResult = new ComparisonResult(110);
        var anotherComparisonResult = new ComparisonResult(111);
        // When
        var result = ComparisonResult.compare(comparisonResult, anotherComparisonResult);
        //Then
        expect(result).toEqual(-1);
    });

    it("should compare comparison results with equal penalty points but one with additional info", function() {
        // Given
        var comparisonResult = new ComparisonResult(111, true);
        var anotherComparisonResult = new ComparisonResult(111);
        // When
        var result = ComparisonResult.compare(comparisonResult, anotherComparisonResult);
        //Then
        expect(result).toEqual(1);
    });

    it("should compare comparison results with null", function() {
        // Given
        var comparisonResult = new ComparisonResult(111);
        var anotherComparisonResult = null;
        // When
        var result = ComparisonResult.compare(comparisonResult, anotherComparisonResult);
        //Then
        expect(result).toEqual(1);
    });

    it("should sort comparison results with descending penalty points", function() {
        // Given
        var comparisonResults = [
            new ComparisonResult(3), new ComparisonResult(1), new ComparisonResult(2), new ComparisonResult(4)
        ];
        // When
        comparisonResults.sort(ComparisonResult.compare);
        //Then
        expect(comparisonResults[0].penaltyPoints).toEqual(1);
        expect(comparisonResults[1].penaltyPoints).toEqual(2);
        expect(comparisonResults[2].penaltyPoints).toEqual(3);
        expect(comparisonResults[3].penaltyPoints).toEqual(4);
    });

});

describe("ScalarComparator test suite", function() {

    it("should compare with no penalty points", function () {
        // Given
        var currentValue = "CURRENT_VALUE";
        var storedValue = "STORED_VALUE";
        var config = {"penaltyPoints": 0};
        // When
        var comparisonResult = ScalarComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare when stored value is different to current value", function () {
        // Given
        var currentValue = "CURRENT_VALUE";
        var storedValue = "STORED_VALUE";
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScalarComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(10);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare when stored value is not null and current value is null", function () {
        // Given
        var currentValue = null;
        var storedValue = "STORED_VALUE";
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScalarComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(10);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare when stored value is null and current value is not null", function () {
        // Given
        var currentValue = "CURRENT_VALUE";
        var storedValue = null;
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScalarComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });
});

describe("ScreenComparator test suite", function() {
    it("should compare screens that are equal", function() {
        // Given
        var currentValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
        var storedValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare screens that are null", function() {
        // Given
        var currentValue = null;
        var storedValue = null;
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare screens with null values", function() {
        // Given
        var currentValue = {"screenWidth": null, "screenHeight": null, "screenColourDepth": null};
        var storedValue = {"screenWidth": null, "screenHeight": null, "screenColourDepth": null};
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare screens when stored screen width is null", function() {
        // Given
        var currentValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
        var storedValue = {"screenWidth": null, "screenHeight": 1200, "screenColourDepth": 24};
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare screens when stored screen width is different to current screen width", function() {
        // Given
        var currentValue = {"screenWidth": 800, "screenHeight": 1200, "screenColourDepth": 24};
        var storedValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(10);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare screens when stored screen height is null", function() {
        // Given
        var currentValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
        var storedValue = {"screenWidth": 1920, "screenHeight": null, "screenColourDepth": 24};
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare screens when stored screen height is different to current screen height", function() {
        // Given
        var currentValue = {"screenWidth": 1920, "screenHeight": 800, "screenColourDepth": 24};
        var storedValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(10);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare screens when stored screen colour depth is null", function() {
        // Given
        var currentValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
        var storedValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": null};
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare screens when stored screen colour depth is different to current screen colour depth", function() {
        // Given
        var currentValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 16};
        var storedValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
        var config = {"penaltyPoints": 10};
        // When
        var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.penaltyPoints).toEqual(10);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });
});

describe("MultiValueComparator test suite", function() {

    it("should compare multi value strings when stored value is null and current value is empty", function() {
        // Given
        var currentValue = "";
        var storedValue = null;
        var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
        // When
        var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare multi value strings when both are null", function() {
        // Given
        var currentValue = null;
        var storedValue = null;
        var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
        // When
        var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare multi value strings when both are empty", function() {
        // Given
        var currentValue = "";
        var storedValue = "";
        var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
        // When
        var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare multi value strings when current value is null", function() {
        // Given
        var currentValue = null;
        var storedValue = "VALUE_A; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
        // When
        var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(false);
        expect(comparisonResult.penaltyPoints).toEqual(111);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare multi value strings when stored value is null", function() {
        // Given
        var currentValue = "VALUE_A; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        var storedValue = null;
        var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
        // When
        var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare multi value strings when both are equal", function() {
        // Given
        var currentValue = "VALUE_A; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        var storedValue = "VALUE_A; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
        // When
        var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare multi value string when there are less differences than max", function() {
        // Given
        var currentValue = "VALUE_AA; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
        // When
        var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare multi value string when there are more differences than max", function() {
        // Given
        var currentValue = "VALUE_AA; VALUE_BB; VALUE_C; VALUE_D; VALUE_E";
        var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
        // When
        var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(false);
        expect(comparisonResult.penaltyPoints).toEqual(111);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare multi value string when there is less percentage diff than max", function() {
        // Given
        var currentValue = "VALUE_AA; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
        // When
        var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare multi value string when there is more percentage diff than max", function() {
        // Given
        var currentValue = "VALUE_AA; VALUE_BB; VALUE_C; VALUE_D; VALUE_E";
        var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
        var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
        // When
        var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(false);
        expect(comparisonResult.penaltyPoints).toEqual(111);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });
});

describe("UserAgentComparator test suite", function() {

    it("should compare user agents", function () {
        // Given
        var currentValue = "USER_AGENT_1234567890.";
        var storedValue = "1234USER_.567890AGENT_";
        var config = {"ignoreVersion": false, "penaltyPoints": 10};
        // When
        var comparisonResult = UserAgentComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(false);
        expect(comparisonResult.penaltyPoints).toEqual(10);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare user agents ignoring version numbers", function () {
        // Given
        var currentValue = "USER_AGENT_1234567890.";
        var storedValue = "1234USER_.567890AGENT_";
        var config = {"ignoreVersion": true, "penaltyPoints": 10};
        // When
        var comparisonResult = UserAgentComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare user agents when both are null", function () {
        // Given
        var currentValue = null;
        var storedValue = null;
        var config = {"ignoreVersion": true, "penaltyPoints": 10};
        // When
        var comparisonResult = UserAgentComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
    });

    it("should compare user agents when current value is null", function () {
        // Given
        var currentValue = null;
        var storedValue = "1234USER_.567890AGENT_";
        var config = {"ignoreVersion": true, "penaltyPoints": 10};
        // When
        var comparisonResult = UserAgentComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(false);
        expect(comparisonResult.penaltyPoints).toEqual(10);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare user agents when stored value is null", function () {
        // Given
        var currentValue = "USER_AGENT_1234567890.";
        var storedValue = null;
        var config = {"ignoreVersion": true, "penaltyPoints": 10};
        // When
        var comparisonResult = UserAgentComparator.compare(currentValue, storedValue, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

});

describe("GeolocationComparator test suite", function() {

    it("should compare location when both locations are null", function () {
        // Given
        var current = null;
        var stored = null;
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
    });

    it("should compare location when both latitudes are null", function () {
        // Given
        var current = {"latitude": null, "longitude": 2.0};
        var stored = {"latitude": null, "longitude": 2.0};
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
    });

    it("should compare location when both longitudes are null", function () {
        // Given
        var current = {"latitude": 2.0, "longitude": null};
        var stored = {"latitude": 2.0, "longitude": null};
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
    });

    it("should compare location when current location is null", function () {
        // Given
        var current = null;
        var stored = {"latitude": 2.0, "longitude": 2.0};
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(false);
        expect(comparisonResult.penaltyPoints).toEqual(111);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare location when current latitude is null", function () {
        // Given
        var current = {"latitude": null, "longitude": 2.0};
        var stored = {"latitude": 2.0, "longitude": 2.0};
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(false);
        expect(comparisonResult.penaltyPoints).toEqual(111);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare location when current longitude is null", function () {
        // Given
        var current = {"latitude": 2.0, "longitude": null};
        var stored = {"latitude": 2.0, "longitude": 2.0};
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(false);
        expect(comparisonResult.penaltyPoints).toEqual(111);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare location when stored location is null", function () {
        // Given
        var current = {"latitude": 2.0, "longitude": 2.0};
        var stored = null;
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare location when stored latitude is null", function () {
        // Given
        var current = {"latitude": 2.0, "longitude": 2.0};
        var stored = {"latitude": null, "longitude": 2.0};
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare location when stored longitude is null", function () {
        // Given
        var current = {"latitude": 2.0, "longitude": 2.0};
        var stored = {"latitude": 2.0, "longitude": null};
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.penaltyPoints).toEqual(0);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare locations that are equal", function () {
        // Given
        var current = {"latitude": 2.0, "longitude": 2.0};
        var stored = {"latitude": 2.0, "longitude": 2.0};
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });

    it("should compare locations that are within tolerable range", function () {
        // Given
        var current = {"latitude": 3.0, "longitude": 3.0};
        var stored = {"latitude": 2.0, "longitude": 2.0};
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(true);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(true);
    });

    it("should compare locations that are outside tolerable range", function () {
        // Given
        var current = {"latitude": 20.0, "longitude": 20.0};
        var stored = {"latitude": 2.0, "longitude": 2.0};
        var config =  {"allowedRange": 100, "penaltyPoints": 111};
        // When
        var comparisonResult = GeolocationComparator.compare(current, stored, config);
        //Then
        expect(comparisonResult.isSuccessful()).toBe(false);
        expect(comparisonResult.penaltyPoints).toEqual(111);
        expect(comparisonResult.additionalInfoInCurrentValue).toBe(false);
    });
});
