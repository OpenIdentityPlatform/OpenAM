/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2011-2015 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.ProtocolFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Map;

/**
 * This class represents a copy of a AuthnRequestInfo in the service provider and
 * is used when in SAML2 failover mode to track AuthnRequest's between multiple instances
 * of OpenAM.
 * The key difference between AuthnRequestInfo and AuthnRequestInfoCopy is 
 * AuthnRequestInfoCopy only keeps those objects that can be serialized.
 * 
 * @author Mark de Reeper mark.dereeper@forgerock.com
 */
public class AuthnRequestInfoCopy implements Serializable {
    private Map paramsMap;
    private String realm;
    private String authnRequest;
    private String relayState;
    private String spEntityID;
    private String idpEntityID;

    /**
     * Default constructor for deserialization.
     */
    public AuthnRequestInfoCopy() {
    }

    public AuthnRequestInfoCopy(AuthnRequestInfo info) throws SAML2Exception {
        
        this.realm = info.getRealm();
        // Next to take the XML representation of the AuthnRequest as it may not be serializable.
        this.authnRequest = info.getAuthnRequest().toXMLString(true, true);
        this.paramsMap = info.getParamsMap();
        this.relayState = info.getRelayState();
        this.idpEntityID = info.getIDPEntityID();
        this.spEntityID = info.getSPEntityID();
    }

    public AuthnRequestInfo getAuthnRequestInfo(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws SAML2Exception {
                
        return new AuthnRequestInfo(httpRequest, httpResponse, realm, spEntityID, idpEntityID, 
                ProtocolFactory.getInstance().createAuthnRequest(authnRequest), relayState, paramsMap);
    }    
}
