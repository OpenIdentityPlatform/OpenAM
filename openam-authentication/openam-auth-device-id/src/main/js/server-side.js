/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
/*
 * Portions Copyrighted 2013 Syntegrity.
 * Portions Copyrighted 2013-2015 ForgeRock AS.
 */

var ScalarComparator = {}, ScreenComparator = {}, MultiValueComparator = {}, UserAgentComparator = {}, GeolocationComparator = {};

var config = {
    profileExpiration: 30,              //in days
    maxProfilesAllowed: 5,
    maxPenaltyPoints: 0,
    attributes: {
        screen: {
            required: true,
            comparator: ScreenComparator,
            args: {
                penaltyPoints: 50
            }
        },
        plugins: {
            installedPlugins: {
                required: false,
                comparator: MultiValueComparator,
                args: {
                    maxPercentageDifference: 10,
                    maxDifferences: 5,
                    penaltyPoints: 100
                }
            }
        },
        fonts: {
            installedFonts: {
                required: false,
                comparator: MultiValueComparator,
                args: {
                    maxPercentageDifference: 10,
                    maxDifferences: 5,
                    penaltyPoints: 100
                }
            }
        },
        timezone: {
            timezone: {
                required: false,
                comparator: ScalarComparator,
                args: {
                    penaltyPoints: 100
                }
            }
        },
        userAgent: {
            required: true,
            comparator: UserAgentComparator,
            args: {
                ignoreVersion: true,
                penaltyPoints: 100
            }
        },
        geolocation: {
            required: false,
            comparator: GeolocationComparator,
            args: {
                allowedRange: 100,			//in miles
                penaltyPoints: 100
            }
        }
    }
};

//---------------------------------------------------------------------------//
//                           Comparator functions                            //
//---------------------------------------------------------------------------//

var all, any, calculateDistance, calculateIntersection, calculatePercentage, nullOrUndefined, splitAndTrim,
    undefinedLocation;

// ComparisonResult

/**
 * Constructs an instance of a ComparisonResult with the given penalty points.
 *
 * @param penaltyPoints (Number) The penalty points for the comparison (defaults to 0).
 * @param additionalInfoInCurrentValue (boolean) Whether the current value contains more information
 *                                               than the stored value (defaults to false).
 */
function ComparisonResult() {

    var penaltyPoints = 0,
        additionalInfoInCurrentValue = false;

    if (arguments[0] !== undefined && arguments[1] !== undefined) {
        penaltyPoints = arguments[0];
        additionalInfoInCurrentValue = arguments[1];
    }

    if (arguments[0] !== undefined && arguments[1] === undefined) {
        if (typeof(arguments[0]) === "boolean") {
            additionalInfoInCurrentValue = arguments[0];
        } else {
            penaltyPoints = arguments[0];
        }
    }

    this.penaltyPoints = penaltyPoints;
    this.additionalInfoInCurrentValue = additionalInfoInCurrentValue;

}

ComparisonResult.ZERO_PENALTY_POINTS = new ComparisonResult(0);

/**
 * Static method for functional programming.
 *
 * @return boolean true if comparisonResult.isSuccessful().
 */
ComparisonResult.isSuccessful =  function(comparisonResult) {
    return comparisonResult.isSuccessful();
};


/**
 * Static method for functional programming.
 *
 * @return boolean true if comparisonResult.additionalInfoInCurrentValue.
 */
ComparisonResult.additionalInfoInCurrentValue =  function(comparisonResult) {
    return comparisonResult.additionalInfoInCurrentValue;
};

/**
 * Comparison function that can be provided as an argument to array.sort
 */
ComparisonResult.compare = function(first, second) {
    if (nullOrUndefined(first) && nullOrUndefined(second)) {
        return 0;
    } else if (nullOrUndefined(first)) {
        return -1;
    } else if (nullOrUndefined(second)) {
        return 1;
    } else {
        if (first.penaltyPoints !== second.penaltyPoints) {
            return first.penaltyPoints - second.penaltyPoints;
        } else {
            return (first.additionalInfoInCurrentValue ? 1 : 0) - (second.additionalInfoInCurrentValue ? 1 : 0);
        }
    }
};

/**
 * Amalgamates the given ComparisonResult into this ComparisonResult.
 *
 * @param comparisonResult The ComparisonResult to include.
 */
