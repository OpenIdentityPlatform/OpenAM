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
 * $Id: DiscoveryDataCache.java,v 1.2 2008/06/25 05:49:45 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.console.service.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenListener;
import com.sun.identity.shared.encode.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/* - NEED NOT LOG - */

/**
 * This class caches discover data object for Console.  Token ID and a randomly
 * generated string are used as key to cache and retrieve a discovery data.
 */
public class DiscoveryDataCache
    implements SSOTokenListener
{
    private static DiscoveryDataCache instance = new DiscoveryDataCache();
    private Map mapTokenIDs = new HashMap(100);

    /**
     * The generated random string is used to cache discover data object when
     * we switch from on tab to another. Since it is used from caching 
     * purposes, usage of secure random is not required.
     */
    private static Random random = new Random();

    private DiscoveryDataCache() {
    }

    /**
     * Gets an instance of discover data cache
     *
     * @return an instance of discover data cache
     */
    public static DiscoveryDataCache getInstance() {
	return instance;
    }

    /**
     * Caches a discovery data object
     *
     * @param token single sign on token
     * @param data Discovery data object to be cached
     * @return an unique key for retrieve this discover data in future
     */
    public String cacheData(SSOToken token, SMDiscoveryServiceData data) {
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
		map.put(randomStr, data);
		mapTokenIDs.put(key, map);
	    }
	} catch (SSOException ssoe) {
	    randomStr = "";
	}
	return randomStr;
    }

    /**
     * Returns cached discovery data object
     *
     * @param token single sign on token
     * @param cacheID Key for retrieve this discovery data.
     * @return discovery data object if it is found. otherwises, return null
     */
    public SMDiscoveryServiceData getData(SSOToken token, String cacheID) {
	SMDiscoveryServiceData data = null;
	String key = token.getTokenID().toString();
	Map map = (Map) mapTokenIDs.get(key);
	if (map != null) {
	    data = (SMDiscoveryServiceData)map.get(cacheID);
	}
	return data;
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
		clearAllData(token.getTokenID());
		break;
	    }
	} catch (SSOException ssoe) {
            // TBD
	}
    }

    /**
     * Clears all cached discovery data of a given single sign on token ID
     *
     * @param tokenID single sign on token ID
     */
    protected void clearAllData(SSOTokenID tokenID) {
	boolean removed = false;
	String key = tokenID.toString();

	synchronized(mapTokenIDs) {
	    removed = (mapTokenIDs.remove(key) != null);
	}
    }

    /**
     * Gets a random string
     *
     * @return random string
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
