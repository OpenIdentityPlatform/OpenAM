/*
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
 * $Id: LibSecurityTokenProvider.java,v 1.3 2008/08/06 17:28:11 exu Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */

package com.sun.identity.liberty.ws.security;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.disco.common.DiscoServiceManager;
import com.sun.identity.liberty.ws.disco.EncryptedResourceID;
import com.sun.identity.plugin.session.SessionException;

import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.SessionManager;

import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.assertion.AudienceRestrictionCondition;
import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.Conditions;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLServiceManager;

import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.saml.xmlsig.KeyProvider;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.math.BigInteger;
import java.security.interfaces.DSAParams;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.cert.X509Certificate;
import java.security.PublicKey;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * The class <code>LibSecurityTokenProvider</code> is an default
 * implementation for <code>SecurityTokenProvider</code>. 
 */

public class LibSecurityTokenProvider implements SecurityTokenProvider {
    
    protected XMLSignatureManager sigManager = null;
    protected KeyProvider keystore = null;
    private Object ssoToken = null;
    private String certAlias = null;
    private X509Certificate wssCert = null;
    // default certificate for the WSC
    private static String DEFAULT_CERT_ALIAS_KEY =
        "com.sun.identity.liberty.ws.wsc.certalias";
    private static String DEFAULT_CERT_ALIAS_VALUE =
        SystemPropertiesManager.get(DEFAULT_CERT_ALIAS_KEY);
    // cert alias for trusted authority, this is used for SAML token signing
    private static String DEFAULT_TA_CERT_ALIAS_KEY =
        "com.sun.identity.liberty.ws.ta.certalias";
    private static String DEFAULT_TA_CERT_ALIAS_VALUE =
        SystemPropertiesManager.get(DEFAULT_TA_CERT_ALIAS_KEY);
    private static String KEYINFO_TYPE =
        "com.sun.identity.liberty.ws.security.keyinfotype";
    private static final String AUTH_INSTANT = "authInstant";
    private static String keyInfoType = 
        SystemPropertiesManager.get(KEYINFO_TYPE);
    private static Debug debug = Debug.getInstance("libIDWSF");
    private static ResourceBundle bundle = Locale.getInstallResourceBundle(
        "fmLibertySecurity");
    protected String authTime = "";
    protected String authType = "";
    protected static SecurityAttributePlugin attributePlugin = null;
    /**
     * Key name for the webservices security attribute mapper.
     */
    private static final String WS_ATTRIBUTE_PLUGIN =
        "com.sun.identity.liberty.ws.attributeplugin";
    
    /**
     * Initializes the <code>LibSecurityTokenProvider</code>.
     *
     * @param credential  The credential of the caller used to see if
     *                    access to this security token provider is allowed
     * @param sigManager XMLSignatureManager  instance of XML digital
     *        signature manager class, used for accessing the certificate
     *        datastore and digital signing of the assertion.
     * @throws SecurityTokenException if the caller does not have
     *         privilege to access the security authority manager
     */
    public void initialize(Object credential,XMLSignatureManager sigManager)
    throws SecurityTokenException {
        // check null for signature manager
        debug.message("LibSecurityTokenProvider.initialize");
        if (sigManager == null) {
            debug.error("AMP: nulll signature manager");
            throw new SecurityTokenException(
                bundle.getString("nullXMLSigManager"));
        }
        
        keystore = sigManager.getKeyProvider();
        
        // check valid Session 
        try {
            ssoToken = credential;
            SessionProvider provider = SessionManager.getProvider();
            if (!provider.isValid(ssoToken)) {
                throw new SecurityTokenException(
                    bundle.getString("invalidSSOToken"));
            }
            String[] tmp = provider.getProperty(ssoToken, 
                SessionProvider.AUTH_METHOD);
            if ((tmp != null) && (tmp.length != 0)) {
                authType = tmp[0];
            }
            tmp = provider.getProperty(ssoToken, 
                SessionProvider.AUTH_INSTANT);
            if ((tmp != null) && (tmp.length != 0)) {
                authTime = tmp[0];
            }
        } catch (SessionException e) {
            debug.error("AMP: invalid SSO Token", e);
            throw new SecurityTokenException(
                bundle.getString("invalidSSOToken"));
        }
        //
        // TODO : privilege checking for the ssoToken, how??
        // maybe a relation between the principal of the SSO and the
        // certificate? super admin shall be allowed without checking
        // still TBD
        //
        this.sigManager = sigManager;
    }
    
