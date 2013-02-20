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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher.AttributeRebuilder;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher.AttributeRebuilders;
import org.forgerock.identity.authentication.modules.common.config.AttributeNameMapping;

public class RiskBasedRunnerConfig {
	
	public static final char FONTS_DELIMITER = ',';

	public static final char PLUGINS_DELIMITER = ',';
	
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-screen-color-depth-penalty-points")
	private Long screenColorDepthPenaltyPoints = 0L;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-screen-resoultion-penalty-points")
	private Long screenResolutionPenaltyPoints = 0L;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-installed-plugins-penalty-points")
	private Long installedPluginsPenaltyPoints = 0L;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-installed-fonts-penalty-points")
	private Long installedFontsPenaltyPoints = 0L;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-timezone-penalty-points")
	private Long timezonePenaltyPoints = 0L;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-location-penalty-points")
	private Long locationPenaltyPoints = 0L;
	
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-location-allowed-range")
	private Long locationAllowedRange = 0L;
	
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-user-agent-penalty-points")
	private Long userAgentPenaltyPoints = 0L;
	
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-persistence-cookie-penalty-points")
	private Long persistentCookiePenaltyPoints = 100L;
	
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-ignore-version-in-user-agent")
	private Boolean ignoreVersionsInUserAgent = false;
		
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-max-tolerated-diffs-in-installed-fonts")
	private Integer maxToleratedNumberOfDifferenceInInstalledFonts = 0;
		
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-max-tolerated-percentage-to-mark-as-different-installed-fonts")
	private Integer maxToleratedPercentageToMarkAsDifferentInstalledFonts = 0;
	
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-max-tolerated-diffs-in-installed-plugins")
	private Integer maxToleratedNumberOfDifferenceInInstalledPlugins = 0;
		
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-max-tolerated-percentage-to-mark-as-different-plugins")
	private Integer maxToleratedPercentageToMarkAsDifferentInstalledPlugins = 0;
	
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-store-profiles-without-confirmation")
	private Boolean storeProfilesWithoutConfirmation = false;
	
	public Long getLocationPenaltyPoints() {
		return locationPenaltyPoints;
	}

	public void setLocationPenaltyPoints(Long locationPenaltyPoints) {
		this.locationPenaltyPoints = locationPenaltyPoints;
	}
	
	public Long getScreenResolutionPenaltyPoints() {
		return screenResolutionPenaltyPoints;
	}

	public void setScreenResolutionPenaltyPoints(Long screenResolutionPenaltyPoints) {
		this.screenResolutionPenaltyPoints = screenResolutionPenaltyPoints;
	}

	public Long getScreenColorDepthPenaltyPoints() {
		return screenColorDepthPenaltyPoints;
	}

	public void setScreenColorDepthPenaltyPoints(Long screenColorDepthPenaltyPoints) {
		this.screenColorDepthPenaltyPoints = screenColorDepthPenaltyPoints;
	}

	public Long getInstalledPluginsPenaltyPoints() {
		return installedPluginsPenaltyPoints;
	}

	public void setInstalledPluginsPenaltyPoints(Long installedPluginsPenaltyPoints) {
		this.installedPluginsPenaltyPoints = installedPluginsPenaltyPoints;
	}

	public Long getInstalledFontsPenaltyPoints() {
		return installedFontsPenaltyPoints;
	}

	public void setInstalledFontsPenaltyPoints(Long installedFontsPenaltyPoints) {
		this.installedFontsPenaltyPoints = installedFontsPenaltyPoints;
	}

	public Long getTimezonePenaltyPoints() {
		return timezonePenaltyPoints;
	}

	public void setTimezonePenaltyPoints(Long timezonePenaltyPoints) {
		this.timezonePenaltyPoints = timezonePenaltyPoints;
	}

	public Long getUserAgentPenaltyPoints() {
		return userAgentPenaltyPoints;
	}

	public void setUserAgentPenaltyPoints(Long userAgentPenaltyPoints) {
		this.userAgentPenaltyPoints = userAgentPenaltyPoints;
	}

	public Long getPersistentCookiePenaltyPoints() {
		return persistentCookiePenaltyPoints;
	}

