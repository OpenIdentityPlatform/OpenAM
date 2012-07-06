/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LibertyClientSSOTokenListener.java,v 1.2 2008/06/25 05:48:17 qcheng Exp $
 *
 */


package com.sun.liberty.jaxrpc;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionListener;
import com.sun.identity.plugin.session.SessionManager;

/**
 * This class <code>LibertyClientSSOTokenListener</code> is used
 * to clean up the bootstap cache upon session expiration.
 */
public class LibertyClientSSOTokenListener implements SessionListener {

    public LibertyClientSSOTokenListener() {
    }

    /**
     * Gets notification when session changes state.
     * @param session The session being invalidated
     */
    public void sessionInvalidated(Object session) {
        try {
            String tokenID = SessionManager.getProvider().getSessionID(session);
            String cacheKey = tokenID + LibertyManagerClient.DISCO_RO;
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("LibertyClientSSOTokenListener." +
                    "sessionInvalidated : Session expired. Cleaning cache");
            }
            if (LibertyManagerClient.bootStrapCache.containsKey(cacheKey)) {
                LibertyManagerClient.bootStrapCache.remove(cacheKey);
            }

            cacheKey = tokenID + LibertyManagerClient.DISCO_CRED;
            if (LibertyManagerClient.bootStrapCache.containsKey(cacheKey)) {
                LibertyManagerClient.bootStrapCache.remove(cacheKey);
            }
        } catch (SessionException se) {
            FSUtils.debug.error(
                "LibertyClientSSOTokenListener.sessionInvalidated:", se);
        }
    }
}
