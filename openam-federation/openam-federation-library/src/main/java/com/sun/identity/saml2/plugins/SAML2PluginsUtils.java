/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: SAML2PluginsUtils.java,v 1.1 2008/07/08 23:03:34 hengming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.saml2.plugins;

import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.datastruct.CollectionHelper;

import java.util.Map;

/**
 * The <code>SAML2PluginsUtils</code> contains utility methods for SAML 2.0 plugins classes.
 */
public class SAML2PluginsUtils {

    private static final String AUTHN_CONFIG = "AUTHN";
    // These are also defined in OpenAM Core but not accessible here.
    private static final String PROFILE_ATTRIBUTE = "iplanet-am-auth-dynamic-profile-creation";
    private static final String IGNORE_PROFILE = "ignore";
    private static final String DYNAMIC_PROFILE = "true";
    private static final String CREATE_ALIAS_PROFILE = "createAlias";

    private static ConfigurationInstance ci;

    static {
        try {
            ci = ConfigurationManager.getConfigurationInstance(AUTHN_CONFIG);
        } catch (ConfigurationException ce) {
            SAML2Utils.debug.error("SAML2PluginsUtils.static:", ce);
        }
    }

    private SAML2PluginsUtils() {}

    /**
     * Checks if dynamic profile creation is enabled.
     * @param realm Realm to check for the dynamic profile creation attribute.
     * @return <code>true</code> if dynamic profile creation is enabled, false otherwise.
     */
    public static boolean isDynamicProfile(String realm) {
        String profileAttribute = getProfileAttribute(realm);
        return DYNAMIC_PROFILE.equalsIgnoreCase(profileAttribute)
                || CREATE_ALIAS_PROFILE.equalsIgnoreCase(profileAttribute);
    }

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     * @param realm realm to check for the dynamical profile creation attribute.
     * @return true if dynamical profile creation or ignore profile is enabled, false otherwise.
     */
    public static boolean isDynamicalOrIgnoredProfile(String realm) {

        String profileAttribute = getProfileAttribute(realm);
        return profileAttribute != null && (profileAttribute.equalsIgnoreCase(DYNAMIC_PROFILE) ||
                profileAttribute.equalsIgnoreCase(CREATE_ALIAS_PROFILE) ||
                profileAttribute.equalsIgnoreCase(IGNORE_PROFILE));
    }

    /**
     * Checks if ignore profile is enabled.
     * @param realm realm to check for the profile creation attribute.
     * @return true if ignore profile is enabled, false otherwise.
     */
    public static boolean isIgnoredProfile(String realm) {
        return IGNORE_PROFILE.equalsIgnoreCase(getProfileAttribute(realm));
    }

    private static String getProfileAttribute(String realm) {

        String result = null;

        if (ci != null) {
            try {
                final Map config = ci.getConfiguration(realm, null);
                // Will be null in Fedlet case
                if (config != null) {
                    result = CollectionHelper.getMapAttr(config, PROFILE_ATTRIBUTE);
                }
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("SAML2PluginsUtils.getProfileAttribute: attr=" + result);
                }
            } catch (ConfigurationException e) {
                SAML2Utils.debug.error("SAML2PluginsUtils.getProfileAttribute: unable to get profile attribute", e);
            }
        } else {
            SAML2Utils.debug.error("SAML2PluginsUtils.getProfileAttribute: " +
                    "ConfigurationInstance for " + AUTHN_CONFIG + " was null");
        }

        return result;
    }
}
