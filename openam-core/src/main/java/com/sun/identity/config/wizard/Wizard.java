/**
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
 * $Id: Wizard.java,v 1.27 2009/01/17 02:05:35 kevinserwin Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2012 ForgeRock AS
 */

package com.sun.identity.config.wizard;

import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.ConfiguratorException;
import com.sun.identity.setup.HttpServletRequestWrapper;
import com.sun.identity.setup.HttpServletResponseWrapper;
import com.sun.identity.setup.SetupConstants;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.shared.Constants;
import org.apache.click.control.ActionLink;

public class Wizard extends AjaxPage implements Constants {

    public int startingTab = 1;

    public ActionLink createConfigLink = 
        new ActionLink("createConfig", this, "createConfig" );
    public ActionLink testUrlLink = 
        new ActionLink("testNewInstanceUrl", this, "testNewInstanceUrl" );
    public ActionLink pushConfigLink = 
        new ActionLink("pushConfig", this, "pushConfig" );
    
    private String cookieDomain = null;
    private String hostName = getHostName();
    private String dataStore = SetupConstants.SMS_EMBED_DATASTORE;
    
    public static String defaultUserName = "cn=Directory Manager";
    public static String defaultPassword = "";
    public static String defaultRootSuffix = DEFAULT_ROOT_SUFFIX;
    public static String defaultSessionRootDN = DEFAULT_SESSION_HA_ROOT_DN;
    public static String defaultSessionStoreType = DEFAULT_SESSION_HA_STORE_TYPE;

    public String defaultPort = Integer.toString(
        AMSetupServlet.getUnusedPort(hostName, 50389, 1000));
    public String defaultAdminPort = Integer.toString(
        AMSetupServlet.getUnusedPort(hostName, 4444, 1000));
    public String defaultJmxPort = Integer.toString(
        AMSetupServlet.getUnusedPort(hostName, 1689, 1000));
    
