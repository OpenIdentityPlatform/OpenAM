/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PageTrailManager.java,v 1.3 2008/07/10 23:27:22 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.console.base;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenListener;
import com.sun.identity.shared.encode.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This singleton class governs the tracking of page trail per user session.
 */
public class PageTrailManager
    implements SSOTokenListener
{
     private static Debug debug = Debug.getInstance(
        AMAdminConstants.CONSOLE_DEBUG_FILENAME);
     
    private static PageTrailManager instance = new PageTrailManager();
    private Map mapTokenIDs = new HashMap(100);

    /**
     * The generated random string is used to cache page trail object when
     * we switch from on tab to another. Since it is used from caching
     * purposes, usage of secure random is not required.
     */
    private static Random random = new Random();

    private PageTrailManager() {
    }

    public static PageTrailManager getInstance() {
        return instance;
    }

    /**
     * Registers a page trail.
     *
     * @param token single sign on token
     * @param pageTrail Page Trail.
     * @return an unique key for retrieve this object in future
     */
    public String registerTrail(SSOToken token, PageTrail pageTrail) {
        String randomStr = "";
        try {
            String key = token.getTokenID().toString();
                                                                                
            synchronized(mapTokenIDs) {
                Map map = (Map) mapTokenIDs.get(key);
                                                                                
                if (map == null) {
                    map = new HashMap(10);
                    token.addSSOTokenListener(this);
                }
                                                                                
                randomStr = getRandomString();
                map.put(randomStr, pageTrail);
                mapTokenIDs.put(key, map);
            }
        } catch (SSOException ssoe) {
            debug.warning("PageTrailManager.registerTrail()", ssoe);
            randomStr = "";
        }
        return randomStr;
    }

    /**
     * Returns cached page trail.
     *
     * @param token single sign on token
     * @param cacheID Key for retrieve this page trail
     * @return page trail object if it is found. otherwises, return null
     */
    public PageTrail getTrail(SSOToken token, String cacheID) {
        Map map = (Map)mapTokenIDs.get(token.getTokenID().toString());
        return (map != null) ? (PageTrail)map.get(cacheID) : null;
    }

    /**
     * Gets notification when single sign on token changes state.
     *
     * @param evt single sign on token event
     */
    public void ssoTokenChanged(SSOTokenEvent evt) {
        try {
            int type = evt.getType();
                                                                                
            switch (type) {
            case SSOTokenEvent.SSO_TOKEN_IDLE_TIMEOUT:
            case SSOTokenEvent.SSO_TOKEN_MAX_TIMEOUT:
            case SSOTokenEvent.SSO_TOKEN_DESTROY:
                SSOToken token = evt.getToken();
                clearAllTrails(token.getTokenID());
                break;
            }
        } catch (SSOException ssoe) {
            debug.warning("PageTrailManager.ssoTokenChanged()", ssoe);
        }
    }

    /**
     * Clears all registered page trails of a given single sign on token ID
     *
     * @param tokenID single sign on token ID
     */
    protected void clearAllTrails(SSOTokenID tokenID) {
        boolean removed = false;
        String key = tokenID.toString();
        synchronized(mapTokenIDs) {
            removed = (mapTokenIDs.remove(key) != null);
        }
    }

    /**
     * Returns random string.
     *
     * @return random string.
     */
    private static String getRandomString() {
        StringBuilder sb = new StringBuilder(30);
        byte[] keyRandom = new byte[5];
        random.nextBytes(keyRandom);
        sb.append(System.currentTimeMillis());
        sb.append(Base64.encode(keyRandom));
        return (sb.toString());
    }
}
