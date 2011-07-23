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
 * $Id: RequestSecurityToken.java,v 1.2 2009/10/13 23:19:49 mallas Exp $
 *
 */

package com.sun.identity.wss.trust;

import com.sun.identity.wss.trust.WSTException;
import org.w3c.dom.Element;
import java.util.List;

/**
 * This abstract class <code>RequestSecurityToken</code> represents the
 * WS-Trust protocol element RequestSecurityToken.
 */

public abstract class RequestSecurityToken {
    
    protected static final String REQUEST_SECURITY_TOKEN = 
                                  "RequestSecurityToken";
    protected static final String REQUEST_TYPE = "RequestType";
    protected static final String TOKEN_TYPE = "TokenType";
    protected static final String APPLIES_TO = "AppliesTo";
    protected static final String EP_REFERENCE = "EndpointReference";
    protected static final String ADDRESS = "Address";
    protected static final String ON_BEHALF_OF = "OnBehalfOf";
    protected static final String KEY_TYPE = "KeyType";
    protected static final String CLAIMS = "Claims";
            
    protected String requestType = null;
    protected String appliesTo = null;
    protected Element oboToken = null;
    protected String keyType = null;
    protected String tokenType = null;
    protected List<ClaimType> claimTypes = null;
    
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
     * Returns the appliesTo attribute.
     * @return the appliesTo attribute
     */
    public String getAppliesTo() {
        return appliesTo;
    }
    
    /**
     * Sets the appliesTo attribute.
     * @param appliesTo the appliesTo attribute.
     */
    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }
    
    /**
     * Returns the OnBehalfOf <code>Element</code>
     * @return the OnBehalfOf <code>Element</code>
     */
    public Element getOnBehalfOf() {
        return oboToken;
    }
    
    /**
     * Sets the Ob behalf of token.
     * @param oboToken the on behalf of token.
     */
    public void setOnBehalfOf(Element oboToken) {
        this.oboToken = oboToken;
    }
    
    /**
     * Returns the keytype
     * @return the keytype.
     */
    public String getKeyType() {
        return keyType;
    }
    
    /**
     * Sets the keytype
     * @param keyType the keytype.
     */
    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }
    
    /**
     * Returns the list of claim types.
     * @return the list of claim types.
     */
    public List getClaimTypes() {
        return claimTypes;
    }
    
    /**
     * Sets the list of claim types
     * @param claimTypes the list of claim types.
     */
    public void setClaimTypes(List claimTypes) {
        this.claimTypes = claimTypes;
    }
    
    //TODO - more to be added as per the protocol but this will suffice
    // our needs for now. 
    /**
     * Returns the DOM Element representation for the request security token.
     * @return the DOM Element representation for the request security token.
     * @throws com.sun.identity.wss.sts.protocol.WSTException
     */
    public abstract Element toDOMElement()  throws WSTException;
    
    /**
     * Converts into XML String
     * @return the XML String for <code>RequestSecurityToken</code>.
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public abstract String toXMLString() throws WSTException;
        
    
}
