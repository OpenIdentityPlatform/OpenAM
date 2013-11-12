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
 * $Id: SecurityTokenManagerClient.java,v 1.9 2008/08/19 19:11:09 veiming Exp $
 *
 */

/*
 * Portions Copyright 2013 ForgeRock AS
 */
package com.sun.identity.liberty.ws.security;

import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.Base64;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.disco.EncryptedResourceID;

import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.AssertionIDReference;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.saml.protocol.AssertionArtifact;

import java.net.InetAddress;
import java.net.URL;

import java.rmi.RemoteException;
import java.rmi.ServerException;

import java.security.cert.X509Certificate;

import java.util.Iterator;
import java.util.ResourceBundle;

import javax.xml.soap.SOAPException;


/**
 * The class <code>SecurityTokenManagerClient</code> is a <code>final</code> 
 * class that provides interfaces to create, get and destroy
 * <code>Assertion</code>s.
 * <p>
 * The class provides mechanisms to manage the <code>Assertion</code>s either
 * locally (i.e., within the same JVM process) or remotely on another instance
 * of OpenSSO. The default constructor will manage the <code>
 * Assertion</code>s locally if it detects SAML web services running locally,
 * else will use on of the configured OpenSSO. The constructor which
 * accepts an <code>URL</code> will always use the URL to manage the assertions.
 * <p>
 * Having obtained an instance of <code>AssertionManagerClient</code>, its
 * methods can be called to create/get <code>Assertion</code>, and 
 * <code>AssertionArtifact</code>, and to obtain decision from an 
 * <code>Query</code>.
 *
 * @supported.api
 */
public final class SecurityTokenManagerClient {

    // Service name in naming
    private static String SERVICE_NAME = "securitytokenmanager";

    // Flag to determine if AssertionManager is local or remote
    private static boolean checkedForLocal;
    private static boolean isLocal;
    private boolean useLocal;

    // Remote JAX-RPC server for objects that use default constructor
    private static SOAPClient remoteStub;

    // If local pointer to SecurityTokenManager instance
    private SecurityTokenManager securityTokenManager;
    private String ssoToken = null;

    // JAX-RPC remote stub
    private SOAPClient stub;

    static ResourceBundle bundle = 
                Locale.getInstallResourceBundle("libLibertySecurity");
    /**
     * Returns an instance of <code>SecurityTokenManagerClient</code>
     *
     * @param credential credential of the caller used to see
     *        if access to this security token manager client is allowed.
     * @throws SecurityTokenException if unable to access the
     *         the security token manager client.
     */
    public SecurityTokenManagerClient(Object credential)
                                      throws SecurityTokenException {
        if (!checkedForLocal) {
            try {
                // Construct the URL for local server.
                remoteStub = getServiceEndPoint(
                    SystemPropertiesManager.get(SAMLConstants.SERVER_PROTOCOL),
                    SystemPropertiesManager.get(SAMLConstants.SERVER_HOST),
                    SystemPropertiesManager.get(SAMLConstants.SERVER_PORT),
                    SystemPropertiesManager.get(SAMLConstants.SERVER_URI));
                remoteStub.send("checkForLocal", null, null, null);
                if (SecurityTokenManagerImpl.isLocal) {
                    isLocal = true;
                    SecurityTokenManager.debug.warning(
                            "STMC(): Using local service");
                    securityTokenManager = new SecurityTokenManager(credential);
                }
                checkedForLocal = true;
            } catch (Exception e) {
                checkedForLocal = true;
                if (SecurityTokenManager.debug.warningEnabled()) {
                    SecurityTokenManager.debug.warning(
                            "SecurityTokenManagerClient()Exception", e);
                }
                throw (new SecurityTokenException(e.getMessage()));
            }
        }
        if (isLocal) {
            useLocal = true;
        } else {
            // Use the remoteStub if set
            stub = remoteStub;
            try {
                ssoToken =
                    SessionManager.getProvider().getSessionID(credential);
                stub.send("initialization", ssoToken, null, ssoToken); 
            } catch (Exception e) {
                if (SecurityTokenManager.debug.warningEnabled()) {
                    SecurityTokenManager.debug.warning(
                            "SecurityTokenManagerClient()Exception", e);
                }
                throw (new SecurityTokenException(e.getMessage()));
            }
        }
    }
    
