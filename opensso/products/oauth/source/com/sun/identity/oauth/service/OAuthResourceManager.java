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
 * $Id: OAuthResourceManager.java,v 1.2 2010/01/20 17:51:37 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

import com.sun.identity.oauth.service.models.AccessToken;
import com.sun.identity.oauth.service.models.Consumer;
import com.sun.identity.oauth.service.models.RequestToken;
import com.sun.identity.shared.DateUtils;
import java.util.ArrayList;
import java.util.Date;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * The OAuth Service Resource Manager
 * This class manages the creation, read, update, search, and deletion
 * of the OAuth Token Service related entities, i.e. OAuth consumers, 
 * OAuth request tokens, and OAuth access Tokens.
 *
 * @author Hua Cui <hua.cui@Sun.COM>
 */
public class OAuthResourceManager implements OAuthServiceConstants {

    private static OAuthResourceManager instance = new OAuthResourceManager();
    private EntityManager em;

    private OAuthResourceManager() {
        // Use ServiceLoader API to find the configured EntityManager
        // implementation. If not found, fall back to the default
        // implementation DefaultEntityManagerImpl.
        ServiceLoader<EntityManager> entityManagers = 
                      ServiceLoader.load(EntityManager.class);
        if (entityManagers != null && entityManagers.iterator().hasNext()) {
            em = entityManagers.iterator().next();
        } else {
            em = new DefaultEntityManagerImpl();
        }    
    }

    /**
     * Returns an instance of OAuthResourceManager.
     *
     * @return an instance of OAuthResourceManager
     */
    public static OAuthResourceManager getInstance() {
        return instance;
    }

    /**
     * Creates a consumer entity into the data store.
     *
     * @param entitySubject the subject corresponding to the entity
     * @param consumer the consumer entity to create in the data store
     * @throws OAuthServiceException if an error condition occurs
     */
    public void createConsumer(String entitySubject, Consumer consumer)
        throws OAuthServiceException {
        if (consumer == null) {
            throw new OAuthServiceException("The consumer is null");
        }
        Map<String, String> attributes = new HashMap<String, String>();
        String consumerName = consumer.getConsName();
        attributes.put(CONSUMER_NAME, consumerName);
        String consumerSecret = consumer.getConsSecret();
        attributes.put(CONSUMER_SECRET, consumerSecret);
        String consumerRASKey = consumer.getConsRsakey();
        attributes.put(CONSUMER_RSA_KEY, consumerRASKey);
        String consumerKey = consumer.getConsKey();
        attributes.put(CONSUMER_KEY, consumerKey);
        Date longExpiry = new Date(System.currentTimeMillis() + 86400000 * 1000);
        String consumerId = em.createEntity(CONSUMER_TYPE, entitySubject,
                                            attributes, longExpiry);
        consumer.setId(consumerId);
    }

    /**
     * Creates a request token entity into the data store.
     *
     * @param entitySubject the subject corresponding to the entity
     * @param requestToken the request token entity to create in the data store
     * @throws OAuthServiceException if an error condition occurs
     */
    public void createRequestToken(String entitySubject, RequestToken requestToken)
        throws OAuthServiceException {
        if (requestToken == null) {
            throw new OAuthServiceException("The request token is null");
        }
        Map<String, String> attributes = new HashMap<String, String>();
        String reqTokenURI = requestToken.getReqtUri();
        attributes.put(REQUEST_TOKEN_URI, reqTokenURI);
        String reqTokenVal = requestToken.getReqtVal();
        attributes.put(REQUEST_TOKEN_VAL, reqTokenVal);
        String reqTokenSecret = requestToken.getReqtSecret();
        attributes.put(REQUEST_TOKEN_SECRET, reqTokenSecret);
        String reqTokenPPalId = requestToken.getReqtPpalid();
        attributes.put(REQUEST_TOKEN_PPAL_ID, reqTokenPPalId);
        Date reqTokenExpiry = requestToken.getReqtLifetime();
        attributes.put(REQUEST_TOKEN_LIFETIME, DateUtils.toUTCDateFormat(reqTokenExpiry));
        String reqTokenCBK = requestToken.getCallback();
        attributes.put(REQUEST_TOKEN_CALLBACK, reqTokenCBK);
        String reqTokenVerifier = requestToken.getVerifier();
        if ((reqTokenVerifier != null) && reqTokenVerifier.trim().length() > 0) {
            attributes.put(REQUEST_TOKEN_VERIFIER, reqTokenVerifier);
        }
        String consumerId = requestToken.getConsumerId().getId();
        attributes.put(CONSUMER_ID, consumerId);
        String reqTokenId = em.createEntity(REQUEST_TOKEN_TYPE, entitySubject,
                                            attributes, reqTokenExpiry);
        requestToken.setId(reqTokenId);
    }

