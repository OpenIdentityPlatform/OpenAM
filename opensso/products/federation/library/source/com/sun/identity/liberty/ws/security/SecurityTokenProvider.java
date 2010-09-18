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
 * $Id: SecurityTokenProvider.java,v 1.3 2008/06/25 05:47:21 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.security;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.disco.EncryptedResourceID;

import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;

import java.security.cert.X509Certificate;

/**
 * The class <code>SecurityTokenProvider</code> is a provider interface 
 * for managing <code>WSS</code> security tokens.
 *
 * @supported.all.api
 */

public interface SecurityTokenProvider {

    /**
     * Initializes the <code>SecurityTokenProvider</code>.
     *
     * @param credential The credential of the caller used
     *        to see if access to this security token provider is allowed.
     * @param sigManager instance of XML digital
     *        signature manager class, used for accessing the certificate
     *        data store and digital signing of the assertion.
     * @throws SecurityTokenException if the caller does not have
     *         privilege to access the security authority manager.
     */
    public void initialize(java.lang.Object credential,
        com.sun.identity.saml.xmlsig.XMLSignatureManager sigManager)
        throws SecurityTokenException;

    /**
     * Sets the alias of the certificate used for issuing <code>WSS</code>
     * token, i.e. <code>WSS</code>  <code>X509</code> Token, <code>WSS</code>
     * SAML Token. If the <code>certAlias</code> is never set, a default
     * certificate will be used for issuing <code>WSS</code> tokens.
     *
     * @param certAlias String alias name for the certificate
     * @throws SecurityTokenException if certificate for the
     *            <code>certAlias</code> could not be found in key store.
     */
    public void setCertAlias(java.lang.String certAlias) 
        throws SecurityTokenException;

    /**
     * Sets the  certificate used for issuing <code>WSS</code> token, i.e.
     * <code>WSS X509</code> Token, <code>WSS</code> SAML Token.
     * If the certificate is never set, a default certificate will
     * be used for issuing <code>WSS</code> tokens.
     *
     * @param cert <code>X509Certificate</code> object.
     * @throws SecurityTokenException if the certificate could not be set.
     */
    public void setCertificate(X509Certificate cert)
        throws SecurityTokenException;

    /**
     * Gets the <code>X509</code> certificate Token.
     *
     * @return <code>X509</code> certificate Token.
     * @throws SecurityTokenException if the token could not be 
     *        obtained.
     */
    public BinarySecurityToken getX509CertificateToken() 
        throws SecurityTokenException;

    /**
     * Creates a SAML Assertion for message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @return Assertion which contains an <code>AuthenticationStatement</code>.
     * @throws SecurityTokenException if the assertion could not be
     *        obtained.
     * @throws SAMLException
     */
    public SecurityAssertion getSAMLAuthenticationToken(
				NameIdentifier senderIdentity)
	throws SecurityTokenException, SAMLException;

    /**
     * Creates a SAML Assertion for message authorization, the assertion could
     * optionally contain an <code>AuthenticationStatement</code> which will be
     * used for message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession <code>SessionContext</code> of  the invocation
     *        identity, it is normally obtained by the credential reference in
     *        the SAML <code>AttributeDesignator<code> for discovery resource
     *        offering which is part of the liberty <code>ID-FF</code>
     *        <code>AuthenResponse</code>.
     * @param resourceID id for the resource to be accessed.
     * @param includeAuthN if true, include an
     *        <code>AutheticationStatement</code> in
     *        the Assertion which will be used for message
     *        authentication. if false, no <code>AuthenticationStatement</code>
     *        will be included.
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
     * @return <code>SecurityAssertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained
     * @throws SAMLException
     */
    public SecurityAssertion getSAMLAuthorizationToken(
				NameIdentifier senderIdentity,
                                SessionContext invocatorSession,
                                String resourceID,
                                boolean includeAuthN,
                                boolean includeResourceAccessStatement,
                                String recipientProviderID)
	throws SecurityTokenException, SAMLException;

