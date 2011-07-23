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
 * $Id: IDFFMetaCache.java,v 1.4 2008/11/10 22:56:57 veiming Exp $
 *
 */

package com.sun.identity.federation.meta;


import com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement;
import com.sun.identity.shared.debug.Debug;
import java.util.Hashtable;

/**
 *
 * This class provides methods for caching the 
 * Federation metadata.
 */
public class IDFFMetaCache {
    
    private static Debug debug = IDFFMetaUtils.debug;

    private static Hashtable entityDescriptorCache = new Hashtable();
    private static Hashtable entityConfigCache = new Hashtable();
    private static Hashtable metaAliasEntityCache = new Hashtable();
    private static Hashtable metaAliasRoleCache = new Hashtable();
    private static Hashtable entitySuccinctIDCache = new Hashtable();
    
    /** 
     * Default Constructor.
     */
    private IDFFMetaCache() {
    }
    
    /**
     * Returns the Entity Descriptor representing the standard metadata under
     * the realm from cache.
     *
     * @param realm The realm under which the entity resides.
     * @param entityID the entity descriptor identifier.
     * @return <code>EntityDescriptorElement</code> for the entity or null
     *         if not found. 
     */
    public static EntityDescriptorElement getEntityDescriptor(
        String realm, String entityID)
    {
        String classMethod = "IDFFMetaCache:getEntityDescriptor" ;
        String cacheKey = buildCacheKey(realm, entityID);
        EntityDescriptorElement entityDescriptor =
            (EntityDescriptorElement)entityDescriptorCache.get(cacheKey);
        if (debug.messageEnabled()) {
            if (entityDescriptor != null) {
                debug.message(classMethod + " Entity Descriptor found for : "
                        + cacheKey );
            } else {
                debug.message(classMethod + "EntityDescriptor not found for :"
                        + cacheKey );
            }
        }
        return entityDescriptor;
    }
    
    /**
     * Updates the Entity Descriptor cache with the Entity Descriptor.
     *
     * @param realm The realm under which the entity resides.
     * @param entityID entity descriptor identifier. 
     * @param entityDescriptor <code>EntityDescriptorElement</code> of
     *        the entity.
     */
    public static void setEntityDescriptor(String realm, String entityID,
            EntityDescriptorElement entityDescriptor) {
        String cacheKey = buildCacheKey(realm, entityID);
        if (entityDescriptor != null) {
            entityDescriptorCache.put(cacheKey,entityDescriptor);
        } else {
            entityDescriptorCache.remove(cacheKey);
            entityConfigCache.remove(cacheKey);   
        }
    }
    
    /**
     * Returns the Entity Config under the realm from the cache.
     *
     * @param realm The realm under which the entity resides.
     * @param entityID the entity config identifier.
     * @return <code>EntityConfigElement</code> object for the entity or null
     *         if not found.
     */
    public static EntityConfigElement getEntityConfig(
        String realm, String entityID) {
        String classMethod = "IDFFMetaCache:getEntityConfig";
        String cacheKey = buildCacheKey(realm, entityID);
        EntityConfigElement entityConfig =
                (EntityConfigElement)entityConfigCache.get(cacheKey);
        if (debug.messageEnabled()) {
            if (entityConfig != null) {
                debug.message(classMethod + "Entity Config found for "
                              + cacheKey);
            } else {
                debug.message(classMethod + "Entity Config not found for "
                             + cacheKey);
            }
        }
        return entityConfig;
    }
    
    /**
     * Updates the the Entity Configuration Cache with Entity Config.
     * @param realm The realm under which the entity resides.
     * @param entityID ID of the entity to be retrieved.
     * @param entityConfig Entity Configuration Object.
     */
    public static void setEntityConfig(String realm, String entityID,
            EntityConfigElement entityConfig) {
        String cacheKey = buildCacheKey(realm, entityID);
        if (entityConfig != null) {
            entityConfigCache.put(cacheKey,entityConfig);
        } else {
            entityConfigCache.remove(cacheKey);
        }
    }

    /**    
     * Returns Entity ID containing IDP or SP with the meta alias.
     * @param metaAlias Meta Alias of the provider to be retrieved.
     * @return Entity ID corresponding to the meta alias, return null if
     *     the Entity ID does not exist in the cache.
     */
    public static String getEntityByMetaAlias(String metaAlias) {
        return (String) metaAliasEntityCache.get(metaAlias);
    }

    /**    
     * Updates Entity ID containing IDP or SP with the meta alias.
     * @param metaAlias Meta Alias of the provider to be set. 
     * @param entityID Entity ID corresponding to provider with the Meta Alias.
     */
    public static void setMetaAliasEntityMapping(String metaAlias,
        String entityID) {
        metaAliasEntityCache.put(metaAlias, entityID);
    }

    /**    
     * Returns provider role corresponding to the meta alias.
     * @param metaAlias Meta Alias of the provider to be retrieved.
     * @return role of the provider with the meta alias, 
     *     return null if the meta alias does not exist in the cache.
     */
    public static String getRoleByMetaAlias(String metaAlias) {
        return (String) metaAliasRoleCache.get(metaAlias);
    }

    /**    
     * Updates provider role corresponding to the meta alias.
     * @param metaAlias Meta Alias of the provider to be set. 
     * @param role Role of the provider with the meta alias.
     */
    public static void setMetaAliasRoleMapping(String metaAlias,
        String role) {
        metaAliasRoleCache.put(metaAlias, role);
    }

    /**    
     * Returns entity ID containing the IDP with the succinct ID.
     * @param succinctId Succinct ID of the IDP.
     * @return entity ID containing the IDP with the succinct ID, 
     *     return null if the entity ID does not exist in the cache.
     */
    public static String getEntityBySuccinctID(String succinctId) {
        return (String) entitySuccinctIDCache.get(succinctId);
    }

    /**    
     * Updates the Entity ID containing the IDP with the succinct ID.
     * @param succinctId Succinct ID of the IDP to be updated.
     * @param entityId Entity ID containing the IDP with the succinct ID.
     */
     public static void setEntitySuccinctIDMapping(String succinctId,
        String entityId) {
        entitySuccinctIDCache.put(succinctId, entityId);
    }

    /**
     * Clears the Entity Descriptor and Entity Config cache.
     */
    public static synchronized void clearCache() {
        entityDescriptorCache.clear();
        entityConfigCache.clear();
        metaAliasEntityCache.clear();
        metaAliasRoleCache.clear();
        entitySuccinctIDCache.clear();
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
