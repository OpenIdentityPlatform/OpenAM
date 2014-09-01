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
 * $Id: PolicyCache.java,v 1.2 2008/06/25 05:43:07 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.console.policy.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
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
 * This class caches policy object for Console.  Token ID and a randomly
 * generated string are used as key to cache and retrieve a policy.
 */
public class PolicyCache
    implements SSOTokenListener
{        
    private static PolicyCache instance = new PolicyCache();
    private Map mapTokenIDs = new HashMap(100);

    /**
     * The generated random string is used to cache policy object when
     * we switch from on tab to another. Since it is used from caching 
     * purposes, usage of secure random is not required.
     */
    private static Random random = new Random();

    private PolicyCache() {
    }

    /**
     * Gets an instance of policy cache
     *
     * @return an instance of policy cache
     */
    public static PolicyCache getInstance() {
        return instance;
    }

    /**
     * Caches a policy object
     *
     * @param token single sign on token
     * @param policy Policy object to be cached
     * @return an unique key for retrieve this policy in future
     */
    public String cachePolicy(SSOToken token, CachedPolicy policy) {
        String randomStr = "";
        if (policy != null) {
            try {
                String key = token.getTokenID().toString();

                synchronized(mapTokenIDs) {
                    Map map = (Map) mapTokenIDs.get(key);

                    if (map == null) {
                        map = new HashMap(10);
                        token.addSSOTokenListener(this);
                    }

                    randomStr = getRandomString();
                    map.put(randomStr, policy);
                    mapTokenIDs.put(key, map);
                }
            } catch (SSOException ssoe) {
                AMModelBase.debug.warning("PolicyCache.cachePolicy", ssoe);
                randomStr = "";
            }
        }

        return randomStr;
    }

    /**
     * Set a policy object with a given ID.
     *
     * @param token Single sign on token.
     * @param cachedID ID of cached policy.
     * @param policy Policy object to be cached.
     */
    public void setPolicy(SSOToken token, String cachedID, CachedPolicy policy){
        if (policy != null) {
            try {
                String key = token.getTokenID().toString();

                synchronized(mapTokenIDs) {
                    Map map = (Map)mapTokenIDs.get(key);

                    if (map == null) {
                        map = new HashMap(10);
                        token.addSSOTokenListener(this);
                    }

                    map.put(cachedID, policy);
                    mapTokenIDs.put(key, map);
                }
            } catch (SSOException ssoe) {
                AMModelBase.debug.warning("PolicyCache.replacePolicy", ssoe);
            }
        }
    }

    /**
     * Returns cached policy object
     *
     * @param token single sign on token
     * @param cacheID Key for retrieve this policy
     * @return policy Policy object.
     * @throws AMConsoleException if policy object cannot be located.
     */
    public CachedPolicy getPolicy(SSOToken token, String cacheID)
        throws AMConsoleException
    {
        CachedPolicy policy = null;
        String key = token.getTokenID().toString();
        Map map = (Map) mapTokenIDs.get(key);

        if (map != null) {
            policy = (CachedPolicy) map.get(cacheID);
        }

        if (policy == null) {
            throw new 
                AMConsoleException("Cannot locate cached policy " + cacheID);
        }

        return policy;
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
                clearAllPolicies(token.getTokenID());
                break;
            }
        } catch (SSOException ssoe) {
            AMModelBase.debug.warning("PolicyCache.ssoTokenChanged", ssoe);
        }
    }

    /**
     * Clears all cached policy of a given single sign on token ID
     *
     * @param tokenID single sign on token ID
     */
    protected void clearAllPolicies(SSOTokenID tokenID) {
        boolean removed = false;
        String key = tokenID.toString();

        synchronized(mapTokenIDs) {
            removed = (mapTokenIDs.remove(key) != null);
        }

        if (removed && AMModelBase.debug.messageEnabled()) {
            AMModelBase.debug.warning("PolicyCache.clearAllPolicies," + key);
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
