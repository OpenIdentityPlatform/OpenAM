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
 * $Id: SecurityTokenManager.java,v 1.4 2008/08/06 17:28:11 exu Exp $
 *
 */


package com.sun.identity.liberty.ws.security;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.disco.EncryptedResourceID;

import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;

import java.util.ResourceBundle;

import java.security.cert.X509Certificate;

/**
 * The class <code>SecurityTokenManager</code> is a final class that
 * provides interfaces to manage Web Service Security (WSS) Tokens.
 *
 * @supported.api
 */

public final class SecurityTokenManager {
    
    //TODO : make those public methods remotable
    private static String TOKEN_PROVIDER =
            "com.sun.identity.liberty.ws.security.TokenProviderImpl";
    private static String providerClass = SystemPropertiesManager.get(TOKEN_PROVIDER);
    static ResourceBundle bundle = Locale.getInstallResourceBundle(
            "libLibertySecurity");
    static Debug debug = Debug.getInstance("libIDWSF");
    private SecurityTokenProvider provider = null;
    
    /**
     * Default constructor
     */
    private SecurityTokenManager() {}
    
    /**
     * Returns the security token manager instance, the default
     * <code>XMLSignatureManager</code> instance will be used for signing
     * and accessing the data store.
     *
     * @param credential The credential of the caller used
     *   to see if access to this security token manager is allowed.
     * @throws SecurityTokenException if unable to access the
     *         the security token manager.
     * @supported.api
     */
    public SecurityTokenManager(java.lang.Object credential)
                                throws SecurityTokenException {
        
        // no null checking for credential since provider may allow it
        // check for null
        if (providerClass == null || providerClass.trim().length() == 0) {
            debug.error("Con: Security Token Provider class is not defined");
            throw new SecurityTokenException(bundle.getString("noProvider"));
        }
        
        // get provider class instance
        try {
            provider = (SecurityTokenProvider)
            Class.forName(providerClass).newInstance();
        } catch (Exception e) {
            debug.message("Con: Unable to get instance of Token Provider", e);
            throw new SecurityTokenException(
                    bundle.getString("noProviderInstance"));
        }
        
        // get default XML signature manager class instance
        XMLSignatureManager manager = null;
        try {
            manager = XMLSignatureManager.getInstance();
        } catch (Exception e) {
            // unerline provider implementation might not need this, return null
            // leave the check to the implementor
            debug.message("Con: Unable to get instance of XMLSigManager", e);
        }
        
        // initialize security token provider
        provider.initialize(credential, manager);
    }
    
    
    /**
     * Gets the security token manager instance, this constructor is only
     * applicable when the client is running in the same JVM as server.
     *
     * @param credential The credential of the caller used
     *   to see if access to this security token manager is allowed.
     * @param signatureManager instance of XML digital
     *         signature manager class, used for accessing the certificate
     *         datastore and digital signing of the assertion.
     * @throws SecurityTokenException if unable to access the
     *         the security token manager.
     */
    public SecurityTokenManager(java.lang.Object credential,
            XMLSignatureManager signatureManager)
            throws SecurityTokenException {
        
        // no null checking for credential since provider may allow it
        // no null checking for signatureManager since provider may allow it
        // check for null
        if (providerClass == null || providerClass.trim().length() == 0) {
            debug.error("Con2: Security Token Provider class is not defined");
            throw new SecurityTokenException(bundle.getString("noProvider"));
        }
        
        // get provider class instance
        try {
            provider = (SecurityTokenProvider)
            Class.forName(providerClass).newInstance();
        } catch (Exception e) {
            debug.message("Con2: Unable to get instance of Token Provider", e);
            throw new SecurityTokenException(
                    bundle.getString("noProviderInstance"));
        }
        
        // initialize security token provider
        provider.initialize(credential, signatureManager);
    }
    