ComparisonResult.prototype.addComparisonResult = function(comparisonResult) {
    this.penaltyPoints += comparisonResult.penaltyPoints;
    if (comparisonResult.additionalInfoInCurrentValue) {
        this.additionalInfoInCurrentValue = comparisonResult.additionalInfoInCurrentValue;
    }
};

/**
 * Returns true if no penalty points have been assigned for the comparison.
 *
 * @return boolean true if the comparison was successful.
 */
ComparisonResult.prototype.isSuccessful = function() {
    return nullOrUndefined(this.penaltyPoints) || this.penaltyPoints === 0;
};

/**
 * Compares two simple objects (String|Number) and if they are equal then returns a ComparisonResult with zero
 * penalty points assigned, otherwise returns a ComparisonResult with the given number of penalty points assigned.
 *
 * @param currentValue (String|Number) The current value.
 * @param storedValue (String|Number) The stored value.
 * @param config: {
 *            "penaltyPoints": (Number) The number of penalty points.
 *        }
 * @return ComparisonResult.
 */
ScalarComparator.compare = function (currentValue, storedValue, config) {
    if (logger.messageEnabled()) {
        logger.message("StringComparator.compare:currentValue: " + JSON.stringify(currentValue));
        logger.message("StringComparator.compare:storedValue: " + JSON.stringify(storedValue));
        logger.message("StringComparator.compare:config: " + JSON.stringify(config));
    }
    if (config.penaltyPoints === 0) {
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (!nullOrUndefined(storedValue)) {
        if (nullOrUndefined(currentValue) || currentValue !== storedValue) {
            return new ComparisonResult(config.penaltyPoints);
        }
    } else if (!nullOrUndefined(currentValue)) {
        return new ComparisonResult(true);
    }

    return ComparisonResult.ZERO_PENALTY_POINTS;
};

/**
 * Compares two screens and if they are equal then returns a ComparisonResult with zero penalty points assigned,
 * otherwise returns a ComparisonResult with the given number of penalty points assigned.
 *
 * @param currentValue: {
 *            "screenWidth": (Number) The current client screen width.
 *            "screenHeight": (Number) The current client screen height.
 *            "screenColourDepth": (Number) The current client screen colour depth.
 *        }
 * @param storedValue: {
 *            "screenWidth": (Number) The stored client screen width.
 *            "screenHeight": (Number) The stored client screen height.
 *            "screenColourDepth": (Number) The stored client screen colour depth.
 *        }
 * @param config: {
 *            "penaltyPoints": (Number) The number of penalty points.
 *        }
 * @return ComparisonResult
 */
ScreenComparator.compare = function (currentValue, storedValue, config) {
    if (logger.messageEnabled()) {
        logger.message("ScreenComparator.compare:currentValue: " + JSON.stringify(currentValue));
        logger.message("ScreenComparator.compare:storedValue: " + JSON.stringify(storedValue));
        logger.message("ScreenComparator.compare:config: " + JSON.stringify(config));
    }

    if (nullOrUndefined(currentValue)) {
        currentValue = {screenWidth: null, screenHeight: null, screenColourDepth: null};
    }
    if (nullOrUndefined(storedValue)) {
        storedValue = {screenWidth: null, screenHeight: null, screenColourDepth: null};
    }

    var comparisonResults = [
        ScalarComparator.compare(currentValue.screenWidth, storedValue.screenWidth, config),
        ScalarComparator.compare(currentValue.screenHeight, storedValue.screenHeight, config),
        ScalarComparator.compare(currentValue.screenColourDepth, storedValue.screenColourDepth, config)];

    if (all(comparisonResults, ComparisonResult.isSuccessful)) {
        return new ComparisonResult(any(comparisonResults, ComparisonResult.additionalInfoInCurrentValue));
    } else {
        return new ComparisonResult(config.penaltyPoints);
    }
};

/**
 * Splits both values using delimiter, trims every value and compares collections of values.
 * Returns zero-result for same multi-value attributes.
 *
 * If collections are not same checks if number of differences is less or equal maxDifferences or
 * percentage of difference is less or equal maxPercentageDifference.
 *
 * If yes then returns zero-result with additional info, else returns penaltyPoints-result.
 *
 * @param currentValue: (String) The current value.
 * @param storedValue: (String) The stored value.
 * @param config: {
 *            "maxPercentageDifference": (Number) The max difference percentage in the values,
 *                                                before the penalty is assigned.
 *            "maxDifferences": (Number) The max number of differences in the values,
 *                                       before the penalty points are assigned.
 *            "penaltyPoints": (Number) The number of penalty points.
  *        }
 * @return ComparisonResult
 */
MultiValueComparator.compare = function (currentValue, storedValue, config) {
    if (logger.messageEnabled()) {
        logger.message("MultiValueComparator.compare:currentValue: " + JSON.stringify(currentValue));
        logger.message("MultiValueComparator.compare:storedValue: " + JSON.stringify(storedValue));
        logger.message("MultiValueComparator.compare:config: " + JSON.stringify(config));
    }

    var delimiter = ";",
        currentValues = splitAndTrim(currentValue, delimiter),
        storedValues = splitAndTrim(storedValue, delimiter),
        maxNumberOfElements = Math.max(currentValues.length, storedValues.length),
        numberOfTheSameElements = calculateIntersection(currentValues, storedValues).length,
        numberOfDifferences = maxNumberOfElements - numberOfTheSameElements,
        percentageOfDifferences = calculatePercentage(numberOfDifferences, maxNumberOfElements);

    if (nullOrUndefined(storedValue) && !nullOrUndefined(currentValue)) {
        return new ComparisonResult(true);
    }

    if (logger.messageEnabled()) {
        logger.message(numberOfTheSameElements + " of " + maxNumberOfElements + " are same");
    }

    if (maxNumberOfElements === 0) {
        logger.message("Ignored because no attributes found in both profiles");
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (numberOfTheSameElements === maxNumberOfElements) {
        logger.message("Ignored because all attributes are same");
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (numberOfDifferences > config.maxDifferences) {
        if (logger.messageEnabled()) {
            logger.message("Would be ignored if not more than " + config.maxDifferences + " differences");
        }
        return new ComparisonResult(config.penaltyPoints);
    }

    if (percentageOfDifferences > config.maxPercentageDifference) {
        if (logger.messageEnabled()) {
            logger.message(percentageOfDifferences + " percents are different");
            logger.message("Would be ignored if not more than " + config.maxPercentageDifference + " percent");
        }
        return new ComparisonResult(config.penaltyPoints);
    }

    if (logger.messageEnabled()) {
        logger.message("Ignored because number of differences(" + numberOfDifferences + ") not more than "
            + config.maxDifferences);
        logger.message(percentageOfDifferences + " percents are different");
        logger.message("Ignored because not more than " + config.maxPercentageDifference + " percent");
    }
    return new ComparisonResult(true);
};

/**
 * Compares two User Agent Strings and if they are equal then returns a ComparisonResult with zero penalty
 * points assigned, otherwise returns a ComparisonResult with the given number of penalty points assigned.
 *
 * @param currentValue (String) The current value.
 * @param storedValue (String) The stored value.
 * @param config: {
 *            "ignoreVersion": (boolean) If the version numbers in the User Agent Strings should be ignore
 *                                       in the comparison.
 *            "penaltyPoints": (Number) The number of penalty points.
 *        }
 * @return A ComparisonResult.
 */
UserAgentComparator.compare = function (currentValue, storedValue, config) {
    if (logger.messageEnabled()) {
        logger.message("UserAgentComparator.compare:currentValue: " + JSON.stringify(currentValue));
        logger.message("UserAgentComparator.compare:storedValue: " + JSON.stringify(storedValue));
        logger.message("UserAgentComparator.compare:config: " + JSON.stringify(config));
    }

    if (config.ignoreVersion) {
        // remove version number
        currentValue = nullOrUndefined(currentValue) ? null : currentValue.replace(/[\d\.]+/g, "").trim();
        storedValue = nullOrUndefined(storedValue) ? null : storedValue.replace(/[\d\.]+/g, "").trim();
    }

    return ScalarComparator.compare(currentValue, storedValue, config);
};

/**
 * Compares two locations, taking into account a degree of difference.
 *
 * @param currentValue: {
 *            "latitude": (Number) The current latitude.
 *            "longitude": (Number) The current longitude.
 *        }
 * @param storedValue: {
 *            "latitude": (Number) The stored latitude.
 *            "longitude": (Number) The stored longitude.
 *        }
 * @param config: {
 *            "allowedRange": (Number) The max difference allowed in the two locations, before the penalty is assigned.
 *            "penaltyPoints": (Number) The number of penalty points.
*         }
 * @return ComparisonResult
 */
GeolocationComparator.compare = function (currentValue, storedValue, config) {
    if (logger.messageEnabled()) {
        logger.message("GeolocationComparator.compare:currentValue: " + JSON.stringify(currentValue));
        logger.message("GeolocationComparator.compare:storedValue: " + JSON.stringify(storedValue));
        logger.message("GeolocationComparator.compare:config: " + JSON.stringify(config));
    }

    // Check for undefined stored or current locations

    if (undefinedLocation(currentValue) && undefinedLocation(storedValue)) {
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }
    if (undefinedLocation(currentValue) && !undefinedLocation(storedValue)) {
        return new ComparisonResult(config.penaltyPoints);
    }
    if (!undefinedLocation(currentValue) && undefinedLocation(storedValue)) {
        return new ComparisonResult(true);
    }

    // Both locations defined, therefore perform comparison

    var distance = calculateDistance(currentValue, storedValue);

    if (logger.messageEnabled()) {
        logger.message("Distance between (" + currentValue.latitude + "," + currentValue.longitude + ") and (" +
            storedValue.latitude + "," + storedValue.longitude + ") is " + distance + " miles");
    }

    if (parseFloat(distance.toPrecision(5)) === 0) {
        logger.message("Location is the same");
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (distance <= config.allowedRange) {
        if (logger.messageEnabled()) {
            logger.message("Tolerated because distance not more then " + config.allowedRange);
        }
        return new ComparisonResult(true);
    } else {
        if (logger.messageEnabled()) {
            logger.message("Would be ignored if distance not more then " + config.allowedRange);
        }
        return new ComparisonResult(config.penaltyPoints);
    }
};


//---------------------------------------------------------------------------//
//                    Device Print Logic - DO NOT MODIFY                     //
//---------------------------------------------------------------------------//

// Utility functions

/**
 * Returns true if evaluating function f on each element of the Array a returns true.
 *
 * @param a: (Array) The array of elements to evaluate
 * @param f: (Function) A single argument function for mapping elements of the array to boolean.
 * @return boolean.
 */
all = function(a, f) {
    var i;
    for (i = 0; i < a.length; i++) {
        if (f(a[i]) === false) {
            return false;
        }
    }
    return true;
};

/**
 * Returns true if evaluating function f on any element of the Array a returns true.
 *
 * @param a: (Array) The array of elements to evaluate
 * @param f: (Function) A single argument function for mapping elements of the array to boolean.
 * @return boolean.
 */
any = function(a, f) {
    var i;
    for (i = 0; i < a.length; i++) {
        if (f(a[i]) === true) {
            return true;
        }
    }
    return false;
};

/**
 * Returns true if the provided location is null or has undefined longitude or latitude values.
 *
 * @param location: {
 *            "latitude": (Number) The latitude.
 *            "longitude": (Number) The longitude.
 *        }
 * @return boolean
 */
undefinedLocation = function(location) {
    return nullOrUndefined(location) || nullOrUndefined(location.latitude) || nullOrUndefined(location.longitude);
};

/**
 * Returns true if the provided value is null or undefined.
 *
 * @param value: a value of any type
 * @return boolean
 */
nullOrUndefined = function(value) {
    return value === null || value === undefined;
};

/**
 * Calculates the distances between the two locations.
 *
 * @param first: {
 *            "latitude": (Number) The first latitude.
 *            "longitude": (Number) The first longitude.
 *        }
 * @param second: {
 *            "latitude": (Number) The second latitude.
 *            "longitude": (Number) The second longitude.
 *        }
 * @return Number The distance between the two locations.
 */
calculateDistance = function(first, second) {
    var factor = (Math.PI / 180),
        theta,
        dist;
    function degreesToRadians(degrees) {
        return degrees * factor;
    }
    function radiansToDegrees(radians) {
        return radians / factor;
    }
    theta = first.longitude - second.longitude;
    dist = Math.sin(degreesToRadians(first.latitude)) * Math.sin(degreesToRadians(second.latitude))
        + Math.cos(degreesToRadians(first.latitude)) * Math.cos(degreesToRadians(second.latitude))
        * Math.cos(degreesToRadians(theta));
    dist = Math.acos(dist);
    dist = radiansToDegrees(dist);
    dist = dist * 60 * 1.1515;
    return dist;
};

/**
 * Converts a String holding a delimited sequence of values into an array.
 *
 * @param text (String) The String representation of a delimited sequence of values.
 * @param delimiter (String) The character delimiting values within the text String.
 * @return (Array) The comma separated values.
 */
splitAndTrim = function(text, delimiter) {

    var results = [],
        i,
        values,
        value;
    if (text === null) {
        return results;
    }

    values = text.split(delimiter);
    for (i = 0; i < values.length; i++) {
        value = values[i].trim();
        if (value !== "") {
            results.push(value);
        }
    }

    return results;
};

/**
 * Converts value to a percentage of range.
 *
 * @param value (Number) The actual number to be converted to a percentage.
 * @param range (Number) The total number of values (i.e. represents 100%).
 * @return (Number) The percentage.
 */
calculatePercentage = function(value, range) {
    if (range === 0) {
        return 0;
    }
    return parseFloat((value / range).toPrecision(2)) * 100;
};

/**
 * Creates a new array containing only those elements found in both arrays received as arguments.
 *
 * @param first (Array) The first array.
 * @param second (Array) The second array.
 * @return (Array) The elements that found in first and second.
 */
calculateIntersection = function(first, second) {
    return first.filter(function(element) {
        return second.indexOf(element) !== -1;
    });
};

function getValue(obj, attributePath) {
    var value = obj,
        i;
    for (i = 0; i < attributePath.length; i++) {
        if (value === undefined) {
            return null;
        }
        value = value[attributePath[i]];
    }
    return value;
}


function isLeafNode(attributeConfig) {
    return attributeConfig.comparator !== undefined;
}

function getAttributePaths(attributeConfig, attributePath) {

    var attributePaths = [],
        attributeName,
        attrPaths,
        attrPath,
        i;

    for (attributeName in attributeConfig) {
        if (attributeConfig.hasOwnProperty(attributeName)) {

            if (isLeafNode(attributeConfig[attributeName])) {
                attrPath = attributePath.slice();
                attrPath.push(attributeName);
                attributePaths.push(attrPath);
            } else {
                attrPath = attributePath.slice();
                attrPath.push(attributeName);
                attrPaths = getAttributePaths(attributeConfig[attributeName], attrPath);
                for (i = 0; i < attrPaths.length; i++) {
                    attributePaths.push(attrPaths[i]);
                }
            }
        }
    }

    return attributePaths;
}

function getDevicePrintAttributePaths(attributeConfig) {
    return getAttributePaths(attributeConfig, []);
}

function hasRequiredAttributes(devicePrint, attributeConfig) {

    var attributePaths = getDevicePrintAttributePaths(attributeConfig),
        i,
        attrValue,
        attrConfig;

    for (i = 0; i < attributePaths.length; i++) {

        attrValue = getValue(devicePrint, attributePaths[i]);
        attrConfig = getValue(attributeConfig, attributePaths[i]);

        if (attrConfig.required && attrValue === undefined) {
            logger.warning("Device Print profile missing required attribute, " + attributePaths[i]);
            return false;
        }
    }

    logger.message("device print has required attributes");
    return true;
}

function compareDevicePrintProfiles(attributeConfig, devicePrint, devicePrintProfiles, maxPenaltyPoints) {

    var attributePaths = getDevicePrintAttributePaths(attributeConfig),
        results,
        j,
        aggregatedComparisonResult,
        i,
        currentValue,
        storedValue,
        attrConfig,
        comparisonResult,
        selectedComparisonResult,
        selectedProfile,
        vals;

    results = [];
    for (j = 0; j < devicePrintProfiles.length; j++) {

        aggregatedComparisonResult = new ComparisonResult();
        for (i = 0; i < attributePaths.length; i++) {

            currentValue = getValue(devicePrint, attributePaths[i]);
            storedValue = getValue(devicePrintProfiles[j].devicePrint, attributePaths[i]);
            attrConfig = getValue(attributeConfig, attributePaths[i]);

            if (storedValue === null) {
                comparisonResult = new ComparisonResult(attrConfig.penaltyPoints);
            } else {
                comparisonResult = attrConfig.comparator.compare(currentValue, storedValue, attrConfig.args);
            }

            if (logger.messageEnabled()) {
                logger.message("Comparing attribute path: " + attributePaths[i]
                    + ", Comparison result: successful=" + comparisonResult.isSuccessful() + ", penaltyPoints="
                    + comparisonResult.penaltyPoints + ", additionalInfoInCurrentValue="
                    + comparisonResult.additionalInfoInCurrentValue);
            }
            aggregatedComparisonResult.addComparisonResult(comparisonResult);
        }
        if (logger.messageEnabled()) {
            logger.message("Aggregated comparison result: successful="
                + aggregatedComparisonResult.isSuccessful() + ", penaltyPoints="
                + aggregatedComparisonResult.penaltyPoints + ", additionalInfoInCurrentValue="
                + aggregatedComparisonResult.additionalInfoInCurrentValue);
        }

        results.push({
            key: aggregatedComparisonResult,
            value: devicePrintProfiles[j]
        });
    }

    if (results.length === 0) {
        return null;
    }

    results.sort(function(a, b) {
        return ComparisonResult.compare(a.key, b.key);
    });
    selectedComparisonResult = results[0].key;
    if (logger.messageEnabled()) {
        logger.message("Selected comparison result: successful=" + selectedComparisonResult.isSuccessful()
            + ", penaltyPoints=" + selectedComparisonResult.penaltyPoints + ", additionalInfoInCurrentValue="
            + selectedComparisonResult.additionalInfoInCurrentValue);
    }

    selectedProfile = null;
    if (selectedComparisonResult.penaltyPoints <= maxPenaltyPoints) {
        selectedProfile = results[0].value;
        if (logger.messageEnabled()) {
            logger.message("Selected profile: " + JSON.stringify(selectedProfile) +
                " with " + selectedComparisonResult.penaltyPoints + " penalty points");
        }
    }

    if (selectedProfile === null) {
        return false;
    }

    /* update profile */
    selectedProfile.selectionCounter = selectedProfile.selectionCounter + 1;
    selectedProfile.lastSelectedDate = new Date().getTime();
    selectedProfile.devicePrint = devicePrint;

    vals = [];
    for (i = 0; i < devicePrintProfiles.length; i++) {
        vals.push(JSON.stringify(devicePrintProfiles[i]));
    }

    idRepository.setAttribute(username, "devicePrintProfiles", vals);

    return true;
}

function matchDevicePrint() {

    if (!username) {
        logger.error("Username not set. Cannot compare user's device print profiles.");
        authState = FAILED;
    } else {

        if (logger.messageEnabled()) {
            logger.message("client devicePrint: " + clientScriptOutputData);
        }

        var getProfiles = function () {

                function isExpiredProfile(devicePrintProfile) {
                    var expirationDate = new Date(),
                        lastSelectedDate;
                    expirationDate.setDate(expirationDate.getDate() - config.profileExpiration);

                    lastSelectedDate = new Date(devicePrintProfile.lastSelectedDate);

                    return lastSelectedDate < expirationDate;
                }

                function getNotExpiredProfiles() {
                    var profile,
                        results = [],
                        profiles = idRepository.getAttribute(username, "devicePrintProfiles"),
                        iter;
                
                    if (profiles) {
                        iter = profiles.iterator();
                        
                        while (iter.hasNext()) {
                            profile = JSON.parse(iter.next());
                            if (!isExpiredProfile(profile)) {
                                results.push(profile);
                            }
                        }
                    }
                    if (logger.messageEnabled()) {
                        logger.message("stored non-expired profiles: " + JSON.stringify(results));
                    }                    
                    return results;
                }

                return getNotExpiredProfiles();
            },
            devicePrint = JSON.parse(clientScriptOutputData),
            devicePrintProfiles = getProfiles();

        if (!hasRequiredAttributes(devicePrint, config.attributes)) {
            logger.message("devicePrint.hasRequiredAttributes: false");
            // Will fail this module but fall-through to next module. Which should be OTP.
            authState = FAILED;
        } else if (compareDevicePrintProfiles(config.attributes, devicePrint, devicePrintProfiles, config.maxPenaltyPoints)) {
            logger.message("devicePrint.hasValidProfile: true");
            authState = SUCCESS;
        } else {
            logger.message("devicePrint.hasValidProfile: false");
            sharedState.put('devicePrintProfile', JSON.stringify(devicePrint));
            // Will fail this module but fall-through to next module. Which should be OTP.
            authState = FAILED;
        }
    }
}

matchDevicePrint();
