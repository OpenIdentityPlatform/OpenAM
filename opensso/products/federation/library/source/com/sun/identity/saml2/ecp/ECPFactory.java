/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ECPFactory.java,v 1.2 2008/06/25 05:47:46 qcheng Exp $
 *
 */


package com.sun.identity.saml2.ecp;

import org.w3c.dom.Element;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.ecp.impl.ECPRelayStateImpl;
import com.sun.identity.saml2.ecp.impl.ECPRequestImpl;
import com.sun.identity.saml2.ecp.impl.ECPResponseImpl;

/**
 * This is the factory class to obtain object instances for concrete elements in
 * the ecp schema. This factory class provides 3 methods for each element.
 * <code>createElementName()</code>,
 * <code>createElementName(String value)</code>,
 * <code>createElementName(org.w3c.dom.Element value)</code>.
 *
 * @supported.all.api
 */
public class ECPFactory  {
    
    private static ECPFactory ecpInstance = new ECPFactory();
    
    /* Constructor for ECPFactory */
    private ECPFactory() {
    }
    
    /**
     * Returns an instance of the <code>ECPFactory</code> Object.
     *
     * @return an instance of the <code>ECPFactory</code> object.
     */
    public static ECPFactory getInstance() {
        return ecpInstance;
    }
    
    /**
     * Returns the <code>ECPRelayState</code> Object.
     *
     * @return the <code>ECPRelayState</code> object.
     * @throws SAML2Exception if <code>ECPRelayState</code> cannot be created.
     */
    public ECPRelayState createECPRelayState() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ECP_RELAY_STATE);
	if (obj == null) {
            return new ECPRelayStateImpl();
	} else {
            return (ECPRelayState) obj;
	}
    }

    /**
     * Returns the <code>ECPRelayState</code> Object.
     *
     * @param value the Document Element of ECP <code>RelayState</code> object.
     * @return the <code>ECPRelayState</code> object.
     * @throws SAML2Exception if <code>ECPRelayState</code> cannot be created.
     */
    
    public ECPRelayState createECPRelayState(Element value)
        throws SAML2Exception {

	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ECP_RELAY_STATE, value);
	if (obj == null) {
            return new ECPRelayStateImpl(value);
	} else {
            return (ECPRelayState) obj;
	}
    }
    
    /**
     * Returns the <code>ECPRelayState</code> Object.
     *
     * @param value ECP <code>RelayState</code> XML String.
     * @return the <code>ECPRelayState</code> object.
     * @throws SAML2Exception if <code>ECPRelayState</code> cannot be created.
     */
    public ECPRelayState createECPRelayState(String value)
        throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ECP_RELAY_STATE, value);
	if (obj == null) {
            return new ECPRelayStateImpl(value);
	} else {
            return (ECPRelayState) obj;
	}
    }

    /**
     * Returns the <code>ECPRequest</code> Object.
     *
     * @return the <code>ECPRequest</code> object.
     * @throws SAML2Exception if <code>ECPRequest</code> cannot be created.
     */
    public ECPRequest createECPRequest() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ECP_REQUEST);
	if (obj == null) {
            return new ECPRequestImpl();
	} else {
            return (ECPRequest) obj;
	}
    }

    /**
     * Returns the <code>ECPRequest</code> Object.
     *
     * @param value the Document Element of ECP <code>Request</code> object.
     * @return the <code>ECPRequest</code> object.
     * @throws SAML2Exception if <code>ECPRequest</code> cannot be created.
     */
    
    public ECPRequest createECPRequest(Element value)
        throws SAML2Exception {

	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ECP_REQUEST, value);
	if (obj == null) {
            return new ECPRequestImpl(value);
	} else {
            return (ECPRequest) obj;
	}
    }
    
    /**
     * Returns the <code>ECPRequest</code> Object.
     *
     * @param value ECP <code>Request</code> XML String.
     * @return the <code>ECPRequest</code> object.
     * @throws SAML2Exception if <code>ECPRequest</code> cannot be created.
     */
    public ECPRequest createECPRequest(String value)
        throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ECP_REQUEST, value);
	if (obj == null) {
            return new ECPRequestImpl(value);
	} else {
            return (ECPRequest) obj;
	}
    }

    /**
     * Returns the <code>ECPResponse</code> Object.
     *
     * @return the <code>ECPResponse</code> object.
     * @throws SAML2Exception if <code>ECPResponse</code> cannot be created.
     */
    public ECPResponse createECPResponse() throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ECP_RESPONSE);
	if (obj == null) {
            return new ECPResponseImpl();
	} else {
            return (ECPResponse) obj;
	}
    }

    /**
     * Returns the <code>ECPResponse</code> Object.
     *
     * @param value the Document Element of ECP <code>Response</code> object.
     * @return the <code>ECPResponse</code> object.
     * @throws SAML2Exception if <code>ECPResponse</code> cannot be created.
     */
    
    public ECPResponse createECPResponse(Element value)
        throws SAML2Exception {

	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ECP_RESPONSE, value);
	if (obj == null) {
            return new ECPResponseImpl(value);
	} else {
            return (ECPResponse) obj;
	}
    }
    
    /**
     * Returns the <code>ECPResponse</code> Object.
     *
     * @param value ECP <code>Response</code> XML String.
     * @return the <code>ECPResponse</code> object.
     * @throws SAML2Exception if <code>ECPResponse</code> cannot be created.
     */
    public ECPResponse createECPResponse(String value)
        throws SAML2Exception {
	Object obj = SAML2SDKUtils.getObjectInstance(
            SAML2SDKUtils.ECP_RESPONSE, value);
	if (obj == null) {
            return new ECPResponseImpl(value);
	} else {
            return (ECPResponse) obj;
	}
    }

}

