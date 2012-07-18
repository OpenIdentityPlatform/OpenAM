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
 * $Id: UserNameToken.java,v 1.8 2009/01/24 01:31:25 mallas Exp $
 *
 */

package com.sun.identity.wss.security;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Date;
import java.util.ResourceBundle;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.DateUtils;
//import com.iplanet.am.util.Locale;
import com.sun.identity.shared.locale.Locale;
import com.iplanet.am.util.SecureRandomManager;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This class <code>UserNameToken</code> represents an Username
 * token that can be inserted into web services security header
 * for message level security.
 *
 * <p>This class implements <code>SecurityToken</code> and can be
 * created through security token factory. 
 */
public class UserNameToken implements SecurityToken {

    private static final String USER_NAME_TOKEN = "UsernameToken";
    private static final String USER_NAME = "Username";
    private static final String PASSWORD = "Password";
    private static final String NONCE = "Nonce";
    private static final String CREATED = "Created";

    private String passwordType = null;
    private boolean setNonce = false;
    private boolean setTimeStamp = false;
    private String nonce = null;
    private String created = null;
    private String username = null;
    private String password = null;
    private String xmlString = null;
    private String id = null;
    private static Debug debug = WSSUtils.debug;
    private static ResourceBundle bundle = WSSUtils.bundle;

    /**
     * Constructs a user name token with the user name token specification.
     *
     * @param tokenSpec the user name token specification.
     * @exception SecurityException if there is a failure.
     */
    public UserNameToken(UserNameTokenSpec tokenSpec) throws SecurityException {

        if(tokenSpec == null) {
           throw new SecurityException(
                 bundle.getString("invalidTokenSpec"));
        }
        debug.message("UserNameToken.constructor:");
        username = tokenSpec.getUserName();
        if(username == null) {
           debug.error("UserNameToken:: username is null");
           throw new SecurityException(bundle.getString("invalidTokenSpec"));
        }
        username = username.trim();
        if(username.length() == 0) {
           debug.error("UserNameToken:: username is null");
           throw new SecurityException(bundle.getString("invalidTokenSpec"));
        }

        passwordType = tokenSpec.getPasswordType();
        
        setNonce = tokenSpec.isCreateNonce();
        setTimeStamp = tokenSpec.isCreateTimeStamp();
        if(setNonce) {
           createNonce();
        }
        if(setTimeStamp) {
           created = DateUtils.toUTCDateFormat(new Date());
        }
        setPassword(tokenSpec.getPassword());
        id = SAMLUtils.generateID();
    }

    /**
     * Constructor to create a username token using username token xml element.
     * @param element username token xml element.
     * @exception SecurityException if the element parsing fails.
     */
    public UserNameToken(Element element) throws SecurityException {

        if(element == null) {
           throw new IllegalArgumentException(
                 bundle.getString("nullInputParameter"));
        }
        if(!USER_NAME_TOKEN.equals(element.getLocalName()) ||
              !WSSConstants.WSSE_NS.equals(element.getNamespaceURI())) {
           throw new SecurityException(
                 bundle.getString("invalidElement"));
        }
        NodeList childNodes = element.getChildNodes();
        if(childNodes == null || childNodes.getLength() == 0) {
           throw new SecurityException(
                 bundle.getString("invalidElement"));
        }
        for(int i=0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if(child.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }
            Element childElem = (Element)child; 
            if(USER_NAME.equals(child.getLocalName()) &&
                 WSSConstants.WSSE_NS.equals(child.getNamespaceURI()) ) {
               username = child.getFirstChild().getNodeValue();

            } else if(PASSWORD.equals(child.getLocalName()) &&
                 WSSConstants.WSSE_NS.equals(child.getNamespaceURI()) ) {
               password = child.getFirstChild().getNodeValue();
               Attr attr = childElem.getAttributeNodeNS(
                                   WSSConstants.WSSE_NS, "Type");
               if(attr != null) {
                  passwordType =  attr.getNodeValue();
               } else {
                  passwordType = childElem.getAttribute("Type");
               }                   

            } else if(NONCE.equals(child.getLocalName()) &&
                  WSSConstants.WSSE_NS.equals(child.getNamespaceURI()) ) {
               nonce = child.getFirstChild().getNodeValue();

            } else if(CREATED.equals(child.getLocalName()) &&
                  WSSConstants.WSU_NS.equals(child.getNamespaceURI()) ) {
               created = child.getFirstChild().getNodeValue();
            } else {
               if(debug.messageEnabled()) {
                  debug.message("UserNameToken.constructor:: Invalid element "+
                     child.getLocalName());
               }
            }
            
        }
        if(username == null || username.length() ==0) {
           debug.error("UserNameToken.constructor:: username is null");
           throw new SecurityException(
                 bundle.getString("invalidElement"));
        }
         
    }

    /**
     * Returns the username in the username token.
     *
     * @return the user name
     */ 
    public String getUserName() {
        return username;
    } 

    /**
     * Sets the user name in the username token.
     * @param username the user name.
     */
    public void setUserName(String username) {
        this.username = username;
    }

    /**
     * Returns the password in the username token.
     * @return the password in the username token.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the password type.
     * @return the password type.
     */
    public String getPasswordType() {
        return passwordType;
    }

