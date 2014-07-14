var ScalarComparator = {}, ScreenComparator = {}, MultiValueComparator = {}, UserAgentComparator = {}, GeolocationComparator = {}, DEBUG = logger;

var config = {
    profileExpiration: 30,    			//in days
    maxProfilesAllowed: 5,
    maxPenaltyPoints: 200,
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

var all, any, calculateDistance, calculateIntersection, calculatePercentage, splitAndTrim;

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
    if (DEBUG.messageEnabled()) {
        DEBUG.message("StringComparator.compare:currentValue: " + JSON.stringify(currentValue));
        DEBUG.message("StringComparator.compare:storedValue: " + JSON.stringify(storedValue));
        DEBUG.message("StringComparator.compare:config: " + JSON.stringify(config));
    }
    if (config.penaltyPoints === 0) {
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (storedValue !== null) {
        if (currentValue === null || currentValue !== storedValue) {
            return new ComparisonResult(config.penaltyPoints);
        }
    } else if (currentValue !== null) {
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
    if (DEBUG.messageEnabled()) {
        DEBUG.message("ScreenComparator.compare:currentValue: " + JSON.stringify(currentValue));
        DEBUG.message("ScreenComparator.compare:storedValue: " + JSON.stringify(storedValue));
        DEBUG.message("ScreenComparator.compare:config: " + JSON.stringify(config));
    }

    // if comparing against old profile
    if (storedValue === undefined) {
        return new ComparisonResult(config.penaltyPoints);
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
    if (DEBUG.messageEnabled()) {
        DEBUG.message("MultiValueComparator.compare:currentValue: " + JSON.stringify(currentValue));
        DEBUG.message("MultiValueComparator.compare:storedValue: " + JSON.stringify(storedValue));
        DEBUG.message("MultiValueComparator.compare:config: " + JSON.stringify(config));
    }

    var delimiter = ";";
    var currentValues = splitAndTrim(currentValue, delimiter);
    var storedValues = splitAndTrim(storedValue, delimiter);

    if (storedValue === null && currentValue !== null && currentValues.length === 0) {
        return new ComparisonResult(true);
    }

    var maxNumberOfElements = Math.max(currentValues.length, storedValues.length);
    var numberOfTheSameElements = calculateIntersection(currentValues, storedValues).length;
    var numberOfDifferences = maxNumberOfElements - numberOfTheSameElements;
    var percentageOfDifferences = calculatePercentage(numberOfDifferences, maxNumberOfElements);

    if (DEBUG.messageEnabled()) {
        DEBUG.message(numberOfTheSameElements + " of " + maxNumberOfElements + " are same");
    }

    if (maxNumberOfElements === 0) {
        DEBUG.message("Ignored because no attributes found in both profiles");
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (numberOfTheSameElements === maxNumberOfElements) {
        DEBUG.message("Ignored because all attributes are same");
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (numberOfDifferences > config.maxDifferences) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Would be ignored if not more than " + config.maxDifferences + " differences");
        }
        return new ComparisonResult(config.penaltyPoints);
    }

    if (percentageOfDifferences > config.maxPercentageDifference) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message(percentageOfDifferences + " percents are different");
            DEBUG.message("Would be ignored if not more than " + config.maxPercentageDifference + " percent");
        }
        return new ComparisonResult(config.penaltyPoints);
    }

     if (DEBUG.messageEnabled()) {
         DEBUG.message("Ignored because number of differences(" + numberOfDifferences + ") not more than "
                 + config.maxDifferences);
         DEBUG.message(percentageOfDifferences + " percents are different");
         DEBUG.message("Ignored because not more than " + config.maxPercentageDifference + " percent");
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
    if (DEBUG.messageEnabled()) {
        DEBUG.message("UserAgentComparator.compare:currentValue: " + JSON.stringify(currentValue));
        DEBUG.message("UserAgentComparator.compare:storedValue: " + JSON.stringify(storedValue));
        DEBUG.message("UserAgentComparator.compare:config: " + JSON.stringify(config));
    }

    if (config.ignoreVersion) {
        // remove version number
        currentValue = currentValue.replace(/[\d\.]+/g, "").trim();
        storedValue = storedValue.replace(/[\d\.]+/g, "").trim();
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
    if (DEBUG.messageEnabled()) {
        DEBUG.message("GeolocationComparator.compare:currentValue: " + JSON.stringify(currentValue));
        DEBUG.message("GeolocationComparator.compare:storedValue: " + JSON.stringify(storedValue));
        DEBUG.message("GeolocationComparator.compare:config: " + JSON.stringify(config));
    }

    // if comparing against old profile
    if (storedValue === undefined) {
        return new ComparisonResult(config.penaltyPoints);
    }

    // both null
    if ((currentValue.latitude === null || currentValue.longitude === null)
        && (storedValue.latitude === null || storedValue.longitude === null)) {
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    // current null, stored not null
    if ((currentValue.latitude === null || currentValue.longitude === null)
        && (storedValue.latitude !== null && storedValue.longitude !== null)) {
        return new ComparisonResult(config.penaltyPoints);
    }

    // current not null, stored null
    if ((currentValue.latitude !== null && currentValue.longitude !== null)
        && (storedValue.latitude === null || storedValue.longitude === null)) {
        return new ComparisonResult(config.penaltyPoints, true);
    }

    // both have values, therefore perform comparison
    var distance = calculateDistance(currentValue, storedValue);

    if (DEBUG.messageEnabled()) {
        DEBUG.message("Distance between (" + currentValue.latitude + "," + currentValue.longitude + ") and (" +
            storedValue.latitude + "," + storedValue.longitude + ") is " + distance + " miles");
    }

    if (parseFloat(distance.toPrecision(5)) === 0) {
        DEBUG.message("Location is the same");
        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

    if (distance <= config.allowedRange) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Tolerated because distance not more then " + config.allowedRange);
        }
        return new ComparisonResult(true);
    } else {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Would be ignored if distance not more then " + config.allowedRange);
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
    for (var i = 0; i < a.length; i++) {
        if (f(a[i]) === false) {
            return false;
        }
    }
    return true;
}

/**
 * Returns true if evaluating function f on any element of the Array a returns true.
 *
 * @param a: (Array) The array of elements to evaluate
 * @param f: (Function) A single argument function for mapping elements of the array to boolean.
 * @return boolean.
 */
any = function(a, f) {
    for (var i = 0; i < a.length; i++) {
        if (f(a[i]) === true) {
            return true;
        }
    }
    return false;
}

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
    var factor = (Math.PI / 180);
    function degreesToRadians(degrees) {
        return degrees * factor;
    }
    function radiansToDegrees(radians) {
        return radians / factor;
    }
    var theta = first.longitude - second.longitude;
    var dist = Math.sin(degreesToRadians(first.latitude)) * Math.sin(degreesToRadians(second.latitude))
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

    var results = [];
    if (text === null) {
        return results;
    }

    var values = text.split(delimiter);
    for (var i = 0; i < values.length; i++) {
        var value = values[i].trim();
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

// ComparisonResult

/**
 * Constructs an instance of a ComparisonResult with the given penalty points.
 *
 * @param penaltyPoints (Number) The penalty points for the comparison (defaults to 0).
 * @param additionalInfoInCurrentValue (boolean) Whether the current value contains more information
 *                                               than the stored value (defaults to false).
 */
function ComparisonResult() {

    var penaltyPoints = 0;
    var additionalInfoInCurrentValue = false;

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
 * @return boolean true if the comparison was successful.
 */
ComparisonResult.prototype.isSuccessful = function() {
    return this.penaltyPoints === null || this.penaltyPoints === 0;
};

function matchDevicePrint() {

    if (!username) {
        DEBUG.error("Username not set. Cannot compare user's device print profiles.");
        authState = FAILED;
    } else {

        var getProfiles = function () {

            function isExpiredProfile(devicePrintProfile) {
                var expirationDate = new Date();
                expirationDate.setDate(expirationDate.getDate() - config.profileExpiration);

                var lastSelectedDate = new Date(devicePrintProfile.lastSelectedDate);

                return lastSelectedDate < expirationDate;
            }

            function getNotExpiredProfiles() {
                var results = [];

                var profiles = idRepository.getAttribute(username, "devicePrintProfiles"),
                    iter = profiles.iterator(),
                    profile;
                while (iter.hasNext()) {
                    profile = JSON.parse(iter.next());
                    if (!isExpiredProfile(profile)) {
                        results.push(profile);
                    }
                }
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("stored non-expired profiles: " + JSON.stringify(results));
                }
                return results;
            }

            return getNotExpiredProfiles();
        };

        if (DEBUG.messageEnabled()) {
            DEBUG.message("client devicePrint: " + clientSideScriptOutput);
        }
        var devicePrint = JSON.parse(clientSideScriptOutput);
        var devicePrintProfiles = getProfiles();


        function getValue(obj, attributePath) {
            var parts = attributePath.split('.'),
                value = obj;
            for (var i = 0; i < parts.length; i++) {
                if (value === undefined) {
                    return null;
                }
                value = value[parts[i]];
            }
            return value;
        }

        function getDevicePrintAttributePaths(devicePrint, attributeConfig) {


            function b(devicePrint, devicePrintProfiles, attributeConfig, attributePath) {

                var attributePaths = [];

                for (var attributeName in attributeConfig) {
                    if (attributeConfig.hasOwnProperty(attributeName)) {
                        var tmp = a(devicePrint, devicePrintProfiles, attributeConfig[attributeName], attributePath + attributeName);
                        attributePaths.push(tmp);
                    }
                }

                return attributePaths;
            }

            function a(devicePrint, devicePrintProfiles, attributeConfig, attributePath) {
                if (attributeConfig.comparator !== undefined) {
                    return attributePath;

                } else {
                    return b(devicePrint, devicePrintProfiles, attributeConfig, attributePath + ".");
                }
            }

            return b(devicePrint, null, attributeConfig, "");
        }

        function hasRequiredAttributes(devicePrint, attributeConfig) {

            var attributePaths = getDevicePrintAttributePaths(devicePrint, attributeConfig);

            for (var i = 0; i < attributePaths.length; i++) {

                var attrValue = getValue(devicePrint, attributePaths[i] + "");
                var attrConfig = getValue(attributeConfig, attributePaths[i] + "");

                if (attrConfig.required && attrValue === undefined) {
                    DEBUG.warning("Device Print profile missing required attribute, " + attributePaths[i]);
                    return false;
                }
            }

            DEBUG.message("device print has required attributes");
            return true;
        }

        function compareDevicePrintProfiles(attributeConfig, devicePrint, devicePrintProfiles, maxPenaltyPoints) {

            var attributePaths = getDevicePrintAttributePaths(devicePrint, attributeConfig);

            var results = [];
            for (var j = 0; j < devicePrintProfiles.length; j++) {

                var aggregatedComparisonResult = new ComparisonResult();
                for (var i = 0; i < attributePaths.length; i++) {

                    var currentValue = getValue(devicePrint, attributePaths[i] + "");
                    var storedValue = getValue(devicePrintProfiles[j].devicePrint, attributePaths[i] + "");
                    var attrConfig = getValue(attributeConfig, attributePaths[i] + "");

                    var comparisonResult;
                    if (storedValue === null) {
                        comparisonResult = new ComparisonResult(attrConfig.penaltyPoints);
                    } else {
                        comparisonResult = attrConfig.comparator.compare(currentValue, storedValue, attrConfig.args);
                    }

                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Comparing attribute path: " + attributePaths[i]
                            + ", Comparison result: successful=" + comparisonResult.isSuccessful() + ", penaltyPoints="
                            + comparisonResult.penaltyPoints + ", additionalInfoInCurrentValue="
                            + comparisonResult.additionalInfoInCurrentValue);
                    }
                    aggregatedComparisonResult.addComparisonResult(comparisonResult);
                }
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Aggregated comparison result: successful="
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

            var selectedComparisonResult = results[0].key;
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Selected comparison result: successful=" + selectedComparisonResult.isSuccessful()
                    + ", penaltyPoints=" + selectedComparisonResult.penaltyPoints + ", additionalInfoInCurrentValue="
                    + selectedComparisonResult.additionalInfoInCurrentValue);
            }

            var selectedProfile = null;
            if (selectedComparisonResult.penaltyPoints <= maxPenaltyPoints) {
                selectedProfile = results[0].value;
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Selected profile: " + JSON.stringify(selectedProfile));
                }
            }

            if (selectedProfile === null) {
                return false;
            }

            /* update profile */
            selectedProfile.selectionCounter = selectedProfile.selectionCounter + 1;
            selectedProfile.lastSelectedDate = new Date().toISOString();
            selectedProfile.devicePrint = devicePrint;

            var vals = [];
            for (var i = 0; i < devicePrintProfiles.length; i++) {
                vals.push(JSON.stringify(devicePrintProfiles[i]));
            }

            idRepository.setAttribute(username, "devicePrintProfiles", vals);

            return true;
        }

        if (!hasRequiredAttributes(devicePrint, config.attributes)) {
            DEBUG.message("devicePrint.hasRequiredAttributes: false");
            // Will fail this module but fall-through to next module. Which should be OTP.
            authState = FAILED;
        } else if (compareDevicePrintProfiles(config.attributes, devicePrint, devicePrintProfiles, config.maxPenaltyPoints)) {
            DEBUG.message("devicePrint.hasValidProfile: true");
            authState = SUCCESS;
        } else {
            DEBUG.message("devicePrint.hasValidProfile: false");
            sharedState.put('devicePrintProfile', JSON.stringify(devicePrint));
            // Will fail this module but fall-through to next module. Which should be OTP.
            authState = FAILED;
        }
    }
}

matchDevicePrint();


//---------------------------------------------------------------------------//
//                        Unit Tests - DO NOT MODIFY                         //
//---------------------------------------------------------------------------//


getTestDetails = function() {
    // http://stackoverflow.com/questions/1340872/how-to-get-javascript-caller-function-line-number-how-to-get-javascript-caller
    function getErrorObject() {
        try { throw Error('') } catch(err) { return err; }
    }
    var err = getErrorObject();
    var caller_line = err.stack.split("\n")[6];
    // e.g. at shouldCreateComparisonResultWithZeroPenaltyPoints (eval at <anonymous> (eval at <anonymous> (http://repl.it/jsrepl/sandbox.js:39:104)), <anonymous>:247:73)
    var callee_function = caller_line.slice(caller_line.indexOf("at ")+2, caller_line.indexOf("(eval"));
    var callee_line_number = caller_line.slice(caller_line.indexOf("<anonymous>:")+12, caller_line.indexOf(":", caller_line.indexOf("<anonymous>:")+12));
    return callee_function + " #" + callee_line_number;
};

logError = function(message) {
    var test_name_and_line_number = getTestDetails();
    console.log("FAIL ["+ test_name_and_line_number + "]: " + message);
};

assertEquals = function(expected, actual) {
    if (expected !== actual) {
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
    if (expected.length !== actual.length) {
        logError("expected " + expected + " but was " + actual);
        return;
    }
    for (var i = 0; i < expected.length; i++) {
        if (expected[i] !== actual[i]) {
            logError("expected " + expected + " but was " + actual);
            return;
        }
    }
};

//DEBUG = {
//    messageEnabled: function() { return true; },
//    message: function(text) { console.log(text); }
//};

// ComparisonResult

function shouldCreateComparisonResultWithZeroPenaltyPoints() {
    // Given
    // When
    var comparisonResult = new ComparisonResult();
    // Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCreateComparisonResultWithZeroPenaltyPoints();

function shouldCreateComparisonResultWithZeroPenaltyPointsUsingConstant() {
    // Given
    // When
    var comparisonResult = ComparisonResult.ZERO_PENALTY_POINTS;
    // Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCreateComparisonResultWithZeroPenaltyPointsUsingConstant();

function shouldCreateComparisonResultWithPenaltyPointsAndAdditionalInfo() {
    // Given
    // When
    var comparisonResult = new ComparisonResult(11, true);
    // Then
    assertEquals(11, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCreateComparisonResultWithPenaltyPointsAndAdditionalInfo();

function shouldCreateComparisonResultWithPenaltyPoints() {
    // Given
    // When
    var comparisonResult = new ComparisonResult(111);
    // Then
    assertEquals(comparisonResult.penaltyPoints, 111);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCreateComparisonResultWithPenaltyPoints();

function shouldCreateComparisonResultWithAdditionalInfo() {
    // Given
    // When
    var comparisonResult = new ComparisonResult(true);
    // Then
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
    var anotherComparisonResult = null;
    //When
    var result = ComparisonResult.compare(comparisonResult, anotherComparisonResult);
    //Then
    assertEquals(1, result);
}
shouldCompareComparisonResultsWithNull();

// ScalarComparator

function shouldCompareWithNoPenaltyPoints() {
    //Given
    var currentValue = "CURRENT_VALUE";
    var storedValue = "STORED_VALUE";
    var config = {"penaltyPoints": 0};
    //When
    var comparisonResult = ScalarComparator.compare(currentValue, storedValue, config);
    //Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareWithNoPenaltyPoints();

function shouldCompareWhenStoredValueIsDifferentToCurrentValue() {
    //Given
    var currentValue = "CURRENT_VALUE";
    var storedValue = "STORED_VALUE";
    var config = {"penaltyPoints": 10};
    //When
    var comparisonResult = ScalarComparator.compare(currentValue, storedValue, config);
    //Then
    assertEquals(10, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareWhenStoredValueIsDifferentToCurrentValue();

function shouldCompareWhenStoredValueIsNotNullAndCurrentValueIsNull() {
    //Given
    var currentValue = null;
    var storedValue = "STORED_VALUE";
    var config = {"penaltyPoints": 10};
    //When
    var comparisonResult = ScalarComparator.compare(currentValue, storedValue, config);
    //Then
    assertEquals(10, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareWhenStoredValueIsNotNullAndCurrentValueIsNull();

function shouldCompareWhenStoredValueIsNullAndCurrentValueIsNotNull() {
    //Given
    var currentValue = "CURRENT_VALUE";
    var storedValue = null;
    var config = {"penaltyPoints": 10};
    //When
    var comparisonResult = ScalarComparator.compare(currentValue, storedValue, config);
    //Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareWhenStoredValueIsNullAndCurrentValueIsNotNull();

// ScreenComparator

function shouldCompareScreensThatAreEqual() {
    // Given
    var currentValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
    var storedValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
    var config = {"penaltyPoints": 10};
    // When
    var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
    // Then
    assertTrue(comparisonResult.isSuccessful());
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareScreensThatAreEqual();

function shouldCompareScreensThatAreNull() {
    // Given
    var currentValue = {"screenWidth": null, "screenHeight": null, "screenColourDepth": null};
    var storedValue = {"screenWidth": null, "screenHeight": null, "screenColourDepth": null};
    var config = {"penaltyPoints": 10};
    // When
    var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
    // Then
    assertTrue(comparisonResult.isSuccessful());
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareScreensThatAreNull();

function shouldCompareScreensWhenStoredScreenWidthIsNull() {
    // Given
    var currentValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
    var storedValue = {"screenWidth": null, "screenHeight": 1200, "screenColourDepth": 24};
    var config = {"penaltyPoints": 10};
    // When
    var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
    // Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareScreensWhenStoredScreenWidthIsNull();

function shouldCompareScreensWhenStoredScreenWidthIsDifferentToCurrentScreenWidth() {
    // Given
    var currentValue = {"screenWidth": 800, "screenHeight": 1200, "screenColourDepth": 24};
    var storedValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
    var config = {"penaltyPoints": 10};
    // When
    var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
    // Then
    assertEquals(10, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareScreensWhenStoredScreenWidthIsDifferentToCurrentScreenWidth();

function shouldCompareScreensWhenStoredScreenHeightIsNull() {
    // Given
    var currentValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
    var storedValue = {"screenWidth": 1920, "screenHeight": null, "screenColourDepth": 24};
    var config = {"penaltyPoints": 10};
    // When
    var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
    // Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareScreensWhenStoredScreenHeightIsNull();

function shouldCompareScreensWhenStoredScreenHeightIsDifferentToCurrentScreenHeight() {
    // Given
    var currentValue = {"screenWidth": 1920, "screenHeight": 800, "screenColourDepth": 24};
    var storedValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
    var config = {"penaltyPoints": 10};
    // When
    var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
    // Then
    assertEquals(10, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareScreensWhenStoredScreenHeightIsDifferentToCurrentScreenHeight();

function shouldCompareScreensWhenStoredScreenColourDepthIsNull() {
    // Given
    var currentValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
    var storedValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": null};
    var config = {"penaltyPoints": 10};
    // When
    var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
    // Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareScreensWhenStoredScreenColourDepthIsNull();

function shouldCompareScreensWhenStoredScreenColourDepthIsDifferentToCurrentScreenColourDepth() {
    // Given
    var currentValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 16};
    var storedValue = {"screenWidth": 1920, "screenHeight": 1200, "screenColourDepth": 24};
    var config = {"penaltyPoints": 10};
    // When
    var comparisonResult = ScreenComparator.compare(currentValue, storedValue, config);
    // Then
    assertEquals(10, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareScreensWhenStoredScreenColourDepthIsDifferentToCurrentScreenColourDepth();

// MultiValueComparator

function shouldCompareMultiValueStringsWhenStoredValueIsNullAndCurrentValueIsEmpty() {
    //Given
    var currentValue = "";
    var storedValue = null;
    var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
    //When
    var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertEquals(comparisonResult.penaltyPoints, 0);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringsWhenStoredValueIsNullAndCurrentValueIsEmpty();

function shouldCompareMultiValueStringsWhenBothAreEmpty() {
    //Given
    var currentValue = "";
    var storedValue = "";
    var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
    //When
    var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringsWhenBothAreEmpty();

function shouldCompareMultiValueStringsWhenBothAreEqual() {
    //Given
    var currentValue = "VALUE_A; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var storedValue = "VALUE_A; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
    //When
    var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringsWhenBothAreEqual();

function shouldCompareMultiValueStringWhenThereAreLessDifferencesThanMax() {
    //Given
    var currentValue = "VALUE_AA; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
    //When
    var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertEquals(0, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringWhenThereAreLessDifferencesThanMax();

function shouldCompareMultiValueStringWhenThereAreMoreDifferencesThanMax() {
    //Given
    var currentValue = "VALUE_AA; VALUE_BB; VALUE_C; VALUE_D; VALUE_E";
    var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
    //When
    var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringWhenThereAreMoreDifferencesThanMax();

function shouldCompareMultiValueStringWhenThereIsLessPercentageDiffThanMax() {
    //Given
    var currentValue = "VALUE_AA; VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
    //When
    var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertEquals(0, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringWhenThereIsLessPercentageDiffThanMax();

function shouldCompareMultiValueStringWhenThereIsMorePercentageDiffThanMax() {
    //Given
    var currentValue = "VALUE_AA; VALUE_BB; VALUE_C; VALUE_D; VALUE_E";
    var storedValue = "VALUE_B; VALUE_C; VALUE_D; VALUE_E";
    var config = {"maxPercentageDifference": 20, "maxDifferences": 1, "penaltyPoints": 111};
    //When
    var comparisonResult = MultiValueComparator.compare(currentValue, storedValue, config);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareMultiValueStringWhenThereIsMorePercentageDiffThanMax();

// UserAgentComparator

function shouldCompareUserAgents() {
    //Given
    var currentValue = "USER_AGENT_1234567890.";
    var storedValue = "1234USER_.567890AGENT_";
    var config = {"ignoreVersion": false, "penaltyPoints": 10};
    //When
    var comparisonResult = UserAgentComparator.compare(currentValue, storedValue, config);
    //Then
    assertEquals(10, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareUserAgents();

function shouldCompareUserAgentsIgnoringVersionNumbers() {
    //Given
    var currentValue = "USER_AGENT_1234567890.";
    var storedValue = "1234USER_.567890AGENT_";
    var config = {"ignoreVersion": true, "penaltyPoints": 10};
    //When
    var comparisonResult = UserAgentComparator.compare(currentValue, storedValue, config);
    //Then
    assertEquals(0, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareUserAgentsIgnoringVersionNumbers();

// GeolocationComparator

function shouldCompareLocationWhenBothXsAreNull() {
    //Given
    var current = {"latitude": null, "longitude": 2.0};
    var stored = {"latitude": null, "longitude": 2.0};
    var config =  {"allowedRange": 100, "penaltyPoints": 111};
    //When
    var comparisonResult = GeolocationComparator.compare(current, stored, config);
    //Then
    assertTrue(comparisonResult.isSuccessful());
}
shouldCompareLocationWhenBothXsAreNull();

function shouldCompareLocationWhenBothYsAreNull() {
    //Given
    var current = {"latitude": 2.0, "longitude": null};
    var stored = {"latitude": 2.0, "longitude": null};
    var config =  {"allowedRange": 100, "penaltyPoints": 111};
    //When
    var comparisonResult = GeolocationComparator.compare(current, stored, config);
    //Then
    assertTrue(comparisonResult.isSuccessful());
}
shouldCompareLocationWhenBothYsAreNull();

function shouldCompareLocationWhenCurrentXIsNull() {
    //Given
    var current = {"latitude": null, "longitude": 2.0};
    var stored = {"latitude": 2.0, "longitude": 2.0};
    var config =  {"allowedRange": 100, "penaltyPoints": 111};
    //When
    var comparisonResult = GeolocationComparator.compare(current, stored, config);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationWhenCurrentXIsNull();

function shouldCompareLocationWhenCurrentYIsNull() {
    //Given
    var current = {"latitude": 2.0, "longitude": null};
    var stored = {"latitude": 2.0, "longitude": 2.0};
    var config =  {"allowedRange": 100, "penaltyPoints": 111};
    //When
    var comparisonResult = GeolocationComparator.compare(current, stored, config);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationWhenCurrentYIsNull();

function shouldCompareLocationWhenStoredXIsNull() {
    //Given
    var current = {"latitude": 2.0, "longitude": 2.0};
    var stored = {"latitude": null, "longitude": 2.0};
    var config =  {"allowedRange": 100, "penaltyPoints": 111};
    //When
    var comparisonResult = GeolocationComparator.compare(current, stored, config);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationWhenStoredXIsNull();

function shouldCompareLocationWhenStoredYIsNull() {
    //Given
    var current = {"latitude": 2.0, "longitude": 2.0};
    var stored = {"latitude": 2.0, "longitude": null};
    var config =  {"allowedRange": 100, "penaltyPoints": 111};
    //When
    var comparisonResult = GeolocationComparator.compare(current, stored, config);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(111, comparisonResult.penaltyPoints);
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationWhenStoredYIsNull();

function shouldCompareLocationsThatAreEqual() {
    //Given
    var current = {"latitude": 2.0, "longitude": 2.0};
    var stored = {"latitude": 2.0, "longitude": 2.0};
    var config =  {"allowedRange": 100, "penaltyPoints": 111};
    //When
    var comparisonResult = GeolocationComparator.compare(current, stored, config);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationsThatAreEqual();

function shouldCompareLocationsThatAreWithinTolerableRange() {
    //Given
    var current = {"latitude": 3.0, "longitude": 3.0};
    var stored = {"latitude": 2.0, "longitude": 2.0};
    var config =  {"allowedRange": 100, "penaltyPoints": 111};
    //When
    var comparisonResult = GeolocationComparator.compare(current, stored, config);
    //Then
    assertTrue(comparisonResult.isSuccessful());
    assertTrue(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationsThatAreWithinTolerableRange();

function shouldCompareLocationsThatAreOutsideTolerableRange() {
    //Given
    var current = {"latitude": 20.0, "longitude": 20.0};
    var stored = {"latitude": 2.0, "longitude": 2.0};
    var config =  {"allowedRange": 100, "penaltyPoints": 111};
    //When
    var comparisonResult = GeolocationComparator.compare(current, stored, config);
    //Then
    assertFalse(comparisonResult.isSuccessful());
    assertEquals(comparisonResult.penaltyPoints, 111);
    assertFalse(comparisonResult.additionalInfoInCurrentValue);
}
shouldCompareLocationsThatAreOutsideTolerableRange();
