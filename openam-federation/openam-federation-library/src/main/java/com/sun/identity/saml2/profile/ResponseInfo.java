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
 * $Id: ResponseInfo.java,v 1.6 2009/06/17 03:09:13 exu Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import java.util.Map;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.protocol.Response;

/**
 * This class stores information about the response made to
 * the Service Provider.
 */
public class ResponseInfo extends CacheObject {
    private Response resp = null;
    private String relayState = null;
    private String profileBinding = null; 
    private Assertion assertion = null;
    private Map attrMap = null;
    private NameID nameId = null;
    private String sessionIndex = null;
    private boolean isLocalLogin = false;

    /**
     * Constructor creates the ResponseInfo.
     * @param response the Response
     * @param binding Profile binding used, one of the following values:
     *     <code>SAML2Constants.HTTP_POST</code>,
     *     <code>SAML2Constants.HTTP_ARTIFACT</code>,
     *     <code>SAML2Constants.PAOS</code>
     * @param relayState relayState retrieved from ECP RelayState.
     */
    public ResponseInfo(Response response, String binding,
        String relayState) {
        this.resp = response;
        this.profileBinding = binding;
        this.relayState = relayState;
        time = System.currentTimeMillis();
    }

    /**
     * Returns the <code>Response</code> object.
     *
     * @return the <code>Response</code> object.
     */
    public Response getResponse() {
        return resp;
    }

    /**
     * Returns the relayState.
     *
     * @return the relayState.
     */
    public String getRelayState() {
        return relayState;
    }

    /**
     * Returns the profile binding,  one of the following values:
     *     <code>null</code>,
     *     <code>SAML2Constants.HTTP_POST</code>,
     *     <code>SAML2Constants.HTTP_ARTIFACT</code>,
     *     <code>SAML2Constants.PAOS</code>
     *
     * @return the binding.
     */
    public String getProfileBinding() {
        return profileBinding;
    }

    /**
     * Sets the authn assertion
     *
     * @param assertion the authn assertion in the response
     */
    public void setAssertion(Assertion assertion) {
        this.assertion = assertion;
    }

    /**
     * Returns the authn assertion
     *
     * @return the authn assertion
     */
    public Assertion getAssertion() {
        return assertion;
    }

    /**
     * Sets the map of the attributes
     *
     * @param attrs the attribute map
     */
    public void setAttributeMap(Map attrs) {
        attrMap = attrs;
    }

    /**
     * Returns the map of the atrributes
     *
     * @return the map of the atrributes
     */
    public Map getAttributeMap() {
        return attrMap;
    }

    /**
     * Sets the NameId
     *
     * @param id the NameId in the assertion
     */
    public void setNameId(NameID id) {
        nameId = id;
    }

    /**
     * Returns the NameID
     *
     * @return the NameID
     */
    public NameID getNameId() {
        return nameId;
    }

    /**
     * Sets SessionIndex.
     *
     * @param index SessionIndex of the session
     */
    public void setSessionIndex(String index) {
        sessionIndex = index;
    }

    /**
     * Returns the SessionIndex.
     *
     * @return the SessionIndex
     */
    public String getSessionIndex() {
        return sessionIndex;
    }

    /**
     * Tells whether the user has been redirected to perform local login.
     *
     * @return <code>true</code> if the user was sent to perform local login.
     */
    public boolean isLocalLogin() {
        return isLocalLogin;
    }

    /**
     * Sets the isLocalLogin flag.
     *
     * @param isLocalLogin The isLocalLogin flag.
     */
    public void setIsLocalLogin(boolean isLocalLogin) {
        this.isLocalLogin = isLocalLogin;
    }
}
