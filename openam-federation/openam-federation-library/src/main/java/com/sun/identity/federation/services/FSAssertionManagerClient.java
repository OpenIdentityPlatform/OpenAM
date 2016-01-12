/*
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
 * $Id: FSAssertionManagerClient.java,v 1.8 2008/08/19 19:11:06 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.federation.services;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSRemoteException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.message.FSAssertion;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.protocol.AssertionArtifact;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.shared.xml.XMLUtils;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.RemoteException;

import org.w3c.dom.Document;

/**
 * The class <code>FSAssertionManagerClient</code> is a <code>final</code> class
 * that provides interfaces to create, get and destroy <code>Assertion</code>s.
 * <p>
 * The class provides mechanisms to manage the <code>Assertion</code>s either
 * locally (i.e., within the same JVM process) or remotely on another instance
 * of OpenAM. The default constructor will manage the <code>
 * Assertion</code>s locally if it detects SAML web services running locally,
 * else will use one of the configured OpenAM. The constructor which
 * accepts an <code>URL</code> will always use the URL to manage the assertions.
 * <p>
 * Having obtained an instance of <code>FSAssertionManagerClient</code>, 
 * its methods can be called to create/get <code>Assertion</code>, and
 * <code>AssertionArtifact</code>, and to obtain decision from an
 * <code>Query</code>.
 *
 */
public final class FSAssertionManagerClient {
    
    // Service name in naming
    private static String SERVICE_NAME = "fsassertionmanager";
    
    // Flag to determine if FSAssertionManager is local or remote
    private static boolean checkedForLocal;
    private static boolean isLocal;
    private boolean useLocal;
    private String hostedEntityId;
    private String metaAlias;
    
    // Remote JAX-RPC server for objects that use default constructor
    private static SOAPClient remoteStub;
    
    // If local pointer to AssertionManager instance
    private static FSAssertionManager assertionManager;
    
    // JAX-RPC remote stub
    private SOAPClient stub;

    /**
     * Returns an instance of <code>AssertionManagerClient</code>.
     *
     * @param metaAlias hosted provider's meta alias.
     * @throws FSException
     */
    public FSAssertionManagerClient(String metaAlias) throws FSException
    {
        if (!checkedForLocal) {
            try {
                // Construct the URL for local server
                this.metaAlias = metaAlias;
                hostedEntityId = FSUtils.getIDFFMetaManager().
                    getEntityIDByMetaAlias(metaAlias);
                remoteStub = getServiceEndPoint(
                    SystemConfigurationUtil.getProperty(
                        SAMLConstants.SERVER_PROTOCOL),
                    InetAddress.getLocalHost().getHostName(),
                    Integer.parseInt(SystemConfigurationUtil.getProperty(
                        SAMLConstants.SERVER_PORT)),
                    SystemConfigurationUtil.getProperty(
                        SAMLConstants.SERVER_URI));
                remoteStub.send("checkForLocal", null, null);
                if (FSAssertionManagerImpl.isLocal) {
                    isLocal = true;
                    assertionManager =
                        FSAssertionManager.getInstance(metaAlias);
                }
                checkedForLocal = true;
            } catch (Exception e) {
                checkedForLocal = true;
                if (FSUtils.debug.warningEnabled()) {
                    FSUtils.debug.warning(
                        "AssertionManagerClient()Exception", e);
                }
                throw (new FSException(e.getMessage()));
            }
        }
        if (isLocal) {
            useLocal = true;
        } else {
            // Use the remoteStub if set
            stub = remoteStub;
        }
    }
    
