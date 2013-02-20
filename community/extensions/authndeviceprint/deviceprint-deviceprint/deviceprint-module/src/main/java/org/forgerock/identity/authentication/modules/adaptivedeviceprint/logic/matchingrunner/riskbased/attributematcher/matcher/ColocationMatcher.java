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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher;

import java.math.BigDecimal;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;

import com.sun.identity.shared.debug.Debug;

public final class ColocationMatcher {
	
	private ColocationMatcher() {
		super();
	}

	private static final Debug debug = Debug.getInstance(ColocationMatcher.class.getName());

	public static ComparisonResult getComparationResult(Double currentLatitude,Double currentLongitude,Double storedLatitude,Double storedLongitude,Long maxToleratedDistance, Long differencePenaltyPoints) {
		
		if(bothNull(currentLatitude, currentLongitude, storedLatitude, storedLongitude)){
			return new ComparisonResult();
		}
		
		if(currentNullStoredNotNull(currentLatitude, currentLongitude, storedLatitude, storedLongitude)){
			return new ComparisonResult(differencePenaltyPoints);
		}
		
		if(storedNullCurrentNotNull(currentLatitude, currentLongitude, storedLatitude, storedLongitude)){
			return new ComparisonResult(differencePenaltyPoints, true);
		}
		

		BigDecimal distance = calculateDistance(currentLatitude, currentLongitude, storedLatitude, storedLongitude);
		
		if (debug.messageEnabled()) {
			debug.message("Distance between ("+currentLatitude+","+currentLongitude+") and ("+storedLatitude+","+storedLongitude+") is "+distance+" miles");
		}
		
		if(distance.compareTo(BigDecimal.ZERO)==0){
			if (debug.messageEnabled()) {
				debug.message("Location is the same");
			}
			return new ComparisonResult();
		}
		
		boolean inMaxToleratedRange = isInMaxToleratedRange(distance,maxToleratedDistance);
		if(inMaxToleratedRange){
			if (debug.messageEnabled()) {
				debug.message("Tolerated because distance not more then "+maxToleratedDistance);
			}
			return new ComparisonResult(true);
		}else{
			if (debug.messageEnabled()) {
				debug.message("Would be ignored if distance not more then "+maxToleratedDistance);
			}
			return new ComparisonResult(differencePenaltyPoints);
		}
	}

	private static boolean storedNullCurrentNotNull(Double currentLatitude,	Double currentLongitude, Double storedLatitude, Double storedLongitude) {
		return atLeastOneNull(currentLatitude,currentLongitude) && !atLeastOneNull(storedLatitude,storedLongitude);
	}

	private static boolean currentNullStoredNotNull(Double currentLatitude,	Double currentLongitude, Double storedLatitude,	Double storedLongitude) {
		return !atLeastOneNull(currentLatitude,currentLongitude) && atLeastOneNull(storedLatitude,storedLongitude);
	}

	private static boolean bothNull(Double currentLatitude, Double currentLongitude, Double storedLatitude,	Double storedLongitude) {
		return atLeastOneNull(currentLatitude,currentLongitude) && atLeastOneNull(storedLatitude,storedLongitude);
	}

	private static boolean atLeastOneNull(Double first, Double second) {
		return first==null || second==null;
	}

	private static boolean isInMaxToleratedRange(BigDecimal distance, Long maxToleratedDistance) {
		return distance.compareTo(BigDecimal.valueOf(maxToleratedDistance))<=0;
	}

	public static BigDecimal calculateDistance(double currentLatitude,	double currentLongitude, double storedLatitude,	double storedLongitude) {
		double distance = distance(currentLatitude, currentLongitude, storedLatitude, storedLongitude);
		return BigDecimal.valueOf(distance).setScale(5, BigDecimal.ROUND_HALF_EVEN);
	}

	private static double distance(double lat1, double lon1, double lat2,  double lon2) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
				+ Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
				* Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		return dist;
	}

	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	private static double rad2deg(double rad) {
		return (rad * 180.0 / Math.PI);
	}
	
}