    /**
     * Sets the alias of the certificate used for issuing <code>WSS</code>
     * token, i.e. <code>WSS</code> <code>X509</code> Token, <code>WSS</code>
     * SAML Token. If the <code>certAlias</code> is never set, a default
     * certificate will be used for issuing <code>WSS</code> tokens.
     *
     * @param certAlias String alias name for the certificate.
     * @throws SecurityTokenException if certificate for the
     *            <code>certAlias</code> could not be found in key store.
     * @supported.api
     */
    public void setCertAlias(java.lang.String certAlias)
    throws SecurityTokenException {
        provider.setCertAlias(certAlias);
    }
    
    /**
     * Sets the  certificate used for issuing <code>WSS</code> token, i.e.
     * <code>WSS</code> <code>X509</code> Token, <code>WSS</code> SAML Token.
     * If the certificate is never set, a default certificate will
     * be used for issuing <code>WSS</code> tokens
     *
     * @param cert <code>X509</code> certificate
     * @throws SecurityTokenException if could not set Certificate.
     */
    public void setCertificate(X509Certificate cert) 
                                throws SecurityTokenException {
        provider.setCertificate(cert);
    }
    
    /**
     * Returns the <code>X509</code> certificate Token.
     *
     * @return <code>X509</code> certificate Token.
     * @throws SecurityTokenException if the binary security token could
     * not be obtained.
     * @supported.api
     */
    public BinarySecurityToken getX509CertificateToken()
    throws SecurityTokenException {
        return provider.getX509CertificateToken();
    }
    
    /**
     * Creates a SAML Assertion for message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @return Assertion which contains an AuthenticationStatement.
     * @throws SecurityTokenException if the assertion could not be
     * obtained.
     */
    public SecurityAssertion getSAMLAuthenticationToken(
            NameIdentifier senderIdentity)
            throws SecurityTokenException, SAMLException {
        return provider.getSAMLAuthenticationToken(senderIdentity);
    }
    
    
    /**
     * Creates a SAML Assertion for message authorization, the assertion could
     * optionally contain an AuthenticationStatement which will be used for
     *  message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession SessionContext of  the invocation identity, it
     *        is normally obtained by the credential reference in the SAML
     *        AttributeDesignator for discovery resource offering which is part 
     *        of the liberty ID-FF AuthenResponse.
     * @param resourceID id for the resource to be accessed.
     * @param includeAuthN if true include an AutheticationStatement in
     *        the Assertion which will be used for message authentication.
     * @param includeResourceAccessStatement if true, a ResourceAccessStatement
     *        will be included in the Assertion (for AuthorizeRequester
     *        directive). If false, a SessionContextStatement will be included
     *        in the Assertion (for AuthenticationSessionContext directive). 
     *        In the case when both AuthorizeRequester and 
     *        AuthenticationSessionContext directive need to be handled, use
     *        "true" as parameter here since the SessionContext will always be 
     *        included in the ResourceAccessStatement.
     * @param recipientProviderID recipient's provider ID.
     * @return the <code>SecurityAssertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained.
     */
    public SecurityAssertion getSAMLAuthorizationToken(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            String resourceID,
            boolean includeAuthN,
            boolean includeResourceAccessStatement,
            String recipientProviderID)
            throws SecurityTokenException, SAMLException {
        return provider.getSAMLAuthorizationToken(senderIdentity,
                invocatorSession,
                resourceID,
                includeAuthN,
                includeResourceAccessStatement,
                recipientProviderID);
    }
    