	public void setPersistentCookiePenaltyPoints(Long persistentCookiePenaltyPoints) {
		this.persistentCookiePenaltyPoints = persistentCookiePenaltyPoints;
	}

	public Long getLocationAllowedRange() {
		return locationAllowedRange;
	}

	public void setLocationAllowedRange(Long locationAllowedRange) {
		this.locationAllowedRange = locationAllowedRange;
	}
	
	public Boolean isIgnoreVersionsInUserAgent() {
		return ignoreVersionsInUserAgent;
	}

	public void setIgnoreVersionsInUserAgent(Boolean ignoreVersionsInUserAgent) {
		this.ignoreVersionsInUserAgent = ignoreVersionsInUserAgent;
	}
	
	public Integer getMaxToleratedNumberOfDifferenceInInstalledFonts() {
		return maxToleratedNumberOfDifferenceInInstalledFonts;
	}

	public void setMaxToleratedNumberOfDifferenceInInstalledFonts(Integer maxToleratedNumberOfDifferenceInInstalledFonts) {
		this.maxToleratedNumberOfDifferenceInInstalledFonts = maxToleratedNumberOfDifferenceInInstalledFonts;
	}
	
	public Integer getMaxToleratedNumberOfDifferenceInInstalledPlugins() {
		return maxToleratedNumberOfDifferenceInInstalledPlugins;
	}
	
	public void setMaxToleratedNumberOfDifferenceInInstalledPlugins(
			Integer maxToleratedNumberOfDifferenceInInstalledPlugins) {
		this.maxToleratedNumberOfDifferenceInInstalledPlugins = maxToleratedNumberOfDifferenceInInstalledPlugins;
	}
	
	public Set<AttributeRebuilder> getUserAgentRebuildPatterns() {
		Set<AttributeRebuilder> rebuilders = new HashSet<AttributeRebuilder>();
		if(isIgnoreVersionsInUserAgent()){
			rebuilders.add(AttributeRebuilders.IGNORE_VERSION_CHARS);
		}
		return rebuilders;
	}

	public Integer getMaxToleratedPercentageToMarkAsDifferentInstalledFonts() {
		return maxToleratedPercentageToMarkAsDifferentInstalledFonts;
	}

	public void setMaxToleratedPercentageToMarkAsDifferentInstalledFonts(
			Integer maxToleratedPercentageToMarkAsDifferentInstalledFonts) {
		this.maxToleratedPercentageToMarkAsDifferentInstalledFonts = maxToleratedPercentageToMarkAsDifferentInstalledFonts;
	}

	public Integer getMaxToleratedPercentageToMarkAsDifferentInstalledPlugins() {
		return maxToleratedPercentageToMarkAsDifferentInstalledPlugins;
	}

	public void setMaxToleratedPercentageToMarkAsDifferentInstalledPlugins(
			Integer maxToleratedPercentageToMarkAsDifferentInstalledPlugins) {
		this.maxToleratedPercentageToMarkAsDifferentInstalledPlugins = maxToleratedPercentageToMarkAsDifferentInstalledPlugins;
	}
	
	public Boolean getStoreProfilesWithoutConfirmation() {
		return storeProfilesWithoutConfirmation;
	}

	public void setStoreProfilesWithoutConfirmation(
			Boolean storeProfilesWithoutConfirmation) {
		this.storeProfilesWithoutConfirmation = storeProfilesWithoutConfirmation;
	}
	
	@Override
	public String toString() {
		return "RiskBasedRunnerConfig [screenColorDepthPenaltyPoints="
				+ screenColorDepthPenaltyPoints
				+ ", screenResolutionPenaltyPoints="
				+ screenResolutionPenaltyPoints
				+ ", installedPluginsPenaltyPoints="
				+ installedPluginsPenaltyPoints
				+ ", installedFontsPenaltyPoints="
				+ installedFontsPenaltyPoints + ", timezonePenaltyPoints="
				+ timezonePenaltyPoints + ", locationPenaltyPoints="
				+ locationPenaltyPoints + ", locationAllowedRange="
				+ locationAllowedRange + ", userAgentPenaltyPoints="
				+ userAgentPenaltyPoints + ", persistentCookiePenaltyPoints="
				+ persistentCookiePenaltyPoints + "]";
	}

	
}
