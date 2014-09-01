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
 * $Id: SecurityTokenManagerIF.java,v 1.3 2008/06/25 05:47:21 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.security;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.disco.EncryptedResourceID;

import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.security.cert.X509Certificate;

import java.util.List;
import java.util.Set;

/**
 * This is the JAX-RPC interface for making SecurityTokenManager remotable.
 */
public interface SecurityTokenManagerIF extends Remote {
    
    /** 
     * Initializes the SecurityTokenManager.
     *
     * @param sessionID the session id.
     * @throws SecurityTokenException if there is an error.
     * @throws RemoteException if there is an error.
     */
    public void initialization(String sessionID)
	throws SecurityTokenException, RemoteException;
    
    /**
     * Checks if the service is available locally.
     *
     * @throws RemoteException if there is an error.
     */
    public void checkForLocal() throws RemoteException;

    /**
     * Sets the Certificate.
     *
     * @param cert the Certificate String.
     * @param alias if true then Certificate Alias will be set.
     * @throws SecurityTokenException if there is an error.
     * @throws RemoteException if there is an error.
     */
    public void setCertificate(String cert, boolean alias)
	throws SecurityTokenException, RemoteException;

    /**
     * Returns the Certificate Token.
     *
     * @return the Certification Token String.
     * @throws SecurityTokenException if there is an error.
     * @throws RemoteException if there is an error.
     */
    public String getX509CertificateToken()
	throws SecurityTokenException, RemoteException;

    /**
     * Returns the SAML Authentication Token.
     *
     * @param senderIdentity the sender's identity.
     * @return the SAML Authentication Token String.
     * @throws SecurityTokenException if there is an error.
     * @throws SAMLException if there is an error.
     * @throws RemoteException if there is an error.
     */
    public String getSAMLAuthenticationToken(
			String senderIdentity)
	throws SecurityTokenException, SAMLException, RemoteException;
    
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
     *        false, a <code>SessionContextStatement</code> will be included in
     *        the Assertion (for <code>AuthenticationSessionContext</code>
     *        directive). In the case when both <code>AuthorizeRequester</code>
     *        and <code>AuthenticationSessionContext</code> directive need to be
     *        handled, use "true" as parameter here since the
     *        <code>SessionContext</code> will always be included in the
     *        <code>ResourceAccessStatement</code>.
     * @param recipientProviderID recipient's provider ID.
     * @return the SAML Authorization Token String.
     * @throws SecurityTokenException if there is an error.
     * @throws SAMLException if there is an error.
     * @throws RemoteException if there is an error.
     */

    public String getSAMLAuthorizationToken(String senderIdentity,
			String invocatorSession,String resourceID,
			boolean encryptedID,boolean includeAuthN,
			boolean includeResourceAccessStatement,
                        String recipientProviderID)
	throws SecurityTokenException, SAMLException, RemoteException;
}
