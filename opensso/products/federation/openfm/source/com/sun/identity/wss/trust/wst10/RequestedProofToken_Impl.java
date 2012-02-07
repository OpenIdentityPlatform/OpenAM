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
 * $Id: RequestedProofToken_Impl.java,v 1.1 2009/08/29 03:06:00 mallas Exp $
 *
 */
package com.sun.identity.wss.trust.wst10;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import com.sun.identity.wss.trust.RequestedProofToken;
import com.sun.identity.wss.trust.WSTException;
import com.sun.identity.wss.sts.STSConstants;
import com.sun.identity.wss.sts.STSUtils;
import com.sun.identity.shared.xml.XMLUtils;

public class RequestedProofToken_Impl extends RequestedProofToken {
    
    private Element rptE = null;
    
    //Default constructor
    public RequestedProofToken_Impl () {
        
    }
    
    public RequestedProofToken_Impl(Element element) throws WSTException {
        
        if(element == null) {
           throw new WSTException("nullElement");    
        }
        
        if(!REQUESTED_PROOF_TOKEN.equals(element.getLocalName())) {
           throw new WSTException("invalidElement"); 
        }
        
        if(!STSConstants.WST10_NAMESPACE.equals(element.getNamespaceURI())) {
           throw new WSTException("invalidNameSpace"); 
        }
        this.rptE = element;
        
        NodeList nl = element.getChildNodes();
        for (int i=0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if(node.getNodeType() != Node.ELEMENT_NODE) {
               continue; 
            }
            Element child = (Element)node;
            String localName = child.getLocalName();
            if(BINARY_SECRET.equals(localName)) {
               binarySecret = new BinarySecret_Impl((Element)child);   
            } else if(ENCRYPTED_KEY.equals(localName)) {
               encryptedKey = (Element)child; 
            }
        }
    }
    
    public Element toDOMElement() throws WSTException {
        if(rptE != null) {
           return rptE; 
        }
        return XMLUtils.toDOMDocument(toXMLString(), 
                  STSUtils.debug).getDocumentElement();
        
    }
    
    public String toXMLString() throws WSTException {
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(STSConstants.WST_PREFIX)
                .append(REQUESTED_PROOF_TOKEN).append(" ")
                .append(STSConstants.WST_XMLNS).append("=\"")
                .append(STSConstants.WST10_NAMESPACE).append("\"")                
                .append(">");
        if(binarySecret != null) {
           sb.append(binarySecret.toXMLString()); 
        }
        
        if(encryptedKey != null) {
           sb.append(XMLUtils.print(encryptedKey)); 
        }
                
        sb.append("</").append(STSConstants.WST_PREFIX)
          .append(REQUESTED_PROOF_TOKEN).append(">");            
        return sb.toString();
    }
     
    
}