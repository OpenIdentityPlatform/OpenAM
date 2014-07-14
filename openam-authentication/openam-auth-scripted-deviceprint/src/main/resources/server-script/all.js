/**
 * Created by craig on 07/07/2014.
 */
// ============================================================================================================
// TDD framework

getTestDetails = function() {
    // http://stackoverflow.com/questions/1340872/how-to-get-javascript-caller-function-line-number-how-to-get-javascript-caller
    function getErrorObject() {
        try { throw Error('') } catch(err) { return err; }
    }
    var err = getErrorObject();
    var caller_line = err.stack.split("\n")[6];
    // e.g. at shouldCreateComparisonResultWithZeroPenaltyPoints (eval at <anonymous> (eval at <anonymous> (http://repl.it/jsrepl/sandbox.js:39:104)), <anonymous>:247:73)
    var index = caller_line.indexOf("at ");
    var callee_function = caller_line.slice(caller_line.indexOf("at ")+2, caller_line.indexOf("(eval"));
    var callee_line_number = caller_line.slice(caller_line.indexOf("<anonymous>:")+12, caller_line.indexOf(":", caller_line.indexOf("<anonymous>:")+12));
    return callee_function + " #" + callee_line_number;
};

logError = function(message) {
    test_name_and_line_number = getTestDetails();
    console.log("FAIL ["+ test_name_and_line_number + "]: " + message);
};

assertEquals = function(expected, actual) {
    if (expected != actual) {
        logError("expected " + expected + " but was " + actual);
    }
};

assertTrue = function(value) {
    if (value !== true) {
        logError("expected true");
    }
};

assertFalse = function(value) {
    if (value !== false) {
        logError("expected false");
    }
};

assertArrayEquals = function(expected, actual) {
    if (expected.length != actual.length) {
        logError("expected " + expected + " but was " + actual);
        return;
    }
    for (var i = 0; i < expected.length; i++) {
        if (expected[i] != actual[i]) {
            logError("expected " + expected + " but was " + actual);
            return;
        }
    }
};

// ============================================================================================================
// Dummy Data

var ldapDevicePrintProfiles =[
    {
        "uuid":"uuid1",
        "lastSelectedDate":"2014-07-03T08:10:46.154+0000",
        "selectionCounter":1,
        "devicePrint":{
            "screen":{
                "screenWidth":1920,
                "screenHeight":1200,
                "screenColourDepth":24
            },
            "timezone":{
                "timezone":-60
            },
            "plugins":{
                "installedPlugins":"QuickTime Plugin.plugin;Default Browser.plugin;googletalkbrowserplugin.plugin;o1dbrowserplugin.plugin;AdobePDFViewerNPAPI.plugin;JavaAppletPlugin.plugin;CitrixOnlineWebDeploymentPlugin.plugin"
            },
            "fonts":{
                "installedFonts":"cursive;monospace;serif;sans-serif;fantasy;default;Arial;Arial Black;Arial Narrow;Arial Rounded MT Bold;Comic Sans MS;Courier;Courier New;Georgia;Impact;Papyrus;Tahoma;Times;Times New Roman;Trebuchet MS;Verdana;"
            },
            "userAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:30.0) Gecko/20100101 Firefox/30.0",
            "appName":"Netscape",
            "appCodeName":"Mozilla",
            "appVersion":"5.0 (Macintosh)",
            "buildID":"20140605174243",
            "platform":"MacIntel",
            "oscpu":"Intel Mac OS X 10.9",
            "product":"Gecko",
            "productSub":"20100101",
            "language":"en-GB",
            "geolocation":{
                "longitude":-2.5942761,
                "latitude":51.451074999999996
            }
        }
    },
    {
        "uuid":"uuid2",
        "lastSelectedDate":"2014-06-30T00:02:46.154+0000",
        "selectionCounter":1,
        "devicePrint":{
            "screen":{
                "screenWidth":1920,
                "screenHeight":1200,
                "screenColourDepth":24
            },
            "timezone":{
                "timezone":-60
            },
            "plugins":{
                "installedPlugins":"QuickTime Plugin.plugin;Default Browser.plugin;googletalkbrowserplugin.plugin;o1dbrowserplugin.plugin;AdobePDFViewerNPAPI.plugin;JavaAppletPlugin.plugin;CitrixOnlineWebDeploymentPlugin.plugin"
            },
            "fonts":{
                "installedFonts":"cursive;monospace;serif;sans-serif;fantasy;default;Arial;Arial Black;Arial Narrow;Arial Rounded MT Bold;Comic Sans MS;Courier;Courier New;Georgia;Impact;Papyrus;Tahoma;Times;Times New Roman;Trebuchet MS;Verdana;"
            },
            "userAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:30.0) Gecko/20100101 Firefox/30.0",
            "appName":"Netscape",
            "appCodeName":"Mozilla",
            "appVersion":"5.0 (Macintosh)",
            "buildID":"20140605174243",
            "platform":"MacIntel",
            "oscpu":"Intel Mac OS X 10.9",
            "product":"Gecko",
            "productSub":"20100101",
            "language":"en-GB",
            "geolocation":{
                "longitude":-2.5942761,
                "latitude":51.451074999999996
            }
        }
    },
    {
        "uuid":"uuid3",
        "lastSelectedDate":"2014-06-30T23:59:46.154+0000",
        "selectionCounter":1,
        "devicePrint":{
            "screen":{
                "screenWidth":1920,
                "screenHeight":1200,
                "screenColourDepth":24
            },
            "timezone":{
                "timezone":-60
            },
            "plugins":{
                "installedPlugins":"QuickTime Plugin.plugin;Default Browser.plugin;googletalkbrowserplugin.plugin;o1dbrowserplugin.plugin;AdobePDFViewerNPAPI.plugin;JavaAppletPlugin.plugin;CitrixOnlineWebDeploymentPlugin.plugin"
            },
            "fonts":{
                "installedFonts":"cursive;monospace;serif;sans-serif;fantasy;default;Arial;Arial Black;Arial Narrow;Arial Rounded MT Bold;Comic Sans MS;Courier;Courier New;Georgia;Impact;Papyrus;Tahoma;Times;Times New Roman;Trebuchet MS;Verdana;"
            },
            "userAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:30.0) Gecko/20100101 Firefox/30.0",
            "appName":"Netscape",
            "appCodeName":"Mozilla",
            "appVersion":"5.0 (Macintosh)",
            "buildID":"20140605174243",
            "platform":"MacIntel",
            "oscpu":"Intel Mac OS X 10.9",
            "product":"Gecko",
            "productSub":"20100101",
            "language":"en-GB",
            "geolocation":{
                "longitude":-2.5942761,
                "latitude":51.451074999999996
            }
        }
    }
];