    /**
     * Creates a SAML Assertion for message authorization, the assertion could
     * optionally contain an <code>AuthenticationStatement</code> which will be
     * used for message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession <code>SessionContext</code> of the invocation
     *        identity, it is normally obtained by the credential reference in
     *        the SAML <code>AttributeDesignator</code> for discovery resource
     *        offering which is part of the liberty <code>ID-FF</code>
     *        <code>AuthenResponse</code>.
     * @param encResourceID Encrypted ID for the resource to be accessed.
     * @param includeAuthN if true, include an
     *        <code>AutheticationStatement</code> in the Assertion which will be
     *        used for message authentication. if false, no
     *        <code>AuthenticationStatement</code> will be included.
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
     * @return <code>SecurityAssertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained
     */
    public SecurityAssertion getSAMLAuthorizationToken(
				NameIdentifier senderIdentity,
                                SessionContext invocatorSession,
                                EncryptedResourceID encResourceID,
                                boolean includeAuthN,
                                boolean includeResourceAccessStatement,
                                String recipientProviderID)
	throws SecurityTokenException;


    /**
     * Creates a SAML assertion. The <code>confirmationMethod</code> will be
     * set to <code>urn:oasis:names:tc:SAML:1.0:cm:bearer</code>.
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession <code>SessionContext</code> of the invocation
     *        identity, it is normally obtained by the credential reference in
     *        the SAML <code>AttributeDesignator</code> for discovery resource
     *        offering which is part of the liberty <code>ID-FF</code>
     *        <code>AuthenResponse</code>.
     * @param resourceID id for the resource to be accessed.
     * @param includeAuthN if true, include an
     *        <code>AutheticationStatement</code> in the Assertion which will
     *        be used for message authentication. if false, no
     *	      <code>AuthenticationStatement</code> will be included.
     * @param includeResourceAccessStatement if true, a
     *        <code>ResourceAccessStatement</code> will be included in the
     *        Assertion (for <code>AuthorizeRequester</code> directive). If
     *        false, a <code>SessionContextStatement</code> will be included in
     *        the Assertion (for <code>AuthenticationSessionContext</code>
     *        directive). In the case when both <code>AuthorizeRequester</code>
     *        and <code>AuthenticationSessionContext</code> directive need to be
     *	      handled, use "true" as parameter here since the
     *        <code>SessionContext</code> will always be included in the
     *	      <code>ResourceAccessStatement</code>.
     * @param recipientProviderID recipient's provider ID.
     * @return <code>SecurityAssertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained
     * @throws SAMLException if the assertion could not be obtained
     */
    public SecurityAssertion getSAMLBearerToken(
				NameIdentifier senderIdentity,
                                SessionContext invocatorSession,
                                String resourceID,
                                boolean includeAuthN,
                                boolean includeResourceAccessStatement,
                                String recipientProviderID)
	throws SecurityTokenException, SAMLException;


    /**
     * Creates a SAML assertion. The <code>confirmationMethod</code> will be
     * set to <code>urn:oasis:names:tc:SAML:1.0:cm:bearer</code>.
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession <code>SessionContext</code> of the invocation
     *        identity, it is normally obtained by the credential reference in
     *        the SAML <code>AttributeDesignator</code> for discovery resource
     *        offering which is part of the liberty <code>ID-FF</code>
     *        <code>AuthenResponse</code>.
     * @param encResourceID Encrypted ID for the resource to be accessed.
     * @param includeAuthN if true, include an
     *        <code>AutheticationStatement</code> in the Assertion which will
     *        be used for message authentication. if false, no
     *        <code>AuthenticationStatement</code> will be included.
     * @param includeResourceAccessStatement if true, a
     *        <code>ResourceAccessStatement</code> will be included in the
     *        Assertion (for <code>AuthorizeRequester</code> directive). If
     *        false, a <code>SessionContextStatement</code> will be included
     *        in the Assertion (for <code>AuthenticationSessionContext</code>
     *        directive). In the case when both <code>AuthorizeRequester</code>
     *        and <code>AuthenticationSessionContext/code> directive need to be
     *	      handled, use "true" as parameter here since the
     *	      <code>SessionContext</code> will always be included in the
     *	      <code>ResourceAccessStatement</code>.
     * @param recipientProviderID recipient's provider ID.
     * @return <code>SecurityAssertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained
     */
    public SecurityAssertion getSAMLBearerToken(
				NameIdentifier senderIdentity,
                                SessionContext invocatorSession,
                                EncryptedResourceID encResourceID,
                                boolean includeAuthN,
                                boolean includeResourceAccessStatement,
                                String recipientProviderID)
	throws SecurityTokenException;

}
