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
 * $Id: AssertionManager.java,v 1.13 2010/01/09 19:41:06 qcheng Exp $
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 */

package com.sun.identity.saml;

import static org.forgerock.openam.utils.Time.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.net.InetAddress;
import org.w3c.dom.Element;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.PeriodicGroupRunnable;
import com.sun.identity.common.ScheduleableGroupAction;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TaskRunnable;
import com.sun.identity.common.TimerPool;
import com.sun.identity.saml.assertion.*;
import com.sun.identity.saml.protocol.*;
import com.sun.identity.saml.common.*;
import com.sun.identity.saml.plugins.*;
import com.sun.identity.saml.xmlsig.*;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.stats.Stats;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.StringTokenizer;
import com.sun.identity.plugin.monitoring.FedMonAgent;
import com.sun.identity.plugin.monitoring.FedMonSAML1Svc;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The class <code>AssertionManager</code> is a <code>final</code> class
 * that provides interfaces to create, get and destroy <code>Assertion</code>s.
 * <p>
 * It is a singleton class; an instance of this class can be obtained by
 * calling <code>AssertionManager.getInstance()</code>.
 * <p>
 * Having obtained an instance of <code>AssertionManager</code>, its methods
 * can be called to create/get <code>Assertion</code>, and 
 * <code>AssertionArtifact</code>, and to obtain decision from an 
 * <code>Query</code>.
 * <p>
 * This class could only be used in the same JVM as OpenAM.
 * @supported.api
 */
public final class AssertionManager {

    // General stats class
    public static Stats assStats;
    public static Stats artStats;
    
    // Assertion Statistics Class
    private static AssertionStats assertionStats;
    
    // Artifact Statistics Class
    private static ArtifactStats artifactStats;
    
    private static final String SUPER_USER =
        "com.sun.identity.authentication.super.user";
    private static String superUser = null;
    private static SessionProvider sessionProvider = null;
    
    private static long cleanUpInterval;
    private static long assertionTimeout;
    private static long artifactTimeout;
    private static long notBeforeSkew;

    private static FedMonAgent agent;
    private static FedMonSAML1Svc saml1Svc;
    
    static {
        assStats = Stats.getInstance("amAssertionMap");
        artStats = Stats.getInstance("amArtifactMap");
        superUser = SystemConfigurationUtil.getProperty(SUPER_USER);
        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            SAMLUtils.debug.error("Static: Couldn't get SessionProvider.", 
                se);
            sessionProvider = null; 
        }
        cleanUpInterval = ((Integer) SAMLServiceManager.getAttribute(
            SAMLConstants.CLEANUP_INTERVAL_NAME)).intValue() * 1000;
        artifactTimeout = ((Integer) SAMLServiceManager.getAttribute(
            SAMLConstants.ARTIFACT_TIMEOUT_NAME)).intValue() * 1000;
        assertionTimeout = ((Integer) SAMLServiceManager.getAttribute(
            SAMLConstants.ASSERTION_TIMEOUT_NAME)).intValue() * 1000;
        notBeforeSkew = ((Integer) SAMLServiceManager.getAttribute(
            SAMLConstants.NOTBEFORE_TIMESKEW_NAME)).intValue() * 1000;

