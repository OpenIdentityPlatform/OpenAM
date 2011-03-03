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
 * $Id: DefaultEntityManagerImpl.java,v 1.1 2009/11/20 19:31:57 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

import com.sun.identity.oauth.service.util.OAuthProperties;
import com.sun.identity.oauth.service.util.OAuthServiceUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Hua Cui <hua.cui@Sun.Com>
 */
public class DefaultEntityManagerImpl 
       implements EntityManager, OAuthServiceConstants {

   // OpenSSO RESTful core token service endpoint
    private static final String CORE_TOKEN_SERVICE =
                            OAuthProperties.get(PathDefs.OPENSSO_SERVER_URL) +
                            PathDefs.OPENSSO_CORE_TOKEN_SERVICE_ENDPOINT;
    private static Client client = Client.create();
    private static WebResource tokenResource = client.resource(CORE_TOKEN_SERVICE);
    private static final String TOKEN_ID = "token.id";
    private static final String TOKEN_SUBJECT = "token.subject";
    private static final String TOKEN_TYPE = "token.type";
    private static final String TOKEN_EXPIRY = "token.expiry";
    private static final String TOKEN_ETAG = "etag";
    private static final String TOKEN_PREFIX = "oauth.";
    private static final String HEADER_IF_MATCH = "If-Match";
    private static String appTokenId = null;
    private static String cookieName = OAuthProperties.get(PathDefs.OPENSSO_COOKIE_NAME);
    private static Cookie cookie = null;

    /**
     * Creates an entity in the data store.
     *
     * @param entityType the type of the entity
     * @param entitySubject the subject of the entity
     * @param entity the entity to create
     * @param expiry the time until which the entity is valid
     * @return entityId the identifier generated for this entity
     * @throws OAuthServiceException if an error condition occurs
     */
    public String createEntity(String entityType, String entitySubject,
        Map<String, String> entity, Date expiry) 
        throws OAuthServiceException {
        String entityId = null;
        if ((entityType != null) && (entity != null)) {
            JSONObject jb = new JSONObject();
            try {
                jb.append(TOKEN_SUBJECT, entitySubject);
                jb.append(TOKEN_TYPE, entityType);
                jb.append(TOKEN_EXPIRY, DateUtils.toUTCDateFormat(expiry));
                Set<String> keySet = entity.keySet();
                Iterator<String> iter = keySet.iterator();
                while (iter.hasNext()) {
                    String attributeName = iter.next();
                    String attributeValue = entity.get(attributeName);
                    jb.append(TOKEN_PREFIX + attributeName, attributeValue);
                } 
                ClientResponse resp = tokenResource
                    .type("application/json")
                    .cookie(getCookie())
                    .post(ClientResponse.class, jb.toString());
                if (resp != null) {
                    int status = resp.getStatus();
                    if (status != TOKEN_CREATED) {
                        throw new OAuthServiceException(
                          "failed to create the token: status code=" + status);
                    }
                    String respStr = resp.getEntity(String.class);
                    jb = new JSONObject(respStr);
                    JSONArray ja = jb.getJSONArray(TOKEN_ID);
                    if (ja.length() > 0) {
                        entityId = ja.getString(0);
                    }
                }         
            } catch (JSONException je) {
                throw new OAuthServiceException(
                         "failed to create the token", je);
            }         
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
    public Map<String, String> readEntity(String entityId)
            throws OAuthServiceException {
        Map<String, String> entity = null;
        if (entityId != null) {
            JSONObject jb = new JSONObject();
            WebResource readResource = tokenResource.path(entityId);
            ClientResponse resp = readResource
                    .cookie(getCookie())
                    .get(ClientResponse.class);
            if (resp != null) {
                try {
                    String respStr = resp.getEntity(String.class);
                    EntityTag etag = resp.getEntityTag();
                    if (etag == null) {
                        throw new OAuthServiceException(
                                "missing entity tag in response");
                    }
                    jb = new JSONObject(respStr);
                    entity = getMapFromJSONObject(jb);
                    if (entity != null) {
                        entity.put(TOKEN_ETAG, etag.toString());
                    }
                } catch (Exception e) {
                    throw new OAuthServiceException(
                         "failed to read the token", e);
                }         
            }         
        }
        return entity;
    }

    private Map<String, String> getMapFromJSONObject(JSONObject jb)
        throws JSONException {
        Map<String, String> entity = null;
        if ((jb != null) && (jb.length() > 0)) {
            entity = new HashMap<String, String>();
            Iterator iter = jb.keys();
            while (iter.hasNext()) {
                String key = (String)iter.next();
                JSONArray ja = jb.getJSONArray(key);
                String value = ja.getString(0);
                if (key.startsWith(TOKEN_PREFIX)) {
                    key = key.substring(TOKEN_PREFIX.length());
                }
                entity.put(key, value);
            }
        }
        return entity;
    }

    // Converts an attribute map into a MultivaluedMap for query
    private void putAll(MultivaluedMap<String, String> multi,
                        Map<String, String> normal){
        if ((multi != null) && (normal != null)) {
            Set<String> keySet = normal.keySet();
            Iterator<String> iter = keySet.iterator();
            while (iter.hasNext()) {
                String attributeName = iter.next();
                String attributeValue = normal.get(attributeName);
                multi.add(TOKEN_PREFIX + attributeName, attributeValue);
            }
        }
    } 

    /**
     * Searches for entities from the data store.
     *
     * @param entityType the type of the entity
     * @param attributes the attributes to construct the search query
     *
     * @return a list of entity identifiers that satisfy the search criteria
     */
    public List<String> searchEntity(String entityType,
                                     Map<String, String> attributes) {
        List<String> ids = new ArrayList<String>();
        if ((attributes == null) || (attributes.isEmpty())) {
            return ids;
        }
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        putAll(queryParams, attributes); 
        if ((entityType != null) && (entityType.trim().length() != 0)) {
            queryParams.add(TOKEN_TYPE, entityType);
        }
        String resp = tokenResource.queryParams(queryParams)
                    .cookie(getCookie()).get(String.class);
        if (resp != null) {
            try {
                JSONArray ja = new JSONArray(resp);
                if (ja != null) {
                    for (int i = 0; i < ja.length(); i++) {
                        String id = ja.getString(i);
                        ids.add(id);
                    }
                }
            } catch (JSONException je) {
                Logger.getLogger(DefaultEntityManagerImpl.class.getName())
                      .log(Level.INFO, null, je);
            }         
        }         
        return ids;
    }


    /**
     * Updates the state of the given entity from the data store.
     *
     * @param entityId the identifier of the entity
     * @param entity the entity to update
     * @param expiry the time until which the entity is valid
     * @throws OAuthServiceException if an error condition occurs
     */
    public void updateEntity(String entityId, Map<String, String> entity,
            Date expiry) throws OAuthServiceException {
        if (entityId != null) {
            if ((entity != null) && (!entity.isEmpty())) {
                String etag = entity.get(TOKEN_ETAG);
                JSONObject jb = new JSONObject();
                try {
                    jb.append(TOKEN_EXPIRY, DateUtils.toUTCDateFormat(expiry));
                    Set<String> keySet = entity.keySet();
                    Iterator<String> iter = keySet.iterator();
                    while (iter.hasNext()) {
                        String attributeName = iter.next();
                        String attributeValue = entity.get(attributeName);
                        jb.append(TOKEN_PREFIX + attributeName, attributeValue);
                    }
                } catch (JSONException je) {
                    throw new OAuthServiceException(
                         "failed to construct update request", je);
                }
                WebResource updateResource = tokenResource.path(entityId);
                ClientResponse resp = updateResource
                    .type("application/json")
                    .header(HEADER_IF_MATCH, etag)
                    .cookie(getCookie())
                    .put(ClientResponse.class, jb.toString());
                if (resp != null) {
                   int status = resp.getStatus();
                   if (status != TOKEN_UPDATED) {
                       if (status == PRECONDITION_FAILED) {
                           // The entity has been updated after the last read
                           Map<String, String> latestEntity = readEntity(entityId);
                           String entityTag = latestEntity.get(TOKEN_ETAG);
                           entity.put(TOKEN_ETAG, entityTag);
                           updateEntity(entityId, entity, expiry);
                       } else {
                           throw new OAuthServiceException(
                             "failed to update the token: status code=" + status);
                       }
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
    public void deleteEntity(String entityId) throws OAuthServiceException {
        if (entityId != null) {
            WebResource deleteResource = tokenResource.path(entityId);
            ClientResponse resp = deleteResource
                    .cookie(getCookie())
                    .delete(ClientResponse.class);
            if (resp != null) {
               int status = resp.getStatus();
               if (status != TOKEN_UPDATED) {
                   throw new OAuthServiceException(
                     "failed to delete the token: status code=" + status);
               }
            }
        }
    }


    // Gets an application SSO token
    private static String getAppTokenId() {
        if (appTokenId == null) {
            synchronized (DefaultEntityManagerImpl.class) {
                if (appTokenId == null) {
                    try {
                        if (OAuthProperties.isServerMode()) {
                            appTokenId = OAuthServiceUtils.getAdminTokenId();
                        } else {
                            String username = OAuthProperties.get(
                                              PathDefs.APP_USER_NAME);
                            String password = OAuthProperties.get(
                                              PathDefs.APP_USER_PASSWORD);
                            appTokenId = OAuthServiceUtils.authenticate(
                                             username, password, false);
                        }
                    } catch (OAuthServiceException oe) {
                        Logger.getLogger(
                           DefaultEntityManagerImpl.class.getName())
                           .log(Level.SEVERE, null, oe);
                        throw new WebApplicationException(oe);
                    }
                }
            }
        }
        return appTokenId;
    }

    // Gets the cookie for accessing core token service
    private static Cookie getCookie() {
        if (cookie == null) {
            synchronized (DefaultEntityManagerImpl.class) {
                if (cookie == null) {
                    cookie = new Cookie(cookieName, getAppTokenId());
                }
            }
        }
        return cookie;
    }
}
