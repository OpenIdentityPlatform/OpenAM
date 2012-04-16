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
 * $Id: FSAssertionManager.java,v 1.12 2009/08/03 18:18:36 bigfatrat Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.common.PeriodicGroupRunnable;
import com.sun.identity.common.ScheduleableGroupAction;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TimerPool;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSAssertion;
import com.sun.identity.federation.message.FSAssertionArtifact;
import com.sun.identity.federation.message.FSAuthenticationStatement;
import com.sun.identity.federation.message.FSSubject;
import com.sun.identity.federation.message.common.AuthnContext;
import com.sun.identity.federation.message.common.IDPProvidedNameIdentifier;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSAttributeStatementHelper;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonIDFFSvc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.assertion.Advice;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.AssertionIDReference;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.assertion.AudienceRestrictionCondition;
import com.sun.identity.saml.assertion.Conditions;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.assertion.SubjectLocality;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLServiceManager;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.protocol.AssertionArtifact;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.shared.stats.Stats;
import com.sun.identity.shared.DateUtils;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * IDP side class which handles assertion and artifact operations.
 */
public final class FSAssertionManager {
    /**
     * Statistics instance for artifacts.
     */
    public static Stats artStats = Stats.getInstance("libIDFFArtifactMap");
    /**
     * Statistics instance for assertions.
     */
    public static Stats assrtStats = Stats.getInstance("libIDFFAssertionMap");
    private static final Integer DEFAULT_CLEANUP_INTERVAL =
        new Integer(IFSConstants.CLEANUP_INTERVAL_DEFAULT);
    private static final Integer DEFAULT_ASSERTION_TIMEOUT =
        new Integer(IFSConstants.ASSERTION_TIMEOUT_DEFAULT);

    private static Map instanceMap = new HashMap();
    
    private Map artIdMap = null;
    private Map idEntryMap = null;
    private PeriodicGroupRunnable assertionTimeoutRunnable;
    private PeriodicGroupRunnable artifactTimeoutRunnable;
    private static String SERVICE_NAMING = "fsassertionmanager";
    
    private int cleanupInterval;
    private int assertionTimeout;
    private int artifactTimeout;
    private FSArtifactStats artIdStats;
    private FSAssertionStats assrtIdStats;

    private static FedMonAgent agent = MonitorManager.getAgent();
    private static FedMonIDFFSvc idffSvc = MonitorManager.getIDFFSvc();

    private class Entry {
        private String destID = null;
        private String artString = null;
        private Object token = null;
        private Assertion assertion = null;
        
        public Entry(
            Assertion assertion,
            String destID,
            String artString,
            Object token)
        {
            this.assertion = assertion;
            this.destID = destID;
            this.artString = artString;
            this.token = token;
        }
        
        public Assertion getAssertion() {
            return assertion;
        }
        
        public String getDestID() {
            return destID;
        }
        
        public String getArtifactString() {
            return artString;
        }
        
        public Object getSessionToken() {
            return token;
        }
    }
    
    private String hostEntityId = null;
    private String realm = null;
    private String metaAlias = null;
    
