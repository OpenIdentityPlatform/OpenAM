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
 * $Id: AMSetupServlet.java,v 1.117 2010/01/20 17:01:35 veiming Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2017-2025 3A Systems, LLC.
 */

package com.sun.identity.setup;

import static com.sun.identity.setup.AMSetupUtils.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.forgerock.openam.entitlement.utils.EntitlementUtils.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.forgerock.openam.utils.IOUtils.*;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.license.License;
import org.forgerock.openam.license.LicenseLocator;
import org.forgerock.openam.license.LicenseSet;
import org.forgerock.openam.license.ServletContextLicenseLocator;
import org.forgerock.openam.setup.EmbeddedOpenDJManager;
import org.forgerock.openam.setup.ZipUtils;
import org.forgerock.openam.upgrade.EmbeddedOpenDJBackupManager;
import org.forgerock.openam.upgrade.OpenDJUpgrader;
import org.forgerock.openam.upgrade.VersionUtils;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.opendj.config.ConfigurationFramework;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.am.util.AdminUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.naming.service.NamingService;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.UI.LoginLogoutMapping;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.internal.server.SMSAuthModule;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.common.ConfigMonitoring;
import com.sun.identity.common.DebugPropertiesObserver;
import com.sun.identity.common.FqdnValidator;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.ServerConfigXML;
import com.sun.identity.common.configuration.ServerConfigXML.DirUserObject;
import com.sun.identity.common.configuration.ServerConfigXML.ServerGroup;
import com.sun.identity.common.configuration.ServerConfigXML.ServerObject;
import com.sun.identity.common.configuration.ServerConfigXMLObserver;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.CachedSMSEntry;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSPropertiesObserver;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * This class is the first class to get loaded by the Servlet container.
 *
 * It has helper methods to determine the status of OpenAM configuration when deployed as a single
 * web-application. If OpenAM is not deployed as single web-application then the configured status
 * returned is always true.
 */
public class AMSetupServlet extends HttpServlet {

    private static ServletContext servletCtx = null;
    private static boolean isConfiguredFlag = false;
    private static boolean isVersionNewer = false;
    private static boolean upgradeCompleted = false;
    private static boolean isOpenDJUpgraded = false;
    private final static String SMS_STR = "sms";
    private static SSOToken adminToken = null;
    private final static String LEGACY_PROPERTIES = "legacy";

    final static String BOOTSTRAP_EXTRA = "bootstrap";    
    final static String BOOTSTRAP_FILE_LOC = "bootstrap.file";
    final static String OPENDS_DIR = "/opends";

    private static String errorMessage = null;
    private static java.util.Locale configLocale;

    private static Set<String> passwordParams = new HashSet<String>();

    static {
        passwordParams.add(SetupConstants.CONFIG_VAR_DS_MGR_PWD);
        passwordParams.add(SetupConstants.CONFIG_VAR_ADMIN_PWD);
        passwordParams.add(SetupConstants.CONFIG_VAR_CONFIRM_ADMIN_PWD);
        passwordParams.add(SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD);
        passwordParams.add(SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM);
        passwordParams.add(SetupConstants.USER_STORE_LOGIN_PWD);
        passwordParams.add(SetupConstants.CONFIG_VAR_ENCRYPTION_KEY);
        passwordParams.add(SetupConstants.USERSTORE_PWD);
    }

    /*
     * Initializes the servlet.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.setProperty("file.separator", "/");
        if (servletCtx == null ) {
            servletCtx = config.getServletContext();
        }
        checkOpenDJUpgrade();
        checkConfigProperties();
        LoginLogoutMapping.setProductInitialized(isConfiguredFlag);
        registerListeners();
        
        if (isConfiguredFlag) {
            // this will sync up bootstrap file will serverconfig.xml
            // due startup; and also register the observer.
            ServerConfigXMLObserver.getInstance().update(true);

            // Syncup embedded opends replication with current server instances.
            if (!syncServerInfoWithRelication()) {
                Debug.getInstance(SetupConstants.DEBUG_NAME)
                        .error("AMSetupServlet.init: embedded replication sync failed.");
            }
        }
        
        isVersionNewer();

        final String[] licenseFilePaths =
                InjectorHolder.getInstance(Key.get(String[].class, Names.named("LICENSE_FILEPATH")));
        if (licenseFilePaths == null) {
            throw new ServletException("Could not get license file paths.");
        }
        licenseLocator = new ServletContextLicenseLocator(getServletContext(), Charset.forName("UTF-8"),
                licenseFilePaths);
    }

    private static LicenseLocator licenseLocator;

    public static LicenseLocator getLicenseLocator() {
        return licenseLocator;
    }

    /*
     * Flag indicating if OpenAM is configured with the latest valid config
     */  
    static public boolean isCurrentConfigurationValid() {
        if (isConfiguredFlag) {
            isVersionNewer = VersionUtils.isVersionNewer();
        }
        return isConfiguredFlag && !isVersionNewer && !upgradeCompleted;
    } 
    
    static public boolean isConfigured() {
        return isConfiguredFlag;
    }

    /**
     * Used this method to check if the version is newer; called post upgrade
     */
    private static void isVersionNewer() {
        if (isConfiguredFlag) {
            isVersionNewer = VersionUtils.isVersionNewer();
        }       
    }

    public static void upgradeCompleted() {
        upgradeCompleted = true;
    }

    public static boolean isUpgradeCompleted() {
        return upgradeCompleted;
    }

    public static boolean isOpenDJUpgraded() {
        return isOpenDJUpgraded;
    }

    public static void enableDebug() {
        Collection<Debug> debugInstances = Debug.getInstances();
        for (Debug d : debugInstances) {
            d.setDebug(Debug.MESSAGE);
        }
    }
    
    /**
     * Checks if the embedded directory (if present) needs to be upgraded
     */
    private static void checkOpenDJUpgrade() throws ServletException {
        if (!isEmbeddedDS()) {
            return;
        }
        String baseDirectory = getBaseDir();
        Debug logger = Debug.getInstance(SetupConstants.DEBUG_NAME);
        ZipUtils zipUtils = new ZipUtils(logger);
        OpenDJUpgrader upgrader = new OpenDJUpgrader(new EmbeddedOpenDJBackupManager(logger, zipUtils, baseDirectory),
                baseDirectory + OPENDS_DIR, servletCtx);
        EmbeddedOpenDJManager embeddedOpenDJManager = new EmbeddedOpenDJManager(logger, baseDirectory, upgrader);
        if (embeddedOpenDJManager.getState() == EmbeddedOpenDJManager.State.UPGRADE_REQUIRED) {
            isOpenDJUpgraded = embeddedOpenDJManager.upgrade() == EmbeddedOpenDJManager.State.UPGRADED;
        }
    }
    
