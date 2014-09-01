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
 * $Id: AMSetupServlet.java,v 1.117 2010/01/20 17:01:35 veiming Exp $
 *
 */

/**
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

package com.sun.identity.setup;

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
import com.sun.identity.common.ConfigMonitoring;
import com.sun.identity.common.DebugPropertiesObserver;
import com.sun.identity.common.FQDNUtils;
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
import com.sun.identity.shared.StringUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
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
import com.sun.identity.sm.SessionHAFailoverSetupSubConfig;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import org.forgerock.openam.upgrade.OpenDJUpgrader;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeUtils;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.security.AccessController;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class is the first class to get loaded by the Servlet 
 * container. 
 * It has helper methods to determine the status of Access Manager 
 * configuration when deployed as a single web-application. If 
 * Access Manager is not deployed as single web-application then the 
 * configured status returned is always true.   
 */
public class AMSetupServlet extends HttpServlet {
    private static ServletContext servletCtx = null;
    private static boolean isConfiguredFlag = false;
    private static boolean isVersionNewer = false;
    private static boolean upgradeCompleted = false;
    private final static String SMS_STR = "sms";
    private static SSOToken adminToken = null;
    private final static String LEGACY_PROPERTIES = "legacy";

    final static String BOOTSTRAP_EXTRA = "bootstrap";    
    final static String BOOTSTRAP_FILE_LOC = "bootstrap.file";
    final static String OPENDS_DIR = "/opends";
    private static final String COLON = ":";
    private static final String HTTPS = "https";

    private static String errorMessage = null;
    private static java.util.Locale configLocale;
    private static boolean debugEnabled = false;

    private static Set passwordParams = new HashSet();

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
        