clientDevicePrint = {
    "screen":{
        "screenWidth":1920,
        "screenHeight":1200,
        "screenColourDepth":24
    },
    "timezone":{
        "timezone":-60
    },
    "plugins":{
        "installedPlugins":"QuickTime Plugin.plugin;Default Browser.plugin;googletalkbrowserplugin.plugin;o1dbrowserplugin.plugin;AdobePDFViewerNPAPI.plugin;JavaAppletPlugin.plugin;CitrixOnlineWebDeploymentPlugin.plugin"
    },
    "fonts":{
        "installedFonts":"cursive;monospace;serif;sans-serif;fantasy;default;Arial;Arial Black;Arial Narrow;Arial Rounded MT Bold;Comic Sans MS;Courier;Courier New;Georgia;Impact;Papyrus;Tahoma;Times;Times New Roman;Trebuchet MS;Verdana;"
    },
    "userAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:30.0) Gecko/20100101 Firefox/30.0",
    "appName":"Netscape",
    "appCodeName":"Mozilla",
    "appVersion":"5.0 (Macintosh)",
    "buildID":"20140605174243",
    "platform":"MacIntel",
    "oscpu":"Intel Mac OS X 10.9",
    "product":"Gecko",
    "productSub":"20100101",
    "language":"en-GB",
    "geolocation":{
        "longitude":-2.5942761,
        "latitude":51.451074999999996
    }
};

// ComparisonResult.js

/**
 * Constructs an instance of a ComparisonResult with the given penalty points.
 *
 * @param penaltyPoints The penalty points for the comparison (defaults to 0).
 * @param additionalInfoInCurrentValue Whether the current value contains more information than the stored value (defaults to false).
 */
