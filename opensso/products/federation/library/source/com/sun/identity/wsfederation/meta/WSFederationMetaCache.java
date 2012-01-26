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
 * $Id: WSFederationMetaCache.java,v 1.4 2009/10/28 23:58:59 exu Exp $
 *
 */

package com.sun.identity.wsfederation.meta;

import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import java.util.Hashtable;

import com.sun.identity.shared.debug.Debug;

import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;

/**
 * The <code>WSFederationMetaCache</code> provides a metadata cache for the
 * WS-Federation implementation.
 */
class WSFederationMetaCache
{
    private static Debug debug = WSFederationMetaUtils.debug;
    private static Hashtable federationCache = new Hashtable();
    private static Hashtable configCache = new Hashtable();

    /*
     * Private constructor ensure that no instance is ever created
     */
    private WSFederationMetaCache() {
    }

    /**
     * Returns the standard metadata entity descriptor under the realm from
     * cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return <code>FederationElement</code> for the entity or null
     *         if not found. 
     */
    static FederationElement getFederation(String realm, String federationId) {
        String cacheKey = buildCacheKey(realm, federationId);
        FederationElement federation =
	    (FederationElement)federationCache.get(cacheKey);
        if (debug.messageEnabled()) {
            debug.message("WSFederationMetaCache.getEntityDescriptor: " + 
                "cacheKey = " + cacheKey + ", found = " + (federation != null));
        }
        return federation;
    }

    /**
     * Adds the standard metadata entity descriptor under the realm to cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @param descriptor <code>FederationElement</code> for the entity. 
     */
    static void putFederation(String realm, String federationId,
            FederationElement federation)
    {
        String cacheKey = buildCacheKey(realm, federationId);
        if (federation != null) {
            if (debug.messageEnabled()) {
                debug.message("WSFederationMetaCache.putFederation: " + 
                    "cacheKey = " + cacheKey);
            }
            federationCache.put(cacheKey, federation);
        } else {
            if (debug.messageEnabled()) {
                debug.message(
                    "WSFederationMetaCache.putFederation: delete cacheEey = " +
                    cacheKey);
            }
            federationCache.remove(cacheKey);
            configCache.remove(cacheKey);
        }
    }

    /**
     * Returns extended entity configuration under the realm from cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return <code>FederationConfigElement</code> object for the entity or 
     * null if not found.
     */
    static FederationConfigElement getEntityConfig(
            String realm, String entityId)
    {
        String cacheKey = buildCacheKey(realm, entityId);
        FederationConfigElement config =
	    (FederationConfigElement)configCache.get(cacheKey);
        if (debug.messageEnabled()) {
            debug.message("SAML2MetaCache.getEntityConfig: cacheKey = " +
			  cacheKey + ", found = " + (config != null));
        }
        return config;
    }

    /**
     * Adds extended entity configuration under the realm to cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @param config <code>FederationConfigElement</code> object for the entity.
     */
    static void putEntityConfig(String realm, String entityId,
        FederationConfigElement config) {
        String cacheKey = buildCacheKey(realm, entityId);
        if (config != null) {
            if (debug.messageEnabled()) {
                debug.message("SAML2MetaCache.putEntityConfig: cacheKey = " +
                    cacheKey);
            }
            configCache.put(cacheKey, config);
        } else {
            if (debug.messageEnabled()) {
                debug.message(
                    "SAML2MetaCache.putEntityConfig: delete cacheKey = " +
                    cacheKey);
            }
            configCache.remove(cacheKey);
        }
    }

    /**
     * Clears cache completely.
     */
    static void clear() {
	federationCache.clear();
	configCache.clear();
    }

    /**
     * Build cache key for federationCache and configCache based on realm and
     * entity ID.
     * @param realm The realm under which the entity resides.
     * @param entityID The entity ID or the name of circle of trust.
     * @return The cache key.
     */
    private static String buildCacheKey(String realm, String entityId) {
        return realm + "//" + entityId;
    }
}