    /**
     * This is the 'execute' operation for the entire wizard.  This method 
     * aggregates all data submitted across the wizard pages here in one lump 
     * and hands it off to the back-end for processing.
     */
    public boolean createConfig() {
        HttpServletRequest req = getContext().getRequest();
        
        HttpServletRequestWrapper request = 
            new HttpServletRequestWrapper(getContext().getRequest());          
        HttpServletResponseWrapper response =                
            new HttpServletResponseWrapper(getContext().getResponse());        
        initializeResourceBundle();
        
        /* 
         * Get the admin password. use the same value for password and confirm
         * value because they were validated in the input screen
         */
        String adminPassword = (String)getContext().getSessionAttribute(
            SessionAttributeNames.CONFIG_VAR_ADMIN_PWD);
        request.addParameter(
            SetupConstants.CONFIG_VAR_ADMIN_PWD, adminPassword);
        request.addParameter(
            SetupConstants.CONFIG_VAR_CONFIRM_ADMIN_PWD, adminPassword);
            
        /*
         * Get the agent password. same value used for password and confirm
         * because they were validated in the input screen.
         */
        String agentPassword = (String)getContext().getSessionAttribute(
            SessionAttributeNames.CONFIG_VAR_AMLDAPUSERPASSWD);
        request.addParameter(
            SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD, agentPassword);
        request.addParameter(
            SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM, agentPassword);
        
        /* 
         * Set the data store information
         */
        String tmp = getAttribute(
            SetupConstants.CONFIG_VAR_DATA_STORE, 
            SetupConstants.SMS_EMBED_DATASTORE);
        request.addParameter(SetupConstants.CONFIG_VAR_DATA_STORE, tmp);

        boolean isEmbedded = false; 
        if (tmp.equals(SetupConstants.SMS_EMBED_DATASTORE)) {
            tmp = getAttribute("configStoreHost", hostName);
            request.addParameter(
                SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST, tmp);
            request.addParameter(
                SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_SSL, "SIMPLE");   
            
            tmp = getAttribute(SetupConstants.DS_EMB_REPL_FLAG, "false");
            
            /*
             * set the embedded replication information for local host port
             * and remote host port
             */
            isEmbedded = tmp.equals(SetupConstants.DS_EMP_REPL_FLAG_VAL);
            if (isEmbedded) {
                request.addParameter(
                    SetupConstants.DS_EMB_REPL_FLAG,
                    SetupConstants.DS_EMP_REPL_FLAG_VAL);
                
                tmp = getAttribute("localRepPort", "");
                request.addParameter(SetupConstants.DS_EMB_REPL_REPLPORT1, tmp);

                tmp = getAttribute("existingHost", "");
                request.addParameter(SetupConstants.DS_EMB_REPL_HOST2, tmp);

                tmp = getAttribute("existingPort", "");
                request.addParameter(SetupConstants.DS_EMB_REPL_ADMINPORT2, tmp);

                tmp = getAttribute("existingRepPort", "");
                request.addParameter(SetupConstants.DS_EMB_REPL_REPLPORT2, tmp);

                tmp = getAttribute("existingserverid", "");
                request.addParameter(SetupConstants.DS_EMB_EXISTING_SERVERID, 
                                     tmp);
            }
        }

        tmp = getAttribute("configStorePort", defaultPort);
        request.addParameter(
            SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT, tmp);

        tmp = getAttribute("configStoreAdminPort", defaultAdminPort);
        request.addParameter(
            SetupConstants.CONFIG_VAR_DIRECTORY_ADMIN_SERVER_PORT, tmp);

        tmp = getAttribute("configStoreJmxPort", defaultJmxPort);
        request.addParameter(
            SetupConstants.CONFIG_VAR_DIRECTORY_JMX_SERVER_PORT, tmp);

        tmp = getAttribute("rootSuffix", defaultRootSuffix);
        request.addParameter(SetupConstants.CONFIG_VAR_ROOT_SUFFIX, tmp);

        tmp = getAttribute(SessionAttributeNames.CONFIG_STORE_SESSION_ROOT_DN, defaultSessionRootDN);
        request.addParameter(SetupConstants.CONFIG_VAR_SESSION_ROOT_DN, tmp);

        tmp = getAttribute(SessionAttributeNames.CONFIG_STORE_SESSION_STORE_TYPE, defaultSessionStoreType);
        request.addParameter(SetupConstants.CONFIG_VAR_SESSION_STORE_TYPE, tmp);
       
        if (!isEmbedded) {
            tmp = getAttribute("configStoreHost", hostName);
            request.addParameter(
                SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST, tmp);
            tmp = getAttribute("configStoreSSL", "SIMPLE");
            request.addParameter(
                SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_SSL, tmp);
        }
        
        tmp = getAttribute("configStoreLoginId", defaultUserName);
        request.addParameter(
            SetupConstants.CONFIG_VAR_DS_MGR_DN, tmp);

        tmp = getAttribute("configStorePassword", "");
        request.addParameter(
            SetupConstants.CONFIG_VAR_DS_MGR_PWD, tmp);
                
        // user store repository
        tmp = (String)getContext().getSessionAttribute(
            SessionAttributeNames.EXT_DATA_STORE);

        if ((tmp != null) && tmp.equals("true")) {                       
            Map store = new HashMap(12);  
            tmp = (String)getContext().getSessionAttribute(
                SessionAttributeNames.USER_STORE_HOST);        
            store.put(SetupConstants.USER_STORE_HOST, tmp);
            
            tmp = (String)getContext().getSessionAttribute(
                SessionAttributeNames.USER_STORE_SSL);        
            store.put(SetupConstants.USER_STORE_SSL, tmp);            

            tmp = (String)getContext().getSessionAttribute(
                SessionAttributeNames.USER_STORE_PORT);
            store.put(SetupConstants.USER_STORE_PORT, tmp);
            tmp = (String)getContext().getSessionAttribute(
                SessionAttributeNames.USER_STORE_ROOT_SUFFIX);
            store.put(SetupConstants.USER_STORE_ROOT_SUFFIX, tmp);
            tmp = (String)getContext().getSessionAttribute(
                SessionAttributeNames.USER_STORE_LOGIN_ID);
            store.put(SetupConstants.USER_STORE_LOGIN_ID, tmp);      
            tmp = (String)getContext().getSessionAttribute(
                SessionAttributeNames.USER_STORE_LOGIN_PWD);
            store.put(SetupConstants.USER_STORE_LOGIN_PWD, tmp);      
            tmp = (String)getContext().getSessionAttribute(
                SessionAttributeNames.USER_STORE_TYPE);
            store.put(SetupConstants.USER_STORE_TYPE, tmp);

            request.addParameter("UserStore", store);
        }
        
        // site configuration is passed as a map of the site information 
        Map siteConfig = new HashMap(5);
        String loadBalancerHost = (String)getContext().getSessionAttribute( 
            SessionAttributeNames.LB_SITE_NAME);
        String primaryURL = (String)getContext().getSessionAttribute(
            SessionAttributeNames.LB_PRIMARY_URL);
        if (loadBalancerHost != null) {
            siteConfig.put(SetupConstants.LB_SITE_NAME, loadBalancerHost);
            siteConfig.put(SetupConstants.LB_PRIMARY_URL, primaryURL);
            request.addParameter(
                SetupConstants.CONFIG_VAR_SITE_CONFIGURATION, siteConfig);
        }

        // server properties
        String serverUrl = (String) getContext().getSessionAttribute(
            SessionAttributeNames.SERVER_URL);
        String serverHost;
        int serverPort;
        
        if (serverUrl == null) {
            serverUrl = req.getRequestURL().toString();
            serverHost = getHostName();
            serverPort = req.getServerPort();
        } else {
            serverHost = getHostName(serverUrl, getHostName());
            serverPort = getServerPort(serverUrl, req.getServerPort());
        }
        
        request.addParameter(
            SetupConstants.CONFIG_VAR_SERVER_HOST, serverHost);
        request.addParameter(
            SetupConstants.CONFIG_VAR_SERVER_PORT, serverPort);
        request.addParameter(
            SetupConstants.CONFIG_VAR_SERVER_URI, req.getRequestURL().toString());
        request.addParameter(
            SetupConstants.CONFIG_VAR_SERVER_URL, 
            getAttribute("serverURL", serverUrl));        

        tmp = (String)getContext().getSessionAttribute(
            SessionAttributeNames.ENCRYPTION_KEY);
        if (tmp == null) {
            tmp = AMSetupServlet.getRandomString();
        }
        request.addParameter(
            SetupConstants.CONFIG_VAR_ENCRYPTION_KEY, tmp);
        tmp = (String)getContext().getSessionAttribute(
            SessionAttributeNames.ENCLDAPUSERPASSWD);
        if (tmp != null) {
            request.addParameter(
                SetupConstants.ENCRYPTED_LDAP_USER_PWD, tmp);
        }

        String cookie = (String)getContext().getSessionAttribute(
            SessionAttributeNames.COOKIE_DOMAIN);
        if (cookie == null) {
            cookie = getCookieDomain();
        }
        request.addParameter(SetupConstants.CONFIG_VAR_COOKIE_DOMAIN, cookie);       
        
        String locale = (String)getContext().getSessionAttribute(
            SessionAttributeNames.PLATFORM_LOCALE);
        if (locale == null) {
            locale = SetupConstants.DEFAULT_PLATFORM_LOCALE;
        }
        request.addParameter(SetupConstants.CONFIG_VAR_PLATFORM_LOCALE, locale);

        String base = (String)getContext().getSessionAttribute(
            SessionAttributeNames.CONFIG_DIR);
        if (base == null) {
            base = getBaseDir(getContext().getRequest());
        }
        request.addParameter(SetupConstants.CONFIG_VAR_BASE_DIR, base);
        request.addParameter("locale", configLocale.toString());
                   
        try {
            if (AMSetupServlet.processRequest(request, response)) {
                writeToResponse("true");           
            } else {
                writeToResponse(AMSetupServlet.getErrorMessage());
            }
        } catch (ConfiguratorException cfe) {
            writeToResponse(cfe.getMessage());
        }
        
        setPath(null);
        return false;
    }


}
