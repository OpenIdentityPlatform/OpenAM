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

package org.forgerock.openam.authentication.modules.deviceprint;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.authentication.modules.deviceprint.model.DevicePrint;

import java.util.Map;

/**
 * Configuration constants and backed by a map, with methods to get the attribute values out of the map.
 */
public class DevicePrintAuthenticationConfig {

    private static final Debug DEBUG = Debug.getInstance("amAuthDevicePrint");

    public static final String PROFILE_EXPIRATION_DAYS = "iplanet-am-auth-adaptive-device-print-profile-expiration-days";
    public static final String MAX_STORED_PROFILES = "iplanet-am-auth-adaptive-device-print-maximum-profiles-stored-quantity";
    public static final String AUTO_STORE_PROFILES = "iplanet-am-auth-adaptive-device-print-store-profiles-without-confirmation";

    public static final String MAX_TOLERATED_PENALTY_POINTS = "iplanet-am-auth-adaptive-device-print-max-tolerated-penalty-points";
    public static final String SCREEN_COLOUR_DEPTH_PENALTY_POINTS = "iplanet-am-auth-adaptive-device-print-screen-color-depth-penalty-points";
    public static final String SCREEN_RESOLUTION_PENALTY_POINTS = "iplanet-am-auth-adaptive-device-print-screen-resolution-penalty-points";
    public static final String INSTALLED_PLUGINS_PENALTY_POINTS = "iplanet-am-auth-adaptive-device-print-installed-plugins-penalty-points";
    public static final String INSTALLED_FONTS_PENALTY_POINTS = "iplanet-am-auth-adaptive-device-print-installed-fonts-penalty-points";
    public static final String TIMEZONE_PENALTY_POINTS = "iplanet-am-auth-adaptive-device-print-timezone-penalty-points";
    public static final String LOCATION_PENALTY_POINTS = "iplanet-am-auth-adaptive-device-print-location-penalty-points";
    public static final String LOCATION_ALLOWED_RANGE = "iplanet-am-auth-adaptive-device-print-location-allowed-range";
    public static final String USER_AGENT_PENALTY_POINTS = "iplanet-am-auth-adaptive-device-print-user-agent-penalty-points";
    public static final String IGNORE_VERSION_IN_USER_AGENT = "iplanet-am-auth-adaptive-device-print-ignore-version-in-user-agent";
    public static final String MAX_TOLERATED_DIFFS_IN_INSTALLED_FONTS = "iplanet-am-auth-adaptive-device-print-max-tolerated-diffs-in-installed-fonts";
    public static final String MAX_TOLERATED_PERCENTAGE_TO_MARK_AS_DIFFERENT_INSTALLED_FONTS = "iplanet-am-auth-adaptive-device-print-max-tolerated-percentage-to-mark-as-different-installed-fonts";
    public static final String MAX_TOLERATED_DIFFS_IN_INSTALLED_PLUGINS = "iplanet-am-auth-adaptive-device-print-max-tolerated-diffs-in-installed-plugins";
    public static final String MAX_TOLERATED_PERCENTAGE_TO_MARK_AS_DIFFERENT_PLUGINS = "iplanet-am-auth-adaptive-device-print-max-tolerated-percentage-to-mark-as-different-plugins";

    public static final String USER_AGENT_REQUIRED = "iplanet-am-auth-adaptive-device-print-user-agent-required";
    public static final String PLUGINS_REQUIRED = "iplanet-am-auth-adaptive-device-print-plugins-required";
    public static final String FONTS_REQUIRED = "iplanet-am-auth-adaptive-device-print-fonts-required";
    public static final String GEO_LOCATION_REQUIRED = "iplanet-am-auth-adaptive-device-print-geolocation-required";
    public static final String SCREEN_PARAMS_REQUIRED = "iplanet-am-auth-adaptive-device-print-screen-params-required";
    public static final String TIMEZONE_REQUIRED = "iplanet-am-auth-adaptive-device-print-timezone-required";

    private final Map<?, ?> config;

    /**
     * Constructs an instance of the DevicePrintAuthenticationConfig.
     *
     * @param config The configuration map of attribute value pairs.
     */
    public DevicePrintAuthenticationConfig(Map<?, ?> config) {
        this.config = config;
    }

    /**
     * Gets the attribute value for the given attribute name, as a String.
     *
     * @param attributeName The name of the attribute.
     * @return The attribute value.
     */
    public String getAttributeValue(String attributeName)  {
        return CollectionHelper.getMapAttr(config, attributeName);
    }

    /**
     * Gets the attribute value for the given attribute name, as an int.
     *
     * @param attributeName The name of the attribute.
     * @return The attribute value.
     */
    public int getInt(String attributeName) {
        return Integer.parseInt(getAttributeValue(attributeName));
    }

    /**
     * Gets the attribute value for the given attribute name, as a boolean.
     *
     * @param attributeName The name of the attribute.
     * @return The attribute value.
     */
    public boolean getBoolean(String attributeName) {
        return Boolean.parseBoolean(getAttributeValue(attributeName));
    }

    /**
     * Gets the attribute value for the given attribute name, as a long.
     *
     * @param attributeName The name of the attribute.
     * @return The attribute value.
     */
    public long getLong(String attributeName) {
        return Long.parseLong(getAttributeValue(attributeName));
    }

    /**
     * Determines if the given Device Print information has the required attributes populated, based from the
     * authentication modules settings.
     *
     * @param devicePrint The Device Print information.
     * @return If the Device Print information has all the required attributes populated.
     */
    public boolean hasRequiredAttributes(DevicePrint devicePrint) {

        boolean hasRequiredAttributes = true;

        hasRequiredAttributes = hasRequiredAttributes && hasRequiredAttribute(FONTS_REQUIRED,
                devicePrint.getInstalledFonts());

        hasRequiredAttributes = hasRequiredAttributes && hasRequiredAttribute(GEO_LOCATION_REQUIRED,
                devicePrint.getLatitude(), devicePrint.getLongitude());

        hasRequiredAttributes = hasRequiredAttributes && hasRequiredAttribute(PLUGINS_REQUIRED,
                devicePrint.getInstalledPlugins());

        hasRequiredAttributes = hasRequiredAttributes && hasRequiredAttribute(SCREEN_PARAMS_REQUIRED,
                devicePrint.getScreenColourDepth(), devicePrint.getScreenHeight(), devicePrint.getScreenWidth());

        hasRequiredAttributes = hasRequiredAttributes && hasRequiredAttribute(TIMEZONE_REQUIRED,
                devicePrint.getTimezone());

        hasRequiredAttributes = hasRequiredAttributes && hasRequiredAttribute(USER_AGENT_REQUIRED,
                devicePrint.getUserAgent());

        return hasRequiredAttributes;
    }

    /**
     * Determines if the attribute is required and if so that the required attributes are populated.
     *
     * @param attributeName The name of the attribute.
     * @param attributeValues The values from the Device Print information.
     * @return If the Device Print values are populated, if required.
     */
    private boolean hasRequiredAttribute(String attributeName, Object... attributeValues) {

        if (getBoolean(attributeName)) {
            for (Object attributeValue : attributeValues) {
                if (attributeValue == null || "".equals(attributeValue)) {
                    DEBUG.message("DevicePrint does not have required attribute: " + attributeName);
                    return false;
                }
            }
        }
        return true;
    }
}
