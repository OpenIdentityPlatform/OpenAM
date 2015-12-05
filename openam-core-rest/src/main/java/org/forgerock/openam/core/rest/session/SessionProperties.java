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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.session;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.session.util.SessionUtils;

/**
 * Responsible for handling the logic around updating properties on an SSOToken.
 *
 * This is of particular importance for calls coming from a public REST interface
 * which need to be routed through the same requirements as PLL calls to the
 * Session Service.
 */
public class SessionProperties {

    public String getProperty(SSOToken token, String key) throws SSOException {
        try {
            SessionUtils.checkPermissionToSetProperty(token, key, null);
        } catch (SessionException e) {
            // Intentionally dropping exception cause to limit information leakage.
            throw new SSOException("Unable to get requested property: " + key);
        }
        return token.getProperty(key);
    }

    public void setProperty(SSOToken token, String key, String value) throws SSOException {

        try {
            SessionUtils.checkPermissionToSetProperty(token, key, value);
        } catch (SessionException e) {
            // Intentionally dropping exception cause to limit information leakage.
            throw new SSOException("Unable to set requested property: " + key);
        }
        token.setProperty(key, value);
    }
}
