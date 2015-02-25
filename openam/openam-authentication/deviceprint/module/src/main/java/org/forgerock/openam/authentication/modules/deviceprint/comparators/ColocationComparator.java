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
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authentication.modules.deviceprint.comparators;

import java.math.BigDecimal;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.authentication.modules.deviceprint.DevicePrintModule;

/**
 * Comparator for comparing two locations.
 */
public class ColocationComparator {
	
	private static final Debug DEBUG = Debug.getInstance("amAuthDevicePrint");

    /**
     * Compares two locations, taking into account a degree of difference.
     *
     * @param currentLatitude The current latitude.
     * @param currentLongitude The current longitude.
     * @param storedLatitude The stored latitude.
     * @param storedLongitude The current longitude.
     * @param maxToleratedDistance The max difference allowed in the two locations, before the penalty is assigned.
     * @param differencePenaltyPoints The number of penalty points.
     * @return A ComparisonResult.
     */
	public ComparisonResult compare(Double currentLatitude, Double currentLongitude, Double storedLatitude,
            Double storedLongitude, long maxToleratedDistance, long differencePenaltyPoints) {
		
		if (bothNull(currentLatitude, currentLongitude, storedLatitude, storedLongitude)) {
			return ComparisonResult.ZERO_PENALTY_POINTS;
		}
		
		if (currentNullStoredNotNull(currentLatitude, currentLongitude, storedLatitude, storedLongitude)) {
			return new ComparisonResult(differencePenaltyPoints);
		}
		
		if (storedNullCurrentNotNull(currentLatitude, currentLongitude, storedLatitude, storedLongitude)) {
			return new ComparisonResult(differencePenaltyPoints, true);
		}

		BigDecimal distance = calculateDistance(currentLatitude, currentLongitude, storedLatitude, storedLongitude);
		
		if (DEBUG.messageEnabled()) {
			DEBUG.message("Distance between (" + currentLatitude + "," + currentLongitude + ") and (" + storedLatitude
                    + "," + storedLongitude + ") is " + distance + " miles");
		}
		
		if (distance.compareTo(BigDecimal.ZERO) == 0) {
			if (DEBUG.messageEnabled()) {
				DEBUG.message("Location is the same");
			}
			return ComparisonResult.ZERO_PENALTY_POINTS;
		}
		
		boolean inMaxToleratedRange = isInMaxToleratedRange(distance,maxToleratedDistance);
		if (inMaxToleratedRange) {
			if (DEBUG.messageEnabled()) {
				DEBUG.message("Tolerated because distance not more then "+maxToleratedDistance);
			}
			return new ComparisonResult(true);
		} else {
			if (DEBUG.messageEnabled()) {
				DEBUG.message("Would be ignored if distance not more then "+maxToleratedDistance);
			}
			return new ComparisonResult(differencePenaltyPoints);
		}
	}

    /**
     * Checks if the stored location is null and the current location is not null.
     *
     * @param currentLatitude The current latitude.
     * @param currentLongitude The current longitude.
     * @param storedLatitude The stored latitude.
     * @param storedLongitude The current longitude.
     * @return If the check passes true, otherwise false.
     */
	private boolean storedNullCurrentNotNull(Double currentLatitude, Double currentLongitude, Double storedLatitude,
            Double storedLongitude) {
		return !atLeastOneNull(currentLatitude,currentLongitude) && atLeastOneNull(storedLatitude,storedLongitude);
	}

    /**
     * Checks if the current location is null and the stored location is not null.
     *
     * @param currentLatitude The current latitude.
     * @param currentLongitude The current longitude.
     * @param storedLatitude The stored latitude.
     * @param storedLongitude The current longitude.
     * @return If the check passes true, otherwise false.
     */
	private boolean currentNullStoredNotNull(Double currentLatitude, Double currentLongitude, Double storedLatitude,
            Double storedLongitude) {
		return atLeastOneNull(currentLatitude,currentLongitude) && !atLeastOneNull(storedLatitude,storedLongitude);
	}

    /**
     * Checks if the stored location is null and the current location is null.
     *
     * @param currentLatitude The current latitude.
     * @param currentLongitude The current longitude.
     * @param storedLatitude The stored latitude.
     * @param storedLongitude The current longitude.
     * @return If the check passes true, otherwise false.
     */
	private boolean bothNull(Double currentLatitude, Double currentLongitude, Double storedLatitude,
            Double storedLongitude) {
		return atLeastOneNull(currentLatitude,currentLongitude) && atLeastOneNull(storedLatitude,storedLongitude);
	}

    /**
     * Checks to see if the x or y co-ordinates is null.
     *
     * @param first The x co-ordinates.
     * @param second The y co-ordinates.
     * @return If either are null.
     */
	private boolean atLeastOneNull(Double first, Double second) {
		return first == null || second == null;
	}

    /**
     * Whether the distance between the two locations is within the allowed difference.
     *
     * @param distance The actual distance between the locations.
     * @param maxToleratedDistance The max difference allowed between the locations.
     * @return True is the check passes, otherwise false.
     */
	private boolean isInMaxToleratedRange(BigDecimal distance, Long maxToleratedDistance) {
		return distance.compareTo(BigDecimal.valueOf(maxToleratedDistance))<=0;
	}

    /**
     * Calculates the distances between the two locations.
     *
     * @param currentLatitude The current latitude.
     * @param currentLongitude The current longitude.
     * @param storedLatitude The stored latitude.
     * @param storedLongitude The stored longitude.
     * @return The distance between the two locations.
     */
	private BigDecimal calculateDistance(double currentLatitude, double currentLongitude, double storedLatitude,
            double storedLongitude) {
		double theta = currentLongitude - storedLongitude;
		double dist = Math.sin(deg2rad(currentLatitude)) * Math.sin(deg2rad(storedLatitude))
				+ Math.cos(deg2rad(currentLatitude)) * Math.cos(deg2rad(storedLatitude))
				* Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;

        return BigDecimal.valueOf(dist).setScale(5, BigDecimal.ROUND_HALF_EVEN);
	}

	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	private double rad2deg(double rad) {
		return (rad * 180.0 / Math.PI);
	}
}
