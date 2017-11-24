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
 * $Id: RequestSecurityTokenResponseCollection_Impl.java,v 1.1 2008/09/19 16:00:57 mallas Exp $
 *
 */

package com.sun.identity.wss.trust.wst13;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.util.ArrayList;
import java.util.Iterator;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.wss.trust.WSTException;
import com.sun.identity.wss.sts.STSConstants;
import com.sun.identity.wss.sts.STSUtils;
import com.sun.identity.wss.trust.RequestSecurityTokenResponseCollection;


public class RequestSecurityTokenResponseCollection_Impl
        extends RequestSecurityTokenResponseCollection {
    
    public RequestSecurityTokenResponseCollection_Impl() {
        //Default constructor
    }
    
    public RequestSecurityTokenResponseCollection_Impl(Element element)
            throws WSTException {
        if(element == null) {
           throw new WSTException("nullElement"); 
        }
        String localName = element.getLocalName();
        if(!REQUEST_SECURITY_TOKEN_RESPONSE_COLLECTION.equals(localName)) {
           throw new WSTException("InvalidElement"); 
        }
        
        NodeList nl = element.getChildNodes();
        if(nl.getLength() == 0) {
           throw new WSTException("nullElements"); 
        }
        rstResponses = new ArrayList();
        for(int i=0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if(node.getNodeType() != Node.ELEMENT_NODE) {
               continue; 
            }
            Element rstrE = (Element)node;
            rstResponses.add(new RequestSecurityTokenResponse_Impl(rstrE));
        }
    }
    
    public Element toDOMElement() throws WSTException {
        return XMLUtils.toDOMDocument(toXMLString(), 
                STSUtils.debug).getDocumentElement();
    }
    public String toXMLString() throws WSTException  {
        
        if(rstResponses.size() == 0) {
           throw new WSTException("null elements"); 
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(STSConstants.WST_PREFIX)
                .append(REQUEST_SECURITY_TOKEN_RESPONSE_COLLECTION).append(" ")
                .append(STSConstants.WST_XMLNS).append("=\"")
                .append(STSConstants.WST13_NAMESPACE).append("\"")
                .append(" ").append(STSConstants.WSP_XMLNS).append("=")
                .append("\"").append(STSConstants.WSP_NS).append("\"")
                .append(" ").append(STSConstants.WSA_XMLNS).append("=")
                .append("\"").append(STSConstants.WSA_NS).append("\"")
                .append(">");
        
        for (Iterator iter = rstResponses.iterator(); iter.hasNext();) {
             RequestSecurityTokenResponse_Impl rstR = 
                    (RequestSecurityTokenResponse_Impl)iter.next();
             sb.append(rstR.toXMLString());
        }
        
        sb.append("</").append(STSConstants.WST_PREFIX)
          .append(REQUEST_SECURITY_TOKEN_RESPONSE_COLLECTION)
          .append(">");
        return sb.toString();
    }

}