    /**
     * Sets the alias of the certificate used for issuing WSS token, i.e.
     * WSS X509 Token, WSS SAML Token.
     * If the certAlias is never set, a default certificate will
     * be used for issuing WSS tokens
     *
     * @param certAlias String alias name for the certificate
     */
    public void setCertAlias(java.lang.String certAlias)
    throws SecurityTokenException {
        if (debug.messageEnabled()) {
            debug.message("AMP : certalias=" + certAlias);
        }
        this.certAlias = certAlias;
        wssCert = this.getX509Certificate();
    }
    
    
    /**
     * Sets the  certificate used for issuing WSS token, i.e.
     * WSS X509 Token, WSS SAML Token.
     * If the certificate is never set, a default certificate will
     * be used for issuing WSS tokens
     *
     * @param cert X509 certificate
     * @throws SecurityTokenException if could not get cert alias from
     * corresponding Certificate.
     */
    public void setCertificate(X509Certificate cert)
    throws SecurityTokenException {
        
        this.certAlias = keystore.getCertificateAlias(cert);
        if (debug.messageEnabled()) {
            debug.message("AMP : certalias=" + certAlias);
        }
        
        if (this.certAlias == null) {
            debug.error("AMP: no cert found");
            throw new SecurityTokenException(bundle.getString("noCertAlias"));
        }
        wssCert = cert;
    }
    
    /**
     * Gets X509 certificate from key store based on the certAlias
     *
     * @return the <code>X509Certificate<code> in the keystore.
     * @throws SecurityTokenException if there is an error retrieving
     *         the certificate.
     */
    private X509Certificate getX509Certificate()
    throws SecurityTokenException {
        if (certAlias == null) {
            // retrieve default certAlias from properties
            if (DEFAULT_CERT_ALIAS_VALUE == null ||
                    DEFAULT_CERT_ALIAS_VALUE.trim().length() == 0) {
                debug.error("AMP: no cert found");
                throw new SecurityTokenException(
                    bundle.getString("noCertAlias"));
            }
            certAlias = DEFAULT_CERT_ALIAS_VALUE;
        }
        // retrieve the cert from the keystore
        X509Certificate cert = keystore.getX509Certificate(certAlias);
        if (cert == null) {
            // the cert does not exists in the keystore
            debug.error("AMP : no cert found in store");
            throw new SecurityTokenException(
                bundle.getString("noMatchingCert"));
        }
        return cert;
    }
    
    /**
     * Gets the X509 certificate Token
     *
     * @return the BinarySecurityToken object.
     * @throws SecurityTokenException if the token could not be obtained .
     */
    public BinarySecurityToken getX509CertificateToken()
    throws SecurityTokenException {
        // get X509Certificate
        if (wssCert == null) {
            wssCert = this.getX509Certificate();
        }
        // return base 64 encoded binary & X509v3
        String value = null;
        try {
            value = Base64.encode(wssCert.getEncoded());
            return new BinarySecurityToken(value,
                    BinarySecurityToken.X509V3,
                    BinarySecurityToken.BASE64BINARY);
        } catch (Exception e) {
            debug.error("getX509Token", e);
            throw new SecurityTokenException(e.getMessage());
        }
    }
    
    /**
     * Creates a SAML Assertion for message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @return Assertion which contains an AuthenticationStatement
     * @throws SecurityTokenException if the assertion could not be obtained
     */
    public SecurityAssertion getSAMLAuthenticationToken(
            NameIdentifier senderIdentity) throws SecurityTokenException {
        return getSAMLToken(senderIdentity, null, null, true, false, null,
            false);
    }
    
    /**
     * Creates a SAML Assertion for message authorization, the assertion could
     * optionally contain an AuthenticationStatement which will be used for
     * message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession SessionContext of  the invocation identity, it
     *            	is normally obtained by the credential reference in
     *			the SAML AttributeDesignator for discovery resource
     *			offering which is part of the liberty ID-FF
     *			AuthenResponse.
     * @param resourceID id for the resource to be accessed.
     * @param includeAuthN if true, include an AutheticationStatement in
     *			the Assertion which will be used for message
     *			authentication. if false, no AuthenticationStatement
     *			will be included.
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
     * @return the <code>Assertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained.
     */
    public SecurityAssertion getSAMLAuthorizationToken(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            String resourceID,
            boolean includeAuthN,
            boolean includeResourceAccessStatement,
            String recipientProviderID)
            throws SecurityTokenException {
        return getSAMLToken(senderIdentity, invocatorSession, resourceID,
                includeAuthN,includeResourceAccessStatement,
                recipientProviderID, false);
    }
    
