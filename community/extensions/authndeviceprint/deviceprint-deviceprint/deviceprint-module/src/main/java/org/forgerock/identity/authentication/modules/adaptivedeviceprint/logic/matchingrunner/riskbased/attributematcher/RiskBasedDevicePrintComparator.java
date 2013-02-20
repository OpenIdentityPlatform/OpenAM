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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher;

import static org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher.AttributeComparatorHelper.compareAttribute;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.RiskBasedRunnerConfig;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher.AdvancedMatcher;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher.ColocationMatcher;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher.MultiValueAttributeMatcher;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;

import com.sun.identity.shared.debug.Debug;

public class RiskBasedDevicePrintComparator {
	
	private static final Debug debug = Debug
			.getInstance(RiskBasedDevicePrintComparator.class.getName());

	public ComparisonResult compareDevicePrintsPart(
			DevicePrint currentDevicePrint, DevicePrint storedDevicePrint,
			RiskBasedRunnerConfig config) {
		
		ComparisonResult aggregatedComparisonResult = new ComparisonResult();
		
		ComparisonResult userAgentComparisonResult = AdvancedMatcher.getComparationResult(
				currentDevicePrint.getUserAgent(), storedDevicePrint.getUserAgent(), 
				config.getUserAgentRebuildPatterns(), config.getUserAgentPenaltyPoints());
		aggregatedComparisonResult.addComparisonReslt(userAgentComparisonResult);
				
		ComparisonResult installedFontsComparisonResult = MultiValueAttributeMatcher.getComparationResult(
				currentDevicePrint.getInstalledFonts(), storedDevicePrint.getInstalledFonts(), 
				RiskBasedRunnerConfig.FONTS_DELIMITER, config.getMaxToleratedPercentageToMarkAsDifferentInstalledFonts(),
				config.getMaxToleratedNumberOfDifferenceInInstalledFonts(), config.getInstalledFontsPenaltyPoints());
		aggregatedComparisonResult.addComparisonReslt(installedFontsComparisonResult);
		
		ComparisonResult installedPluginsComparisonResult = MultiValueAttributeMatcher.getComparationResult(
				currentDevicePrint.getInstalledPlugins(), storedDevicePrint.getInstalledPlugins(), 
				RiskBasedRunnerConfig.PLUGINS_DELIMITER, config.getMaxToleratedPercentageToMarkAsDifferentInstalledPlugins(),
				config.getMaxToleratedNumberOfDifferenceInInstalledPlugins(),config.getInstalledPluginsPenaltyPoints());
		aggregatedComparisonResult.addComparisonReslt(installedPluginsComparisonResult);
		
		ComparisonResult persistentCookieComparisonResult = compareAttribute(currentDevicePrint.getPersistentCookie(), storedDevicePrint.getPersistentCookie(), config.getPersistentCookiePenaltyPoints());
		aggregatedComparisonResult.addComparisonReslt(persistentCookieComparisonResult);
		
		ComparisonResult colorDepthComparisonResult = compareAttribute(currentDevicePrint.getScreenColorDepth(), storedDevicePrint.getScreenColorDepth(), config.getScreenColorDepthPenaltyPoints());
		aggregatedComparisonResult.addComparisonReslt(colorDepthComparisonResult);
		
		ComparisonResult timezoneComparisonResult = compareAttribute(currentDevicePrint.getTimezone(), storedDevicePrint.getTimezone(), config.getTimezonePenaltyPoints());
		aggregatedComparisonResult.addComparisonReslt(timezoneComparisonResult);
		
		ComparisonResult screenResolutionComparisonResult = getScreenResolutionComparisonResult(currentDevicePrint, storedDevicePrint, config);
		aggregatedComparisonResult.addComparisonReslt(screenResolutionComparisonResult);
		
		ComparisonResult locationComparisonResult = ColocationMatcher.getComparationResult(
				currentDevicePrint.getLatitude(), currentDevicePrint.getLongitude(), 
				storedDevicePrint.getLatitude(), storedDevicePrint.getLongitude(), 
				config.getLocationAllowedRange(), config.getLocationPenaltyPoints());
		aggregatedComparisonResult.addComparisonReslt(locationComparisonResult);
		
		if(debug.messageEnabled()) {
			debug.message("Compared device current print: " + currentDevicePrint);
			debug.message("Compared stored device print: " + storedDevicePrint);
			debug.message("Penalty points");
			debug.message("UserAgent " + userAgentComparisonResult + " fonts:" + installedFontsComparisonResult + " plugins" + installedPluginsComparisonResult);
			debug.message("cookie: " + persistentCookieComparisonResult + " colorDepth: " + colorDepthComparisonResult + " timezone:" + timezoneComparisonResult + " screenResolution:" + screenResolutionComparisonResult);
		}
		
		return aggregatedComparisonResult;
	}

	
	private ComparisonResult getScreenResolutionComparisonResult(DevicePrint currentDevicePrint, DevicePrint storedDevicePrint,
			RiskBasedRunnerConfig config) {
		Long screenResolutionPenaltyPoints = config.getScreenResolutionPenaltyPoints();

		ComparisonResult widthComparisonResult = compareAttribute(currentDevicePrint.getScreenWidth(),storedDevicePrint.getScreenWidth(), screenResolutionPenaltyPoints);
		ComparisonResult heightComparisonResult = compareAttribute(currentDevicePrint.getScreenHeight(), storedDevicePrint.getScreenHeight(), screenResolutionPenaltyPoints);

		if(widthComparisonResult.isSuccessful() && heightComparisonResult.isSuccessful()) {
			return new ComparisonResult(widthComparisonResult.getAdditionalInfoInCurrentValue() || heightComparisonResult.getAdditionalInfoInCurrentValue()); 
		} else {
			return new ComparisonResult(screenResolutionPenaltyPoints);
		}
	}
	
}
