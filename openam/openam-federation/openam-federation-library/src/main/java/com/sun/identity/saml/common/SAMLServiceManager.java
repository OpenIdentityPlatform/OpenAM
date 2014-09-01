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
 * $Id: SAMLServiceManager.java,v 1.10 2008/12/15 23:02:19 hengming Exp $
 *
 */

package com.sun.identity.saml.common;

import java.lang.NumberFormatException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;
import com.sun.identity.saml.plugins.ActionMapper;
import com.sun.identity.saml.plugins.AttributeMapper;
import com.sun.identity.saml.plugins.ConsumerSiteAttributeMapper;
import com.sun.identity.saml.plugins.DefaultNameIdentifierMapper;
import com.sun.identity.saml.plugins.DefaultAttributeMapper;
import com.sun.identity.saml.plugins.NameIdentifierMapper;
import com.sun.identity.saml.plugins.PartnerSiteAttributeMapper;
import com.sun.identity.saml.plugins.PartnerAccountMapper;
import com.sun.identity.saml.plugins.SiteAttributeMapper;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.plugin.configuration.ConfigurationException;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationActionEvent;

/**
 * This is a singleton class. It stores the current values of all the
 * AttributeSchema defined in SAML service schema. It updates its store by
 * listening to SAML ServiceSchema events.
 */
public class SAMLServiceManager implements ConfigurationListener {
    private static ConfigurationInstance ci = null;
    private static Map map = null;
    private static SAMLServiceManager instance = null;
    private static Object ssoToken =null;
    /**
     * Flag used by SAMLClient to determine if the client is local to
     * AssertionManager or not.
     */
    public static boolean localFlag = false; 
    private static String serverProtocol = null;
    private static String serverHost = null;
    private static String serverPort = null;
    private static String serverURI;
    private static String serverURL = null;
    private static boolean removeAssertion = false;
    private static String DEFAULT_PARTNER_ACCOUNT_MAPPER =
        "com.sun.identity.saml.plugins.DefaultPartnerAccountMapper"; 
    /**
     * Default Constructor.
     */
    private SAMLServiceManager() {
    }

    /**
     * This class contains the mapping between supported target 
     * host:port(target) and its saml-aware-servlet URL & sourceid.
     * target is the primary Key.
     */
    public static class SiteEntry {
        private String hostname = null; 
        private int portnumber = -1; 
        private String sid = null; 
        private String saml = null;
        private String post = null;
        private String prefVersion=null;

        /**
         * Constructs <code>SiteEntry</code> object.
         * @param host hostname of the target site.
         * @param port port number of the target site.
         * @param sourceid <code>Base64</code> encoded 20-byte sequence which
         *          represents id of the site.
         * @param samlUrl site's endpoint which handles SAML web browser
         *          artifact profile.
         * @param postUrl site's endpoint which handles SAML web broser POST
         *          profile.
         * @param version prefered SAML version the site wants to use.
         *          Its possible values are 1.0, 1.1.
         */
        public SiteEntry(String host, int port, String sourceid, String samlUrl,
                         String postUrl, String version)
        {
            hostname = host; 
            portnumber = port;
            sid = sourceid; 
            saml = samlUrl; 
            post = postUrl;
            prefVersion = version;         
        }

        /**
         * Returns URL endpoint which handles SAML web browser artifact profile.
         * @return String URL endpoint which handles SAML artifact profile.
         */
        public String getSAMLUrl() {return saml;}

        /**
         * Returns URL endpoint which handles SAML web browser POST profile.
         * @return String URL endpoint which handles SAML POST profile.
         */
        public String getPOSTUrl() {return post;}

        /**
         * Returns host name of the target.
         * @return String host name of the target.
         */
        public String getHostName() {return hostname;}

        /**
         * Returns port of the target.
         * @return int port of the target.
         */
        public int    getPort() {return portnumber;}

        /**
         * Returns source ID of the site.
         * @return String source ID of the site. It's a Base64 encoded 20-byte
         * sequence.
         */
        public String getSourceID() {return sid;}

        /**
         * Returns the SAML version the partner site is preferred to use.
         * Its value is 1.0 or 1.1.
         * @return String SAML version the partner site is preferred to use.
         */
        public String getVersion() {return prefVersion;}
    }
    
    /**
     * Contains the mapping between Source ID (SourceID) and its 
     * SOAP-Receiver-servlet URL (SOAPUrl).  SourceID is the primary Key.
     */
    public static class SOAPEntry {
        private String destID = null; 
        private String soapRevUrl = null;
        private String authenType = null; 
        private String userid = null; 
        private String basicAuthUserID = null;
        private String basicAuthPasswd = null;
        private String certalias = null;
        private PartnerAccountMapper partnerAcctMapper = null;
        private SiteAttributeMapper _siteAttributeMapper = null;
        private PartnerSiteAttributeMapper _partnerSiteAttributeMapper = null;
        private ConsumerSiteAttributeMapper consumerSiteAttrMapper = null;
        private NameIdentifierMapper nameIdentifierMapper = null;
        private AttributeMapper attributeMapper = null;
        private ActionMapper actionMapper = null;
        private String _issuer = null;
        private Set origHostSet = null;
        private String prefVersion = null; 

