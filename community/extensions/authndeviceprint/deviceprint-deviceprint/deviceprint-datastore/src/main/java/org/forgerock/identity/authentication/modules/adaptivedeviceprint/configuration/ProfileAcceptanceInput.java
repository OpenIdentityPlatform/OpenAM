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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.configuration;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.ProfileMatchingRuleResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.UserProfile;
import org.forgerock.identity.authentication.modules.common.config.AttributeNameMapping;

public class ProfileAcceptanceInput {

	@AttributeNameMapping("profileAcceptanceCommonConfig")
	private ProfileAcceptanceCommonConfig commonConfig;

	@AttributeNameMapping("matchingResult")
	private ProfileMatchingRuleResult matchingResult;

	@AttributeNameMapping("selectedUserProfile")
	private UserProfile selectedUserProfile;

	@AttributeNameMapping("currentDevicePrint")
	private DevicePrint currentDevicePrint;

	@AttributeNameMapping("matchingModuleNotFailed")
	private Boolean matchingModuleNotFailed = false;

	@AttributeNameMapping("storeCookie")
	private Boolean storeCookie = false;

	public ProfileAcceptanceCommonConfig getCommonConfig() {
		return commonConfig;
	}

	public void setCommonConfig(ProfileAcceptanceCommonConfig commonConfig) {
		this.commonConfig = commonConfig;
	}

	public ProfileMatchingRuleResult getMatchingResult() {
		return matchingResult;
	}

	public void setMatchingResult(ProfileMatchingRuleResult matchingResult) {
		this.matchingResult = matchingResult;
	}

	public UserProfile getSelectedUserProfile() {
		return selectedUserProfile;
	}

	public void setSelectedUserProfile(UserProfile selectedUserProfile) {
		this.selectedUserProfile = selectedUserProfile;
	}

	public DevicePrint getCurrentDevicePrint() {
		return currentDevicePrint;
	}

	public void setCurrentDevicePrint(DevicePrint currentDevicePrint) {
		this.currentDevicePrint = currentDevicePrint;
	}

	public Boolean getMatchingModuleNotFailed() {
		return matchingModuleNotFailed;
	}

	public void setMatchingModuleNotFailed(Boolean matchingModuleNotFailed) {
		this.matchingModuleNotFailed = matchingModuleNotFailed;
	}

	public Boolean getStoreCookie() {
		return storeCookie;
	}

	public void setStoreCookie(Boolean storeCookie) {
		this.storeCookie = storeCookie;
	}

}