        if (isConfiguredFlag && !ServerConfiguration.isLegacy()) { 
            // this will sync up bootstrap file will serverconfig,xml
            // due startup; and also register the observer.
            ServerConfigXMLObserver.getInstance().update(true);

            // Syncup embedded opends replication with current 
            // server instances.
            if (syncServerInfoWithRelication() == false) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "AMSetupServlet.init: embedded replication sync failed.");
            }
        }
        
        isVersionNewer();
    }

    /*
     * Flag indicating if OpenAM is configured with the latest valid config
     */  
    static public boolean isCurrentConfigurationValid() {
        if (isConfiguredFlag) {
            isVersionNewer = UpgradeUtils.isVersionNewer();    
        }
        
        return isConfiguredFlag && !isVersionNewer && !upgradeCompleted;
    } 
    
    static public boolean isConfigured() {
        return isConfiguredFlag;
    }

    static boolean isConfigured(String baseDir) {
        String bootstrapFile = baseDir + "/bootstrap";
        File file = new File(bootstrapFile);
        if (file.exists()) {
            try {
                isConfiguredFlag = Bootstrap.load(
                    new BootstrapData(baseDir), false);
                    LoginLogoutMapping.setProductInitialized(
                        isConfiguredFlag);
                return isConfiguredFlag;
            } catch (Exception e) {
                //ignore
                return false;
            }
        } else {
            return false;
        }
    }
    
    /**
     * Used this method to check if the version is newer; called post upgrade
     */
    public static void isVersionNewer() {
        if (isConfiguredFlag) {
            isVersionNewer = UpgradeUtils.isVersionNewer();
        }       
    }

    public static void upgradeCompleted() {
        upgradeCompleted = true;
    }

    public static boolean isUpgradeCompleted() {
        return upgradeCompleted;
    }

    public static void enableDebug() {
        Collection<Debug> debugInstances = Debug.getInstances();
        
        for (Debug d : debugInstances) {
            d.setDebug(Debug.MESSAGE);
        }
        
        debugEnabled = true;
    }
    
    public static void disableDebug() {
        if (debugEnabled) {
            Collection<Debug> debugInstances = Debug.getInstances();

            for (Debug d : debugInstances) {
                d.setDebug(Debug.ERROR);
            }
        }
    }
    
    /**
     * Checks if the embedded directory (if present) needs to be upgraded
     */
    public static void checkOpenDJUpgrade() {
        // check for embedded directory
        if (!isEmbeddedDS()) {
            return;
        }
        
        // check if upgrade is required
        OpenDJUpgrader upgrader = new OpenDJUpgrader(getBaseDir() + OPENDS_DIR, servletCtx);
        if (!upgrader.isUpgradeRequired()) {
            return;
        }
        
        // backup embedded directory
        createOpenDJBackup();
                
        // initiate upgrade
        try {
            upgrader.upgrade();
        } catch (Exception ex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("OpenDJ upgrade exception: ", ex);
        }
    }
    
    protected static void createOpenDJBackup() {
        try {
            UpgradeServices.createUpgradeDirectories(getBaseDir());
        } catch (UpgradeException ue) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("Upgrade cannot create backup directory", ue);
            return;
        }
        
        ZipOutputStream zOut = null;
        String baseDir = getBaseDir();
        String backupDir = baseDir + "/backups/";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStamp = dateFormat.format(new Date());
        File backupFile = new File(backupDir + "opendj.backup." + dateStamp + ".zip");
  
        if (backupFile.exists()) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("Upgrade cannot continue as backup file exists! " + backupFile.getName());
            return;
        }
       
        File opendjDirectory = new File(baseDir + OPENDS_DIR);
        if (opendjDirectory.exists() && opendjDirectory.isDirectory()) {
            final String[] filenames = opendjDirectory.list();
            
            try {
                zOut = new ZipOutputStream(new FileOutputStream(backupFile));
                
                // Compress the files
                for (int i = 0; i < filenames.length; i++) {
                    zipDir(new File(baseDir + OPENDS_DIR + File.separator + filenames[i]), 
                                    baseDir + OPENDS_DIR + File.separator, zOut, (baseDir + File.separator).length());
                }

                zOut.close();
            } catch (IOException ioe) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error("IOException", ioe);
            } finally {
                if (zOut != null) {
                    try {
                        zOut.close();
                    } catch (IOException ioe) {
                        // do nothing
                    }
                }
            }
        }
    }
    
    protected static void zipDir(File filename, String dirName, ZipOutputStream zOut, int stripLen) {
        try {
            if (!filename.exists()) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error("file not found");
            }
            
            if (!filename.isDirectory()) {
                zOut.putNextEntry(new ZipEntry((dirName + filename.getName()).replace('\\','/').substring(stripLen)));
                FileInputStream fileIn = new FileInputStream(filename);
                byte[] buffer =new byte[(int)filename.length()];
                int inLen = fileIn.read(buffer);
 
                if (inLen != filename.length()) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME).error("Short read: " + (filename.length() - inLen));
                }
            
                zOut.write(buffer, 0, buffer.length);
                zOut.closeEntry();
                fileIn.close();
                filename = null;
            } else {
                String subdirname = dirName + filename.getName() + File.separator;
                filename = null;
                zOut.putNextEntry(new ZipEntry(subdirname.replace('\\','/').substring(stripLen)));
                zOut.closeEntry();
                
                String[] dirlist=(new File(subdirname)).list();
                
                for(int i = 0 ; i < dirlist.length ; i++){
                    zipDir(new File(subdirname + dirlist[i]), subdirname, zOut, stripLen);
                }
            }
        } catch(Exception ex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error("Unable to create zip file", ex);
        }        
    }

    /**
     * Checks if the product is already configured. This is required 
     * when the container on which WAR is deployed is restarted. If  
     * product is configured the flag is set true. Also the flag is
     * set to true in case of non-single war deployment.
     */
    public static void checkConfigProperties() {
        String overrideAMC = SystemProperties.get(
            SetupConstants.AMC_OVERRIDE_PROPERTY);
        isConfiguredFlag = overrideAMC == null || 
            overrideAMC.equalsIgnoreCase("false");
        
        if ((!isConfiguredFlag) && (servletCtx != null)) {
            String baseDir = getBaseDir();
            try {
                String bootstrapFile = getBootStrapFile();
                if (bootstrapFile != null) {
                    isConfiguredFlag = Bootstrap.load(
                        new BootstrapData(baseDir), false);
                } else {                    
                    if (baseDir != null) {
                        isConfiguredFlag = loadAMConfigProperties(
                            baseDir + "/" + 
                            SetupConstants.AMCONFIG_PROPERTIES);
                    }
                }
            } catch (ConfiguratorException e) {
                //ignore, WAR may not be configured yet.
            	System.out.println("checkConfigProperties :" +e);
            } catch (Exception e) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "AMSetupServlet.checkConfigProperties", e);
            }
        }
    }
    
    private static boolean loadAMConfigProperties(String fileLocation)
        throws IOException {
        boolean loaded = false;
        File test = new File(fileLocation);
        
        if (test.exists()) {
            FileInputStream fin = null;
            
            try {
                fin = new FileInputStream(fileLocation);
                if (fin != null) {
                    Properties props = new Properties();
                    props.load(fin);
                    SystemProperties.initializeProperties(props);
                    loaded =true;
                }
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
    public static boolean checkInitState(ServletContext servletctx) {
        return isConfiguredFlag;
    }

    static String configure(ServletContext servletct, Map data) {
        servletCtx = servletct;
        HttpServletRequestWrapper req = 
            new HttpServletRequestWrapper(data);
        HttpServletResponseWrapper res = 
            new HttpServletResponseWrapper(null);
        boolean result = processRequest(req, res);
        return (result) ? (String)req.getParameter(
            SetupConstants.CONFIG_VAR_BASE_DIR) : null;
            
    }

    @Override
    public void doPost(HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException, ConfiguratorException {
        
        HttpServletRequestWrapper req = 
            new HttpServletRequestWrapper(request);
        HttpServletResponseWrapper res = 
            new HttpServletResponseWrapper(response);
           
        String loadBalancerHost = 
            request.getParameter("LB_SITE_NAME");
        String primaryURL = request.getParameter("LB_PRIMARY_URL");
        
        if (loadBalancerHost != null) {     
            // site configuration is passed as a map of the site 
            // information 
            Map siteConfig = new HashMap(5);
            siteConfig.put(SetupConstants.LB_SITE_NAME, loadBalancerHost);
            siteConfig.put(SetupConstants.LB_PRIMARY_URL, primaryURL);
            req.addParameter(
                SetupConstants.CONFIG_VAR_SITE_CONFIGURATION, siteConfig);
        }
                
        String userStoreType = request.getParameter("USERSTORE_TYPE");        

       if (userStoreType != null) {     
            // site configuration is passed as a map of the site information 
            Map store = new HashMap(12);  
            String tmp = (String)request.getParameter(
                "USERSTORE_DOMAINNAME");
            String domainName = tmp;
            store.put(SetupConstants.USER_STORE_DOMAINNAME, tmp);
            tmp = (String)request.getParameter("USERSTORE_HOST"); 
            if (tmp == null || tmp.length() == 0) {
                String[] hostAndPort = {""};
                try {
                    if (domainName != null && 
                        domainName.length() > 0) {
                        hostAndPort = getLdapHostAndPort(domainName);
                    }
                } catch (NamingException nex) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME)
                        .error("AMSetupServlet:Naming Exception"+
                        "get host and port from domain name" + nex);
                        
                } catch (IOException ioex) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME)
                        .error("AMSetupServlet:IO Exception. get"+
                        " host and port from domain name" + ioex);
                }
                String host = hostAndPort[0];
                String port = hostAndPort[1];
                if (host != null) {
                    store.put(SetupConstants.USER_STORE_HOST, host);
                    store.put(SetupConstants.USER_STORE_PORT, port);
                }
            } else {
                store.put(SetupConstants.USER_STORE_HOST, tmp);
                tmp = (String)request.getParameter("USERSTORE_PORT");
                store.put(SetupConstants.USER_STORE_PORT, tmp);
            }
            tmp = (String)request.getParameter("USERSTORE_SSL");
            store.put(SetupConstants.USER_STORE_SSL, tmp);
            tmp = (String)request.getParameter("USERSTORE_SUFFIX");
            if (tmp == null || tmp.length() == 0) {
                if (domainName != null && domainName.length() > 0) {
                    String umRootSuffix = dnsDomainToDN(domainName);
                    store.put(SetupConstants.USER_STORE_ROOT_SUFFIX,
                        umRootSuffix);
                }
            } else {
                store.put(SetupConstants.USER_STORE_ROOT_SUFFIX, tmp);
            }
            tmp = (String)request.getParameter("USERSTORE_MGRDN"); 
            store.put(SetupConstants.USER_STORE_LOGIN_ID, tmp);      
            tmp = (String)request.getParameter("USERSTORE_PASSWD"); 
            store.put(SetupConstants.USER_STORE_LOGIN_PWD, tmp);      
            store.put(SetupConstants.USERSTORE_PWD, tmp);      
            store.put(SetupConstants.USER_STORE_TYPE, userStoreType);

            req.addParameter("UserStore", store);
        }
        
        boolean result = processRequest(req, res);
       
        if (result == false) {
            response.getWriter().write( 
                    "Configuration failed - check installation logs!" );
        } else
            response.getWriter().write( "Configuration complete!" );
    }
        
    /**
     * The main entry point for configuring Access Manager. The parameters
     * are passed from configurator page.
     *
     * @param request Servlet request.
     * @param response Servlet response. 
     * @return <code>true</code> if the configuration succeeded.
     */
    public static boolean processRequest(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        HttpServletRequestWrapper req = new HttpServletRequestWrapper(request);
        HttpServletResponseWrapper res = new HttpServletResponseWrapper(
            response);
        return processRequest(req, res);
    }

    public static boolean processRequest(
            IHttpServletRequest request,
            IHttpServletResponse response
    ) {
        setLocale(request);
        InstallLog.getInstance().open();
        /*
         * This logic needs refactoring later. setServiceConfigValues()
         * attempts to check if directory is up and makes a call
         * back to this class. The implementation'd
         * be cleaner if classes&methods are named better and separated than 
         * intertwined together.
         */
        ServicesDefaultValues.setServiceConfigValues(request);

        // set debug directory
        Map map = ServicesDefaultValues.getDefaultValues();
        String basedir = (String)map.get(SetupConstants.CONFIG_VAR_BASE_DIR); 
        String uri = (String)map.get(SetupConstants.CONFIG_VAR_SERVER_URI);
        SystemProperties.initializeProperties(
            Constants.SERVICES_DEBUG_DIRECTORY, basedir + uri + "/debug");

        // used for site configuration later
        Map siteMap = (Map)map.remove(
            SetupConstants.CONFIG_VAR_SITE_CONFIGURATION);

        Map userRepo = (Map)map.remove("UserStore");
        
        try {
            isConfiguredFlag = configure(request, map, userRepo);
            if (isConfiguredFlag) {
                FQDNUtils.getInstance().init();
                //postInitialize was called at the end of configure????
                postInitialize(getAdminSSOToken());
            }
            LoginLogoutMapping.setProductInitialized(isConfiguredFlag);
            registerListeners();
            
            if (isConfiguredFlag) {
                boolean legacy = ServerConfiguration.isLegacy();
                String fileBootstrap = getBootstrapLocator();
                if (fileBootstrap != null) {
                   writeToFileEx(fileBootstrap, basedir);
                }

                if (!legacy) {
                    // this will write bootstrap file after configuration is 
                    // done; and also register the observer.
                    ServerConfigXMLObserver.getInstance().update(true);
                    Map mapBootstrap = new HashMap(2);
                    Set set = new HashSet(2);
                    set.add(fileBootstrap);
                    mapBootstrap.put(BOOTSTRAP_FILE_LOC, set);

                    if (fileBootstrap == null) {
                        set.add(getPresetConfigDir());
                    } else {
                        set.add(fileBootstrap); 
                    }
                    // this is to store the bootstrap location
                    String serverInstanceName = 
                        SystemProperties.getServerInstanceName(); 

                    SSOToken adminToken = (SSOToken)
                        AccessController.doPrivileged(
                        AdminTokenAction.getInstance());
                    ServerConfiguration.setServerInstance(adminToken,
                        serverInstanceName, mapBootstrap);

                    // store the ds admin port if we are running in embedded mode
                    String dataStore = (String) map.get(
                        SetupConstants.CONFIG_VAR_DATA_STORE);

                    if (dataStore.equals(SetupConstants.SMS_EMBED_DATASTORE)) {
                        String dsAdminPort =
                            (String)map.get(SetupConstants.CONFIG_VAR_DIRECTORY_ADMIN_SERVER_PORT);
                        Map mapAdminPort = new HashMap(2);
                        Set set2 = new HashSet(2);
                        set2.add(dsAdminPort);
                        mapAdminPort.put(Constants.DS_ADMIN_PORT, set2);
                        ServerConfiguration.setServerInstance(adminToken,
                            serverInstanceName, mapAdminPort);
                    }

                    // setup site configuration information
                    if ((siteMap != null) && !siteMap.isEmpty()) {
                        String site = (String)siteMap.get(
                            SetupConstants.LB_SITE_NAME);
                        String primaryURL = (String)siteMap.get(
                            SetupConstants.LB_PRIMARY_URL);
                        Boolean isSessionHASFOEnabled = Boolean.valueOf( (String)siteMap.get(
                                SetupConstants.LB_SESSION_HA_SFO));

                        /* 
                         * If primary url is null that means we are adding
                         * to an existing site. we don't need to create it 
                         * first.
                         */
                        if ((primaryURL != null) && (primaryURL.length() > 0)) {
                            Set sites = SiteConfiguration.getSites(adminToken);
                            if (!sites.contains(site)) {
                                SiteConfiguration.createSite(adminToken,
                                    site, primaryURL, Collections.EMPTY_SET);
                            }
                        } 

                        if (!ServerConfiguration.belongToSite( 
                            adminToken, serverInstanceName, site)) 
                        {
                            ServerConfiguration.addToSite(
                                adminToken, serverInstanceName, site);
                        }

                        /**
                         * Now create the SubSchema for Global Session to automate
                         * setting the Session HA Failover property.
                         * @since 10.1.0
                         */
                        Map values = new HashMap(1);
                        Set innerValues = new HashSet(1);
                        innerValues.add(isSessionHASFOEnabled.toString());
                        values.put(CoreTokenConstants.IS_SFO_ENABLED, innerValues);
                        SessionHAFailoverSetupSubConfig.getInstance().
                                createSessionHAFOSubConfigEntry(adminToken, site,
                                        SessionHAFailoverSetupSubConfig.AM_SESSION_SERVICE, values);
                    } // End of site map check.
                    if (EmbeddedOpenDS.isMultiServer(map)) {
                        // Setup Replication port in SMS for each server
                        updateReplPortInfo(map);
                    }
                    EntitlementConfiguration ec =
                        EntitlementConfiguration.getInstance(
                        SubjectUtils.createSuperAdminSubject(), "/");
                    ec.reindexApplications();

                }
            }
        } catch (Exception e) {
            InstallLog.getInstance().write(
                 "AMSetupServlet.processRequest: error", e);
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.processRequest: error", e);
            Object[] params = {e.getMessage(), basedir};
            throw new ConfiguratorException("configuration.failed",
                params, configLocale);
        } finally {
            InstallLog.getInstance().close();
            SetupProgress.closeOutputStream();
        }

        if (WebtopNaming.configMonitoring() >= 0) {
            ConfigMonitoring cm = new ConfigMonitoring();
            cm.configureMonitoring();
        } else {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "WebtopNaming.configMonitoring returned error.");
        }

        return isConfiguredFlag;
    }
    
    private static void writeInputToFile(IHttpServletRequest request)
        throws IOException {
        StringBuilder buff = new StringBuilder();
        Map<String, ?> map = request.getParameterMap();
        
        for (Map.Entry<String, ?> entry : map.entrySet()) {
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
                    Map<String, String> valMap = (Map) entry.getValue();
                    
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
        
        String basedir = (String)map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
        writeToFile(basedir + SetupConstants.CONFIG_PARAM_FILE, buff.toString());
    }
    
    private static void checkBaseDir(String basedir, IHttpServletRequest req)
        throws IOException {
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
                throw new ConfiguratorException(
                    "Base directory specified :" +
                    basedir +
                    " cannot be used - has preexisting config data.");
            }
        }
        SetupProgress.reportEnd("emb.success", null);
    }

    // (i) install, configure and start an embedded instance.
    // or
    // (ii) install, configure, and replicate embedded instance
    private static boolean setupEmbeddedDS(Map map, String dataStore)
        throws ConfigurationException, Exception {
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
            throw new ConfigurationException(
                "OpenDJ cannot be started.");
        }

        // Determine if DITLoaded flag needs to be set: multi instance
        if (EmbeddedOpenDS.isMultiServer(map)) {
            // Replication 
            // TOFIX: Temporary fix until OpenDS auto-loads schema
            List schemaFiles = getSchemaFiles(dataStore);
            String basedir = (String) map.get(
                SetupConstants.CONFIG_VAR_BASE_DIR);
            writeSchemaFiles(basedir, schemaFiles, map, dataStore);
            // Get the remote host name from the SERVER URL  
            // entered in the 'Add to existing instance' and place
            // it in the map. This is for console configurator.
            // For cli configurator, the "DS_EMB_REPL_HOST2" would
            // be entered in the config. file itself.
            String existingInstance = (String) map.get(
                SetupConstants.DS_EMB_EXISTING_SERVERID);
            if (existingInstance != null) {
                int ndx1 = existingInstance.indexOf("://");
                if ((ndx1 != -1) && 
                    (ndx1 != (existingInstance.length() -1))) {
                    String str1 = existingInstance.substring(ndx1+3);
                    int ndx2 = str1.indexOf(":");
                    if ((ndx2 != -1) && (ndx2 != (str1.length() -1))) {
                        String finalStr = str1.substring(0, ndx2);
                        map.put(SetupConstants.DS_EMB_REPL_HOST2, 
                            finalStr);
                    }
                }
            }
            EmbeddedOpenDS.setupReplication(map);
            ditLoaded = true;
        }
        return ditLoaded;
    }
    
    private static boolean setupSMDatastore(Map map) 
        throws Exception {
        boolean isDITLoaded = ((String) map.get(
            SetupConstants.DIT_LOADED)).equals("true");
        String dataStore = (String) map.get(
            SetupConstants.CONFIG_VAR_DATA_STORE);

        if (dataStore.equals(SetupConstants.SMS_EMBED_DATASTORE)) {
            isDITLoaded = setupEmbeddedDS(map, dataStore);
        }

        if (!isDITLoaded) {
            List schemaFiles = getSchemaFiles(dataStore);
            String basedir = (String)map.get(
                SetupConstants.CONFIG_VAR_BASE_DIR);
            writeSchemaFiles(basedir, schemaFiles, map, dataStore);
        }
        return isDITLoaded;
    }
    
    private static void configureServerInstance(
        SSOToken adminSSOToken,
        String serverInstanceName,
        String strAMConfigProperties,
        boolean isDITLoaded,
        String basedir,
        String strServerConfigXML,
        Map propAMConfig,
        Map map
    ) throws SMSException, SSOException, IOException, ConfigurationException {
        SetupProgress.reportStart(
            "configurator.progress.configure.server.instance", null);
        if (ServerConfiguration.isLegacy(adminSSOToken)) {
            Map mapProp = ServerConfiguration.getDefaultProperties();
            mapProp.putAll(propAMConfig);
            appendLegacyProperties(mapProp);
            Properties tmp = new Properties();
            tmp.putAll(mapProp);
            SystemProperties.initializeProperties(tmp, true, false);

            writeToFile(basedir + "/" + SetupConstants.AMCONFIG_PROPERTIES,
                mapToString(mapProp));
            writeToFile(basedir + "/serverconfig.xml", strServerConfigXML);
            String hostname = (String) map.get(
                SetupConstants.CONFIG_VAR_SERVER_HOST);
            updatePlatformServerList(serverInstanceName, hostname);
        } else {
            try {
                if (!isDITLoaded) {
                    ServerConfiguration.createDefaults(adminSSOToken);
                }
                if (!isDITLoaded ||
                    !ServerConfiguration.isServerInstanceExist(
                    adminSSOToken, serverInstanceName)) {
                    ServerConfiguration.createServerInstance(adminSSOToken,
                        serverInstanceName,
                        ServerConfiguration.getPropertiesSet(
                        strAMConfigProperties),
                        strServerConfigXML);
                }
            } catch (UnknownPropertyNameException ex) {
            // ignore, property names are valid because they are
            // gotten from template.
            }

        }
        SetupProgress.reportEnd("emb.done", null);
    }
    
    private static boolean configure(
        IHttpServletRequest request,
        Map map,
        Map userRepo
    ) throws Exception {
        boolean configured = false;
        boolean existingConfiguration = false;
        try {
            String basedir = (String)map.get(
                SetupConstants.CONFIG_VAR_BASE_DIR);
            checkBaseDir(basedir, request);
            boolean isDITLoaded = setupSMDatastore(map);

            String serverURL = (String)map.get(
                SetupConstants.CONFIG_VAR_SERVER_URL);
            String deployuri = (String)map.get(
                SetupConstants.CONFIG_VAR_SERVER_URI);
            // do this here since initializeConfigProperties needs the dir
            setupSecurIDDirs(basedir,deployuri);

            SetupProgress.reportStart("configurator.progress.reinit.system", null);
            Map mapFileNameToConfig = initializeConfigProperties();
            String strAMConfigProperties = (String)
                mapFileNameToConfig.get(SetupConstants.AMCONFIG_PROPERTIES);
            String strServerConfigXML = (String)mapFileNameToConfig.get(
                SystemProperties.CONFIG_FILE_NAME);
            Properties propAMConfig = ServerConfiguration.getProperties(
                strAMConfigProperties);
            // Set the install property since reInitConfigProperties
            // initializes SMS which inturn initializes EventService
            propAMConfig.put(Constants.SYS_PROPERTY_INSTALL_TIME, "true");
            String serverInstanceName = serverURL + deployuri;
            reInitConfigProperties(serverInstanceName,
                propAMConfig, strServerConfigXML);
            // SystemProperties gets reinitialized and installTime property
            // has to set again
            SystemProperties.initializeProperties(
                Constants.SYS_PROPERTY_INSTALL_TIME, "true");
            SetupProgress.reportEnd("emb.done", null);
            
            SSOToken adminSSOToken = getAdminSSOToken();

            if (!isDITLoaded) {
                RegisterServices regService = new RegisterServices();
                boolean bUseExtUMDS = (userRepo != null) && !userRepo.isEmpty();
                regService.registers(adminSSOToken, bUseExtUMDS);
                processDataRequests("/WEB-INF/template/sms");
            }

            // Set installTime to false, to avoid in-memory notification from
            // SMS in cases where not needed, and to denote that service  
            // registration got completed during configuration phase and it 
            // has passed installtime.
            SystemProperties.initializeProperties(
                Constants.SYS_PROPERTY_INSTALL_TIME, "false");
            configureServerInstance(adminSSOToken, serverInstanceName,
                strAMConfigProperties, isDITLoaded, basedir, strServerConfigXML,
                propAMConfig, map);

            // Embedded :get our serverid and configure embedded idRepo
            String dataStore = (String)map.get(
                SetupConstants.CONFIG_VAR_DATA_STORE);
            boolean embedded = dataStore.equals(
                SetupConstants.SMS_EMBED_DATASTORE);
            // Ensure this service are initialized before continuing
            WebtopNaming.initialize();
            NamingService.initialize();

            if (embedded) {
                try {
                    String serverID = WebtopNaming.getAMServerID();
                    String entry = 
                      map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST)+
                      ":"+
                      map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT)+
                     "|"+ ((serverID==null)?"":serverID);
                    String orgName = (String) 
                            map.get(SetupConstants.SM_CONFIG_ROOT_SUFFIX);
                    updateEmbeddedIdRepo(orgName, "embedded", entry);
                } catch (Exception ex) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                        "EmbeddedDS : failed to setup serverid", ex);
                    throw ex;
                }
            }

            SystemProperties.setServerInstanceName(serverInstanceName);
            LDIFTemplates.copy(basedir, servletCtx);
            ServiceXMLTemplates.copy(basedir + "/template/xml", 
                servletCtx);
            createDotVersionFile(basedir);
            handlePostPlugins(adminSSOToken);
            postInitialize(adminSSOToken);
           
            if (!isDITLoaded && (userRepo != null) && 
                !userRepo.isEmpty()) {
                // Construct the SMSEntry for the node to check to 
                // see if this is an existing configuration store, 
                // or new store.
                ServiceConfig sc = 
                    UserIdRepo.getOrgConfig(adminSSOToken);
                if (sc != null) {
                    CachedSMSEntry cEntry = 
                        CachedSMSEntry.getInstance(adminSSOToken,
                        ("ou=" + userRepo.get("userStoreHostName") 
                        + "," + sc.getDN()));
                    SMSEntry entry = cEntry.getClonedSMSEntry();
                    if (entry.isNewEntry()) {
                        UserIdRepo.getInstance().configure(
                            userRepo, basedir, servletCtx, 
                            adminSSOToken);
                    } else {
                        existingConfiguration = true;
                    }
                }
            }

            /*
             * Requiring the keystore.jks file in OpenSSO workspace.
             * The createIdentitiesForWSSecurity is for the 
             * JavaEE/NetBeans integration that we had done.
             */
            createPasswordFiles(basedir, deployuri);
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
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.configure: error", e);
            errorMessage = e.getMessage();
            throw e;
        }
        return configured;
    }
    
    public static String getErrorMessage() {
        return (errorMessage != null) ? errorMessage : ""; 
    }

    private static void appendLegacyProperties(Map prop) {
        ResourceBundle res = ResourceBundle.getBundle(LEGACY_PROPERTIES);
        for (Enumeration i = res.getKeys(); i.hasMoreElements(); ) {
            String key = (String)i.nextElement();
            prop.put(key, (String)res.getString(key));
        }
    }

    private static void postInitialize(SSOToken adminSSOToken)
        throws SSOException, SMSException {
        SMSEntry.initializeClass();
        AMAuthenticationManager.reInitializeAuthServices();
        
        AMIdentityRepository.clearCache();
        ServiceManager svcMgr = new ServiceManager(adminSSOToken);
        svcMgr.clearCache();
        LoginLogoutMapping lmp = new LoginLogoutMapping();
        lmp.initializeAuth(servletCtx);
        LoginLogoutMapping.setProductInitialized(true);
    }
    
    private static Map createBootstrapResource(boolean legacy)
        throws IOException {
        Map initMap = new HashMap();
        Map map = ServicesDefaultValues.getDefaultValues();
        String serverURL = (String)map.get(
            SetupConstants.CONFIG_VAR_SERVER_URL);
        String basedir = (String)map.get(
            SetupConstants.CONFIG_VAR_BASE_DIR);
        String deployuri = (String)map.get(
                SetupConstants.CONFIG_VAR_SERVER_URI);

        String dataStore = (String)map.get(
            SetupConstants.CONFIG_VAR_DATA_STORE);
        if (legacy) {
            initMap.put(BootstrapData.FF_BASE_DIR,
                basedir);
        } else if (dataStore.equals(SetupConstants.SMS_FF_DATASTORE)) {
            initMap.put(BootstrapData.PROTOCOL, BootstrapData.PROTOCOL_FILE);
            initMap.put(BootstrapData.FF_BASE_DIR,
                basedir + deployuri + "/" + SMS_STR );
            initMap.put(BootstrapData.PWD, 
                map.get(SetupConstants.CONFIG_VAR_ADMIN_PWD));
            initMap.put(BootstrapData.BASE_DIR, basedir);
        } else {
            String tmp = (String)map.get(
                SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_SSL);
            boolean ssl = (tmp != null) && tmp.equals("SSL");
            initMap.put(BootstrapData.PROTOCOL, (ssl) ?
                BootstrapData.PROTOCOL_LDAPS : BootstrapData.PROTOCOL_LDAP);
            initMap.put(BootstrapData.DS_HOST, 
                map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST));
            initMap.put(BootstrapData.DS_PORT, 
                map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT));
            initMap.put(BootstrapData.DS_BASE_DN,
                map.get(SetupConstants.CONFIG_VAR_ROOT_SUFFIX));
            initMap.put(BootstrapData.DS_MGR,
                map.get(SetupConstants.CONFIG_VAR_DS_MGR_DN));
            initMap.put(BootstrapData.PWD, 
                map.get(SetupConstants.CONFIG_VAR_ADMIN_PWD));
            initMap.put(BootstrapData.DS_PWD, 
                map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD));
        }
        initMap.put(BootstrapData.SERVER_INSTANCE, serverURL + deployuri);
        return initMap;
    }
    
    private static void handlePostPlugins(SSOToken adminSSOToken)
        throws IllegalAccessException, InstantiationException,
            ClassNotFoundException 
    {
        if (servletCtx == null) {
            return;
        }
        List plugins = getConfigPluginClasses();
        for (Iterator i = plugins.iterator(); i.hasNext(); ) {
            ConfiguratorPlugin plugin  = (ConfiguratorPlugin)i.next();
            plugin.doPostConfiguration(servletCtx, adminSSOToken);
        }
    }

    private static List getConfigPluginClasses()
        throws IllegalAccessException, InstantiationException,
            ClassNotFoundException
    {
        List plugins = new ArrayList();
        try {
            ResourceBundle rb = ResourceBundle.getBundle(
                SetupConstants.PROPERTY_CONFIGURATOR_PLUGINS);
            String strPlugins = rb.getString(
                SetupConstants.KEY_CONFIGURATOR_PLUGINS);

            if (strPlugins != null) {
                StringTokenizer st = new StringTokenizer(strPlugins);
                while (st.hasMoreTokens()) {
                    String className = st.nextToken();
                    Class clazz = Class.forName(className);
                    plugins.add((ConfiguratorPlugin)clazz.newInstance());
                }
            }
        } catch (IllegalAccessException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.getConfigPluginClasses: error", e);
            throw e;
        } catch (InstantiationException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.getConfigPluginClasses: error", e);
            throw e;
        } catch (ClassNotFoundException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.getConfigPluginClasses: error", e);
            throw e;
        } catch (MissingResourceException e) {
            //ignore if there are no configurator plugins.
        }
        return plugins;
    }

    private static void reInitConfigProperties(
        String serverName,
        Properties prop,
        String strServerConfigXML
    ) throws FileNotFoundException, SMSException, IOException, SSOException,
        LDAPServiceException,IllegalAccessException, InstantiationException,
        ClassNotFoundException
    {
        SystemProperties.initializeProperties(prop, true, false);
        Crypt.reinitialize();
        initDSConfigMgr(strServerConfigXML);
        AdminUtils.initialize();
        SMSAuthModule.initialize();
        SystemProperties.initializeProperties(prop, true, true);
        DebugPropertiesObserver.getInstance().notifyChanges();
        SMSPropertiesObserver.getInstance().notifyChanges();
        
        List plugins = getConfigPluginClasses();
        Map map = ServicesDefaultValues.getDefaultValues();
        String basedir = (String)map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
        for (Iterator i = plugins.iterator(); i.hasNext(); ) {
            ConfiguratorPlugin plugin = (ConfiguratorPlugin)i.next();
            plugin.reinitConfiguratioFile(basedir);
        }
    }

    public static String getPresetConfigDir() 
        throws ConfiguratorException {
        String configDir = System.getProperty(
            SetupConstants.JVM_PROP_PRESET_CONFIG_DIR);

        if ((configDir == null) || (configDir.length() == 0)) {
            try {
                ResourceBundle rb = ResourceBundle.getBundle(
                    SetupConstants.BOOTSTRAP_PROPERTIES_FILE);
                configDir = rb.getString(SetupConstants.PRESET_CONFIG_DIR);
            } catch (MissingResourceException e) {
                //ignored because bootstrap properties file maybe absent.
            }
        }
            
        if ((configDir != null) && (configDir.length() > 0)) {
            String realPath = getNormalizedRealPath(servletCtx);
            if (realPath != null) {
                configDir = StringUtils.strReplaceAll(configDir,
                    SetupConstants.TAG_REALPATH, realPath);
            } else {
                throw new ConfiguratorException(
                   "cannot get configuration path");
            }
        }

        return configDir;
    }

    /**
     * Returns location of the bootstrap file.
     *
     * @return Location of the bootstrap file. Returns null if the file
     *         cannot be located 
     * @throws ConfiguratorException if servlet context is null or deployment
     *         application real path cannot be determined.
     */
    static String getBootStrapFile()
        throws ConfiguratorException {
        String bootstrap = null;
        
        String configDir = getPresetConfigDir();
        if ((configDir != null) && (configDir.length() > 0)) {
            bootstrap = configDir + "/bootstrap";
        } else {
            String locator = getBootstrapLocator();
            FileReader frdr = null;

            try {
                frdr = new FileReader(locator);
                BufferedReader brdr = new BufferedReader(frdr);
                bootstrap = brdr.readLine() + "/bootstrap";
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
            if (!test.exists()) {
                bootstrap = null;
            }
        }
        return bootstrap;
    }

    // this is the file which contains the base dir.
    // this file is not created if configuration directory is 
    // preset in bootstrap.properties
    private static String getBootstrapLocator()
        throws ConfiguratorException {
        String configDir = getPresetConfigDir();
        if ((configDir != null) && (configDir.length() > 0)) {
            return null;
        }
        
        if (servletCtx != null) {
            String path = getNormalizedRealPath(servletCtx);
            if (path != null) {
                String home = System.getProperty("user.home");
                File newPath = new File(home + "/" + SetupConstants.CONFIG_VAR_BOOTSTRAP_BASE_DIR);
                File oldPath = new File(home + "/" + SetupConstants.CONFIG_VAR_BOOTSTRAP_LEGACY_BASE_DIR);

                return (oldPath.exists() && !newPath.exists() ? oldPath.getPath() : newPath.getPath())
                        + "/" + SetupConstants.CONFIG_VAR_BOOTSTRAP_BASE_PREFIX + path;
            } else {
                throw new ConfiguratorException(
                        "Cannot read the bootstrap path");
            }
        } else {
            return null;
        }
    }
    
    public static String getBaseDir()
        throws ConfiguratorException {
        String configDir = getPresetConfigDir();
        if ((configDir != null) && (configDir.length() > 0)) {
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
                throw new ConfiguratorException(
                    "Cannot read the bootstrap path");
            }
        } else {
            throw new ConfiguratorException("Servlet Context is null");
        }
    }

    public static String getRealPath(String path) {
        return servletCtx.getRealPath(path);
    }

    public static String getNormalizedRealPath(ServletContext servletCtx) {
        String path = null;
        if (servletCtx != null) {
            path = getAppResource(servletCtx);
            
            if (path != null) {
                String realPath = servletCtx.getRealPath("/");
                if ((realPath != null) && (realPath.length() > 0)) {
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
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "AMSetupServlet.getAppResource: Cannot access the resource",
                    mue);
            }
        } else {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.getAppResource: Context is null");
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
    private static void processDataRequests(String xmlBaseDir)
        throws SMSException, SSOException, IOException, PolicyException
    {
        SetupProgress.reportStart("configurator.progress.configure.system",
            null);
        SSOToken ssoToken = getAdminSSOToken();
        try {
            Map map = ServicesDefaultValues.getDefaultValues();
            String hostname = (String)map.get(
                SetupConstants.CONFIG_VAR_SERVER_HOST);
            ConfigureData configData = new ConfigureData(
                xmlBaseDir, servletCtx, hostname, ssoToken);
            configData.configure();
            SetupProgress.reportEnd("emb.done", null);
        } catch (SMSException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.processDataRequests", e);
            throw e;
        } catch (SSOException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.processDataRequests", e);
            throw e;
        } catch (IOException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.processDataRequests", e);
            throw e;
        }
    }

    /**
     * Helper method to return Admin token
     * @return Admin Token
     */
    private static SSOToken getAdminSSOToken() {
        if (adminToken == null) {
            adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        }
        return adminToken;
    }

    /**
     * Initialize AMConfig.properties with host specific values
     */
    private static Map initializeConfigProperties()
        throws SecurityException, IOException {
        Map mapFileNameToContent = new HashMap();
        List dataFiles = getTagSwapConfigFiles();
        
        Map map = ServicesDefaultValues.getDefaultValues();
        String basedir = (String)map.get(
            SetupConstants.CONFIG_VAR_BASE_DIR);
       
        String deployuri =
            (String)map.get(SetupConstants.CONFIG_VAR_SERVER_URI);
        try {
            File fhm = new File(basedir + deployuri + "/" + SMS_STR);
            fhm.mkdirs();
        } catch (SecurityException e){
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.initializeConfigProperties", e);
            throw e;
        }

        // this is for the servicetag-registry.xml stuff, a bit later
        String version = (String)map.get(SetupConstants.VERSION);
        boolean isEnterprise = version.contains("Enterprise");
        String libRegDir = basedir + "/" + deployuri + "/lib/registration";
        File reglibDir = new File(libRegDir);
        
        for (Iterator i = dataFiles.iterator(); i.hasNext(); ) {
            String file = (String)i.next();
            StringBuffer sbuf = null;
    
            /*
             * if the file's not there, just skip it
             * usually will be about a file included with OpenSSO,
             * so it's informational, rather than a "real" error.
             */
            try {
                sbuf = readFile(file);
            } catch (IOException ioex) {
                break;
            }
            
            int idx = file.lastIndexOf("/");
            String absFile = (idx != -1) ? file.substring(idx+1) : file;
            
            if (absFile.equalsIgnoreCase(SetupConstants.AMCONFIG_PROPERTIES)) {
                String dbOption = 
                    (String)map.get(SetupConstants.CONFIG_VAR_DATA_STORE);
                boolean embedded = 
                    dbOption.equals(SetupConstants.SMS_EMBED_DATASTORE);
                boolean dbSunDS = false;
                boolean dbMsAD  = false;
                if (embedded) {
                    dbSunDS = true;
                } else { // Keep old behavior for now.
                    dbSunDS = dbOption.equals(SetupConstants.SMS_DS_DATASTORE);
                    dbMsAD  = dbOption.equals(SetupConstants.SMS_AD_DATASTORE);
                }

                if (dbSunDS || dbMsAD) {
                    int idx1 = sbuf.indexOf(
                        SetupConstants.CONFIG_VAR_SMS_DATASTORE_CLASS);
                    if (idx1 != -1) {
                        String dataStoreClass = embedded ?
                            SetupConstants.CONFIG_VAR_EMBEDDED_DATASTORE_CLASS:
                            SetupConstants.CONFIG_VAR_DS_DATASTORE_CLASS;
                        sbuf.replace(idx1, idx1 +
                            (SetupConstants.CONFIG_VAR_SMS_DATASTORE_CLASS)
                            .length(), dataStoreClass);
                    }
                }
            }

            /*
             *  if Linux, the default PAM service name is "password",
             *  rather than "other"
             */
            if (determineOS().equals(SetupConstants.LINUX)) {
                map.put(SetupConstants.PAM_SERVICE_NAME,
                        SetupConstants.LINUX_PAM_SVC_NAME);
            }
            
            String swapped = ServicesDefaultValues.tagSwap(sbuf.toString(),
                file.endsWith("xml"));
            
            if (absFile.equalsIgnoreCase(SetupConstants.AMCONFIG_PROPERTIES) ||
                absFile.equalsIgnoreCase(SystemProperties.CONFIG_FILE_NAME)
            ) {
                mapFileNameToContent.put(absFile, swapped);
            } else if (absFile.equalsIgnoreCase(
                    SetupConstants.SECURID_PROPERTIES))
            {
                writeToFile(basedir + deployuri + "/auth/ace/data/" + absFile,
                    swapped);
            } else {
                writeToFile(basedir + "/" + absFile, swapped);
            }
        }
        return mapFileNameToContent;
    }
    
    public static StringBuffer readFile(String file)
        throws IOException {
        InputStreamReader fin = null;
        StringBuffer sbuf = new StringBuffer();
        InputStream is = null;
        
        try {
            if ((is = getResourceAsStream(servletCtx, file)) == null) {
                throw new IOException(file + " not found");
            }
            fin = new InputStreamReader(is);
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (Exception ex) {
                    //No handling required
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                    // no handling required
                }
            }
        }
        return sbuf;
    }
    
    private static void writeToFileEx(String fileName, String content)
        throws IOException {    
        File btsFile = new File(fileName);
        if (!btsFile.getParentFile().exists()) {
            btsFile.getParentFile().mkdirs();
        }
        writeToFile(fileName, content);
    }
    
    public static void writeToFile(String fileName, String content) 
        throws IOException {
        FileWriter fout = null;
        try {
            fout = new FileWriter(fileName);
            fout.write(content);
        } catch (IOException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.writeToFile", e);
            throw e;
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
     * Returns secure random string.
     *
     * @return secure random string.
     */
    public static String getRandomString() {
        String randomStr = null;
        try {
            byte [] bytes = new byte[24];
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.nextBytes(bytes);
            randomStr = Base64.encode(bytes).trim();
        } catch (Exception e) {
            randomStr = null;
            Debug.getInstance(SetupConstants.DEBUG_NAME).message(
                "AMSetupServlet.getRandomString:" +
                "Exception in generating encryption key.", e);
        }
        return (randomStr != null) ? randomStr : 
            SetupConstants.CONFIG_VAR_DEFAULT_SHARED_KEY;
    }

    /**
      * Returns a unused port on a given host.
      *    @param hostname (eg localhost)
      *    @param start starting port number to check (eg 389).
      *    @param incr port number increments to check (eg 1000).
      *    @return available port num if found. -1 of not found.
      */
    static public int getUnusedPort(String hostname, int start, int incr)
    {
        int defaultPort = -1;
        for (int i=start;i<65500 && (defaultPort == -1);i+=incr) {
            if (canUseAsPort(hostname, i))
            {
                defaultPort = i;
            }
        }
        return defaultPort;
    }
    
    /**
      * Checks whether the given host:port is currenly under use.
      *    @param hostname (eg localhost)
      *    @param port port number.
      *    @return  true if not in use, false if in use.
      */
    public static boolean canUseAsPort(String hostname, int port)
    {
        boolean canUseAsPort = false;
        ServerSocket serverSocket = null;
        try {
            InetSocketAddress socketAddress =
                new InetSocketAddress(hostname, port);
            serverSocket = new ServerSocket();
            //if (!isWindows()) {
              //serverSocket.setReuseAddress(true);
            //}
            serverSocket.bind(socketAddress);
            canUseAsPort = true;
     
            serverSocket.close();
       
            Socket s = null;
            try {
              s = new Socket();
              s.connect(socketAddress, 1000);
              canUseAsPort = false;
       
            } catch (Throwable t) {
            }
            finally {
              if (s != null) {
                try {
                  s.close();
                } catch (Throwable t)
                {
                }
              }
            }
     
     
        } catch (IOException ex) {
          canUseAsPort = false;
        } catch (NullPointerException ne) {      
           canUseAsPort = false;  
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
            }
            } catch (Exception ex) { }
        }
     
        return canUseAsPort;
    }

    /**
      * Obtains misc config data from a remote OpenSSO server :
      *     opends admin port
      *     config basedn
      *     flag to indicate replication is already on or not
      *     opends replication port or opends sugested port
      *
      * @param server URL string representing the remote OpenSSO
      *        server
      * @param userid - admin userid on remote server (only amdmin)
      * @param pwd - admin password
      * @return Map of confif params.
      * @throws <code>ConfiguratonException</code>
      *   errorCodes :
      *     400=Bad Request - userid/passwd param missing
      *     401=Unauthorized - invalid credentials
      *     405=Method Not Allowed - only POST is honored
      *     408=Request Timeout - requested timed out
      *     500=Internal Server Error 
      *     701=File Not Found - incorrect deployuri/server
      *     702=Connection Error - failed to connect
      */
    public static Map getRemoteServerInfo(
        String server,
        String userid,
        String pwd
    ) throws ConfigurationException {
        HttpURLConnection conn = null;
        try {
            // Construct data
            String data = "IDToken1=" + URLEncoder.encode(userid, "UTF-8")+
                          "&IDToken2="+ URLEncoder.encode(pwd, "UTF-8");
            // Send data
            URL url = new URL(server + "/getServerInfo.jsp");
            
            conn = (HttpURLConnection) url.openConnection();
            
            if (url.getProtocol().equals(HTTPS)) {
                HttpsURLConnection sslConn = (HttpsURLConnection) conn;
                sslConn.setHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            }
            
            conn.setDoOutput(true);
            OutputStreamWriter wr = 
                       new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();

            BufferedReader rd = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            String line;
            Map info = null;
            while ((line = rd.readLine()) != null) {
                if (line.length() == 0)
                    continue;
                info = BootstrapData.queryStringToMap(line);
            }
            wr.close();
            rd.close();
            return info;
        } catch (javax.net.ssl.SSLHandshakeException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).warning(
                    "AMSetupServlet.getRemoteServerInfo()", e);
            throw new ConfiguratorException("702", null, 
                         java.util.Locale.getDefault());
        } catch (IllegalArgumentException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).warning(
                    "AMSetupServlet.getRemoteServerInfo()", e);
            throw new ConfiguratorException("702", null, 
                         java.util.Locale.getDefault());
        } catch (java.net.MalformedURLException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).warning(
                    "AMSetupServlet.getRemoteServerInfo()", e);
            throw new ConfiguratorException("702", null, 
                         java.util.Locale.getDefault());
        } catch (java.net.UnknownHostException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).warning(
                    "AMSetupServlet.getRemoteServerInfo()", e);
            throw new ConfiguratorException("702", null, 
                         java.util.Locale.getDefault());
        } catch (FileNotFoundException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).warning(
                    "AMSetupServlet.getRemoteServerInfo()", e);
            throw new ConfiguratorException("701", null, 
                         java.util.Locale.getDefault());
        } catch (java.net.ConnectException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).warning(
                    "AMSetupServlet.getRemoteServerInfo()", e);
            throw new ConfiguratorException("702", null, 
                         java.util.Locale.getDefault());
        } catch (IOException e) {
            ConfiguratorException cex = null;
            int status = 0;
            if (conn != null) {
                try {
                   status = conn.getResponseCode();
                } catch (Exception ig) {}
            }
  
            if (status == 401 || status == 400 || status == 405 ||
                status == 408 ) {
                 cex = new ConfiguratorException(""+status, null, 
                         java.util.Locale.getDefault());
            } else  {
                 cex = new ConfiguratorException(e.getMessage());
            }
            Debug.getInstance(SetupConstants.DEBUG_NAME).warning(
                    "AMSetupServlet.getRemoteServerInfo()", e);
            throw cex;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Returns schema file names.
     *
     * @param dataStore Name of data store configuration data.
     * @throws MissingResourceException if the bundle cannot be found.
     */
    private static List getSchemaFiles(String dataStore)
        throws MissingResourceException
    {
        List fileNames = new ArrayList();
        ResourceBundle rb = ResourceBundle.getBundle(
            SetupConstants.SCHEMA_PROPERTY_FILENAME);
        String strFiles;

        boolean embedded = dataStore.equals(SetupConstants.SMS_EMBED_DATASTORE);
        if (embedded) {
            strFiles = rb.getString(
                SetupConstants.OPENDS_SMS_PROPERTY_FILENAME); 
        } else {
            strFiles = rb.getString(
                SetupConstants.DS_SMS_PROPERTY_FILENAME);
        }
        
        StringTokenizer st = new StringTokenizer(strFiles);
        while (st.hasMoreTokens()) {
            fileNames.add(st.nextToken());
        }
        return fileNames;
    }

    private static List getTagSwapConfigFiles()
        throws MissingResourceException
    {
        List fileNames = new ArrayList();
        ResourceBundle rb = ResourceBundle.getBundle("configuratorTagSwap");
        String strFiles = rb.getString("tagswap.files");
        StringTokenizer st = new StringTokenizer(strFiles);
        while (st.hasMoreTokens()) {
            fileNames.add(st.nextToken());
        }
        return fileNames;
    }


    private static boolean isIPAddress(String hostname) {
        StringTokenizer st = new StringTokenizer(hostname, ".");
        boolean isIPAddr = (st.countTokens() == 4);
        if (isIPAddr) {
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                try {
                    int node = Integer.parseInt(token);
                    isIPAddr = (node >= 0) && (node < 256);
                } catch (NumberFormatException e) {
                    isIPAddr = false;
                }
            }
        }
        return isIPAddr;
    }

    /**
     * Tag swaps strings in schema files.
     *
     * @param basedir the configuration base directory.
     * @param schemaFiles List of schema files to be loaded.
     * @throws IOException if data files cannot be written.
     */
    private static void writeSchemaFiles(
        String basedir,
        List schemaFiles,
        Map map,
        String dataStore
    )  throws Exception {
        SetupProgress.reportStart("configurator.progress.tagswap.schemafiles", 
            null);
        Set absSchemaFiles = new HashSet();
        for (Iterator i = schemaFiles.iterator(); i.hasNext(); ) {
            String file = (String)i.next();
            String content = readFile(file).toString();
            FileWriter fout = null;
            
            try {
                int idx = file.lastIndexOf("/");
                String absFile = basedir + "/" + 
                    ((idx != -1) ? file.substring(idx+1) : file);
                fout = new FileWriter(absFile);
                absSchemaFiles.add(absFile);
                fout.write(ServicesDefaultValues.tagSwap(content));
            } catch (IOException ioex) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "AMSetupServlet.writeSchemaFiles: " +
                    "Exception in writing schema files:" , ioex);
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
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "AMSetupServlet.writeSchemaFiles: " +
                    "Unable to rebuild indexes in OpenDJ: " + ret);
                throw new Exception("Unable to rebuild indexes in OpenDJ: " + ret);
            }
        }

        for(Iterator iter = absSchemaFiles.iterator(); iter.hasNext(); ) {
            File file = new File((String)iter.next());
            file.delete();
        }
    }

    /**
     * Update famsts.wsdl with Keystore location.
     *
     * @param basedir the configuration base directory.
     * @param deployuri the deployment URI. 
     * @throws IOException if password files cannot be written.
     */
    private static void updateSTSwsdl(
        String basedir, 
        String deployuri 
    ) throws IOException {
        // Get OpenSSO web application base location.
        URL url = servletCtx.getResource("/WEB-INF/lib/opensso.jar");
        // TODO: Needs to be Fixed...JAR Name not valid!
        String webAppLocation = (url.toString()).substring(5);
        int index = webAppLocation.indexOf("WEB-INF");
        webAppLocation = webAppLocation.substring(0, index-1);
        
        // Update famsts.wsdl with Keystore location.
        String contentWSDL = readFile("/WEB-INF/wsdl/famsts.wsdl").toString();
        contentWSDL = StringUtils.strReplaceAll(contentWSDL,
            "@KEYSTORE_LOCATION@", basedir + deployuri);
        BufferedWriter outWSDL = 
            new BufferedWriter(new FileWriter(webAppLocation +
            "/WEB-INF/wsdl/famsts.wsdl"));
        outWSDL.write(contentWSDL);
        outWSDL.close();
    }
    
    /**
     * Create the storepass and keypass files
     *
     * @param basedir the configuration base directory.
     * @param deployuri the deployment URI. 
     * @throws IOException if password files cannot be written.
     */
    private static void createPasswordFiles(
        String basedir, 
        String deployuri 
    ) throws IOException
    {
        String pwd = Crypt.encrypt("changeit");
        String location = basedir + deployuri;
        writeContent(location + "/.keypass", pwd);
        writeContent(location + "/.storepass", pwd);
        copyCtxFile("/WEB-INF/template/keystore", "keystore.jks", location);
    }

    /**
     * Helper method to create the storepass and keypass files
     *
     * @param fName is the name of the file to create.
     * @param content is the password to write in the file.
     */
    private static void writeContent(String fName, String content)
        throws IOException
    {
        FileWriter fout = null;
        try {
            fout = new FileWriter(new File(fName));
            fout.write(content);
        } catch (IOException ioex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.writeContent: " +
                "Exception in creating password files:" , ioex);
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
      * Update Embedded Idrepo instance with new embedded opends isntance.
      */
    private static void  updateEmbeddedIdRepo(
        String orgName, 
        String configName, 
        String entry
    ) throws SMSException, SSOException {
        SSOToken token = (SSOToken)
            AccessController.doPrivileged(AdminTokenAction.getInstance());
        ServiceConfigManager scm = new ServiceConfigManager(token,
            IdConstants.REPO_SERVICE, "1.0");
        ServiceConfig sc = scm.getOrganizationConfig(orgName, null);
        if (sc != null) {
            ServiceConfig subConfig = sc.getSubConfig(configName);
            if (subConfig != null) {
                Map configMap = subConfig.getAttributes();
                Set vals = (Set)configMap.get(
                    "sun-idrepo-ldapv3-config-ldap-server");
                vals.add(entry);
                HashMap mp = new HashMap(2);
                mp.put("sun-idrepo-ldapv3-config-ldap-server", vals);
                subConfig.setAttributes(mp);
            }
        }
    }

    /**
     * Update platform server list and Organization alias
     */
    private static void updatePlatformServerList(
        String serverURL,
        String hostName
    ) throws SMSException, SSOException 
    {
        SSOToken token = getAdminSSOToken();
        ServiceSchemaManager ssm = new ServiceSchemaManager(
            "iPlanetAMPlatformService", token);
        ServiceSchema ss = ssm.getGlobalSchema();
        AttributeSchema as = ss.getAttributeSchema(
            "iplanet-am-platform-server-list");
        Set values = as.getDefaultValues();
        if (!isInPlatformList(values, serverURL)) {
            String instanceName = getNextAvailableServerId(values);
            values.add(serverURL + "|" + instanceName);
            as.setDefaultValues(values);

            // Update Organization Aliases
            OrganizationConfigManager ocm =
                new OrganizationConfigManager(token, "/");
        
            Map attrs = ocm.getAttributes("sunIdentityRepositoryService");
            Set origValues = (Set)attrs.get("sunOrganizationAliases");
            if (!origValues.contains(hostName)) {
                values = new HashSet();
                values.add(hostName);
                ocm.addAttributeValues("sunIdentityRepositoryService",
                    "sunOrganizationAliases", values);
            }
        }
    }

    private static String getNextAvailableServerId(Set values) {
        int instanceNumber = 1;
        int maxNumber = 1;

        for (Iterator items = values.iterator(); items.hasNext();) {
            String item = (String) items.next();
            int index1 = item.indexOf('|');

            if (index1 != -1) {
                int index2 = item.indexOf('|', index1 + 1);
                item = (index2 == -1) ? item.substring(index1 + 1) :
                    item.substring(index1 + 1, index2);

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
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                "AgentService", token);
            return (ssm != null);
        } catch (SSOException ex) {
            return false;
        } catch (SMSException ex) {
            return false;
        }
    }

    private static void createDemoUser() 
        throws IdRepoException, SSOException {
        SetupProgress.reportStart("configurator.progress.create.demo.user", 
            null);
        Map attributes = new HashMap();
        Set setSN = new HashSet(2);
        setSN.add("demo");
        attributes.put("sn", setSN);
        Set setCN = new HashSet(2);
        setCN.add("demo");
        attributes.put("cn", setCN);
        Set setPwd = new HashSet(2);
        setPwd.add("changeit");
        attributes.put("userpassword", setPwd);
        Set setStatus = new HashSet(2);
        setStatus.add("Active");
        attributes.put("inetuserstatus", setStatus);
        try {
            AMIdentityRepository amir = new AMIdentityRepository(
                getAdminSSOToken(), "/");
            amir.createIdentity(IdType.USER, "demo", attributes);
        } catch (IdRepoException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.createDemoUser", e);
            throw e;
        } catch (SSOException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.createDemoUser", e);
            throw e;
        }
        SetupProgress.reportEnd("emb.done", null);
    }

    /**
     * Creates Identities for WS Security
     *
     * @param serverURL URL at which Access Manager is configured.
     */
    private static void createIdentitiesForWSSecurity(
        String serverURL,
        String deployuri
    ) throws IdRepoException, SSOException {
        SSOToken token = getAdminSSOToken();
        
        if (!isAgentServiceLoad(token)) {
            return;
        }
        
        SetupProgress.reportStart("configurator.progress.create.wss.agents",
            null);
        AMIdentityRepository idrepo = new AMIdentityRepository(token, "/");
        //createUser(idrepo, "jsmith", "John", "Smith");
        //createUser(idrepo, "jondoe", "Jon", "Doe");
        Map config = new HashMap();

        // Add WSC configuration
        config.put("sunIdentityServerDeviceStatus","Active");
        config.put("SecurityMech","urn:sun:wss:security:null:UserNameToken");
        config.put("UserCredential","UserName:test|UserPassword:test");
        config.put("useDefaultStore","true");
        config.put("privateKeyAlias","test");
        config.put("publicKeyAlias","test");
        config.put("isRequestSign","true");
        config.put("keepSecurityHeaders","true");
        config.put("WSPEndpoint","default");
        config.put("EncryptionAlgorithm","AES");
        config.put("EncryptionStrength","128");
        config.put("SigningRefType","DirectReference");
        config.put("DnsClaim","wsc");
        config.put("SignedElements","Body,SecurityToken,Timestamp,To,From," +
                   "ReplyTo,Action,MessageID");
        config.put("AgentType","WSCAgent");
        createAgent(token, idrepo, "wsc", "wsc", "WSC", "", config);

        // Add WSP configuration
        config.remove("AgentType");
        config.put("AgentType","WSPAgent");
        config.put("DetectMessageReplay","true");
        config.put("DetectUserTokenReplay","true");
        config.remove("SecurityMech");
        config.put("SecurityMech","urn:sun:wss:security:null:UserNameToken," +
                                  "urn:sun:wss:security:null:SAML2Token-HK," +
                                  "urn:sun:wss:security:null:SAML2Token-SV," +
                                  "urn:sun:wss:security:null:X509Token");
        config.remove("DnsClaim");
        config.put("DnsClaim","wsp");
        createAgent(token, idrepo, "wsp", "wsp", "WSP", "", config);
        config.remove("keepSecurityHeaders");        

        // Add STS Client configuration
        config.remove("AgentType");
        config.put("DnsClaim","wsc");
        config.put("AgentType","STSAgent");
        config.remove("SecurityMech");
        config.remove("keepSecurityHeaders");
        config.remove("DetectMessageReplay");
        config.remove("DetectUserTokenReplay");
        config.remove("WSPEndpoint");
        config.put("SecurityMech","urn:sun:wss:security:null:X509Token");
        config.put("STSEndpoint",serverURL + deployuri + "/sts");
        config.put("STSMexEndpoint",serverURL + deployuri + "/sts/mex");
        config.put("WSTrustVersion", "1.3");
        config.put("KeyType", "PublicKey");
        //createAgent(idrepo, "defaultSTS", "STS", "", config);
        createAgent(token, idrepo, "SecurityTokenService", 
            "SecurityTokenService", "STS", "", config);
        config.remove("KeyType");

        // Add Agent Authenticator configuration
        Map configAgentAuth = new HashMap();
        configAgentAuth.put("AgentType","SharedAgent");
        configAgentAuth.put("sunIdentityServerDeviceStatus","Active");
        configAgentAuth.put("AgentsAllowedToRead",
            "wsc,wsp,SecurityTokenService");
        createAgent(token, idrepo, "agentAuth", "changeit", 
            "Agent_Authenticator", "", configAgentAuth);
        
        /*
        // Add UsernameToken profile
        createAgent(idrepo, "UserNameToken", "WSP",
            "WS-I BSP UserName Token Profile Configuration", config);

        // Add SAML-HolderOfKey
        config.remove("SecurityMech");
        config.remove("UserCredential");
        config.put("SecurityMech","urn:sun:wss:security:null:SAMLToken-HK");
        createAgent(idrepo, "SAML-HolderOfKey", "WSP",
            "WS-I BSP SAML Holder Of Key Profile Configuration", config);

        // Add SAML-SenderVouches
        config.remove("SecurityMech");
        config.put("SecurityMech","urn:sun:wss:security:null:SAMLToken-SV");
        createAgent(idrepo, "SAML-SenderVouches", "WSP",
            "WS-I BSP SAML Sender Vouches Token Profile Configuration", config);

        // Add X509Token
        config.remove("SecurityMech");
        config.put("SecurityMech","urn:sun:wss:security:null:X509Token");
        createAgent(idrepo, "X509Token", "WSP",
            "WS-I BSP X509 Token Profile Configuration", config);

        // Add LibertyX509Token
        config.put("TrustAuthority","LocalDisco");
        config.put("WSPEndpoint","http://wsp.com");
        config.remove("SecurityMech");
        config.put("SecurityMech","urn:liberty:security:2005-02:null:X509");
        createAgent(idrepo, "LibertyX509Token", "WSP",
            "Liberty X509 Token Profile Configuration", config);

        // Add LibertyBearerToken
        config.remove("SecurityMech");
        config.put("SecurityMech","urn:liberty:security:2005-02:null:Bearer");
        createAgent(idrepo, "LibertyBearerToken", "WSP",
            "Liberty SAML Bearer Token Profile Configuration", config);

        // Add LibertySAMLToken
        config.remove("SecurityMech");
        config.put("SecurityMech","urn:liberty:security:2005-02:null:SAML");
        createAgent(idrepo, "LibertySAMLToken", "WSP",
            "Liberty SAML Token Profile Configuration", config);

        // Add local discovery service
        config.clear();
        config.put("AgentType","DiscoveryAgent");
        config.put("Endpoint", serverURL + deployuri + "/Liberty/disco");
        createAgent(idrepo, "LocalDisco", "Discovery",
            "Local Liberty Discovery Service Configuration", config);*/
        SetupProgress.reportEnd("emb.done", null);
    }

    private static void createUser(
        AMIdentityRepository idrepo,
        String uid,
        String gn,
        String sn
    ) throws IdRepoException, SSOException 
    {
        Map attributes = new HashMap();
        Set values = new HashSet();
        values.add(uid);
        attributes.put("uid", values);
        values = new HashSet();
        values.add(gn);
        attributes.put("givenname", values);
        values = new HashSet();
        values.add(sn);
        attributes.put("sn", values);
        values = new HashSet();
        values.add(gn + " " + sn);
        attributes.put("cn", values);
        values = new HashSet();
        values.add(uid);
        attributes.put("userPassword", values);
        AMIdentity id = idrepo.createIdentity(IdType.USER, uid, attributes);
        id.assignService("sunIdentityServerDiscoveryService",
            Collections.EMPTY_MAP);
    }


    private static void createAgent(
        SSOToken adminToken,
        AMIdentityRepository idrepo,
        String name,
        String password,
        String type,
        String desc,
        Map config
    ) throws IdRepoException, SSOException {
        AMIdentity amid = new AMIdentity(adminToken, name, IdType.AGENTONLY, 
            "/", null);
        if (!amid.isExists()) {
            Map attributes = new HashMap();

            Set values = new HashSet();
            values.add(password);
            attributes.put("userpassword", values);

            for (Iterator i = config.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                String value = (String) config.get(key);
                values = new HashSet();
                if (value.indexOf(",") != -1) {
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

    private static void createMonitoringAuthFile(String basedir,
        String deployuri)
    {
        SetupProgress.reportStart(
            "configurator.progress.setup.monitorauthfile", null);
        /*
         *  make sure the basedir + "/" + deployuri + "/lib/registration"
         *  directory exists, and then create the monitoring auth file
         *  there.
         */
        String monAuthFile = basedir + "/" + deployuri + "/openam_mon_auth";
        String encpwd =
            (String)AccessController.doPrivileged(new EncodeAction("changeit"));
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
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.createMonitoringAuthFile:" +
                "failed to create monitoring authentication file");
            SetupProgress.reportEnd("emb.failed", null);
        }
    }

    private static void setupSecurIDDirs(String basedir, String deployuri) {
        /*
         *  make sure the basedir + "/" + deployuri + "/auth/ace/data"
         *  directory exists.
         */

        String aceDataDir = basedir + "/" + deployuri + "/auth/ace/data";
        File dataAceDir = new File(aceDataDir);
        if (!dataAceDir.mkdirs()) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.setupSecurIDDirs: " +
                "failed to create SecurID data directory");
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
        String [] propFiles = {"log4j.properties"};
        try {
            for (int i = 0; i < propFiles.length; i++) {
                copyCtxFile ("/WEB-INF/classes", propFiles[i], destDir);
            }
        } catch (IOException ioex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.copyAuthSecurIDFiles:", ioex);
        }
    }

    private static boolean copyCtxFile (String srcDir, String file,
        String destDir) throws IOException
    {
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
    
    private static void initDSConfigMgr(String str) 
        throws LDAPServiceException {
        ByteArrayInputStream bis = null;
        try {
            bis = new ByteArrayInputStream(str.getBytes());
            DSConfigMgr.initInstance(bis);
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

    private static String mapToString(Map map) {
        StringBuilder buff = new StringBuilder();
        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            buff.append(key).append("=").append((String)map.get(key))
                .append("\n");
        }
        return buff.toString();
    }

    private static String determineOS() {
        String OS_ARCH = System.getProperty("os.arch");
        String OS_NAME = System.getProperty("os.name");
        if ((OS_ARCH.toLowerCase().indexOf(SetupConstants.X86) >= 0) ||
                (OS_ARCH.toLowerCase().indexOf(SetupConstants.AMD) >= 0)){
            if (OS_NAME.toLowerCase().indexOf(SetupConstants.WINDOWS) >= 0) {
                return SetupConstants.WINDOWS;
            } else {
                if (OS_NAME.toLowerCase().indexOf(SetupConstants.SUNOS) >= 0) {
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
        Map map = request.getParameterMap();
        String superLocale = (String)map.get("locale");
        if ((superLocale != null) && (superLocale.length() > 0)) {
            configLocale = Locale.getLocaleObjFromAcceptLangHeader(
                superLocale);
        } else {
            String acceptLangHeader = request.getHeader("Accept-Language");
            if ((acceptLangHeader !=  null) &&
                (acceptLangHeader.length() > 0))
            {
                configLocale = Locale.getLocaleObjFromAcceptLangHeader(
                   acceptLangHeader);
            } else {
                configLocale = java.util.Locale.getDefault();
            }
        }
        SetupProgress.setLocale(configLocale);
    }
    
    private static void createDotVersionFile(String basedir)
        throws IOException {
        String version = SystemProperties.get(Constants.AM_VERSION);
        writeToFile(basedir + "/.version", version);
    }
    
    private static boolean isEmbeddedDS() {
        return (new File(getBaseDir() + OPENDS_DIR)).exists();     
    }

    /**
     * Synchronizes embedded replication state with current server list.
     * @returns boolean true is sync succeeds else false.
     */
    private static boolean syncServerInfoWithRelication() {
        // We need to execute syn only if we are in Embedded mode
        if (!isEmbeddedDS()) {
            return true;
        }

        try {
            if (getAdminSSOToken() == null) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.syncServerInfoWithRelication: "+
                     "Could not sync servers with embedded replication:"+
                     "no admin token");
                return false;
            }
            // Determine which server this is
            String myName = WebtopNaming.getLocalServer();

            // See if we need to execute sync

            // Check if we are already replication with other servers
            Properties props = ServerConfiguration.getServerInstance
                                                 (adminToken, myName);
            String syncFlag = props.getProperty(
                                    Constants.EMBED_SYNC_SERVERS);
            if ("off".equals(syncFlag)) {
                return true;
            }
            String myReplPort = props.getProperty(
                                    Constants.EMBED_REPL_PORT);
            String myDSPort = null;
            // Get server list
            Set serverSet = ServerConfiguration.getServers(adminToken);
            if (serverSet == null) { 
                return true;
            }

            String dsAdminPort = props.getProperty(Constants.DS_ADMIN_PORT);

            Set currServerSet = new HashSet();
            Set currServerDSSet = new HashSet();
            Set currServerDSAdminPortsSet = new HashSet();

            for (String sname : (Set<String>) serverSet) {
                Properties p = ServerConfiguration.getServerInstance(
                                   adminToken, sname);
                String hname = p.getProperty(Constants.AM_SERVER_HOST);
                String rPort = p.getProperty(Constants.EMBED_REPL_PORT);
                currServerSet.add(hname + ":" + rPort);
                ServerGroup sg = getSMSServerGroup(sname); 
                currServerDSSet.add(hname + ":" + getSMSPort(sg));
                currServerDSAdminPortsSet.add(hname + ":" + p.getProperty(Constants.DS_ADMIN_PORT));
            }

            ServerGroup sGroup = getSMSServerGroup(myName); 
            boolean stats = EmbeddedOpenDS.syncReplicatedServers(
                  currServerSet, dsAdminPort, getSMSPassword(sGroup));
            boolean statd = EmbeddedOpenDS.syncReplicatedDomains(
                  currServerSet, dsAdminPort, getSMSPassword(sGroup));
            boolean statl = EmbeddedOpenDS.syncReplicatedServerList(
                  currServerDSAdminPortsSet, getSMSPort(sGroup), getSMSPassword(sGroup));
            return stats || statd || statl;
        } catch (Exception ex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.syncServerInfoWithRelication: "+
                 "Could not sync servers with embedded replication:", ex);
            return false;
        }
    }

    /**
     * Gets <code>ServerGroup</code> for SMS for specified server
     * @param sname servername of groupo to find.
     * @returns <code>ServerGroup</code> instance
     */
    private static ServerGroup getSMSServerGroup(String sname) 
        throws Exception
    {
        String xml = 
            ServerConfiguration.getServerConfigXML(adminToken, sname);
        ServerConfigXML scc = new ServerConfigXML(xml);
        return scc.getSMSServerGroup() ;
    }
    /**
     * Gets clear password of SMS datastore
     * @param ssg <code>ServerGroup</code> instance representing SMS
     * or Configuration datastore.
     * @returns clear password
     */
    private static String getSMSPassword(ServerGroup ssg) throws 
        Exception
    {
        DirUserObject sduo = (DirUserObject) ssg.dsUsers.get(0);
        String epass = sduo.password;
        String pass = (String) AccessController.doPrivileged(
            new DecodeAction(epass));
        return pass;
    }
    /**
     * Gets port number of SMS datastore
     * @param ssg <code>ServerGroup</code> instance representing SMS
     * or Configuration datastore.
     * @returns port
     */
    private static String getSMSPort(ServerGroup ssg) throws Exception
    {
        ServerObject sobj = (ServerObject) ssg.hosts.get(0);
        return sobj.port;
    }

    private static void updateReplPortInfo(Map map)
    {
        try {
            String instanceName = WebtopNaming.getLocalServer();
            Map newValues = new HashMap();
            // Update this instance first...
            newValues.put("com.sun.embedded.replicationport", 
                (String) map.get(
                    SetupConstants.DS_EMB_REPL_REPLPORT1));
            ServerConfiguration.setServerInstance(
                    getAdminSSOToken(),
                    instanceName,
                    newValues);

            // Update remote instance
            instanceName = (String) map.get(
                SetupConstants.DS_EMB_EXISTING_SERVERID);
            newValues.put("com.sun.embedded.replicationport", 
                (String) map.get(
                    SetupConstants.DS_EMB_REPL_REPLPORT2));
            // Update remote instance ...
            ServerConfiguration.setServerInstance(
                    getAdminSSOToken(),
                    instanceName,
                    newValues);
        } catch (Exception ex ) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "AMSetupServlet.updateReplPortInfo: "+
                "could not add replication port info to SM", ex);
        }
    }

    public static InputStream getResourceAsStream(ServletContext 
        servletContext, String file) {

        if (servletContext == null) {
            // remove leading '/'
            file = file.substring(1);
            return Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(file);
        } else {
            return servletContext.getResourceAsStream(file);
        }
    }

    // Method to convert the domain name to the root suffix.
    // eg., Domain Name amqa.test.com is converted to root suffix
    // DC=amqa,DC=test,DC=com
    static String dnsDomainToDN(String domainName) {
        StringBuilder buf = new StringBuilder();
        for (String token : domainName.split("\\.")) {
            if(token.length()==0)   continue;
            if(buf.length()>0)  buf.append(",");
            buf.append("DC=").append(token);
        }
        return buf.toString();
    }

    // Method to get hostname and port number with the
    // provided Domain Name for Active Directory user data store.
    private String[] getLdapHostAndPort(String domainName)
        throws NamingException, IOException {
        if (!domainName.endsWith(".")) {
            domainName+='.';
        }
        DirContext ictx = null;
        // Check if domain name is a valid one.
        // The resource record type A is defined in RFC 1035.
        try {
            Hashtable env = new Hashtable();
            env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.dns.DnsContextFactory");
            ictx = new InitialDirContext(env);
            Attributes attributes =
                ictx.getAttributes(domainName, new String[]{"A"});
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
        String serverHostName = null;
        String serverPortStr = null;
        final String ldapServer = "_ldap._tcp." + domainName;
        try {
            // Attempting to resolve ldapServer to SRV record.
            // This is a mechanism defined in MSDN, querying
            // SRV records for _ldap._tcp.DOMAINNAME.
            // and get host and port from domain.
            Attributes attributes =
                ictx.getAttributes(ldapServer, new String[]{"SRV"});
            Attribute attr = attributes.get("SRV");
            if (attr == null) {
                throw new NamingException();
            }
            String[] srv = attr.get().toString().split(" ");
            String hostNam = srv[3];
            serverHostName =
                hostNam.substring(0, hostNam.length() -1);
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
        if (isConfiguredFlag) {
            ServiceLoader<SetupListener> listeners = ServiceLoader.load(
                SetupListener.class);
            for (SetupListener p : listeners) {
                p.addListener();
            }
        }
    }
}
