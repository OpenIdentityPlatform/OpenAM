/*
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
 * $Id: SSOSessionListener.java,v 1.2 2008/06/25 05:41:43 qcheng Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 * Portions Copyrighted 2022 Open Identity Platform Community
 */

package com.iplanet.sso.providers.dpro;

import org.forgerock.util.Reject;

import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionListener;
import com.iplanet.sso.SSOTokenListener;

/**
 * This Class <code>SOSessionListener</code> implements 
 * <code>SessionListener</code> interface. 
 * For every SSOToken a listener is added to Session using SessionListener. 
 * In the event of any changes to the Session, the sessionChanged method is 
 * triggered, which calls the ssoTokenChanged
 * 
 */
public class SSOSessionListener implements SessionListener {
    
    private final SSOTokenListener ssoListener;

    public SSOSessionListener(SSOTokenListener listener) {
        Reject.ifNull(listener);
        ssoListener = listener;
    }

    @Override
    public void sessionChanged(SessionEvent sessionEvent) {
        switch (sessionEvent.getType()) {
        case IDLE_TIMEOUT:
        case MAX_TIMEOUT:
        case LOGOUT:
        case DESTROY:
        case PROPERTY_CHANGED:
        case EVENT_URL_ADDED:
            tryToPropagateEventToTokenListeners(sessionEvent);
            break;
        default:
            // ignore all other session event types
        }
    }

    private void tryToPropagateEventToTokenListeners(SessionEvent sessionEvent) {
        try {
            ssoListener.ssoTokenChanged(new SSOTokenEventImpl(sessionEvent));
        } catch (Throwable t) {
            // SSOTokenListener should be responsible for handling its own errors
            SSOProviderImpl.debug.error("Unknown Error in calling ssoTokenChanged method", t);
        }
    }
}
