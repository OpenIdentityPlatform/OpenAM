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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.requiredattributes;

import org.forgerock.identity.authentication.modules.common.config.AttributeNameMapping;

public class RequiredAttributesConfig {

	//@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-persistent-cookie-required")
	//private Boolean persistentCookieRequired = false;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-user-agent-required")
	private Boolean userAgentRequired = false;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-plugins-required")
	private Boolean pluginsRequired = false;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-fonts-required")
	private Boolean fontsRequired = false;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-geolocation-required")
	private Boolean geolocationRequired = false;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-screen-params-required")
	private Boolean screenParamsRequired = false;

	@AttributeNameMapping("iplanet-am-auth-adaptive-device-print-timezone-required")
	private Boolean timezoneRequired = false;

	public Boolean isUserAgentRequired() {
		return userAgentRequired;
	}

	public void setUserAgentRequired(Boolean userAgentRequired) {
		this.userAgentRequired = userAgentRequired;
	}

	public Boolean isPluginsRequired() {
		return pluginsRequired;
	}

	public void setPluginsRequired(Boolean pluginsRequired) {
		this.pluginsRequired = pluginsRequired;
	}

	public Boolean isFontsRequired() {
		return fontsRequired;
	}

	public void setFontsRequired(Boolean fontsRequired) {
		this.fontsRequired = fontsRequired;
	}

	public Boolean isGeolocationRequired() {
		return geolocationRequired;
	}

	public void setGeolocationRequired(Boolean geolocationRequired) {
		this.geolocationRequired = geolocationRequired;
	}

	public Boolean isScreenParamsRequired() {
		return screenParamsRequired;
	}

	public void setScreenParamsRequired(Boolean screenParamsRequired) {
		this.screenParamsRequired = screenParamsRequired;
	}

	public Boolean isTimezoneRequired() {
		return timezoneRequired;
	}

	public void setTimezoneRequired(Boolean timezoneRequired) {
		this.timezoneRequired = timezoneRequired;
	}
	
	@Override
	public String toString() {
		return "RequiredAttributesConfig [userAgentRequired="
				+ userAgentRequired	+ ", pluginsRequired="
				+ pluginsRequired + ", fontsRequired="
				+ fontsRequired + ", geolocationRequired="
				+ geolocationRequired + ", screenParamsRequired="
				+ screenParamsRequired + ", timezoneRequired="
				+ timezoneRequired + "]";
	}

}