    /**
     * Creates an access token entity into the data store.
     *
     * @param entitySubject the subject corresponding to the entity
     * @param accessToken the access token entity to create in the data store
     * @throws OAuthServiceException if an error condition occurs
     */
    public void createAccessToken(String entitySubject, AccessToken accessToken)
        throws OAuthServiceException {
        if (accessToken == null) {
            throw new OAuthServiceException("The access token is null");
        }
        Map<String, String> attributes = new HashMap<String, String>();
        String accTokenURI = accessToken.getAcctUri();
        attributes.put(ACCESS_TOKEN_URI, accTokenURI);
        String accTokenVal = accessToken.getAcctVal();
        attributes.put(ACCESS_TOKEN_VAL, accTokenVal);
        String accTokenSecret = accessToken.getAcctSecret();
        attributes.put(ACCESS_TOKEN_SECRET, accTokenSecret);
        String accTokenPPalId = accessToken.getAcctPpalid();
        attributes.put(ACCESS_TOKEN_PPAL_ID, accTokenPPalId);
        Date accTokenExpiry = accessToken.getAcctLifetime();
        attributes.put(ACCESS_TOKEN_LIFETIME, DateUtils.toUTCDateFormat(accTokenExpiry));
        short accTokenOneTime = accessToken.getAcctOnetime();
        attributes.put(ACCESS_TOKEN_ONETIME, "" + accTokenOneTime);
        String consumerId = accessToken.getConsumerId().getId();
        attributes.put(CONSUMER_ID, consumerId);
        String accTokenId = em.createEntity(ACCESS_TOKEN_TYPE, entitySubject,
                                            attributes, accTokenExpiry);
        accessToken.setId(accTokenId);
    }

