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
 * $Id: WSFederationClient.java,v 1.1 2009/12/14 23:42:47 mallas Exp $
 *
 */
package com.sun.identity.wsfederation.client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;

import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * This class <code>WSFederationClient</code> is used to retrieve the SAML
 * Assertion remotely for a given user session.
 */

public class WSFederationClient {

    private static final Client client = Client.create();

    /**
     * Returns the user SAML Assertion for a given session established by
     * WS-Federation protocol.
     * @param tokenID the URL encoded session string.
     * @param entityID the entityID of the federation provider
     * @param entityRole the entity role of the federation profile.
     *        Possible values are "RP" or "IP".
     * @return the user's SAML assertion xml string.
     * @throws com.sun.identity.wsfederation.common.WSFederationException
     */
    public static String getUserSAMLAssertion(String tokenID,
            String entityID, String entityRole) throws WSFederationException {

        if(tokenID == null) {
           throw new WSFederationException("tokenIDisNull");
        }

        String url = SystemPropertiesManager.get(SAMLConstants.SERVER_PROTOCOL)+
                "://" + SystemPropertiesManager.get(SAMLConstants.SERVER_HOST) +
                ":" + SystemPropertiesManager.get(SAMLConstants.SERVER_PORT) +
                  SystemPropertiesManager.get(SAMLConstants.SERVER_URI) +
                 "/federationws/wsfederationservice";
        System.out.println("URL: " + url);
        return getUserSAMLAssertion(tokenID, url, entityID, entityRole);
    }

    /**
     * Returns the user SAML Assertion for a given session established by
     * WS-Federation protocol.
     * @param tokenID the URL encoded session string.
     * @param url the endpoint where the assertion could be obtained
     * @param entityID the entityID of the federation provider
     * @param entityRole the entity role of the federation profile.
     *        Possible values are "RP" or "IP".
     * @return the user's SAML assertion xml string.
     * @throws com.sun.identity.wsfederation.common.WSFederationException
     */
    public static String getUserSAMLAssertion(String tokenID, String url,
            String entityID, String entityRole) throws WSFederationException {

        if(tokenID == null) {
           throw new WSFederationException("tokenIDisNull");
        }


        String endpoint = url + "?token=" + tokenID;

        if(entityID != null) {
           url = url + "&entityID=" + entityID;
        }

        if(entityRole != null) {
           url = url + "&entityRole=" + entityRole;
        }
        WebResource resource = client.resource(endpoint);
        ClientResponse clr = resource.get(ClientResponse.class);
        return clr.getEntity(String.class);

    }


    
} 
