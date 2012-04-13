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
 * $Id: OAuthServiceUtils.java,v 1.1 2009/11/20 19:31:58 huacui Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock AS
 */
package com.sun.identity.oauth.service.util;

import com.sun.identity.oauth.service.OAuthServiceConstants;
import com.sun.identity.oauth.service.OAuthServiceException;
import com.sun.identity.oauth.service.PathDefs;
import com.sun.identity.security.AdminTokenId;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.MultivaluedMap;

/**
 *
 * @author huacui
 */
public class OAuthServiceUtils implements OAuthServiceConstants {
    private static final String AUTHN_USERNAME = "username";
    private static final String AUTHN_PASSWORD = "password";
    private static final String SUBJECT_ID = "subjectid";
    private static final String TOKEN_ID = "tokenid";
    private static final String UUID_SESSION_PROPERTY_NAME = "sun.am.UniversalIdentifier";
    private static final String ATTRIBUTE_TOKEN_ID_KEY = "token.id";
    private static final String USERDETAILS_NAME_KEY = "userdetails.attribute.name";
    private static final String USERDETAILS_VALUE_KEY = "userdetails.attribute.value";

    private static Client client = Client.create();

    // OpenSSO RESTful authentication service endpoint
    private static WebResource authenticateResource =
     client.resource(OAuthProperties.get(PathDefs.OPENSSO_SERVER_URL) +
                     PathDefs.OPENSSO_SERVER_AUTHENTICATION_ENDPOINT);

     // OpenSSO RESTful attributes service endpoint
    private static WebResource attributesResource =
     client.resource(OAuthProperties.get(PathDefs.OPENSSO_SERVER_URL) +
                     PathDefs.OPENSSO_SERVER_ATTRIBUTES_ENDPOINT);

    // OpenSSO RESTful token validation service endpoint
    private static WebResource tokenValidationResource =
     client.resource(OAuthProperties.get(PathDefs.OPENSSO_SERVER_URL) +
                     PathDefs.OPENSSO_SERVER_TOKEN_VALIDATION_ENDPOINT);


    public static String authenticate(String username, String password, boolean appAuth)
        throws OAuthServiceException {
        MultivaluedMap params = new MultivaluedMapImpl();
        params.add(AUTHN_USERNAME, username);
        params.add(AUTHN_PASSWORD, password);
        if (appAuth) {
            params.add("uri", "module=application");
        }

        String response;
        try {
            response = authenticateResource.queryParams(params).get(String.class);
        }
        catch (UniformInterfaceException uie) {
            throw new OAuthServiceException("Authentication failed", uie);
        }

        // ensure response is in expected format
        if (!response.startsWith(ATTRIBUTE_TOKEN_ID_KEY + "=")) {
            return null;
        }

        String tokenId = response.substring(9);
        tokenId = tokenId.substring(0, tokenId.length() - 1);
        return tokenId;
    }

    // This method is only called in server mode
    public static String getAdminTokenId() throws OAuthServiceException {
        String adminTokenIdString = null;
        try {
            AdminTokenId adminTokenId = (AdminTokenId)Class.forName(
                       "com.sun.identity.security.AdminTokenIdImpl")
                       .newInstance();
            adminTokenIdString = adminTokenId.getAdminTokenId();
        } catch (Exception e) {
            throw new OAuthServiceException("Getting Admin token failed", e);
        }
        return adminTokenIdString;
    }

    public static String getUUIDByTokenId(String tokenId) throws OAuthServiceException {
        String uuid;

        MultivaluedMapImpl params = new MultivaluedMapImpl();
        params.add(SUBJECT_ID, tokenId);
        params.add("attributenames", UUID_SESSION_PROPERTY_NAME);

        String response;
        try {
            response = attributesResource.queryParams(params).get(String.class);
        }
        catch (UniformInterfaceException uie) {
            throw new OAuthServiceException("Get uuid failed", uie);
        }

        if (response == null) {
            return null;
        }

        int index = response.indexOf(USERDETAILS_NAME_KEY + "=" + UUID_SESSION_PROPERTY_NAME);
        index = response.indexOf(USERDETAILS_VALUE_KEY + "=", index);
        int startIdx = index + USERDETAILS_VALUE_KEY.length() + 1;
        int idx = response.indexOf(USERDETAILS_NAME_KEY + "=", startIdx);
        int endIdx;
        if (idx > 0) {
            endIdx = idx;
        } else {
            endIdx = response.length() - 1;
        }
        uuid = response.substring(startIdx, endIdx).trim();
        return uuid;
    }

    public static boolean isTokenValid(String tokenId)
        throws OAuthServiceException {
        boolean result = false;
        MultivaluedMap params = new MultivaluedMapImpl();
        params.add(TOKEN_ID, tokenId);

        String response;
        try {
            response = tokenValidationResource.queryParams(params).get(String.class);
        }
        catch (UniformInterfaceException uie) {
            throw new OAuthServiceException("Validate token failed", uie);
        }

        // ensure response is in expected format
        if (response.startsWith("boolean=true")) {
            result = true;
        }
        return result;
    }
}