    /**
     * Returns an instance of <code>FSAssertionManagerClient</code>
     * that will use the provided <code>URL</code> for the management
     * of assertions.
     *
     * @param metaAlias hosted provider's meta alias.
     * @param url the <code>FSAssertionManager</code> service URL that
     *  will be used to create, get and delete <code>Assertion</code>s
     * @throws FSException
     */
    public FSAssertionManagerClient(String metaAlias, String url)
        throws FSException {
        try {
            // Construct the JAX-RPC stub and set the URL endpoint
            this.metaAlias = metaAlias;
            this.hostedEntityId = FSUtils.getIDFFMetaManager().
                getEntityIDByMetaAlias(metaAlias);
            String[] urls = {url};
            stub = new SOAPClient(urls);
        } catch (Exception e) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning(
                    "FSAssertionManagerClient() Exception", e);
            }
            throw (new FSException(e.getMessage()));
        }
    }
    
    // Private method to get the service endpoint URL
    private static SOAPClient getServiceEndPoint(
        String protocol, String hostname, int port, String uri)
        throws Exception 
    {
        // Obtain the URL for the service endpoint
        URL weburl = SystemConfigurationUtil.getServiceURL(
            SERVICE_NAME, protocol, hostname, port, uri);
        String iurl = weburl.toString();
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionManagerClient with URL: " + iurl);
        }
        
        // Obtaining the stub for JAX-RPC and setting the endpoint URL
        String[] urls = {iurl};
        return new SOAPClient(urls);
    }
    
    /**
     * Returns assertion associated with the <code>AssertionArtifact</code>.
     * @param artifact An <code>AssertionArtifact</code>.
     * @param destID The destination site requesting the assertion using
     *  the artifact. This String is compared with the destID that
     *  the artifact is created for originally.
     * @return The Assertion referenced to by artifact.
     * @exception FSException If an error occurred during the process, or no
     *  assertion maps to the input artifact.
     */
    protected Assertion getAssertion(AssertionArtifact artifact, String destID)
        throws FSException
    {
        if (useLocal) {
            return (assertionManager.getAssertion(artifact, destID));
        }
        
        String assertion = null;
        try {
            Object[] obj = {metaAlias, artifact.getAssertionArtifact(),
                Base64.encode(SAMLUtils.stringToByteArray(destID))};
            assertion = (String) stub.send("getAssertion", obj, null, null); 
            if (assertion == null && FSUtils.debug.messageEnabled()) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("AMC:getAssertion(" + artifact + 
                        ", " + destID + "): Server returned NULL");
                }
            } else {
                if (FSUtils.debug.warningEnabled()) {
                    FSUtils.debug.warning("AMC:getAssertion: asserion:" + 
                        assertion);
                }
            }
            return ((assertion == null) ? null : new FSAssertion(
                XMLUtils.toDOMDocument(assertion, FSUtils.debug)
                    .getDocumentElement()));
        } catch (RemoteException re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getAssertion: " + artifact, re);
            }
            throw (new FSException(re.getMessage()));
        } catch (FSRemoteException re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getAssertion: " + artifact, re);
            }
            throw (new FSException(re.getMessage()));
        } catch (SAMLException re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getAssertion: " + artifact, re);
            }
            throw (new FSException(re.getMessage()));
        } catch (Exception re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getAssertion: " + artifact, re);
            }
            throw (new FSException(re.getMessage()));
        }
        
    }
    
    /**
     * Returns the destination id the artifact is created for.
     * @param artifact <code>AssertionArtifact</code> object
     * @return destination id
     * @exception FSException if error occurred.
     */
    protected String getDestIdForArtifact(AssertionArtifact artifact)
        throws FSException 
    {
        if (useLocal) {
            return (assertionManager.getDestIdForArtifact(artifact));
        }
        String providerId = null;
        try {
            Object[] obj = {metaAlias, artifact.getAssertionArtifact()};
            providerId = (String) stub.send("getDestIdForArtifact", obj, 
            		null, null); 
            if (providerId == null && FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "AMC:getDestIdForArtifact(" + artifact +
                    "): Server returned NULL");
            }
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getDestIdForArtifact: returning" +
                    providerId);
            }
            return providerId;
        } catch (RemoteException re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getDestIdForArtifact: " + artifact,
                    re);
            }
            throw (new FSException(re.getMessage()));
        } catch (FSRemoteException re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getDestIdForArtifact: " + artifact,
                    re);
            }
            throw (new FSException(re.getMessage()));
        } catch (Exception re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getDestIdForArtifact: " + artifact,
                    re);
            }
            throw (new FSException(re.getMessage()));
        }
    }


    /**
     * Checks if the user exists.
     * @param userDN user ID
     * @return <code>true</code> if the user exists; <code>false</code>
     *  otherwise.
     * @exception FSException if error occurred.
     */
    public boolean isUserExists(String userDN)
        throws FSException
    {
        try {
            Object[] obj = {userDN, metaAlias};
            Boolean ret = (Boolean) stub.send("isUserExists", obj, null,
            		null);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("AMC:isUserExists(" + userDN + ")"
                    + " returned " + ret);
            }
            return ret.booleanValue();
        } catch (RemoteException re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:isUserExists: " + userDN, re);
            }
            throw (new FSException(re.getMessage()));
        } catch (FSRemoteException re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:isUserExists: " + userDN, re);
            }
            throw (new FSException(re.getMessage()));
        } catch (Exception re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:isUserExists: " + userDN, re);
            }
            throw (new FSException(re.getMessage()));
        }
    }
    protected Status getErrorStatus( AssertionArtifact artifact )
        throws FSException
    {
        String status = null;
        try {
            Object[] obj = {metaAlias, artifact.getAssertionArtifact()};
            status = (String) stub.send("getErrorStatus", obj, null, null); 
            if (status == null && FSUtils.debug.messageEnabled()) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("AMC:getErrorStatus(" + artifact + 
                         "): Server returned NULL");
                }
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("AMC:getErrorStatus: status:" + 
                        status );
                }
            }
            if ( null != status ) {
                Document doc = XMLUtils.toDOMDocument( status, FSUtils.debug );
                if ( null != doc ) {
                    return new Status( doc.getDocumentElement() );
                }
            }
        } catch (RemoteException re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getErrorStatus: " + artifact, re);
            }
            throw (new FSException(re.getMessage()));
        } catch (FSRemoteException re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getErrorStatus: " + artifact, re);
            }
            throw (new FSException(re.getMessage()));
        } catch (SAMLException re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getErrorStatus: " + artifact, re);
            }
            throw (new FSException(re.getMessage()));
        } catch (Exception re) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("AMC:getErrorStatus: " + artifact, re);
            }
            throw (new FSException(re.getMessage()));
        }
        return null;
    }
}