    /**
     * Returns an instance of <code>SecurityTokenManagerClient</code>
     * that will use the provided <code>URL</code> for the management
     * of security tokens.
     *
     * @param url the <code>SecurityTokenManagerClient</code> service URL that
     *        will be used to get <code>BinarySecurityToken</code> and
     *	      <code>SAMLSecurityToken</code>.
     * @param credential credential of the caller used to see
     *        if access to this security token manager client is allowed.
     * @throws SecurityTokenException if unable to access the
     *         the security token manager client.
     */
    public SecurityTokenManagerClient(String url, Object credential)
        throws SecurityTokenException {
        try {
	    // Construct the JAX-RPC stub and set the URL endpoint
            ssoToken = SessionManager.getProvider().getSessionID(credential);
            String[] urls = {url};
            stub = new SOAPClient(urls);
            stub.send("initialization", ssoToken, null, ssoToken);
            useLocal = false;
        } catch (Exception e) {
            if (SecurityTokenManager.debug.warningEnabled()) {
                SecurityTokenManager.debug.warning("STMC() Exception", e);
            }
            throw (new SecurityTokenException(e.getMessage()));
        }
    }

    // Private method to get the service endpoint URL
    private static SOAPClient getServiceEndPoint(String protocol,
        String hostname, String port, String uri) throws Exception {
        // Obtain the URL for the service endpoint
        int intPort = Integer.parseInt(port);
        URL weburl = SystemConfigurationUtil.getServiceURL(SERVICE_NAME,
            protocol, hostname, intPort, uri);
        String iurl = weburl.toString();
        if (SecurityTokenManager.debug.messageEnabled()) {
            SecurityTokenManager.debug.message(
                "SecurityTokenManagerClient with URL: " + iurl);
        }
        String[] urls = {iurl};
        return new SOAPClient(urls);
    }

