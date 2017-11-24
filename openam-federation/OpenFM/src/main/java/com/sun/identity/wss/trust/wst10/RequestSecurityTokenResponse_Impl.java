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
 * $Id: RequestSecurityTokenResponse_Impl.java,v 1.2 2009/08/29 03:06:00 mallas Exp $
 *
 */

package com.sun.identity.wss.trust.wst10;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import com.sun.identity.wss.sts.STSConstants;
import com.sun.identity.wss.sts.STSUtils;
import com.sun.identity.wss.trust.WSTException;
import com.sun.identity.wss.trust.RequestSecurityTokenResponse;
import com.sun.identity.shared.xml.XMLUtils;

public class RequestSecurityTokenResponse_Impl extends
          RequestSecurityTokenResponse {
    
    private Element rstrE = null;
    
    public RequestSecurityTokenResponse_Impl() {
        //Constructor
    }
    
    public RequestSecurityTokenResponse_Impl(Element element)
            throws WSTException {
        
        if(element == null) {
           throw new WSTException("Invalid Element");
        }
        
        if(!(REQUEST_SECURITY_TOKEN_RESPONSE.equals(element.getLocalName()))
               || !(STSConstants.WST10_NAMESPACE.equals(
                          element.getNamespaceURI()))) {
           throw new WSTException("Invalid Element"); 
        }
        this.rstrE = element;
        NodeList nl = element.getChildNodes();
        if(nl.getLength() == 0) {
           throw new WSTException("Invalid element"); 
        }
        
        for (int i=0; i< nl.getLength(); i++) {
            Node node = nl.item(i);
            if(node.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }
            
            Element child = (Element)node;
            String localName = child.getLocalName();
            if(TOKEN_TYPE.equals(localName)) {
               tokenType = XMLUtils.getElementValue(child);            
            } else if(REQUESTED_SECURITY_TOKEN.equals(localName)) {
               requestedSecurityToken = child; 
            } else if(APPLIES_TO.equals(localName)) {
               appliesTo = STSUtils.getAppliesTo(child);
            } else if(REQUEST_TYPE.equals(requestType)) {
               requestType =  XMLUtils.getElementValue(child);
            } else if(REQUESTED_PROOF_TOKEN.equals(localName)) {
               requestedProofToken = 
                       new RequestedProofToken_Impl((Element)child); 
            }
        }
    }
    
    public Element toDOMElement() throws WSTException {
        if(rstrE != null) {
           return rstrE; 
        }
        
        return XMLUtils.toDOMDocument(toXMLString(), 
                  STSUtils.debug).getDocumentElement();
    }
    
    public String toXMLString() throws WSTException {
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(STSConstants.WST_PREFIX)
                .append(REQUEST_SECURITY_TOKEN_RESPONSE).append(" ")
                .append(STSConstants.WST_XMLNS).append("=\"")
                .append(STSConstants.WST10_NAMESPACE).append("\"")
                .append(" ").append(STSConstants.WSP_XMLNS).append("=")
                .append("\"").append(STSConstants.WSP_NS).append("\"")
                .append(" ").append(STSConstants.WSA_XMLNS).append("=")
                .append("\"").append(STSConstants.WSA_NS).append("\"")
                .append(">");
        if(tokenType != null) {
           sb.append("<").append(STSConstants.WST_PREFIX).append(TOKEN_TYPE)
                .append(">").append(tokenType).append("</")
                .append(STSConstants.WST_PREFIX)
                .append(TOKEN_TYPE).append(">");
        }
        
        if(requestType != null) {
           sb.append("<").append(STSConstants.WST_PREFIX).append(REQUEST_TYPE)
                .append(">").append(requestType).append("</")
                .append(STSConstants.WST_PREFIX)
                .append(REQUEST_TYPE).append(">"); 
        }
        
        if(appliesTo != null) {
           sb.append("<").append(STSConstants.WSP_PREFIX)
             .append(APPLIES_TO).append(">")
             .append("<").append(STSConstants.WSA_PREFIX)
             .append(EP_REFERENCE).append(">").append("<")
             .append(STSConstants.WSA_PREFIX)
             .append(ADDRESS).append(">").append(appliesTo)
             .append("</").append(STSConstants.WSA_PREFIX)
             .append(ADDRESS).append(">")
             .append("</").append(STSConstants.WSA_PREFIX)
             .append(EP_REFERENCE).append(">")
             .append("</").append(STSConstants.WSP_PREFIX)
             .append(APPLIES_TO).append(">"); 
        }
        
        if(requestedProofToken != null) {
           sb.append(requestedProofToken.toXMLString());
        }
        sb.append("</").append(STSConstants.WST_PREFIX)
          .append(REQUEST_SECURITY_TOKEN_RESPONSE).append(">");
            
        return sb.toString();
    }

}