        /**
         * Constructs a <code>SOAPEntry</code>.
         * @param sourceid Source ID of the partner site.
         * @param soapUrl SOAP Receiver endpoint URL.
         * @param authType Authentication type should be used to protect
         *      the soap receiver.
         * @param user ID of the user whose attributes will be used for
         *      basic authentication.
         * @param bAuthUserID user ID for basic authentication
         * @param bAuthPasswd user password for basic authentication
         * @param certAlias certificate alias used to verify signature
         * @param partnerAccountMapper <code>PartnerAccountMapper</code>
         *      plugin instance
         * @param siteAttributeMapper <code>SiteAttributeMapper</code> plugin
         *      instance
         * @param partnerSiteAttributeMapper
         *      <code>PartnerSiteAttributeMapper</code> plugin instance
         * @param consumerSiteAttrMapper
         *      <code>ConsumerSiteAttributeMapper</code> plugin instance
         * @param nameIdentifierMapper <code>NameIdentifierMapper</code> plugin 
         *        instance
         * @param attrMapper <code>AttributeMapper</code> plugin instance
         * @param actionMapper <code>ActionMapper</code> plugin instance
         * @param issuer String which is the issuer of the site
         * @param origHostSet Set of hostname, ipaddress, and/or certificate
         *      alias of the partner site.
         * @param version SAML version the partner site is preferred to.
         */
        public SOAPEntry(String sourceid, String soapUrl, String authType, 
                         String user, String bAuthUserID,
                        String bAuthPasswd, String certAlias, 
                         PartnerAccountMapper partnerAccountMapper,
                         SiteAttributeMapper siteAttributeMapper, 
                         PartnerSiteAttributeMapper partnerSiteAttributeMapper,
                         ConsumerSiteAttributeMapper consumerSiteAttrMapper,
                         NameIdentifierMapper nameIdentifierMapper,
                         AttributeMapper attrMapper, ActionMapper actionMapper,
                        String issuer, Set origHostSet, String version) 
        {
            destID = sourceid;
            soapRevUrl = soapUrl; 
            authenType = authType; 
            userid = user; 
            basicAuthUserID = bAuthUserID;
            basicAuthPasswd = bAuthPasswd;
            certalias = certAlias; 
            partnerAcctMapper = partnerAccountMapper; 
            _siteAttributeMapper = siteAttributeMapper;
            _partnerSiteAttributeMapper = partnerSiteAttributeMapper;
            this.consumerSiteAttrMapper = consumerSiteAttrMapper;
            this.nameIdentifierMapper = nameIdentifierMapper;
            attributeMapper = attrMapper;
            this.actionMapper = actionMapper;
            _issuer = issuer; 
            this.origHostSet = origHostSet;
            prefVersion = version; 
        }

        /**
         * Returns source ID of the partner site.
         * This is <code>Base64</code> encoded 20-byte sequence.
         * @return source ID of the partner site.
         */
        public String getSourceID() {return destID;}

        /**
         * Returns SOAP receiver endpoint url.
         * @return String SOAP receiver endpoint url.
         */
        public String getSOAPUrl() {return soapRevUrl;}

        /**
         * Returns the authentication type used to protect the partner site's
         * soap receiver.
         * @return String authenticaiton type.
         */
        public String getAuthType() {return authenType;}

        /**
         * Returns user ID whose attributes will be retrieved to do basic auth.
         * This Method is depreciated. You should use method
         * <code>getBasicAuthUserID</code> and <code>getBasicAuthPassword</code>
         * instead.
         * @return String user ID.
         * @deprecated
         * @see #getBasicAuthUserID()
         * @see #getBasicAuthPassword()
         */
        public String getUser() {return userid;}

        /**
         * Returns user ID for basic authentication. This is used when the
         * partner soap endpoint is protected by HTTP basic authentication.
         * @return String user ID for basic authentication.
         */
        public String getBasicAuthUserID() {return basicAuthUserID;}

        /**
         * Returns user password for basic authentication. This is used when the
         * partner soap endpoint is protected by HTTP basic authentication.
         * @return String user password for basic authentication.
         */
        public String getBasicAuthPassword() {return basicAuthPasswd;}

        /**
         * Returns certificate alias can be used to verify a signature signed
         * by the partner site. It is used when the signature doesn't contain
         * a &lt;KeyInfo> element.
         * @return String certificate alias.
         */
        public String getCertAlias() {return certalias;}

        /**
         * Returns SAML 1.x version the partner site preferred to use.
         * It's value is 1.0 or 1.1.
         * @return String SAML 1.x version the partner site preferred to use.
         */
        public String getVersion() { return prefVersion;}

        /**
         * Returns <code>PartnerAccountMapper</code> instance.
         * @return PartnerAccountMapper instance.
         */
        public PartnerAccountMapper getPartnerAccountMapper()
            { return partnerAcctMapper; }

        /**
         * Returns <code>SiteAttributeMapper</code> instance.
         * @return SiteAttributeMapper instance.
         */
        public SiteAttributeMapper getSiteAttributeMapper() {
            SAMLUtilsCommon.debug.message("getSiteAttributeMapper() called");
            if (_siteAttributeMapper == null) {
                   SAMLUtilsCommon.debug.message("siteMapper is null");
            }
            return _siteAttributeMapper;
        }

        /**
         * Returns <code>PartnerSiteAttributeMapper</code> instance.
         * @return PartnerSiteAttributeMapper instance.
         */
        public PartnerSiteAttributeMapper getPartnerSiteAttributeMapper() {
            SAMLUtilsCommon.debug.message("getPartnerSiteAttrMapper() called");
            if (localFlag && _partnerSiteAttributeMapper == null) {
                SAMLUtilsCommon.debug.message("partnerSiteMapper is null");
            }
            return _partnerSiteAttributeMapper;
        }

        /**
         * Returns <code>ConsumerSiteAttributeMapper</code> instance.
         * @return ConsumerSiteAttributeMapper instance.
         */
        public ConsumerSiteAttributeMapper getConsumerSiteAttributeMapper() {
            if (localFlag && consumerSiteAttrMapper == null) {
                SAMLUtilsCommon.debug.message("consumerSiteMapper is null");
            }
            return consumerSiteAttrMapper;
        }

        /**
         * Returns <code>NameIdentifierMapper</code> instance.
         *
         * @return the <code>NameIdentifierMapper</code> instance.
         */
        public NameIdentifierMapper getNameIdentifierMapper() {
            return nameIdentifierMapper;
        }

        /**
         * Returns <code>AttributeMapper</code> for handling
         * <code>AttributeQuery</code> sent by the partner site.
         * @return AttributeMapper instance.
         */
        public AttributeMapper getAttributeMapper() {return attributeMapper;}

        /**
         * Returns <code>ActionMapper</code> for handling
         * <code>AuthorizationDecisionQuery</code> sent by the partner site.
         * @return ActionMapper instance.
         */
        public ActionMapper getActionMapper() {return actionMapper;}

        /**
         * Returns issuer value of the partner site.
         * @return String issuer value of the partner site.
         */
        public String getIssuer() {return _issuer;}

