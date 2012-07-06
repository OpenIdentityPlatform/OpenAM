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
 * $Id: COTCache.java,v 1.2 2008/06/25 05:46:38 qcheng Exp $
 *
 */
package com.sun.identity.cot;


import java.util.Hashtable;
import com.sun.identity.shared.debug.Debug;

/**
 * This class caches circle of trust data.
 */
public class COTCache {
    
    private static Hashtable cotCache = new Hashtable();
    private static Debug debug = COTUtils.debug;
    
    /**
     * Constructor.
     */
    private COTCache() {
    }
    
    
    /**
     * Returns the circle of trust descriptor under the realm from
     * cache.
     *
     * @param realm the realm under which the circle of trust resides.
     * @param name the circle of trust name.
     * @return <code>CircleOfTrustDescriptor</code> for the circle of trust
     *        or null if not found.
     */
    static CircleOfTrustDescriptor getCircleOfTrust(String realm, String name) {
        String classMethod = "CircleOfDescriptorCache:getCircleOfTrust:";
        String cacheKey = buildCacheKey(realm, name);
        CircleOfTrustDescriptor cotDesc =
                (CircleOfTrustDescriptor)cotCache.get(cacheKey);
        if (COTUtils.debug.messageEnabled()) {
            COTUtils.debug.message(classMethod + "cacheKey = " + cacheKey +
                    ", found = " + (cotDesc != null));
        }
        return cotDesc;
    }
    
    /**
     * Updates the Circle of Trust Cache.
     *
     * @param realm The realm under which the circle of trust resides.
     * @param name Name of the circle of trust.
     * @param cotDescriptor <code>COTDescriptor</code> for the
     *                circle of trust.
     */
    static void putCircleOfTrust(String realm, String name,
            CircleOfTrustDescriptor cotDescriptor) {
        String classMethod = "CircleOfTrustCache:putCircleOfTrust";
        String cacheKey = buildCacheKey(realm, name);
        
        if (debug.messageEnabled()) {
            debug.message(classMethod + ": cacheKey = " + cacheKey);
        }
        cotCache.put(cacheKey, cotDescriptor);
    }
    
    /**
     * Clears the circle of trust cache.
     */
    static void clear() {
        cotCache.clear();
    }
    
    /**
     * Builds cache key for circle of trust cache.
     *
     * @param realm the realm to which the circle of trust belongs.
     * @param cotName the name of the circle of trust.
     * @return the cache key.
     */
    private static String buildCacheKey(String realm, String cotName) {
        return realm + "//" + cotName;
    }
}
