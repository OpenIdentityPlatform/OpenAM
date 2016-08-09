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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.agent;

/**
 * Collection of Agent constants.
 */
public final class AgentConstants {

    /**
     * Private constructor.
     */
    private AgentConstants() {
    }

    /**
     * CDSSO redirect URI attribute name.
     */
    public static final String CDSSO_REDIRECT_URI_ATTRIBUTE_NAME = "com.sun.identity.agents.config.cdsso.redirect.uri";

    /**
     * Logout entry URI attribute name.
     */
    public static final String LOGOUT_ENTRY_UTI_ATTRIBUTE_NAME = "com.sun.identity.agents.config.logout.entry.uri";

    /**
     * Agent password attribute name.
     */
    public static final String USER_PASSWORD_ATTRIBUTE_NAME = "userpassword";

    /**
     * Agent root Url for CDSSO attribute name.
     */
    public static final String AGENT_ROOT_URL_FOR_CDSSO_ATTRIBUTE_NAME = "sunIdentityServerDeviceKeyValue"; // "agentRootURL";
}
