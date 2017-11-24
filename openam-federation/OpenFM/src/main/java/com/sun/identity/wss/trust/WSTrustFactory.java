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
 * $Id: WSTrustFactory.java,v 1.1 2008/09/19 16:00:56 mallas Exp $
 *
 */

package com.sun.identity.wss.trust;

import org.w3c.dom.Element;
import com.sun.identity.wss.sts.STSConstants;

/**
 * The class <code>WSTrustFactory</code> is a factory class that generates
 * ws-trust protocol objects.
 * TODO - We need to make this abstract to make it truely pluggable.
 */
public class WSTrustFactory {
    
    private String protocolVersion = null;
    
    private WSTrustFactory(String version) throws WSTException {
        
        if(!(STSConstants.WST_VERSION_13.equals(version)) &&
                !(STSConstants.WST_VERSION_10.equals(version))) {
            throw new WSTException("Unsupported version");
        }
         this.protocolVersion = version;
    }
    
    /**
     * Returns an instance of <code>WSTrustFactory</code> for the default
     * WS-Trust protocol 1.0
     * @return the instance of <code>WSTrustFactory</code> for the default
     * WS-Trust protocol 1.0
     * @throws WSTException if it is unsupported version.
     */
    public static WSTrustFactory newInstance() throws WSTException {
        return new WSTrustFactory(STSConstants.WST_VERSION_10);
    }
    
    /**
     * Returns an instance of <code>WSTrustFactory</code> for the given
     * protocol version.
     * @param protocolVersion the protocol version.
     * @return an instance of <code>WSTrustFactory</code> for the given
     * protocol version.
     */
    public static WSTrustFactory newInstance(String protocolVersion)
            throws WSTException {
        return new WSTrustFactory(protocolVersion);
    }
    
    /**
     * Returns an instance of <code>RequestSecurityToken</code>
     * @return the instance of <code>RequestSecurityToken</code>
     */
    public RequestSecurityToken createRequestSecurityToken() {
        if(STSConstants.WST_VERSION_13.equals(protocolVersion)) {
           return new com.sun.identity.wss.trust.wst13.
                   RequestSecurityToken_Impl(); 
        } else {
           return new com.sun.identity.wss.trust.wst10.
                   RequestSecurityToken_Impl();
        }
    }
    /**
     * Returns an instance of <code>RequestSecurityToken</code> for 
     *        given DOM Element.
     * @param element the DOM Element for <code>RequestSecurityToken</code>
     * @return the instance of <code>RequestSecurityToken</code>
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public RequestSecurityToken createRequestSecurityToken(Element element) 
                    throws WSTException {
        if(STSConstants.WST_VERSION_13.equals(protocolVersion)) {
           return new com.sun.identity.wss.trust.wst13.
                   RequestSecurityToken_Impl(element); 
        } else {
           return new com.sun.identity.wss.trust.wst10.
                   RequestSecurityToken_Impl(element);
        }
    }
    
    /**
     * Returns an instance of <code>RequestSecurityTokenResponse</code>
     * @return the instance of <code>RequestSecurityTokenResponse</code>
     */
    public RequestSecurityTokenResponse createRequestSecurityTokenResponse() {
        if(STSConstants.WST_VERSION_13.equals(protocolVersion)) {
           return new com.sun.identity.wss.trust.wst13.
                   RequestSecurityTokenResponse_Impl(); 
        } else {
           return new com.sun.identity.wss.trust.wst10.
                   RequestSecurityTokenResponse_Impl(); 
        }
    }
    
    /**
     * Returns an instance of <code>RequestSecurityTokenResponse</code> for
     *    a given DOM element
     * @param element the DOM element representation of
     *          <code>RequestSecurityTokenResponse</code>
     * @return an instance of <code>RequestSecurityTokenResponse</code>
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public RequestSecurityTokenResponse createRequestSecurityTokenResponse
            (Element element) throws WSTException {
        if(STSConstants.WST_VERSION_13.equals(protocolVersion)) {
           return new com.sun.identity.wss.trust.wst13.
                   RequestSecurityTokenResponse_Impl(element); 
        } else {
           return new com.sun.identity.wss.trust.wst10.
                   RequestSecurityTokenResponse_Impl(element); 
        }
    }
    
    /**
     * Returns an instance of <code>RequestSecurityTokenResponseCollection</code>
     * @return an instance of <code>RequestSecurityTokenResponseCollection</code>
     */
    public RequestSecurityTokenResponseCollection 
            createRequestSecurityTokenResponseCollection() {
        if(STSConstants.WST_VERSION_13.equals(protocolVersion)) {
           return new com.sun.identity.wss.trust.wst13.
                   RequestSecurityTokenResponseCollection_Impl(); 
        } else {
           return new com.sun.identity.wss.trust.wst10.
                   RequestSecurityTokenResponseCollection_Impl(); 
        }
    }
    
    /**
     * Returns an instance of <code>RequestSecurityTokenResponseCollection</code>
     * for a given DOM Element.
     * @param element the DOM Element representation of
     *        <code>RequestSecurityTokenResponseCollection</code>
     * @return the <code><code>RequestSecurityTokenResponseCollection</code>
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public RequestSecurityTokenResponseCollection 
            createRequestSecurityTokenResponseCollection(Element element)
            throws WSTException {
        if(STSConstants.WST_VERSION_13.equals(protocolVersion)) {
           return new com.sun.identity.wss.trust.wst13.
                   RequestSecurityTokenResponseCollection_Impl(element); 
        } else {
           return new com.sun.identity.wss.trust.wst10.
                   RequestSecurityTokenResponseCollection_Impl(element); 
        }
    }

}
