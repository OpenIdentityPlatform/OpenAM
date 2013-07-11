/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.auth.shared;

import com.google.inject.Inject;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang3.StringUtils;

/**
 * Responsible for generating SSOTokens.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SSOTokenFactory {
    private final SSOTokenManager manager;
    private final Debug debug;

    /**
     * Guice initialised constructor.
     * @param manager Non null SSOTokenManager for creating Tokens.
     */
    @Inject
    public SSOTokenFactory(SSOTokenManager manager) {
        this(manager, Debug.getInstance("amIdentityServices"));
    }

    /**
     * Testing Constructor with dependencies exposed.
     * @param manager Non null SSOTokenManager for creating Tokens.
     * @param debug Non null Debugger.
     */
    public SSOTokenFactory(SSOTokenManager manager, Debug debug) {
        this.manager = manager;
        this.debug = debug;
    }

    /**
     * Create an SSO Token using the SSOTokenManager.
     * @param tokenId Non and non empty token id to use.
     * @return Null if the SSOToken could not be created. Otherwise a valid SSOToken.
     */
    public SSOToken getTokenFromId(String tokenId) {
        if (StringUtils.isEmpty(tokenId)) {
            return null;
        }

        if (debug.messageEnabled()) {
            debug.message("Creating SSOToken for ID: " + tokenId);
        }

        try {
            return manager.createSSOToken(tokenId);
        } catch (SSOException e) {
            debug.warning("Failed to create SSO Token for ID: " + tokenId, e);
            return null;
        }
    }
}
