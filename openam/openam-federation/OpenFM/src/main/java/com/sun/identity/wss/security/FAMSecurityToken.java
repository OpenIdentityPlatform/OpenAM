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
 * $Id: FAMSecurityToken.java,v 1.6 2009/12/23 22:32:28 mrudul_uchil Exp $
 *
 */
package com.sun.identity.wss.security;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;
import com.sun.identity.wss.sts.STSConstants;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * The class <code>FAMSecurityToken</code> represents Federation
 * OpenSSO Security Token using OpenSSO Single Sign-on
 * Token.
 */
public class FAMSecurityToken implements SecurityToken {
    
    
     private static Debug debug = WSSUtils.debug;
     private String tokenID = null;
     private String appTokenID = null;
     private String tokenType = null;
    
     /**
      * Constructor for FAMSecurityToken
      * @param ssoToken Single Sign on token of the subject.
      * @throws com.sun.identity.wss.security.SecurityException
      */
     public FAMSecurityToken(SSOToken ssoToken) throws SecurityException {
         try {
             SSOTokenManager.getInstance().validateToken(ssoToken); 
             tokenID = ssoToken.getTokenID().toString();
             tokenType = SecurityToken.WSS_FAM_SSO_TOKEN;
             appTokenID = (WSSUtils.getAdminToken()).getTokenID().toString();
         } catch (SSOException se) {
             throw new SecurityException(se.getMessage());
         }
         
     }
     
     /**
      * Constructor that uses DOM Element
      * @param element the DOM representation of security token.
      * @throws com.sun.identity.wss.security.SecurityException
      */
     public FAMSecurityToken(Element element) throws SecurityException {
        if(element == null) {
           throw new SecurityException(
                 WSSUtils.bundle.getString("nullInput"));
        }
        
        String localName = element.getLocalName();
        if(!"FAMToken".equals(localName)) {
           throw new SecurityException(
                 WSSUtils.bundle.getString("invalidElement"));
        }
        
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();
        if(length == 0) {
           throw new SecurityException(
                 WSSUtils.bundle.getString("invalidElement"));
        }
        
        for (int i=0; i < length; i++ ) {
            Node child = (Node)nl.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            String childName = child.getLocalName();
            if(childName.equals("TokenValue")) {
               tokenID = XMLUtils.getElementValue((Element)child);
            } else if(childName.equals("AppTokenValue")) {
               appTokenID = XMLUtils.getElementValue((Element)child);
            } else if (childName.equals("TokenType")) {
               tokenType = XMLUtils.getElementValue((Element)child);
            }
        }
         
     }
     
     /**
      * Returns the security token type.
      * @return the security token type.
      */
     public String getTokenType() {
         return tokenType;
     }
     
     /**
      * Returns the security token value.
      * @return the security token value.
      */
     public String getTokenID() {
         return tokenID;
     }

     /**
      * Returns the Application/Client token value.
      * @return the Application/Client token value.
      */
     public String getAppTokenID() {
         return appTokenID;
     }

      /**
       * Convert the security token into DOM Object.
       * 
       * @return the DOM Document Element.
       *
       * @exception SecurityException if any failure is occured.
       */
     public Element toDocumentElement() throws SecurityException {
         StringBuffer sb = new StringBuffer();
         if ((appTokenID != null) && (appTokenID.length() != 0)) {
             sb.append("<fam:FAMToken xmlns:fam=\"")
               .append(STSConstants.FAM_TOKEN_NS).append("\"").append(">")
               .append("\n").append("<fam:TokenValue>").append(tokenID)
               .append("</fam:TokenValue>").append("\n")
               .append("<fam:AppTokenValue>").append(appTokenID)
               .append("</fam:AppTokenValue>").append("\n")
               .append("<fam:TokenType>").append(tokenType)
               .append("</fam:TokenType>").append("\n")
               .append("</fam:FAMToken>");
         } else {
             sb.append("<fam:FAMToken xmlns:fam=\"")
               .append(STSConstants.FAM_TOKEN_NS).append("\"").append(">")
               .append("\n").append("<fam:TokenValue>").append(tokenID)
               .append("</fam:TokenValue>").append("\n")
               .append("<fam:TokenType>").append(tokenType)
               .append("</fam:TokenType>").append("\n")
               .append("</fam:FAMToken>");

         }
         Document document = XMLUtils.toDOMDocument(sb.toString(), debug);
         return document.getDocumentElement();          
     }
}