    /**
     * Sets the password to the username token.
     * @param passwd the password to the username token.
     * @exception SecurityException if the password digest is failed when
     *            the digest password type is set.
     */
    public void setPassword(String passwd) throws SecurityException {
        if(passwd == null) {
           debug.error("UserNameToken.setPassword:: password is empty");
           throw new SecurityException(
                 bundle.getString("invalidTokenSpec"));
        }

        if(passwordType != null && 
               WSSConstants.PASSWORD_DIGEST_TYPE.equals(passwordType)) {
           password = getPasswordDigest(passwd, nonce, created);

        } else {
           password = passwd;
        }
    }

    /**
     * Returns the nonce
     * @return the nonce
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Returns the created.
     * @return the created.
     */
    public String getCreated() {
        return created;
    }
    
    /**
     * Returns the signing id for username token.
     * @return the signing id for the username token.
     */
    public String getSigningId() {
        return id;
    }

    /**
     * Returns as the string format for this username token
     */
    public String toString() {

        if(xmlString != null) {
           return xmlString;
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(WSSConstants.WSSE_TAG).append(":")
          .append(USER_NAME_TOKEN).append(" ")
          .append(WSSConstants.TAG_XML_WSSE).append("=\"")
          .append(WSSConstants.WSSE_NS).append("\" ")
          .append(WSSConstants.TAG_XML_WSU).append("=\"")
          .append(WSSConstants.WSU_NS).append("\" ")
          .append(WSSConstants.WSU_ID).append("=\"").append(id)
          .append("\">\n")
          .append("<").append(WSSConstants.WSSE_TAG).append(":")
          .append(USER_NAME).append(">").append(username)
          .append("</").append(WSSConstants.WSSE_TAG).append(":")
          .append(USER_NAME).append(">\n")
          .append("<").append(WSSConstants.WSSE_TAG).append(":")
          .append(PASSWORD);
        if(passwordType != null) {
           sb.append(" ").append("Type=").append("\"").append(passwordType)
             .append("\"");
        }
        sb.append(">").append(password)
          .append("</").append(WSSConstants.WSSE_TAG).append(":")
          .append(PASSWORD).append(">\n");
        if(nonce != null) { 
           sb.append("<").append(WSSConstants.WSSE_TAG).append(":")
             .append(NONCE).append(" ").append("EncodingType=")
             .append("\"").append(BinarySecurityToken.BASE64BINARY)
             .append("\"").append(">").append(nonce)
             .append("</").append(WSSConstants.WSSE_TAG).append(":")
             .append(NONCE).append(">\n");
        }
        if(created != null) {
           sb.append("<").append(WSSConstants.WSU_TAG).append(":")
             .append(CREATED).append(">").append(created)
             .append("</").append(WSSConstants.WSU_TAG).append(":")
             .append(CREATED).append(">\n");
        }
        sb.append("</").append(WSSConstants.WSSE_TAG).append(":")
          .append(USER_NAME_TOKEN).append(">\n");

        xmlString = sb.toString();
        if(debug.messageEnabled()) {
           debug.message("UserNameToken.toString:: \n" + xmlString);
        }
        return xmlString;
    }

    /**
     * Returns the XML document element for the username security token.
     * @return Element the XML Element for the username security token.
     * @exception if the XML document conversion is failed.
     */
    public Element toDocumentElement() throws SecurityException {
        Document document = XMLUtils.toDOMDocument(toString(), debug);
        if(document == null) {
           throw new SecurityException(
                 bundle.getString("cannotConvertToDocument"));
        }
        return document.getDocumentElement();
    }

    /**
     * Returns this security token type.
     *
     * @return user name security token.
     */
    public String getTokenType() {
        return SecurityToken.WSS_USERNAME_TOKEN ;
    }

    /**
     * Creates nonce.
     */
    private void createNonce() throws SecurityException {

        byte[] nonceValue = new byte[18];
        try {
            SecureRandom secureRandom = SecureRandomManager.getSecureRandom();
            secureRandom.nextBytes(nonceValue);
            nonce = Base64.encode(nonceValue);
        } catch (Exception ex) {
            debug.error("UserNameToken.createNonce:: exception", ex);
            throw new SecurityException(); 
        }

    }

    /**
     * Returns the password digest for the given password using nonce 
     * and created timestamp.
     * @param password the password that needs to be digested.
     * @param nonce the nonce that is used to digest the password.
     * @param created the created that is used to digest the password.
     * @exception SecurityException if the password digest is failed.
     */
    public static String getPasswordDigest(String password, String nonce, 
                  String created) throws SecurityException {
        try {
            if((nonce == null) || (created == null) || (password == null) ) {
               debug.error("UserNameToken.getPasswordDigest:: nonce and " +
               "created are required"); 
               throw new IllegalArgumentException("nullInputParams");
            }
            byte[] b1 = Base64.decode(nonce);
            byte[] b2 = created.getBytes("UTF-8");
            byte[] b3 = password.getBytes("UTF-8");
            byte[] b4 = new byte[b1.length + b2.length + b3.length];
            int i = 0;
            int offset = 0;
            System.arraycopy(b1, 0, b4, offset, b1.length);
            offset += b1.length;

            System.arraycopy(b2, 0, b4, offset, b2.length);
            offset += b2.length;

            System.arraycopy(b3, 0, b4, offset, b3.length);

            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            sha.reset();
            sha.update(b4);
            return Base64.encode(sha.digest());
            
        } catch (Exception ex) {
            debug.error("UserNameToken.getPasswordDigest:: password digest" +
            " error.", ex);
            throw new SecurityException(
                  bundle.getString("passwordDigestFailed"));
        }
          
    }

}
