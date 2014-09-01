/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SecurityTokenManagerImpl.java,v 1.3 2008/06/25 05:47:21 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.security;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.disco.EncryptedResourceID;

import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;

import java.io.ByteArrayInputStream;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * This class implements the <code>SecurityTokenManagerIF</code>.
 */
public class SecurityTokenManagerImpl implements SecurityTokenManagerIF {

    private SecurityTokenManager securityTokenManager;
    private static Object lock = new Object();

    // Flag used to check if service is available locally
    protected static boolean isLocal;

    /** 
     * Initializes the SecurityTokenManager.
     *
     * @param sessionID the session id.
     * @throws SecurityTokenException if there is an error.
     */
    public void initialization(String sessionID) throws SecurityTokenException {
        try {
            Object session =
                SessionManager.getProvider().getSession(sessionID);
            securityTokenManager = new SecurityTokenManager(session);
        } catch (Exception e) {
            SecurityTokenManager.debug.error(
                    "SecurityTokenManagerImpl: Unable to get " +
                    "SecurityTokenManager", e);
            throw(new SecurityTokenException(e.getMessage()));
        }
        
    }
    
    /**
     * Sets the Certificate.
     *
     * @param cert the Certificate String.
     * @param alias if true then Certificate Alias will be set.
     * @throws SecurityTokenException if there is an error.
     */
    public void setCertificate(String cert, boolean alias)
                                  throws SecurityTokenException {
        if (alias) { // passing cert alias
            securityTokenManager.setCertAlias(cert);
        } else { //passed Base64 encoded certificate
            securityTokenManager.setCertificate(getX509Certificate(cert));
        }
    }
    
    /**
     * Checks if the service is available locally.
     */
    public void checkForLocal() {
        isLocal = true;
    }
    
    /**
     * Returns the Certificate Token.
     *
     * @return the Certification Token String.
     * @throws SecurityTokenException if there is an error.
     */
    public String getX509CertificateToken() throws SecurityTokenException {
        return securityTokenManager.getX509CertificateToken().toString();
    }
    
    /**
     * Returns the SAML Authentication Token.
     *
     * @return the SAML Authentication Token String.
     * @throws SecurityTokenException if there is an error.
     * @throws SAMLException if there is an error.
     */
    public String  getSAMLAuthenticationToken(String senderIdentity)
    throws SecurityTokenException, SAMLException {
        NameIdentifier ni = new NameIdentifier(XMLUtils.toDOMDocument(
                senderIdentity,SecurityTokenManager.debug).getDocumentElement());
        SecurityAssertion assertion =
                securityTokenManager.getSAMLAuthenticationToken(ni);
        return assertion.toString(true, true);
    }
    
    /**
     * Returns the SAML Authorization Token.
     *
     * @param senderIdentity the identity of the sender.
     * @param invocatorSession the session identifier 
     * @param resourceID the resource Identifier.
     * @param encryptedID boolean value to determine if the identifier
     *        is encrypted.
     * @param includeAuthN boolean value to deteremine if the authentication
     *        information should be included.
     * @param includeResourceAccessStatement if true, a
     *        <code>ResourceAccessStatement</code> will be included in the
     *        Assertion (for <code>AuthorizeRequester</code> directive). If
     *        false, a <code>SessionContextStatement</code> will be included i
     *        the Assertion (for <code>AuthenticationSessionContext</code>
     *        directive). In the case when both <code>AuthorizeRequester</code
     *        and <code>AuthenticationSessionContext</code> directive need to be
     *        handled, use "true" as parameter here since the
     *        <code>SessionContext</code> will always be included in the
     *        <code>ResourceAccessStatement</code>.
     * @param recipientProviderID recipient's provider ID.
     * @return the SAML Authentication Token String.
     * @throws SecurityTokenException if there is an error.
     * @throws SAMLException if there is an error.
     */
    
    public String  getSAMLAuthorizationToken(String senderIdentity,
            String invocatorSession,String resourceID,boolean encryptedID,
            boolean includeAuthN,boolean includeResourceAccessStatement,
            String recipientProviderID)
            throws SecurityTokenException, SAMLException {
        NameIdentifier ni = new NameIdentifier(XMLUtils.toDOMDocument(
                senderIdentity,SecurityTokenManager.debug).getDocumentElement());
        SessionContext sc = new SessionContext(XMLUtils.
                toDOMDocument(invocatorSession,SecurityTokenManager.debug).
                getDocumentElement());
        SecurityAssertion assertion = null;
        if (encryptedID) {
            // TODO
        } else {
            assertion = securityTokenManager.getSAMLAuthorizationToken(
                    ni, sc, resourceID, includeAuthN,
                    includeResourceAccessStatement, recipientProviderID);
        }
        return assertion.toString(true, true);
    }

    /**
     * Returns the <code>X509Certificate</code>.
     */
    private X509Certificate getX509Certificate(String certString) {
        X509Certificate cert = null;
        
        try {
            
            StringBuffer xml = new StringBuffer(100);
            xml.append(SAMLConstants.BEGIN_CERT);
            xml.append(certString);
            xml.append(SAMLConstants.END_CERT);
            
            byte[] barr = null;
            barr = (xml.toString()).getBytes();
            
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(barr);
            
            while (bais.available() > 0) {
                cert = (java.security.cert.X509Certificate)
                cf.generateCertificate(bais);
            }
        } catch (Exception e) {
            SecurityTokenManager.debug.error(
                    "getX509Certificate Exception: ", e);
        }
        return cert;
    }
}
