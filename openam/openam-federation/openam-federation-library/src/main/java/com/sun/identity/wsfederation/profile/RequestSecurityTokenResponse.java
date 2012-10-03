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
 * $Id: RequestSecurityTokenResponse.java,v 1.4 2009/12/14 23:42:48 mallas Exp $
 *
 */

package com.sun.identity.wsfederation.profile;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import java.io.InputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class encapsulates the WS-Trust &lt;RequestSecurityTokenResponse&gt; 
 * element
 */
public class RequestSecurityTokenResponse {
    private static Debug debug = WSFederationUtils.debug;
    
    protected boolean valid = true;
    protected String  recipient	= null;
    protected boolean validationDone = true;
    
    protected String appliesTo = null; 
    protected String issuer = null; 
    protected RequestedSecurityToken token = null;
    
    /** 
     * Creates a new instance of RequestSecurityTokenResponse (RSTR).
     * @param token the &lt;RequestedSecurityToken&gt; for the RSTR
     * @param appliesTo the consumer of the RSTR
     */
    public RequestSecurityTokenResponse(RequestedSecurityToken token, 
        String appliesTo) {
        this.token = token;
        this.appliesTo = appliesTo;
    }
    
    /** 
     * Creates a new instance of RequestSecurityTokenResponse (RSTR).
     * @param token the &lt;RequestedSecurityToken&gt; for the RSTR
     */
    public RequestSecurityTokenResponse(RequestedSecurityToken token) {
        this(token,null);
    }
    
    /**
     * Creates a new instance of RequestSecurityTokenResponse (RSTR) from a DOM 
     * Element
     * @param root &lt;RequestedSecurityToken&gt; element
     * @throws WSFederationException if an error occurs.
     */
    public RequestSecurityTokenResponse(Element root) 
        throws WSFederationException {
        String classMethod = "RequestSecurityTokenResponse:" + 
            "RequestSecurityTokenResponse(Element)";
	// Make sure this is a Response
	if (root == null) {
            if ( debug.messageEnabled() ) {
                debug.message(classMethod + "null input.");
            }
	    throw new WSFederationException(
		WSFederationUtils.bundle.getString("nullInput"));
	}
	String tag = null;
	if (((tag = root.getLocalName()) == null) ||
	    (!tag.equals(WSFederationConstants.RSTR_TAG_NAME))) {
            if ( debug.messageEnabled() ) {
                debug.message(classMethod + "wrong input.");
            }
	    throw new WSFederationException(
		WSFederationUtils.bundle.getString("wrongInput"));
	}

        if ( debug.messageEnabled() ) {
            debug.message(classMethod + "found RequestSecurityTokenResponse.");
        }
        
        NodeList list = root.getChildNodes();
	int length = list.getLength();
	for (int i = 0; i < length; i++) {
	    Node child = list.item(i);
            String name = child.getLocalName();
            
            if ( debug.messageEnabled() ) {
                debug.message(classMethod + "examining:"+name);
            }
            
            if ( name.equals(WSFederationConstants.APPLIESTO_TAG_NAME))
            {
                NodeList nodes = 
                    ((Element)child).getElementsByTagNameNS(
                    WSFederationConstants.WS_ADDRESSING_URI, 
                    WSFederationConstants.ADDRESS_TAG_NAME);
                // ASSUME exactly one address
                if(nodes == null || nodes.getLength() == 0) {
                   continue;
                }

                String appliesTo = nodes.item(0).getTextContent();
                
                if ( debug.messageEnabled() ) {
                    debug.message(classMethod + "found AppliesTo:" + appliesTo);
                }
            }
            else if ( name.equals(WSFederationConstants.RST_TAG_NAME))
            {
                if ( debug.messageEnabled() ) {
                    debug.message(classMethod + "found RequestedSecurityToken");
                }                
                
                token = RequestedSecurityTokenFactory.createToken(child);
            }
        }        
    }
    
    /**
     * Returns RequestSecurityTokenResponse object based on the XML document 
     * received from server. This method is used primarily at the client side. 
     * The schema of the XML document is defined in WS-Trust.
     *
     * @param xml The RequestSecurityTokenResponse XML document String.
     * @return RequestSecurityTokenResponse object based on the XML document 
     * received from server.
     * @exception WSFederationException if XML parsing failed
     */
    public static RequestSecurityTokenResponse parseXML(String xml) 
        throws WSFederationException {
	// parse the xml string
	Document doc = XMLUtils.toDOMDocument(xml, debug);
	Element root = doc.getDocumentElement();

	return new RequestSecurityTokenResponse(root);
    }
    
    /**
     * Returns RequestSecurityTokenResponse object based on the data in the 
     * input stream. This method is used primarily at the client side. 
     * The schema of the XML document is defined in WS-Trust.
     *
     * @param is an InputStream
     * @return RequestSecurityTokenResponse object based on the XML document 
     * received from server.
     * @exception WSFederationException if XML parsing failed
     */
    public static RequestSecurityTokenResponse parseXML(InputStream is) 
        throws WSFederationException {
	Document doc = XMLUtils.toDOMDocument(is, debug);
	Element root = doc.getDocumentElement();

	return new RequestSecurityTokenResponse(root);
    }
    
    /** 
     * This method returns the component RequestedSecurityToken
     * @return The RequestedSecurityToken contained in the RSTR.
     */
    public RequestedSecurityToken getRequestedSecurityToken() {
	return token;
    }

    /** 
     * This method returns the intended consumer of the RSTR
     * @return The intended consumer of the RSTR.
     */
    public String getAppliesTo() {
	return appliesTo;
    }

    /** 
     * This method marshalls the RSTR, returning a String comprising the textual 
     * XML representation.
     * @return The textual XML representation of the RSTR.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("<wst:RequestSecurityTokenResponse "+
            "xmlns:wst=\"http://schemas.xmlsoap.org/ws/2005/02/trust\">")
        .append(token.toString())
        .append("<wsp:AppliesTo ")
        .append("xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">")
        .append("<wsa:EndpointReference xmlns:")
        .append("wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">")
        .append("<wsa:Address>" + appliesTo + "</wsa:Address>")
        .append("</wsa:EndpointReference>")
        .append("</wsp:AppliesTo>")
        .append("</wst:RequestSecurityTokenResponse>");

        return buffer.toString();
    }
}
