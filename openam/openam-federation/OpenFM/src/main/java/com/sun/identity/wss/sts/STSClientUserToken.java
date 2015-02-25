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
 * $Id: STSClientUserToken.java,v 1.13 2009/12/23 22:32:29 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.sts;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.xml.XMLUtils;
import javax.xml.parsers.ParserConfigurationException;
import com.sun.identity.wss.security.FAMSecurityToken;
import com.sun.identity.wss.security.SecurityException;
import com.sun.identity.wss.security.SecurityToken;


/**
 * This class implements ClientUserToken for on behalf of token that will be 
 * used to pass end user OpenSSO SSO Token to the STS service.
 */
public class STSClientUserToken implements ClientUserToken {
     
    FAMSecurityToken famToken = null;
    String tokenType = null;
    String tokenValue = null;
    String appTokenID = null;
    
    /** Creates a new instance of STSClientUserToken */    
    public STSClientUserToken() {        
    }
    
    public void init (Object credential) throws FAMSTSException {        
        try {
            if(credential instanceof SSOToken) {
               famToken = new FAMSecurityToken((SSOToken)credential);               
               tokenType = SecurityToken.WSS_FAM_SSO_TOKEN;
               tokenValue = famToken.getTokenID();
               appTokenID = famToken.getAppTokenID();
            } else if (credential instanceof Element) {
               Element element = (Element)credential;
               if(!"Assertion".equals(element.getLocalName())) {
                  throw new FAMSTSException(
                          STSUtils.bundle.getString("unsupportedcredential")); 
               }
               String ns = element.getNamespaceURI();               
               if(STSConstants.SAML10_ASSERTION.equals(ns)){
                  tokenType = SecurityToken.WSS_SAML_TOKEN; 
               } else if(STSConstants.SAML20_ASSERTION.equals(ns)) {
                  tokenType = SecurityToken.WSS_SAML2_TOKEN; 
               } else {
                  throw new FAMSTSException(
                          STSUtils.bundle.getString("unsupportedcredential")); 
               }
               this.tokenValue = XMLUtils.print((Element)credential);              
            } else {
               throw new FAMSTSException(
                          STSUtils.bundle.getString("unsupportedcredential")); 
            }
        } catch (SecurityException sse) {
            throw new FAMSTSException(sse.getMessage());
        }
    }
    
    public STSClientUserToken(Element element) throws FAMSTSException {
        parse(element);
    }
    
    public void parse(Element element) throws FAMSTSException {
        if(element == null) {
           throw new FAMSTSException(STSUtils.bundle.getString("nullinput"));
        }
                
        String localName = element.getLocalName();
        if(!"FAMToken".equals(localName)) {
           throw new FAMSTSException(
                   STSUtils.bundle.getString("invalidelementname"));
        }
        
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();
        if(length == 0) {
           throw new FAMSTSException(
                   STSUtils.bundle.getString("invalidelementname"));
        }
        
        for (int i=0; i < length; i++ ) {
            Node child = (Node)nl.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            String childName = child.getLocalName();
            if(childName.equals("TokenValue")) {
               tokenValue = XMLUtils.getChildrenValue((Element)child);
            } else if(childName.equals("AppTokenValue")) {
               appTokenID = XMLUtils.getElementValue((Element)child);
            } else if (childName.equals("TokenType")) {
               tokenType = XMLUtils.getElementValue((Element)child);
            }
        }
    }
    
    public Element getTokenValue() {        
        Document document = XMLUtils.toDOMDocument(toString(), STSUtils.debug);
        return document.getDocumentElement();
    }     
    
    public String getTokenId() {
        return tokenValue;
    }

    public String getPrincipalName() throws FAMSTSException {        
        try {
            SSOToken ssoToken = 
                 SSOTokenManager.getInstance().createSSOToken(tokenValue);
            return ssoToken.getPrincipal().getName(); 
        } catch (SSOException se) {
            throw new FAMSTSException(se.getMessage()); 
        }
    }
    
    @Override
    public String toString() {
        if(famToken != null) {
           try {
               return XMLUtils.print(famToken.toDocumentElement());
           } catch (SecurityException se) {
               throw new RuntimeException(se.getMessage());
           }
        }
        StringBuffer sb = new StringBuffer();
        if ((appTokenID != null) && (appTokenID.length() != 0)) {
            sb.append("<fam:FAMToken xmlns:fam=\"")
               .append(STSConstants.FAM_TOKEN_NS).append("\"").append(">")
               .append("\n").append("<fam:TokenValue>").append(tokenValue)
               .append("</fam:TokenValue>").append("\n")
               .append("<fam:AppTokenValue>").append(appTokenID)
               .append("</fam:AppTokenValue>").append("\n")
               .append("<fam:TokenType>").append(tokenType)
               .append("</fam:TokenType>").append("\n")
               .append("</fam:FAMToken>");
        } else {
            sb.append("<fam:FAMToken xmlns:fam=\"")
               .append(STSConstants.FAM_TOKEN_NS).append("\"").append(">")
               .append("\n").append("<fam:TokenValue>").append(tokenValue)
               .append("</fam:TokenValue>").append("\n")
               .append("<fam:TokenType>").append(tokenType)
               .append("</fam:TokenType>").append("\n")
               .append("</fam:FAMToken>");

        }
        
        return sb.toString();
    }
    
    public String getType() {
        return tokenType;
    }

    /**
     * Returns the Application/Client token value.
     * @return the Application/Client token value.
     */
    public String getAppTokenID() {
        return appTokenID;
    }
}
