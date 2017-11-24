/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: BinarySecret_Impl.java,v 1.1 2009/08/29 03:06:01 mallas Exp $
 *
 */

package com.sun.identity.wss.trust.wst13;

import com.sun.identity.wss.trust.BinarySecret;
import com.sun.identity.wss.trust.WSTException;
import com.sun.identity.wss.sts.STSConstants;
import com.sun.identity.wss.sts.STSUtils;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;

import org.w3c.dom.Element;


public class BinarySecret_Impl extends BinarySecret {
    
    private Element secretE = null; 
    
    public BinarySecret_Impl() {
        
    }
    
    public BinarySecret_Impl(Element element) throws WSTException {
        
        if(element == null) {
           throw new WSTException("nullElement");    
        }
        
        if(!BINARY_SECRET.equals(element.getLocalName())) {
           throw new WSTException("invalidElement"); 
        }
        
        if(!STSConstants.WST13_NAMESPACE.equals(element.getNamespaceURI())) {
           throw new WSTException("invalidNameSpace"); 
        }
        this.secretE = element;
        String secretValue = XMLUtils.getElementValue(element);
        if(secretValue == null) {
           throw new WSTException("noSecretValue"); 
        }   
        secret = Base64.decode(secretValue);
        
    }
    
    /**
     * Returns the DOM Element representation for the binary secret.
     * @return the DOM Element representation for the binary secret.
     * @throws com.sun.identity.wss.sts.protocol.WSTException
     */
    public Element toDOMElement()  throws WSTException {
        if(secretE != null) {
           return secretE; 
        }
        return XMLUtils.toDOMDocument(toXMLString(), 
                  STSUtils.debug).getDocumentElement();
    }
    
    /**
     * Converts into XML String
     * @return the XML String for <code>BinarySecret</code>.
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public String toXMLString() throws WSTException {
        
        if(secret == null) {
           throw new WSTException("nullSecret");
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(STSConstants.WST_PREFIX)
                .append(BINARY_SECRET).append(" ")
                .append(STSConstants.WST_XMLNS).append("=\"")
                .append(STSConstants.WST13_NAMESPACE).append("\"")                
                .append(">");
        
        sb.append(Base64.encode(secret));                
        sb.append("</").append(STSConstants.WST_PREFIX)
          .append(BINARY_SECRET).append(">");            
        return sb.toString();
    }
    
}