    /**
     * Creates a SAML Assertion for message authorization, the assertion could
     * optionally contain an AuthenticationStatement which will be used for
     * message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession SessionContext of  the invocation identity, it
     *			is normally obtained by the credential reference in the
     *			SAML AttributeDesignator for discovery resource offering
     *			which is part of the liberty ID-FF AuthenResponse.
     * @param encResourceID Encrypted ID for the resource to be accessed.
     * @param includeAuthN if true, include an AutheticationStatement in the
     *			Assertion which will be used for message authentication.
     *			if false, no AuthenticationStatement will be included.
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
     * @return the <code>Assertion</code> object
     * @throws SecurityTokenException if the assertion could not be obtained
     */
    public SecurityAssertion getSAMLAuthorizationToken(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            EncryptedResourceID encResourceID,
            boolean includeAuthN,
            boolean includeResourceAccessStatement,
            String recipientProviderID)
            throws SecurityTokenException {
        return getSAMLToken(senderIdentity, invocatorSession, encResourceID,
                includeAuthN, includeResourceAccessStatement,
                recipientProviderID, false);
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
     * @return the <code>SecurityAssertion</code>
     * @throws SecurityTokenException if the assertion could not be obtained
     */
    public SecurityAssertion getSAMLBearerToken(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            String resourceID,
            boolean includeAuthN,
            boolean includeResourceAccessStatement,
            String recipientProviderID)
            throws SecurityTokenException {
        return getSAMLToken(senderIdentity, invocatorSession, resourceID,
                includeAuthN, includeResourceAccessStatement,
                recipientProviderID, true);
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
     * @return the <code>Assertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained
     */
    public SecurityAssertion getSAMLBearerToken(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            EncryptedResourceID encResourceID,
            boolean includeAuthN,
            boolean includeResourceAccessStatement,
            String recipientProviderID)
            throws SecurityTokenException {
        return getSAMLToken(senderIdentity, invocatorSession, encResourceID,
                includeAuthN,includeResourceAccessStatement,
                recipientProviderID, true);
    }
    
    /**
     * Returns the Security Assertion.
     */
    private SecurityAssertion getSAMLToken(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            Object resourceID,
            boolean includeAuthN,
            boolean includeResourceAccessStatement,
            String recipientProviderID,
            boolean isBear)
            throws SecurityTokenException {
        if (debug.messageEnabled()) {
            debug.message("getSAMLToken: isBear = " + isBear);
        }
        
        if (senderIdentity== null) {
            debug.error(
                "LibSecurityTokenProvider.getSAMLToken:senderIdentity is null");
            throw new SecurityTokenException(
                bundle.getString("nullSenderIdentity"));
        }
        
        
        boolean statementNotFound = true;
        SecurityAssertion assertion = null;
        
        Set statements = new HashSet();
        if (includeAuthN) {
            AuthenticationStatement authStatement =
                    createAuthenticationStatement(senderIdentity, isBear);
            statements.add(authStatement);
            statementNotFound = false;
        }
        if (includeResourceAccessStatement) {
            ResourceAccessStatement ras = createResourceAccessStatement(
                    senderIdentity,
                    invocatorSession,
                    resourceID,
                    isBear);
            statements.add(ras);
            statementNotFound = false;
        } else {
            if (invocatorSession!=null) {
                SessionContextStatement scs = createSessionContextStatement(
                        senderIdentity,
                        invocatorSession,
                        isBear);
                statements.add(scs);
                statementNotFound = false;
            }
        }
        // make sure the statements is not empty
        if (statementNotFound) {
            debug.error("getSAMLAuthorizationToken: SAML statement should " +
                "not be null.");
            throw new SecurityTokenException(
                bundle.getString("nullStatement"));
        }
        
        String issuer = DiscoServiceManager.getDiscoProviderID();
        
        //Check for the attribute statements.
        attributePlugin = getAttributePlugin();
        if(attributePlugin != null) {
            
            List attributes = attributePlugin.getAttributes(
                    senderIdentity, resourceID, issuer);
            
            if(attributes != null && attributes.size() != 0) {
                AttributeStatement attributeStatement =
                     createAttributeStatement(senderIdentity,attributes,isBear);
                if(attributeStatement != null) {
                    statements.add(attributeStatement);
                }
            }
        }


        Date issueInstant = newDate();
        try {
            if (recipientProviderID != null) {
                List audience = new ArrayList();
                audience.add(recipientProviderID);
                AudienceRestrictionCondition arc =
                    new AudienceRestrictionCondition(audience);
                Conditions conditions = new Conditions();
                conditions.addAudienceRestrictionCondition(arc);

                assertion = new SecurityAssertion("", issuer, issueInstant,
                    conditions, statements);
            } else {
                assertion = new SecurityAssertion("", issuer, issueInstant,
                    statements);
            }
            assertion.signXML(DEFAULT_TA_CERT_ALIAS_VALUE);
        } catch (Exception e) {
            debug.error("getSAMLToken.signXML", e);
            throw new SecurityTokenException(
                bundle.getString("nullAssertion"));
        }
        return assertion;
    }
    
    
    /**
     * Creates Authentication Statement for the name identifier.
     */
    private AuthenticationStatement createAuthenticationStatement(
            NameIdentifier senderIdentity, boolean isBearer)
            throws SecurityTokenException {
        AuthenticationStatement authStatement = null;
        
        try {
            String authMethod = SAMLServiceManager.getAuthMethodURI(authType);
            Date authInstant = DateUtils.stringToDate(authTime);
            Subject subject = null;
            SubjectConfirmation subConfirmation = null;
            if (isBearer) {
                subConfirmation = new SubjectConfirmation(
                        SAMLConstants.CONFIRMATION_METHOD_BEARER);
            } else {
                subConfirmation = new SubjectConfirmation(
                        SAMLConstants.CONFIRMATION_METHOD_HOLDEROFKEY);
                subConfirmation.setKeyInfo(createKeyInfo());
                
            }
            subject = new Subject(senderIdentity, subConfirmation);
            authStatement = new AuthenticationStatement(authMethod,
                    authInstant,
                    subject);
        } catch (Exception e) {
            debug.error("createAuthenticationStatement: ", e);
            throw new SecurityTokenException(e.getMessage());
        }
        
        return authStatement;
    }
    
    /**
     * Creates <code>ResourceAccessStatement</code> object.
     */
    private ResourceAccessStatement createResourceAccessStatement(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            Object resourceID,
            boolean isBear) throws SecurityTokenException {
        if (debug.messageEnabled()) {
            debug.message("LibSecurityTokenProvider." +
                "createResourceAccessStatement: resourceID class = " +
                resourceID.getClass() + ", value = " + resourceID);
        }
        
        ResourceAccessStatement ras = null;
        try {
            ProxySubject proxySubject = null;
            Subject      subject = null;
            
            List subjects = createSubjectAndProxySubject(senderIdentity,
                    invocatorSession,
                    isBear);
            subject = (Subject)subjects.get(0);
            if (subjects.size() == 2) {
                proxySubject = (ProxySubject)subjects.get(1);
            }
            
            if (resourceID instanceof String) {
                ras = new ResourceAccessStatement(
                        (String)resourceID,
                        proxySubject,
                        invocatorSession,
                        subject);
            } else {
                ras = new ResourceAccessStatement(
                        (EncryptedResourceID)resourceID,
                        proxySubject,
                        invocatorSession,
                        subject);
            }
            
            if (debug.messageEnabled()) {
                debug.message("LibSecurityTokenProvider." +
                    "createResourceAccessStatement: ras = " + ras);
            }
        } catch (Exception e) {
            debug.error("createResourceAccessStatement: ", e);
            throw new SecurityTokenException(e.getMessage());
        }
        
        return ras;
    }
    
    /**
     * Returns a list of Subjects.
     */
    private List createSubjectAndProxySubject(NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            boolean isBear) throws Exception {
        List returnList = new ArrayList();
        Subject subject = null;
        SubjectConfirmation subConfirmation = null;
        ProxySubject proxySubject = null;
        NameIdentifier sessIdentity = null;
        
        if (invocatorSession != null &&
                !(sessIdentity = invocatorSession.getSessionSubject()
                .getNameIdentifier()).equals(senderIdentity)) {
            
            subConfirmation = new SubjectConfirmation(
                    SAMLConstants.CONFIRMATION_METHOD_SENDERVOUCHES);
            // add proxy subject
            subject = new Subject(sessIdentity, subConfirmation);
            proxySubject = createProxySubject(senderIdentity,
                    isBear);
            returnList.add(subject);
            returnList.add(proxySubject);
        } else {
            if (isBear) {
                subConfirmation = new SubjectConfirmation(
                        SAMLConstants.CONFIRMATION_METHOD_BEARER);
            } else {
                subConfirmation = new SubjectConfirmation(
                        SAMLConstants.CONFIRMATION_METHOD_HOLDEROFKEY);
                subConfirmation.setKeyInfo(createKeyInfo());
            }
            subject = new Subject(senderIdentity, subConfirmation);
            returnList.add(subject);
        }
        return returnList;
        
    }
    
    /**
     * Creates the <code>SessionContextStatement</code> object.
     */
    private SessionContextStatement createSessionContextStatement(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            boolean isBear) throws SecurityTokenException {
        try {
            ProxySubject proxySubject = null;
            Subject      subject = null;
            
            List subjects = createSubjectAndProxySubject(senderIdentity,
                    invocatorSession,
                    isBear);
            subject = (Subject)subjects.get(0);
            if (subjects.size() == 2) {
                proxySubject = (ProxySubject)subjects.get(1);
            }
            
            return new SessionContextStatement(invocatorSession,
                    proxySubject,
                    subject);
        } catch (Exception e) {
            debug.error("createSessionContextStatement: ", e);
            throw new SecurityTokenException(e.getMessage());
        }
    }
    
    /**
     * Creates a <code>ProxySubject</code> object.
     */
    private ProxySubject createProxySubject(
            NameIdentifier senderIdentity,
            boolean isBear)
            throws SecurityTokenException, SAMLException {
        SubjectConfirmation subConfirmation = null;
        if (isBear) {
            subConfirmation = new SubjectConfirmation(
                    SAMLConstants.CONFIRMATION_METHOD_BEARER);
        } else {
            subConfirmation = new SubjectConfirmation(
                    SAMLConstants.CONFIRMATION_METHOD_HOLDEROFKEY);
            subConfirmation.setKeyInfo(createKeyInfo());
        }
        return new ProxySubject(senderIdentity, subConfirmation);
    }
    
    /**
     * Returns the <code>KeyInfo</code> object as a Document Element.
     */
    private Element createKeyInfo() throws SecurityTokenException {
        X509Certificate cert = getX509Certificate();
        Document doc = null;
        try {
            doc = XMLUtils.newDocument();
        } catch (Exception e) {
            debug.error("createKeyInfo: ", e);
            throw new SecurityTokenException(e.getMessage());
        }
        
        String keyNameTextString = null;
        String base64CertString = null;
        
        PublicKey pk = null;
        try {
            pk = cert.getPublicKey();
            keyNameTextString = cert.getSubjectDN().getName();
            base64CertString = Base64.encode(cert.getEncoded());
        } catch (Exception e) {
            debug.error("createKeyInfo: ", e);
            throw new SecurityTokenException(e.getMessage());
        }
        
        Element keyInfo = doc.createElementNS(
                SAMLConstants.XMLSIG_NAMESPACE_URI,
                SAMLConstants.TAG_KEYINFO);
        keyInfo.setAttribute("xmlns", SAMLConstants.XMLSIG_NAMESPACE_URI);
        if ((keyInfoType!=null)&&(keyInfoType.equalsIgnoreCase("certificate"))){
            //put Certificate in KeyInfo
            Element x509Data = doc.createElementNS(
                    SAMLConstants.XMLSIG_NAMESPACE_URI,
                    SAMLConstants.TAG_X509DATA);
            Element x509Certificate = doc.createElementNS(
                    SAMLConstants.XMLSIG_NAMESPACE_URI,
                    SAMLConstants.TAG_X509CERTIFICATE);
            Text certText = doc.createTextNode(base64CertString);
            x509Certificate.appendChild(certText);
            keyInfo.appendChild(x509Data).appendChild(x509Certificate);
        } else { //put public key in keyinfo
            Element keyName = doc.createElementNS(
                    SAMLConstants.XMLSIG_NAMESPACE_URI,
                    SAMLConstants.TAG_KEYNAME);
            Text keyNameText = doc.createTextNode(keyNameTextString);
            
            Element keyvalue = doc.createElementNS(
                    SAMLConstants.XMLSIG_NAMESPACE_URI,
                    SAMLConstants.TAG_KEYVALUE);
            
            if (pk.getAlgorithm().equals("DSA")) {
                DSAPublicKey dsakey = (DSAPublicKey) pk;
                DSAParams dsaParams = dsakey.getParams();
                BigInteger _p = dsaParams.getP();
                BigInteger _q = dsaParams.getQ();
                BigInteger _g = dsaParams.getG();
                BigInteger _y = dsakey.getY();
                
                Element DSAKeyValue = doc.createElementNS(
                        SAMLConstants.XMLSIG_NAMESPACE_URI
                        , "DSAKeyValue");
                Element p = doc.createElementNS(
                        SAMLConstants.XMLSIG_NAMESPACE_URI, "P");
                Text value_p =
                        doc.createTextNode(Base64.encode(_p.toByteArray()));
                p.appendChild(value_p);
                DSAKeyValue.appendChild(p);
                
                Element q = doc.createElementNS(
                        SAMLConstants.XMLSIG_NAMESPACE_URI, "Q");
                Text value_q =
                        doc.createTextNode(Base64.encode(_q.toByteArray()));
                q.appendChild(value_q);
                DSAKeyValue.appendChild(q);
                
                Element g = doc.createElementNS(
                        SAMLConstants.XMLSIG_NAMESPACE_URI, "G");
                Text value_g =
                        doc.createTextNode(Base64.encode(_g.toByteArray()));
                g.appendChild(value_g);
                DSAKeyValue.appendChild(g);
                
                Element y = doc.createElementNS(
                        SAMLConstants.XMLSIG_NAMESPACE_URI, "Y");
                Text value_y =
                        doc.createTextNode(Base64.encode(_y.toByteArray()));
                y.appendChild(value_y);
                DSAKeyValue.appendChild(y);
                keyvalue.appendChild(DSAKeyValue);
                
            } else { // It is RSA
                RSAPublicKey rsakey = (RSAPublicKey) pk;
                BigInteger exponent = rsakey.getPublicExponent();
                BigInteger modulus  = rsakey.getModulus();
                Element RSAKeyValue = doc.createElementNS(
                        SAMLConstants.XMLSIG_NAMESPACE_URI
                        , "RSAKeyValue");
                Element modulusNode = doc.createElementNS(
                        SAMLConstants.XMLSIG_NAMESPACE_URI
                        , "Modulus");
                Element exponentNode = doc.createElementNS(
                        SAMLConstants.XMLSIG_NAMESPACE_URI
                        , "Exponent");
                RSAKeyValue.appendChild(modulusNode);
                RSAKeyValue.appendChild(exponentNode);
                Text modulusValue =
                      doc.createTextNode(Base64.encode(modulus.toByteArray()));
                modulusNode.appendChild(modulusValue);
                Text exponentValue =
                      doc.createTextNode(Base64.encode(exponent.toByteArray()));
                exponentNode.appendChild(exponentValue);
                keyvalue.appendChild(RSAKeyValue);
            }
            
            keyInfo.appendChild(keyName).appendChild(keyNameText);
            keyInfo.appendChild(keyvalue);
        }
        return keyInfo;
    }
    
    private AttributeStatement createAttributeStatement(
            NameIdentifier senderIdentity, List attributes,boolean isBearer) {
        AttributeStatement attributeStatement = null;
        try {
            Subject subject = null;
            SubjectConfirmation subConfirmation = null;
            if (isBearer) {
                subConfirmation = new SubjectConfirmation(
                        SAMLConstants.CONFIRMATION_METHOD_BEARER);
            } else {
                subConfirmation = new SubjectConfirmation(
                        SAMLConstants.CONFIRMATION_METHOD_HOLDEROFKEY);
                subConfirmation.setKeyInfo(createKeyInfo());
                
            }
            subject = new Subject(senderIdentity, subConfirmation);
            return new AttributeStatement(subject, attributes);
            
        } catch (Exception e) {
            if(debug.messageEnabled()) {
                debug.message("createAttributeStatement: ", e);
            }
        }
        
        return null;
    }
    
    private static SecurityAttributePlugin getAttributePlugin() {
        if(attributePlugin != null) {
            return attributePlugin;
        }
        
        String pluginName = SystemPropertiesManager.get(WS_ATTRIBUTE_PLUGIN);
        if(pluginName == null || pluginName.length() == 0) {
            return null;
        }
        
        try {
            Class pluginClass = Class.forName(pluginName);
            attributePlugin =
                    (SecurityAttributePlugin)pluginClass.newInstance();
        } catch (Exception ex) {
            
            if(debug.warningEnabled()) {
                debug.warning("LibSecurityTokenProvider." +
                    "getAttributePlugin: Exception", ex);
            }
        }    
        return attributePlugin;
        
    }
}
