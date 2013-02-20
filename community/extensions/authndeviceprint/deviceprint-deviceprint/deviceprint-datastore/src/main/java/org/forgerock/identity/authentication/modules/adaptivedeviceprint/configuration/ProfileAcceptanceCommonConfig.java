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

import org.forgerock.identity.authentication.modules.common.config.AttributeNameMapping;

public class ProfileAcceptanceCommonConfig {
	/**
	 * Name of the field in user record where profile information is stored
	 */
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-ldap-field-name")
	private String adaptiveProfilesFieldName;

	/**
	 * Default name of the profile. This name will be suggested during storing
	 * the profile.
	 */
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-default-profile-name")
	private String defaultProfileName;

	/**
	 * Name of the profile expiration days configuration attribute
	 */
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-profile-expiration-days")
	private Integer profileExpirationDays;

	/**
	 * Name of the maximum stored profiles quantity configuration attribute
	 */
	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-maximum-profiles-stored-quantity")
	private Integer profileMaximumProfilesStoredQuantity;

	public String getAdaptiveProfilesFieldName() {
		return adaptiveProfilesFieldName;
	}

	public void setAdaptiveProfilesFieldName(String adaptiveProfilesFieldName) {
		this.adaptiveProfilesFieldName = adaptiveProfilesFieldName;
	}

	public String getDefaultProfileName() {
		return defaultProfileName;
	}

	public void setDefaultProfileName(String defaultProfileName) {
		this.defaultProfileName = defaultProfileName;
	}

	public Integer getProfileExpirationDays() {
		return profileExpirationDays;
	}

	public void setProfileExpirationDays(Integer profileExpirationDays) {
		this.profileExpirationDays = profileExpirationDays;
	}

	public Integer getProfileMaximumProfilesStoredQuantity() {
		return profileMaximumProfilesStoredQuantity;
	}

	public void setProfileMaximumProfilesStoredQuantity(
			Integer profileMaximumProfilesStoredQuantity) {
		this.profileMaximumProfilesStoredQuantity = profileMaximumProfilesStoredQuantity;
	}

}