        /**
         * Returns set of hostname, ipaddress, and/or certificate alias of
         * partner site that is allowed to send soap request.
         * @return Set of hostname, ipaddress, and/or certificate alias.
         */ 
        public Set getHostSet() {
            if (origHostSet != null) {
                Set newSet = new HashSet();
                Iterator iter = origHostSet.iterator();
                while (iter.hasNext()) {
                    InetAddress []addrs = null;
                    String name = (String) iter.next();
                    try {
                        /* calling it everytime and rely on the jvm caching
                            policy.
                            For jdk1.3: use jvm option sun.net.inetaddr.ttl.
                            For jdk1.4: use key networkaddress.cache.ttl in
                                        java.security
                            Value -1 means cache forever; 0 means no caching;
                            A positive value, such as 10 means cache for 10
                            seconds.
                        */
                        addrs = InetAddress.getAllByName(name);
                        for (int m = 0, length = addrs.length; m < length; m++){
                            newSet.add(addrs[m].getHostAddress());
                        }
                    } catch (Exception ne) {
                        if (SAMLUtilsCommon.debug.warningEnabled()) {
                            SAMLUtilsCommon.debug.warning("SAMLServiceManager: "
                            + "getHostSet: possible wrong hostname in the "
                            + "host list.");
                        }
                    }
                    newSet.add(name);
                }
                return newSet;
            } else {
                return origHostSet;
            }
        }
    }

    private static void init() {
        SAMLUtilsCommon.debug.message("SAMLServiceManager.init:"
                + " Constructing a new instance of SAMLServiceManager");
        instance = new SAMLServiceManager();
        try {
            ci = ConfigurationManager.getConfigurationInstance(
                 SAMLConstants.SAML_SERVICE_NAME);
            ci.addListener(new SAMLServiceManager()); 
            setValues();
            String rmAssertion = SystemConfigurationUtil.getProperty(
                SAMLConstants.REMOVE_ASSERTION_NAME);
            if ((rmAssertion != null) && (rmAssertion.length() != 0)) {
                removeAssertion = (Boolean.valueOf(rmAssertion)).booleanValue();
            }
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("SAMLServiceManager.init()",e);
        }
    }

    /**
     * Returns the value of flag remove assertion.
     * @return true if need to remove assertion; false otherwise.
     */
    public static boolean getRemoveAssertion() {
        if (instance == null) {
            init();
        }
        return removeAssertion;
    }

    /**
     * Returns server protocol.
     * @return String server protocol.
     */
    public static String getServerProtocol() {
        if (instance == null) {
            init();
        }
        return serverProtocol;
    }

    /**
     * Returns server host.
     * @return String server host.
     */
    public static String getServerHost() {
        if (instance == null) {
            init();
        }
        return serverHost;
    }

    /**
     * Returns server port in string format.
     * @return String server port.
     */
    public static String getServerPort() {
        if (instance == null) {
            init();
        }
        return serverPort;
    }

    /**
     * Returns server URI.
     * @return server URI.
     */
    public static String getServerURI() {
        if (instance == null) {
            init();
        }
        return serverURI;
    }

    /**
     * Returns server url. It is in the format of
     * serverProtocol://serverhost:serverPort/serverURI.
     *
     * @return String server url.
     */
    public static String getServerURL() {
        if (instance == null) {
            init();
        }
        return serverURL;
    }