function ComparisonResult() {

    penaltyPoints = 0;
    additionalInfoInCurrentValue = false;

    // TODO: Simplfiy constructor overloading copied from Java

    if (arguments[0] !== undefined && arguments[1] !== undefined) {
        penaltyPoints = arguments[0];
        additionalInfoInCurrentValue = arguments[1];
    }

    if (arguments[0] !== undefined && arguments[1] === undefined) {
        if (typeof(arguments[0]) == "boolean") {
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
 * Comparison function that can be provided as an argument to array.sort
 */
ComparisonResult.compare = function(first, second) {
    if (first === null && second === null) {
        return 0;
    } else if (first === null) {
        return -1;
    } else if (second === null) {
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
 * @return If the comparison was successful.
 */
ComparisonResult.prototype.isSuccessful = function() {
    return this.penaltyPoints === null || this.penaltyPoints === 0;
};

// ColocationComparator.js

/**
 * Constructs an instance of a ColocationComparator
 */
function ColocationComparator() {
}

/**
 * Compares two locations, taking into account a degree of difference.
 *
 * @param currentLatitude (double) The current latitude.
 * @param currentLongitude (double) The current longitude.
 * @param storedLatitude (double) The stored latitude.
 * @param storedLongitude (double) The current longitude.
 * @param maxToleratedDistance (long) The max difference allowed in the two locations, before the penalty is assigned.
 * @param differencePenaltyPoints(long) The number of penalty points.
 * @return A ComparisonResult.
 */
ColocationComparator.prototype.compare = function(currentLatitude, currentLongitude, storedLatitude,
                                                  storedLongitude, maxToleratedDistance, differencePenaltyPoints) {

    if (this.bothNull(currentLatitude, currentLongitude, storedLatitude, storedLongitude)) {
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (this.currentNullStoredNotNull(currentLatitude, currentLongitude, storedLatitude, storedLongitude)) {
        return new ComparisonResult(differencePenaltyPoints);
    }

    if (this.storedNullCurrentNotNull(currentLatitude, currentLongitude, storedLatitude, storedLongitude)) {
        return new ComparisonResult(differencePenaltyPoints, true);
    }

    distance = this.calculateDistance(currentLatitude, currentLongitude, storedLatitude, storedLongitude);

//  if (DEBUG.messageEnabled()) {
//    DEBUG.message("Distance between (" + currentLatitude + "," + currentLongitude + ") and (" + storedLatitude
//                 + "," + storedLongitude + ") is " + distance + " miles");
//  }

    if (Number.parseFloat(distance.toPrecision(5)) === 0) {
//    if (DEBUG.messageEnabled()) {
//      DEBUG.message("Location is the same");
//    }
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    inMaxToleratedRange = this.isInMaxToleratedRange(distance, maxToleratedDistance);
    if (inMaxToleratedRange) {
//    if (DEBUG.messageEnabled()) {
//      DEBUG.message("Tolerated because distance not more then "+maxToleratedDistance);
//    }
        return new ComparisonResult(true);
    } else {
//    if (DEBUG.messageEnabled()) {
//      DEBUG.message("Would be ignored if distance not more then "+maxToleratedDistance);
//    }
        return new ComparisonResult(differencePenaltyPoints);
    }
};

/**
 * Checks if the stored location is null and the current location is null.
 *
 * @param currentLatitude (double) The current latitude.
 * @param currentLongitude (double) The current longitude.
 * @param storedLatitude (double) The stored latitude.
 * @param storedLongitude (double) The current longitude.
 * @return If the check passes true, otherwise false.
 */
ColocationComparator.prototype.bothNull = function(currentLatitude, currentLongitude, storedLatitude, storedLongitude) {
    return this.atLeastOneNull(currentLatitude, currentLongitude) && this.atLeastOneNull(storedLatitude, storedLongitude);
};

/**
 * Checks to see if the x or y co-ordinates is null.
 *
 * @param first (double) The x co-ordinates.
 * @param second (double) The y co-ordinates.
 * @return If either are null.
 */
ColocationComparator.prototype.atLeastOneNull = function(first, second) {
    return first === null || second === null;
};

/**
 * Checks if the current location is null and the stored location is not null.
 *
 * @param currentLatitude (double) The current latitude.
 * @param currentLongitude (double) The current longitude.
 * @param storedLatitude (double) The stored latitude.
 * @param storedLongitude (double) The current longitude.
 * @return If the check passes true, otherwise false.
 */
ColocationComparator.prototype.currentNullStoredNotNull = function(currentLatitude, currentLongitude, storedLatitude, storedLongitude) {
    return this.atLeastOneNull(currentLatitude, currentLongitude) && !this.atLeastOneNull(storedLatitude, storedLongitude);
};

/**
 * Checks if the stored location is null and the current location is not null.
 *
 * @param currentLatitude (double) The current latitude.
 * @param currentLongitude (double) The current longitude.
 * @param storedLatitude (double) The stored latitude.
 * @param storedLongitude (double) The current longitude.
 * @return If the check passes true, otherwise false.
 */
ColocationComparator.prototype.storedNullCurrentNotNull = function(currentLatitude, currentLongitude, storedLatitude, storedLongitude) {
    return !this.atLeastOneNull(currentLatitude, currentLongitude) && this.atLeastOneNull(storedLatitude, storedLongitude);
};

/**
 * Calculates the distances between the two locations.
 *
 * @param currentLatitude The current latitude.
 * @param currentLongitude The current longitude.
 * @param storedLatitude The stored latitude.
 * @param storedLongitude The stored longitude.
 * @return The distance between the two locations.
 */
ColocationComparator.prototype.calculateDistance = function(currentLatitude, currentLongitude, storedLatitude, storedLongitude) {
    var factor = (Math.PI / 180);
    function degreesToRadians(degrees) {
        return degrees * factor;
    }
    function radiansToDegrees(radians) {
        return radians / factor;
    }
    theta = currentLongitude - storedLongitude;
    dist = Math.sin(degreesToRadians(currentLatitude)) * Math.sin(degreesToRadians(storedLatitude))
        + Math.cos(degreesToRadians(currentLatitude)) * Math.cos(degreesToRadians(storedLatitude))
        * Math.cos(degreesToRadians(theta));
    dist = Math.acos(dist);
    dist = radiansToDegrees(dist);
    dist = dist * 60 * 1.1515;
    return dist;
};

/**
 * Whether the distance between the two locations is within the allowed difference.
 *
 * @param distance (double) The actual distance between the locations.
 * @param maxToleratedDistance (long) The max difference allowed between the locations.
 * @return True is the check passes, otherwise false.
 */
ColocationComparator.prototype.isInMaxToleratedRange = function(distance, maxToleratedDistance) {
    return distance <= maxToleratedDistance;
};

// MultiValueAttrComparator.js

/**
 * Compares two Strings of comma separated values.
 */
function MultiValueAttributeComparator() {
}

/**
 * Splits both attributes using delimiter, trims every value and compares collections of values.
 * Returns zero-result for same multi-value attributes.
 *
 * If collections are not same checks if number of differences is less or equal maxToleratedNumberOfDifferences or
 * percentage of difference is less or equal maxToleratedPercentageToMarkAsDifferent.
 *
 * If yes then returns zero-result with additional info, else returns penaltyPoints-result.
 *
 * @param currentAttribute The current value.
 * @param storedAttribute The stored value.
 * @param maxToleratedNumberOfDifferences The max number of differences in the values, before the penalty points
 *                                        are assigned.
 * @param maxToleratedPercentageToMarkAsDifferent The max difference percentage in the values, before the penalty
 *                                                is assigned.
 * @param penaltyPoints The number of penalty points.
 * @return A ComparisonResult.
 */
MultiValueAttributeComparator.prototype.compare = function(currentAttribute, storedAttribute,
                                                           maxToleratedPercentageToMarkAsDifferent, maxToleratedNumberOfDifferences,
                                                           penaltyPoints) {

    currentAttributes = this.convertAttributesToList(currentAttribute);
    storedAttributes = this.convertAttributesToList(storedAttribute);

    if (storedAttribute === null && currentAttribute !== null && currentAttributes.length === 0) {
        return new ComparisonResult(true);
    }

    var maxToleratedDifferences = maxToleratedNumberOfDifferences;
    var maxToleratedPercentage = maxToleratedPercentageToMarkAsDifferent;

    var maxNumberOfElements = Math.max(currentAttributes.length, storedAttributes.length);
    var numberOfTheSameElements = this.getNumberOfSameElements(currentAttributes, storedAttributes);
    var numberOfDifferences = this.getNumberOfDifferences(numberOfTheSameElements, maxNumberOfElements);
    var percentageOfDifferences = this.getPercentageOfDifferences(numberOfDifferences, maxNumberOfElements);

//  if (DEBUG.messageEnabled()) {
//    DEBUG.message(numberOfTheSameElements + " of " + maxNumberOfElements + " are same");
//  }

    if (maxNumberOfElements === 0) {
//    if (DEBUG.messageEnabled()) {
//      DEBUG.message("Ignored because no attributes found in both profiles");
//    }
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (numberOfTheSameElements === maxNumberOfElements) {
//    if (DEBUG.messageEnabled()) {
//      DEBUG.message("Ignored because all attributes are same");
//    }
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (numberOfDifferences > maxToleratedDifferences) {
//    if (DEBUG.messageEnabled()) {
//      DEBUG.message("Would be ignored if not more than " + maxToleratedDifferences + " differences");
//    }
        return new ComparisonResult(penaltyPoints);
    }

    if (percentageOfDifferences > maxToleratedPercentage) {
//    if (DEBUG.messageEnabled()) {
//      DEBUG.message(percentageOfDifferences + " percents are different");
//      DEBUG.message("Would be ignored if not more than " + maxToleratedPercentage + " percent");
//    }
        return new ComparisonResult(penaltyPoints);
    }

//     if (DEBUG.messageEnabled()) {
//         DEBUG.message("Ignored because number of differences(" + numberOfDifferences + ") not more than "
//                 + maxToleratedDifferences);
//         DEBUG.message(percentageOfDifferences + " percents are different");
//         DEBUG.message("Ignored because not more than " + maxToleratedPercentage + " percent");
//     }
    return new ComparisonResult(true);
};

MultiValueAttributeComparator.DELIMITER = ";";

/**
 * Converts a comma separated String into a List.
 *
 * @param multiAttribute The comma separated String.
 * @return A list of the comma separated values.
 */
MultiValueAttributeComparator.prototype.convertAttributesToList = function(multiAttribute) {

    results = [];
    if (multiAttribute === null) {
        return results;
    }

    var attributes = multiAttribute.split(MultiValueAttributeComparator.DELIMITER);
    for (i = 0; i < attributes.length; i++) {
        var attribute = attributes[i];
        if (attribute.trim() !== "") {
            results.push(attribute.trim());
        }
    }

    return results;
};

/**
 * Gets the percentage of the differences between the two values.
 *
 * @param numberOfDifferences The actual number of differences.
 * @param maxNumberOfElements The number of values in the largest multi-value.
 * @return The percentage of differences.
 */
MultiValueAttributeComparator.prototype.getPercentageOfDifferences = function(numberOfDifferences, maxNumberOfElements) {
    if (maxNumberOfElements === 0) {
        return 0;
    }
    // divide by 2, keeping result to 2 decimal places then multiply by 100
    return Number.parseFloat((numberOfDifferences / maxNumberOfElements).toPrecision(2)) * 100;
//  return this.numberOfDifferences.divide(maxNumberOfElements, 2, RoundingMode.HALF_UP).multiply(HUNDRED);
};

/**
 * Gets the number of differences between the two values.
 *
 * @param numberOfSameElements The number of elements that are equal.
 * @param maxNumberOfElements The number of values in the largest multi-value.
 * @return The number of differences.
 */
MultiValueAttributeComparator.prototype.getNumberOfDifferences = function(numberOfSameElements, maxNumberOfElements) {
    return maxNumberOfElements - numberOfSameElements;
};

/**
 * Gets the number of elements that are equal between the two lists of values.
 *
 * @param currentAttributes The current values.
 * @param storedAttributes The stored values.
 * @return The number of elements that are equal.
 */
MultiValueAttributeComparator.prototype.getNumberOfSameElements = function(currentAttributes, storedAttributes) {
    var union = currentAttributes.filter(function(element) {
        return storedAttributes.indexOf(element) !== -1;
    });
    return union.length;
};

// DevicePrintComparator.js

/**
 * Comparator for comparing two Device Print objects to determine how similar they are based
 * from the penalty points assigned to each attribute on the Device Print object.
 *
 * @param multiValueAttributeComparator An instance of the MultiValueAttributeComparator.
 * @param colocationComparator An instance of the ColocationComparator.
 */
function DevicePrintComparator(multiValueAttributeComparator, colocationComparator) {
    this.multiValueAttributeComparator = multiValueAttributeComparator;
    this.colocationComparator = colocationComparator;
}

/**
 * Compares two Device Print objects to determine how similar they are based from the penalty points
 * assigned to each attribute on the Device Print object.
 *
 * @param currentDevicePrint The latest Device Print object.
 * @param storedDevicePrint A previously stored Device Print object.
 * @param config An instance of the DevicePrintAuthenticationConfig.
 * @return A ComparisonResult detailing the number of penalty points assigned to this comparison.
 */
DevicePrintComparator.prototype.compare = function(currentDevicePrint, storedDevicePrint, config) {

    var aggregatedComparisonResult = new ComparisonResult();

    var userAgentComparisonResult = this.compareUserAgent(
        currentDevicePrint.userAgent, storedDevicePrint.userAgent,
        config.userAgentPenaltyPoints, config.ignoreVersionInUserAgent);
    aggregatedComparisonResult.addComparisonResult(userAgentComparisonResult);

    var installedFontsComparisonResult = this.multiValueAttributeComparator.compare(
        currentDevicePrint.fonts.installedFonts, storedDevicePrint.fonts.installedFonts,
        config.maxToleratedDiffsInInstalledFonts,
        config.maxToleratedPercentageToMarkAsDifferentInstalledFonts,
        config.installedFontsPenaltyPoints);
    aggregatedComparisonResult.addComparisonResult(installedFontsComparisonResult);

    var installedPluginsComparisonResult = this.multiValueAttributeComparator.compare(
        currentDevicePrint.plugins.installedPlugins, storedDevicePrint.plugins.installedPlugins,
        config.maxToleratedDiffsInInstalledPlugins,
        config.maxToleratedPercentageToMarkAsDifferentPlugins,
        config.installedPluginsPenaltyPoints);
    aggregatedComparisonResult.addComparisonResult(installedPluginsComparisonResult);

    var colorDepthComparisonResult = this.compareStrings( // XXX: Should be numeric comparison?
        currentDevicePrint.screen.screenColourDepth, storedDevicePrint.screen.screenColourDepth,
        config.screenColourDepthPenaltyPoints);
    aggregatedComparisonResult.addComparisonResult(colorDepthComparisonResult);

    var timezoneComparisonResult = this.compareStrings(
        currentDevicePrint.timezone.timezone, storedDevicePrint.timezone.timezone,
        config.timezonePenaltyPoints);
    aggregatedComparisonResult.addComparisonResult(timezoneComparisonResult);

    var screenResolutionComparisonResult = this.compareScreenResolution(
        currentDevicePrint.screen.screenWidth, currentDevicePrint.screen.screenHeight,
        storedDevicePrint.screen.screenWidth, storedDevicePrint.screen.screenHeight,
        config.screenResolutionPenaltyPoints);
    aggregatedComparisonResult.addComparisonResult(screenResolutionComparisonResult);

    var locationComparisonResult = this.colocationComparator.compare(
        currentDevicePrint.geolocation.latitude, currentDevicePrint.geolocation.longitude,
        storedDevicePrint.geolocation.latitude, storedDevicePrint.geolocation.longitude,
        config.locationAllowedRange, config.locationPenaltyPoints);
    aggregatedComparisonResult.addComparisonResult(locationComparisonResult);

    // if (DEBUG.messageEnabled()) {
    //     DEBUG.message("Compared device current print: " + currentDevicePrint);
    //     DEBUG.message("Compared stored device print: " + storedDevicePrint);
    //     DEBUG.message("Penalty points: " + aggregatedComparisonResult.getPenaltyPoints());
    //     DEBUG.message("UserAgent: " + userAgentComparisonResult + ", fonts: " + installedFontsComparisonResult
    //             + ", plugins: " + installedPluginsComparisonResult + ", colourDepth: " + colorDepthComparisonResult
    //             + ", timezone: " + timezoneComparisonResult + ", screenRes: " + screenResolutionComparisonResult
    //             + ", location: " + locationComparisonResult);
    // }

    return aggregatedComparisonResult;
};

/**
 * Compares two Strings and if they are equal then returns a ComparisonResult with zero penalty points assigned,
 * otherwise returns a ComparisonResult with the given number of penalty points assigned.
 *
 * @param currentValue The current value.
 * @param storedValue The stored value.
 * @param penaltyPoints The number of penalty points.
 * @return A ComparisonResult.
 */
DevicePrintComparator.prototype.compareStrings = function(currentValue, storedValue, penaltyPoints) {

    if (penaltyPoints === 0) {
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (storedValue !== null) {
        if (currentValue === null || currentValue !== storedValue) {
            return new ComparisonResult(penaltyPoints);
        }
    } else if (currentValue !== null) {
        return new ComparisonResult(true);
    }

    return ComparisonResult.ZERO_PENALTY_POINTS;
};

/**
 * Compares two User Agent Strings and if they are equal then returns a ComparisonResult with zero penalty
 * points assigned, otherwise returns a ComparisonResult with the given number of penalty points assigned.
 *
 * @param currentValue The current value.
 * @param storedValue The stored value.
 * @param penaltyPoints The number of penalty points.
 * @param ignoreVersion If the version numbers in the User Agent Strings should be ignore in the comparison.
 * @return A ComparisonResult.
 */
DevicePrintComparator.prototype.compareUserAgent = function(currentValue, storedValue, penaltyPoints, ignoreVersion) {

    if (ignoreVersion) {
        // remove version number
        currentValue = currentValue.replace(/[\d\.]+/g, "").trim();
        storedValue = storedValue.replace(/[\d\.]+/g, "").trim();
    }

    return this.compareStrings(currentValue, storedValue, penaltyPoints);
};

/**
 * Compares two Screen resolution Strings and if they are equal then returns a ComparisonResult with zero penalty
 * points assigned, otherwise returns a ComparisonResult with the given number of penalty points assigned.
 *
 * @param currentWidth The current width.
 * @param currentHeight The current height.
 * @param storedWidth The stored width.
 * @param storedHeight The stored height.
 * @param penaltyPoints The number of penalty points.
 * @return A ComparisonResult.
 */
DevicePrintComparator.prototype.compareScreenResolution = function(currentWidth, currentHeight, storedWidth, storedHeight, penaltyPoints) {

    var widthComparisonResult = this.compareStrings(currentWidth, storedWidth, penaltyPoints);
    var heightComparisonResult = this.compareStrings(currentHeight, storedHeight, penaltyPoints);

    if (widthComparisonResult.isSuccessful() && heightComparisonResult.isSuccessful()) {
        return new ComparisonResult(widthComparisonResult.additionalInfoInCurrentValue  || heightComparisonResult.additionalInfoInCurrentValue);
    } else {
        return new ComparisonResult(penaltyPoints);
    }
};

// UserProfilesDao.js

function UserProfilesDao() {

}

/**
 * Gets the list of device print profiles that have been saved for the user.
 *
 * @returns a JavaScript array of JSON strings.
 */
UserProfilesDao.prototype.getProfiles = function() {
    return idRepository.getAttribute(username, "devicePrintProfiles");
};

/**
 * Sets the list of device print profiles for the user.
 * NOTE: will completely replace the current value.
 *
 * @param profiles expected to be a JavaScript array of JSON strings.
 */
UserProfilesDao.prototype.updateProfile = function(profiles) {
    idRepository.setAttribute(username, "devicePrintProfiles", profiles);
};

// DevicePrintService.js

/**
 * This class exposes services to parse Device Print information from the client, find matches against stored user
 * profiles, and update the user profiles in LDAP.
 *
 * @param config JSON object storing configuration attributes.
 * @param userProfilesDao An instance of the UserProfilesDao.
 * @param extractorFactory An instance of the DevicePrintExtractorFactory.
 * @param devicePrintComparator An instance of the DevicePrintComparator.
 */
function DevicePrintService(config, userProfilesDao, extractorFactory, devicePrintComparator) {
    this.config = config;
    this.userProfilesDao = userProfilesDao;
    this.extractorFactory = extractorFactory;
    this.devicePrintComparator = devicePrintComparator;
}

DevicePrintService.prototype.hasRequiredAttributes = function(devicePrint) {
    for (var requiredAttribute in this.config.requiredAttributes) {
        if (this.config.requiredAttributes.hasOwnProperty(requiredAttribute)) {
            if (this.config.requiredAttributes[requiredAttribute] && devicePrint[requiredAttribute] === undefined) {
                console.warn("Device Print profile missing required attribute, " + requiredAttribute);
                return false;
            }
        }
    }

    return true;
};


DevicePrintService.prototype.hasValidProfile = function(devicePrint) {
    var selectedProfile = this.getBestMatchingUserProfile(devicePrint);
    if (selectedProfile != null) {
        selectedProfile.lastSelectedDate = new Date();
        selectedProfile.selectionCounter = selectedProfile.selectionCounter + 1;
        selectedProfile.devicePrint = devicePrint;
        return true;
    }
    return false;
};

/**
 * Uses the given Device Print information to find the best matching stored Device Print information from stored
 * User Profiles. It uses the penalty points set in the authentication module settings to determine whether a stored
 * Device print matches the given one.
 *
 * If no match is found null is returned.
 *
 * @param devicePrint The Device Print to find a match for.
 * @return The matching User Profile or null.
 */
DevicePrintService.prototype.getBestMatchingUserProfile = function(devicePrint) {

    var results = [];

    var notExpiredProfiles = this.getNotExpiredProfiles();
    for (var i = 0; i < notExpiredProfiles.length; i++) {
        var userProfile = notExpiredProfiles[i];
        var storedDevicePrint = userProfile.devicePrint;
        var comparisonResult = this.devicePrintComparator.compare(devicePrint, storedDevicePrint, this.config);
        results.push({
            key: comparisonResult,
            value: userProfile
        });
    }

    if (results.length === 0) {
        return null;
    }

    results.sort(function(first, second) {
        return ComparisonResult.compare(first.key, second.key);
    });
    var selectedComparisonResult = results[0].key;

    var selectedProfile = null;
    if (selectedComparisonResult.penaltyPoints <= this.config.maxToleratedPenaltyPoints) {
        selectedProfile = results[0].value;
    }

    return selectedProfile;
}

/**
 * Gets the list of valid, non-expired User's profiles.
 *
 * @return The valid User profiles.
 */
DevicePrintService.prototype.getNotExpiredProfiles = function() {
    var results = [];

    var profiles = this.userProfilesDao.getProfiles();
    for (var i = 0; i < profiles.length; i++) {
        if (!this.isExpiredProfile(profiles[i])) {
            results.push(profiles[i]);
        }
    }

    return results;
};

/**
 * Determines whether a User's profile has expired due to it not being accessed within the profile expiration
 * authentication module setting.
 *
 * @param userProfile The User profile to check if has expired.
 * @return If the user profile has expired or not.
 */
DevicePrintService.prototype.isExpiredProfile = function(devicePrintProfile) {
    var expirationDate = new Date();
    expirationDate.setDate(expirationDate.getDate() - this.config.profileExpirationDays);

    var lastSelectedDate = new Date(devicePrintProfile.lastSelectedDate);

    return lastSelectedDate < expirationDate;
};


// ============================================================================================================
// TESTS

// DevicePrintService.js

// @BeforeMethod
// public void setUpMethod() {

//     devicePrintAuthenticationConfig = mock(DevicePrintAuthenticationConfig.class);
//     userProfilesDao = mock(UserProfilesDao.class);
//     extractorFactory = mock(DevicePrintExtractorFactory.class);
//     devicePrintComparator = mock(DevicePrintComparator.class);

//     given(devicePrintAuthenticationConfig.getInt(
//             DevicePrintAuthenticationConfig.PROFILE_EXPIRATION_DAYS)).willReturn(30);
//     given(devicePrintAuthenticationConfig.getInt(
//             DevicePrintAuthenticationConfig.MAX_STORED_PROFILES)).willReturn(2);

//     devicePrintService = new DevicePrintService(devicePrintAuthenticationConfig, userProfilesDao, extractorFactory,
//             devicePrintComparator);
// }

// @Test
// public void shouldCheckHasRequiredAttributes() {

//     //Given
//     DevicePrint devicePrint = mock(DevicePrint.class);

//     //When
//     devicePrintService.hasRequiredAttributes(devicePrint);

//     //Then
//     verify(devicePrintAuthenticationConfig).hasRequiredAttributes(devicePrint);
// }

// @Test
// public void shouldGetCurrentDevicePrint() {

//     //Given
//     HttpServletRequest request = mock(HttpServletRequest.class);
//     Set<Extractor> extractors = new HashSet<Extractor>();
//     Extractor extractorOne = mock(Extractor.class);
//     Extractor extractorTwo = mock(Extractor.class);

//     given(extractorFactory.getExtractors()).willReturn(extractors);
//     extractors.add(extractorOne);
//     extractors.add(extractorTwo);

//     //When
//     DevicePrint devicePrint = devicePrintService.getDevicePrint(request);

//     //Then
//     verify(extractorOne).extractData(Matchers.<DevicePrint>anyObject(), eq(request));
//     verify(extractorTwo).extractData(Matchers.<DevicePrint>anyObject(), eq(request));
//     assertNotNull(devicePrint);
// }

// XXX: Flaky test: replace hard-coded string with getDate()
function testIsExpired() {
    // Given
    var config = {
        profileExpirationDays: 7
    };
    var devicePrintService = new DevicePrintService(config, null, null, null);
    // When
    // Then
    assertFalse(devicePrintService.isExpiredProfile(ldapDevicePrintProfiles[0]));
    assertTrue(devicePrintService.isExpiredProfile(ldapDevicePrintProfiles[1]));
    assertFalse(devicePrintService.isExpiredProfile(ldapDevicePrintProfiles[2]));
}
testIsExpired();
// TODO: handle bad input? in Java?

// XXX: Flaky test
function testGetNotExpiredProfiles() {
    // Given
    var config = {
        profileExpirationDays: 7
    };
    var mockUserProfilesDao = {
        getProfiles: function() { return ldapDevicePrintProfiles; }
    };
    var devicePrintService = new DevicePrintService(config, mockUserProfilesDao, null, null);
    // When
    var notExpiredProfiles = devicePrintService.getNotExpiredProfiles();
    // Then
    assertArrayEquals(
        [ldapDevicePrintProfiles[0], ldapDevicePrintProfiles[2]],
        notExpiredProfiles);
}
testGetNotExpiredProfiles();

function shouldGetBestMatchingUserProfileWithNoStoredProfiles() {
    //Given
    var config = {};
    var devicePrint = {}; // mock(DevicePrint.class);
    var mockUserProfilesDao = {
        getProfiles: function() { return []; }
    };
    var devicePrintService = new DevicePrintService(config, mockUserProfilesDao, null, null);
    //When
    var selectedUserProfile = devicePrintService.getBestMatchingUserProfile(devicePrint);
    //Then
    assertEquals(null, selectedUserProfile);
}
shouldGetBestMatchingUserProfileWithNoStoredProfiles();

function shouldGetBestMatchingUserProfile() {

    function getDate(daysAgo) {
        var result = new Date();
        result.setDate(result.getDate() - daysAgo);
        return result;
    }

    //Given
    var mockConfig = {
        maxStoredProfiles: 2,
        maxToleratedPenaltyPoints: 50,
        profileExpirationDays: 30
    };
    var devicePrint = {}; // mock(DevicePrint.class);
    var userProfileOneDevicePrint = {};
    var userProfileTwoDevicePrint = {};
    var userProfileThreeDevicePrint = {};
    var userProfileOne = {
        lastSelectedDate: getDate(10),
        devicePrint: userProfileOneDevicePrint
    };
    var userProfileTwo = {
        lastSelectedDate: getDate(31),
        devicePrint: userProfileTwoDevicePrint
    };
    var userProfileThree = {
        lastSelectedDate: getDate(20),
        devicePrint: userProfileThreeDevicePrint
    };
    var userProfileOneResult = new ComparisonResult(30);
    var userProfileThreeResult = new ComparisonResult(20);

    var userProfiles = [userProfileOne, userProfileTwo, userProfileThree];
    var mockUserProfilesDao = {
        getProfiles: function() {
            return [userProfileOne, userProfileTwo, userProfileThree];
        }
    };

    var mockDevicePrintComparator = {
        compare: function(currentDevicePrint, storedDevicePrint, config) {
            if (storedDevicePrint === userProfileOneDevicePrint) {
                return userProfileOneResult;
            } else if (storedDevicePrint === userProfileThreeDevicePrint) {
                return userProfileThreeResult;
            }
        }
    };

    var devicePrintService = new DevicePrintService(mockConfig, mockUserProfilesDao, null, mockDevicePrintComparator);

    //When
    var selectedUserProfile = devicePrintService.getBestMatchingUserProfile(devicePrint);

    //Then
    assertEquals(userProfileThree, selectedUserProfile);
}
shouldGetBestMatchingUserProfile();

// private Date getDate(int daysAgo) {
//     Calendar calendar = Calendar.getInstance();
//     calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
//     return calendar.getTime();
// }

// @Test
// public void shouldCreateNewProfile() throws NotUniqueUserProfileException {

//     //Given
//     DevicePrint devicePrint = mock(DevicePrint.class);

//     given(userProfilesDao.getProfiles()).willReturn(new ArrayList<UserProfile>());

//     //When
//     devicePrintService.createNewProfile(devicePrint);

//     //Then
//     verify(userProfilesDao).removeProfile(anyString());
//     ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
//     verify(userProfilesDao).addProfile(userProfileCaptor.capture());
//     UserProfile userProfile = userProfileCaptor.getValue();
//     assertEquals(userProfile.getDevicePrint(), devicePrint);
//     verify(userProfilesDao).saveProfiles();
// }

// @Test
// public void shouldCreateNewProfileAndDeleteOlderOnes() throws NotUniqueUserProfileException {

//     //Given
//     DevicePrint devicePrint = mock(DevicePrint.class);
//     List<UserProfile> userProfiles = spy(new ArrayList<UserProfile>());
//     UserProfile userProfileOne = mock(UserProfile.class);
//     UserProfile userProfileTwo = mock(UserProfile.class);
//     UserProfile userProfileThree = mock(UserProfile.class);

//     userProfiles.add(userProfileOne);
//     userProfiles.add(userProfileTwo);
//     userProfiles.add(userProfileThree);
//     given(userProfilesDao.getProfiles()).willReturn(userProfiles);

//     given(userProfileOne.getLastSelectedDate()).willReturn(getDate(10));
//     given(userProfileTwo.getLastSelectedDate()).willReturn(getDate(31));
//     given(userProfileThree.getLastSelectedDate()).willReturn(getDate(30));

//     //When
//     devicePrintService.createNewProfile(devicePrint);

//     //Then
//     verify(userProfilesDao).removeProfile(anyString());

//     verify(userProfiles).remove(userProfileTwo);
//     verify(userProfiles).remove(userProfileThree);

//     ArgumentCaptor<UserProfile> userProfileCaptor = ArgumentCaptor.forClass(UserProfile.class);
//     verify(userProfilesDao).addProfile(userProfileCaptor.capture());
//     UserProfile userProfile = userProfileCaptor.getValue();
//     assertEquals(userProfile.getDevicePrint(), devicePrint);
//     verify(userProfilesDao).saveProfiles();
// }

// @Test
// public void shouldUpdateProfile() throws NotUniqueUserProfileException {

//     //Given
//     UserProfile userProfile = mock(UserProfile.class);
//     DevicePrint devicePrint = mock(DevicePrint.class);

//     given(userProfile.getUuid()).willReturn("USER_PROFILE_UUID");
//     given(userProfilesDao.getProfiles()).willReturn(new ArrayList<UserProfile>());

//     //When
//     devicePrintService.updateProfile(userProfile, devicePrint);

//     //Then
//     verify(userProfile).setSelectionCounter(anyLong());
//     verify(userProfile).setLastSelectedDate(Matchers.<Date>anyObject());
//     verify(userProfile).setDevicePrint(devicePrint);
//     verify(userProfilesDao).removeProfile(anyString());
//     verify(userProfilesDao).addProfile(userProfile);
//     verify(userProfilesDao).saveProfiles();
// }

// DevicePrintComparator.js

function shouldCompareDevicePrints() {
    //Given
    var currentDevicePrint = {
        "screen":{
            "screenWidth": "SCREEN_WIDTH",             // XXX: integer?
            "screenHeight": "SCREEN_HEIGHT",           // XXX: integer?
            "screenColourDepth": "SCREEN_COLOUR_DEPTH" // XXX: integer?
        },
        "timezone":{
            "timezone": "TIMEZONE"                      // XXX: integer?
        },
        "plugins":{
            "installedPlugins":"INSTALLED_PLUGINS"
        },
        "fonts":{
            "installedFonts":"INSTALLED_FONTS"
        },
        "userAgent":"USER_AGENT",
        "geolocation":{
            "longitude": 2.0,
            "latitude": 3.0
        }
    };

    var storedDevicePrint = {
        "screen":{
            "screenWidth": "SCREEN_WIDTH",
            "screenHeight": "SCREEN_HEIGHT",
            "screenColourDepth": "SCREEN_COLOUR_DEPTH"
        },
        "timezone":{
            "timezone": "TIMEZONE"
        },
        "plugins":{
            "installedPlugins":"INSTALLED_PLUGINS"
        },
        "fonts":{
            "installedFonts":"INSTALLED_FONTS"
        },
        "userAgent":"USER_AGENT",
        "geolocation":{
            "longitude": 2.0,
            "latitude": 3.0
        }
    };

    var config = {
        requiredAttributes: {
            fonts: true,
            plugins: true,
            screen: true,
            geolocation: false,
            timezone: true
        },

        userAgentPenaltyPoints: 100,
        ignoreVersionInUserAgent: false,

        maxToleratedDiffsInInstalledFonts: 5,
        maxToleratedPercentageToMarkAsDifferentInstalledFonts: 10,
        installedFontsPenaltyPoints: 100,

        maxToleratedDiffsInInstalledPlugins: 5,
        maxToleratedPercentageToMarkAsDifferentPlugins: 10,
        installedPluginsPenaltyPoints: 100,

        screenColourDepthPenaltyPoints: 100,

        timezonePenaltyPoints: 100,

        screenResolutionPenaltyPoints: 100,

        locationAllowedRange: 100,
        locationPenaltyPoints: 100,
    };
    var cr = new ComparisonResult(10);
    var mockMultiValueAttributeComparator = {
        compare: function(currentAttribute, storedAttribute,
                          maxToleratedPercentageToMarkAsDifferent, maxToleratedNumberOfDifferences,
                          penaltyPoints) {
            this.compareCallCount = this.compareCallCount + 1;
            return cr;
        },
        compareCallCount: 0
    };
    var mockColocationComparator = {
        compare: function(currentLatitude, currentLongitude, storedLatitude,
                          storedLongitude, maxToleratedDistance, differencePenaltyPoints) {
            this.compareCallCount = this.compareCallCount + 1;
            return cr;
        },
        compareCallCount: 0
    };
    var devicePrintComparator = new DevicePrintComparator(mockMultiValueAttributeComparator, mockColocationComparator);

    //When
    var comparisonResult = devicePrintComparator.compare(currentDevicePrint, storedDevicePrint, config);

    //Then
    assertEquals(2, mockMultiValueAttributeComparator.compareCallCount);
    assertEquals(1, mockColocationComparator.compareCallCount);

    assertEquals(30, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareDevicePrints();

function shouldCompareWithNoPenaltyPoints() {
    //Given
    var devicePrintComparator = new DevicePrintComparator(null, null);
    //When
    var comparisonResult = devicePrintComparator.compareStrings("CURRENT_VALUE", "STORED_VALUE", 0);
    //Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareWithNoPenaltyPoints();

function shouldCompareWhenStoredValueIsDifferentToCurrentValue() {
    //Given
    var devicePrintComparator = new DevicePrintComparator(null, null);
    //When
    var comparisonResult = devicePrintComparator.compareStrings("CURRENT_VALUE", "STORED_VALUE", 10);
    //Then
    assertEquals(10, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareWhenStoredValueIsDifferentToCurrentValue();

function shouldCompareWhenStoredValueIsNotNullAndCurrentValueIsNull() {
    //Given
    var devicePrintComparator = new DevicePrintComparator(null, null);
    //When
    var comparisonResult = devicePrintComparator.compareStrings(null, "STORED_VALUE", 10);
    //Then
    assertEquals(10, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareWhenStoredValueIsNotNullAndCurrentValueIsNull();

function shouldCompareWhenStoredValueIsNullAndCurrentValueIsNotNull() {
    //Given
    var devicePrintComparator = new DevicePrintComparator(null, null);
    //When
    var comparisonResult = devicePrintComparator.compareStrings("CURRENT_VALUE", null, 10);
    //Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareWhenStoredValueIsNullAndCurrentValueIsNotNull();

function shouldCompareUserAgentsIgnoringVersionNumbers() {
    //Given
    var devicePrintComparator = new DevicePrintComparator(null, null);
    //When
    var comparisonResult = devicePrintComparator.compareUserAgent("USER_AGENT_1234567890.",
        "1234USER_.567890AGENT_", 10, true);
    //Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareUserAgentsIgnoringVersionNumbers();

function shouldCompareScreenResolutionWhenNotTheSame() {
    //Given
    var devicePrintComparator = new DevicePrintComparator(null, null);
    //When
    var comparisonResult = devicePrintComparator.compareScreenResolution("CURRENT_WIDTH",
        "CURRENT_HEIGHT", "STORED_WIDTH", "STORED_HEIGHT", 10);
    //Then
    assertEquals(10, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareScreenResolutionWhenNotTheSame();

// MultiValueAttributeComparator.js

function shouldCompareMultiValueStringsWhenStoredValueIsNullAndCurrentValueIsEmpty() {
    //Given
    var multiValueAttrComparator = new MultiValueAttributeComparator();
    var currentValue = "";
    var storedValue = null;
    //When
    var comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertEquals(comparisonResult.penaltyPoints, 0);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringsWhenStoredValueIsNullAndCurrentValueIsEmpty();

function shouldCompareMultiValueStringsWhenBothAreEmpty() {
    //Given
    var multiValueAttrComparator = new MultiValueAttributeComparator();
    var currentValue = "";
    var storedValue = "";
    //When
    var comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringsWhenBothAreEmpty();

function shouldCompareMultiValueStringsWhenBothAreEqual() {
    //Given
    var multiValueAttrComparator = new MultiValueAttributeComparator();
    var currentValue = "VALUE_A; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var storedValue = "VALUE_A; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    //When
    var comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringsWhenBothAreEqual();

function shouldCompareMultiValueStringWhenThereAreLessDifferencesThanMax() {
    //Given
    var multiValueAttrComparator = new MultiValueAttributeComparator();
    var currentValue = "VALUE_AA; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    //When
    var comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertEquals(0, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringWhenThereAreLessDifferencesThanMax();

function shouldCompareMultiValueStringWhenThereAreMoreDifferencesThanMax() {
    //Given
    var multiValueAttrComparator = new MultiValueAttributeComparator();
    var currentValue = "VALUE_AA; VALUE_BB; VALUE_C; VALUE_D; VALUE_E";
    var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    //When
    var comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringWhenThereAreMoreDifferencesThanMax();

function shouldCompareMultiValueStringWhenThereIsLessPercentageDiffThanMax() {
    //Given
    var multiValueAttrComparator = new MultiValueAttributeComparator();
    var currentValue = "VALUE_AA; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    //When
    var comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertEquals(0, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringWhenThereIsLessPercentageDiffThanMax();

function shouldCompareMultiValueStringWhenThereIsMorePercentageDiffThanMax() {
    //Given
    var multiValueAttrComparator = new MultiValueAttributeComparator();
    var currentValue = "VALUE_AA; VALUE_BB; VALUE_C; VALUE_D; VALUE_E";
    var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    //When
    var comparisonResult = multiValueAttrComparator.compare(currentValue, storedValue, 20, 1, 111);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringWhenThereIsMorePercentageDiffThanMax;

// ColocationComparator.js

function shouldCompareLocationWhenBothXsAreNull() {
    //Given
    var colocationComparator = new ColocationComparator();
    //When
    var comparisonResult = colocationComparator.compare(null, 2.0, null, 2.0, 100, 111);
    //Then
    assertTrue(comparisonResult.isSuccessful());
}
shouldCompareLocationWhenBothXsAreNull();

function shouldCompareLocationWhenBothYsAreNull() {
    //Given
    var colocationComparator = new ColocationComparator();
    //When
    var comparisonResult = colocationComparator.compare(2.0, null, 2.0, null, 100, 111);
    //Then
    assertTrue(comparisonResult.isSuccessful());
}
shouldCompareLocationWhenBothYsAreNull();

function shouldCompareLocationWhenCurrentXIsNull() {
    //Given
    //When
    var comparisonResult = colocationComparator.compare(null, 2.0, 2.0, 2.0, 100, 111);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationWhenCurrentXIsNull();

function shouldCompareLocationWhenCurrentYIsNull() {
    //Given
    var colocationComparator = new ColocationComparator();
    //When
    var comparisonResult = colocationComparator.compare(2.0, null, 2.0, 2.0, 100, 111);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationWhenCurrentYIsNull();

function shouldCompareLocationWhenStoredXIsNull() {
    //Given
    var colocationComparator = new ColocationComparator();
    //When
    var comparisonResult = colocationComparator.compare(2.0, 2.0, null, 2.0, 100, 111);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationWhenStoredXIsNull();

function shouldCompareLocationWhenStoredYIsNull() {
    //Given
    var colocationComparator = new ColocationComparator();
    //When
    var comparisonResult = colocationComparator.compare(2.0, 2.0, 2.0, null, 100, 111);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationWhenStoredYIsNull();

function shouldCompareLocationsThatAreEqual() {
    //Given
    var colocationComparator = new ColocationComparator();
    //When
    var comparisonResult = colocationComparator.compare(2.0, 2.0, 2.0, 2.0, 100, 111);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationsThatAreEqual();

function shouldCompareLocationsThatAreWithinTolerableRange() {
    //Given
    var colocationComparator = new ColocationComparator();
    //When
    var comparisonResult = colocationComparator.compare(3.0, 3.0, 2.0, 2.0, 100, 111);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationsThatAreWithinTolerableRange();

function shouldCompareLocationsThatAreOutsideTolerableRange() {
    //Given
    var colocationComparator = new ColocationComparator();
    //When
    var comparisonResult = colocationComparator.compare(20.0, 20.0, 2.0, 2.0, 100, 111);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(comparisonResult.penaltyPoints, 111);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationsThatAreOutsideTolerableRange();

// ComparisonResult.js

function shouldCreateComparisonResultWithZeroPenaltyPoints() {
    var comparisonResult = new ComparisonResult();
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCreateComparisonResultWithZeroPenaltyPoints();

function shouldCreateComparisonResultWithZeroPenaltyPointsUsingConstant() {
    var comparisonResult = ComparisonResult.ZERO_PENALTY_POINTS;
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCreateComparisonResultWithZeroPenaltyPointsUsingConstant();

function shouldCreateComparisonResultWithPenaltyPointsAndAdditionalInfo() {
    var comparisonResult = new ComparisonResult(11, true);
    assertEquals(11, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCreateComparisonResultWithPenaltyPointsAndAdditionalInfo();

function shouldCreateComparisonResultWithPenaltyPoints() {
    var comparisonResult = new ComparisonResult(111);
    assertEquals(comparisonResult.penaltyPoints, 111);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCreateComparisonResultWithPenaltyPoints();

function shouldCreateComparisonResultWithAdditionalInfo() {
    var comparisonResult = new ComparisonResult(true);
    assertEquals(comparisonResult.penaltyPoints, 0);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCreateComparisonResultWithAdditionalInfo();

function shouldAddComparisonResult() {
    // Given
    var comparisonResult = new ComparisonResult(111);
    var anotherComparisonResult = new ComparisonResult(321);
    //When
    comparisonResult.addComparisonResult(anotherComparisonResult);
    //Then
    assertEquals(comparisonResult.penaltyPoints, 432);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldAddComparisonResult();

function shouldGetComparisonResultSuccessful() {
    //Given
    var comparisonResult = new ComparisonResult(0);
    //When
    var isSuccessful = comparisonResult.isSuccessful();
    //Then
    assertTrue(isSuccessful);
}
shouldGetComparisonResultSuccessful();

function shouldGetComparisonResultUnsuccessful() {
    //Given
    var comparisonResult = new ComparisonResult(1);
    //When
    var isSuccessful = comparisonResult.isSuccessful();
    //Then
    assertFalse(isSuccessful);
}
shouldGetComparisonResultUnsuccessful();

function shouldCompareComparisonResultsEqually() {
    //Given
    var comparisonResult = new ComparisonResult(111);
    var anotherComparisonResult = new ComparisonResult(111);
    //When
    var result = comparisonResult.compareTo(anotherComparisonResult);
    //Then
    assertEquals(0, result);
}
shouldCompareComparisonResultsEqually();

function shouldCompareComparisonResultsEqually() {
    //Given
    var comparisonResult = new ComparisonResult(111);
    var anotherComparisonResult = new ComparisonResult(111);
    //When
    var result = ComparisonResult.compare(comparisonResult, anotherComparisonResult);
    //Then
    assertEquals(0, result);
}
shouldCompareComparisonResultsEqually();

function shouldCompareComparisonResultsNotEqualPenaltyPoints() {
    //Given
    var comparisonResult = new ComparisonResult(110);
    var anotherComparisonResult = new ComparisonResult(111);
    //When
    var result = ComparisonResult.compare(comparisonResult, anotherComparisonResult);
    //Then
    assertEquals(-1, result);
}
shouldCompareComparisonResultsNotEqualPenaltyPoints();

function shouldCompareComparisonResultsWithEqualPenaltyPointsButOneWithAdditionalInfo() {
    //Given
    var comparisonResult = new ComparisonResult(111, true);
    var anotherComparisonResult = new ComparisonResult(111);
    //When
    var result = ComparisonResult.compare(comparisonResult, anotherComparisonResult);
    //Then
    assertEquals(1, result);
}
shouldCompareComparisonResultsWithEqualPenaltyPointsButOneWithAdditionalInfo();

function shouldCompareComparisonResultsWithNull() {
    //Given
    var comparisonResult = new ComparisonResult(111);
    anotherComparisonResult = null;
    //When
    var result = ComparisonResult.compare(comparisonResult, anotherComparisonResult);
    //Then
    assertEquals(1, result);
}
shouldCompareComparisonResultsWithNull();

