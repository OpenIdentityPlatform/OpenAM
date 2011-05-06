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
 * $Id: EntityManager.java,v 1.1 2009/11/20 19:31:57 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The OAuth Service Entity Manager 
 *
 * @author Hua Cui <hua.cui@Sun.COM>
 */
public interface EntityManager {

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
    public String createEntity(String entityType, String entitySubject,
      Map<String, String> entity, Date expiry) throws OAuthServiceException;

    /**
     * Reads an entity from the data store.
     *
     * @param entityId the identifier of the entity
     * @return entity the entity to read
     * @throws OAuthServiceException if an error condition occurs
     */
    public Map<String, String> readEntity(String entityId) throws OAuthServiceException;


    /**
     * Searches for entities from the data store.
     *
     * @param entityType the type of the entity
     * @param attributes the attributes to construct the search query
     *
     * @return a list of entity identifiers that satisfy the search criteria
     */
    public List<String> searchEntity(String entityType,
                         Map<String, String> attributes);


    /**
     * Updates the state of the given entity from the data store.
     *
     * @param entityId the identifier of the entity 
     * @param entity the entity to update
     * @param expiry the time until which the entity is valid
     * @throws OAuthServiceException if an error condition occurs
     */
    public void updateEntity(String entityId, Map<String, String> entity,
            Date expiry) throws OAuthServiceException;


    /**
     * Deletes an entity from the data store.
     *
     * @param entityId the identifier of the entity
     * @throws OAuthServiceException if an error condition occurs
     */
    public void deleteEntity(String entityId) throws OAuthServiceException;

}
