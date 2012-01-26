/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EntityManagerImpl.java,v 1.1 2009/11/20 19:31:57 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

import com.sun.identity.oauth.service.util.UniqueRandomString;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of the EntityManager interface
 *
 * @author Hua Cui <hua.cui@Sun.COM>
 */
public class EntityManagerImpl implements EntityManager {

    // Cache uses entity types as the keys and entity caches as values
    static Map<String, Map> entityCaches = new HashMap<String, Map>();

    /**
     * Creates an entity into the data store.
     *
     * @param entityType the type of the entity
     * @param entitySubject the subject of the entity
     * @param entity the entity to create
     * @param expiry the time until which the entity is valid
     * @return entityId the identifier generated for this entity
     * @throws OAuthServiceException if an error condition occurs
     */
    public synchronized String createEntity(String entityType, String entitySubject,
        Map<String, String> entity, Date expiry) throws OAuthServiceException {
        String entityId = null;
        if ((entityType != null) && (entity != null)) {
            Map<String, EntityWithExpiry> entityCache = entityCaches.get(entityType);
            if (entityCache == null) {
                entityCache = new HashMap<String, EntityWithExpiry>();
                entityCaches.put(entityType, entityCache);
            }
            entityId = (new UniqueRandomString()).getString();
            entityCache.put(entityId, new EntityWithExpiry(entity, expiry));
        }
        return entityId;
    }

    /**
     * Reads an entity from the data store.
     *
     * @param entityId the identifier of the entity
     * @return entity the entity to read
     * @throws OAuthServiceException if an error condition occurs
     */
    public synchronized Map<String, String> readEntity(String entityId)
            throws OAuthServiceException {
        Map<String, String> entity = null;
        if (entityId != null) {
            Set<String> keySet = entityCaches.keySet();
            Iterator<String> iter = keySet.iterator();
            while (iter.hasNext()) {
                String entityType = iter.next();
                Map<String, EntityWithExpiry> entityCache =
                                     entityCaches.get(entityType);
                if (entityCache != null) {
                    EntityWithExpiry tsEntity = entityCache.get(entityId);
                    if (tsEntity != null) {
                        entity = tsEntity.getEntity();
                        break;
                    }
                }
            }
        }
        return entity;
    }

    /**
     * Searches for entities from the data store.
     *
     * @param entityType the type of the entity
     * @param attributes the attributes to construct the search query
     *
     * @return a list of entity identifiers that satisfy the search criteria
     */
    public synchronized List<String> searchEntity(String entityType,
                                     Map<String, String> attributes) {
        List<String> ids = new ArrayList<String>();
        if ((attributes == null) || (attributes.isEmpty())) {
            return ids;
        }
        Map<String, String> entity = null;
        Map<String, EntityWithExpiry> entityCache = null;
        if ((entityType == null) || (entityType.trim().length() == 0)) {
            Set<String> keySet = entityCaches.keySet();
            Iterator<String> iter = keySet.iterator();
            while (iter.hasNext()) {
                entityType = iter.next();
                entityCache = entityCaches.get(entityType);
                if (entityCache != null) {
                    Set<String> keys = entityCache.keySet();
                    Iterator<String> it = keys.iterator();
                    while (it.hasNext()) {
                        String entityId = it.next();
                        EntityWithExpiry tsEntity = entityCache.get(entityId);
                        if (tsEntity != null) {
                            entity = tsEntity.getEntity();
                            if ((entity != null) && (!entity.isEmpty())) {
                                if (contains(entity, attributes)) {
                                    ids.add(entityId);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            entityCache = entityCaches.get(entityType);
            if (entityCache != null) {
                Set<String> keys = entityCache.keySet();
                Iterator<String> it = keys.iterator();
                while (it.hasNext()) {
                    String entityId = it.next();
                    EntityWithExpiry tsEntity = entityCache.get(entityId);
                    if (tsEntity != null) {
                        entity = tsEntity.getEntity();
                        if ((entity != null) && (!entity.isEmpty())) {
                            if (contains(entity, attributes)) {
                                ids.add(entityId);
                            }
                        }
                    }
                }
            }
        }
        return ids;
    }

    // check if map1 contains map2
     private boolean contains(Map<String, String> map1,
                              Map<String, String> map2) {
         if ((map1.isEmpty() || map2.isEmpty())) {
             return false;
         }
         Set<String> keys = map2.keySet();
         Iterator<String> iter = keys.iterator();
         while (iter.hasNext()) {
             String key = iter.next();
             String value = map2.get(key);
             if (!value.equals(map1.get(key))) {
                 return false;
             }
         }
         return true;
     }
    
    /**
     * Updates the state of the given entity from the data store.
     *
     * @param entityId the identifier of the entity
     * @param entity the entity to update
     * @param expiry the time until which the entity is valid
     * @throws OAuthServiceException if an error condition occurs
     */
    public synchronized void updateEntity(String entityId, Map<String, String> entity,
            Date expiry) throws OAuthServiceException {
        if (entityId != null) {
            Set<String> keySet = entityCaches.keySet();
            Iterator<String> iter = keySet.iterator();
            while (iter.hasNext()) {
                String entityType = iter.next();
                Map<String, EntityWithExpiry> entityCache =
                                     entityCaches.get(entityType);
                if (entityCache != null) {
                    EntityWithExpiry tsEntity = entityCache.get(entityId);
                    if (tsEntity != null) {
                        entityCache.put(entityId, new EntityWithExpiry(entity, expiry));
                        break;
                    }
                }
            }
        }
    }


    /**
     * Deletes an entity from the data store.
     *
     * @param entityId the identifier of the entity
     * @throws OAuthServiceException if an error condition occurs
     */
    public synchronized void deleteEntity(String entityId) throws OAuthServiceException {
        if (entityId != null) {
            Set<String> keySet = entityCaches.keySet();
            Iterator<String> iter = keySet.iterator();
            while (iter.hasNext()) {
                String entityType = iter.next();
                Map<String, EntityWithExpiry> entityCache =
                                     entityCaches.get(entityType);
                if (entityCache != null) {
                    EntityWithExpiry tsEntity = entityCache.get(entityId);
                    if (tsEntity != null) {
                        entityCache.remove(entityId);
                        break;
                    }
                }
            }
        }
    }

    class EntityWithExpiry {
        private Map<String, String> entity;
        private Date expiry;

        EntityWithExpiry(Map<String, String> entity, Date expiry) {
            this.entity = entity;
            this.expiry = expiry;
        }

        Map<String, String> getEntity() {
            return entity;
        }        

        void setEntity(Map<String, String> entity) {
            this.entity = entity;
        }

        Date getExpiry() {
            return expiry;
        }
   
        void setExpiry(Date expiry) {
            this.expiry = expiry;
        }
    }
}
