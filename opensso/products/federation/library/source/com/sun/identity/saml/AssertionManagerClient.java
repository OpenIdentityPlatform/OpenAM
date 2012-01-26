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
 * $Id: AssertionManagerClient.java,v 1.8 2008/08/19 19:11:11 veiming Exp $
 *
 */

package com.sun.identity.saml;

import java.util.*;
import java.net.URL;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.AssertionIDReference;
import com.sun.identity.saml.protocol.AssertionArtifact;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.shared.jaxrpc.JAXRPCHelper;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.common.SystemConfigurationUtil;

/**
 * The class <code>AssertionManagerClient</code> is a <code>final</code> class
 * that provides interfaces to create, get and destroy <code>Assertion</code>s.
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
 */
public final class AssertionManagerClient {

    // Service name in naming
    private static String SERVICE_NAME = "samlassertionmanager";

    // Flag to determine if AssertionManager is local or remote
    static boolean checkedForLocal;
    static boolean isLocal;

    // Instance variable to use local service via AssertionManager
    private boolean useLocal;

    // Remote JAX-RPC server for objects that use default constructor
    private static SOAPClient remoteStub;

    // If local pointer to AssertionManager instance
    private static AssertionManager assertionManager;

    // JAX-RPC remote stub
    private SOAPClient stub;