    /**
     * Checks if the product is already configured. This is required 
     * when the container on which WAR is deployed is restarted. If  
     * product is configured the flag is set true. Also the flag is
     * set to true in case of non-single war deployment.
     */
    private static void checkConfigProperties() {
        String overrideAMC = SystemProperties.get(SetupConstants.AMC_OVERRIDE_PROPERTY);
        isConfiguredFlag = overrideAMC == null || overrideAMC.equalsIgnoreCase("false");
        
        if (!isConfiguredFlag && servletCtx != null) {
            // This call has side effect that sets the System base dir property
            String baseDir = getBaseDir();
            try {
                if (canBootstrap()) {
                    isConfiguredFlag = Bootstrap.load(new BootstrapData(baseDir), false);
                } else if (baseDir != null) {
                        isConfiguredFlag = loadAMConfigProperties(baseDir + "/" + SetupConstants.AMCONFIG_PROPERTIES);
                }
            } catch (ConfiguratorException e) {
                //ignore, WAR may not be configured yet.
            	System.out.println("checkConfigProperties :" + e);
            } catch (Exception e) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.checkConfigProperties", e);
            }
        }
    }
    
    private static boolean loadAMConfigProperties(String fileLocation) throws IOException {
        boolean loaded = false;
        File test = new File(fileLocation);
        
        if (test.exists()) {
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(fileLocation);
                Properties props = new Properties();
                props.load(fin);
                SystemProperties.initializeProperties(props);
                loaded = true;
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        }
        return loaded;
    }
    
    /**
     * Invoked from the filter to decide which page needs to be 
     * displayed.
     * @param servletctx is the Servlet Context
     * @return true if AM is already configured, false otherwise 
     */
    public static boolean isConfigured(ServletContext servletctx) {
        return isConfiguredFlag;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException,
            ConfiguratorException {

        // Only continue if we are not already configured
        if (isConfigured()) {
            response.getWriter().write("Already Configured!") ;
            return;
        }

        HttpServletRequestWrapper req = new HttpServletRequestWrapper(request);
        HttpServletResponseWrapper res = new HttpServletResponseWrapper(response);
           
        String loadBalancerHost = request.getParameter("LB_SITE_NAME");
        String primaryURL = request.getParameter("LB_PRIMARY_URL");

        if (loadBalancerHost != null) {     
            // site configuration is passed as a map of the site 
            // information 
            Map<String, String> siteConfig = new HashMap<String, String>(5);
            siteConfig.put(SetupConstants.LB_SITE_NAME, loadBalancerHost);
            siteConfig.put(SetupConstants.LB_PRIMARY_URL, primaryURL);
            req.addParameter(SetupConstants.CONFIG_VAR_SITE_CONFIGURATION, siteConfig);
        }
                
        String userStoreType = request.getParameter("USERSTORE_TYPE");        

        if (userStoreType != null) {
            // site configuration is passed as a map of the site information 
            Map<String, String> store = new HashMap<String, String>(12);
            String tmp = request.getParameter("USERSTORE_DOMAINNAME");
            String domainName = tmp;
            store.put(SetupConstants.USER_STORE_DOMAINNAME, tmp);
            tmp = request.getParameter("USERSTORE_HOST");
            if (tmp == null || tmp.length() == 0) {
                String[] hostAndPort = {""};
                try {
                    if (domainName != null && domainName.length() > 0) {
                        hostAndPort = getLdapHostAndPort(domainName);
                    }
                } catch (NamingException nex) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME)
                        .error("AMSetupServlet:Naming Exception get host and port from domain name" + nex);
                        
                } catch (IOException ioex) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME)
                        .error("AMSetupServlet:IO Exception. get host and port from domain name" + ioex);
                }
                String host = hostAndPort[0];
                String port = hostAndPort[1];
                if (host != null) {
                    store.put(SetupConstants.USER_STORE_HOST, host);
                    store.put(SetupConstants.USER_STORE_PORT, port);
                }
            } else {
                store.put(SetupConstants.USER_STORE_HOST, tmp);
                tmp = request.getParameter("USERSTORE_PORT");
                store.put(SetupConstants.USER_STORE_PORT, tmp);
            }
            tmp = request.getParameter("USERSTORE_SSL");
            store.put(SetupConstants.USER_STORE_SSL, tmp);
            tmp = request.getParameter("USERSTORE_SUFFIX");
            if (tmp == null || tmp.length() == 0) {
                if (StringUtils.isEmpty(domainName)) {
                    String umRootSuffix = dnsDomainToDN(domainName);
                    store.put(SetupConstants.USER_STORE_ROOT_SUFFIX, umRootSuffix);
                }
            } else {
                store.put(SetupConstants.USER_STORE_ROOT_SUFFIX, tmp);
            }
            tmp = request.getParameter("USERSTORE_MGRDN");
            store.put(SetupConstants.USER_STORE_LOGIN_ID, tmp);      
            tmp = request.getParameter("USERSTORE_PASSWD");
            store.put(SetupConstants.USER_STORE_LOGIN_PWD, tmp);      
            store.put(SetupConstants.USERSTORE_PWD, tmp);      
            store.put(SetupConstants.USER_STORE_TYPE, userStoreType);

            req.addParameter(SetupConstants.USER_STORE, store);
        }
        
        boolean result = processRequest(req, res);
       
        if (!result) {
            response.getWriter().write("Configuration failed - check installation logs!");
        } else {
            response.getWriter().write("Configuration complete!");
        }
    }

    public static boolean processRequest(IHttpServletRequest request, IHttpServletResponse response) {

        // Only continue if we are not already configured
        if (isConfigured()) {
            return true;
        }

        setLocale(request);
        final InstallLog installLog = InstallLog.getInstance();
        installLog.open((String) request.getParameterMap().get(SetupConstants.CONFIG_VAR_BASE_DIR));

        /*
         * This logic needs refactoring later. setServiceConfigValues()
         * attempts to check if directory is up and makes a call
         * back to this class. The implementation'd
         * be cleaner if classes&methods are named better and separated than
         * intertwined together.
         */
        ServicesDefaultValues.setServiceConfigValues(request);

        // set debug directory
        Map<String, Object> map = ServicesDefaultValues.getDefaultValues();
        String basedir = (String) map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
        String uri = (String) map.get(SetupConstants.CONFIG_VAR_SERVER_URI);
        SystemProperties.initializeProperties(Constants.SERVICES_DEBUG_DIRECTORY, basedir + uri + "/debug");

        // used for site configuration later
        Map<String, Object> siteMap = (Map<String, Object>) map.remove(SetupConstants.CONFIG_VAR_SITE_CONFIGURATION);

        Map<String, Object> userRepo = (Map<String, Object>) map.remove(SetupConstants.USER_STORE);

        try {
            // Check for click-through license acceptance before processing the request.
            SetupProgress.reportStart("configurator.progress.license.check", new Object[0]);
            if (!isLicenseAccepted(request)) {
                SetupProgress.reportEnd("configurator.progress.license.rejected",
                        new Object[]{SetupConstants.ACCEPT_LICENSE_PARAM});
                return false;
            }
            SetupProgress.reportEnd("configurator.progress.license.accepted", new Object[0]);

            /*
             * As we have got this far then the user must have accepted the license, so we log this implicitly.
             */
            LicenseSet licenses = getLicenseLocator().getRequiredLicenses();
            for (License license : licenses) {
                installLog.write(String.format("License, %s, has been accepted.%n", license.getFilename()));
                String licenseHash = Hash.hash(license.toString());
                installLog.write(String.format("License Hash: %s.%n", licenseHash));
            }

            isConfiguredFlag = configure(request, map, userRepo);
            if (isConfiguredFlag) {
                FqdnValidator.getInstance().initialize();
                //postInitialize was called at the end of configure???? 
                //postInitialize(getAdminSSOToken()); //don't think this is necessary
            }
            LoginLogoutMapping.setProductInitialized(isConfiguredFlag);
            registerListeners();
            
            if (isConfiguredFlag) {
                String fileBootstrap = getBootstrapLocator();
                if (fileBootstrap != null) {
                   writeToFileEx(fileBootstrap, basedir);
                }

                // this will write bootstrap file after configuration is
                // done; and also register the observer.
                ServerConfigXMLObserver.getInstance().update(true);
                // register our other observers
                SMSPropertiesObserver.getInstance().notifyChanges();
                DebugPropertiesObserver.getInstance().notifyChanges();
                Map<String, Set<String>> mapBootstrap = new HashMap<String, Set<String>>(2);
                Set<String> set = new HashSet<String>(2);
                set.add(fileBootstrap);
                mapBootstrap.put(BOOTSTRAP_FILE_LOC, set);

                if (fileBootstrap == null) {
                    set.add(getPresetConfigDir());
                } else {
                    set.add(fileBootstrap);
                }
                // this is to store the bootstrap location
                String serverInstanceName = SystemProperties.getServerInstanceName();

                SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
                ServerConfiguration.setServerInstance(adminToken, serverInstanceName, mapBootstrap);

                // store the ds admin port if we are running in embedded mode
                String dataStore = (String) map.get(SetupConstants.CONFIG_VAR_DATA_STORE);

                if (dataStore.equals(SetupConstants.SMS_EMBED_DATASTORE)) {
                    String dsAdminPort = (String) map.get(SetupConstants.CONFIG_VAR_DIRECTORY_ADMIN_SERVER_PORT);
                    Map<String, Set<String>> mapAdminPort = new HashMap<String, Set<String>>(2);
                    Set<String> set2 = new HashSet<String>(2);
                    set2.add(dsAdminPort);
                    mapAdminPort.put(Constants.DS_ADMIN_PORT, set2);
                    ServerConfiguration.setServerInstance(adminToken, serverInstanceName, mapAdminPort);
                }

                // setup site configuration information
                if (siteMap != null && !siteMap.isEmpty()) {
                    String site = (String) siteMap.get( SetupConstants.LB_SITE_NAME);
                    String primaryURL = (String) siteMap.get(SetupConstants.LB_PRIMARY_URL);

                    /*
                     * If primary url is null that means we are adding
                     * to an existing site. we don't need to create it
                     * first.
                     */
                    if (primaryURL != null && primaryURL.length() > 0) {
                        Set<String> sites = SiteConfiguration.getSites(adminToken);
                        if (!sites.contains(site)) {
                            SiteConfiguration.createSite(adminToken, site, primaryURL, Collections.EMPTY_SET);
                        }
                    }

                    if (!ServerConfiguration.belongToSite(adminToken, serverInstanceName, site)) {
                        ServerConfiguration.addToSite(adminToken, serverInstanceName, site);
                    }

                }
                if (EmbeddedOpenDS.isMultiServer(map)) {
                    // Setup Replication port in SMS for each server
                    updateReplPortInfo(map);
                }
                EntitlementConfiguration ec = getEntitlementConfiguration(SubjectUtils.createSuperAdminSubject(), "/");
                ec.reindexApplications();
            }
        } catch (Exception e) {
            installLog.write("AMSetupServlet.processRequest: error", e);
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.processRequest: error", e);
            Object[] params = {e.getMessage(), basedir};
            throw new ConfiguratorException("configuration.failed", params, configLocale);
        } finally {
            installLog.write("\n\nDumping all configuration parameters...\n");
            installLog.write("\nRequest Parameters:\n");
            dumpConfigurationProperties(installLog, request.getParameterMap());
            if (siteMap != null && !siteMap.isEmpty()) {
                installLog.write("\nSite configuration items:\n");
                dumpConfigurationProperties(installLog, siteMap);
            }
            if (userRepo != null && !userRepo.isEmpty()) {
                installLog.write("\nExternal user repo configuration items:\n");
                dumpConfigurationProperties(installLog, userRepo);
            }
            if (map != null && !map.isEmpty()) {
                installLog.write("\nMain configuration items:\n");
                dumpConfigurationProperties(installLog, map);
            }
            installLog.write("\nFinished dumping all configuration parameters\n");
            installLog.close();
            SetupProgress.closeOutputStream();
        }

        if (WebtopNaming.configMonitoring() >= 0) {
            ConfigMonitoring cm = new ConfigMonitoring();
            cm.configureMonitoring();
        } else {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("WebtopNaming.configMonitoring returned error.");
        }

        return isConfiguredFlag;
    }

    // The list of constants, passwords for example, that should be hashed out if logged.
    private static final String[] CONFIG_ITEMS_TO_HASH = new String[] {
            SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD,
            SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM,
            SetupConstants.CONFIG_VAR_ADMIN_PWD,
            SetupConstants.CONFIG_VAR_CONFIRM_ADMIN_PWD,
            SetupConstants.CONFIG_VAR_DS_MGR_PWD,
            SetupConstants.CONFIG_VAR_ENCRYPTION_KEY,
            SetupConstants.ENC_PWD_PROPERTY,
            SetupConstants.ENCRYPTED_AD_ADMIN_PWD,
            SetupConstants.ENCRYPTED_ADMIN_PWD,
            SetupConstants.ENCRYPTED_LDAP_USER_PWD,
            SetupConstants.ENCRYPTED_SM_DS_PWD,
            SetupConstants.HASH_ADMIN_PWD,
            SetupConstants.HASH_LDAP_USER_PWD,
            SetupConstants.SSHA512_LDAP_USERPWD,
            SetupConstants.UM_DS_DIRMGRPASSWD,
            SetupConstants.USERSTORE_PWD,
            SetupConstants.USER_STORE_LOGIN_PWD,
            SetupConstants.USER_STORE
    };

    // Used to provide a lookup list of items that should be hashed out.
    private static final List<String> CONFIG_ITEMS_TO_HASH_LIST =
            new ArrayList<String>(Arrays.asList(CONFIG_ITEMS_TO_HASH));

    /**
     * Iterate over the supplied properties (sorted by property name) and write them out to the passed install log.
     * Property values that are in the CONFIG_ITEMS_TO_HASH_LIST will have their value masked.
     * @param installLog The log to write the properties into
     * @param properties A non-null set of properties to iterate over and write out to the log
     */
    private static void dumpConfigurationProperties(InstallLog installLog, Map<String, Object> properties) {
        SortedMap<String, Object> sortedProperties = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        sortedProperties.putAll(properties);
        for (String key : sortedProperties.keySet()) {
            if (CONFIG_ITEMS_TO_HASH_LIST.contains(key)) {
                installLog.write(key + " = #########" + "\n");
            } else {
                installLog.write(key + " = " + sortedProperties.get(key) + "\n");
            }
        }
    }

    /**
     * Verify that the user has accepted the terms of all required licenses. This is indicated by the presence of a
     * request parameter {@code licenseAccepted=true}.
     *
     * @param request the servlet request.
     * @return true if the license acceptance parameter is present and correct, otherwise false.
     */
    private static boolean isLicenseAccepted(IHttpServletRequest request) {
        try {
            return Boolean.parseBoolean(request.getParameterMap().get(SetupConstants.ACCEPT_LICENSE_PARAM).toString());
        } catch (NullPointerException ex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("Invalid license acceptance parameter", ex);
            return false;
        }
    }
    
    private static void writeInputToFile(IHttpServletRequest request)
        throws IOException {
        StringBuilder buff = new StringBuilder();
        Map<String, Object> map = request.getParameterMap();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!entry.getKey().equals("actionLink")) {
                if (entry.getValue() instanceof String) {
                    buff.append(entry.getKey()).append("=");
                    if (passwordParams.contains(entry.getKey())) {
                        buff.append("********");
                    } else if (entry.getKey().equals(SetupConstants.CONFIG_VAR_SERVER_URI)) {
                        buff.append(request.getContextPath());
                    } else {
                        buff.append(entry.getValue());
                    }
                    buff.append("\n");
                } else if (entry.getValue() instanceof Map) {
                    Map<String, String> valMap = (Map<String, String>) entry.getValue();
                    for (Map.Entry<String, String> mapEntry : valMap.entrySet()) {
                        buff.append(entry.getKey()).append(".").append(mapEntry.getKey()).append("=");
                        if (passwordParams.contains(mapEntry.getKey())) {
                            buff.append("********");
                        } else {
                            buff.append(mapEntry.getValue());
                        }
                        buff.append("\n");
                    }
                }
            }
        }
        
        String basedir = (String) map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
        writeToFile(basedir + SetupConstants.CONFIG_PARAM_FILE, buff.toString());
    }
    
    private static void checkBaseDir(String basedir, IHttpServletRequest req) throws IOException {
        Object[] params = {basedir};
        SetupProgress.reportStart("emb.checkingbasedir", params);

        File baseDirectory = new File(basedir);
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs();
            writeInputToFile(req);
        } else {
            File bootstrapFile = new File(basedir + "/" + BOOTSTRAP_EXTRA);
            File opendsDir = new File(basedir + OPENDS_DIR);
            if (bootstrapFile.exists() || opendsDir.exists()) {
                SetupProgress.reportEnd("emb.basedirfailed", null);
                throw new ConfiguratorException("Base directory specified :" + basedir
                        + " cannot be used - has preexisting config data.");
            }
        }
        SetupProgress.reportEnd("emb.success", null);
    }

    // (i) install, configure and start an embedded instance.
    // or
    // (ii) install, configure, and replicate embedded instance
    private static boolean setupEmbeddedDS(Map<String, Object> map, String dataStore) throws Exception {
        boolean ditLoaded = false;
        EmbeddedOpenDS.setup(map, servletCtx);
        AMSetupDSConfig dsConfig = AMSetupDSConfig.getInstance();

        // wait for at most 10 seconds for OpenDS to come up
        int sleepTime = 10;
        while (!dsConfig.isDServerUp() && (sleepTime-- > 0)) {
            // sleep one second a time
            Thread.sleep(1000);
        }
        if (!dsConfig.isDServerUp()) {
            throw new ConfigurationException("OpenDJ cannot be started.");
        }

        // Determine if DITLoaded flag needs to be set: multi instance
        if (EmbeddedOpenDS.isMultiServer(map)) {
            // Replication 
            // TOFIX: Temporary fix until OpenDS auto-loads schema
            List<String> schemaFiles = getSchemaFiles(dataStore);
            String basedir = (String) map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
            writeSchemaFiles(basedir, schemaFiles, map, dataStore);
            // Get the remote host name from the SERVER URL  
            // entered in the 'Add to existing instance' and place
            // it in the map. This is for console configurator.
            // For cli configurator, the "DS_EMB_REPL_HOST2" would
            // be entered in the config. file itself.
            String existingInstance = (String) map.get(SetupConstants.DS_EMB_EXISTING_SERVERID);
            if (existingInstance != null) {
                int ndx1 = existingInstance.indexOf("://");
                if (ndx1 != -1 && ndx1 != (existingInstance.length() - 1)) {
                    String str1 = existingInstance.substring(ndx1+3);
                    int ndx2 = str1.indexOf(":");
                    if (ndx2 != -1 && ndx2 != (str1.length() -1)) {
                        String finalStr = str1.substring(0, ndx2);
                        map.put(SetupConstants.DS_EMB_REPL_HOST2, finalStr);
                    }
                }
            }
            EmbeddedOpenDS.setupReplication(map);
            ditLoaded = true;
        }
        return ditLoaded;
    }
    
    private static boolean setupSMDatastore(Map<String, Object> map) throws Exception {
        boolean isDITLoaded = map.get(SetupConstants.DIT_LOADED).equals("true");
        String dataStore = (String) map.get(SetupConstants.CONFIG_VAR_DATA_STORE);

        if (dataStore.equals(SetupConstants.SMS_EMBED_DATASTORE)) {
            isDITLoaded = setupEmbeddedDS(map, dataStore);
        }

        if (!isDITLoaded) {
            List<String> schemaFiles = getSchemaFiles(dataStore);
            String basedir = (String) map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
            writeSchemaFiles(basedir, schemaFiles, map, dataStore);
        }
        return isDITLoaded;
    }
    
    private static void configureServerInstance(SSOToken adminSSOToken, String serverInstanceName,
            String strAMConfigProperties, boolean isDITLoaded, String basedir, String strServerConfigXML,
            Map propAMConfig, Map<String, Object> map) throws SMSException, SSOException, IOException,
            ConfigurationException {
        SetupProgress.reportStart("configurator.progress.configure.server.instance", null);
        try {
            if (!isDITLoaded) {
                ServerConfiguration.createDefaults(adminSSOToken);
                final String cookieDomain = (String) map.get(SetupConstants.CONFIG_VAR_COOKIE_DOMAIN);
                if (isNotEmpty(cookieDomain)) {
                    ServiceSchemaManager scm = new ServiceSchemaManager(Constants.SVC_NAME_PLATFORM, adminSSOToken);
                    ServiceSchema globalSchema = scm.getGlobalSchema();
                    globalSchema.setAttributeDefaults(Constants.ATTR_COOKIE_DOMAINS, asSet(cookieDomain));
                }
            }
            if (!isDITLoaded || !ServerConfiguration.isServerInstanceExist(adminSSOToken, serverInstanceName)) {
                ServerConfiguration.createServerInstance(adminSSOToken, serverInstanceName,
                        ServerConfiguration.getPropertiesSet(strAMConfigProperties), strServerConfigXML);
            }
        } catch (UnknownPropertyNameException ex) {
            // ignore, property names are valid because they are
            // gotten from template.
        }
        SetupProgress.reportEnd("emb.done", null);
    }
    
    private static boolean configure(IHttpServletRequest request, Map<String, Object> map, Map<String, Object> userRepo)
            throws Exception {
        boolean configured;
        boolean existingConfiguration = false;
        try {
            String basedir = (String) map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
            checkBaseDir(basedir, request);
            boolean isDITLoaded = setupSMDatastore(map);

            String serverURL = (String) map.get(SetupConstants.CONFIG_VAR_SERVER_URL);
            String deployuri = (String) map.get(SetupConstants.CONFIG_VAR_SERVER_URI);
            // do this here since initializeConfigProperties needs the dir
            setupSecurIDDirs(basedir, deployuri);

            SetupProgress.reportStart("configurator.progress.reinit.system", null);
            Map mapFileNameToConfig = initializeConfigProperties();
            String strAMConfigProperties = (String) mapFileNameToConfig.get(SetupConstants.AMCONFIG_PROPERTIES);
            String strServerConfigXML = (String) mapFileNameToConfig.get(SystemProperties.CONFIG_FILE_NAME);
            Properties propAMConfig = ServerConfiguration.getProperties(strAMConfigProperties);
            // Set the install property since reInitConfigProperties
            // initializes SMS which inturn initializes EventService
            propAMConfig.put(Constants.SYS_PROPERTY_INSTALL_TIME, "true");
            String serverInstanceName = serverURL + deployuri;
            reInitConfigProperties(serverInstanceName, propAMConfig, strServerConfigXML);
            // SystemProperties gets reinitialized and installTime property
            // has to set again
            SystemProperties.initializeProperties(Constants.SYS_PROPERTY_INSTALL_TIME, "true");
            SetupProgress.reportEnd("emb.done", null);
            
            SSOToken adminSSOToken = getAdminSSOToken();

            if (!isDITLoaded) {
                RegisterServices regService = new RegisterServices();
                boolean bUseExtUMDS = userRepo != null && !userRepo.isEmpty();
                regService.registers(adminSSOToken, bUseExtUMDS);
                processDataRequests("/WEB-INF/template/sms");
            }

            // Set installTime to false, to avoid in-memory notification from
            // SMS in cases where not needed, and to denote that service  
            // registration got completed during configuration phase and it 
            // has passed installtime.
            SystemProperties.initializeProperties(Constants.SYS_PROPERTY_INSTALL_TIME, "false");
            configureServerInstance(adminSSOToken, serverInstanceName, strAMConfigProperties, isDITLoaded, basedir,
                    strServerConfigXML, propAMConfig, map);

            // Embedded :get our serverid and configure embedded idRepo
            String dataStore = (String) map.get(SetupConstants.CONFIG_VAR_DATA_STORE);
            boolean embedded = dataStore.equals(SetupConstants.SMS_EMBED_DATASTORE);
            // Ensure this service are initialized before continuing
            WebtopNaming.initialize();
            NamingService.initialize();

            if (embedded) {
                try {
                    String serverID = WebtopNaming.getAMServerID();
                    String entry = map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST) + ":"
                            + map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT) + "|"
                            + (serverID == null ? "" : serverID);
                    String orgName = (String) map.get(SetupConstants.SM_CONFIG_ROOT_SUFFIX);
                    updateEmbeddedIdRepo(orgName, "embedded", entry);
                } catch (Exception ex) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME).error("EmbeddedDS : failed to setup serverid", ex);
                    throw ex;
                }
            }

            SystemProperties.setServerInstanceName(serverInstanceName);
            LDIFTemplates.copy(basedir, servletCtx);
            ServiceXMLTemplates.copy(basedir + "/template/xml", servletCtx);
            createDotVersionFile(basedir);
            handlePostPlugins(adminSSOToken);
           
            if (!isDITLoaded && userRepo != null && !userRepo.isEmpty()) {
                // Construct the SMSEntry for the node to check to 
                // see if this is an existing configuration store, 
                // or new store.
                ServiceConfig sc = UserIdRepo.getOrgConfig(adminSSOToken);
                if (sc != null) {
                    CachedSMSEntry cEntry = CachedSMSEntry.getInstance(adminSSOToken,
                            "ou=" + userRepo.get("userStoreHostName") + "," + sc.getDN());
                    SMSEntry entry = cEntry.getClonedSMSEntry();
                    if (entry.isNewEntry()) {
                        UserIdRepo.getInstance().configure(userRepo, basedir, servletCtx, adminSSOToken);
                    } else {
                        existingConfiguration = true;
                    }
                }
            }
            // postInitialize requires the user repo to be configured
            postInitialize(adminSSOToken);

            // create .storepass and .keypass files to unlock the keystore
            // The template keystore file stores the password protected with "changeit"
            String storePass = AMSetupUtils.getRandomString();
            createPasswordFiles(basedir + deployuri, storePass, "changeit");
            if (!isDITLoaded) {
                if ((userRepo == null) || userRepo.isEmpty()) {
                    createDemoUser();
                }
                if (!existingConfiguration) {
                    createIdentitiesForWSSecurity(serverURL, deployuri);
                }
            }

            String aceDataDir = basedir + "/" + deployuri + "/auth/ace/data";
            copyAuthSecurIDFiles(aceDataDir);

            createMonitoringAuthFile(basedir, deployuri);

            isConfiguredFlag = true;
            configured = true;
        } catch (Exception e) { 
            // catch all because we want all exception to be logged
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.configure: error", e);
            errorMessage = e.getMessage();
            throw e;
        }
        return configured;
    }

    public static String getErrorMessage() {
        return errorMessage != null ? errorMessage : "";
    }

    private static void appendLegacyProperties(Map prop) {
        ResourceBundle res = ResourceBundle.getBundle(LEGACY_PROPERTIES);
        for (Enumeration i = res.getKeys(); i.hasMoreElements();) {
            String key = (String)i.nextElement();
            prop.put(key, res.getString(key));
        }
    }

    private static void postInitialize(SSOToken adminSSOToken) throws SSOException, SMSException {
        SMSEntry.initializeClass();
        AMAuthenticationManager.reInitializeAuthServices();
        
        AMIdentityRepository.clearCache();
        ServiceManager svcMgr = new ServiceManager(adminSSOToken);
        svcMgr.clearCache();
        LoginLogoutMapping lmp = new LoginLogoutMapping();
        lmp.initializeAuth(servletCtx);
        LoginLogoutMapping.setProductInitialized(true);
    }
    
    private static void handlePostPlugins(SSOToken adminSSOToken) throws IllegalAccessException, InstantiationException,
            ClassNotFoundException {
        if (servletCtx == null) {
            return;
        }
        List<ConfiguratorPlugin> plugins = getConfigPluginClasses();
        for (ConfiguratorPlugin plugin : plugins) {
            plugin.doPostConfiguration(servletCtx, adminSSOToken);
        }
    }

    private static List getConfigPluginClasses() throws IllegalAccessException, InstantiationException,
            ClassNotFoundException {
        List<Object> plugins = new ArrayList<Object>();
        try {
            ResourceBundle rb = ResourceBundle.getBundle(SetupConstants.PROPERTY_CONFIGURATOR_PLUGINS);
            String strPlugins = rb.getString(SetupConstants.KEY_CONFIGURATOR_PLUGINS);

            if (strPlugins != null) {
                StringTokenizer st = new StringTokenizer(strPlugins);
                while (st.hasMoreTokens()) {
                    String className = st.nextToken();
                    Class<ConfiguratorPlugin> clazz = (Class<ConfiguratorPlugin>) Class.forName(className);
                    plugins.add(clazz.asSubclass(ConfiguratorPlugin.class).newInstance());
                }
            }
        } catch (IllegalAccessException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.getConfigPluginClasses: error", e);
            throw e;
        } catch (InstantiationException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.getConfigPluginClasses: error", e);
            throw e;
        } catch (ClassNotFoundException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.getConfigPluginClasses: error", e);
            throw e;
        } catch (MissingResourceException e) {
            //ignore if there are no configurator plugins.
        }
        return plugins;
    }

    private static void reInitConfigProperties(String serverName, Properties prop, String strServerConfigXML)
            throws SMSException, IOException, SSOException, LDAPServiceException,IllegalAccessException,
            InstantiationException, ClassNotFoundException {
        SystemProperties.initializeProperties(prop, true, false);
        Crypt.reinitialize();
        initDSConfigMgr(strServerConfigXML);
        AdminUtils.initialize();
        SMSAuthModule.initialize();
        SystemProperties.initializeProperties(prop, true, true);

        List<ConfiguratorPlugin> plugins = getConfigPluginClasses();
        Map map = ServicesDefaultValues.getDefaultValues();
        String basedir = (String) map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
        for (ConfiguratorPlugin plugin : plugins) {
            plugin.reinitConfiguratioFile(basedir);
        }
    }

    public static String getPresetConfigDir() throws ConfiguratorException {
        String configDir = System.getProperty(SetupConstants.JVM_PROP_PRESET_CONFIG_DIR);

        if (configDir == null || configDir.length() == 0) {
            try {
                ResourceBundle rb = ResourceBundle.getBundle(SetupConstants.BOOTSTRAP_PROPERTIES_FILE);
                configDir = rb.getString(SetupConstants.PRESET_CONFIG_DIR);
            } catch (MissingResourceException e) {
                //ignored because bootstrap properties file maybe absent.
            }
        }

        if (configDir != null && configDir.length() > 0) {
            String realPath = getNormalizedRealPath(servletCtx);
            if (realPath != null) {
                configDir = com.sun.identity.shared.StringUtils.
                        strReplaceAll(configDir, SetupConstants.TAG_REALPATH, realPath);
            } else {
                throw new ConfiguratorException("cannot get configuration path");
            }
        }

        return configDir;
    }

    /**
     * Returns location of the bootstrap file.
     *
     *  return the legacy bootstrap file OR boot.properties. If both files exist, the legacy bootstrap is returned
     *
     * @return Location of the bootstrap file. Returns null if a bootstrap file
     *         cannot be located 
     * @throws ConfiguratorException if servlet context is null or deployment
     *         application real path cannot be determined.
     */
    static String getBootStrapFile() throws ConfiguratorException {
        String bootstrap = null;
        String bootJson = null;

        String configDir = getPresetConfigDir();
        if (configDir != null && configDir.length() > 0) {
            bootstrap = configDir + "/bootstrap";
            bootJson = configDir + "/boot.json";
        } else {
            String locator = getBootstrapLocator();
            FileReader frdr = null;

            try {
                frdr = new FileReader(locator);
                BufferedReader brdr = new BufferedReader(frdr);
                String basePath = brdr.readLine();
                bootstrap = basePath + "/bootstrap";
                bootJson = basePath + "/boot.json";
            } catch (IOException e) {
                //ignore
            } finally {
                if (frdr != null) {
                    try {
                        frdr.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
        }

        if (bootstrap != null) {
            File test = new File(bootstrap);
            if (!test.canRead()) {
                bootstrap = null;
            }
        }
        if (bootJson != null) {
            File test = new File(bootJson);
            if (!test.exists()) {
                bootJson = null;
            }
        }

        return bootstrap != null ? bootstrap : bootJson;
    }

    /**
     * Determine if we can boot from one of
     * <ul>
     *     <li>legacy bootstrap file</li>
     *     <li>boot.json</li>
     * </ul>
     *
     * @return true if the system can boot from files or environment variables
     * @throws ConfiguratorException if we encounter an I/O error or security exception while checking for boot properties
     */
    static boolean canBootstrap() throws ConfiguratorException {
        String bsFile = getBootStrapFile();
        // We may do more checks here in the future.
        return bsFile != null;
    }

    // this is the file which contains the base dir.
    // this file is not created if configuration directory is 
    // preset in bootstrap.properties
    private static String getBootstrapLocator() throws ConfiguratorException {
        String configDir = getPresetConfigDir();
        if (configDir != null && configDir.length() > 0) {
            return null;
        }
        
        if (servletCtx != null) {
            String path = getNormalizedRealPath(servletCtx);
            if (path != null) {
                String home = System.getProperty("user.home");
                File newPath = new File(home + "/" + SetupConstants.CONFIG_VAR_BOOTSTRAP_BASE_DIR);
                File oldPath = new File(home + "/" + SetupConstants.CONFIG_VAR_BOOTSTRAP_LEGACY_BASE_DIR);

                String fullOldPath = oldPath.getPath() + "/" + SetupConstants.CONFIG_VAR_BOOTSTRAP_BASE_PREFIX + path;
                String fullNewPath = newPath.getPath() + "/" + SetupConstants.CONFIG_VAR_BOOTSTRAP_BASE_PREFIX + path;

                Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
                String bootstrapLocatorResult;
                // Simple case where just the old path exists.
                if (oldPath.exists() && !newPath.exists()) {
                    bootstrapLocatorResult = fullOldPath;
                    if (debug.messageEnabled()) {
                        debug.message("AMSetupServlet.getBootstrapLocator: only old path exists, returning old "
                                + bootstrapLocatorResult);
                    }
                // There is a chance that both new and old path locations exist when newer installations have been done
                // from scratch but the instance to consider is in the old path, double check for an old config before
                // returning the new path when finding both.
                } else if (oldPath.exists() && newPath.exists()) {
                    // Test if we have a config file in the old path
                    File testOldPath = new File(fullOldPath);
                    if (testOldPath.exists()) {
                        bootstrapLocatorResult = fullOldPath;
                        if (debug.messageEnabled()) {
                            debug.message("AMSetupServlet.getBootstrapLocator: both old and new paths exist, found a "
                                    + "config in the old path, returning old " + bootstrapLocatorResult);
                        }
                    } else {
                        bootstrapLocatorResult = fullNewPath;
                        if (debug.messageEnabled()) {
                            debug.message("AMSetupServlet.getBootstrapLocator: both old and new paths exist but did "
                                    + "not find a config in old path, returning new " + bootstrapLocatorResult);
                        }
                    }
                } else {
                    bootstrapLocatorResult = fullNewPath;
                    if (debug.messageEnabled()) {
                        debug.message("AMSetupServlet.getBootstrapLocator: only new path exists, returning new "
                                + bootstrapLocatorResult);
                    }
                }

                return bootstrapLocatorResult;

            } else {
                throw new ConfiguratorException(
                        "Cannot read the bootstrap path");
            }
        } else {
            return null;
        }
    }

    public static String getBaseDir() throws ConfiguratorException {
        String configDir = getPresetConfigDir();
        if (configDir != null && configDir.length() > 0) {
            return configDir;
        }
        if (servletCtx != null) {
            String path = getNormalizedRealPath(servletCtx);
            if (path != null) {
                String bootstrap = getBootstrapLocator();
                File test = new File(bootstrap);
                if (!test.exists()) {
                    return null;
                }
                FileReader frdr = null;
                try {
                    frdr = new FileReader(bootstrap);
                    BufferedReader brdr = new BufferedReader(frdr);
                    return brdr.readLine();
                } catch (IOException e) {
                    throw new ConfiguratorException(e.getMessage());
                } finally {
                    if (frdr != null) {
                        try {
                            frdr.close();
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                }
            } else {
                throw new ConfiguratorException("Cannot read the bootstrap path");
            }
        } else {
            throw new ConfiguratorException("Servlet Context is null");
        }
    }

    private static String getNormalizedRealPath(ServletContext servletCtx) {
        String path = null;
        if (servletCtx != null) {
            path = getAppResource(servletCtx);
            
            if (path != null) {
                String realPath = servletCtx.getRealPath("/");
                if (realPath != null && realPath.length() > 0) {
                    realPath = realPath.replace('\\', '/');
                    path = realPath.replaceAll("/", "_");
                } else {
                    path = path.replaceAll("/", "_");
                }
                int idx = path.indexOf(":");
                if (idx != -1) {
                    path = path.substring(idx + 1);
                }
            }
        }
        return path;
    }

    /**
     * Returns URL of the default resource.
     *
     * @return URL of the default resource. Returns null of servlet context is
     *         null.
     */
    private static String getAppResource(ServletContext servletCtx) {
        if (servletCtx != null) {
            try {
                java.net.URL turl = servletCtx.getResource("/");
                return turl.getPath();
            } catch (MalformedURLException mue) {
                Debug.getInstance(SetupConstants.DEBUG_NAME)
                        .error("AMSetupServlet.getAppResource: Cannot access the resource", mue);
            }
        } else {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.getAppResource: Context is null");
        }
        return null;
    }

    /**
     * This method takes the name of XML file, process each 
     * request object one by one immediately after parsing.
     *
     * @param xmlBaseDir is the location of request xml files
     * @throws SMSException if error occurs in the service management space.
     * @throws SSOException if administrator single sign on is not valid.
     * @throws IOException if error accessing the configuration files.
     * @throws PolicyException if policy cannot be loaded.
     */
    private static void processDataRequests(String xmlBaseDir) throws SMSException, SSOException, IOException,
            PolicyException {
        SetupProgress.reportStart("configurator.progress.configure.system", null);
        SSOToken ssoToken = getAdminSSOToken();
        try {
            Map map = ServicesDefaultValues.getDefaultValues();
            String hostname = (String) map.get(SetupConstants.CONFIG_VAR_SERVER_HOST);
            ConfigureData configData = new ConfigureData(xmlBaseDir, servletCtx, hostname, ssoToken);
            configData.configure();
            SetupProgress.reportEnd("emb.done", null);
        } catch (SMSException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.processDataRequests", e);
            throw e;
        } catch (SSOException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.processDataRequests", e);
            throw e;
        } catch (IOException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.processDataRequests", e);
            throw e;
        }
    }

    /**
     * Helper method to return Admin token
     * @return Admin Token
     */
    private static SSOToken getAdminSSOToken() {
        if (adminToken == null) {
            adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        }
        return adminToken;
    }

    /**
     * Initialize AMConfig.properties with host specific values
     */
    private static Map initializeConfigProperties() throws SecurityException, IOException {
        Map<String, String> mapFileNameToContent = new HashMap<String, String>();
        List<String> dataFiles = getTagSwapConfigFiles();
        
        Map<String, Object> map = ServicesDefaultValues.getDefaultValues();
        String basedir = (String) map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
       
        String deployuri = (String) map.get(SetupConstants.CONFIG_VAR_SERVER_URI);
        try {
            File fhm = new File(basedir + deployuri + "/" + SMS_STR);
            fhm.mkdirs();
        } catch (SecurityException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.initializeConfigProperties", e);
            throw e;
        }

        // this is for the servicetag-registry.xml stuff, a bit later

        for (String file : dataFiles) {
            StringBuilder sbuf;
    
            /*
             * if the file's not there, just skip it
             * usually will be about a file included with OpenAM,
             * so it's informational, rather than a "real" error.
             */
            try {
                sbuf = new StringBuilder(readFile(file));
            } catch (IOException ioex) {
                break;
            }
            
            int idx = file.lastIndexOf("/");
            String absFile = idx != -1 ? file.substring(idx + 1) : file;
            
            if (absFile.equalsIgnoreCase(SetupConstants.AMCONFIG_PROPERTIES)) {
                String dbOption = (String) map.get(SetupConstants.CONFIG_VAR_DATA_STORE);
                boolean embedded = dbOption.equals(SetupConstants.SMS_EMBED_DATASTORE);
                boolean dbSunDS;
                boolean dbMsAD  = false;
                if (embedded) {
                    dbSunDS = true;
                } else { // Keep old behavior for now.
                    dbSunDS = dbOption.equals(SetupConstants.SMS_DS_DATASTORE);
                    dbMsAD  = dbOption.equals(SetupConstants.SMS_AD_DATASTORE);
                }

                if (dbSunDS || dbMsAD) {
                    int idx1 = sbuf.indexOf(SetupConstants.CONFIG_VAR_SMS_DATASTORE_CLASS);
                    if (idx1 != -1) {
                        String dataStoreClass = embedded ?
                                SetupConstants.CONFIG_VAR_EMBEDDED_DATASTORE_CLASS :
                                SetupConstants.CONFIG_VAR_DS_DATASTORE_CLASS;
                        sbuf.replace(idx1, idx1 + SetupConstants.CONFIG_VAR_SMS_DATASTORE_CLASS.length(),
                                dataStoreClass);
                    }
                }
            }

            String swapped = ServicesDefaultValues.tagSwap(sbuf.toString(), file.endsWith("xml"));
            
            if (absFile.equalsIgnoreCase(SetupConstants.AMCONFIG_PROPERTIES) ||
                    absFile.equalsIgnoreCase(SystemProperties.CONFIG_FILE_NAME)) {
                mapFileNameToContent.put(absFile, swapped);
            } else if (absFile.equalsIgnoreCase(SetupConstants.SECURID_PROPERTIES)) {
                writeToFile(basedir + deployuri + "/auth/ace/data/" + absFile, swapped);
            } else {
                writeToFile(basedir + "/" + absFile, swapped);
            }
        }
        return mapFileNameToContent;
    }

    public static String readFile(String file) throws IOException {
        return AMSetupUtils.readFile(servletCtx, file);
    }

    private static void writeToFileEx(String fileName, String content) throws IOException {
        File btsFile = new File(fileName);
        if (!btsFile.getParentFile().exists()) {
            btsFile.getParentFile().mkdirs();
        }
        writeToFile(fileName, content);
    }

    /**
     * Returns schema file names.
     *
     * @param dataStore Name of data store configuration data.
     * @throws MissingResourceException if the bundle cannot be found.
     */
    private static List<String> getSchemaFiles(String dataStore) throws MissingResourceException {
        List<String> fileNames = new ArrayList<String>();
        ResourceBundle rb = ResourceBundle.getBundle(SetupConstants.SCHEMA_PROPERTY_FILENAME);
        String strFiles;

        boolean embedded = dataStore.equals(SetupConstants.SMS_EMBED_DATASTORE);
        if (embedded) {
            strFiles = rb.getString(SetupConstants.OPENDS_SMS_PROPERTY_FILENAME);
        } else {
            strFiles = rb.getString(SetupConstants.DS_SMS_PROPERTY_FILENAME);
        }
        
        StringTokenizer st = new StringTokenizer(strFiles);
        while (st.hasMoreTokens()) {
            fileNames.add(st.nextToken());
        }
        return fileNames;
    }

    private static List<String> getTagSwapConfigFiles() throws MissingResourceException {
        List<String> fileNames = new ArrayList<String>();
        ResourceBundle rb = ResourceBundle.getBundle("configuratorTagSwap");
        String strFiles = rb.getString("tagswap.files");
        StringTokenizer st = new StringTokenizer(strFiles);
        while (st.hasMoreTokens()) {
            fileNames.add(st.nextToken());
        }
        return fileNames;
    }

    /**
     * Tag swaps strings in schema files.
     *
     * @param basedir the configuration base directory.
     * @param schemaFiles List of schema files to be loaded.
     * @throws IOException if data files cannot be written.
     */
    private static void writeSchemaFiles(String basedir, List<String> schemaFiles, Map map, String dataStore)
            throws Exception {
        SetupProgress.reportStart("configurator.progress.tagswap.schemafiles", null);
        Set<String> absSchemaFiles = new HashSet<String>();
        for (String file : schemaFiles) {
            String content = readFile(file);
            FileWriter fout = null;
            
            try {
                int idx = file.lastIndexOf("/");
                String absFile = basedir + "/" + (idx != -1 ? file.substring(idx + 1) : file);
                fout = new FileWriter(absFile);
                absSchemaFiles.add(absFile);
                fout.write(ServicesDefaultValues.tagSwap(content));
            } catch (IOException ioex) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.writeSchemaFiles: "
                        + "Exception in writing schema files:" , ioex);
                throw ioex;
            } finally {
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }
            }
        }
        SetupProgress.reportEnd("emb.success", null);
        
        AMSetupDSConfig dsConfig = AMSetupDSConfig.getInstance();
        dsConfig.loadSchemaFiles(schemaFiles);

        if (dataStore.equals(SetupConstants.SMS_EMBED_DATASTORE)) {
            int ret = EmbeddedOpenDS.rebuildIndex(map);

            if (ret != 0) {
                Object[] error = { Integer.toString(ret) };
                SetupProgress.reportStart("emb.rebuildindex.failed", null);
                SetupProgress.reportEnd("emb.rebuildindex.failedmsg", error);
                Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.writeSchemaFiles: "
                        + "Unable to rebuild indexes in OpenDJ: " + ret);
                throw new Exception("Unable to rebuild indexes in OpenDJ: " + ret);
            }
        }

        for (String schemaFile : absSchemaFiles) {
            File file = new File(schemaFile);
            file.delete();
        }
    }
    
    /**
     * Create the storepass and keypass files
     *
     * @param basedir the configuration base directory.
     * @param keystorePwd password for .storepass
     * @param keyPassword password for the .keypass - usually the same as above
     * @throws IOException if password files cannot be written.
     */
    static void createPasswordFiles(String basedir, String keystorePwd, String keyPassword) throws IOException {
        // We no longer encrypt the password in the keypass /storepass, because
        // the boot passwords are stored in the keystore, and we need
        // to be able to open the keystore to boot.
        // The keystore now moves to the basedir
        writeContent(basedir + "/.keypass", keyPassword);
        writeContent(basedir + "/.storepass", keystorePwd);
        copyCtxFile("/WEB-INF/template/keystore", "keystore.jks", basedir);
        copyCtxFile("/WEB-INF/template/keystore", "keystore.jceks", basedir);
        // the sample keystore files are encrypted with the default "changeit". We need to open and then save them
        // the new password
        AMKeyProvider jceks = new AMKeyProvider(true, basedir + "/keystore.jceks", "changeit", "JCEKS", "changeit");
        AMKeyProvider jks = new AMKeyProvider(true, basedir + "/keystore.jks", "changeit", "JKS", "changeit");

        jceks.setKey(keystorePwd, keyPassword);
        jks.setKey(keystorePwd, keyPassword);

        try {
            jceks.store();
            jks.store();
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new IOException("Can't update keystore password", e);
        }
    }

    /**
     * Helper method to create the storepass and keypass files
     *
     * @param fName   is the name of the file to create.
     * @param content is the password to write in the file.
     * @throws IOException
     */
    private static void writeContent(String fName, String content) throws IOException {
        FileWriter fout = null;
        try {
            Files.write(Paths.get(fName), content.getBytes(UTF_8));
            chmodFileReadOnly(new File(fName));

        } catch (IOException ioex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME)
                    .error("AMSetupServlet.writeContent: Exception in creating password files:", ioex);
            throw ioex;
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception ex) {
                    //No handling requried
                }
            }
        }
    }

    /**
     * Change a file to be read only for owner (eg password file on disk that we want to protect)
     *
     * @param f - the File handle to the file
     * @throws IOException if the file does not exist or permissions can not be changed
     */
    public static void chmodFileReadOnly(File f) throws IOException {
        // Tyy to chmod the file to be owner readable only
        if ( !(f.setReadOnly() && f.setReadable(false, false) && f.setReadable(true, true))) {
            // Unable to set permissions...
            // Debug log may not be established right now so we cant really log a message...
            // So we have to ignore this
            // Debug.WARNING("Could not chmod file to to be readonly. Will also try POSIX");
        }
    }

    /**
      * Update Embedded Idrepo instance with new embedded opends isntance.
      */
    private static void  updateEmbeddedIdRepo(String orgName, String configName, String entry) throws SMSException,
            SSOException {
        SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        ServiceConfigManager scm = new ServiceConfigManager(token, IdConstants.REPO_SERVICE, "1.0");
        ServiceConfig sc = scm.getOrganizationConfig(orgName, null);
        if (sc != null) {
            ServiceConfig subConfig = sc.getSubConfig(configName);
            if (subConfig != null) {
                Map<String, Set<String>> configMap = subConfig.getAttributes();
                Set<String> vals = configMap.get("sun-idrepo-ldapv3-config-ldap-server");
                vals.add(entry);
                HashMap<String, Set<String>> mp = new HashMap<String, Set<String>>(2);
                mp.put("sun-idrepo-ldapv3-config-ldap-server", vals);
                subConfig.setAttributes(mp);
            }
        }
    }

    /**
     * Update platform server list and Organization alias
     */
    private static void updatePlatformServerList(String serverURL, String hostName) throws SMSException, SSOException {
        SSOToken token = getAdminSSOToken();
        ServiceSchemaManager ssm = new ServiceSchemaManager("iPlanetAMPlatformService", token);
        ServiceSchema ss = ssm.getGlobalSchema();
        AttributeSchema as = ss.getAttributeSchema("iplanet-am-platform-server-list");
        Set<String> values = as.getDefaultValues();
        if (!isInPlatformList(values, serverURL)) {
            String instanceName = getNextAvailableServerId(values);
            values.add(serverURL + "|" + instanceName);
            as.setDefaultValues(values);

            // Update Organization Aliases
            OrganizationConfigManager ocm = new OrganizationConfigManager(token, "/");
        
            Map<String, Object> attrs = ocm.getAttributes("sunIdentityRepositoryService");
            Set<String> origValues = (Set<String>) attrs.get("sunOrganizationAliases");
            if (!origValues.contains(hostName)) {
                values = new HashSet<String>();
                values.add(hostName);
                ocm.addAttributeValues("sunIdentityRepositoryService", "sunOrganizationAliases", values);
            }
        }
    }

    private static String getNextAvailableServerId(Set<String> values) {
        int maxNumber = 1;

        for (String item : values) {
            int index1 = item.indexOf('|');

            if (index1 != -1) {
                int index2 = item.indexOf('|', index1 + 1);
                item = index2 == -1 ? item.substring(index1 + 1) : item.substring(index1 + 1, index2);

                try {
                    int n = Integer.parseInt(item);
                    if (n > maxNumber) {
                        maxNumber = n;
                    }
                } catch (NumberFormatException nfe) {
                    // Ignore and continue
                }
            }
        }
        String instanceName = Integer.toString(maxNumber + 1);
        
        if (instanceName.length() == 1) {
            instanceName = "0" + instanceName;
        }
        
        return instanceName;
    }
    
    private static boolean isInPlatformList(Set values, String hostname) {
        boolean found = false;
        for (Iterator items = values.iterator(); items.hasNext() && !found;) {
            String item = (String) items.next();
            int idx = item.indexOf('|');
            if (idx != -1) {
                String svr = item.substring(0, idx);
                found = svr.equals(hostname);
            }
        }
        return found;
    }
    
    private static boolean isAgentServiceLoad(SSOToken token) {
        try {
            new ServiceSchemaManager("AgentService", token);
            return true;
        } catch (SSOException ex) {
            return false;
        } catch (SMSException ex) {
            return false;
        }
    }

    private static void createDemoUser() 
        throws IdRepoException, SSOException {
        SetupProgress.reportStart("configurator.progress.create.demo.user", null);
        Map<String, Set<String>> attributes = new HashMap<String, Set<String>>();
        attributes.put("sn", CollectionUtils.asSet("demo"));
        attributes.put("cn", CollectionUtils.asSet("demo"));
        attributes.put("userpassword", CollectionUtils.asSet("changeit"));
        attributes.put("inetuserstatus", CollectionUtils.asSet("Active"));
        try {
            AMIdentityRepository amir = new AMIdentityRepository(getAdminSSOToken(), "/");
            amir.createIdentity(IdType.USER, "demo", attributes);
        } catch (IdRepoException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.createDemoUser", e);
            throw e;
        } catch (SSOException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.createDemoUser", e);
            throw e;
        }
        SetupProgress.reportEnd("emb.done", null);
    }

    /**
     * Creates Identities for WS Security
     *
     * @param serverURL URL at which OpenAM is configured.
     */
    private static void createIdentitiesForWSSecurity(String serverURL, String deployuri) throws IdRepoException,
            SSOException {
        SSOToken token = getAdminSSOToken();
        
        if (!isAgentServiceLoad(token)) {
            return;
        }
        
        SetupProgress.reportStart("configurator.progress.create.wss.agents", null);
        AMIdentityRepository idrepo = new AMIdentityRepository(token, "/");
        Map<String, String> config = new HashMap<String, String>();

        // Add WSC configuration
        config.put("sunIdentityServerDeviceStatus", "Active");
        config.put("SecurityMech", "urn:sun:wss:security:null:UserNameToken");
        config.put("UserCredential", "UserName:test|UserPassword:test");
        config.put("useDefaultStore", "true");
        config.put("privateKeyAlias", "test");
        config.put("publicKeyAlias", "test");
        config.put("isRequestSign", "true");
        config.put("keepSecurityHeaders", "true");
        config.put("WSPEndpoint", "default");
        config.put("EncryptionAlgorithm", "AES");
        config.put("EncryptionStrength", "128");
        config.put("SigningRefType", "DirectReference");
        config.put("DnsClaim", "wsc");
        config.put("SignedElements", "Body,SecurityToken,Timestamp,To,From,ReplyTo,Action,MessageID");
        config.put("AgentType", "WSCAgent");
        createAgent(token, idrepo, "wsc", "wsc", "WSC", "", config);

        // Add WSP configuration
        config.remove("AgentType");
        config.put("AgentType", "WSPAgent");
        config.put("DetectMessageReplay", "true");
        config.put("DetectUserTokenReplay", "true");
        config.remove("SecurityMech");
        config.put("SecurityMech", "urn:sun:wss:security:null:UserNameToken,"
                + "urn:sun:wss:security:null:SAML2Token-HK," + "urn:sun:wss:security:null:SAML2Token-SV,"
                + "urn:sun:wss:security:null:X509Token");
        config.remove("DnsClaim");
        config.put("DnsClaim", "wsp");
        createAgent(token, idrepo, "wsp", "wsp", "WSP", "", config);
        config.remove("keepSecurityHeaders");        

        // Add STS Client configuration
        config.remove("AgentType");
        config.put("DnsClaim", "wsc");
        config.put("AgentType", "STSAgent");
        config.remove("SecurityMech");
        config.remove("keepSecurityHeaders");
        config.remove("DetectMessageReplay");
        config.remove("DetectUserTokenReplay");
        config.remove("WSPEndpoint");
        config.put("SecurityMech", "urn:sun:wss:security:null:X509Token");
        config.put("STSEndpoint", serverURL + deployuri + "/sts");
        config.put("STSMexEndpoint", serverURL + deployuri + "/sts/mex");
        config.put("WSTrustVersion", "1.3");
        config.put("KeyType", "PublicKey");
        createAgent(token, idrepo, "SecurityTokenService", "SecurityTokenService", "STS", "", config);
        config.remove("KeyType");

        // Add Agent Authenticator configuration
        Map<String, String> configAgentAuth = new HashMap<String, String>();
        configAgentAuth.put("AgentType", "SharedAgent");
        configAgentAuth.put("sunIdentityServerDeviceStatus", "Active");
        configAgentAuth.put("AgentsAllowedToRead", "wsc,wsp,SecurityTokenService");
        createAgent(token, idrepo, "agentAuth", "changeit", "Agent_Authenticator", "", configAgentAuth);
        
        SetupProgress.reportEnd("emb.done", null);
    }


    private static void createAgent(SSOToken adminToken, AMIdentityRepository idrepo, String name, String password,
            String type, String desc, Map<String, String> config) throws IdRepoException, SSOException {
        AMIdentity amid = new AMIdentity(adminToken, name, IdType.AGENTONLY, "/", null);
        if (!amid.isExists()) {
            Map<String, Set<String>> attributes = new HashMap<String, Set<String>>();

            Set<String> values = new HashSet<String>();
            values.add(password);
            attributes.put("userpassword", values);

            for (String key : config.keySet()) {
                String value = config.get(key);
                values = new HashSet<String>();
                if (value.contains(",")) {
                    StringTokenizer st = new StringTokenizer(value, ",");
                    while (st.hasMoreTokens()) {
                        values.add(st.nextToken());
                    }
                } else {
                    values.add(value);
                }
                attributes.put(key, values);
            }

            idrepo.createIdentity(IdType.AGENTONLY, name, attributes);
        }
    }

    private static void createMonitoringAuthFile(String basedir, String deployuri) {
        SetupProgress.reportStart("configurator.progress.setup.monitorauthfile", null);
        /*
         *  make sure the basedir + "/" + deployuri + "/lib/registration"
         *  directory exists, and then create the monitoring auth file
         *  there.
         */
        String monAuthFile = basedir + "/" + deployuri + "/openam_mon_auth";
        String encpwd = AccessController.doPrivileged(new EncodeAction("changeit"));
        try {
            File mFileSave = new File(monAuthFile + "~");
            File monFile = new File(monAuthFile);
            // Check for Existing File
            if (monFile.exists()) {
                monFile.renameTo(mFileSave);
            }
            FileWriter fwrtr = new FileWriter (monFile);
            String stout = "demo " + encpwd + "\n";
            fwrtr.write(stout);
            fwrtr.flush();
        } catch (IOException ex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME)
                    .error("AMSetupServlet.createMonitoringAuthFile:failed to create monitoring authentication file");
            SetupProgress.reportEnd("emb.failed", null);
        }
    }

    /**
     * Makes sure that the 'basedir + "/" + deployuri + "/auth/ace/data"' directory exists.
     */
    private static void setupSecurIDDirs(String basedir, String deployuri) {

        String aceDataDir = basedir + "/" + deployuri + "/auth/ace/data";
        File dataAceDir = new File(aceDataDir);
        if (!dataAceDir.mkdirs()) {
            Debug.getInstance(SetupConstants.DEBUG_NAME)
                    .error("AMSetupServlet.setupSecurIDDirs: failed to create SecurID data directory");
        }
    }

    private static void copyAuthSecurIDFiles (String destDir) {
        /*
         * not rsa_api.properties, as it's tagged swapped and
         * written to <configdir>/<uri>/auth/ace/data earlier.
         *
         * if file isn't copied, it's probably because this is
         * an OpenSSO deployment, rather than OpenSSO, so it would
         * just be informational, but at the debug error level.
         * additionally, before some point, the debug stuff can't
         * be invoked.
         */
        try {
            copyCtxFile("/WEB-INF/classes", "log4j.properties", destDir);
        } catch (IOException ioex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.copyAuthSecurIDFiles:", ioex);
        }
    }

    private static boolean copyCtxFile (String srcDir, String file, String destDir) throws IOException {
        InputStream in = getResourceAsStream(servletCtx, srcDir + "/" + file);
        if (in != null) {
            FileOutputStream fos = new FileOutputStream(destDir + "/" + file);
            byte[] b = new byte[2000];
            int len;
            while ((len = in.read(b)) > 0) {
                fos.write(b, 0, len);
            }
            fos.close();
            in.close();
        } else {
            return false;
        }
        return true;
    }
    
    private static void initDSConfigMgr(String str) throws LDAPServiceException {
        ByteArrayInputStream bis = null;
        try {
            bis = new ByteArrayInputStream(str.getBytes());
            DSConfigMgr.initInstance(bis, true);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    private static String mapToString(Map<String, Object> map) {
        StringBuilder buff = new StringBuilder();
        for (String key : map.keySet()) {
            buff.append(key).append("=").append((String)map.get(key))
                .append("\n");
        }
        return buff.toString();
    }

    private static String determineOS() {
        String OS_ARCH = System.getProperty("os.arch");
        String OS_NAME = System.getProperty("os.name");
        if (OS_ARCH.toLowerCase().contains(SetupConstants.X86) ||
                OS_ARCH.toLowerCase().contains(SetupConstants.AMD)) {
            if (OS_NAME.toLowerCase().contains(SetupConstants.WINDOWS)) {
                return SetupConstants.WINDOWS;
            } else {
                if (OS_NAME.toLowerCase().contains(SetupConstants.SUNOS)) {
                    return SetupConstants.X86SOLARIS;
                } else {
                    return SetupConstants.LINUX;
                }
            }
        } else {
            return SetupConstants.SOLARIS;
        }
    }

    private static void setLocale(IHttpServletRequest request) {
        Map<String, Object> map = request.getParameterMap();
        String superLocale = (String)map.get("locale");
        if (superLocale != null && superLocale.length() > 0) {
            configLocale = Locale.getLocaleObjFromAcceptLangHeader(superLocale);
        } else {
            String acceptLangHeader = request.getHeader("Accept-Language");
            if (acceptLangHeader != null && acceptLangHeader.length() > 0) {
                configLocale = Locale.getLocaleObjFromAcceptLangHeader(acceptLangHeader);
            } else {
                configLocale = java.util.Locale.getDefault();
            }
        }
        SetupProgress.setLocale(configLocale);
    }
    
    private static void createDotVersionFile(String basedir) throws IOException {
        String version = SystemProperties.get(Constants.AM_VERSION);
        writeToFile(basedir + "/.version", version);
    }
    
    private static boolean isEmbeddedDS() {
        return new File(getBaseDir() + OPENDS_DIR).exists();
    }

    /**
     * Synchronizes embedded replication state with current server list.
     * @return boolean true is sync succeeds else false.
     */
    private static boolean syncServerInfoWithRelication() {
        // We need to execute syn only if we are in Embedded mode
        if (!isEmbeddedDS()) {
            return true;
        }

        try {
            if (getAdminSSOToken() == null) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.syncServerInfoWithRelication: "
                        + "Could not sync servers with embedded replication:no admin token");
                return false;
            }
            // Determine which server this is
            String myName = WebtopNaming.getLocalServer();

            // See if we need to execute sync

            // Check if we are already replication with other servers
            Properties props = ServerConfiguration.getServerInstance(adminToken, myName);
            String syncFlag = props.getProperty(Constants.EMBED_SYNC_SERVERS);
            if ("off".equals(syncFlag)) {
                return true;
            }
            // Get server list
            Set<String> serverSet = ServerConfiguration.getServers(adminToken);
            if (serverSet == null) { 
                return true;
            }

            String dsAdminPort = props.getProperty(Constants.DS_ADMIN_PORT);

            Set<String> currServerSet = new CaseInsensitiveHashSet<String>();
            Set<String> currServerDSSet = new CaseInsensitiveHashSet<String>();
            Set<String> currServerDSAdminPortsSet = new CaseInsensitiveHashSet<String>();

            for (String sname : serverSet) {
                Properties p = ServerConfiguration.getServerInstance(adminToken, sname);
                String hname = p.getProperty(Constants.AM_SERVER_HOST);
                String rPort = p.getProperty(Constants.EMBED_REPL_PORT);
                currServerSet.add(hname + ":" + rPort);
                ServerGroup sg = getSMSServerGroup(sname); 
                currServerDSSet.add(hname + ":" + getSMSPort(sg));
                currServerDSAdminPortsSet.add(hname + ":" + p.getProperty(Constants.DS_ADMIN_PORT));
            }

            // Ensure OpenDJ system properties are setup so that it can discover its installation root
            final String embeddedDjInstallRoot = getBaseDir() + "/" + SetupConstants.SMS_OPENDS_DATASTORE;
            for (String property : OpenDJUpgrader.INSTALL_ROOT_PROPERTIES) {
                System.setProperty(property, embeddedDjInstallRoot);
            }

            // Force initialization of embedded DJ configuration with the correct installation root
            if (!ConfigurationFramework.getInstance().isInitialized()) {
                ConfigurationFramework.getInstance().initialize(embeddedDjInstallRoot);
            }

            ServerGroup sGroup = getSMSServerGroup(myName); 
            boolean stats = EmbeddedOpenDS.syncReplicatedServers(currServerSet, dsAdminPort, getSMSPassword(sGroup));
            boolean statd = EmbeddedOpenDS.syncReplicatedDomains(currServerSet, dsAdminPort, getSMSPassword(sGroup));
            boolean statl = EmbeddedOpenDS.syncReplicatedServerList(currServerDSAdminPortsSet, getSMSPort(sGroup),
                    getSMSPassword(sGroup));
            return stats || statd || statl;
        } catch (Exception ex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.syncServerInfoWithRelication: "
                    + "Could not sync servers with embedded replication:", ex);
            return false;
        }
    }

    /**
     * Gets <code>ServerGroup</code> for SMS for specified server
     * @param sname servername of groupo to find.
     * @return <code>ServerGroup</code> instance
     */
    private static ServerGroup getSMSServerGroup(String sname) throws Exception {
        String xml = ServerConfiguration.getServerConfigXML(adminToken, sname);
        ServerConfigXML scc = new ServerConfigXML(xml);
        return scc.getSMSServerGroup() ;
    }
    /**
     * Gets clear password of SMS datastore
     * @param ssg <code>ServerGroup</code> instance representing SMS
     * or Configuration datastore.
     * @return clear password
     */
    private static String getSMSPassword(ServerGroup ssg) throws Exception {
        DirUserObject sduo = (DirUserObject) ssg.dsUsers.get(0);
        String epass = sduo.password;
        return AccessController.doPrivileged(new DecodeAction(epass));
    }
    /**
     * Gets port number of SMS datastore
     * @param ssg <code>ServerGroup</code> instance representing SMS
     * or Configuration datastore.
     * @return port
     */
    private static String getSMSPort(ServerGroup ssg) throws Exception {
        ServerObject sobj = (ServerObject) ssg.hosts.get(0);
        return sobj.port;
    }

    private static void updateReplPortInfo(Map<String, Object> map) {
        try {
            String instanceName = WebtopNaming.getLocalServer();
            Map<String, Object> newValues = new HashMap<String, Object>();
            // Update this instance first...
            newValues.put("com.sun.embedded.replicationport", map.get(SetupConstants.DS_EMB_REPL_REPLPORT1));
            ServerConfiguration.setServerInstance(getAdminSSOToken(), instanceName, newValues);

            // Update remote instance
            instanceName = (String) map.get(SetupConstants.DS_EMB_EXISTING_SERVERID);
            newValues.put("com.sun.embedded.replicationport", map.get(SetupConstants.DS_EMB_REPL_REPLPORT2));
            // Update remote instance ...
            ServerConfiguration.setServerInstance(getAdminSSOToken(), instanceName, newValues);
        } catch (Exception ex ) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("AMSetupServlet.updateReplPortInfo: "
                    + "could not add replication port info to SM", ex);
        }
    }

    // Method to convert the domain name to the root suffix.
    // eg., Domain Name amqa.test.com is converted to root suffix
    // DC=amqa,DC=test,DC=com
    private static String dnsDomainToDN(String domainName) {
        StringBuilder buf = new StringBuilder();
        for (String token : domainName.split("\\.")) {
            if (token.isEmpty()) {
                continue;
            } else {
                buf.append(",");
            }
            buf.append("DC=").append(token);
        }
        return buf.toString();
    }

    // Method to get hostname and port number with the
    // provided Domain Name for Active Directory user data store.
    private String[] getLdapHostAndPort(String domainName) throws NamingException, IOException {
        if (!domainName.endsWith(".")) {
            domainName+='.';
        }
        DirContext ictx;
        // Check if domain name is a valid one.
        // The resource record type A is defined in RFC 1035.
        try {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            ictx = new InitialDirContext(env);
            Attributes attributes = ictx.getAttributes(domainName, new String[]{"A"});
            Attribute attrib = attributes.get("A");
            if (attrib == null) {
                throw new NamingException();
            }
        } catch (NamingException e) {
            // Failed to resolve domainName to A record.
            // throw exception.
            throw e;
        }

        // then look for the LDAP server
        String serverHostName;
        String serverPortStr;
        String ldapServer = "_ldap._tcp." + domainName;
        try {
            // Attempting to resolve ldapServer to SRV record.
            // This is a mechanism defined in MSDN, querying
            // SRV records for _ldap._tcp.DOMAINNAME.
            // and get host and port from domain.
            Attributes attributes = ictx.getAttributes(ldapServer, new String[]{"SRV"});
            Attribute attr = attributes.get("SRV");
            if (attr == null) {
                throw new NamingException();
            }
            String[] srv = attr.get().toString().split(" ");
            String hostNam = srv[3];
            serverHostName = hostNam.substring(0, hostNam.length() -1);
            serverPortStr = srv[2];
        } catch (NamingException e) {
            // Failed to resolve ldapServer to SRV record.
            // throw exception.
            throw e;
        }
       // try to connect to LDAP port to make sure this machine
       // has LDAP service
       int serverPort = Integer.parseInt(serverPortStr);
       try {
           new Socket(serverHostName, serverPort).close();
       } catch (IOException e) {
           throw e;
       }

       String[] hostAndPort = new String[2];
       hostAndPort[0] = serverHostName;
       hostAndPort[1] = serverPortStr;

       return hostAndPort;
   }

    private static void registerListeners() {
        if (isCurrentConfigurationValid()) {
            ServiceLoader<SetupListener> listeners = ServiceLoader.load(SetupListener.class);
            for (SetupListener p : listeners) {
                p.setupComplete();
            }
        }
    }
}
