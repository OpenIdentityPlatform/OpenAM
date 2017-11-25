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
 * $Id: RequestSecurityToken_Impl.java,v 1.3 2009/10/13 23:19:50 mallas Exp $
 *
 */
package com.sun.identity.wss.trust.wst10;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.util.Iterator;
import java.util.ArrayList;

import com.sun.identity.wss.trust.RequestSecurityToken;
import com.sun.identity.wss.trust.WSTException;
import com.sun.identity.wss.sts.STSConstants;
import com.sun.identity.wss.sts.STSUtils;
import com.sun.identity.wss.trust.ClaimType;
import com.sun.identity.shared.xml.XMLUtils;


public class RequestSecurityToken_Impl extends RequestSecurityToken {
    
    private Element rstE = null;
    
    public RequestSecurityToken_Impl() {
       // Constructor 
    }
    
    public RequestSecurityToken_Impl(Element element) throws WSTException {
        
        //TODO Schema checking
        if(element == null) {
           throw new WSTException("NullElement");    
        }
        
        if(!REQUEST_SECURITY_TOKEN.equals(element.getLocalName())) {
           throw new WSTException("InvalidElement"); 
        }
        
        if(!STSConstants.WST10_NAMESPACE.equals(element.getNamespaceURI())) {
           throw new WSTException("InvalidNameSpace"); 
        }
        this.rstE = element;
        
        NodeList nl = element.getChildNodes();
        for (int i=0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if(node.getNodeType() != Node.ELEMENT_NODE) {
               continue; 
            }            
            Element child = (Element)node;
            String localName = child.getLocalName();
            if(TOKEN_TYPE.equals(localName)) {
               tokenType =  XMLUtils.getElementValue(child);
            } else if(REQUEST_TYPE.equals(localName)) {
               requestType = XMLUtils.getElementValue(child);              
            } else if(APPLIES_TO.equals(localName)) {
               appliesTo = STSUtils.getAppliesTo(child); 
            } else if(ON_BEHALF_OF.equals(localName)) {
               oboToken = (Element)child.getFirstChild();
            } else if(KEY_TYPE.equals(localName)) {
               keyType =  XMLUtils.getElementValue(child);
            } else if(CLAIMS.equals(localName)) {
               NodeList claimNodes = child.getChildNodes();
               claimTypes = new ArrayList();
               for (int j =0; j < claimNodes.getLength(); j++) {
                   Node n = claimNodes.item(j);
                   if(n.getNodeType() != Node.ELEMENT_NODE) {
                      continue; 
                   }
                   ClaimType claimType = 
                           new ClaimType((Element)n);
                   claimTypes.add(claimType);
               }
            }
        }        
        
    }
    
    public Element toDOMElement() throws WSTException {
        if(rstE != null) {
           return rstE;
        }
        
        return XMLUtils.toDOMDocument(
                toXMLString(), STSUtils.debug).getDocumentElement();
    }
    
    public String toXMLString() throws WSTException {
        
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(STSConstants.WST_PREFIX)
                .append(REQUEST_SECURITY_TOKEN).append(" ")
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
        
        if(requestType == null || requestType.length() == 0) {
           throw new WSTException("RequestType is null");    
        }
        sb.append("<").append(STSConstants.WST_PREFIX).append(REQUEST_TYPE)
                .append(">").append(requestType).append("</")
                .append(STSConstants.WST_PREFIX)
                .append(REQUEST_TYPE).append(">");
        
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
        
        if(claimTypes != null && !claimTypes.isEmpty()) {
           sb.append("<").append(STSConstants.WST_PREFIX)
             .append(CLAIMS).append(" Dialect=").append("\"")
             .append(ClaimType.IDENTITY_NS).append("\"").append(">");             
           for (Iterator iter = claimTypes.iterator(); iter.hasNext();) {
               ClaimType ct = (ClaimType)iter.next();
               sb.append(ct.toXMLString());
           }
           sb.append("</").append(STSConstants.WST_PREFIX)
             .append(CLAIMS).append(">");
        }
        
        if(oboToken != null) {
           sb.append("<").append(STSConstants.WST_PREFIX)
             .append(ON_BEHALF_OF).append(">")
             .append(XMLUtils.print(oboToken)).append("</")
             .append(STSConstants.WST_PREFIX)
             .append(ON_BEHALF_OF).append(">");
        }
        
        if(keyType != null) {
           sb.append("<").append(STSConstants.WST_PREFIX)
             .append(KEY_TYPE).append(">")
             .append(keyType).append("</")
             .append(STSConstants.WST_PREFIX)
             .append(KEY_TYPE).append(">");
        }                
        
        sb.append("</").append(STSConstants.WST_PREFIX)
          .append(REQUEST_SECURITY_TOKEN).append(">");
        return sb.toString();
    }

}
