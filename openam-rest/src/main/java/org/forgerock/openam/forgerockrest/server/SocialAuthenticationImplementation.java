/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.forgerockrest.server;

import org.forgerock.openam.utils.StringUtils;

/**
 * A class to encapsulate a social authentication implementation.
 */
public final class SocialAuthenticationImplementation {

    public static final String SERVICE_NAME = "socialAuthNService";
    public static final String ENABLED_IMPLEMENTATIONS_ATTRIBUTE = "socialAuthNEnabled";
    public static final String DISPLAY_NAME_ATTRIBUTE = "socialAuthNDisplayName";
    public static final String CHAINS_ATTRIBUTE = "socialAuthNAuthChain";
    public static final String ICONS_ATTRIBUTE = "socialAuthNIcon";

    private String iconPath;
    private String authnChain;
    private String displayName;

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public String getAuthnChain() {
        return authnChain;
    }

    public void setAuthnChain(String authnChain) {
        this.authnChain = authnChain;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Check if the values in the specified implementation are valid.  Currently this just involves checking the
     * values are non null.
     *
     * @return true if the implementation has all its required values
     */
    public boolean isValid() {
        return StringUtils.isNotEmpty(getAuthnChain()) &&
               StringUtils.isNotEmpty(getDisplayName()) &&
               StringUtils.isNotEmpty(getIconPath());
    }
}