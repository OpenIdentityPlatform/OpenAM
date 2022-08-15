/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SSOTokenEventImpl.java,v 1.2 2008/06/25 05:41:43 qcheng Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */

package com.iplanet.sso.providers.dpro;

import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenEvent;

/**
 * This {@link SSOTokenEvent} represents a change in {@link SSOToken} state.
 */
class SSOTokenEventImpl implements SSOTokenEvent {

    private final SessionEvent sessionEvent;

  /**
   * Creates a SSOTokenEventImpl object.
   *
   * @param event The SessionEvent
   * @see SessionEvent
   */
    SSOTokenEventImpl(SessionEvent event) {
        sessionEvent = event;
    }

  /**
   * Returns the {@link SSOToken}.
   *
   * @return The SSO token affected by this event.
   */ 
    public SSOToken getToken() {
        return new SSOTokenImpl(sessionEvent.getSession());
    }

    /**
     * Gets the time of this event.
     * 
     * @return The event time as UTC milliseconds from the epoch.
     */
    public long getTime() {
        return sessionEvent.getTime();
    }

    /**
     * Gets the type of this event.
     *
     * @return The type of this event. Possible types are :
     *         {@link SSOTokenEvent#SSO_TOKEN_IDLE_TIMEOUT}, {@link SSOTokenEvent#SSO_TOKEN_MAX_TIMEOUT},
     *         {@link SSOTokenEvent#SSO_TOKEN_DESTROY} and {@link SSOTokenEvent#SSO_TOKEN_PROPERTY_CHANGED}.
     * @exception SSOException is thrown if the SSOTokenEvent type is not one of the above.
     */
    public int getType() throws SSOException {
        switch (sessionEvent.getType()) {
        case IDLE_TIMEOUT:
            return SSOTokenEvent.SSO_TOKEN_IDLE_TIMEOUT;
        case MAX_TIMEOUT:
            return SSOTokenEvent.SSO_TOKEN_MAX_TIMEOUT;
        case LOGOUT:
            return SSOTokenEvent.SSO_TOKEN_DESTROY;
        case DESTROY:
            return SSOTokenEvent.SSO_TOKEN_DESTROY;
        case PROPERTY_CHANGED:
            return SSOTokenEvent.SSO_TOKEN_PROPERTY_CHANGED;
        case EVENT_URL_ADDED:
            return SSOTokenEvent.SSO_TOKEN_URL_EVENT_ADDED;
        }
        throw new SSOException(SSOProviderBundle.rbName, "invalidevent", null);
    }
}