    private static SOAPClient getRemoteStub()
        throws SecurityTokenException {
	boolean foundServer = false;
	Exception ee = null;
	SOAPClient remoteStub = null;
	try {
	    // Get the list of platform servers
	    Iterator serverList =
		SystemConfigurationUtil.getServerList().iterator();

	    // Get a server that is responding
	    while (serverList.hasNext() && !foundServer) {
		URL u = new URL((String) serverList.next());
		remoteStub = getServiceEndPoint(u.getProtocol(), u.getHost(),
		    Integer.toString(u.getPort()), u.getPath());
		// Check if the server is active
		try {
		    // this call will throw an exception if server is down
		    remoteStub.send("checkForLocal", null, null, null);
		    if (SecurityTokenManager.debug.messageEnabled()) {
			SecurityTokenManager.debug.message(
			    "STMC(): Using the remote URL: " + u.toString());
		    }
		    foundServer = true;
		    if (SecurityTokenManager.debug.warningEnabled()) {
			SecurityTokenManager.debug.warning(
			    "STMC:getRemoteStub: remote server being used: "
			    + u.toString());
		    }
		} catch (Exception e) {
		    ee = e;
		    if (SecurityTokenManager.debug.warningEnabled()) {
			SecurityTokenManager.debug.warning(
			    "STMC:getRemoteStub: server (" +
			    u.toString() + ") error: ", e);
		    }
		}
	    }
	} catch (Exception f) {
	    ee = f;
	    if (SecurityTokenManager.debug.warningEnabled()) {
		SecurityTokenManager.debug.warning(
			"STMC:getRemoteStub: generic error: ", f);
	    }
	}
	
	if (!foundServer) {
	    // No valid server found. Return the last exception
	    if (ee != null) {
		throw (new SecurityTokenException(ee.getMessage()));
	    } else {
		throw (new SecurityTokenException(
		    bundle.getString("serverNotFound")));
	    }
	}
	return (remoteStub);
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
     * 
     * @supported.api
     */
    public void setCertAlias(java.lang.String certAlias)
        throws SecurityTokenException {
        if (useLocal) {
            securityTokenManager.setCertAlias(certAlias);
        } else {
            try {
                Object[] obj = {certAlias, Boolean.TRUE};
                stub.send("setCertificate", obj, null, ssoToken);
	    } catch (Exception e) {
		if (SecurityTokenManager.debug.warningEnabled()) {
		    SecurityTokenManager.debug.warning(
			"STMC:setCertAlias()", e);
		}
		throw (new SecurityTokenException(e.getMessage()));
	    }
	}
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
	if (useLocal) {
	    securityTokenManager.setCertificate(cert);
	} else {
	    try {
		String certString = Base64.encode(cert.getEncoded());
                Object[] obj = {certString, Boolean.FALSE};
                stub.send("setCertificate", obj, null, ssoToken);
	    } catch (Exception e) {
		if (SecurityTokenManager.debug.warningEnabled()) {
		    SecurityTokenManager.debug.warning(
			"STMC:setCertificate()", e);
		}
		throw (new SecurityTokenException(e.getMessage()));
	    }
	}
    }

    /**
     * Gets the <code>X509</code> certificate Token.
     *
     * @return <code>X509</code> certificate Token.
     * @throws SecurityTokenException if the binary security token could
     * not be obtained.
     */
    public BinarySecurityToken getX509CertificateToken()
                                      throws SecurityTokenException {
	if (useLocal) {
	    return securityTokenManager.getX509CertificateToken();
	} 

	String bst = null;
	try {
            bst = (String) stub.send("getX509CertificateToken", null, null,
                ssoToken);
            return (new BinarySecurityToken(XMLUtils.toDOMDocument(bst,
                    SecurityTokenManager.debug).getDocumentElement()));
	} catch (Exception e) {
	    if (SecurityTokenManager.debug.warningEnabled()) {
		SecurityTokenManager.debug.warning(
		    "STMC:getX509CertificateToken()", e);
	    }
	    throw (new SecurityTokenException(e.getMessage()));
	}
    }

    /**
     * Creates a SAML Assertion for message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @return Assertion which contains an <code>AuthenticationStatement</code>.
     * @throws SecurityTokenException if the assertion could not be obtained.
     * @throws SAMLException if unable to generate the SAML Assertion. 
     */
    public SecurityAssertion getSAMLAuthenticationToken(
            NameIdentifier senderIdentity)
            throws SecurityTokenException, SAMLException {
	if (useLocal) {
	    return (securityTokenManager.getSAMLAuthenticationToken(
			senderIdentity));
	}

	try {
            String ni = senderIdentity.toString(true, true);
            String assertion = (String) stub.send("getSAMLAuthenticationToken", 
                ni, null, ssoToken);
            return (new SecurityAssertion(XMLUtils.toDOMDocument(assertion,
                    SecurityTokenManager.debug).getDocumentElement()));
	} catch (Exception re) {
	    if (SecurityTokenManager.debug.warningEnabled()) {
		SecurityTokenManager.debug.warning(
		    "STMC:getSAMLAuthenticationToken()", re);
	    }
	    throw (new SAMLException(re.getMessage()));
	}
    }

    /**
     * Creates a SAML Assertion for message authorization, the assertion could
     * optionally contain an <code>AuthenticationStatement</code> which will be
     * used for message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession <code>SessionContext</code> of  the invocation 
     *        identity, it is normally obtained by the credential reference in 
     *        the SAML <code>AttributeDesignator</code> for discovery resource 
     *        offering which is part of the liberty <code>ID-FF</code> 
     *        <code>AuthenResponse</code>.
     * @param resourceID id for the resource to be accessed.
     * @param includeAuthN if true, include an 
     *        <code>AutheticationStatement</code> in the Assertion which will be 
     *        used for message authentication. 
     * @param includeResourceAccessStatement if true, a 
     *        <code>ResourceAccessStatement</code>
     *        will be included in the Assertion 
     *        (for <code>AuthorizeRequester</code> directive). If false, 
     *        a <code>SessionContextStatement</code> will be included in the 
     *        Assertion (for <code>AuthenticationSessionContext</code> 
     *        directive). In the case when both <code>AuthorizeRequester</code> 
     *        and <code>AuthenticationSessionContext</code> directive need to be
     *        handled, use "true" as parameter here since the 
     *        <code>SessionContext</code> will always be included in the 
     *        <code>ResourceAccessStatement</code>.
     * @param recipientProviderID recipient's provider ID.
     * @return the <code>SecurityAssertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained.
     * @throws SAMLException if unable to generate the SAML Assertion.
     */
    public SecurityAssertion getSAMLAuthorizationToken(
            NameIdentifier senderIdentity,
            SessionContext invocatorSession,
            String resourceID,
            boolean includeAuthN,
            boolean includeResourceAccessStatement,
            String recipientProviderID)
            throws SecurityTokenException, SAMLException {
	if (useLocal) {
	    return (securityTokenManager.getSAMLAuthorizationToken(
			senderIdentity, invocatorSession, resourceID,
			includeAuthN, includeResourceAccessStatement,
                        recipientProviderID));
	}

	try {
            String ni = senderIdentity.toString(true, true);
            String sc = invocatorSession.toXMLString(true, true);
            Object[] obj = {ni, sc, resourceID, Boolean.FALSE, 
                Boolean.valueOf(includeAuthN),
                Boolean.valueOf(includeResourceAccessStatement),
                recipientProviderID};
            String assertion = (String) stub.send("getSAMLAuthorizationToken",
                obj, null, ssoToken);
            return (new SecurityAssertion(XMLUtils.toDOMDocument(assertion,
		    SecurityTokenManager.debug).getDocumentElement()));
	} catch (Exception e) {
	    if (SecurityTokenManager.debug.warningEnabled()) {
		SecurityTokenManager.debug.warning(
			"STMC:createAssertionArtifact:", e);
	    }
	    throw (new SecurityTokenException(e.getMessage()));
	}
    }

    /**
     * Creates a SAML Assertion for message authorization, the assertion could
     * optionally contain an <code>AuthenticationStatement</code> which will be
     * used for message authentication.
     *
     * @param senderIdentity name identifier of the sender.
     * @param invocatorSession <code>SessionContext</code> of  the invocation 
     *        identity, it is normally obtained by the credential reference in
     *        the SAML <code>AttributeDesignator</code> for discovery resource   
     *        offering which is part of the liberty <code>ID-FF</code> 
     *        <code>AuthenResponse</code>.
     * @param encResourceID Encrypted ID for the resource to be accessed.
     * @param includeAuthN if true, include an 
     *        <code>AutheticationStatement</code> in the
     *        Assertion which will be used for message authentication. 
     * @param includeResourceAccessStatement if true, 
     *        a <code>ResourceAccessStatement</code> will be included in the 
     *        Assertion (for <code>AuthorizeRequester</code> directive). If
     *        false, a <code>SessionContextStatement</code> will be included in 
     *        the Assertion (for <code>AuthenticationSessionContext</code> 
     *        directive). In the case when both <code>AuthorizeRequester</code> 
     *        and <code>AuthenticationSessionContext</code> directive need to be
     *        handled, use "true" as parameter here since the 
     *        <code>SessionContext</code> will always be included in the 
     *        <code>ResourceAccessStatement</code>.
     * @param recipientProviderID recipient's provider ID.
     * @return the <code>SecurityAssertion</code> object.
     * @throws SecurityTokenException if the assertion could not be obtained.
     * @throws SAMLException if unable to generate the SAML Assertion.
     */
    public SecurityAssertion getSAMLAuthorizationToken(
		NameIdentifier senderIdentity,
		SessionContext invocatorSession,
		EncryptedResourceID encResourceID,
		boolean includeAuthN,
		boolean includeResourceAccessStatement,
                String recipientProviderID)
	throws SecurityTokenException, SAMLException {
        if (useLocal) {
            return (securityTokenManager.getSAMLAuthorizationToken(
                senderIdentity, invocatorSession, encResourceID,
                includeAuthN, includeResourceAccessStatement,
                recipientProviderID));
        }

        String assertion = null;

        try {
            String ni = senderIdentity.toString(true, true);
            String sc = invocatorSession.toXMLString(true, true);
            String resourceID = encResourceID.toString();
            Object[] obj = {ni, sc, resourceID,  Boolean.TRUE,
                Boolean.valueOf(includeAuthN),
                Boolean.valueOf(includeResourceAccessStatement),
                recipientProviderID};
            assertion = (String) stub.send("getSAMLAuthorizationToken",
                obj, null, ssoToken);
            return (new SecurityAssertion(XMLUtils.toDOMDocument(assertion,
                    SecurityTokenManager.debug).getDocumentElement()));
        } catch (Exception e) {
            if (SecurityTokenManager.debug.warningEnabled()) {
                SecurityTokenManager.debug.warning(
                        "STMC:getSAMLAuthorizationToken() ", e);
            }
            throw (new SecurityTokenException(e.getMessage()));
        }
    }
}