    /**
     * Reads a consumer entity from the data store.
     *
     * @param consumerId the identifier of the consumer
     *
     * @return the consumer entity to read
     * @throws OAuthServiceException if an error condition occurs
     */
    public Consumer readConsumer(String consumerId) throws OAuthServiceException {
        if (consumerId == null) {
            throw new OAuthServiceException("The consumer id is null");
        }
        Map<String, String> attributes = em.readEntity(consumerId);
        if ((attributes == null) || (attributes.isEmpty())) {
            throw new OAuthServiceException("Could not read the consumer entity");
        }
        Consumer consumer = new Consumer();
        String consumerName = attributes.get(CONSUMER_NAME);
        if ((consumerName == null) || (consumerName.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid consumer name");
        }
        consumer.setConsName(consumerName);
        String consumerSecret = attributes.get(CONSUMER_SECRET);
        if ((consumerSecret == null) || (consumerSecret.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid consumer secret");
        }
        consumer.setConsSecret(consumerSecret);
        String consumerRSAKey = attributes.get(CONSUMER_RSA_KEY);
        if ((consumerRSAKey != null) && (consumerRSAKey.trim().length() != 0)) {
            consumer.setConsRsakey(consumerRSAKey);
        }
        String consumerKey = attributes.get(CONSUMER_KEY);
        if ((consumerKey == null) || (consumerKey.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid consumer key");
        }
        consumer.setConsKey(consumerKey);
        String etag = attributes.get(ETAG);
        /*
        if ((etag == null) || (etag.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid etag");
        } */
        consumer.setEtag(etag);
        consumer.setId(consumerId);
        return consumer;
    }

    /**
     * Reads a request token entity from the data store.
     *
     * @param reqTokenId the identifier of the request token
     *
     * @return the request token entity to read
     * @throws OAuthServiceException if an error condition occurs
     */
    public RequestToken readRequestToken(String reqTokenId) throws OAuthServiceException {
        if (reqTokenId == null) {
            throw new OAuthServiceException("The request token id is null");
        }
        Map<String, String> attributes = em.readEntity(reqTokenId);
        if ((attributes == null) || (attributes.isEmpty())) {
            throw new OAuthServiceException("Could not read the request token entity");
        }
        RequestToken reqToken = new RequestToken();
        String reqTokenURI = attributes.get(REQUEST_TOKEN_URI);
        if ((reqTokenURI == null) || (reqTokenURI.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid request token URI");
        }
        reqToken.setReqtUri(reqTokenURI);
        String reqTokenVal = attributes.get(REQUEST_TOKEN_VAL);
        if ((reqTokenVal == null) || (reqTokenVal.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid request token value");
        }
        reqToken.setReqtVal(reqTokenVal);
        String reqTokenSecret = attributes.get(REQUEST_TOKEN_SECRET);
        if ((reqTokenSecret == null) || (reqTokenSecret.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid request token secret");
        }
        reqToken.setReqtSecret(reqTokenSecret);
        String reqTokenPPalId = attributes.get(REQUEST_TOKEN_PPAL_ID);
    
        reqToken.setReqtPpalid(reqTokenPPalId);
        String reqTokenExpiry = attributes.get(REQUEST_TOKEN_LIFETIME);
        if (reqTokenExpiry != null) {
            try {
                Date expiry = DateUtils.stringToDate(reqTokenExpiry);
                reqToken.setReqtLifetime(expiry);
            } catch (ParseException pe) {
                throw new OAuthServiceException("invalid request token expiry", pe);
            }
        }
        String reqTokenCBK = attributes.get(REQUEST_TOKEN_CALLBACK);
        if ((reqTokenCBK == null) || (reqTokenCBK.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid request token callback");
        }
        reqToken.setCallback(reqTokenCBK);
        String reqTokenVerifier = attributes.get(REQUEST_TOKEN_VERIFIER);
        reqToken.setVerifier(reqTokenVerifier);

        String consumerId = attributes.get(CONSUMER_ID);
        if ((consumerId == null) || (consumerId.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid request token consumer id");
        }
        Consumer consumer = readConsumer(consumerId);
        reqToken.setConsumerId(consumer);
        String etag = attributes.get(ETAG);
        /*
        if ((etag == null) || (etag.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid etag");
        } */
        reqToken.setEtag(etag);
        reqToken.setId(reqTokenId);
        return reqToken;
    }

    /**
     * Reads an access token entity from the data store.
     *
     * @param accTokenId the identifier of the access token
     *
     * @return the access token entity to read
     * @throws OAuthServiceException if an error condition occurs
     */
    public AccessToken readAccessToken(String accTokenId) throws OAuthServiceException {
        if (accTokenId == null) {
            throw new OAuthServiceException("The access token id is null");
        }
        Map<String, String> attributes = em.readEntity(accTokenId);
        if ((attributes == null) || (attributes.isEmpty())) {
            throw new OAuthServiceException("Could not read the access token entity");
        }
        AccessToken accToken = new AccessToken();
        String accTokenURI = attributes.get(ACCESS_TOKEN_URI);
        if ((accTokenURI == null) || (accTokenURI.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid access token URI");
        }
        accToken.setAcctUri(accTokenURI);
        String accTokenVal = attributes.get(ACCESS_TOKEN_VAL);
        if ((accTokenVal == null) || (accTokenVal.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid access token value");
        }
        accToken.setAcctVal(accTokenVal);
        String accTokenSecret = attributes.get(ACCESS_TOKEN_SECRET);
        if ((accTokenSecret == null) || (accTokenSecret.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid access token secret");
        }
        accToken.setAcctSecret(accTokenSecret);
        String accTokenPPalId = attributes.get(ACCESS_TOKEN_PPAL_ID);

        accToken.setAcctPpalid(accTokenPPalId);
        String accTokenExpiry = attributes.get(ACCESS_TOKEN_LIFETIME);
        if (accTokenExpiry != null) {
            try {
                Date expiry = DateUtils.stringToDate(accTokenExpiry);
                accToken.setAcctLifetime(expiry);
            } catch (ParseException pe) {
                throw new OAuthServiceException("invalid access token expiry", pe);
            }
        }
        String consumerId = attributes.get(CONSUMER_ID);
        if ((consumerId == null) || (consumerId.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid access token consumer id");
        }
        Consumer consumer = readConsumer(consumerId);
        accToken.setConsumerId(consumer);
        String etag = attributes.get(ETAG);
        /*
        if ((etag == null) || (etag.trim().length()== 0)) {
            throw new OAuthServiceException("Invalid etag");
        } */
        accToken.setEtag(etag);
        accToken.setId(accTokenId);
        return accToken;
    }

    /**
     * Updates a consumer entity in the data store.
     *
     * @param consumer the consumer entity to update in the data store
     * @throws OAuthServiceException if an error condition occurs
     */
    public void updateConsumer(Consumer consumer) throws OAuthServiceException {
        if (consumer == null) {
            throw new OAuthServiceException("The consumer is null");
        }
        Map<String, String> attributes = new HashMap<String, String>();
        String consumerName = consumer.getConsName();
        attributes.put(CONSUMER_NAME, consumerName);
        String consumerSecret = consumer.getConsSecret();
        attributes.put(CONSUMER_SECRET, consumerSecret);
        String consumerRASKey = consumer.getConsRsakey();
        attributes.put(CONSUMER_RSA_KEY, consumerRASKey);
        String consumerKey = consumer.getConsKey();
        attributes.put(CONSUMER_KEY, consumerKey);
        String etag = consumer.getEtag();
        attributes.put(ETAG, etag);
        String consumerId = consumer.getId();
        em.updateEntity(consumerId, attributes, null);
    }

/**
     * Updates a request token entity in the data store.
     *
     * @param requestToken the request token entity to update in the data store
     * @throws OAuthServiceException if an error condition occurs
     */
    public void updateRequestToken(RequestToken requestToken) throws OAuthServiceException {
        if (requestToken == null) {
            throw new OAuthServiceException("The request token is null");
        }
        Map<String, String> attributes = new HashMap<String, String>();
        String reqTokenURI = requestToken.getReqtUri();
        attributes.put(REQUEST_TOKEN_URI, reqTokenURI);
        String reqTokenVal = requestToken.getReqtVal();
        attributes.put(REQUEST_TOKEN_VAL, reqTokenVal);
        String reqTokenSecret = requestToken.getReqtSecret();
        attributes.put(REQUEST_TOKEN_SECRET, reqTokenSecret);
        String reqTokenPPalId = requestToken.getReqtPpalid();
        attributes.put(REQUEST_TOKEN_PPAL_ID, reqTokenPPalId);
        Date reqTokenExpiry = requestToken.getReqtLifetime();
        attributes.put(REQUEST_TOKEN_LIFETIME, DateUtils.toUTCDateFormat(reqTokenExpiry));
        String reqTokenCBK = requestToken.getCallback();
        attributes.put(REQUEST_TOKEN_CALLBACK, reqTokenCBK);
        String reqTokenVerifier = requestToken.getVerifier();
        if ((reqTokenVerifier != null) && reqTokenVerifier.trim().length() > 0) {
            attributes.put(REQUEST_TOKEN_VERIFIER, reqTokenVerifier);
        }
        String consumerId = requestToken.getConsumerId().getId();
        attributes.put(CONSUMER_ID, consumerId);
        String etag = requestToken.getEtag();
        attributes.put(ETAG, etag);
        String reqTokenId = requestToken.getId();
        em.updateEntity(reqTokenId, attributes, reqTokenExpiry);
    }

    /**
     * Updates an access token entity in the data store.
     *
     * @param accessToken the access token entity to update in the data store
     * @throws OAuthServiceException if an error condition occurs
     */
    public void updateAccessToken(AccessToken accessToken) throws OAuthServiceException {
        if (accessToken == null) {
            throw new OAuthServiceException("The access token is null");
        }
        Map<String, String> attributes = new HashMap<String, String>();
        String accTokenURI = accessToken.getAcctUri();
        attributes.put(ACCESS_TOKEN_URI, accTokenURI);
        String accTokenVal = accessToken.getAcctVal();
        attributes.put(ACCESS_TOKEN_VAL, accTokenVal);
        String accTokenSecret = accessToken.getAcctSecret();
        attributes.put(ACCESS_TOKEN_SECRET, accTokenSecret);
        String accTokenPPalId = accessToken.getAcctPpalid();
        attributes.put(ACCESS_TOKEN_PPAL_ID, accTokenPPalId);
        Date accTokenExpiry = accessToken.getAcctLifetime();
        attributes.put(ACCESS_TOKEN_LIFETIME, DateUtils.toUTCDateFormat(accTokenExpiry));
        short accTokenOneTime = accessToken.getAcctOnetime();
        attributes.put(ACCESS_TOKEN_ONETIME, "" + accTokenOneTime);
        String consumerId = accessToken.getConsumerId().getId();
        attributes.put(CONSUMER_ID, consumerId);
        String etag = accessToken.getEtag();
        attributes.put(ETAG, etag);
        String accTokenId = accessToken.getId();
        em.updateEntity(accTokenId, attributes, accTokenExpiry);
    }

    /**
     * Searches for consumer entities from the data store.
     *
     * @param attributes the attribute-value pairs used for the search
     *
     * @return a list of consumers that satisfy the search criteria
     * @throws OAuthServiceException if an error condition occurs
     */
    public List<Consumer> searchConsumers(Map<String, String> attributes)
        throws OAuthServiceException {
        if ((attributes == null) || (attributes.isEmpty())) {
            return null;
        }
        List<String> ids = em.searchEntity(CONSUMER_TYPE, attributes);
        if ((ids == null) || (ids.isEmpty())) {
            return null;
        }
        List<Consumer> consumers = new ArrayList<Consumer>();
        Iterator<String> iter = ids.iterator();
        while (iter.hasNext()) {
            String id = iter.next();
            Consumer consumer = readConsumer(id);
            if (consumer != null) {
                consumers.add(consumer);
            }
        }
        return consumers;
    }

    /**
     * Searches for request token entities from the data store.
     *
     * @param attributes the attribute-value pairs used for the search
     *
     * @return a list of request tokens that satisfy the search criteria
     * @throws OAuthServiceException if an error condition occurs
     */
    public List<RequestToken> searchRequestTokens(Map<String, String> attributes)
        throws OAuthServiceException {
        if ((attributes == null) || (attributes.isEmpty())) {
            return null;
        }
        List<String> ids = em.searchEntity(REQUEST_TOKEN_TYPE, attributes);
        if ((ids == null) || (ids.isEmpty())) {
            return null;
        }
        List<RequestToken> requestTokens = new ArrayList<RequestToken>();
        Iterator<String> iter = ids.iterator();
        while (iter.hasNext()) {
            String id = iter.next();
            RequestToken requestToken = readRequestToken(id);
            if (requestToken != null) {
                requestTokens.add(requestToken);
            }
        }
        return requestTokens;
    }

    /**
     * Searches for access token entities from the data store.
     *
     * @param attributes the attribute-value pairs used for the search
     *
     * @return a list of access tokens that satisfy the search criteria
     * @throws OAuthServiceException if an error condition occurs
     */
    public List<AccessToken> searchAccessTokens(Map<String, String> attributes)
        throws OAuthServiceException {
        if ((attributes == null) || (attributes.isEmpty())) {
            return null;
        }
        List<String> ids = em.searchEntity(ACCESS_TOKEN_TYPE, attributes);
        if ((ids == null) || (ids.isEmpty())) {
            return null;
        }
        List<AccessToken> accessTokens = new ArrayList<AccessToken>();
        Iterator<String> iter = ids.iterator();
        while (iter.hasNext()) {
            String id = iter.next();
            AccessToken accessToken = readAccessToken(id);
            if (accessToken != null) {
                accessTokens.add(accessToken);
            }
        }
        return accessTokens;
    }

    /**
     * Deletes a consumer entity from the data store.
     *
     * @param consumer the consumer entity to delete
     * @throws OAuthServiceException if an error condition occurs
     */
    public void deleteConsumer(Consumer consumer) throws OAuthServiceException {
        if (consumer == null) {
            throw new OAuthServiceException("The consumer is null");
        }
        em.deleteEntity(consumer.getId());
    }

    /**
     * Deletes a request token entity from the data store.
     *
     * @param requestToken the request token entity to delete
     * @throws OAuthServiceException if an error condition occurs
     */
    public void deleteRequestToken(RequestToken requestToken) throws OAuthServiceException {
        if (requestToken == null) {
            throw new OAuthServiceException("The request token is null");
        }
        em.deleteEntity(requestToken.getId());
    }

    /**
     * Deletes an access token entity from the data store.
     *
     * @param accessToken the access token entity to delete
     * @throws OAuthServiceException if an error condition occurs
     */
    public void deleteAccessToken(AccessToken accessToken) throws OAuthServiceException {
        if (accessToken == null) {
            throw new OAuthServiceException("The access token is null");
        }
        em.deleteEntity(accessToken.getId());
    }
}
