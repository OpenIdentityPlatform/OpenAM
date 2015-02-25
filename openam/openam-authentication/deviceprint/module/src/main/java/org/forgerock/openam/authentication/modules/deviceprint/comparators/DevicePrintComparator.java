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
 * Portions Copyrighted 2013-2014 ForgeRock Inc.
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd
 */

package org.forgerock.openam.authentication.modules.deviceprint.comparators;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.authentication.modules.deviceprint.DevicePrintAuthenticationConfig;
import org.forgerock.openam.authentication.modules.deviceprint.DevicePrintModule;
import org.forgerock.openam.authentication.modules.deviceprint.model.DevicePrint;

/**
 * Comparator for comparing two Device Print objects to determine how similar they are based from the penalty points
 * assigned to each attribute on the Device Print object.
 */
public class DevicePrintComparator {

    private static final Debug DEBUG = Debug.getInstance("amAuthDevicePrint");

    private final MultiValueAttributeComparator multiValueAttributeComparator;
    private final ColocationComparator colocationComparator;

    /**
     * Constructs an instance of the DevicePrintComparator.
     *
     * @param multiValueAttributeComparator An instance of the MultiValueAttributeComparator.
     * @param colocationComparator An instance of the ColocationComparator.
     */
    public DevicePrintComparator(MultiValueAttributeComparator multiValueAttributeComparator,
            ColocationComparator colocationComparator) {
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
    public ComparisonResult compare(DevicePrint currentDevicePrint, DevicePrint storedDevicePrint,
            DevicePrintAuthenticationConfig config) {

        ComparisonResult aggregatedComparisonResult = new ComparisonResult();

        ComparisonResult userAgentComparisonResult = compareUserAgent(
                currentDevicePrint.getUserAgent(), storedDevicePrint.getUserAgent(),
                config.getLong(DevicePrintAuthenticationConfig.USER_AGENT_PENALTY_POINTS),
                config.getBoolean(DevicePrintAuthenticationConfig.IGNORE_VERSION_IN_USER_AGENT));

        aggregatedComparisonResult.addComparisonResult(userAgentComparisonResult);


        ComparisonResult installedFontsComparisonResult = compare(
                currentDevicePrint.getInstalledFonts(), storedDevicePrint.getInstalledFonts(),
                config.getInt(DevicePrintAuthenticationConfig.MAX_TOLERATED_DIFFS_IN_INSTALLED_FONTS),
                config.getInt(DevicePrintAuthenticationConfig.MAX_TOLERATED_PERCENTAGE_TO_MARK_AS_DIFFERENT_INSTALLED_FONTS),
                config.getLong(DevicePrintAuthenticationConfig.INSTALLED_FONTS_PENALTY_POINTS));

        aggregatedComparisonResult.addComparisonResult(installedFontsComparisonResult);


        ComparisonResult installedPluginsComparisonResult = compare(
                currentDevicePrint.getInstalledPlugins(), storedDevicePrint.getInstalledPlugins(),
                config.getInt(DevicePrintAuthenticationConfig.MAX_TOLERATED_DIFFS_IN_INSTALLED_PLUGINS),
                config.getInt(DevicePrintAuthenticationConfig.MAX_TOLERATED_PERCENTAGE_TO_MARK_AS_DIFFERENT_PLUGINS),
                config.getLong(DevicePrintAuthenticationConfig.INSTALLED_PLUGINS_PENALTY_POINTS));

        aggregatedComparisonResult.addComparisonResult(installedPluginsComparisonResult);


        ComparisonResult colorDepthComparisonResult = compare(
                currentDevicePrint.getScreenColourDepth(), storedDevicePrint.getScreenColourDepth(),
                config.getLong(DevicePrintAuthenticationConfig.SCREEN_COLOUR_DEPTH_PENALTY_POINTS));

        aggregatedComparisonResult.addComparisonResult(colorDepthComparisonResult);


        ComparisonResult timezoneComparisonResult = compare(currentDevicePrint.getTimezone(),
                storedDevicePrint.getTimezone(),
                config.getLong(DevicePrintAuthenticationConfig.TIMEZONE_PENALTY_POINTS));

        aggregatedComparisonResult.addComparisonResult(timezoneComparisonResult);


        ComparisonResult screenResolutionComparisonResult = compareScreenResolution(
                currentDevicePrint.getScreenWidth(), currentDevicePrint.getScreenHeight(),
                storedDevicePrint.getScreenWidth(), storedDevicePrint.getScreenHeight(),
                config.getLong(DevicePrintAuthenticationConfig.SCREEN_RESOLUTION_PENALTY_POINTS));

        aggregatedComparisonResult.addComparisonResult(screenResolutionComparisonResult);


        ComparisonResult locationComparisonResult = compare(currentDevicePrint.getLatitude(),
                currentDevicePrint.getLongitude(), storedDevicePrint.getLatitude(), storedDevicePrint.getLongitude(),
                config.getLong(DevicePrintAuthenticationConfig.LOCATION_ALLOWED_RANGE),
                config.getLong(DevicePrintAuthenticationConfig.LOCATION_PENALTY_POINTS));

        aggregatedComparisonResult.addComparisonResult(locationComparisonResult);


        if (DEBUG.messageEnabled()) {
            DEBUG.message("Compared device current print: " + currentDevicePrint);
            DEBUG.message("Compared stored device print: " + storedDevicePrint);
            DEBUG.message("Penalty points: " + aggregatedComparisonResult.getPenaltyPoints());
            DEBUG.message("UserAgent: " + userAgentComparisonResult + ", fonts: " + installedFontsComparisonResult
                    + ", plugins: " + installedPluginsComparisonResult + ", colourDepth: " + colorDepthComparisonResult
                    + ", timezone: " + timezoneComparisonResult + ", screenRes: " + screenResolutionComparisonResult
                    + ", location: " + locationComparisonResult);
        }

        return aggregatedComparisonResult;
    }

    /**
     * Compares two Strings and if they are equal then returns a ComparisonResult with zero penalty points assigned,
     * otherwise returns a ComparisonResult with the given number of penalty points assigned.
     *
     * @param currentValue The current value.
     * @param storedValue The stored value.
     * @param penaltyPoints The number of penalty points.
     * @return A ComparisonResult.
     */
    public ComparisonResult compare(String currentValue, String storedValue, long penaltyPoints) {

        if (penaltyPoints == 0L) {
            return ComparisonResult.ZERO_PENALTY_POINTS;
        }

        if (storedValue != null) {
            if (currentValue == null || !currentValue.equals(storedValue)) {
                return new ComparisonResult(penaltyPoints);
            }
        } else if (currentValue != null) {
            return new ComparisonResult(true);
        }

        return ComparisonResult.ZERO_PENALTY_POINTS;
    }

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
    public ComparisonResult compareUserAgent(String currentValue, String storedValue, long penaltyPoints,
            boolean ignoreVersion) {

        if (ignoreVersion) {
            // remove version number
            currentValue = currentValue.replaceAll("[[0-9]\\.]+", "").trim();
            storedValue = storedValue.replaceAll("[[0-9]\\.]+", "").trim();
        }

        return compare(currentValue, storedValue, penaltyPoints);
    }

    /**
     * Compares two Strings of comma separated values and if they are equal then returns a ComparisonResult with zero
     * penalty points assigned, otherwise returns a ComparisonResult with the given number of penalty points assigned.
     *
     * @param currentValue The current value.
     * @param storedValue The stored value.
     * @param maxDifferences The max number of differences in the values, before the penalty points are assigned.
     * @param maxDifferencesPercentage The max difference percentage in the values, before the penalty is assigned.
     * @param penaltyPoints The number of penalty points.
     * @return A ComparisonResult.
     */
    public ComparisonResult compare(String currentValue, String storedValue, int maxDifferences,
            int maxDifferencesPercentage, long penaltyPoints) {
        return multiValueAttributeComparator.compare(currentValue, storedValue, maxDifferencesPercentage, maxDifferences,
                penaltyPoints);
    }

    /**
     * Compares two locations and if they are equal then returns a ComparisonResult with zero penalty points assigned,
     * otherwise returns a ComparisonResult with the given number of penalty points assigned.
     *
     * @param currentLatitude The current latitude.
     * @param currentLongitude The current longitude.
     * @param storedLatitude The stored latitude.
     * @param storedLongitude The stored longitude.
     * @param maxToleratedDistance The max difference allowed in the two locations, before the penalty is assigned.
     * @param penaltyPoints The number of penalty points.
     * @return A ComparisonResult.
     */
    public ComparisonResult compare(Double currentLatitude, Double currentLongitude, Double storedLatitude,
            Double storedLongitude, long maxToleratedDistance, long penaltyPoints) {
        return colocationComparator.compare(currentLatitude, currentLongitude, storedLatitude, storedLongitude,
                maxToleratedDistance, penaltyPoints);
    }

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
    public ComparisonResult compareScreenResolution(String currentWidth, String currentHeight, String storedWidth,
            String storedHeight, long penaltyPoints) {

        ComparisonResult widthComparisonResult = compare(currentWidth, storedWidth, penaltyPoints);
        ComparisonResult heightComparisonResult = compare(currentHeight, storedHeight, penaltyPoints);

        if (widthComparisonResult.isSuccessful() && heightComparisonResult.isSuccessful()) {
            return new ComparisonResult(widthComparisonResult.getAdditionalInfoInCurrentValue()
                    || heightComparisonResult.getAdditionalInfoInCurrentValue());
        } else {
            return new ComparisonResult(penaltyPoints);
        }
    }
}
