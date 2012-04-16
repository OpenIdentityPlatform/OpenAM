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
 * $Id: SAML2MetaCache.java,v 1.4 2008/07/08 01:08:43 exu Exp $
 *
 */

 /*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.saml2.meta;

import java.util.Hashtable;

import com.sun.identity.shared.debug.Debug;

import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadataattr.EntityAttributesType;
import com.sun.identity.saml2.jaxb.metadataattr.EntityAttributesElement;
import com.sun.identity.saml2.jaxb.metadataattr.ObjectFactory;

/**
 * The <code>SAML2MetaCache</code> provides metadata cache.
 */
class SAML2MetaCache
{
    private static Debug debug = SAML2MetaUtils.debug;

    private static Hashtable descriptorCache = new Hashtable();
    private static Hashtable configCache = new Hashtable();

    private SAML2MetaCache() {
    }

    /**
     * Returns the standard metadata entity descriptor under the realm from
     * cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @return <code>EntityDescriptorElement</code> for the entity or null
     *         if not found. 
     */
    static EntityDescriptorElement getEntityDescriptor(
            String realm, String entityId) 
    {
        String cacheKey = buildCacheKey(realm, entityId);
        EntityDescriptorElement descriptor =
	    (EntityDescriptorElement)descriptorCache.get(cacheKey);
        if (debug.messageEnabled()) {
            debug.message("SAML2MetaCache.getEntityDescriptor: cacheKey = " +
                          cacheKey + ", found = " + (descriptor != null));
        }
        return descriptor;
    }

    /**
     * Adds the standard metadata entity descriptor under the realm to cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved. 
     * @param descriptor <code>EntityDescriptorElement</code> for the entity. 
     */
    static void putEntityDescriptor(String realm, String entityId,
            EntityDescriptorElement descriptor)
    {
        String cacheKey = buildCacheKey(realm, entityId);
        if (descriptor != null) {
            if (debug.messageEnabled()) {
                debug.message("SAML2MetaCache.putEntityDescriptor: cacheKey = " +
                    cacheKey);
            }
            descriptorCache.put(cacheKey, descriptor);
        } else {
            if (debug.messageEnabled()) {
                debug.message(
                    "SAML2MetaCache.putEntityDescriptor: delete cacheEey = " +
                    cacheKey);
            }
            descriptorCache.remove(cacheKey);
            configCache.remove(cacheKey);
        }
    }

    /**
     * Returns extended entity configuration under the realm from cache.
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @return <code>EntityConfigElement</code> object for the entity or null
     *         if not found.
     */
    static EntityConfigElement getEntityConfig(
            String realm, String entityId)
    {
        String cacheKey = buildCacheKey(realm, entityId);
        EntityConfigElement config =
	    (EntityConfigElement)configCache.get(cacheKey);
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
     * @param config <code>EntityConfigElement</code> object for the entity.
     */
    static void putEntityConfig(String realm, String entityId,
        EntityConfigElement config) {
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
        if (debug.messageEnabled()) {
            debug.message("SAML2MetaCache.clear() called");
        }
	descriptorCache.clear();
	configCache.clear();
    }

    /**
     * Build cache key for descriptorCache and configCache based on realm and
     * entity ID.
     * @param realm The realm under which the entity resides.
     * @param entityID The entity ID or the name of circle of trust.
     * @return The cache key.
     */
    private static String buildCacheKey(String realm, String entityId) {
        return realm + "//" + entityId;
    }
}