    private FSAssertionManager(String metaAlias) 
        throws FSException
    {
        idEntryMap = new HashMap();
        artIdMap = new HashMap();

        this.metaAlias = metaAlias;
        realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
        try {
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            if (metaManager != null) {
                hostEntityId = metaManager.getEntityIDByMetaAlias(metaAlias);
            }
        } catch (IDFFMetaException ie) {
            throw new FSException(ie);
        }
        if (hostEntityId == null) {
            throw new FSException("nullProviderID", null);
        }
        Map attributes;
        cleanupInterval = IFSConstants.CLEANUP_INTERVAL_DEFAULT * 1000;
        assertionTimeout = IFSConstants.ASSERTION_TIMEOUT_DEFAULT * 1000;
        artifactTimeout = IFSConstants.ARTIFACT_TIMEOUT_DEFAULT * 1000;
        try {
            BaseConfigType idpConfig = FSUtils.getIDFFMetaManager().
                getIDPDescriptorConfig(realm, hostEntityId);
            attributes = IDFFMetaUtils.getAttributes(idpConfig);
            try {
                cleanupInterval = Integer.parseInt(
                    IDFFMetaUtils.getFirstAttributeValue(attributes,
                    IFSConstants.CLEANUP_INTERVAL)) * 1000;
            } catch (Exception e) {
                FSUtils.debug.error("FSAssertionManager: "
                    + "Exception while parsing interval", e);
            }
            try {
                assertionTimeout = Integer.parseInt(
                    IDFFMetaUtils.getFirstAttributeValue(attributes, 
                    IFSConstants.ASSERTION_INTERVAL)) * 1000;
            } catch (Exception e) {
                FSUtils.debug.error("AssertionManager: "
                    + "Exception while parsing timeout", e);
            }
            if (assertionTimeout <
                IFSConstants.ASSERTION_TIMEOUT_ALLOWED_DIFFERENCE) {
                assertionTimeout =
                    IFSConstants.ASSERTION_TIMEOUT_ALLOWED_DIFFERENCE;
            }
            try {
                artifactTimeout = Integer.parseInt(
                    IDFFMetaUtils.getFirstAttributeValue(attributes,
                    IFSConstants.ARTIFACT_TIMEOUT)) * 1000;
            } catch(Exception ex){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSAssertionManager: "
                        + "ArtifactTimeOut configuration not found in FSConfig."
                        + " Using Default");
                }
            }
        } catch(Exception e){
            FSUtils.debug.error("FSAssertionManager: "
                + "Exception while parsing cleanup assertion :", e);
        }
        ScheduleableGroupAction timeoutAction = new
            ScheduleableGroupAction() {
            public void doGroupAction(Object obj) {
                Entry entry = (Entry) idEntryMap.remove(obj);
                if ((agent != null) && agent.isRunning() && (idffSvc != null)){
                    idffSvc.setAssertions((long)idEntryMap.size());
                }
                if (entry != null) {
                    String artString = entry.getArtifactString();
                    if (artString != null) {
                        artIdMap.remove(artString);
                    }
                    if ((agent != null) &&
                        agent.isRunning() &&
                        (idffSvc != null))
                    {
                        idffSvc.setArtifacts((long)artIdMap.size());
                    }
                }
            }
        };
        assertionTimeoutRunnable = new PeriodicGroupRunnable(timeoutAction,
            cleanupInterval, assertionTimeout, true);
        artifactTimeoutRunnable = new PeriodicGroupRunnable(timeoutAction,
            cleanupInterval, artifactTimeout, true);
        TimerPool pool = SystemTimerPool.getTimerPool();
        pool.schedule(assertionTimeoutRunnable, new Date(((
            System.currentTimeMillis() + cleanupInterval) / 1000) * 1000));
        pool.schedule(artifactTimeoutRunnable, new Date(((
            System.currentTimeMillis() + cleanupInterval) / 1000) * 1000));
        
        if (assrtStats.isEnabled()) {
            assrtIdStats = new FSAssertionStats(idEntryMap, realm,hostEntityId);
            assrtStats.addStatsListener(assrtIdStats);

            artIdStats = new FSArtifactStats(artIdMap, realm, hostEntityId);
            artStats.addStatsListener(artIdStats);
        }

    }
    
    /**
     * Returns hosted provider meta alias.
     * @return hosted provider meta alias.
     * @see #setMetaAlias(String)
     */
    public String getMetaAlias() {
        return metaAlias; 
    }

    /**
     * Sets hosted provider meta alias.
     * @param metaAlias hosted provider meta alias
     * @see #getMetaAlias()
     */
    public void setMetaAlias(String metaAlias) {
        this.metaAlias = metaAlias;
    }

    /**
     * Returns the realm which hosted provider resides.
     * @return the realm which hosted provider resides.
     * @see #setRealm(String)
     */
    public String getRealm() {
        return realm; 
    }

    /**
     * Sets the realm which hosted provider resides.
     * @param realm The realm which hosted provider resides
     * @see #getRealm()
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Returns hosted provider Entity ID.
     * @return hosted provider Entity ID
     * @see #setEntityId(String)
     */
    public String getEntityId() {
        return hostEntityId; 
    }

    /**
     * Sets hosted provider Entity ID.
     * @param entityId hosted provider Entity ID
     * @see #getEntityId()
     */
    public void setEntityId(String entityId) {
        this.hostEntityId = entityId;
    }

    /**
     * Returns artifact to assertion ID map.
     * @return artifact to assertion ID map
     */
    public Map getArtIdMap() {
        return artIdMap;
    }

    /**
     * Returns assertion ID to <code>Entry</code> object map.
     * @return assertion ID to <code>Entry</code> object map
     */
    public Map getIdEntryMap() {
        return idEntryMap;
    }

    /**
     * Returns a <code>FSAssertionManager</code> instance.
     * @param metaAlias hosted entity ID
     * @return <code>FSAssertionManager</code> instance.
     * @exception FSException if error occurrs.
     */
    public static synchronized FSAssertionManager getInstance(
        String metaAlias)
        throws FSException
    { 

        FSUtils.debug.message("FSAssertionManager.getInstance: Called");
        FSAssertionManager instance = 
            (FSAssertionManager) instanceMap.get(metaAlias);
        if (instance == null) {
            if (FSUtils.debug.messageEnabled() ) {
                FSUtils.debug.message("FSAssertionManager.getInstance: " +
                    "Constructing a new instance of FSAssertionManager");
            }
            instance = new FSAssertionManager(metaAlias);
            synchronized (instanceMap) {
                instanceMap.put(metaAlias, instance);
            }
        }
        return(instance);
    }
    

    /**
     * Creates an assertion artifact.
     * @param id session ID
     * @param realm the realm in which the provider resides
     * @param spEntityID service provider's entity ID
     * @param spHandle service provider issued <code>NameIdentifier</code>
     * @param idpHandle identity provider issued <code>NameIdentifier</code>
     * @param inResponseTo value to InResponseTo attribute. It's the request ID.
     * @param minorVersion request minor version, used to determine assertion's
     *  minor version
     * @exception FSException,SAMLException if error occurrs
     */
    public AssertionArtifact createFSAssertionArtifact(
        String id,
        String realm,
        String spEntityID,
        NameIdentifier spHandle,
        NameIdentifier idpHandle,
        String inResponseTo,
        int minorVersion)
    throws FSException, SAMLException  
    {
        // check input
        if ((id == null) ||(spEntityID == null)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager: null input for"
                    + " method createFSAssertionArtifact.");
            }
            throw new FSException("nullInput",null);
        }
        
        // create assertion id and artifact
        String handle = SAMLUtils.generateAssertionHandle();
        if (handle == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager.createFSAssertionArt"
                 + "ifact: couldn't generate assertion handle.");
            }
            throw new FSException("errorCreateArtifact", null);
        }
        
        // TODO: should obtain it through meta
        String sourceSuccinctID = FSUtils.generateSourceID(hostEntityId);
        byte bytesSourceId[] = SAMLUtils.stringToByteArray(sourceSuccinctID);
        byte bytesHandle[] = null;
        try{
            bytesHandle =  handle.getBytes(IFSConstants.SOURCEID_ENCODING);
        } catch(Exception e){
            FSUtils.debug.error(
                "FSAssertionManager.createFSAssertionArt: ", e);
            return null;
        }
        AssertionArtifact art = new FSAssertionArtifact(
            bytesSourceId, bytesHandle);
        int assertionMinorVersion = IFSConstants.FF_11_ASSERTION_MINOR_VERSION;
        if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            assertionMinorVersion = 
                IFSConstants.FF_12_ART_ASSERTION_MINOR_VERSION;
        }
        Assertion assertion =
            createFSAssertion(id,
                              art,
                              realm,
                              spEntityID,
                              spHandle,
                              idpHandle,
                              inResponseTo,
                              assertionMinorVersion);
        return art;
    }
    
    /**
     * Creates an assertion artifact.
     * @param id session ID
     * @param artifact assertion artifact
     * @param realm the realm under which the entity resides.
     * @param spEntityID service provider's entity ID
     * @param spHandle service provider issued <code>NameIdentifier</code>
     * @param idpHandle identity provider issued <code>NameIdentifier</code>
     * @param inResponseTo value to InResponseTo attribute. It's the request ID.
     * @param assertionMinorVersion minor version the assertion should use
     * @exception FSException,SAMLException if error occurrs
     */
    public FSAssertion createFSAssertion(
        String id,
        AssertionArtifact artifact,
        String realm,
        String spEntityID,
        NameIdentifier spHandle,
        NameIdentifier idpHandle,
        String inResponseTo,
        int assertionMinorVersion)
    throws FSException, SAMLException
    {
        FSUtils.debug.message(
            "FSAssertionManager.createFSAssertion(id): Called");
        // check input
        if ((id == null) ||(spEntityID == null)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager: null input for"
                    + " method createFSAssertion.");
            }
            throw new FSException("nullInput",null);
        }
        
        String destID = spEntityID;
        
        String authMethod = null;
        String authnContextStatementRef = null;
        String authnContextClassRef = null;
        Date authInstant = null;
        String securityDomain = null;
        Object token = null;
        String univId = null;
        SubjectLocality authLocality = null;

        FSSessionManager sessionManager = 
             FSSessionManager.getInstance(metaAlias);

        IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
        Map attributes = new HashMap();
        if (metaManager != null) {
            BaseConfigType idpConfig = null;
            try {
                idpConfig = metaManager.getIDPDescriptorConfig(
                    realm, hostEntityId);
            } catch (IDFFMetaException e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSAssertionManager.createFSAssertion: exception while"
                        + " obtaining idp extended meta:", e);
                }
                idpConfig = null;
            }
            if (idpConfig != null) {
                attributes = IDFFMetaUtils.getAttributes(idpConfig);
            }
        }

        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            token = sessionProvider.getSession(id);
            String[] strAuthInst = null;
            try {
                strAuthInst = sessionProvider.getProperty(
                    token, SessionProvider.AUTH_INSTANT);
            } catch (UnsupportedOperationException ue) {
                if (FSUtils.debug.warningEnabled()) {
                    FSUtils.debug.warning(
                        "FSAssertionManager.createFSAssertion(id):", ue);
                }
            } catch (SessionException se) {
                if (FSUtils.debug.warningEnabled()) {
                    FSUtils.debug.warning(
                        "FSAssertionManager.createFSAssertion(id):", se);
                }
            }
            if ((strAuthInst != null) && (strAuthInst.length >= 1)){
                try {
                    authInstant = DateUtils.stringToDate(strAuthInst[0]);
                } catch(ParseException ex){
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSAssertionManager." + 
                            "createFSAssertion(id): AuthInstant not found" +
                            "in the Token");
                    }
                }
            } else {
                authInstant = new java.util.Date();
            }

            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAssertionManager.createFSAssertion(id):AuthInstant = " 
                    + authInstant);
            }
            try {
                String[] strAuthMethod = sessionProvider.getProperty(
                    token, SessionProvider.AUTH_METHOD);
                if ((strAuthMethod != null) && (strAuthMethod.length >= 1)) {
                    authMethod = strAuthMethod[0];
                }
            } catch (UnsupportedOperationException ue) {
                if (FSUtils.debug.warningEnabled()) {
                    FSUtils.debug.warning(
                        "FSAssertionManager.createFSAssertion(id):", ue);
                }
            } catch (SessionException se) {
                if (FSUtils.debug.warningEnabled()) {
                    FSUtils.debug.warning(
                        "FSAssertionManager.createFSAssertion(id):", se);
                }
            }

            String assertionIssuer = IDFFMetaUtils.getFirstAttributeValue(
                attributes, IFSConstants.ASSERTION_ISSUER);
            if (assertionIssuer == null) {
                assertionIssuer = SystemConfigurationUtil.getProperty(
                        "com.iplanet.am.server.host");
            }

            try {
                String ipAddress = 
                    InetAddress.getByName(assertionIssuer).getHostAddress();
                authLocality = new SubjectLocality(ipAddress, assertionIssuer);
            } catch(UnknownHostException uhe) {
                FSUtils.debug.error("FSAssertionManager.constructor: couldn't"
                    + " obtain the localhost's ipaddress:", uhe);
            }
            
            try {
                FSSession session = sessionManager.getSession(token);
                authnContextClassRef = session.getAuthnContext();
                authnContextStatementRef = authnContextClassRef;
            } catch(Exception ex){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionManager.createFSAssertion"
                        + "(id): AuthnContextStatement for the token is null"
                        + " Assertion will not contain any "
                        + " AuthenticationStatement");
                }
                authnContextStatementRef = null;
            }
            if (authnContextStatementRef != null){
                if (assertionMinorVersion == 
                    IFSConstants.FF_11_ASSERTION_MINOR_VERSION) 
                {
                    authMethod = IFSConstants.AC_XML_NS;
                } else {
                    authMethod = IFSConstants.AC_12_XML_NS;
                }
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAssertionManager.createFSAssertion(id):"
                    + "AuthnContextStatement used for authenticating the user: "
                    +  authnContextStatementRef);
            }
            
            univId = sessionProvider.getPrincipalName(token);
            
            securityDomain = hostEntityId;
        } catch(Exception e) {
            FSUtils.debug.error("FSAssertionManager.createAssertion(id):"
                + " exception retrieving info from the session: ", e);
            throw new FSException(
                "alliance_manager_no_local_descriptor", null, e);
        }
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionManager.createAssertion(id):"
                + " Creating Authentication Assertion for user with"
                + "opaqueHandle= " 
                + spHandle.getName() 
                + " And SecurityDomain= " 
                + securityDomain);
        }
        SubjectConfirmation subConfirmation = null;
        String artString = null;
        if (artifact != null) {
            artString = artifact.getAssertionArtifact();
            if (assertionMinorVersion ==
                IFSConstants.FF_11_ASSERTION_MINOR_VERSION) 
            {
                subConfirmation = new SubjectConfirmation(
                    SAMLConstants.DEPRECATED_CONFIRMATION_METHOD_ARTIFACT);
            } else {
                subConfirmation = new SubjectConfirmation(
                    SAMLConstants.CONFIRMATION_METHOD_ARTIFACT);
            }
            subConfirmation.setSubjectConfirmationData(artString);
        } else {
            // set to bearer for POST profile
            subConfirmation = new SubjectConfirmation(
                SAMLConstants.CONFIRMATION_METHOD_BEARER);
        }
        IDPProvidedNameIdentifier idpNi = null;
        if (assertionMinorVersion == 
                IFSConstants.FF_12_POST_ASSERTION_MINOR_VERSION  ||
            assertionMinorVersion ==
                IFSConstants.FF_12_ART_ASSERTION_MINOR_VERSION) 
        {
            idpNi = new IDPProvidedNameIdentifier(
                    idpHandle.getName(),
                    idpHandle.getNameQualifier(), 
                    spHandle.getFormat());
            idpNi.setMinorVersion(IFSConstants.FF_12_PROTOCOL_MINOR_VERSION);
        } else {
            idpNi = new IDPProvidedNameIdentifier(idpHandle.getNameQualifier(),
                    idpHandle.getName());
        }
        
        FSSubject sub = new FSSubject(spHandle, subConfirmation, idpNi);
        AuthnContext authnContext = new AuthnContext(
            authnContextClassRef, authnContextStatementRef);
        authnContext.setMinorVersion(assertionMinorVersion);
        FSAuthenticationStatement statement = new FSAuthenticationStatement(
            authMethod, authInstant, sub, authLocality, null, authnContext);
        FSSession session = sessionManager.getSession(univId, id);
        
        if (session == null){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager.createAssertion(id): "
                    + "AssertionManager could not find a valid Session for"
                    + "userId: "
                    + univId + " SessionID: " + id);
            }
            return null;
        }
        
        String sessionIndex = session.getSessionIndex();
        if (sessionIndex == null) {
            sessionIndex = SAMLUtils.generateID();
            session.setSessionIndex(sessionIndex);
        }

        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSAssertionManager.createAssertion(id): SessionIndex: " +
                sessionIndex);
        }
        statement.setSessionIndex(sessionIndex);
        
        //setReauthenticateOnOrAfter date
        Date issueInstant = new Date();
        // get this period from the config
        
        FSUtils.debug.message("here before date");
        Date notAfter;
        if (artifact != null) {
            notAfter = new Date(issueInstant.getTime() + artifactTimeout);
        } else {
            notAfter = new Date(issueInstant.getTime() + assertionTimeout);
        }
        FSUtils.debug.message("here after date");
        statement.setReauthenticateOnOrAfter(notAfter);
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionManager.createAssertion(id):"
                + " Authentication Statement: " + statement.toXMLString());
        }
        
        Conditions cond = new Conditions(null, notAfter);
        if ((destID != null) &&(destID.length() != 0)) {
            List targets = new ArrayList();
            targets.add(destID);
            cond.addAudienceRestrictionCondition(
                new AudienceRestrictionCondition(targets));
        }
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionManager.createAssertion(id):"
                + " Authentication Statement: " + statement.toXMLString());
        }

        /**
         * This is added to create an attribute statement for the bootstrap
         * information.
         */
        AttributeStatement attribStatement =  null;
        Advice advice = null;
        String generateBootstrapping = IDFFMetaUtils.getFirstAttributeValue(
            attributes, IFSConstants.GENERATE_BOOTSTRAPPING);
        
        if (assertionMinorVersion != IFSConstants.FF_11_ASSERTION_MINOR_VERSION
            &&
            (generateBootstrapping != null &&
                generateBootstrapping.equals("true")))
        {
            AuthnContext authContext = new AuthnContext(
                null, authnContextStatementRef);
            authContext.setMinorVersion(
                IFSConstants.FF_12_PROTOCOL_MINOR_VERSION);
            try {
                FSDiscoveryBootStrap bootStrap = new FSDiscoveryBootStrap(
                    token, authContext, sub, univId, destID, realm);
                attribStatement = bootStrap.getBootStrapStatement();
                if (bootStrap.hasCredentials()) {
                    advice = bootStrap.getCredentials();
                }
            } catch (Exception e) {
                FSUtils.debug.error("FSAssertionManager.createAssertion(id):"
                    + "exception when generating bootstrapping resource "
                    + "offering:", e);
            }
        }

        AssertionIDReference aID = new AssertionIDReference();
        Set statements = new HashSet();
        statements.add(statement);
        if (attribStatement != null) {
            statements.add(attribStatement);
        }

        String attributePluginImpl = IDFFMetaUtils.getFirstAttributeValue(
            attributes, IFSConstants.ATTRIBUTE_PLUGIN);
        if ((attributePluginImpl != null) && 
            (attributePluginImpl.length() != 0)) 
        {
            try {
                Object pluginClass = 
                    Thread.currentThread().getContextClassLoader().loadClass(
                        attributePluginImpl).newInstance();
                List attribStatements = null;
                if (pluginClass instanceof FSRealmAttributePlugin) {
                    FSRealmAttributePlugin attributePlugin =
                        (FSRealmAttributePlugin)pluginClass;
                    attribStatements =
                        attributePlugin.getAttributeStatements(
                            realm, hostEntityId, destID, sub, token);
                } else if (pluginClass instanceof FSAttributePlugin) {
                    FSAttributePlugin attributePlugin =
                        (FSAttributePlugin)pluginClass;
                    attribStatements =
                        attributePlugin.getAttributeStatements(
                            hostEntityId, destID, sub, token);
                }

                if ((attribStatements != null) && 
                    (attribStatements.size() != 0)) 
                {
                    Iterator iter = attribStatements.iterator();
                    while (iter.hasNext()) {
                        statements.add((AttributeStatement)iter.next()); 
                    }
                }
            } catch (Exception ex) {
                FSUtils.debug.error(
                    "FSAssertion.createAssertion(id):getAttributePlugin:", ex);
            }
        }

        if (IDFFMetaUtils.isAutoFedEnabled(attributes)) {
            AttributeStatement autoFedStatement = 
                FSAttributeStatementHelper.getAutoFedAttributeStatement(
                    realm, hostEntityId, sub, token);
            statements.add(autoFedStatement);
        }

        FSAssertion assertion = new FSAssertion(aID.getAssertionIDReference(), 
                                                hostEntityId, 
                                                issueInstant, 
                                                cond, 
                                                advice,
                                                statements, 
                                                inResponseTo);
        assertion.setMinorVersion(assertionMinorVersion);
        assertion.setID(aID.getAssertionIDReference());
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionManager.createAssertion(id):"
                + " Assertion created successfully: " 
                + assertion.toXMLString());
        }


        String aIDString = assertion.getAssertionID();
        Entry entry = new Entry(assertion, destID, artString, token);
        Integer maxNumber = null; 
        try {
            int temp = Integer.parseInt(IDFFMetaUtils.getFirstAttributeValue(
                attributes, IFSConstants.ASSERTION_LIMIT));
            maxNumber = new Integer(temp);
        } catch(Exception ex){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager.createAssertion(id):"
                    + " Assertion MAX number configuration not found in "
                    + "FSConfig. Using Default");
            }
            maxNumber = null;
        }
        if (maxNumber == null) {
            maxNumber = new Integer(IFSConstants.ASSERTION_MAX_NUMBER_DEFAULT);
        }
        
        int maxValue = maxNumber.intValue();
        if ((maxValue != 0) &&(idEntryMap.size() > maxValue)) {
            FSUtils.debug.error("FSAssertionManager.createAssertion: "
                + "reached maxNumber of assertions.");
            throw new FSException("errorCreateAssertion", null);
        }
        Object oldEntry = null;
        try {
            synchronized(idEntryMap) {
                oldEntry = idEntryMap.put(aIDString, entry);
            }
            if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
                idffSvc.setAssertions((long)idEntryMap.size());
            }
        } catch(Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager: couldn't add "
                    + "to idEntryMap.", e);
            }
            throw new FSException("errorCreateAssertion",null);
        }
        if (LogUtil.isAccessLoggable(Level.FINER)) {
            String[] data = { assertion.toString() };
            LogUtil.access(Level.FINER,LogUtil.CREATE_ASSERTION,data, token);
        } else {
            String[] data = { assertion.getAssertionID() } ;
            LogUtil.access(Level.INFO,LogUtil.CREATE_ASSERTION,data, token);
        }
        if (artString != null) {
            try {
                synchronized(artIdMap) {
                    oldEntry = artIdMap.put(artString, aIDString);
                }
                if ((agent != null) && agent.isRunning() && (idffSvc != null)){
                    idffSvc.setArtifacts((long)artIdMap.size());
                }
            } catch(Exception e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionManager: couldn't add "
                    + "artifact to the artIdMap.", e);
                }
                throw new FSException("errorCreateArtifact",null);
            }
            if (oldEntry != null) {
                artifactTimeoutRunnable.removeElement(aIDString);
            }
            artifactTimeoutRunnable.addElement(aIDString);
        } else {
            if (oldEntry != null) {
                assertionTimeoutRunnable.removeElement(aIDString);
            }
            assertionTimeoutRunnable.addElement(aIDString);
        }
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionManager.createAssertion(id):"
                + " Returning Assertion: " + assertion.toXMLString());
        }
        return assertion;
    }
    
    /**
     * Retrieves the assertion associated with an artifact.
     * @param artifact assertion artifact
     * @param destID destination ID of the site who sent the request
     * @return assertion associated with the artifact
     * @exception FSException if the assertion could not be retrieved
     */
    public Assertion getAssertion(
        AssertionArtifact artifact,
        String destID
    ) throws FSException 
    {
        if ((artifact == null) ||
            (destID == null || destID.length() == 0))
        {
            FSUtils.debug.message("FSAssertionManager: input is null.");
            throw new FSException("nullInput",null);
        }
        String artString = artifact.getAssertionArtifact();
        // get server id.
        String remoteUrl = SAMLUtils.getServerURL(
                                        artifact.getAssertionHandle());
        if (remoteUrl != null) { // not this server
            // call AssertionManagerClient.getAssertion
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("AssertionManager.getAssertion(art, " +
                    "destid: calling another server in lb site:" + remoteUrl);
            }
            FSAssertionManagerClient amc = new FSAssertionManagerClient(
                metaAlias, getFullServiceURL(remoteUrl));
            return amc.getAssertion(artifact, destID);
        } // else 
        
        String aIDString = null;
        
        try {
            aIDString = (String) artIdMap.get(artString);
            if (aIDString == null) {
                throw new FSException("nullInput",null);
            }
        } catch(Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager.getAssertion(art, de"
                    + "stid): no AssertionID found corresponding to artifact.");
            }
            throw new FSException("noMatchingAssertion",null);
        }
        
        Entry entry = null;
        try {
            entry =(Entry) idEntryMap.get(aIDString);
            if (entry == null) {
                throw new FSException("nullEntry",null);
            }
        } catch(Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager.getAssertion(art, de"
                + "stid): no Entry found corresponding to artifact.");
            }
            throw new FSException("noMatchingAssertion", null);
        }

        // check the destination id
        String dest = entry.getDestID();
        if (dest == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager.getAssertion(art, de"
                    + "stid): no destID found corresponding to artifact.");
            }
            throw new FSException("noDestIDMatchingArtifact",null);
        }
        if (!dest.equals(destID)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager.getAssertion(art, de"
                    + "stid): destinationID doesn't match.");
            }
            throw new FSException("destIDNotMatch",null);
        }
        
        synchronized(artIdMap) {
            artIdMap.remove(artString);
        }
        if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
            idffSvc.setArtifacts((long)artIdMap.size());
        }
        artifactTimeoutRunnable.removeElement(aIDString);
        synchronized(idEntryMap) {
            idEntryMap.remove(aIDString);
        }
        if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
            idffSvc.setAssertions((long)idEntryMap.size());
        }
        assertionTimeoutRunnable.removeElement(aIDString);
       
        Assertion assertion = entry.getAssertion();
        if (assertion == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager.getAssertion(art, de"
                    + "stid): no Assertion found corresponding to aID.");
            }
            throw new FSException("noMatchingAssertion",null);
        }

        if (!assertion.isTimeValid()) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionManager: assertion "
                    + aIDString + " is expired.");
            }
            throw new FSException("assertionTimeNotValid",null);
        }
        return assertion;
    }
    
    /**
     * Finds the destination id for whom the artifact is issued for.
     * @param artifact assertion artifact
     * @return destination id
     * @exception FSException if error occurrs
     */
    public String getDestIdForArtifact(
        AssertionArtifact artifact
    ) throws FSException
    {
        FSUtils.debug.message(
            "FSAssertionManager.getDestIdForArtifact: Called");
        String artString = artifact.getAssertionArtifact();
        
        // get server id.
        String remoteUrl = SAMLUtils.getServerURL(
                                        artifact.getAssertionHandle());
        if (remoteUrl != null) { // not this server
            // call FSAssertionManagerClient.getAssertion
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "AssertionManager.getDestIdForArtifact(art, " +
                    "destid: calling another server in lb site:" + remoteUrl);
            }
            FSAssertionManagerClient amc = new FSAssertionManagerClient(
                metaAlias, getFullServiceURL(remoteUrl));
            return amc.getDestIdForArtifact(artifact);
        } // else 
        
        String aIDString = null;
        
        try {
            aIDString =(String) artIdMap.get(artString);
            if (aIDString == null) {
                throw new FSException("nullInput",null);
            }
        } catch(Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAssertionManager.getDestIdForArtifact :" +
                    "no AssertionID found corresponding to artifact.");
            }
            throw new FSException("noMatchingAssertion",null);
        }
        
        Entry entry = null;
        try {
            entry =(Entry) idEntryMap.get(aIDString);
            if (entry == null) {
                throw new FSException("nullEntry",null);
            }
        } catch(Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAssertionManager.getDestIdForArtifact: " +
                    "no Entry found corresponding to artifact.");
            }
            throw new FSException("noMatchingAssertion",null);
        }
        
        String dest = entry.getDestID();
        if (dest == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAssertionManager.getDestIdForArtifact: " +
                    "no destID found corresponding to artifact.");
            }
            throw new FSException("noDestIDMatchingArtifact",null);
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSAssertionManager.getDestIdForArtifact: " +
                "Destination ProviderID found for Artifact: " + dest);
        }
        return dest;
    }

    private String getFullServiceURL(String shortUrl) {
        String result = null;
        try {
            URL u = new URL(shortUrl);
            URL weburl = SystemConfigurationUtil.getServiceURL(
                SERVICE_NAMING, u.getProtocol(), u.getHost(), u.getPort(),
                u.getPath());
            result = weburl.toString();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "AssertionManager.getFullServiceURL:full remote URL is: " +
                    result);
            }
        } catch (Exception e) {
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning(
                    "AssertionManager.getFullServiceURL:Exception:", e);
            }
        }
        return result;
    }
    
    /**
     * Specific assertion class containing date and status.
     * This is used to store information about a fault artifact.
     */
    class ErrorAssertion extends Assertion {
        private Status _status;
        public ErrorAssertion( java.util.Date issue, Status status ) {
            _issueInstant = issue;
            _status = status;
        }
        public Status getStatus() {
            return _status;
        }
        
    }
    
    /**
     * Store the status of a given artifact (original error)
     * @param aa reference artifact
     * @param s stored status
     */
    public void setErrStatus( AssertionArtifact aa, Status s ) {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("setErrStatus( " + aa + ", " + s + " )" );
        }
        
        String artString = aa.getAssertionArtifact();
        Assertion assertion = new ErrorAssertion( new java.util.Date(), s );
        Entry e = new Entry( assertion, null, artString, null );
        Object oldEntry = null;
        synchronized (idEntryMap) {
            oldEntry = idEntryMap.put( artString, e );
        }
        if (oldEntry != null) {
            assertionTimeoutRunnable.removeElement(artString);
            if ((agent != null) && agent.isRunning() && (idffSvc != null)) {
                idffSvc.setAssertions((long)idEntryMap.size());
            }
        }
        assertionTimeoutRunnable.addElement(artString);
    }
    /**
     * Retrieve the original status of a reference artifact.
     * @param aa reference artifact
     * @return The status as originally recorded.
     */
    public Status getErrorStatus( AssertionArtifact aa ) {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("getErrorStatus( " + aa + " )" );
        }
        Entry e = null;
        Status s = null;
        String remoteUrl = SAMLUtils.getServerURL(
                aa.getAssertionHandle());
        if (remoteUrl != null) { // not this server
            // call AssertionManagerClient.getAssertion
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("AssertionManager.getAssertion(art, " 
                        + "destid: calling another server in lb site:" 
                        + remoteUrl);
            }
            try {
                FSAssertionManagerClient amc = new FSAssertionManagerClient(
                    metaAlias, getFullServiceURL(remoteUrl));
                s = amc.getErrorStatus( aa );
            } catch ( FSException fse ) {
                FSUtils.debug.error( fse.getMessage() );
            }
        } else { 
            e = (Entry) idEntryMap.get( aa.getAssertionArtifact() );
            if ( null != e ) {
                Assertion assertion = e.getAssertion();
                if ( assertion instanceof ErrorAssertion ) {
                    ErrorAssertion eassert = (ErrorAssertion) assertion;
                    s = eassert.getStatus();
                }
            }
        }
        return s;
    }
}
