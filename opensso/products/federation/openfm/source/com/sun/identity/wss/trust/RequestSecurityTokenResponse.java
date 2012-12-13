/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RequestSecurityTokenResponse.java,v 1.2 2009/08/29 03:05:59 mallas Exp $
 *
 */

package com.sun.identity.wss.trust;

import com.sun.identity.wss.trust.WSTException;
import org.w3c.dom.Element;

/**
 * This abstract class <code>RequestSecurityTokenResponse</code> represents the
 * WS-Trust protocol element RequestSecurityTokenResponse. 
 */
public abstract class RequestSecurityTokenResponse {
    
    
    protected static final String REQUEST_SECURITY_TOKEN_RESPONSE = 
                  "RequestSecurityTokenResponse";
    protected static final String REQUESTED_SECURITY_TOKEN =
                  "RequestedSecurityToken";
    protected static final String REQUESTED_PROOF_TOKEN = "RequestedProofToken";
    protected static final String APPLIES_TO = "AppliesTo";
    protected static final String TOKEN_TYPE = "TokenType";
    protected static final String REQUEST_TYPE = "RequestType";
    protected static final String EP_REFERENCE = "EndpointReference";
    protected static final String ADDRESS = "Address";
    
    protected String tokenType = null;
    protected Element requestedSecurityToken = null;
    protected String appliesTo = null;
    protected String requestType = null;
    protected RequestedProofToken requestedProofToken = null;
    
    /**
     * Returns the token type.
     * @return the token type.
     */
    public String getTokenType() {
       return tokenType;    
    }
    
    /**
     * Sets the token type.
     * @param tokenType the token type.
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    /**
     * Returns the request type                     
     * @return the request type.
     */
    public String getRequestType() {
        return requestType;
    }
    
    /**
     * Sets the request type.
     * @param requestType the request type
     */
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }
    
    /**
     * Returns the requested security token as a DOM Element.
     * @return the requested security token as a DOM Element
     */
    public Element getRequestedSecurityToken() {
        return requestedSecurityToken;
    }
        
    /**
     * Sets the requested security token.
     * @param secToken the requested security token elemtn
     */
    public void setRequestedSecurityToken(Element secToken) {
        this.requestedSecurityToken = secToken;
    }
    
    /**
     * Returns the AppliesTo attribute.
     * @return the AppliesTo attribute.
     */
    public String getAppliesTo() {
        return appliesTo;
    }
    
    /**
     * Sets the appliesTo attribute element
     * @param appliesTo the appliesTo attribute element.
     */
    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }
    
    /**
     * Returns the requested proof token.
     * @return the requested proof token.
     */
    public RequestedProofToken getRequestedProofToken() {
        return requestedProofToken;
    }
    
    /**
     * Sets the requested proof token.
     * @param requestedProofToken the requested proof token.
     */
    public void setRequestedProofToken(RequestedProofToken requestedProofToken){
        this.requestedProofToken = requestedProofToken;
    }
    
    //TODO - Add more element protocols.
    
    /**
     * Converts into DOM Element.
     * @return the DOMElement representation of
     *                      <code>RequestSecurityTokenResponse</code>
     */
    public abstract Element toDOMElement() throws WSTException;
    
    /**
     * Converts into XML String
     * @return the XML String for <code>RequestSecurityTokenResponse</code>.
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public abstract String toXMLString() throws WSTException;
        

}