    /**
     * Creates a SAML Assertion for message authorization, the assertion could
     * optionally contain an AuthenticationStatement which will be used for
     * message authentication.
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession SessionContext of  the invocation identity, it
     *        is normally obtained by the credential reference in the SAML
     *        AttributeDesignator for discovery resource offering which is part 
     *        of the liberty ID-FF AuthenResponse.
     * @param encResourceID Encrypted ID for the resource to be accessed.
     * @param includeAuthN if true, include an AutheticationStatement in the
     *        Assertion which will be used for message authentication. 
     * @param includeResourceAccessStatement if true, a ResourceAccessStatement
     *        will be included in the Assertion (for AuthorizeRequester 
     *        directive). If false, a SessionContextStatement will be included 
     *        in the Assertion (for AuthenticationSessionContext directive).
     *        In the case when both AuthorizeRequester and 
     *        AuthenticationSessionContext directive need to be handled, use
     *        "true" as parameter here since the SessionContext will always be 
     *        included in the ResourceAccessStatement.
     * @param recipientProviderID recipient's provider ID.
     * @return the <code>SecurityAssertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained.
     */
    public SecurityAssertion getSAMLAuthorizationToken(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            EncryptedResourceID encResourceID,
            boolean includeAuthN,
            boolean includeResourceAccessStatement,
            String recipientProviderID)
            throws SecurityTokenException {
        return provider.getSAMLAuthorizationToken(senderIdentity,
                invocatorSession,
                encResourceID,
                includeAuthN,
                includeResourceAccessStatement,
                recipientProviderID);
    }
    
    
    /**
     * Creates a SAML assertion. The confirmationMethod will be set to
     * "urn:oasis:names:tc:SAML:1.0:cm:bearer".
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession SessionContext of  the invocation identity, it
     *			is normally obtained by the credential reference in the
     *			SAML AttributeDesignator for discovery resource
     *                  offering which is part of the liberty ID-FF
     *                  AuthenResponse.
     * @param resourceID id for the resource to be accessed.
     * @param includeAuthN if true, include an AutheticationStatement in the
     *			Assertion which will be used for message
     *                  authentication.	if false, no AuthenticationStatement
     *                  will be included.
     * @param includeResourceAccessStatement if true, a ResourceAccessStatement
     *			will be included in the Assertion (for
     *			AuthorizeRequester directive). If false, a
     *			SessionContextStatement will be included in the
     *			Assertion (for AuthenticationSessionContext directive).
     *			In the case when both AuthorizeRequester and
     *			AuthenticationSessionContext directive need to be
     *			handled, use "true" as parameter here since the
     *			SessionContext will always be included in the
     *			ResourceAccessStatement.
     * @param recipientProviderID recipient's provider ID.
     * @return the <code>SecurityAssertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained
     */
    public SecurityAssertion getSAMLBearerToken(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            String resourceID,
            boolean includeAuthN,
            boolean includeResourceAccessStatement,
            String recipientProviderID)
            throws SecurityTokenException, SAMLException {
        return provider.getSAMLBearerToken(senderIdentity,
                invocatorSession,
                resourceID,
                includeAuthN,
                includeResourceAccessStatement,
                recipientProviderID);
    }
    
    
    /**
     * Creates a SAML assertion. The confirmationMethod will be set to
     * "urn:oasis:names:tc:SAML:1.0:cm:bearer".
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession SessionContext of  the invocation identity, it
     *			is normally obtained by the credential reference in the
     *			SAML AttributeDesignator for discovery resource
     *                  offering which is part of the liberty ID-FF
     *                  AuthenResponse.
     * @param encResourceID Encrypted ID for the resource to be accessed.
     * @param includeAuthN if true, include an AutheticationStatement in the
     *			Assertion which will be used for message
     *                  authentication.	if false, no AuthenticationStatement
     *                  will be included.
     * @param includeResourceAccessStatement if true, a ResourceAccessStatement
     *			will be included in the Assertion (for
     *			AuthorizeRequester directive). If false, a
     *			SessionContextStatement will be included in the
     *			Assertion (for AuthenticationSessionContext directive).
     *			In the case when both AuthorizeRequester and
     *			AuthenticationSessionContext directive need to be
     *			handled, use "true" as parameter here since the
     *			SessionContext will always be included in the
     *			ResourceAccessStatement.
     * @param recipientProviderID recipient's provider ID.
     * @return the <code>SecurityAssertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained
     * @supported.api
     */
    public SecurityAssertion getSAMLBearerToken(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            EncryptedResourceID encResourceID,
            boolean includeAuthN,
            boolean includeResourceAccessStatement,
            String recipientProviderID)
            throws SecurityTokenException {
        return provider.getSAMLBearerToken(senderIdentity,
                invocatorSession,
                encResourceID,
                includeAuthN,
                includeResourceAccessStatement,
                recipientProviderID);
    }   
}
