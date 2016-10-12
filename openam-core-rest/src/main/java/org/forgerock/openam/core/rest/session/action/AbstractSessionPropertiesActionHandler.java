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

package org.forgerock.openam.core.rest.session.action;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.session.SessionResourceUtil;
import org.forgerock.openam.session.SessionPropertyWhitelist;

/**
 * Abstract handler for session properties actions.
 */
public abstract class AbstractSessionPropertiesActionHandler implements ActionHandler {

    private final SessionPropertyWhitelist sessionPropertyWhitelist;
    private final SessionResourceUtil sessionResourceUtil;

    /**
     * Constructs a AbstractSessionPropertiesActionHandler instance
     *
     * @param sessionPropertyWhitelist An instance of the sessionPropertyWhitelist.
     * @param sessionResourceUtil An instance of SessionResourceUtil.
     */
    public AbstractSessionPropertiesActionHandler(SessionPropertyWhitelist sessionPropertyWhitelist,
            SessionResourceUtil sessionResourceUtil) {
        this.sessionPropertyWhitelist = sessionPropertyWhitelist;
        this.sessionResourceUtil = sessionResourceUtil;
    }

    JsonValue getSessionProperties(String tokenId) throws SSOException, IdRepoException {
        JsonValue result = json(object());
        SSOToken target = getToken(tokenId);
        String realm = getTargetRealm(target);
        for (String property : sessionPropertyWhitelist.getAllListedProperties(realm)) {
            final String value = target.getProperty(property);
            result.add(property, value == null ? "" : value);
        }
        return result;
    }

    SSOToken getToken(String tokenId) throws SSOException {
        return sessionResourceUtil.getTokenWithoutResettingIdleTime(tokenId);
    }

    String getTargetRealm(SSOToken ssoToken) throws IdRepoException, SSOException {
        return sessionResourceUtil.convertDNToRealm(sessionResourceUtil.getIdentity(ssoToken).getRealm());
    }
}
