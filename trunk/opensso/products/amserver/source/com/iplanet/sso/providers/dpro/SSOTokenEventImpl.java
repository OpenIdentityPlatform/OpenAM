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
 */

package com.iplanet.sso.providers.dpro;

import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenEvent;

/**
 * This class <code>SSOTokenEventImpl</code> implements the interface 
 * <code>SSOTokenEvent</code>. The <code> SSOTokenEvent</code>>represents a 
 * change in SSOToken.
 * </p>
 * The following are possible sso token event types: SSO_TOKEN_IDLE_TIMEOUT,
 * SSO_TOKEN_MAX_TIMEOUT and SSO_TOKEN_DESTORY
 * 
 * @see com.iplanet.sso.SSOTokenEvent
 */

class SSOTokenEventImpl implements SSOTokenEvent {
    private com.iplanet.dpro.session.SessionEvent sessionEvent;

  /**
   * Creates a SSOTokenEventImpl object
   * @param event The SessionEvent
   * @see com.iplanet.dpro.session.SessionEvent
   */
    SSOTokenEventImpl(com.iplanet.dpro.session.SessionEvent event) {
        sessionEvent = event;
    }

  /**
   * Returns the SSOToken
   * @return token , returns the changed token
   */ 
   
    public SSOToken getToken() {
        SSOToken ssoToken = new SSOTokenImpl(sessionEvent.getSession());
        return ssoToken;
    }

    /**
     * Gets the time of this event
     * 
     * @return The event time as UTC milliseconds from the epoch
     */
    public long getTime() {
        return sessionEvent.getTime();
    }

    /**
     * Gets the type of this event.
     * 
     * @return The type of this event. Possible types are :
     *         SSO_TOKEN_IDLE_TIMEOUT, SSO_TOKEN_MAX_TIMEOUT and
     *         SSO_TOKEN_DESTORY
     * @exception A
     *                SSOException is thrown if the SSOTokenEvent type is not
     *                one of the above.
     */
    public int getType() throws SSOException {
        int state = sessionEvent.getType();
        switch (state) {
        case SessionEvent.IDLE_TIMEOUT:
            return SSOTokenEvent.SSO_TOKEN_IDLE_TIMEOUT;
        case SessionEvent.MAX_TIMEOUT:
            return SSOTokenEvent.SSO_TOKEN_MAX_TIMEOUT;
        case SessionEvent.LOGOUT:
            return SSOTokenEvent.SSO_TOKEN_DESTROY;
        case SessionEvent.DESTROY:
            return SSOTokenEvent.SSO_TOKEN_DESTROY;
        case SessionEvent.PROPERTY_CHANGED:
            return SSOTokenEvent.SSO_TOKEN_PROPERTY_CHANGED;
        }
        throw new SSOException(SSOProviderBundle.rbName, "invalidevent", null);
    }
}
