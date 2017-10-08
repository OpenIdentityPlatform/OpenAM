/*
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
 * $Id: AuthnRequestInfo.java,v 1.2 2008/06/25 05:47:53 qcheng Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.saml2.protocol.AuthnRequest;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class stores information about the request made to
 * the Service Provider.
 */
public class AuthnRequestInfo extends CacheObject {
    Map paramsMap;
    String realm;
    AuthnRequest authnRequest;
    String relayState;
    String spEntityID;
    String idpEntityID;
    HttpServletRequest request;
    HttpServletResponse response;
    

    /**
     * Constructor creates the AuthnRequest Info for a request.
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @param realm to which the Service Provider belongs.
     * @param spEntityID the entityID of Service Provider.
     * @param authnReq the Authentication Request Object
     * @param relayState the Redirection URL on completion of Request.
     * @param paramsMap Map of other parameters sent by the requester.
     */

    public AuthnRequestInfo(HttpServletRequest request,
                            HttpServletResponse response,
                            String realm,
                            String spEntityID,
                            String idpEntityID,
                            AuthnRequest authnReq,
                            String relayState,
                            Map paramsMap) {

        this.realm = realm;
        authnRequest = authnReq;
        this.relayState = relayState;
        this.paramsMap = paramsMap;
        this.request = request;
        this.response = response;
        this.spEntityID = spEntityID;
        this.idpEntityID = idpEntityID;
        time = currentTimeMillis();
    }

    /**
     * Returns the realm.
     *
     * @return realm to which SP belongs.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Returns the Service Provider Entity ID.
     *
     * @return Service Provider Identifier.
     */
    protected String getSPEntityID() {
        return spEntityID;
    }

    /**
     * Returns the Identity Provider Entity ID.
     *
     * @return the Identity Provider Identifier.
     */
    protected String getIDPEntityID() {
        return idpEntityID;
    }

    /**
     * Returns the <code>AuthnRequest</code> Object.
     *
     * @return the <code>AuthnRequest</code> Object.
     */
    public AuthnRequest getAuthnRequest() {
        return authnRequest;
    }

    /**
     * Returns the Map of parameters in the Request.
     *
     * @return Map of request parameters.
     */
    protected Map getParamsMap() {
        return paramsMap;
    }

    /**
     * Returns the <code>RelayState</code> parameter value.
     *
     * @return the RelayState parameter value.
     */
    protected String getRelayState() {
        return relayState;
    }

    /**
     * Returns the <code>HttpServletRequest</code> object
     *
     * @return the <code>HttpServletRequest</code> object.
     */
    protected HttpServletRequest getServletRequest() {
        return request;
    }

    /**
     * Returns the <code>HttpServletResponse</code> parameter value.
     *
     * @return the <code>HttpServletResponse</code> object.
     */
    protected HttpServletResponse getServletResponse() {
        return response;
    }
}