    // implemented as synchronized to preserve the event handling order.
    private static synchronized void setValues() {
        if (ci == null) {
            // set the values as default
            Map newMap = new HashMap();
            newMap.put(SAMLConstants.ARTIFACT_TIMEOUT_NAME,
                new Integer(SAMLConstants.ARTIFACT_TIMEOUT_DEFAULT));
            newMap.put(SAMLConstants.ASSERTION_TIMEOUT_NAME,
                new Integer(SAMLConstants.ASSERTION_TIMEOUT_DEFAULT));  
            newMap.put(SAMLConstants.ARTIFACT_NAME,
                                          SAMLConstants.ARTIFACT_NAME_DEFAULT);
            newMap.put(SAMLConstants.TARGET_SPECIFIER, 
                                        SAMLConstants.TARGET_SPECIFIER_DEFAULT);
            newMap.put(SAMLConstants.ASSERTION_MAX_NUMBER_NAME,
                new Integer(SAMLConstants.ASSERTION_MAX_NUMBER_DEFAULT));

            newMap.put(SAMLConstants.CLEANUP_INTERVAL_NAME,
                new Integer(SAMLConstants.CLEANUP_INTERVAL_DEFAULT));

            newMap.put(SAMLConstants.SIGN_REQUEST,  
                        Boolean.valueOf(SAMLConstants.SIGN_REQUEST_DEFAULT));

            newMap.put(SAMLConstants.SIGN_RESPONSE,
                        Boolean.valueOf(SAMLConstants.SIGN_RESPONSE_DEFAULT));
            
            newMap.put(SAMLConstants.SIGN_ASSERTION,
                        Boolean.valueOf(SAMLConstants.SIGN_ASSERTION_DEFAULT));
            map = newMap;
        } else {
            // set the values
            try {
                Map newMap = new HashMap();
                Map attrs = ci.getConfiguration(null, null); 
                // retrieve not before time skew period 
                Set values = (Set) 
                        attrs.get(SAMLConstants.NOTBEFORE_TIMESKEW_NAME);
                int value = SAMLConstants.NOTBEFORE_TIMESKEW_DEFAULT;
                if ((values != null) && (values.size() == 1)) {
                    try {
                        value = Integer.parseInt((String)
                            values.iterator().next());
                    } catch (NumberFormatException nfe) {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager:invalid"
                                + " not before time skew period value: " + value
                                + ", using default.", nfe);
                        value = SAMLConstants.NOTBEFORE_TIMESKEW_DEFAULT;
                    } 
                    if (value <= 0) {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager:invalid"
                                + " not before time skew period value=" + value
                                + ", using default.");
                        value = SAMLConstants.NOTBEFORE_TIMESKEW_DEFAULT;
                    }
                }
                Integer newValue = new Integer(value);
                newMap.put(SAMLConstants.NOTBEFORE_TIMESKEW_NAME, newValue);

                // retrieve artifact timeout
                values = (Set) 
                        attrs.get(SAMLConstants.ARTIFACT_TIMEOUT_NAME);
                value = SAMLConstants.ARTIFACT_TIMEOUT_DEFAULT;
                if ((values != null) && (values.size() == 1)) {
                    try {
                        value = Integer.parseInt((String)
                            values.iterator().next());
                    } catch (NumberFormatException nfe) {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager:invalid"
                                + " artifact timeout value: " + value
                                + ", using default.", nfe);
                        value = SAMLConstants.ARTIFACT_TIMEOUT_DEFAULT;
                    }
                    if (value <= 0) {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager:invalid"
                                + " artifact timeout value=" + value
                                + ", using default.");
                        value = SAMLConstants.ARTIFACT_TIMEOUT_DEFAULT;
                    }
                }
                newValue = new Integer(value);
                newMap.put(SAMLConstants.ARTIFACT_TIMEOUT_NAME, newValue);

                // retrieve assertion timeout
                values = (Set) attrs.get(SAMLConstants.ASSERTION_TIMEOUT_NAME);
                value = SAMLConstants.ASSERTION_TIMEOUT_DEFAULT;
                if ((values != null) && (values.size() == 1)) {
                    try {
                        value = Integer.parseInt((String)
                            values.iterator().next());
                    } catch (NumberFormatException nfe) {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager:invalid"
                                + " assertion timeout value: " + value
                                + ", using default.", nfe);
                        value = SAMLConstants.ASSERTION_TIMEOUT_DEFAULT;
                    }
                    if (value <= 0) {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager:invalid"
                                + " assertion timeout value=" + value
                                + ", using default.");
                        value = SAMLConstants.ASSERTION_TIMEOUT_DEFAULT;
                    }
                }
                newValue = new Integer(value);
                newMap.put(SAMLConstants.ASSERTION_TIMEOUT_NAME, newValue);

                values = (Set)
                        attrs.get(SAMLConstants.ASSERTION_MAX_NUMBER_NAME);
                value = SAMLConstants.ASSERTION_MAX_NUMBER_DEFAULT;
                if ((values != null) && (values.size() == 1)) {
                    try {
                        value = Integer.parseInt((String)
                            values.iterator().next());
                    } catch (NumberFormatException nfe) {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager:invalid"
                                + " assertion max number value: " + value
                                + ", using default.", nfe);
                        value = SAMLConstants.ASSERTION_MAX_NUMBER_DEFAULT;
                    }
                    if (value < 0) {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager:invalid"
                                + " assertion max number value=" + value
                                + ", using default.");
                        value = SAMLConstants.ASSERTION_MAX_NUMBER_DEFAULT;
                    }
                }
                newValue = new Integer(value);
                newMap.put(SAMLConstants.ASSERTION_MAX_NUMBER_NAME, newValue);

                values = (Set) attrs.get(SAMLConstants.CLEANUP_INTERVAL_NAME);
                value = SAMLConstants.CLEANUP_INTERVAL_DEFAULT;
                if ((values != null) && (values.size() == 1)) {
                    try {
                        value = Integer.parseInt((String)
                            values.iterator().next());
                    } catch (NumberFormatException nfe) {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager:invalid"
                                + " cleanup interval value: " + value
                                + ", using default.", nfe);
                        value = SAMLConstants.CLEANUP_INTERVAL_DEFAULT;
                    }
                    if (value <= 0) {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager:invalid"
                                + " cleanup interval value=" + value
                                + ", using default.");
                        value = SAMLConstants.CLEANUP_INTERVAL_DEFAULT;
                    }
                }
                newValue = new Integer(value);
                newMap.put(SAMLConstants.CLEANUP_INTERVAL_NAME, newValue);

                // retrieve the Artifact Name from the SAML saml config file 
                String artifactName = CollectionHelper.getMapAttr(
                    attrs, SAMLConstants.ARTIFACT_NAME,
                    SAMLConstants.ARTIFACT_NAME_DEFAULT); 
                newMap.put(SAMLConstants.ARTIFACT_NAME, artifactName);

                values = (Set)attrs.get(SAMLConstants.NAME_ID_FORMAT_MAP);
                Map nameIDFormatAttrMap = null;
                if ((values != null) && (!values.isEmpty())) {
                    for(Iterator iter = values.iterator(); iter.hasNext(); ) {
                        String str = (String)iter.next();
                        int index = str.indexOf("=");
                        if (index != -1) {
                            String nameIDFormat =
                                str.substring(0, index).trim();
                            String attrName = str.substring(index + 1).trim();
                            if ((nameIDFormat.length() != 0) && 
                                (attrName.length() != 0)) {

                                if (nameIDFormatAttrMap == null) {
                                    nameIDFormatAttrMap = new HashMap();
                                }
                                nameIDFormatAttrMap.put(nameIDFormat, attrName);
                            }
                        }
                    }
                    newMap.put(SAMLConstants.NAME_ID_FORMAT_MAP,
                        nameIDFormatAttrMap);
                }

                values = (Set)attrs.get(SAMLConstants.ATTRIBUTE_MAP);
                Map attrMap = null;
                if ((values != null) && (!values.isEmpty())) {
                    for(Iterator iter = values.iterator(); iter.hasNext(); ) {
                        String str = (String)iter.next();
                        int index = str.indexOf("=");
                        if (index != -1) {
                            String samlAttr =
                                str.substring(0, index).trim();
                            String localAttr = str.substring(index + 1).trim();
                            if ((samlAttr.length() != 0) && 
                                (localAttr.length() != 0)) {

                                if (attrMap == null) {
                                    attrMap = new HashMap();
                                }
                                attrMap.put(samlAttr, localAttr);
                            }
                        }
                    }
                    newMap.put(SAMLConstants.ATTRIBUTE_MAP, attrMap);
                }

                // get the targets which accept POST
                Set targets = (Set)attrs.get(SAMLConstants.POST_TO_TARGET_URLS);
                if ((targets == null) || (targets.size() == 0)) {
                    SAMLUtilsCommon.debug.message("SAMLServiceManager: No POST "
                        + "to targets found");
                } else {
                    Set targetsNoProtocol = Collections.synchronizedSet(
                        new HashSet());
                    // strip off protocol from the URL
                    Iterator it = targets.iterator();
                    String targetString = null;
                    while ( it.hasNext()) {
                        try {
                            targetString = (String)it.next();
                            URL url = new URL(targetString);
                            String targetNoProtocol = new StringBuffer(url.
                                getHost().toLowerCase()).append(":")
                                .append(String.valueOf(url.getPort()))
                                .append("/").append(url.getPath()).toString(); 
                            targetsNoProtocol.add(targetNoProtocol);
                        } catch (MalformedURLException me) {
                            SAMLUtilsCommon.debug.error("SAMLServiceManager: "
                                    + "Malformed Url in the POST to target "
                                    + "list, skipping entry:"+targetString);
                        }
                    }
                    if (targetsNoProtocol.size() > 0 ) {
                        newMap.put(SAMLConstants.POST_TO_TARGET_URLS, 
                            targetsNoProtocol);
                    } else {
                        SAMLUtilsCommon.debug.error("SAMLServiceManager: All"
                            +" POST to target URLs malformed");
                    }
                }
                
                // retrieve site id and site issuer name list

                // get my server host and port info
                serverProtocol = SystemConfigurationUtil.getProperty(
                     SAMLConstants.SERVER_PROTOCOL);
                serverHost = SystemConfigurationUtil.getProperty(
                    SAMLConstants.SERVER_HOST);
                serverPort = SystemConfigurationUtil.getProperty(
                    SAMLConstants.SERVER_PORT);
                serverURI = SystemConfigurationUtil.getProperty(
                    SAMLConstants.SERVER_URI);
                String legacyId = 
                    serverProtocol + "://" + serverHost + ":" + serverPort;
                serverURL = legacyId + serverURI;
                String sb = serverURL;
                    
                Map siteidMap = new HashMap();
                Map issuerNameMap = new HashMap();
                Map instanceMap = new HashMap(); 
                Set siteIDNameList = (Set) attrs.get(
                                SAMLConstants.SITE_ID_ISSUER_NAME_LIST);
                if (siteIDNameList.size() == 0) {
                    SAMLUtilsCommon.debug.error("SAMLServiceManager: No Site ID"
                        + " or Issuer Name in the SAML service config.");
                 } else {
                    String entry = null;
                    StringTokenizer tok1 = null;
                    String instanceID = null;
                    String siteID = null;
                    String issuerName = null;
                    String element = null;
                    String key = null;
                    Iterator iter = siteIDNameList.iterator();
                    while (iter.hasNext()) {
                        entry = (String) iter.next();

                        // reset
                        instanceID = null;
                        siteID = null;
                        issuerName = null;

                        tok1 = new StringTokenizer(entry, "|");
                        while (tok1.hasMoreElements()) {
                            element = tok1.nextToken();
                            int pos = -1;
                            if ((pos = element.indexOf("=")) == -1) {
                                SAMLUtilsCommon.debug.error("SAMLSManager: "
                                    + "wrong format: " + element);
                                break;
                            }
                            int nextpos = pos + 1 ;
                            if (nextpos >= element.length()) {
                                SAMLUtilsCommon.debug.error("SAMLSManager: "
                                    + "wrong format: " + element);
                                break;
                            }
                            key = element.substring(0, pos);
                            if (key.equalsIgnoreCase(SAMLConstants.INSTANCEID))
                            {
                                instanceID = element.substring(nextpos);
                            } else if (key.equalsIgnoreCase(
                                                SAMLConstants.SITEID))
                            {
                                siteID = element.substring(nextpos);
                            } else if (key.equalsIgnoreCase(
                                                SAMLConstants.ISSUERNAME))
                            {
                                issuerName = element.substring(nextpos);
                            } else {
                                SAMLUtilsCommon.debug.error("SAMLSManager: "
                                    + "wrong format: " + element);
                            }
                        } // end of looping tokens in each entry

                        if (instanceID == null) {
                            SAMLUtilsCommon.debug.error("SAMLServiceManager: "
                                + "missing instanceID:" + entry);
                            break;
                        }
                        boolean thisSite = instanceID.equalsIgnoreCase(sb) ||
                            instanceID.equalsIgnoreCase(legacyId);
                        if (siteID != null) {
                            siteID = SAMLUtilsCommon.getDecodedSourceIDString(
                                siteID);
                            if (siteID != null) {
                                siteidMap.put(instanceID, siteID);
                                instanceMap.put(siteID, instanceID); 
                                if (SAMLUtilsCommon.debug.messageEnabled()) {
                                    SAMLUtilsCommon.debug.message("SAMLSMangr: "
                                        + "add instanceID: " + instanceID
                                        + ", serverURL=" + sb 
                                        + ", legacy serverURL=" + legacyId 
                                        + ", isthissite=" + thisSite);
                                }
                                if (thisSite) {
                                    newMap.put(SAMLConstants.SITE_ID, siteID);
                                }
                            }
                        }
                        if (issuerName != null) {
                            issuerNameMap.put(instanceID, issuerName);
                            if (thisSite) {
                                newMap.put(SAMLConstants.ISSUER_NAME,
                                                        issuerName);
                            }
                        }
                    } // end of looping all the entries in the list
                }
                // set default site id
                if (!siteidMap.containsKey(sb) && 
                    !siteidMap.containsKey(legacyId)) {
                    String siteID = SAMLSiteID.generateSourceID(sb);
                    if (SAMLUtilsCommon.debug.warningEnabled()) {
                        SAMLUtilsCommon.debug.warning("SAMLSManager: site " + sb
                            + " not configured, create new " + siteID);
                    }
                    if (siteID != null) {
                        siteID = SAMLUtilsCommon.getDecodedSourceIDString(
                            siteID);
                        if (siteID != null) {
                            siteidMap.put(sb, siteID);
                            instanceMap.put(siteID, sb); 
                            newMap.put(SAMLConstants.SITE_ID, siteID);
                        } else {
                            SAMLUtilsCommon.debug.error("Missing Site ID.");
                        }
                    }
                }
                // set default issuer name
                if (!issuerNameMap.containsKey(sb) && 
                    !issuerNameMap.containsKey(legacyId)) {
                    if (SAMLUtilsCommon.debug.warningEnabled()) {
                        SAMLUtilsCommon.debug.warning("SAMLSManager:issuer for "
                            + sb + " not configured, set to " + sb);
                    }
                    issuerNameMap.put(sb, sb);
                    newMap.put(SAMLConstants.ISSUER_NAME, sb);
                }
                newMap.put(SAMLConstants.SITE_ID_LIST, siteidMap);
                newMap.put(SAMLConstants.INSTANCE_LIST, instanceMap); 
                newMap.put(SAMLConstants.ISSUER_NAME_LIST, issuerNameMap);
                
                Boolean signRequest = Boolean.valueOf(
                    CollectionHelper.getMapAttr(
                        attrs, SAMLConstants.SIGN_REQUEST,
                        SAMLConstants.SIGN_REQUEST_DEFAULT));
                newMap.put(SAMLConstants.SIGN_REQUEST, signRequest);

                Boolean signResponse = Boolean.valueOf(
                    CollectionHelper.getMapAttr(
                        attrs, SAMLConstants.SIGN_RESPONSE,
                        SAMLConstants.SIGN_RESPONSE_DEFAULT));
                newMap.put(SAMLConstants.SIGN_RESPONSE, signResponse);

                Boolean signAssertion = Boolean.valueOf(
                    CollectionHelper.getMapAttr(
                        attrs, SAMLConstants.SIGN_ASSERTION,
                        SAMLConstants.SIGN_ASSERTION_DEFAULT));
                newMap.put(SAMLConstants.SIGN_ASSERTION, signAssertion);

                //retrieve target name 
                String targetName = CollectionHelper.getMapAttr(
                    attrs, SAMLConstants.TARGET_SPECIFIER,
                    SAMLConstants.TARGET_SPECIFIER_DEFAULT);
                newMap.put(SAMLConstants.TARGET_SPECIFIER, targetName); 

                 
                //retrieve the partner URL list 
                Set soapRevList = Collections.synchronizedSet(new HashSet()); 
                soapRevList = (Set)attrs.get(SAMLConstants.PARTNER_URLS); 
                if (soapRevList.size() == 0) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("SAMLServiceManager: " 
                            + "No entry in partner url config!");     
                    }
                } else {
                    Set _Sites = Collections.synchronizedSet(new HashSet()); 
                    Map _Soaps = Collections.synchronizedMap(new HashMap()); 
                    Object[] soapObjects = soapRevList.toArray();
                    int size = soapObjects.length; 
                    String e = null; 
                    String element = null; 
                    for (int i = 0; i < size; i++) {
                        String _siteID = null; 
                        String _samlUrl = null;
                        String postUrl = null;
                        String host = null; 
                        int port = -1; 
                        String _destID = null; 
                        String _soapRevUrl = null;
                        String _authType = null; 
                        String _user = null; 
                        String basic_auth_user = null;
                        String basic_auth_passwd = null;
                        String _certAlias = null;
                        String preferVersion = null; 
                        PartnerAccountMapper _partnerAccountMapper = null;
                        SiteAttributeMapper _siteAttributeMapper = null;
                        PartnerSiteAttributeMapper _partnerSiteAttributeMapper
                            = null;
                        ConsumerSiteAttributeMapper consumerSiteAttrMapper
                            = null;
                        NameIdentifierMapper niMapper = null;
                        AttributeMapper attrMapper = null;
                        ActionMapper actionMapper = null;
                        String _issuer = null;
                        Set hostSet = null;
                        Set origHostSet = null;
                        e = (String) soapObjects[i];
                        // retrieve the trusted server list
                        if (e.toUpperCase().indexOf(SAMLConstants.SOURCEID)==-1)
                        {
                            SAMLUtilsCommon.debug.error("Ignore this trusted " 
                                + "site since SourceID is absent:"+e);
                            continue; 
                        }
                        StringTokenizer tok1 = new StringTokenizer(e, "|");
                        while (tok1.hasMoreElements()) {    
                            // break on "|"
                            element = tok1.nextToken();
                            if (SAMLUtilsCommon.debug.messageEnabled()) {
                                SAMLUtilsCommon.debug.message("SAMLSManager:" +
                                    " PartnerUrl List:" +  element);
                            }
            
                          //manually break on "=" since sourceid may contain "="
                            int pos = -1; 
                            //ignore the attribute which not include "="
                            if ((pos = element.indexOf("=")) == -1) {
                                SAMLUtilsCommon.debug.error("SAMLSManager:" +
                                    " illegal format of PartnerUrl:"
                                    +element);    
                                break;
                            }
                            int nextpos = pos + 1 ;
                            //ignore the attribute which is like "SOAPUrl="  
                            if (nextpos >= element.length()) {
                                break;
                            }
                               
                            String key = element.substring(0, pos);
                            if (key.equalsIgnoreCase(SAMLConstants.SOURCEID)) {
                                _destID = 
                                    SAMLUtilsCommon.getDecodedSourceIDString(
                                    element.substring(nextpos));
                            } else if (key.equalsIgnoreCase(
                                                    SAMLConstants.TARGET)) {
                                _siteID = element.substring(nextpos);   
                            } else if (key.equalsIgnoreCase(
                                                    SAMLConstants.SAMLURL)) {
                                _samlUrl = element.substring(nextpos).trim(); 
                            } else if (key.equalsIgnoreCase(
                                                    SAMLConstants.POSTURL)) {
                                postUrl = element.substring(nextpos).trim(); 
                            } else if (key.equalsIgnoreCase(
                                                    SAMLConstants.SOAPUrl)) {
                                _soapRevUrl = element.substring(nextpos).trim();
                            } else if (key.equalsIgnoreCase(
                                                    SAMLConstants.AUTHTYPE)) {
                                _authType = element.substring(nextpos); 
                                if (SAMLUtilsCommon.debug.messageEnabled()) {
                                    SAMLUtilsCommon.debug.message("authtype =" +
                                                            _authType); 
                                }
                            } else if (key.equalsIgnoreCase(SAMLConstants.UID)){
                                _user = element.substring(nextpos);
                                if (SAMLUtilsCommon.debug.messageEnabled()) {
                                    SAMLUtilsCommon.debug.message("user = "
                                        + _user);
                                }
                            } else if (key.equalsIgnoreCase(
                                        SAMLConstants.AUTH_UID))
                            {
                                basic_auth_user = element.substring(nextpos);
                                if (SAMLUtilsCommon.debug.messageEnabled()) {
                                    SAMLUtilsCommon.debug.message(
                                        "basic auth user=" + basic_auth_user);
                                }
                            } else if (key.equalsIgnoreCase(
                                        SAMLConstants.AUTH_PASSWORD))
                            {
                                basic_auth_passwd = SAMLUtilsCommon.
                                    decodePassword(
                                    element.substring(nextpos));
                            } else if (key.equalsIgnoreCase(
                                             SAMLConstants.ACCOUNTMAPPER))
                            {  
                                try {
                                    Object temp = Class.forName(
                                      element.substring(nextpos)).newInstance();
                                    if (temp instanceof 
                                         PartnerAccountMapper) {
                                        _partnerAccountMapper =
                                           (PartnerAccountMapper) temp;
                                    } else {
                                        SAMLUtilsCommon.debug.error(
                                        "SAMLServiceManager:Invalid account " +
                                        "mapper");
                                    }
                                } catch (InstantiationException ie) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"+
                                                          ie); 
                                } catch (IllegalAccessException ae) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"+
                                                          ae); 
                                } catch (ClassNotFoundException ce) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"+
                                                          ce); 
                                    _partnerAccountMapper = null;
                                }
                            } else if (key.equalsIgnoreCase(
                                         SAMLConstants.PARTNERACCOUNTMAPPER)) {
                                // for backward compatibility
                                try {
                                    _partnerAccountMapper =
                                           (PartnerAccountMapper) Class.
                                           forName(element.substring(nextpos)).
                                           newInstance();
                                } catch (InstantiationException ie) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"
                                                          , ie); 
                                } catch (IllegalAccessException ae) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"
                                                          , ae); 
                                } catch (ClassNotFoundException ce) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"
                                                          , ce); 
                                    _partnerAccountMapper = null;
                                }
                            } else if (key.equalsIgnoreCase(
                                            SAMLConstants.CERTALIAS)) {
                                _certAlias = element.substring(nextpos);
                                if (SAMLUtilsCommon.debug.messageEnabled()) {
                                    SAMLUtilsCommon.debug.message("certAlias = "
                                        + _certAlias);
                                }
                            } else if (key.equalsIgnoreCase(
                                           SAMLConstants.SITEATTRIBUTEMAPPER)) {
                                try {
                                    Object temp = Class.forName(
                                      element.substring(nextpos)).newInstance();
                                    if (temp instanceof SiteAttributeMapper) {
                                        _siteAttributeMapper = 
                                            (SiteAttributeMapper) temp;
                                    } else if (temp instanceof 
                                         PartnerSiteAttributeMapper) {
                                        _partnerSiteAttributeMapper =
                                           (PartnerSiteAttributeMapper) temp;
                                    } else if (temp instanceof 
                                         ConsumerSiteAttributeMapper) {
                                        consumerSiteAttrMapper =
                                           (ConsumerSiteAttributeMapper) temp;
                                    } else {
                                        SAMLUtilsCommon.debug.error(
                                        "SAMLServiceManager:Invalid site " +
                                        "attribute mapper");
                                    }
                                } catch (InstantiationException ie) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"+
                                                          ie); 
                                } catch (IllegalAccessException ae) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"+
                                                          ae); 
                                } catch (ClassNotFoundException ce) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"+
                                                          ce); 
                                    _siteAttributeMapper = null;
                                }
                            } else if (key.equalsIgnoreCase(
                                   SAMLConstants.PARTNERSITEATTRIBUTEMAPPER)) {
                                try {
                                    Object temp = Class.forName(
                                      element.substring(nextpos)).newInstance();
                                    if (temp instanceof 
                                         PartnerSiteAttributeMapper) {
                                        _partnerSiteAttributeMapper =
                                           (PartnerSiteAttributeMapper) temp;
                                    } else if (temp instanceof 
                                         ConsumerSiteAttributeMapper) {
                                        consumerSiteAttrMapper =
                                           (ConsumerSiteAttributeMapper) temp;
                                    } else {
                                        SAMLUtilsCommon.debug.error(
                                        "SAMLServiceManager:Invalid site " +
                                        "partner attribute mapper");
                                    }
                                } catch (InstantiationException ie) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"
                                                          , ie); 
                                } catch (IllegalAccessException ae) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"
                                                          , ae); 
                                } catch (ClassNotFoundException ce) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"
                                                          , ce); 
                                    _partnerSiteAttributeMapper= null;
                                }
                            } else if (key.equalsIgnoreCase(
                                SAMLConstants.NAMEIDENTIFIERMAPPER)) {
                                try {
                                    niMapper = (NameIdentifierMapper)
                                         Class.forName(element.substring(
                                         nextpos)).newInstance();
                                } catch (Exception ex) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:",
                                        ex);
                                }
                            } else if (key.equalsIgnoreCase(
                                        SAMLConstants.ATTRIBUTEMAPPER)) {
                                try {
                                    attrMapper = (AttributeMapper) Class.
                                            forName(element.substring(nextpos)).
                                            newInstance();
                                } catch (Exception ex) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"
                                        + ex);
                                }
                            } else if (key.equalsIgnoreCase(
                                        SAMLConstants.ACTIONMAPPER)) {
                                try {
                                    actionMapper = (ActionMapper) Class.
                                           forName(element.substring(nextpos)).
                                           newInstance();
                                } catch (Exception ex) {
                                    SAMLUtilsCommon.debug.error("SAMLSManager:"
                                        + ex);
                                }
                            } else if (key.equalsIgnoreCase(
                                                    SAMLConstants.ISSUER)) {
                                _issuer = element.substring(nextpos).trim();
                                if (SAMLUtilsCommon.debug.messageEnabled()) {
                                    SAMLUtilsCommon.debug.message(
                                        "issuer = "+ _issuer);
                                }
                            } else if (key.equalsIgnoreCase(
                                SAMLConstants.HOST_LIST)) {
                                origHostSet = new HashSet();
                                hostSet = new HashSet();
                                /* calling InetAddress.getAllByName here has
                                    two purposes:
                                        - emmit any errors if there is any
                                        - call the getAllByName method to init
                                        the jvm caching
                                */
                                StringTokenizer st = new StringTokenizer(
                                    element.substring(nextpos),",");
                                InetAddress []addr = null;
                                while (st.hasMoreTokens()) {
                                    String token = st.nextToken().trim();
                                    try {
                                        addr = InetAddress.getAllByName(token);
                                        for (int m = 0, length = addr.length;
                                                        m < length; m++)
                                        {
                                            hostSet.add(
                                                addr[m].getHostAddress());
                                        }
                                    } catch (Exception ne) {
                                        if (SAMLUtilsCommon.debug.warningEnabled()) {
                                            SAMLUtilsCommon.debug.warning(
                                                "SAML Service"
                                                + " Manager: possible wrong " 
                                                + "hostname in the host list.");
                                        }
                                    }
                                    //add here anyways, since
                                    // it could be an alias name too
                                    hostSet.add(token); 
                                    origHostSet.add(token);
                                    }
                                if (SAMLUtilsCommon.debug.messageEnabled()) {
                                    SAMLUtilsCommon.debug.message("hostSet = "
                                        +hostSet);
                                }
                            } else if (key.equalsIgnoreCase(
                                       SAMLConstants.VERSION)) {
                                preferVersion = element.substring(nextpos);   
                            }
                        }
                        //provide default auth type 
                        if (_authType == null) {
                            _authType = SAMLConstants.NOAUTH; 
                        }
                       
                        // provide default AccountMapper
                        if (_partnerAccountMapper == null) {
                            try {
                                _partnerAccountMapper = (PartnerAccountMapper)
                                  Class.forName(
                                  DEFAULT_PARTNER_ACCOUNT_MAPPER).newInstance();
                            } catch (Exception ex0) {
                                // ignore
                            }
                        }
 
                        // provide default AttributeMapper
                        if (attrMapper == null) {
                            attrMapper = new DefaultAttributeMapper();
                        }
                        
                        // default version 
                        if (preferVersion == null ||
                            preferVersion.length() == 0)
                        {
                            try { 
                                preferVersion = SystemConfigurationUtil.
                                    getProperty(SAMLConstants.
                                    SAML_PROTOCOL_VERSION).trim();
                            } catch (Exception pe) {
                                preferVersion =
                                    SAMLConstants.PROTOCOL_VERSION_1_0; 
                            }
                        }
                        
                        // create truseted server set 
                        if (_destID == null || _destID.length() == 0)
                        { 
                           SAMLUtilsCommon.debug.error("Ignore this trusted " +
                                "site since SourceID is misconfigured: " + e);
                        } else {
                            if (_siteID == null || _siteID.length() == 0 || 
                                ((_samlUrl == null || _samlUrl.length() == 0) &&
                                (postUrl == null || postUrl.length() == 0)))
                            {
                                SAMLUtilsCommon.debug.warning("Target or both"
                                    +" SAMLUrl and POSTUrl are misconfigured:"
                                    +e);
                            }
                            if (_siteID != null && _siteID.length() != 0) {
                                StringTokenizer tok2 = 
                                             new StringTokenizer(_siteID, ","); 
                                while (tok2.hasMoreElements()) {
                                    String el = tok2.nextToken();
                                    if (SAMLUtilsCommon.debug.messageEnabled()){
                                        SAMLUtilsCommon.debug.message( 
                                            "SAMLServiceManager:target= " + el);
                                    }

                                    // break the target url to host and port 
                                    StringTokenizer pt=
                                        new StringTokenizer(el, ":");
                                    if (pt.countTokens() == 2) {
                                        host = pt.nextToken().trim();
                                        port = Integer.parseInt(pt.nextToken().
                                                            trim());
                                    } else {
                                        host = el; 
                                        port = -1; 
                                    }
                                }
                                SiteEntry server = new SiteEntry(host, port, 
                                    _destID, _samlUrl, postUrl, preferVersion);
                                _Sites.add(server); 
                            }
                           // create the soap receiver map                      
                            SOAPEntry server = new SOAPEntry(
                                   _destID, _soapRevUrl, _authType,
                                   _user, basic_auth_user, basic_auth_passwd,
                                    _certAlias,
                                   _partnerAccountMapper, _siteAttributeMapper,
                                   _partnerSiteAttributeMapper, 
                                   consumerSiteAttrMapper, niMapper,
                                   attrMapper,actionMapper, _issuer, 
                                   origHostSet,preferVersion);
                            _Soaps.put(_destID, server); 
                            if(_issuer != null) {
                               _Soaps.put(_issuer, server);
                            }
                        }     
                    }
                    newMap.put(SAMLConstants.TRUSTED_SERVER_LIST , _Sites); 
                    newMap.put(SAMLConstants.PARTNER_URLS, _Soaps); 
                }
                map = newMap;
            } catch (Exception e) {
                SAMLUtilsCommon.debug.error("SAMLServiceManager.setValues:"
                                + " Exception:", e);
            }
        }
    }

    /**
     * Retrieves current value of an AttributeSchema in the SAML
     * ServiceSchema.
     * @param attributeName the name of the attributeSchema.
     * @return the value of the attribute schema. It could return null if
     *            input attibuteName is null, or the attributeName can not be
     *            found in the service schema.
     */
    public static synchronized Object getAttribute(String attributeName) {
        if (instance == null) {
            init();
        }
        return map.get(attributeName);
    }
    
    /**
     * Returns corresponding Authentication method URI to be set in Assertion.
     * @param authModuleName name of the authentication module used to
     *          authenticate the user.
     * @return String corresponding Authentication Method URI to be set in
     *          Assertion.
     */
    public static String getAuthMethodURI(String authModuleName) {
        if (authModuleName == null) {
            return null;
        }

        if (authModuleName.equalsIgnoreCase(SAMLConstants.AUTH_METHOD_CERT)) {
            return SAMLConstants.AUTH_METHOD_CERT_URI;
        }
        if (authModuleName.equalsIgnoreCase(SAMLConstants.AUTH_METHOD_KERBEROS))
        {
            return SAMLConstants.AUTH_METHOD_KERBEROS_URI;
        }
        if (SAMLConstants.passwordAuthMethods.contains(
            authModuleName.toLowerCase()))
        {
            return SAMLConstants.AUTH_METHOD_PASSWORD_URI;
        }
        if (SAMLConstants.tokenAuthMethods.contains(
            authModuleName.toLowerCase()))
        {
            return SAMLConstants.AUTH_METHOD_HARDWARE_TOKEN_URI;
        } else {
            StringBuffer sb = new StringBuffer(100);
            sb.append(SAMLConstants.AUTH_METHOD_URI_PREFIX).
                        append(authModuleName);
            return sb.toString();
        }
    }
    
    /**
     * Updates SAML configuration when SAML service's configuration
     * data has been changed.
     *
     * @param e Configuration action event, like ADDED, DELETED, MODIFIED etc.
     */
    public void configChanged(ConfigurationActionEvent e) {
        if (SAMLUtilsCommon.debug.messageEnabled()) {
            SAMLUtilsCommon.debug.message("SAMLServiceManager:configChanged");
        }
        setValues();
    }
}
