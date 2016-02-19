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
 * $Id: ManageNameIDRequestInfo.java,v 1.4 2009/11/20 21:41:16 exu Exp $
 *
 */


package com.sun.identity.saml2.profile;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;

/**
 * This class stores information about the request made to
 * the Service Provider.
 */
public class ManageNameIDRequestInfo extends CacheObject {
    Map paramsMap;
    ManageNameIDRequest mniRequest;
    String relayState;
    HttpServletRequest request;
    HttpServletResponse response;
    static Debug debug = SAML2Utils.debug;
    Object oldSession = null;
    NameID nameID = null;

    /**
     * Constructor creates the ManageNameIDRequest Info for a request.
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @param mniRequest the Authentication Request Object
     * @param relayState the Redirection URL on completion of Request.
     * @param paramsMap Map of other parameters sent by the requester.
     * @param session session object.
     */
    
    protected ManageNameIDRequestInfo(HttpServletRequest request,
        HttpServletResponse response, ManageNameIDRequest mniRequest,
        String relayState, Map paramsMap, Object session) {

        this.mniRequest = mniRequest;
        this.relayState = relayState;
        this.paramsMap = paramsMap;
        this.request = request;
        this.response = response;
        time = System.currentTimeMillis();
        oldSession = session;
    }

    /**
     * Returns the <code>ManageNameIDRequest</code> Object.
     *
     * @return the <code>ManageNameIDRequest</code> Object.
     */
    protected ManageNameIDRequest getManageNameIDRequest() {
        return mniRequest;
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
     * Returns the <code>HttpServletRequest</code> object.
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

    /**
     * Returns the session parameter value.
     *
     * @return the session object.
     */
    protected Object getSession() {
        String method = "ManageNameIDRequestInfo:getSession ";
        Object session = null;
        try {
            session = 
                SessionManager.getProvider().getSession(request);
        } catch (SessionException se) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(method, se);
            }
            session = null;
        }
        if (session == null) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(method +
                    "Failed to get session from request.");
            }
            session = paramsMap.get(SAML2Constants.SESSION);
        }

	if (session == null) {
	    session = oldSession;
	}

        return session;
    }

    public void setNameID(NameID nameID) {
	this.nameID = nameID;
    }

    public NameID getNameID() {
	return nameID;
    }


}
