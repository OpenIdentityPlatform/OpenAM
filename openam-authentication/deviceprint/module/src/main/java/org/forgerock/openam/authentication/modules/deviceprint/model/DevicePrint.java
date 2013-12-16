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
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
 */

package org.forgerock.openam.authentication.modules.deviceprint.model;

/**
 * Represents Device Print data gathered by JS collectors.
 *
 * @author mbilski
 */
public class DevicePrint {

	private String screenColourDepth;

	private String screenHeight;

	private String screenWidth;

	private String installedPlugins;

	private String installedFonts;

	private String timezone;

	private Double longitude;

	private Double latitude;

	private String userAgent;

    /**
     * Gets the Screen Colour Depth.
     *
     * @return The Screen Colour Depth.
     */
	public String getScreenColourDepth() {
		return screenColourDepth;
	}

    /**
     * Sets the Screen Colour Depth.
     *
     * @param screenColourDepth The Screen Colour Depth.
     */
	public void setScreenColourDepth(String screenColourDepth) {
		this.screenColourDepth = screenColourDepth;
	}

    /**
     * Gets the Screen Height.
     *
     * @return The Screen Height.
     */
	public String getScreenHeight() {
		return screenHeight;
	}

    /**
     * Sets the Screen Height.
     *
     * @param screenHeight The Screen Height.
     */
	public void setScreenHeight(String screenHeight) {
		this.screenHeight = screenHeight;
	}

    /**
     * Gets the Screen Width.
     *
     * @return The Screen Width.
     */
	public String getScreenWidth() {
		return screenWidth;
	}

    /**
     * Sets the Screen Width.
     *
     * @param screenWidth The Screen Width.
     */
	public void setScreenWidth(String screenWidth) {
		this.screenWidth = screenWidth;
	}

    /**
     * Gets the Installed Plugins.
     *
     * @return The Installed Plugins.
     */
	public String getInstalledPlugins() {
		return installedPlugins;
	}

    /**
     * Sets the Installed Plugins.
     *
     * @param installedPlugins The Installed Plugins.
     */
	public void setInstalledPlugins(String installedPlugins) {
		this.installedPlugins = installedPlugins;
	}

    /**
     * Gets the Installed Fonts.
     *
     * @return The Installed Fonts.
     */
	public String getInstalledFonts() {
		return installedFonts;
	}

    /**
     * Sets the Installed Fonts.
     *
     * @param installedFonts The Installed Fonts.
     */
	public void setInstalledFonts(String installedFonts) {
		this.installedFonts = installedFonts;
	}

    /**
     * Gets the Timezone.
     *
     * @return The Timezone.
     */
	public String getTimezone() {
		return timezone;
	}

    /**
     * Sets the Timezone.
     *
     * @param timezone The Timezone.
     */
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

    /**
     * Gets the Longitude.
     *
     * @return The Longitude.
     */
	public Double getLongitude() {
		return longitude;
	}

    /**
     * Sets the Longitude.
     *
     * @param longitude The Longitude.
     */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

    /**
     * Gets the Latitude.
     *
     * @return The Latitude.
     */
	public Double getLatitude() {
		return latitude;
	}

    /**
     * Sets the Latitude.
     *
     * @param latitude The Latitude.
     */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

    /**
     * Gets the User Agent.
     *
     * @return The User Agent.
     */
	public String getUserAgent() {
		return userAgent;
	}

    /**
     * Sets the User Agent.
     *
     * @param userAgent The User Agent.
     */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

    /**
     * Merges the given Device Print information with this Device Print information.
     *
     * If the attribute on this Device Print is null then the attribute value from the other Device Print is copied
     * across.
     *
     * @param other The other Device Print to merge in.
     */
    public void merge(DevicePrint other) {
        if (isNull(getScreenColourDepth())) {
            setScreenColourDepth(other.getScreenColourDepth());
        }
        if (isNull(getScreenHeight())) {
            setScreenHeight(other.getScreenHeight());
        }
        if (isNull(getScreenWidth())) {
            setScreenWidth(other.getScreenWidth());
        }
        if (isNull(getInstalledPlugins())) {
            setInstalledPlugins(other.getInstalledPlugins());
        }
        if (isNull(getInstalledFonts())) {
            setInstalledFonts(other.getInstalledFonts());
        }
        if (isNull(getTimezone())) {
            setTimezone(other.getTimezone());
        }
        if (isNull(getLongitude())) {
            setLongitude(other.getLongitude());
        }
        if (isNull(getLatitude())) {
            setLatitude(other.getLatitude());
        }
        if (isNull(getUserAgent())) {
            setUserAgent(other.getUserAgent());
        }
    }

    /**
     * Simple check if value is null.
     *
     * @param value The value.
     * @return If the value is null.
     */
    private boolean isNull(Object value) {
        return value == null;
    }

    /**
     * {@inheritDoc}
     */
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
				+ ((screenColourDepth == null) ? 0 : screenColourDepth.hashCode());
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

    /**
     * {@inheritDoc}
     */
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
		if (screenColourDepth == null) {
			if (other.screenColourDepth != null)
				return false;
		} else if (!screenColourDepth.equals(other.screenColourDepth))
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

    /**
     * {@inheritDoc}
     */
	@Override
	public String toString() {
		return "DevicePrint [screenColourDepth=" + screenColourDepth
				+ ", screenHeight=" + screenHeight + ", screenWidth="
				+ screenWidth + ", installedPlugins=" + installedPlugins
				+ ", installedFonts=" + installedFonts + ", timezone="
				+ timezone + ", longitude="
				+ longitude + ", latitude=" + latitude + ", userAgent="
				+ userAgent + "]";
	}
}
