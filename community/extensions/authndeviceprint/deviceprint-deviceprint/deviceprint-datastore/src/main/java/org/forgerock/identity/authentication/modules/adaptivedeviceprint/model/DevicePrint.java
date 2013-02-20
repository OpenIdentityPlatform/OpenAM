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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.model;

import java.lang.reflect.Field;

/**
 * Represents data gathered by JS collectors.
 * 
 * @author mbilski
 * 
 */
public class DevicePrint {

	private String screenColorDepth;

	private String screenHeight;

	private String screenWidth;

	private String installedPlugins;

	private String installedFonts;

	private String timezone;

	private Double longitude;

	private Double latitude;

	private String userAgent;

	private String persistentCookie;

	public String getScreenColorDepth() {
		return screenColorDepth;
	}

	public void setScreenColorDepth(String screenColorDepth) {
		this.screenColorDepth = screenColorDepth;
	}

	public String getScreenHeight() {
		return screenHeight;
	}

	public void setScreenHeight(String screenHeight) {
		this.screenHeight = screenHeight;
	}

	public String getScreenWidth() {
		return screenWidth;
	}

	public void setScreenWidth(String screenWidth) {
		this.screenWidth = screenWidth;
	}

	public String getInstalledPlugins() {
		return installedPlugins;
	}

	public void setInstalledPlugins(String installedPlugins) {
		this.installedPlugins = installedPlugins;
	}

	public String getInstalledFonts() {
		return installedFonts;
	}

	public void setInstalledFonts(String installedFonts) {
		this.installedFonts = installedFonts;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getPersistentCookie() {
		return persistentCookie;
	}

	public void setPersistentCookie(String persistentCookie) {
		this.persistentCookie = persistentCookie;
	}

	/**
	 * Merge this object with other. Sets the field if in this object field is
	 * null and in the other it is not.
	 */
	public void merge(DevicePrint other) {
		for (Field field : this.getClass().getDeclaredFields()) {
			try {
				if (field.get(this) == null) {
					field.set(this, field.get(other));
				}
			} catch (Exception e) {
			}
		}
	}

	@SuppressWarnings("all")
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((installedFonts == null) ? 0 : installedFonts.hashCode());
		result = prime
				* result
				+ ((installedPlugins == null) ? 0 : installedPlugins.hashCode());
		result = prime * result
				+ ((latitude == null) ? 0 : latitude.hashCode());
		result = prime * result
				+ ((longitude == null) ? 0 : longitude.hashCode());
		result = prime
				* result
				+ ((persistentCookie == null) ? 0 : persistentCookie.hashCode());
		result = prime
				* result
				+ ((screenColorDepth == null) ? 0 : screenColorDepth.hashCode());
		result = prime * result
				+ ((screenHeight == null) ? 0 : screenHeight.hashCode());
		result = prime * result
				+ ((screenWidth == null) ? 0 : screenWidth.hashCode());
		result = prime * result
				+ ((timezone == null) ? 0 : timezone.hashCode());
		result = prime * result
				+ ((userAgent == null) ? 0 : userAgent.hashCode());
		return result;
	}

	@SuppressWarnings("all")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DevicePrint other = (DevicePrint) obj;
		
		if (installedFonts == null) {
			if (other.installedFonts != null)
				return false;
		} else if (!installedFonts.equals(other.installedFonts))
			return false;
		if (installedPlugins == null) {
			if (other.installedPlugins != null)
				return false;
		} else if (!installedPlugins.equals(other.installedPlugins))
			return false;
		if (latitude == null) {
			if (other.latitude != null)
				return false;
		} else if (!latitude.equals(other.latitude))
			return false;
		if (longitude == null) {
			if (other.longitude != null)
				return false;
		} else if (!longitude.equals(other.longitude))
			return false;
		if (persistentCookie == null) {
			if (other.persistentCookie != null)
				return false;
		} else if (!persistentCookie.equals(other.persistentCookie))
			return false;
		if (screenColorDepth == null) {
			if (other.screenColorDepth != null)
				return false;
		} else if (!screenColorDepth.equals(other.screenColorDepth))
			return false;
		if (screenHeight == null) {
			if (other.screenHeight != null)
				return false;
		} else if (!screenHeight.equals(other.screenHeight))
			return false;
		if (screenWidth == null) {
			if (other.screenWidth != null)
				return false;
		} else if (!screenWidth.equals(other.screenWidth))
			return false;
		if (timezone == null) {
			if (other.timezone != null)
				return false;
		} else if (!timezone.equals(other.timezone))
			return false;
		if (userAgent == null) {
			if (other.userAgent != null)
				return false;
		} else if (!userAgent.equals(other.userAgent))
			return false;
		return true;
	}

	@SuppressWarnings("all")
	@Override
	public String toString() {
		return "DevicePrint [screenColorDepth=" + screenColorDepth
				+ ", screenHeight=" + screenHeight + ", screenWidth="
				+ screenWidth + ", installedPlugins=" + installedPlugins
				+ ", installedFonts=" + installedFonts + ", timezone="
				+ ", longitude="
				+ longitude + ", latitude=" + latitude + ", userAgent="
				+ userAgent + ", persistentCookie=" + persistentCookie + "]";
	}
	
	
}