        agent = MonitorManager.getAgent();
        saml1Svc = MonitorManager.getSAML1Svc();
    }
    
    // Singleton instance of AssertionManager
    private static AssertionManager instance = null;

    // used to store artifact to assertionID mapping
    private static Map artEntryMap = null;
    // used to store assertionIDString to entry mapping
    private static Map idEntryMap = null;
    
    private static TaskRunnable assertionTimeoutRunnable;
    private static TaskRunnable artifactTimeoutRunnable;
    private static TaskRunnable goThroughRunnable;

    private static String assertionVersion = null; 
    private static String protocolVersion = null; 
    

    private class Entry {
        private String destID = null;
        private String artString = null;
        private Object token = null;
        private Assertion assertion = null;

        public Entry(Assertion assertion, String destID, String artString,
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

        public void setDestID(String newDestID) {
            destID = newDestID;
        }

        public String getArtifactString() {
            return artString;
        }

        public void setArtifactString(String newArtifactString) {
            artString = newArtifactString;
        }

        public Object getSSOToken() {
            return token;
        }
    }

    private class ArtEntry {
        private String aID = null;
        private long expiretime = 0;

        public ArtEntry(String aID, long expiretime) {
            this.aID = aID;
            this.expiretime = expiretime;
        }

        public String getAssertionID() {
            return aID;
        }

        public long getExpireTime() {
            return expiretime;
        }
    }

    /**
     * Default Constructor
     */
    private AssertionManager() {
        idEntryMap = new HashMap();
        artEntryMap = new HashMap();
        try { 
            assertionVersion = SystemConfigurationUtil.getProperty(
                SAMLConstants.SAML_ASSERTION_VERSION); 
            protocolVersion = SystemConfigurationUtil.getProperty(
                SAMLConstants.SAML_PROTOCOL_VERSION); 
        } catch (Exception e) { 
            assertionVersion = SAMLConstants.ASSERTION_VERSION_1_0; 
            protocolVersion = SAMLConstants.PROTOCOL_VERSION_1_0; 
        }
        TimerPool timerPool = SystemTimerPool.getTimerPool();
        ScheduleableGroupAction assertionTimeoutAction = new
            ScheduleableGroupAction() {
            public void doGroupAction(Object obj) {
                deleteAssertion((String) obj, null);
            }
        };
        
        assertionTimeoutRunnable = new PeriodicGroupRunnable(
            assertionTimeoutAction, cleanUpInterval, assertionTimeout, true);
        timerPool.schedule(assertionTimeoutRunnable, new Date(((
                currentTimeMillis() + cleanUpInterval) / 1000) * 1000));
        
        ScheduleableGroupAction artifactTimeoutAction = new
            ScheduleableGroupAction() {
            public void doGroupAction(Object obj) {
                deleteAssertion(null, (String) obj);
            }
        };
        
        artifactTimeoutRunnable = new PeriodicGroupRunnable(
            artifactTimeoutAction, cleanUpInterval, artifactTimeout, true);
        
        timerPool.schedule(artifactTimeoutRunnable, new Date(((
                currentTimeMillis() + cleanUpInterval) / 1000) * 1000));
        
        goThroughRunnable = new GoThroughRunnable(cleanUpInterval);
        
        timerPool.schedule(goThroughRunnable, new Date(((
                currentTimeMillis() + cleanUpInterval) / 1000) * 1000));
        
        if (assStats.isEnabled()) {
            artifactStats = new ArtifactStats(artEntryMap);
            artStats.addStatsListener(artifactStats);
            assertionStats = new AssertionStats(idEntryMap);
            assStats.addStatsListener(assertionStats);
        }

    }

    /**
     * Gets the singleton instance of <code>AssertionManager</code>.
     * @return The singleton <code>AssertionManager</code> instance
     * @throws SAMLException if unable to get the singleton
     *         <code>AssertionManager</code> instance.
     * @supported.api
     */
    public static AssertionManager getInstance() throws SAMLException {
        // not throwing any exception
        if (instance == null) {
            synchronized (AssertionManager.class) {
                if (instance == null) {
                    if (SAMLUtils.debug.messageEnabled() ) {
                        SAMLUtils.debug.message("Constructing a new instance"
                                + " of AssertionManager");
                    }
                    instance = new AssertionManager();
                }
            }
        }
        return instance;
    }

    /** 
     * This method creates an Assertion that contains an
     * <code>AuthenticationStatement</code>.
     * @param token user's session object that contains authentication
     *     information which is needed to create the 
     *     <code>AuthenticationStatement</code>. 
     * @return Assertion The created Assertion.
     * @throws SAMLException If the Assertion cannot be created.
     * @supported.api
     */
    public Assertion createAssertion(Object token)
        throws SAMLException
    {
        if (assertionVersion.equals(SAMLConstants.ASSERTION_VERSION_1_0)) {
            return createAssertion(token, null,
                SAMLConstants.DEPRECATED_CONFIRMATION_METHOD_ARTIFACT, 0);
        } else if(assertionVersion.equals(
            SAMLConstants.ASSERTION_VERSION_1_1)) {
            return createAssertion(token, null,
                SAMLConstants.CONFIRMATION_METHOD_ARTIFACT, 1);
        } else { 
            throw new SAMLException(SAMLUtils.bundle.getString(
                "assertionVersionNotSupport"));
        }
    } 

    /**
     * This method creates an Assertion that contains an 
     * <code>AuthenticationStatement</code> and 
     * an <code>AttributeStatement</code>.
     * @param token User' session object that contains authentication
     *     information which is needed to create the 
     *     <code>AuthenticationStatement</code> for the Assertion.
     * @param attributes A list of Attribute objects which are used to
     *     create the <code>AttributeStatement</code> for the Assertion.
     * @return Assertion The created Assertion.
     * @throws SAMLException If the Assertion cannot be created.
     * @supported.api
     */
    public Assertion createAssertion(Object token, List attributes)
        throws SAMLException
    {   if (assertionVersion.equals(SAMLConstants.ASSERTION_VERSION_1_0)) {
            return createAssertion(token, attributes,
                SAMLConstants.DEPRECATED_CONFIRMATION_METHOD_ARTIFACT, 0);
        } else if (assertionVersion.equals(
            SAMLConstants.ASSERTION_VERSION_1_1)) {
            return createAssertion(token, attributes,
                SAMLConstants.CONFIRMATION_METHOD_ARTIFACT, 1);
        } else { 
            throw new SAMLException(SAMLUtils.bundle.getString(
                        "assertionVersionNotSupport"));
        }
    } 

    private Assertion createAssertion(Object token, List attributes,
        String confirmationMethod, int minorVersion)
        throws SAMLException
    {
        if (token == null) {
            SAMLUtils.debug.error("AssertionManager.createAssertion(id):"
                + "input Session is null.");
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }
        if (sessionProvider == null) {
            throw new SAMLException(SAMLUtils.bundle.getString(
                "nullSessionProvider"));
        }
        String id = sessionProvider.getSessionID(token);
        return createAssertion(id, null, null, attributes, 
             confirmationMethod, minorVersion, null);
    }

    /**
     * This method creates an <code>AssertionArtifact</code> for the given
     * Assertion.
     *
     * @param assertion The Assertion for which an Artifact needs to be created.
     * @param destID The <code>sourceID</code> of the site for which the 
     *        <code>AssertionArtifact</code> is created. It is in raw String
     *        format (not Base64 encoded, for example.) This String can be
     *        obtained from converting the 20 bytes sequence to char Array, then
     *        from the char Array to String.
     * @return <code>AssertionArtifact</code>
     * @throws SAMLException If the <code>AssertionArtifact</code> cannot be
     *         created.
     * @supported.api
     */
    public AssertionArtifact createAssertionArtifact(Assertion assertion,
                                                        String destID)
        throws SAMLException
    {
        if ((assertion == null) || (destID == null) || (destID.length() == 0)) {
            SAMLUtils.debug.error("AssertionManager.createAssertionArti"
                    + "fact(Assertion, String): null input.");
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }

        Map partner = (Map) SAMLServiceManager.getAttribute(
                                                SAMLConstants.PARTNER_URLS);
        if ((partner == null) || (!partner.containsKey(destID))) {
            SAMLUtils.debug.error("AssertionManager.createAssertionArtifact:" +
                "(Assertion, String): destID not in partner list.");
            throw new SAMLException(
                        SAMLUtils.bundle.getString("destIDNotFound"));
        }

        String handle = SAMLUtils.generateAssertionHandle();
        if (handle == null) {
            SAMLUtils.debug.error("AssertionManager.createAssertion"
                        + "Artifact(Assertion,String): couldn't generate "
                        + "assertion handle.");
            throw new SAMLResponderException(
                SAMLUtils.bundle.getString("errorCreateArtifact"));
        }
        String sourceID = (String) SAMLServiceManager.getAttribute(
                                        SAMLConstants.SITE_ID);
        AssertionArtifact art = new AssertionArtifact(sourceID, handle);
        String artString = art.getAssertionArtifact();
        String aID = assertion.getAssertionID();
        Entry entry = (Entry) idEntryMap.get(aID);
        if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
            saml1Svc.incSAML1Cache(FedMonSAML1Svc.ASSERTIONS,
                FedMonSAML1Svc.CREAD);
        }
        if ((entry == null) && !validateNumberOfAssertions(idEntryMap)) {
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ASSERTIONS,
                    FedMonSAML1Svc.CMISS);
            }
            entry = new Entry(assertion, destID, artString, null);
            try {
                synchronized (idEntryMap) {
                    idEntryMap.put(aID, entry);
                }
                if ((agent != null) &&
                    agent.isRunning() &&
                    (saml1Svc != null))
                {
                        saml1Svc.incSAML1Cache(
                        FedMonSAML1Svc.ASSERTIONS,
                        FedMonSAML1Svc.CWRITE);
                }
                goThroughRunnable.addElement(aID);
            } catch (Exception e) {
                SAMLUtils.debug.error("AssertionManager.createAssertion"
                        + "Artifact(Assertion,String): couldn't add to "
                        + "idEntryMap." + e);
                throw new SAMLResponderException(
                        SAMLUtils.bundle.getString("errorCreateArtifact"));
            }
            if (LogUtils.isAccessLoggable(java.util.logging.Level.FINER)) {
                String[] data = {SAMLUtils.bundle.getString("assertionCreated"),
                    assertion.toString(true, true)};
                LogUtils.access(java.util.logging.Level.FINER, 
                    LogUtils.ASSERTION_CREATED, data);
            } else {
                String[] data = {SAMLUtils.bundle.getString("assertionCreated"),
                    assertion.getAssertionID()};
                LogUtils.access(java.util.logging.Level.INFO, 
                    LogUtils.ASSERTION_CREATED, data);
            }
        } else {
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ASSERTIONS,
                    FedMonSAML1Svc.CHIT);
            }
            String preArtString = entry.getArtifactString();
            if (preArtString != null) {
                if ((agent != null) &&
                    agent.isRunning() &&
                    (saml1Svc != null))
                {
                        saml1Svc.incSAML1Cache(
                        FedMonSAML1Svc.ARTIFACTS,
                        FedMonSAML1Svc.CREAD);
                }
            
                if (artEntryMap.containsKey(preArtString)) {
                    if ((agent != null) &&
                        agent.isRunning() &&
                        (saml1Svc != null))
                    {
                            saml1Svc.incSAML1Cache(
                            FedMonSAML1Svc.ARTIFACTS,
                            FedMonSAML1Svc.CHIT);
                    }
                    SAMLUtils.debug.error("AssertionManager.createAssertion"
                        + "Artifact(Asssertion, String): Artifact exists for "
                        + "the assertion.");
                    throw new SAMLResponderException(
                        SAMLUtils.bundle.getString("errorCreateArtifact"));
                } else {
                    if ((agent != null) &&
                        agent.isRunning() &&
                        (saml1Svc != null))
                    {
                            saml1Svc.incSAML1Cache(
                            FedMonSAML1Svc.ARTIFACTS,
                            FedMonSAML1Svc.CMISS);
                    }
                }
            }
            entry.setDestID(destID);
            entry.setArtifactString(artString);
        }
        // add to artEntry map
        try {
            Object oldEntry = null;
            synchronized (artEntryMap) {
                oldEntry = artEntryMap.put(artString, new
                    ArtEntry(aID, currentTimeMillis() +
                        artifactTimeout));
            }
            if (oldEntry != null) {
                artifactTimeoutRunnable.removeElement(artString);
            }
            artifactTimeoutRunnable.addElement(artString);
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ARTIFACTS,
                    FedMonSAML1Svc.CWRITE);
            }
        } catch (Exception e) {
            SAMLUtils.debug.error("AssertionManager.createAssertionArt"
                    + "fact(Assertion,String): couldn't add artifact to the "
                    + "artEntryMap", e);
            throw new SAMLResponderException(
                SAMLUtils.bundle.getString("errorCreateArtifact"));
        }
        String[] data = {SAMLUtils.bundle.getString("assertionArtifactCreated"),
                         artString, aID};
        LogUtils.access(java.util.logging.Level.INFO,
                        LogUtils.ASSERTION_ARTIFACT_CREATED, data);
        return art;
    }

    /**
     * This method gets all valid Assertions managed by this 
     * <code>AssertionManager</code>.
     * @param token User's session object which is allowed to get all
     *     Assertion.
     * @return A Set of valid Assertion IDs. Each element in the Set is a
     * String representing an Assertion ID. 
     * @throws SAMLException If this method can not gets all valid Assertions.
     * @supported.api
     */
    public Set getAssertions(Object token)
        throws SAMLException
    {
        if (token == null) {
            SAMLUtils.debug.error("AssertionManager.getAssertions(Object"
                + "): input session is null.");
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }
        
        if (!isSuperUser(token)) {
            SAMLUtils.debug.error("AssertionManager.getAssertions(Object"
                + "): Session doesn't have the privilege.");
            throw new SAMLException(SAMLUtils.bundle.getString("noPrivilege"));
        }
        
        return idEntryMap.keySet();
    }

    private boolean isSuperUser(Object token) {
        try {
            if (sessionProvider == null) {
                SAMLUtils.debug.error("SessionProvider is null.");
                return false;
            }
            String userID = (String) sessionProvider.getProperty(token, 
                Constants.UNIVERSAL_IDENTIFIER)[0];
            if (superUser != null && superUser.length() > 0) {
                return superUser.equalsIgnoreCase(userID);
            }
        } catch (Exception e) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(
                    "AssertionManager.isSuperUser:Exception: ", e);
            }
        }
        return false;
    }

    /**
     * This method gets the Assertion based on the Assertion ID.
     * @param id The Assertion ID.
     * @return An Assertion identified by the Assertion ID.
     * @throws SAMLException If this method can not get the Assertion.
     */
    public Assertion getAssertion(String id)
        throws SAMLException
    {
        if ((id == null) || (id.length() == 0)) {
            SAMLUtils.debug.error("AssertionManager.getAssetion(String): "
                        + "id is null.");
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }
        AssertionIDReference idRef = new AssertionIDReference(id);
        return getAssertion(idRef, null, false);
    }

    /**
     * This method gets all valid <code>AssertionArtifacts</code>
     * managed by this <code>AssertionManager</code>.
     *
     * @param token User's session object which is allowed to get all
     *     <code>AssertionArtifacts</code>.
     * @return A Set of valid <code>AssertionArtifacts</code>. Each element in
     *     the Set is an <code>AssertionArtifacts</code> object representing
     *     an artifact.
     * @throws SAMLException If this method can not gets all valid 
     *     <code>AssertionArtifacts</code>.
     * @supported.api
     */
    public Set getAssertionArtifacts(Object token)
        throws SAMLException
    {
        if (token == null) {
            SAMLUtils.debug.error("AssertionManager.getAssertionArtifacts(" + 
                "Object token): input token is null.");
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }
        
        if (!isSuperUser(token)) {
            SAMLUtils.debug.error("AssertionManager.getAssertionArtifacts(" + 
                "Object token): Session doesn't have the privilege.");
            throw new SAMLException(SAMLUtils.bundle.getString("noPrivilege"));
        }
        
        return artEntryMap.keySet();
    }
 
    /**
     * Returns Assertion that contains <code>AuthenticationStatement</code>.
     * @param id The String that contains authentication information which
     *          is needed to create the assertion. It could be a string
     *          representation of an id, a cookie, etc.
     * @param artifact the value to be set in the SubjectConfirmation of the
     *        <code>AuthenticationStatement</code>. If it's null, 
     *        <code>SubjectConfirmation</code> is set to bearer.
     * @param destID A String that is the site the assertion is created for.
     * @param targetUrl A URL String representing the target site 
     * @param version The relying party preferred Assertion version number. 
     * @return Assertion The created Assertion.
     * @throws SAMLException If the Assertion cannot be created.
     */
    public Assertion createSSOAssertion(String id, AssertionArtifact artifact,
        String destID, String targetUrl, String version)
        throws SAMLException {
        return createSSOAssertion(id, artifact, null, null, destID,
            targetUrl, version);
    }
     
    /**
     * Returns Assertion that contains <code>AuthenticationStatement</code>.
     * @param id The String that contains authentication information which
     *          is needed to create the assertion. It could be a string
     *          representation of an id, a cookie, etc.
     * @param artifact the value to be set in the SubjectConfirmation of the
     *        <code>AuthenticationStatement</code>. If it's null, 
     *        <code>SubjectConfirmation</code> is set to bearer.
     * @param request The HttpServletRerquest object of the request.
     * @param response The HttpServletResponse object.
     * @param destID A String that is the site the assertion is created for.
     * @param targetUrl A URL String representing the target site 
     * @param version The relying party preferred Assertion version number. 
     * @return Assertion The created Assertion.
     * @throws SAMLException If the Assertion cannot be created.
     */
    public Assertion createSSOAssertion(String id, AssertionArtifact artifact,
        HttpServletRequest request, HttpServletResponse response,
        String destID, String targetUrl, String version)
        throws SAMLException
    {
        List attributes = null;
        Map partnerURLs = (Map) SAMLServiceManager.getAttribute(
                                        SAMLConstants.PARTNER_URLS);
        SAMLServiceManager.SOAPEntry partnerEntry = 
                (SAMLServiceManager.SOAPEntry)partnerURLs.get(destID);

        if (partnerEntry != null) {
            try {
                if (sessionProvider == null) {
                    throw new SAMLException(SAMLUtils.bundle.getString(
                        "nullSessionProvider"));
                }    
                Object userSession = sessionProvider.getSession(id);   
                ConsumerSiteAttributeMapper cMapper =
                    partnerEntry.getConsumerSiteAttributeMapper();
                if (cMapper != null) {
                    attributes = cMapper.getAttributes(userSession,
                        request, response, targetUrl);
                } else {
                    PartnerSiteAttributeMapper pMapper =
                        partnerEntry.getPartnerSiteAttributeMapper();
                    if (pMapper != null) {
                        attributes = pMapper.getAttributes(
                            userSession, targetUrl);
                    } else {
                        SiteAttributeMapper mapper =
                            partnerEntry.getSiteAttributeMapper();
                        if (mapper != null) {
                            attributes = mapper.getAttributes(userSession);
                        }
                    }
                }
            } catch ( SessionException ssoe) {
                SAMLUtils.debug.error("AssertionManager.createAssertion(id):"
                    + " exception retrieving info from the Session", ssoe);
                return null;
            }
        }
   
        String nameIDFormat = request.getParameter(
            SAMLConstants.NAME_ID_FORMAT);
        if (artifact == null) {
            // SAML post profile 
            if (version.equals(SAMLConstants.ASSERTION_VERSION_1_1)) {
                // set minor version to 1
                return createAssertion(id, artifact, destID, attributes,
                     SAMLConstants.CONFIRMATION_METHOD_BEARER, 1, nameIDFormat);
            } else {
                // set minor version to 0 
                return createAssertion(id, artifact, destID, attributes,
                     SAMLConstants.CONFIRMATION_METHOD_BEARER, 0, nameIDFormat);
            }
        } else {
            if(version == null || version.equals(
                SAMLConstants.ASSERTION_VERSION_1_0)) {
                return createAssertion(id, artifact, destID, attributes, 
                    SAMLConstants.DEPRECATED_CONFIRMATION_METHOD_ARTIFACT, 0,
                    nameIDFormat); 
            } else if (version.equals(SAMLConstants.ASSERTION_VERSION_1_1)) { 
                return createAssertion(id, artifact, destID, attributes, 
                    SAMLConstants.CONFIRMATION_METHOD_ARTIFACT, 1,
                    nameIDFormat);
            } else {
                SAMLUtils.debug.error("Input version " + version + 
                                       " is not supported.");
                return null; 
            }
        }
    }

    private Assertion createAssertion(String id, AssertionArtifact artifact,
        String destID, List attributes, String confirmationMethod,
        int minorVersion, String nameIDFormat) throws SAMLException 
    {
        // check input
        if ((id == null) || (id.length() == 0)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.createAssertion(id):"
                        + "null input.");
            }
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }
        validateNumberOfAssertions(idEntryMap);
        String authMethod = null;
        Date authInstant = null;
        Object token = null;
        String clientIP = null;
        try {
            if (sessionProvider == null) {
                throw new SAMLException(SAMLUtils.bundle.getString(
                    "nullSessionProvider"));
            }
            token = sessionProvider.getSession(id);
            authMethod = (String) sessionProvider.getProperty(
                token, SessionProvider.AUTH_METHOD)[0];
            String authSSOInstant = (String)
                sessionProvider.getProperty(token,"authInstant")[0]; 
            if (authSSOInstant == null || authSSOInstant.equals("")) {
                authInstant = newDate();
            } else {
                authInstant = DateUtils.stringToDate(authSSOInstant);
            }                

            try {
                InetAddress clientIPAddress =
                    InetAddress.getByName(sessionProvider.getProperty(
                    token,"Host")[0]);
                clientIP = clientIPAddress.getHostAddress();
            } catch (Exception e) {
                // catching exception here since client ip is optional
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager." + 
                        "createAssertion(id):" +
                        "exception when obtaining client ip: ", e);
                }
            }
        } catch (Exception e) {
            SAMLUtils.debug.error("AssertionManager." +
                "createAssertion(id):" +
                " exception retrieving info from the Session: ", e);
            return null;
        }

        Map partnerURLs =
            (Map) SAMLServiceManager.getAttribute(SAMLConstants.PARTNER_URLS);
        SAMLServiceManager.SOAPEntry partnerEntry =
            (SAMLServiceManager.SOAPEntry)partnerURLs.get(destID);

        NameIdentifierMapper niMapper = null;
        if (partnerEntry != null) {
            niMapper = partnerEntry.getNameIdentifierMapper();
        }
        if (niMapper == null) {
            niMapper = new DefaultNameIdentifierMapper();
        }

        String srcID =
            (String)SAMLServiceManager.getAttribute(SAMLConstants.SITE_ID);

        NameIdentifier ni = niMapper.getNameIdentifier(token, srcID, destID, nameIDFormat);
        if (ni == null) {
            SAMLUtils.debug.error("AssertionManager.createAssertion(id): " +
                                  "name identifier is null.");
            return null;
        }

        SubjectConfirmation subConfirmation = null;
        String artString = null;
        if ((confirmationMethod != null) && (confirmationMethod.length() > 0)) {
            subConfirmation = new SubjectConfirmation(confirmationMethod);
        } else {
            if (artifact != null) {
                if (minorVersion == 0) {
                    // set default for SAML Artifact profile 
                    // here, we use SAML 1.0 confirmation method as default.
                    confirmationMethod = 
                        SAMLConstants.DEPRECATED_CONFIRMATION_METHOD_ARTIFACT;
                } else {
                    confirmationMethod = 
                        SAMLConstants.CONFIRMATION_METHOD_ARTIFACT;
                }
                subConfirmation = new SubjectConfirmation(confirmationMethod);
            } else {
                // set to bearer for POST profile
                subConfirmation = new SubjectConfirmation(
                                SAMLConstants.CONFIRMATION_METHOD_BEARER);
            }
        }

        if (artifact != null) { 
            artString = artifact.getAssertionArtifact(); 
        }
        Subject sub = new Subject(ni, subConfirmation);
        SubjectLocality subjLocality = null;
        if ((clientIP != null) && (clientIP.length() != 0)) {
            subjLocality = new SubjectLocality(clientIP, null);
        }
        Set statements = new HashSet();
        statements.add(new AuthenticationStatement(authMethod, authInstant,
                        sub, subjLocality, null));

        if ((attributes != null) && (!attributes.isEmpty())) {
            statements.add(new AttributeStatement(sub, attributes));
        }
        Date issueInstant = newDate();
        Date notBefore = new Date(issueInstant.getTime() - notBeforeSkew);
        // TODO: this period will be different for bearer
        Date notAfter = new Date(issueInstant.getTime() + assertionTimeout);
        Conditions cond = new Conditions(notBefore, notAfter);
        String issuer = (String) SAMLServiceManager.getAttribute(
                                        SAMLConstants.ISSUER_NAME);
        Assertion assertion = new Assertion(null, issuer, issueInstant, cond,
                                        statements);
        assertion.setMinorVersion(minorVersion);
        String aIDString = assertion.getAssertionID();

        // TODO:set AuthorityBinding if any

        if (((Boolean) SAMLServiceManager.getAttribute(
                SAMLConstants.SIGN_ASSERTION)).booleanValue())
        {
            assertion.signXML();
        }

        Entry entry = new Entry(assertion, destID, artString, token);
        try {
            Object oldEntry = null;
            synchronized (idEntryMap) {
                oldEntry = idEntryMap.put(aIDString, entry);
            }
            if (oldEntry != null) {
                assertionTimeoutRunnable.removeElement(aIDString);
            }
            assertionTimeoutRunnable.addElement(aIDString);
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ASSERTIONS,
                    FedMonSAML1Svc.CWRITE);
            }
        } catch (Exception e) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager: couldn't add "
                        + "to idEntryMap.", e);
            }
            throw new SAMLResponderException(
                    SAMLUtils.bundle.getString("errorCreateAssertion"));
        }

        if (LogUtils.isAccessLoggable(java.util.logging.Level.FINER)) {
            String[] data = { SAMLUtils.bundle.getString("assertionCreated"),
                assertion.toString(true, true)};
            LogUtils.access(java.util.logging.Level.FINER,
                LogUtils.ASSERTION_CREATED, data);
        } else {
            String[] data = { SAMLUtils.bundle.getString("assertionCreated"),
                aIDString};
            LogUtils.access(java.util.logging.Level.INFO,
                LogUtils.ASSERTION_CREATED, data);
        }

        if (artString != null) {
            // put artifact in artEntryMap
            try {
                Object oldEntry = null;
                synchronized (artEntryMap) {
                    oldEntry = artEntryMap.put(artString, new
                        ArtEntry(aIDString,
                        (currentTimeMillis() + artifactTimeout)));
                }
                if (oldEntry != null) {
                    artifactTimeoutRunnable.removeElement(artString);
                }
                artifactTimeoutRunnable.addElement(artString);
                if ((agent != null) && agent.isRunning() && (saml1Svc != null)){
                        saml1Svc.incSAML1Cache(
                        FedMonSAML1Svc.ARTIFACTS,
                        FedMonSAML1Svc.CWRITE);
                }
            } catch (Exception e) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager: couldn't add "
                        + "artifact to the artEntryMap.", e);
                }
                throw new SAMLResponderException(
                    SAMLUtils.bundle.getString("errorCreateArtifact"));
            }
            String[] data = {SAMLUtils.bundle.getString(
                  "assertionArtifactCreated"), artString, aIDString};
            LogUtils.access(java.util.logging.Level.INFO,
                        LogUtils.ASSERTION_ARTIFACT_CREATED, data);
        }

        if (token != null) {
            // create a listener and add the listener to the token
            AssertionSSOTokenListener listener = new AssertionSSOTokenListener(
                aIDString, artString);
            try {
                sessionProvider.addListener(token, listener);
            } catch (SessionException e) {
                SAMLUtils.debug.error("AssertionManager.createAssertion(id):"
                    + " Couldn't add listener to session:", e);
            } catch (UnsupportedOperationException uoe) {
                SAMLUtils.debug.warning("AssertionManager.createAssertion(id):"
                    + " Operation add listener to session not supported:",
                    uoe);
            }
        }

        return assertion;
    }

    /**
     * Deletes an assertion from the server. This method is used by the
     * AssertionSSOTokenListener and cleanup method in the package.
     * @param assertionID the id of the Assertion to be deleted.
     * @param artifact the artifact associated with this assertionID.
     *          When it's null, no artifact is associated with this assertionID.
     */ 
    void deleteAssertion(String assertionID, String artifact) {
        ArtEntry artEntry = null;
        if (artifact != null) {
            // this is the case when Session expired, and the assertion
            // was created for artifact
            artEntry = (ArtEntry) artEntryMap.remove(artifact);
            String[] data = {SAMLUtils.bundle.getString(
                "assertionArtifactRemoved"), artifact};
            LogUtils.access(java.util.logging.Level.FINE, 
                LogUtils.ASSERTION_ARTIFACT_REMOVED, data);
        }

        if (assertionID != null) {
            Entry entry = null;
            entry = (Entry) idEntryMap.remove(assertionID);
            if (entry != null) {
                String[] data = {SAMLUtils.bundle.getString("assertionRemoved"),
                    assertionID};
                LogUtils.access(java.util.logging.Level.FINE,
                    LogUtils.ASSERTION_REMOVED, data);
                if (artifact == null) {
                    // this is the case when assertion expired, check to see
                    // if the assertion is associated with an artifact
                    String artString = entry.getArtifactString();
                    if (artString != null) {
                        synchronized (artEntryMap) {
                            artEntryMap.remove(artString);
                        }
                        String[] data2 = {SAMLUtils.bundle.getString(
                            "assertionArtifactRemoved"), artifact};
                        LogUtils.access(java.util.logging.Level.FINE,
                            LogUtils.ASSERTION_ARTIFACT_REMOVED, data2);
                    }
                }
            }
        } else {
           if ((artEntry != null) && SAMLServiceManager.getRemoveAssertion()) {
                synchronized (idEntryMap) {
                    idEntryMap.remove(artEntry.getAssertionID());
                }
            }
        }
    }

    /**
     * Gets assertion associated with the AssertionArtifact.
     * @param artifact An AssertionArtifact.
     * @param destID A Set of String that represents the destination site id.
     *          The destination site requesting the assertion using
     *          the artifact. This String is compared with the destID that
     *          the artifact is created for originally.
     * @param destCheckFlag true if desire to match the destionation id, 
     *                      otherwise it is false. If it is false, destID can 
     *                      be any string, including null. 
     * @return The Assertion referenced to by artifact.
     * @throws SAMLException If an error occurred during the process, or no
     *          assertion maps to the input artifact.
     */
    private Assertion getAssertion(AssertionArtifact artifact, Set destID, 
                                  boolean destCheckFlag)
                                  throws SAMLException {
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("getAssertion(arti): destID set= " +
                Base64.encode(SAMLUtils.stringToByteArray(
                    (String)destID.iterator().next())));
        }
        // check the destination id; also if this artifact exists
        String artString = artifact.getAssertionArtifact();

        // get server id.
        String remoteUrl = SAMLUtils.getServerURL(
                                        artifact.getAssertionHandle());
        if (remoteUrl != null) { // not this server
            // call AssertionManagerClient.getAssertion
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion(art, " +
                    "destid: calling another server in lb site:" + remoteUrl);
            }
            AssertionManagerClient amc = new AssertionManagerClient(
                                        SAMLUtils.getFullServiceURL(remoteUrl));
            return amc.getAssertion(artifact, destID);
        } // else 
        // get the assertion ID
        String aIDString = null;
        long timeout = 0;

        ArtEntry artEntry = (ArtEntry) artEntryMap.get(artString);
        if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
            saml1Svc.incSAML1Cache(FedMonSAML1Svc.ARTIFACTS,
                FedMonSAML1Svc.CREAD);
        }
        if (artEntry == null) {
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ARTIFACTS,
                    FedMonSAML1Svc.CMISS);
            }
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion(art, de"
                    + "stid): no Assertion found corresponding to artifact.");
            }
            throw new SAMLException(
                    SAMLUtils.bundle.getString("noMatchingAssertion"));
        } else {
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ARTIFACTS,
                    FedMonSAML1Svc.CHIT);
            }
        }
        aIDString = (String) artEntry.getAssertionID();
        if (aIDString == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion(art, de"
                + "stid): no AssertionID found corresponding to artifact.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("noMatchingAssertion"));
        }

        timeout = artEntry.getExpireTime();
        if (currentTimeMillis() > timeout) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion(art, "
                    + "destid): artifact timed out.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("artifactTimedOut"));
        }

        Entry entry = null;
        entry = (Entry) idEntryMap.get(aIDString);
        if (entry == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion(art, de"
                   + "stid): no Entry found corresponding to artifact.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("noMatchingAssertion"));
        }

        if (destCheckFlag) {
            // check the destination id
            String dest = entry.getDestID();
            if (dest == null) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAssertion(" + 
                    "art, destid): no destID found corresponding to artifact.");
                }
                throw new SAMLException(
                    SAMLUtils.bundle.getString("noDestIDMatchingArtifact"));
            }
            if (destID == null || !destID.contains(dest)) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAssertion(art"+
                                    ", destid): destinationID doesn't match.");
                }
                throw new SAMLException(
                    SAMLUtils.bundle.getString("destIDNotMatch"));
            }
        }
        Assertion assertion = entry.getAssertion();
        if (assertion == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion(art, de"
                    + "stid): no Assertion found corresponding to aID.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("noMatchingAssertion"));
        }
        
        // remove the asssertion from artEntryMap
        synchronized (artEntryMap) {
            artEntryMap.remove(artString);
        }
        artifactTimeoutRunnable.removeElement(artString);
        String[] data = {SAMLUtils.bundle.getString(
            "assertionArtifactVerified"), artString};
        LogUtils.access(java.util.logging.Level.INFO,
            LogUtils.ASSERTION_ARTIFACT_VERIFIED, data);

        if (SAMLServiceManager.getRemoveAssertion()) {
            synchronized(idEntryMap) {
                idEntryMap.remove(aIDString);
            }
            assertionTimeoutRunnable.removeElement(aIDString);
        }
        
        // check the time of the assertion
        if (!assertion.isTimeValid()) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager: assertion "
                        + aIDString + " is expired.");
            }
            throw new SAMLException(
                        SAMLUtils.bundle.getString("assertionTimeNotValid"));
        }
        return assertion;
    }
    
    /**
     * Gets assertion associated with the AssertionArtifact.
     * @param artifact An AssertionArtifact.
     * @param destID The destination site requesting the assertion using
     *          the artifact. This String is compared with the destID that
     *          the artifact is created for originally.
     * @return The Assertion referenced to by artifact.
     * @throws SAMLException If an error occurred during the process, or no
     *          assertion maps to the input artifact.
     */
    public Assertion getAssertion(AssertionArtifact artifact, String destID)
                                 throws SAMLException {
        if ((artifact == null) || destID == null || destID.length() == 0) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager: input is null.");
            }
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("nullInput"));
        }
        Set destSet = new HashSet();
        destSet.add(destID);
        return getAssertion(artifact, destSet, true); 
    }
    
     /**
     * Gets assertion associated with the AssertionArtifact.
     * @param artifact An AssertionArtifact.
     * @param destID A Set of String that represents the destination site id. 
     *          The destination site requesting the assertion using
     *          the artifact. Each string in this set compares with the destID 
     *          that the artifact is created for originally. If found match, 
     *          continue the operation. Otherwise, throws error.     
     * @return The Assertion referenced to by artifact.
     * @throws SAMLException If an error occurred during the process, or no
     *          assertion maps to the input artifact.
     */
    public Assertion getAssertion(AssertionArtifact artifact, Set destID)
                                 throws SAMLException {
        if ((artifact == null) || destID == null || destID.isEmpty()) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager: input is null.");
            }
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("nullInput"));
        }
        return getAssertion(artifact, destID, true); 
    }
    
    /**
     * Gets assertion associated with the AssertionArtifact.
     * @param artifact An AssertionArtifact.
     * @return The Assertion referenced to by artifact.
     * @throws SAMLException If an error occurred during the process, or no
     *          assertion maps to the input artifact.
     */
    protected  Assertion getAssertion(AssertionArtifact artifact)
                                throws SAMLException {
        if ((artifact == null)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager: input is null.");
            }
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("nullInput"));
        }
        return getAssertion(artifact, null, false);
    }
    
    /**
     * Gets assertion created from the query.
     * @param query An Assertion Query.
     * @param destID to whom the assertion will be created for.
     * @return The Assertion that is created from the query.
     * @throws SAMLException If the Assertion cannot be created due to an 
     *          error in the query or in the receiver.
     */
    public Assertion getAssertion(Query query, String destID)
                                throws SAMLException {
        if (query == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion: input"
                                        + " query is null.");
            }
            throw new SAMLRequesterException(
                        SAMLUtils.bundle.getString("nullInput"));
        }

        Assertion result = null;
        int queryType = query.getQueryType();
        if (queryType == Query.AUTHENTICATION_QUERY) {
            result = getAuthenticationAssertion((AuthenticationQuery)query,
                                                destID);
        } else if (queryType == Query.AUTHORIZATION_DECISION_QUERY) {
            result = getAuthorizationDecisionAssertion(
                        (AuthorizationDecisionQuery)query, destID);
        } else if (queryType == Query.ATTRIBUTE_QUERY) {
            result = getAttributeAssertion((AttributeQuery)query, destID);
        } else {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion: this "
                    + "type of query is not supported:" + queryType);
            }
            throw new SAMLRequesterException(
                        SAMLUtils.bundle.getString("queryNotSupported"));
        }
        return result;
    }

    /**
     * Gets assertion created from an AttributeQuery.
     * @param query An AttributeQuery.
     * @param destID to whom the assertion will be created for. Currently,
     *        it is the <code>sourceID</code> of the site that sends the query.
     * @return The Assertion that is created from the query.
     * @throws SAMLException If the Assertion cannot be created.
     */
    private Assertion getAttributeAssertion(AttributeQuery query, String destID)
                                        throws SAMLException
    {
        if (query == null) {
            // no need to log the error again
            return null;
        }

        if ((destID == null) || (destID.length() == 0)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAttributeAssertion"
                        + ": missing destID.");
            }
            throw new SAMLException(
                        SAMLUtils.bundle.getString("missingDestID"));
        }
        validateNumberOfAssertions(idEntryMap);
        Map entries = (Map) SAMLServiceManager.getAttribute(
                                        SAMLConstants.PARTNER_URLS);
        if (entries == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAttributeAssertion"
                    + ": empty partner URL list.");
            }
            throw new SAMLException(
                        SAMLUtils.bundle.getString("emptyPartnerURLList"));
        }

        SAMLServiceManager.SOAPEntry destSite = (SAMLServiceManager.SOAPEntry)
                                        entries.get(destID);
        AttributeMapper attrMapper = null;
        if ((destSite == null) ||
            ((attrMapper = destSite.getAttributeMapper()) == null))
        {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAttributeAssertion"
                    + ": couldn't obtain AttributeMapper.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("errorObtainAttributeMapper"));
        }

        Subject subject = query.getSubject();
        String tokenID = attrMapper.getSSOTokenID(query);
        Object token = null;
        String issuerName = (String) SAMLServiceManager.getAttribute(
                                SAMLConstants.ISSUER_NAME);
        if (tokenID != null) {
            try {
                if (sessionProvider == null) {
                    throw new SAMLException(SAMLUtils.bundle.getString(
                        "nullSessionProvider"));
                }
                token = sessionProvider.getSession(tokenID);
            } catch (Exception e) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAttribute"
                        + "Assertion: invalid SSO token:", e);
                }
                throw new SAMLException(
                    SAMLUtils.bundle.getString("invalidSSOToken"));
            }
        } else { // token is null
            Assertion assertion = attrMapper.getSSOAssertion(query);
            if (assertion == null) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAttribute"
                        + "Assertion: couldn't find SSOAssertion in query.");
                }
                throw new SAMLException(
                        SAMLUtils.bundle.getString("noSSOAssertion"));
            }
            if (!assertion.isSignatureValid()) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAttribute"
                        + "Assertion: SSOAssertion is signature invalid.");
                }
                throw new SAMLException(
                    SAMLUtils.bundle.getString("assertionSignatureNotValid"));
            }
            if (!assertion.isTimeValid()) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAttribute"
                        + "Assertion: SSOAssertion is time invalid.");
                }
                throw new SAMLException(
                        SAMLUtils.bundle.getString("assertionTimeNotValid"));
            }
            Iterator iter = assertion.getStatement().iterator();
            Statement statement = null;
            Subject ssoSubject = null;
            while (iter.hasNext()) {
                statement = (Statement) iter.next();
                if (statement.getStatementType() ==
                                Statement.AUTHENTICATION_STATEMENT)
                {
                    ssoSubject = ((AuthenticationStatement) statement).
                                                                getSubject();
                    break;
                }
            }
            if (ssoSubject == null) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAttribute"
                            + "Assertion: missing AuthenticationStatement in "
                            + "SSOAssertion.");
                }
                throw new SAMLException(
                    SAMLUtils.bundle.getString("noAuthNStatement"));
            }
            String issuer = assertion.getIssuer();
            String aID = assertion.getAssertionID();
            if ((issuerName != null) && (issuerName.equals(issuer)) &&
                (SAMLUtils.getServerURL(aID) == null))
            {
                // this server is the issuer
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAttrAssertion:"
                        + "this server is the issuer.");
                }
                Entry entry = (Entry) idEntryMap.get(aID);
                if ((agent != null) && agent.isRunning() && (saml1Svc != null)){
                        saml1Svc.incSAML1Cache(
                        FedMonSAML1Svc.ASSERTIONS,
                        FedMonSAML1Svc.CREAD);
                }
                if (entry != null) {
                    token = entry.getSSOToken();
                    if (token != null) {
                        verifySSOTokenAndNI(token,
                                        ssoSubject.getNameIdentifier());
                    }
                    if ((agent != null) &&
                        agent.isRunning() &&
                        (saml1Svc != null))
                    {
                        saml1Svc.incSAML1Cache(
                            FedMonSAML1Svc.ASSERTIONS,
                            FedMonSAML1Svc.CHIT);
                    }
                } else {
                    if ((agent != null) &&
                        agent.isRunning() &&
                        (saml1Svc != null))
                    {
                        saml1Svc.incSAML1Cache(
                            FedMonSAML1Svc.ASSERTIONS,
                            FedMonSAML1Svc.CMISS);
                    }
                }
            } else { // this machine is not the issuer
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAttrAssertion:"
                        + "this server is not the issuer.");
                }
                token = checkAssertionAndCreateSSOToken(assertion, null,
                                                        ssoSubject);
            }
        }

        // get here then got a valid token
        List attributes = attrMapper.getAttributes(query, destID, token);
        if ((attributes == null) || (attributes.size() == 0)) {
            return null;
        }

        Set stmtSet = new HashSet();
        stmtSet.add(new AttributeStatement(subject, attributes));
        Date issueInstant = newDate();
        Date notBefore = new Date(issueInstant.getTime() - notBeforeSkew);
        Date notAfter = new Date(issueInstant.getTime() + assertionTimeout);
        Conditions cond = new Conditions(notBefore, notAfter);
        Assertion newAssertion = new Assertion(null, issuerName, issueInstant,
                                        cond, stmtSet);
        if (((Boolean) SAMLServiceManager.getAttribute(
                SAMLConstants.SIGN_ASSERTION)).booleanValue())
        {
            newAssertion.signXML();
        }
        String aIDString = newAssertion.getAssertionID();
        // don't save the token and don't add listener 
        Entry newEntry = new Entry(newAssertion, destID, null, null);

        // add newEntry to idEntryMap
        try {
            Object oldEntry = null;
            synchronized (idEntryMap) {
                oldEntry = idEntryMap.put(aIDString, newEntry);
            }
            if (oldEntry != null) {
                assertionTimeoutRunnable.removeElement(aIDString);
            }
            assertionTimeoutRunnable.addElement(aIDString);
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ASSERTIONS,
                    FedMonSAML1Svc.CWRITE);
            }
        } catch (Exception e) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAttributeAssertion"
                    + " couldn't add assertion to the idEntryMap.", e);
            }
        }

        if (LogUtils.isAccessLoggable(java.util.logging.Level.FINER)) {
            String[] data = { SAMLUtils.bundle.getString("assertionCreated"),
                              newAssertion.toString(true, true)};
            LogUtils.access(java.util.logging.Level.FINER,
                    LogUtils.ASSERTION_CREATED, data);
        } else {
            String[] data = { SAMLUtils.bundle.getString("assertionCreated"),
                              aIDString};
            LogUtils.access(java.util.logging.Level.INFO,
                    LogUtils.ASSERTION_CREATED, data);
        }

        return newAssertion;
    }

    /**
     * Gets assertion created from an AuthenticationQuery.
     * @param query An AuthenticationQuery.
     * @param destID to whom the assertion will be created for.
     * @return The Assertion that is created from the query.
     * @throws SAMLException If the Assertion cannot be created.
     */
    private Assertion getAuthenticationAssertion(
                AuthenticationQuery query, String destID) throws SAMLException
    {
        if (query == null) {
            // no need to log the error again
            return null;
        }
        validateNumberOfAssertions(idEntryMap);

        // get the subject of the query
        Subject subject = query.getSubject();

        // get SubjectConfirmation
        SubjectConfirmation sc = subject.getSubjectConfirmation();
        if (sc == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthNAssertion:"
                    + " missing SubjectConfirmation.");
            }
            // since we couldn't find the SSOToken in SubjectConfirmationData
            // we don't know if the subject is authenticated to OpenAM.
            throw new SAMLException(
                SAMLUtils.bundle.getString("missingSubjectConfirmation"));
        }
        // check ConfirmationMethod
        if (!SAMLUtils.isCorrectConfirmationMethod(sc)) {
            // don't need to log again
            throw new SAMLException(
                SAMLUtils.bundle.getString("wrongConfirmationMethodValue"));
        }

        // get SubjectConfirmationData
        Element scData = sc.getSubjectConfirmationData();
        if (scData == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthNAssertion:"
                    + " missing SubjectConfirmationData in the Subject.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("missingSubjectConfirmationData"));
        }

        // SSOTokenID == scData
        String authMethod = null;
        Date authInstant = null;
        String nameQualifier = null;
        String name = null;
        Object token = null;
        String clientIP = null;
        try {
            if (sessionProvider == null) {
                throw new SAMLException(SAMLUtils.bundle.getString(
                    "nullSessionProvider"));
            } 
            token = sessionProvider.getSession(
                XMLUtils.getElementString(scData));
            authMethod = SAMLServiceManager.getAuthMethodURI(
               sessionProvider.getProperty(token, "AuthType")[0]);
            // get authenticationInstant
            authInstant = DateUtils.stringToDate(
               sessionProvider.getProperty(token, "authInstant")[0]);
            // get the nameQualifier of the NameIdentifier
            nameQualifier = XMLUtils.escapeSpecialCharacters(
               sessionProvider.getProperty(token, "Organization")[0]);          
            // get the name of the NameIdentifier
            name = XMLUtils.escapeSpecialCharacters(
               sessionProvider.getPrincipalName(token));      
            try {
                InetAddress clientIPAddress = 
                    InetAddress.getByName(sessionProvider.getProperty(
                    token,"ipaddress")[0]);
                clientIP = clientIPAddress.getHostAddress();
            } catch (Exception e) {
                // catching exception here since clientIP is optional
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager." +
                        "getAuthNAssertion: exception when getting " +
                        "client ip.");
                }
            }
        } catch (Exception e) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthNAssertion:"
                    + " exception retrieving info from the SSOToken:", e);
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("wrongSubjectConfirmationData"));
        }

        // get and check NameIdentifier
        NameIdentifier ni = subject.getNameIdentifier();
        if (ni != null) {
            String niName = ni.getName();
            String niNameQualifier = ni.getNameQualifier();
            if (((niName != null) && (!niName.equalsIgnoreCase(name))) ||
                ((niNameQualifier != null) &&
                (!niNameQualifier.equalsIgnoreCase(nameQualifier))))
            {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAuthNAssertion"
                        + ": NameIdentifier is different from info in "
                        + "SubjectConfirmation");
                }
                throw new SAMLException(
                    SAMLUtils.bundle.getString("wrongNameIdentifier"));
            }
        }

        // get and check AuthenticationMethod in the query
        String am = query.getAuthenticationMethod();
        // check it against authMethod
        if ((am != null) && (am.length() != 0) &&
            (!am.equalsIgnoreCase(authMethod)))
        {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthNAssertion:"
                    + " couldn't form an assertion matching the "
                    + "AuthenticationMethod in the query.");
            }
            throw new SAMLException(SAMLUtils.bundle.getString(
                "authenticationMethodInQueryNotMatch"));
        }
        SubjectLocality subjLocality = null;
        if ((clientIP != null) && (clientIP.length() != 0)) {
            subjLocality = new SubjectLocality(clientIP, null);
        }
        AuthenticationStatement statement =
            new AuthenticationStatement(authMethod, authInstant, subject,
                subjLocality, null);
        Date issueInstant = newDate();
        // get this period from the config
        Date notAfter = new Date(issueInstant.getTime() + assertionTimeout);
        Date notBefore = new Date(issueInstant.getTime() - notBeforeSkew);
        Conditions cond = new Conditions(notBefore, notAfter);
        String issuer = (String) SAMLServiceManager.getAttribute(
                                SAMLConstants.ISSUER_NAME);
        Set statements = new HashSet();
        statements.add(statement);
        Assertion assertion = new Assertion(null, issuer, issueInstant, cond,
                                                statements);
        if (((Boolean) SAMLServiceManager.getAttribute(
                SAMLConstants.SIGN_ASSERTION)).booleanValue())
        {
            assertion.signXML();
        }
        String aIDString = assertion.getAssertionID();

        Entry entry = new Entry(assertion, destID, null, token);

        // add entry to idEntryMap
        try {
            Object oldEntry = null;
            synchronized (idEntryMap) {
                oldEntry = idEntryMap.put(aIDString, entry);
            }
            if (oldEntry != null) {
                assertionTimeoutRunnable.removeElement(aIDString);
            }
            assertionTimeoutRunnable.addElement(aIDString);
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ASSERTIONS,
                    FedMonSAML1Svc.CWRITE);
            }
        } catch (Exception e) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthNAssertion:"
                    + " couldn't add assertion to the idEntryMap.", e);
            }
            throw new SAMLResponderException(
                SAMLUtils.bundle.getString("errorCreateAssertion"));
        }

        if (LogUtils.isAccessLoggable(java.util.logging.Level.FINER)) {
            String[] data = { SAMLUtils.bundle.getString("assertionCreated"),
                              assertion.toString(true, true)};
            LogUtils.access(java.util.logging.Level.FINER,
                    LogUtils.ASSERTION_CREATED, data);
        } else {
            String[] data = { SAMLUtils.bundle.getString("assertionCreated"),
                              aIDString};
            LogUtils.access(java.util.logging.Level.INFO,
                    LogUtils.ASSERTION_CREATED, data);
        }

        // create a listener and add the listener to the token
        AssertionSSOTokenListener listener =
            new AssertionSSOTokenListener(aIDString);
        try {
            sessionProvider.addListener(token, listener);
        } catch (SessionException e)  {
            SAMLUtils.debug.error("AssertionManager.getAuthNAssertion:"
                + " Couldn't add listener to token:", e);
            // don't need to throw an exception
        }

        return assertion;
    }

    /**
     * Gets assertion created from an AuthorizationDecisionQuery.
     * @param query An AuthorizationDecisionQuery.
     * @param destID to whom the assertion will be created for.
     * @return The Assertion that is created from the query.
     * @throws SAMLException If the Assertion cannot be created.
     */
    private Assertion getAuthorizationDecisionAssertion(
                AuthorizationDecisionQuery query, String destID)
                throws SAMLException
    {
        return getAuthorizationDecisionAssertion(query, destID, true);
    }

    /**
     * Gets assertion created from an AuthorizationDecisionQuery.
     * @param query An AuthorizationDecisionQuery.
     * @param destID to whom the assertion will be created for.
     * @param store if true, the assertion is stored internally.
     * @return The Assertion that is created from the query.
     * @throws SAMLException If the Assertion cannot be created.
     */
    private Assertion getAuthorizationDecisionAssertion(
                AuthorizationDecisionQuery query, String destID, boolean store)
                throws SAMLException
    {
        if (query == null) {
            // no need to log the error again
            return null;
        }

        if ((destID == null) || (destID.length() == 0)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthZAssertion: "
                    + "missing destID.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("missingDestID"));
        }

        Map entries = (Map) SAMLServiceManager.getAttribute(
                        SAMLConstants.PARTNER_URLS);
        if (entries == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthZAssertion: "
                    + "empty partnerURL list.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("emptyPartnerURLList"));
        }

        SAMLServiceManager.SOAPEntry destSite = (SAMLServiceManager.SOAPEntry)
                                                entries.get(destID);
        ActionMapper actionMapper = null;
        if ((destSite == null) ||
            ((actionMapper = destSite.getActionMapper()) == null))
        {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthZAssertion: "
                    + "couldn't obtain ActionMapper.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("errorObtainActionMapper"));
        }

        Subject querySubject = query.getSubject();
        NameIdentifier queryNI = querySubject.getNameIdentifier();
        Object token = null;
        boolean existingToken = true;

        String tokenID = actionMapper.getSSOTokenID(query);
        if (tokenID != null) {
            // if there is a token, then the token must be valid
            try {
                if (sessionProvider == null) {
                    throw new SAMLException(SAMLUtils.bundle.getString(
                        "nullSessionProvider"));
                }
                token = sessionProvider.getSession(tokenID);
            } catch (Exception e) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAuthZAssertion"
                        + ": invalid SSO token:", e);
                }
                throw new SAMLException(
                    SAMLUtils.bundle.getString("invalidSSOToken"));
            }
            verifySSOTokenAndNI(token, queryNI);
        } else {
            Assertion assertion = actionMapper.getSSOAssertion(query, destID);
            if (assertion != null) {
                // if there is an assertion, then it must be valid
                Map tokenMap = verifyAssertionAndGetSSOToken(querySubject,
                    assertion);
                token = (Object) tokenMap.get("true");
                if (token == null){
                    existingToken = false;
                    token = (Object) tokenMap.get("false");
                }
            }
        }

        if (token == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthZAssertion: "
                    + "Couldn't obtain ssotoken.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("cannotVerifySubject"));
        }

        Map map = actionMapper.getAuthorizationDecisions(query, token, destID);

        // no need to invalidate the newly created ssotoken since the token
        // will be invalidated/destroyed when the short maxSessionTime and
        // maxIdleTime are reached.
        return getAuthorizationDecisionAssertion(query, destID, true,
                                token, existingToken, map);
    }

    private Map verifyAssertionAndGetSSOToken(Subject querySubject,
                                                Assertion assertion)
                                                throws SAMLException
    {
        if ((querySubject == null) || (assertion == null)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.verifyAssertionAnd"
                        + "GetSSOToken: null input.");
            }
            throw new SAMLException(
                        SAMLUtils.bundle.getString("cannotVerifySubject"));
        }

        if (!assertion.isSignatureValid()) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.verifyAssertionAnd"
                    + "GetSSOToken: SSOAssertion is signature invalid.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("assertionSignatureNotValid"));
        }
        if (!assertion.isTimeValid()) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.verifyAssertionAnd"
                    + "GetSSOToken: SSOAssertion is time invalid.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("assertionTimeNotValid"));
        }
        
        // TODO: check AudienceRestrictionConditions if any

        Map tokenMap = new HashMap();
        Object token = null;
        String issuerName = (String) SAMLServiceManager.getAttribute(
                                                SAMLConstants.ISSUER_NAME);
        String issuer = assertion.getIssuer();
        String aID = assertion.getAssertionID();
        if ((issuerName != null) && (issuerName.equals(issuer)) &&
            (SAMLUtils.getServerURL(aID) == null))
        {
            // this server is the issuer
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthZAssertion:"
                    + "this server is the issuer.");
            }
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ASSERTIONS,
                    FedMonSAML1Svc.CREAD);
            }
            Entry entry = (Entry) idEntryMap.get(aID);
            if (entry != null) {
                if ((agent != null) && agent.isRunning() && (saml1Svc != null)){
                        saml1Svc.incSAML1Cache(
                        FedMonSAML1Svc.ASSERTIONS,
                        FedMonSAML1Svc.CHIT);
                }
                token = entry.getSSOToken();
                if (token != null) {
                    verifySSOTokenAndNI(token,
                                        querySubject.getNameIdentifier());
                    tokenMap.put("true", token);
                    return tokenMap;
                }
            } else {
                if ((agent != null) && agent.isRunning() && (saml1Svc != null)){
                    saml1Svc.incSAML1Cache(
                        FedMonSAML1Svc.ASSERTIONS,
                        FedMonSAML1Svc.CMISS);
                }
            }

            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.verifyAssertionAnd"
                    + "GetSSOToken: either not an AuthN assertion or token "
                    + "is not for this subject.");
            }
            throw new SAMLException(SAMLUtils.bundle.
                                        getString("cannotVerifySubject"));
        } else { 
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthZAssertion:"
                        + "this server is not the issuer.");
            }
            Iterator iter = assertion.getStatement().iterator();
            Statement statement = null;
            AuthenticationStatement ssoStatement = null;
            while (iter.hasNext()) {
                statement = (Statement) iter.next();
                if (statement.getStatementType() ==
                                Statement.AUTHENTICATION_STATEMENT)
                {
                    ssoStatement = (AuthenticationStatement) statement;
                    break;
                }
            }
            if (ssoStatement == null) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.verifyAssertion"
                        + "AndGetSSOToken:  missing AuthenticationStatement in "
                        + "SSOAssertion.");
                }
                throw new SAMLException(
                    SAMLUtils.bundle.getString("noAuthNStatement"));
            }

            token = checkAssertionAndCreateSSOToken(assertion,
                        (AuthenticationStatement)statement, querySubject);
            tokenMap.put("false", token);
        }
        return tokenMap;
    }

    private void verifySSOTokenAndNI(Object token, NameIdentifier ni)
        throws SAMLException 
    {
        String name = null;
        String nameQualifier = null;
        try {
            if (sessionProvider == null) {
                throw new SAMLException(SAMLUtils.bundle.getString(
                    "nullSessionProvider"));
            }
            name = XMLUtils.escapeSpecialCharacters(
                sessionProvider.getPrincipalName(token));
            nameQualifier = XMLUtils.escapeSpecialCharacters(
                sessionProvider.getProperty(token, "Organization")[0]);
        } catch (Exception e) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.verifySSOTokenAndNI: "
                    + "Session is not valid.", e);
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("cannotVerifySubject"));
        }

        if (ni == null) {
            return;
        }

        String niName = ni.getName();
        String niNameQualifier = ni.getNameQualifier();
        if (((niName != null) && (!niName.equalsIgnoreCase(name))) ||
            ((niNameQualifier != null) && (!niNameQualifier.
                                        equalsIgnoreCase(nameQualifier))))
        {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.verifySSOToken"
                        + "AndNI: NameIdentifier is different from info in "
                        + "token.");
            }
            throw new SAMLException(SAMLUtils.bundle.
                                getString("wrongNameIdentifier"));
        }
    }

    private Object checkAssertionAndCreateSSOToken(Assertion assertion,
        AuthenticationStatement statement, Subject subject)
        throws SAMLException
    {
        // check if issuer is on our list.
        String issuer = assertion.getIssuer();
        SAMLServiceManager.SOAPEntry sourceSite =
                                SAMLUtils.getSourceSite(issuer);
        if (sourceSite == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.checkAssertionAnd"
                    + "CreateSSOToken: issuer is not on the partnerURL list.");
            }
            throw new SAMLException(SAMLUtils.bundle.
                                        getString("cannotVerifySubject"));
        }

        // TODO: check AudienceRestrictionCondition if any

        if (statement != null) {
            // check the subject
            if ((subject == null) || (!subject.equals(statement.getSubject())))
            {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.verifyAndGetSSO"
                        + "Token: wrong subject in evidence.");
                }
                throw new SAMLException(SAMLUtils.bundle.
                                        getString("cannotVerifySubject"));
            }
        }
        return createTempSSOToken(assertion, subject, sourceSite);
    }

    private Object createTempSSOToken(Assertion assertion, Subject subject,
                        SAMLServiceManager.SOAPEntry sourceSite)
                        throws SAMLException
    {
        List assertions = new ArrayList();
        assertions.add(assertion);        
        String srcID = sourceSite.getSourceID();
        String name = null;
        String org = null;

        PartnerAccountMapper paMapper = sourceSite.getPartnerAccountMapper();
        if (paMapper != null) {
            Map map = paMapper.getUser(assertions, srcID, null);
            name = (String) map.get(PartnerAccountMapper.NAME);
            org = (String) map.get(PartnerAccountMapper.ORG);
        }

        if ((org == null) || (name == null)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager." +
                    "createTempSSOToken: couldn't map the subject " +
                    "to a local user.");
            }
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("cannotMapSubject"));
        } else {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager." +
                    "createTempSSOToken: org = " + org + ", name = " + 
                    name);
            }
        }
        
        Object token = null; 
        try {
            Map infoMap = new HashMap();
            if ((org != null) && (org.length() != 0)) {
                infoMap.put(SessionProvider.REALM, org);
            } else {
                infoMap.put(SessionProvider.REALM, "/");
            }
            infoMap.put(SessionProvider.PRINCIPAL_NAME, name);
            infoMap.put(SessionProvider.AUTH_LEVEL, "0");        
            token = SAMLUtils.generateSession(null, null, infoMap); 
        } catch (Exception e) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManger." + 
                    "createTempSSOToken: Couldn't retrieve " + 
                    "the ssotoken.", e);
            }
            throw new SAMLResponderException(
                SAMLUtils.bundle.getString("errorCreateAssertion"));
        }
        return token; 
    }

    /**
     * @param addListener A listener to the single sign on token is added only
     *        when both store and addListener are true.
     */
    private Assertion getAuthorizationDecisionAssertion(
        AuthorizationDecisionQuery query, String destID,
        boolean store, Object token,
        boolean addListener, Map actionMap)
        throws SAMLException
    {
        if (actionMap == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAuthZAssertion: "
                    + "actionMap from ActionMapper is null.");
            }
            throw new SAMLException(
                        SAMLUtils.bundle.getString("nullAuthZDecision"));
        }

        validateNumberOfAssertions(idEntryMap);
        int decision;
        List newActions = null;
        if ((newActions = (List) actionMap.get(ActionMapper.PERMIT)) != null) {
            decision = AuthorizationDecisionStatement.DecisionType.PERMIT;
        } else if ((newActions = (List) actionMap.get(ActionMapper.DENY))
                                                                != null)
        {
            decision = AuthorizationDecisionStatement.DecisionType.DENY;
        } else {
            newActions = (List) actionMap.get(ActionMapper.INDETERMINATE);
            if (newActions == null) {
                // try not to be too restrictive
                newActions = query.getAction();
            }
            decision = 
                AuthorizationDecisionStatement.DecisionType.INDETERMINATE;
        }

        //create statement
        AuthorizationDecisionStatement statement =
                new AuthorizationDecisionStatement(
                        query.getSubject(), query.getResource(), decision,
                        newActions, query.getEvidence());

        Date issueInstant = newDate();
        Date notAfter = new Date(issueInstant.getTime() + assertionTimeout);
        Date notBefore = new Date(issueInstant.getTime() - notBeforeSkew);
        Conditions cond = new Conditions(notBefore, notAfter);
        String issuer = (String) SAMLServiceManager.getAttribute(
                                                SAMLConstants.ISSUER_NAME);
        Set statements = new HashSet();
        statements.add(statement);
        Assertion assertion = new Assertion(null, issuer, issueInstant, cond,
                                                statements);
        if (((Boolean) SAMLServiceManager.getAttribute(
                SAMLConstants.SIGN_ASSERTION)).booleanValue())
        {
            assertion.signXML();
        }
        String aIDString = assertion.getAssertionID();

        if (store) {
            Entry entry = null;
            if (addListener) {
                // create a listener and add the listener to the token
                AssertionSSOTokenListener listener =
                        new AssertionSSOTokenListener(aIDString);
                try {
                    if (sessionProvider == null) {
                        throw new SAMLException(SAMLUtils.bundle.
                            getString("nullSessionProvider"));
                    }
                    sessionProvider.addListener(token, listener);
                } catch (SessionException e)  {
                    SAMLUtils.debug.error("AssertionManager.getAuthNAssertion:"
                        + " Couldn't get listener to token:", e);
                    // don't need to throw an exception
                }
            }
            entry = new Entry(assertion, destID, null, null);

            // put assertion in idEntryMap
            try {
                Object oldEntry = null;
                synchronized (idEntryMap) {
                    oldEntry = idEntryMap.put(aIDString, entry);
                }
                if (oldEntry != null) {
                    assertionTimeoutRunnable.removeElement(aIDString);
                }
                assertionTimeoutRunnable.addElement(aIDString);
                if ((agent != null) && agent.isRunning() && (saml1Svc != null)){
                    saml1Svc.incSAML1Cache(
                        FedMonSAML1Svc.ASSERTIONS,
                        FedMonSAML1Svc.CWRITE);
                }
            } catch (Exception e) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager.getAuthZAssertion"
                        + ": couldn't add assertion to the idAssertionMap.", e);
                }
                throw new SAMLResponderException(
                    SAMLUtils.bundle.getString("errorCreateAssertion"));
            }

            if (LogUtils.isAccessLoggable(java.util.logging.Level.FINER)) {
                String[] data = {SAMLUtils.bundle.getString("assertionCreated"),
                    assertion.toString(true, true)};
                LogUtils.access(java.util.logging.Level.FINER,
                    LogUtils.ASSERTION_CREATED, data);
            } else {
                String[] data = {SAMLUtils.bundle.getString("assertionCreated"),
                    aIDString};
                LogUtils.access(java.util.logging.Level.INFO,
                    LogUtils.ASSERTION_CREATED, data);
            }
        }
        
        return assertion;
    }

    /**
     * Gets the Assertion referenced by an <code>AssertionIDReference</code>.
     * @param idRef The <code>AssertionIDReference</code> which references to an
     *        Assertion.
     * @return the Assertion referenced by the <code>AsertionIDReference</code>.
     * @throws SAMLException If an error occurred during the process; or
     *          the assertion could not be found.
     */
    public Assertion getAssertion(AssertionIDReference idRef)
                                        throws SAMLException
    {
        return getAssertion(idRef, null, false);
    }

    /**
     * Gets the Assertion referenced by an <code>AssertionIDReference</code>.
     * This method is usually used after the call
     * <code>AssertionManager.getAssertions(SSOToken)</code>.
     * The assertion is retrieved from this <code>AssertionManager</code> only.
     * @param idRef The <code>AssertionIDReference</code> which references to an
     *        Assertion.
     * @param token Use's session object that is allowed to obtain the
     *        assertion. This token must have top level administrator role.
     * @return the Assertion referenced by the <code>AsertionIDReference</code>.
     * @throws SAMLException If an error occurred during the process; the token
     *         does not have the privilege; or the assertion could not be
     *         found.
     * @supported.api
     */
    public Assertion getAssertion(AssertionIDReference idRef, Object token)
        throws SAMLException {
        if (token == null) {
            SAMLUtils.debug.error("AssertionManager.getAssertion(idRef, token"
                + "): input token is null.");
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }
        
        if (!isSuperUser(token)) {
            SAMLUtils.debug.error("AssertionManager.getAssertion(idRef, token"
                + "): Session doesn't have the privilege.");
            throw new SAMLException(SAMLUtils.bundle.getString("noPrivilege"));
        }
        return getAssertion(idRef, null, true);
    }

    /**
     * Gets the Assertion referenced by an <code>AssertionIDReference</code>.
     * @param idRef The <code>AssertionIDReference</code> which references to an
     *        Assertion.
     * @param destID The destination site id requesting the assertion using
     *        the assertion id reference. This String is compared with the
     *        <code>destID</code> that the assertion is created for originally.
     *        This field is not used (could be null) if the assertion was
     *        created without a <code>destID</code> originally. This String can
     *        be obtained from converting the 20 byte site id sequence to char
     *        array, then a new String from the char array.
     * @return the Assertion referenced by the <code>AsertionIDReference</code>.
     * @throws SAMLException If an error occurred during the process; or
     *          the assertion could not be found.
     * @supported.api
     */
    public Assertion getAssertion(AssertionIDReference idRef, String destID)
                                throws SAMLException {
        if (destID == null) {
            return getAssertion(idRef, null, false);
        } else {
            Set destSet = new HashSet();
            destSet.add(destID); 
            return getAssertion(idRef, destSet, false);
        }
    }
    
    /**
     * Gets the Assertion referenced by an <code>AssertionIDReference</code>.
     * @param idRef The <code>AssertionIDReference</code> which references to an
     *        Assertion.
     * @param destID A Set of destination site id. The destination site id
     *        requesting the assertion using the assertion id reference.
     *        This String is compared with the <code>destID</code> that the
     *        assertion is created for originally. This field is not used
     *        (could be null) if the assertion was created without a
     *        <code>destID</code> originally. This String can be obtained from
     *        converting the 20 byte site id sequence to char array, then a new
     *        String from the char array.
     * @return the Assertion referenced by the <code>AsertionIDReference</code>.
     * @throws SAMLException If an error occurred during the process; or
     *          the assertion could not be found.
     * @supported.api
     */
    public Assertion getAssertion(AssertionIDReference idRef, Set destID)
                                throws SAMLException {
        return getAssertion(idRef, destID, false);
    }
    
    /**
     * Gets the Assertion referenced by an <code>AssertionIDReference</code>.
     * @param id The <code>AssertionIDReference</code> which references to an
     *        Assertion.
     * @param destID A Set of String that represents the destination id. 
     *        The destination site id requesting the assertion using
     *        the assertion id reference. This String is compared with the
     *        <code>destID</code> that the assertion is created for originally.
     *        This field is not used (could be null) if the assertion was
     *        created without a <code>destID</code> originally. This String can
     *        be obtained from converting the 20 byte site id sequence to char
     *        array, then a new String from the char array.
     * @param useToken A boolean value. If set to true, destID is not
     *          checked against with the string that the assertion is created
     *          for originallyr, the assertion is retrieved from this server
     *          only.
     * @return the Assertion referenced by the <code>AsertionIDReference</code>.
     * @throws SAMLException If an error occurred during the process; or
     *          the assertion could not be found.
     */
    private Assertion getAssertion(AssertionIDReference idRef, Set destID,
                                boolean useToken)
                                throws SAMLException {
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("getAssertion(idRef): destID set=" +
                                     destID);
        }
        if (idRef == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion(Asser"
                    + "tionIDRef): null AssertionID.");
            }
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("nullInput"));
        }
        String aIDString = idRef.getAssertionIDReference();
        if (!useToken) {
            // get server id.
            String remoteUrl = SAMLUtils.getServerURL(aIDString);
            if (remoteUrl != null) { // not this server
                // call AssertionManagerClient.getAssertion
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager." + 
                        "getAssertion(idRef): calling another server" +
                        " in lb site:" + remoteUrl);
                }
                AssertionManagerClient amc = new AssertionManagerClient(
                                        SAMLUtils.getFullServiceURL(remoteUrl));
                return amc.getAssertion(idRef, destID);
            } //else 
        }

        Entry entry = (Entry) idEntryMap.get(aIDString);
        if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
            saml1Svc.incSAML1Cache(FedMonSAML1Svc.ASSERTIONS,
                FedMonSAML1Svc.CREAD);
        }
        if (entry == null) {
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ASSERTIONS,
                    FedMonSAML1Svc.CMISS);
            }
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion(Asser"
                    + "tionIDRef): no matching assertion found in idEntryMap.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("noMatchingAssertion"));
        } else {
            if ((agent != null) && agent.isRunning() && (saml1Svc != null)) {
                saml1Svc.incSAML1Cache(FedMonSAML1Svc.ASSERTIONS,
                    FedMonSAML1Svc.CHIT);
            }
        }

        Assertion assertion = entry.getAssertion();
        if (assertion == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.getAssertion("
                    + "AssertionIDRef): no matching assertion found.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("noMatchingAssertion"));
        }

        if (!useToken) {
            // check if the destID is correct
            String dest = entry.getDestID();
            if (dest != null) {
                if ((destID == null) || (!destID.contains(dest))) {
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message("AssertionManager.getAssertion("
                            + "AssertionID): destID doesn't match.");
                    }
                    throw new SAMLException(
                        SAMLUtils.bundle.getString("destIDNotMatch"));
                }
            }
        }

        // check the time of the assertion
        if (!assertion.isTimeValid()) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager: assertion "
                        + aIDString + " is expired.");
            }
            throw new SAMLException("assertionTimeNotValid");
        }

        return assertion;
    }

    /**
     * Creates an AssertionArtifact.
     * @param id The String that contains authentication information which
     *          is needed to create the assertion. It could be a string
     *          representation of an id, a cookie, etc.
     * @param destID The destination site that the artifact is created for.
     * @return The AssertionArtifact.
     * @throws SAMLException If the AssertionArtifact cannot be created.
     */
    public AssertionArtifact createAssertionArtifact(String id,
        String destID) throws SAMLException {
        return createAssertionArtifact(id, destID, null, null);
    }
    /**
     * Creates an AssertionArtifact.
     * @param id The String that contains authentication information which
     *          is needed to create the assertion. It could be a string
     *          representation of an id, a cookie, etc.
     * @param destID The destination site that the artifact is created for.
     * @param targetUrl A URL String representing the target site 
     * @param version The relying party preferred Assertion version number. 
     * @return The AssertionArtifact.
     * @throws SAMLException If the AssertionArtifact cannot be created.
     */
    public AssertionArtifact createAssertionArtifact(String id,
                                String destID, String targetUrl,
                                String version)
                                throws SAMLException {
        return createAssertionArtifact(id, destID, null, null, 
            targetUrl, version);
    }
    /**
     * Creates an AssertionArtifact.
     * @param id The String that contains authentication information which
     *          is needed to create the assertion. It could be a string
     *          representation of an id, a cookie, etc.
     * @param destID The destination site that the artifact is created for.
     * @param request The HttpServletRerquest object of the request.
     * @param response The HttpServletResponse object.
     * @param targetUrl A URL String representing the target site 
     * @param version The relying party preferred Assertion version number. 
     * @return The AssertionArtifact.
     * @throws SAMLException If the AssertionArtifact cannot be created.
     */
    public AssertionArtifact createAssertionArtifact(String id,
        String destID, HttpServletRequest request,
        HttpServletResponse response, String targetUrl,
        String version) throws SAMLException {
        // check input
        if ((id == null) || (destID == null)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager: null input for"
                        + " method createAssertionArtifact.");
            }
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("nullInput"));
        }

        Map partner = (Map) SAMLServiceManager.getAttribute(
                                                SAMLConstants.PARTNER_URLS);
        if ((partner == null) || (!partner.containsKey(destID))) {
            SAMLUtils.debug.error("AssertionManager.createAssertionArtifact:" +
                "(String, String): destID not in partner list.");
            throw new SAMLException(
                        SAMLUtils.bundle.getString("destIDNotFound"));
        }

        // create assertion id and artifact
        String handle = SAMLUtils.generateAssertionHandle();
        if (handle == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionManager.createAssertionArt"
                    + "ifact: couldn't generate assertion handle.");
            }
            throw new SAMLResponderException(
                SAMLUtils.bundle.getString("errorCreateArtifact"));
        }

        String sourceID = (String) SAMLServiceManager.getAttribute(
                                        SAMLConstants.SITE_ID);
        AssertionArtifact art = new AssertionArtifact(sourceID, handle);
        Assertion assertion = createSSOAssertion(id, art, 
            request, response, destID, targetUrl, version);
        try {
            if (version != null) { 
                StringTokenizer st = new StringTokenizer(version,".");
                if (st.countTokens() == 2) { 
                    assertion.setMajorVersion(
                        Integer.parseInt(st.nextToken().trim())); 
                    assertion.setMinorVersion(
                        Integer.parseInt(st.nextToken().trim()));
                }
            }
        } catch (NumberFormatException ne) { 
            throw new SAMLException(ne.getMessage()); 
        }
        return art;
    }

    /**
     * This method returns the decision of an AuthorizationQuery.
     * @param authZQuery An AuthorizationQuery that contains the question:
     *                  Is this subject authorized to perfrom this action on
     *                  this resource?
     * @param destID the SourceID of the site where the query is from.
     * @return an int whose value is defined in
     *          AuthorizationDecisionStatement.DecisionType.
     */
    public int isAllowed(AuthorizationDecisionQuery authZQuery, String destID) {
        if (authZQuery == null) {
            SAMLUtils.debug.error("AssertionManager.isAllowed: null input.");
            return AuthorizationDecisionStatement.DecisionType.INDETERMINATE;
        }

        Assertion assertion = null;
        try {
            assertion = getAuthorizationDecisionAssertion(authZQuery, destID,
                                                                false);
        } catch (SAMLException e) {
            SAMLUtils.debug.error("AssertionManager.isAllowed: exception thrown"
                + " when trying to get an assertion from authZQuery. ", e);
            return AuthorizationDecisionStatement.DecisionType.INDETERMINATE;
        }

        // double check, shouldn't be here
        if (assertion == null) {
            return AuthorizationDecisionStatement.DecisionType.INDETERMINATE;
        } 

        // Got an assertion
        Set statements = assertion.getStatement();
        if ((statements != null) && (!statements.isEmpty())) {
            Iterator iter = statements.iterator();
            while (iter.hasNext()) {
                Statement statement = (Statement) iter.next();
                if (statement.getStatementType() ==
                        Statement.AUTHORIZATION_DECISION_STATEMENT)
                {
                    // we know there should be only one authZstatement
                    return ((AuthorizationDecisionStatement) statement).
                                getDecision();
                }
            }
            // still here means no authZstatement
            SAMLUtils.debug.error("AssertionManager.isAllowed: no "
                        + "authZstatement in assertion.");
            return AuthorizationDecisionStatement.DecisionType.INDETERMINATE;
        } else {
            SAMLUtils.debug.error("AssertionManager.isAllowed: no statements in"
                + " assertion.");
            return AuthorizationDecisionStatement.DecisionType.INDETERMINATE;
        }
    }

    boolean validateNumberOfAssertions(Map idEntryMap) 
        throws SAMLResponderException 
    {
        Integer maxNumber = (Integer) SAMLServiceManager.getAttribute(
                        SAMLConstants.ASSERTION_MAX_NUMBER_NAME);
        int maxValue = maxNumber.intValue();
        if ((maxValue != 0) && (idEntryMap.size() > maxValue)) {
            SAMLUtils.debug.error("AssertionManager.createAssertion"
                + "Artifact(assertion,String): reached maxNumber of "
                + "assertions.");
            throw new SAMLResponderException(
                SAMLUtils.bundle.getString("errorCreateArtifact"));
        } else {
            return false;
        }
    }

    private class GoThroughRunnable extends GeneralTaskRunnable {
        private Set keys;
        private long runPeriod;
        
        public GoThroughRunnable(long runPeriod) {
            this.keys = new HashSet();
            this.runPeriod = runPeriod;
        }
        
        public boolean addElement(Object obj) {
           synchronized (keys) {
                return keys.add(obj);
            }
        }
    
        public boolean removeElement(Object obj) {
            synchronized (keys) {
                return keys.remove(obj);
            }
        }
    
        public boolean isEmpty() {
            return false;
        }
    
        public long getRunPeriod() {
            return runPeriod;
        }
        
        public void run() {
            long currentTime = currentTimeMillis();
            String keyString;
            Entry entry;
            Assertion assertion;
            SAMLUtils.debug.message("Clean up runnable wakes up..");
            synchronized (keys) {
                Iterator keyIter = keys.iterator();
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("AssertionManager::"
                        +"CleanUpThread::number of assertions in "
                        + "IdEntryMap:"+idEntryMap.size());
                }
                while (keyIter.hasNext()) {
                    keyString = (String) keyIter.next();
                    entry = (Entry) idEntryMap.get(keyString);
                    if (entry != null) {
                        assertion = entry.getAssertion();
                        if (assertion != null) {
                            if (assertion.getConditions() != null) {
                                if (!assertion.isTimeValid()) {
                                    keyIter.remove();
                                    deleteAssertion(keyString, null);
                                }
                            } else {
                                // if conditions are absent, calculate time
                                // validity of assertion as if notBefore is
                                // issueInstant - notBeforeSkew and notOnOrAfter
                                // is assertion time out + issueInstant
   
                                Date issueInstant = assertion.getIssueInstant();
                                Date notBefore = new Date(issueInstant.getTime()
                                    - notBeforeSkew);
                                Date notOnOrAfter = new Date(
                                    issueInstant.getTime() + assertionTimeout);
                                if (!((currentTime >= notBefore.getTime()) &&
                                    (currentTime < notOnOrAfter.getTime()))) {
                                    keyIter.remove();
                                    deleteAssertion(keyString, null);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