    /**
     * Default Constructor
     *
     * @throws SAMLException if it cannot be constructed.
     */
    public AssertionManagerClient() throws SAMLException {
        if (!checkedForLocal) {
            try {
                // Construct the URL for locally defined server.
                // This will throw URLNotFoundException if host
                // is not part of Naming platform list
                remoteStub = getServiceEndPoint(
                    SystemConfigurationUtil.getProperty(
                        SAMLConstants.SERVER_PROTOCOL),
                    SystemConfigurationUtil.getProperty(
                        SAMLConstants.SERVER_HOST),
                    SystemConfigurationUtil.getProperty(
                        SAMLConstants.SERVER_PORT),
                    SystemConfigurationUtil.getProperty(
                        SAMLConstants.SERVER_URI));
                    
                // The following call will throw one of the following
                // exception if service does not exist or does not have
                // permissions: javax.xml.soap.SOAPException
                // java.rmi.RemoteException, java.rmi.ServerException
                remoteStub.send("checkForLocal", null, null,null);
                if (isLocal) {
                    SAMLUtils.debug.warning("AMC(): Using local service");
                    assertionManager = AssertionManager.getInstance();
                }
                checkedForLocal = true;
            /*
             * The following code is commented out since we are not ready
             * to do a fall back funtion yet, as rest of the OpenSSO
             * donot support this. The server specified in AMConfig.properties
             * will only be used to determine the remote server.
              */
            /* } catch (URLNotFoundException unfe) {
                if (SAMLUtils.debug.warningEnabled()) {
                    SAMLUtils.debug.warning("AMC: No SAML service locally. " +
                        "URLNotFound.", unfe);
                }
                remoteStub = getRemoteStub();
                checkedForLocal = true;
            } catch (SOAPException se) {
                if (SAMLUtils.debug.warningEnabled()) {
                    SAMLUtils.debug.warning("AMC: No SAML service locally. " +
                        "SOAPException.", se);
                }
                remoteStub = getRemoteStub();
                checkedForLocal = true;
            } catch (ServerException se) {
                if (SAMLUtils.debug.warningEnabled()) {
                    SAMLUtils.debug.warning("AMC: No SAML service locally. " +
                        "ServerException.", se);
                }
                remoteStub = getRemoteStub();
                checkedForLocal = true;
            } catch (RemoteException re) {
                if (SAMLUtils.debug.warningEnabled()) {
                    SAMLUtils.debug.warning("AMC: No SAML service locally. " +
                        "RemoteException.", re);
                }
                remoteStub = getRemoteStub();
                checkedForLocal = true;
            */

            } catch (Exception e) {
                if (SAMLUtils.debug.warningEnabled()) {
                    SAMLUtils.debug.warning(
                        "AssertionManagerClient()Exception", e);
                }
                throw (new SAMLException(e.getMessage()));
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
     * Constructs an instance of <code>AssertionManagerClient</code>
     * that will use the provided <code>URL</code> for the management
     * of assertions.
     *
     * @param url <code>AssertionManager</code> service URL that will be used
     *        to create, get and delete assertions.
     * @throws SAMLException if it cannot be constructed.
     */
    public AssertionManagerClient(String url) throws SAMLException {
        try {
            // Construct the JAX-RPC SOAPClient
            String[] urls = { url };
            stub = new SOAPClient(urls);
            useLocal = false;
        } catch (Exception e) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning(
                    "AssertionManagerClient() Exception", e);
            }
            throw (new SAMLException(e.getMessage()));
        }
    }

    /**
     * Constructs an instance of <code>AssertionManagerClient</code>
     * that will use the provided <code>URLs</code> for the management
     * of assertions. In case of server failure, it will fall back
     * to next available server. 
     *
     * @param urls an array of <code>AssertionManager</code> service URLs that
     *        will be used to create, get and delete assertions.
     * @throws SAMLException if it cannot be constructed.
     */
    public AssertionManagerClient(String[] urls) throws SAMLException {
        try {
            // Construct the JAX-RPC SOAPClient
            stub = new SOAPClient(urls);
            useLocal = false;
        } catch (Exception e) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning(
                    "AssertionManagerClient() Exception", e);
            }
            throw (new SAMLException(e.getMessage()));
        }
    }

    // Private method to get the service endpoint URL
    private static SOAPClient getServiceEndPoint(String protocol,
        String hostname, String port, String uri) throws Exception {
        // Obtain the URL for the service endpoint
        URL weburl = SystemConfigurationUtil.getServiceURL(
            SERVICE_NAME, protocol, hostname, Integer.parseInt(port), uri);
        String iurl = weburl.toString();
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("AssertionManagerClient with URL: " + iurl);
        }

        // Obtaining the stub for JAX-RPC
        String[] urls = { iurl };
        return (new SOAPClient(urls));
    }

    private static SOAPClient getRemoteStub() throws SAMLException {
        SOAPClient remoteStub = null;
        try {
            // Get a valid server from JAXRPCUtil. This throws
            // Exception if no servers are found
            URL u = new URL(JAXRPCHelper.getValidURL(SERVICE_NAME));
            remoteStub = getServiceEndPoint(u.getProtocol(), u.getHost(),
                Integer.toString(u.getPort()), u.getPath());

            // The following call will check if the JVM contains the
            // the service instance also. If this is a server instance also
            // "short-circuit" will be performed.
            remoteStub.send("checkForLocal", null, null,null);
        } catch (Exception ee) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning(
                    "AMC:getRemoteStub: generic error: ", ee);
            }
            throw (new SAMLException(ee.getMessage()));
        }
        return (remoteStub);
    }

    /** 
     * Returns an assertion that contains an authentication statement.
     * @param token User session that contains authentication
     *        information which is needed to create the authentication
     *        statement. 
     * @return the created assertion.
     * @throws SAMLException if the assertion cannot be created.
     */
    public Assertion createAssertion(Object token) throws SAMLException {
        if (useLocal) {
            return (assertionManager.createAssertion(token));
        }

        String assertion = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object[] args = { sessionProvider.getSessionID(token) };
            assertion = (String) stub.send("createAssertion", args, null,
                null);
            return (new Assertion(XMLUtils.toDOMDocument(assertion,
                    SAMLUtils.debug).getDocumentElement()));
        } catch (Exception re) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("AMC:createAssertion(SSO)", re);
            }
            throw (new SAMLException(re.getMessage()));
        }
    }

    /**
     * Returns an assertion that contains an authentication and attribute
     * statement.
     * @param token User session that contains authentication
     *        information which is needed to create the authentication
     *        statement for the assertion.
     * @param attributes A list of attribute objects which are used to create
     *        the attribute statement.
     * @return The created assertion.
     * @throws SAMLException If the Assertion cannot be created.
     */
    public Assertion createAssertion(Object token, List attributes)
        throws SAMLException
    {
        if (useLocal) {
            return (assertionManager.createAssertion(token, attributes));
        }

        // Check for null or empty attributes
        if (attributes == null || attributes.isEmpty())
            return (createAssertion(token));

        String assertion = null;
        try {
            List attrs = new LinkedList();
            for (Iterator iter = attributes.iterator(); iter.hasNext();) {
                Attribute attribute = (Attribute) iter.next();
                attrs.add(attribute.toString(true, true));
            }
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object[] args = { sessionProvider.getSessionID(token), attrs };
            assertion = (String) stub.send("createAssertion2", args, null,
                null);
            return (new Assertion(XMLUtils.toDOMDocument(assertion,
                    SAMLUtils.debug).getDocumentElement()));
        } catch (Exception re) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("AMC:createAssertion(SSO, attrs)", re);
            }
            throw (new SAMLException(re.getMessage()));
        }
    }

    /**
     * Returns an <code>AssertionArtifact</code> for the given <code>
     * Assertion</code>.
     * @param assertion The Assertion for which an Artifact needs to be
     *       created. 
     * @param target The <code>sourceID</code> of the site for which the
     *        <code>AssertionArtifact</code> is created. It is in raw String
     *        format (not Base64 encoded, for example.) This String can be
     *        obtained from converting the 20 bytes sequence to char Array,
     *        then from the char Array to String.
     * @return <code>AssertionArtifact</code>
     * @throws SAMLException if the <code>AssertionArtifact</code> cannot be
     *         created.
     */
    public AssertionArtifact createAssertionArtifact(Assertion assertion,
        String target) throws SAMLException
    {
        if (useLocal) {
            return (assertionManager.createAssertionArtifact(
                    assertion, target));
        }

        String aa = null;
        try {
            Object[] args = {assertion.toString(true, true),
                Base64.encode(SAMLUtils.stringToByteArray(target)) };
            aa = (String) stub.send("createAssertionArtifact", args, null,
            		null);
            return (new AssertionArtifact(aa));
        } catch (Exception re) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("AMC:createAssertionArtifact:", re);
            }
            throw (new SAMLException(re.getMessage()));
        }
    }

    /**
     * Returns the <code>Assertion</code> based on the
     * <code>AssertionIDReference</code>.
     *
     * @param idRef The <code>AssertionIDReference</code> which references to an
     *        Assertion.
     * @param destID A set of String that representing the destination site id.
     *        The destination site id requesting the assertion using
     *        the assertion id reference. This String is compared with the
     *        <code>destID</code> that the assertion is created for originally.
     *        This field is not used (could be null) if the assertion was
     *        created without a <code>destID</code> originally. This String can
     *        be obtained from converting the 20 byte site id sequence to char
     *        array, then a new String from the char array.
     * @return the Assertion referenced by the
     *         <code>AssertionIDReference</code>.
     * @throws SAMLException if an error occurred during the process; or
     *          the assertion could not be found.
     */
    public Assertion getAssertion(AssertionIDReference idRef, Set destID)
        throws SAMLException
    {
        if (useLocal)
            return (assertionManager.getAssertion(idRef, destID));

        String assertion = null;
        try {
            Set destSet = new HashSet(); 
            if (destID != null && !destID.isEmpty()) {
                Iterator it = destID.iterator();
                while (it.hasNext()) {
                    destSet.add(Base64.encode(SAMLUtils.stringToByteArray(
                                          (String) it.next())));
                }
            }
            Object[] args = { idRef.getAssertionIDReference(), destSet };
            assertion = (String) stub.send("getAssertionByIdRef", args, null,
            		null);
            return (new Assertion(XMLUtils.toDOMDocument(assertion,
                    SAMLUtils.debug).getDocumentElement()));
        } catch (Exception re) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("AMC:getAssertion: " + idRef, re);
            }
            throw (new SAMLException(re.getMessage()));
        }
    }
    
    /**
     * Returns the <code>Assertion</code> based on the
     * <code>AssertionIDReference</code>
     *
     * @param idRef The <code>AssertionIDReference</code> which references to
     *        an Assertion.
     * @param destID The destination site id requesting the assertion using
     *        the assertion id reference. This String is compared with the
     *        <code>destID</code> that the assertion is created for originally.
     *        This field is not used (could be null) if the assertion was
     *        created without a <code>destID</code> originally. This String can
     *        be obtained from converting the 20 byte site id sequence to char
     *        array, then a new String from the char array.
     * @return the Assertion referenced by the
     *         <code>AssertionIDReference</code>.
     * @throws SAMLException If an error occurred during the process; or
     *          the assertion could not be found.
     */
    public Assertion getAssertion(AssertionIDReference idRef, String destID)
        throws SAMLException
    {
        if (useLocal) {
            return (assertionManager.getAssertion(idRef, destID));
        }

        String assertion = null;
        try {
            // rpc could not handle destID is null or empty string case
            if (destID == null || destID.length() == 0) {
                destID = " ";
            }
            Object[] args = { idRef.getAssertionIDReference(),
                Base64.encode(SAMLUtils.stringToByteArray(destID)) };
            assertion = (String) stub.send("getAssertionByIdRef2", args, null,
            		null);
            return (new Assertion(XMLUtils.toDOMDocument(assertion,
                    SAMLUtils.debug).getDocumentElement()));
        } catch (Exception re) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("AMC:getAssertion: " + idRef, re);
            }
            throw (new SAMLException(re.getMessage()));
        }
    }

    /**
     * Returns assertion associated with the <code>AssertionArtifact</code>.
     * @param artifact An <code>AssertionArtifact</code>.
     * @param destID  A Set of String that represents the destination id. 
     *          The destination site requesting the assertion using the
     *          artifact. This String is compared with the <code>destID</code>
     *          that the artifact is created for originally. This field must not
     *          be null or empty set.
     * @return The Assertion referenced to by artifact.
     * @throws SAMLException If an error occurred during the process, or no
     *          assertion maps to the input artifact.
     */
    public Assertion getAssertion(AssertionArtifact artifact, Set destID)
        throws SAMLException
    {
        if (useLocal) {
            return (assertionManager.getAssertion(artifact, destID));
        }

        String assertion = null;
        try {
            if (destID == null || destID.isEmpty()) {
                SAMLUtils.debug.error("AssertionManagerClient:getAssertion(" + 
                             "AssertionArtifact, Set): destID set is null");
                throw new SAMLException("nullInput");
            }
            Set destSet = new HashSet();
            Iterator it = destID.iterator();
            while (it.hasNext()) {
                destSet.add(Base64.encode(SAMLUtils.stringToByteArray(
                                         (String) it.next())));
            }
            Object[] args = { artifact.getAssertionArtifact(), destSet };
            assertion = (String) stub.send("getAssertion", args, null,
            		null);
            return (new Assertion(XMLUtils.toDOMDocument(assertion,
                    SAMLUtils.debug).getDocumentElement()));
        } catch (Exception re) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("AMC:getAssertion: " + artifact, re);
            }
            throw (new SAMLException(re.getMessage()));
        }
    }
    
    /**
     * Returns assertion associated with the <code>AssertionArtifact</code>.
     * @param artifact An <code>AssertionArtifact</code>.
     * @param destID The destination site requesting the assertion using
     *          the artifact. This String is compared with the
     *          <code>destID</code> that the artifact is created for originally.
     * @return The Assertion referenced to by artifact.
     * @throws SAMLException If an error occurred during the process, or no
     *          assertion maps to the input artifact.
     */
   protected Assertion getAssertion(AssertionArtifact artifact, String destID)
        throws SAMLException
    {
        if (useLocal) {
            return (assertionManager.getAssertion(artifact, destID));
        }

        String assertion = null;
        try {
            Object[] args = { artifact.getAssertionArtifact(),
                Base64.encode(SAMLUtils.stringToByteArray(destID)) };
            assertion = (String) stub.send("getAssertion2", args, null,
            		null);
            return (new Assertion(XMLUtils.toDOMDocument(assertion,
                    SAMLUtils.debug).getDocumentElement()));
        } catch (Exception re) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("AMC:getAssertion: " + artifact, re);
            }
            throw (new SAMLException(re.getMessage()));
        }
    }

    /**
     * Returns the <code>Assertion</code> based on the 
     * <code>AssertionIDReference</code>.
     *
     * @param idRef The <code>AssertionIDReference</code> which references to an
     *        Assertion.
     * @param token User session that is allowed to obtain the assertion.
     *        This token must have top level administrator role.
     * @return the Assertion referenced by the
     *         <code>AssertionIDReference</code>.
     * @throws SAMLException If an error occurred during the process; or
     *         the assertion could not be found.
     */
    public Assertion getAssertion(AssertionIDReference idRef, Object token)
        throws SAMLException
    {
        if (useLocal) {
            return (assertionManager.getAssertion(idRef, token));
        }

        String assertion = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object[] args = { idRef.getAssertionIDReference(),
                sessionProvider.getSessionID(token) };
            assertion = (String) stub.send("getAssertionByIdRefToken",
                args, null, null);
            return (new Assertion(XMLUtils.toDOMDocument(assertion,
                    SAMLUtils.debug).getDocumentElement()));
        } catch (Exception re) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("AMC:getAssertion: " + idRef, re);
            }
            throw (new SAMLException(re.getMessage()));
        }
    }
        
    /**
     * Returns all valid <code>AssertionArtifacts</code> managed by
     * this instance (or the identified remote instance) of OpenSSO.
     * @param token User session which is allowed to get all
     *        <code>AssertionArtifacts</code>
     * @return A Set of valid <code>AssertionArtifacts</code>. Each element
     *         in the Set is an <code>AssertionArtifact</code> object
     *         representing an artifact.
     * @throws SAMLException If this method can not gets all valid
     *         <code>AssertionArtifacts</code>.
     */
    public Set getAssertionArtifacts(Object token) throws SAMLException {
        if (useLocal) {
            return (assertionManager.getAssertionArtifacts(token));
        }

        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            return ((Set) stub.send("getAssertionArtifacts",
                sessionProvider.getSessionID(token), null));
        } catch (Exception re) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("AMC:getAssertionArtifacts: ", re);
            }
            throw (new SAMLException(re.getMessage()));
        }
    }

    /**
     * Returns all valid <code>Assertion</code>s managed by this instance
     * (or the identified remote instance) of OpenSSO.
     *
     * @param token User session which is allowed to get all Assertions.
     * @return A Set of valid Assertion IDs. Each element in the Set is a
     *        String representing an Assertion ID. 
     * @throws SAMLException if this method can not gets all valid Assertions.
     */
    public Set getAssertions(Object token) throws SAMLException {
        if (useLocal) {
            return (assertionManager.getAssertions(token));
        }

        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            return ((Set) stub.send("getAssertions",
                sessionProvider.getSessionID(token), null));
        } catch (Exception re) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("AMC:getAssertions: ", re);
            }
            throw (new SAMLException(re.getMessage()));
        }
    }
}
