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
 * $Id: ServerSiteModelImpl.java,v 1.6 2009/07/07 06:14:13 veiming Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.ServerConfigXML;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Server and Site model implementation.
 */
public class ServerSiteModelImpl
    extends AMModelBase
    implements ServerSiteModel {
    /**
     * Creates a simple model using default resource bundle. 
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public ServerSiteModelImpl(HttpServletRequest req,  Map map) {
        super(req, map);
    }

    /**
     * Returns a set of site names.
     *
     * @return a set of site names.
     * @throws AMConsoleException if error occurs when getting the site names.
     */
    public Set getSiteNames()
        throws AMConsoleException {
        String[] param = getServerInstanceForLogMsg();
        logEvent("ATTEMPT_GET_SITE_NAMES", param);
        
        try {
            Set siteNames = SiteConfiguration.getSites(getUserSSOToken());
            logEvent("SUCCEED_GET_SITE_NAMES", param);
            return siteNames;
        } catch (SMSException e){
            String[] params = {e.getMessage()};
            logEvent("SMS_EXCEPTION_GET_SITE_NAMES", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {e.getMessage()};
            logEvent("SSO_EXCEPTION_GET_SITE_NAMES", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }
    
    /**
     * Returns a primary URL of site.
     *
     * @param name Name of Site.
     * @return a primary URL of site.
     * @throws AMConsoleException if error occurs when getting the site primary
     *         URL.
     */
    public String getSitePrimaryURL(String name) 
        throws AMConsoleException {
        String[] param = {name};
        logEvent("ATTEMPT_GET_SITE_PRIMARY_URL", param);
        
        try {
            String url = SiteConfiguration.getSitePrimaryURL(getUserSSOToken(),
                name);
            logEvent("SUCCEED_GET_SITE_PRIMARY_URL", param);
            return url;
        } catch (SMSException e){
            String[] params = {name, e.getMessage()};
            logEvent("SMS_EXCEPTION_GET_SITE_PRIMARY_URL", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {name, e.getMessage()};
            logEvent("SSO_EXCEPTION_GET_SITE_PRIMARY_URL", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }
    
    /**
     * Returns a set of failover URLs of site.
     *
     * @param name Name of Site.
     * @return a set of failover URLs of site.
     * @throws AMConsoleException if error occurs when getting the site primary
     *         URL.
     */
    public Set getSiteFailoverURLs(String name)
        throws AMConsoleException {
        String[] param = {name};
        logEvent("ATTEMPT_GET_SITE_FAILOVER_URLS", param);
        try {
            Set urls = SiteConfiguration.getSiteSecondaryURLs(getUserSSOToken(),
                name);
            logEvent("SUCCEED_GET_SITE_FAILOVER_URLS", param);
            return urls;
        } catch (SMSException e){
            String[] params = {name, e.getMessage()};
            logEvent("SMS_EXCEPTION_GET_SITE_FAILOVER_URLS", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {name, e.getMessage()};
            logEvent("SSO_EXCEPTION_GET_SITE_FAILOVER_URLS", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }
    
    /**
     * Returns a set of servers that belong to site.
     *
     * @param name Name of Site.
     * @return a set of servers that belong to site.
     * @throws AMConsoleException if error occurs when getting the servers set.
     */
    public Set getSiteServers(String name)
        throws AMConsoleException {
        String[] param = {name};
        logEvent("ATTEMPT_GET_SITE_MEMBERS", param);
        try {
            Set members = SiteConfiguration.listServers(getUserSSOToken(),
                name);
            logEvent("SUCCEED_GET_SITE_MEMBERS", param);
            return members;
        } catch (ConfigurationException e){
            String[] params = {name, e.getMessage()};
            logEvent("SMS_EXCEPTION_GET_SITE_MEMBERS", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e){
            String[] params = {name, e.getMessage()};
            logEvent("SMS_EXCEPTION_GET_SITE_MEMBERS", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {name, e.getMessage()};
            logEvent("SSO_EXCEPTION_GET_SITE_MEMBERS", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }
    
    /**
     * Creates a site.
     *
     * @param name Name of Site.
     * @param url Primary URL of Site.
     * @throws AMConsoleException if error occurs when creating site.
     */
    public void createSite(String name, String url) 
        throws AMConsoleException {
        String[] param = {name};
        logEvent("ATTEMPT_CREATE_SITE", param);
        try {
            SiteConfiguration.createSite(getUserSSOToken(),
                name, url, Collections.EMPTY_SET);
            logEvent("SUCCEED_CREATE_SITE", param);
        } catch (ConfigurationException e){
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e){
            String[] params = {name, e.getMessage()};
            logEvent("SMS_EXCEPTION_CREATE_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {name, e.getMessage()};
            logEvent("SSO_EXCEPTION_CREATE_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Creates a server.
     *
     * @param name Name of Server.
     * @throws AMConsoleException if error occurs when creating server.
     */
    public void createServer(String name) 
        throws AMConsoleException {
        SSOToken ssoToken = getUserSSOToken();
        String[] param = {name};
        logEvent("ATTEMPT_CREATE_SERVER", param);

        try {
            String svrConfigXML = ServerConfiguration.getServerConfigXML(
                ssoToken, SystemProperties.getServerInstanceName());
            ServerConfiguration.createServerInstance(ssoToken, name, 
                Collections.EMPTY_MAP, svrConfigXML);
            logEvent("SUCCEED_CREATE_SERVER", param);
        } catch (UnknownPropertyNameException e) {
            // this will not happen because we do not set any property during
            // creation.
        } catch (SSOException e) {
            String[] params = {name, e.getMessage()};
            logEvent("SSO_EXCEPTION_CREATE_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (ConfigurationException e) {
            String[] params = {name, e.getMessage()};
            logEvent("CONFIGURATION_EXCEPTION_CREATE_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e) {
            String[] params = {name, e.getMessage()};
            logEvent("SMS_EXCEPTION_CREATE_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (IOException e) {
            String[] params = {name, e.getMessage()};
            logEvent("IO_EXCEPTION_CREATE_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }
        
    /**
     * Deletes a set of sites.
     *
     * @param sites Set of of sites to be deleted.
     * @throws AMConsoleException if error occurs when getting the site name.
     */
    public void deleteSites(Set sites)
        throws AMConsoleException {
        String siteName = null;
        try {
            if ((sites != null) && !sites.isEmpty()) {
                SSOToken token = getUserSSOToken();

                for (Iterator i = sites.iterator(); i.hasNext(); ) {
                    siteName = (String)i.next();

                    String[] param = {siteName};
                    logEvent("ATTEMPT_DELETE_SITE", param);
                    SiteConfiguration.deleteSite(token, siteName);
                    logEvent("SUCCEED_DELETE_SITE", param);
                }
            }
        } catch (ConfigurationException e){
            String[] params = {siteName, e.getMessage()};
            logEvent("SMS_EXCEPTION_DELETE_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e){
            String[] params = {siteName, e.getMessage()};
            logEvent("SMS_EXCEPTION_DELETE_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {siteName, e.getMessage()};
            logEvent("SSO_EXCEPTION_DELETE_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Modifies site profile.
     *
     * @param siteName name of site.
     * @param primaryURL Primary URL.
     * @param failoverURLs Failover URLs.
     * @throws AMConsoleException if site profile cannot be modified.
     */
    public void modifySite(
        String siteName, 
        String primaryURL, 
        Set failoverURLs
    ) throws AMConsoleException {
        try {
            SSOToken ssoToken = getUserSSOToken();
            String[] param = {siteName};
            logEvent("ATTEMPT_MODIFY_SITE", param);
            
            SiteConfiguration.setSitePrimaryURL(ssoToken, siteName, primaryURL);
            SiteConfiguration.setSiteSecondaryURLs(ssoToken, siteName, 
                failoverURLs);
            logEvent("SUCCEED_MODIFY_SITE", param);
        } catch (ConfigurationException e) {
            String[] params = {siteName, e.getMessage()};
            logEvent("CONFIGURATION_EXCEPTION_MODIFY_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e) {
            String[] params = {siteName, e.getMessage()};
            logEvent("SMS_EXCEPTION_MODIFY_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            String[] params = {siteName, e.getMessage()};
            logEvent("SSO_EXCEPTION_MODIFY_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }
    
    /**
     * Returns a set of server names.
     *
     * @return a set of server names.
     * @throws AMConsoleException if error occurs when getting the names.
     */
    public Set getServerNames()
        throws AMConsoleException {
        String[] param = getServerInstanceForLogMsg();
        logEvent("ATTEMPT_GET_SERVER_NAMES", param);

        try {
            Set names = ServerConfiguration.getServers(getUserSSOToken());
            logEvent("SUCCEED_GET_SERVER_NAMES", param);
            return names;
        } catch (SMSException e){
            String[] params = {e.getMessage()};
            logEvent("SMS_EXCEPTION_GET_SERVER_NAMES", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {e.getMessage()};
            logEvent("SSO_EXCEPTION_GET_SERVER_NAMES", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns a site name of which server belongs to.
     *
     * @param name Name of Server.
     * @return a site name.
     * @throws AMConsoleException if error occurs when getting the site name.
     */
    public String getServerSite(String name) 
        throws AMConsoleException {
        String[] param = {name};
        logEvent("ATTEMPT_GET_SERVER_SITE", param);
        try {
            String siteName = ServerConfiguration.getServerSite(
                getUserSSOToken(), name);
            logEvent("SUCCEED_GET_SERVER_SITE", param);
            return siteName;
        } catch (SMSException e){
            String[] params = {name, e.getMessage()};
            logEvent("SMS_EXCEPTION_GET_SERVER_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {name, e.getMessage()};
            logEvent("SSO_EXCEPTION_GET_SERVER_SITE", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Deletes a set of servers.
     *
     * @param servers Set of of servers to be deleted.
     * @throws AMConsoleException if error occurs when getting the site name.
     */
    public void deleteServers(Set servers)
        throws AMConsoleException {

        if (servers.contains(SystemProperties.getServerInstanceName())) {
            throw new AMConsoleException(getLocalizedString(
                "exception.cannot.delete.this.server.instance"));
        }
        String serverName = null;
        try {
            if ((servers != null) && !servers.isEmpty()) {
                SSOToken token = getUserSSOToken();

                for (Iterator i = servers.iterator(); i.hasNext(); ) {
                    serverName = (String)i.next();
                    String[] param = {serverName};
                    logEvent("ATTEMPT_DELETE_SERVER", param);
                    ServerConfiguration.deleteServerInstance(token, serverName);
                    logEvent("SUCCEED_DELETE_SERVER", param);
                }
            }
        } catch (SMSException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("SMS_EXCEPTION_DELETE_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("SSO_EXCEPTION_DELETE_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }
    
    /**
     * Returns edit site title page.
     *
     * @param siteName Name of site.
     * @return edit site title page.
     */
    public String getEditSitePageTitle(String siteName) {
        Object[] param = {siteName};
        return MessageFormat.format(
            getLocalizedString("page.title.site.edit"), param);
    }

    /**
     * Clones a server.
     *
     * @param origServer Name of the server to be cloned.
     * @param cloneServer Name of clone server.
     * @throws AMConsoleException if server cannot be cloned.
     */
    public void cloneServer(String origServer, String cloneServer)
        throws AMConsoleException {
        String[] param = {origServer, cloneServer};
        logEvent("ATTEMPT_CLONE_SERVER", param);

        try {
            ServerConfiguration.cloneServerInstance(getUserSSOToken(),
                origServer, cloneServer);
            logEvent("SUCCEED_CLONE_SERVER", param);
        } catch (ConfigurationException e){
            String[] params = {origServer, cloneServer, e.getMessage()};
            logEvent("CONFIGURATION_EXCEPTION_CLONE_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e){
            String[] params = {origServer, cloneServer, e.getMessage()};
            logEvent("SMS_EXCEPTION_CLONE_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {origServer, cloneServer, e.getMessage()};
            logEvent("SSO_EXCEPTION_CLONE_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }
    
    /**
     * Returns edit server title page.
     *
     * @param serverName Name of server.
     * @return edit server title page.
     */
    public String getEditServerPageTitle(String serverName) {
        Object[] param = {serverName};
        return MessageFormat.format(
            getLocalizedString("page.title.server.edit"), param);
    }
    
    /**
     * Returns server configuration.
     *
     * @param serverName name of server.
     * @return server configuration.
     * @throws AMConsoleException if server profile cannot be retrieved.
     */
    public Map getServerConfiguration(String serverName)
        throws AMConsoleException {
        String[] param = {serverName};
        try {
            SSOToken ssoToken = getUserSSOToken();

            logEvent("ATTEMPT_GET_SERVER_CONFIG", param);
            Map attrs = ServerConfiguration.getServerInstance(
                ssoToken, serverName);
            removeHiddenProperties(attrs);
            logEvent("SUCCEED_GET_SERVER_CONFIG", param);
            return attrs;
        } catch (SMSException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("SMS_EXCEPTION_GET_SERVER_CONFIG", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (IOException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("IO_EXCEPTION_GET_SERVER_CONFIG", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("SSO_EXCEPTION_GET_SERVER_CONFIG", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    private static void removeHiddenProperties(Map attrs) {
        attrs.remove(Constants.AM_SERVER_PROTOCOL);
        attrs.remove(Constants.AM_SERVER_HOST);
        attrs.remove(Constants.AM_SERVER_PORT);
        attrs.remove(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        attrs.remove(Constants.SERVER_MODE);
        attrs.remove(SetupConstants.AMC_OVERRIDE_PROPERTY);
    }

    /**
     * Returns default server configuration.
     *
     * @return server configuration.
     */
    public Map getServerDefaults() {
        String[] param = getServerInstanceForLogMsg();
        logEvent("ATTEMPT_GET_SERVER_DEFAULT_CONFIG", param);
        Map map = ServerConfiguration.getDefaults(getUserSSOToken());
        logEvent("SUCCEED_GET_SERVER_DEFAULT_CONFIG", param);
        return map;
    }
    
    /**
     * Modifies server profile.
     *
     * @param serverName name of server.
     * @param parentSite Parent site.
     * @param values Property Values.
     * @throws AMConsoleException if server profile cannot be modified.
     * @throws UnknownPropertyNameException if property names are unknown.
     */
    public void modifyServer(String serverName, String parentSite, Map values)
        throws AMConsoleException, UnknownPropertyNameException {
        String[] param = {serverName};

        try {
            SSOToken ssoToken = getUserSSOToken();
            logEvent("ATTEMPT_MODIFY_SERVER", param);
            
            if (parentSite != null) {
                String currentSite = ServerConfiguration.getServerSite(
                    ssoToken, serverName);
                if ((currentSite == null) || !currentSite.equals(parentSite)) {
                    ServerConfiguration.setServerSite(
                        ssoToken, serverName, parentSite);
                }
            }

            ServerConfiguration.setServerInstance(ssoToken, serverName, values);
            logEvent("SUCCEED_MODIFY_SERVER", param);
        } catch (ConfigurationException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("CONFIGURATION_EXCEPTION_MODIFY_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (IOException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("IO_EXCEPTION_MODIFY_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("SMS_EXCEPTION_MODIFY_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("SSO_EXCEPTION_MODIFY_SERVER", params);
            throw new AMConsoleException(getErrorString(e));
        }        
    }
    
    /**
     * Updates server property inheritance settings.
     *
     * @param serverName Name of server.
     * @param toInherit Properties to inherit.
     * @param notToInherit Properties not to inherit.
     * @throws AMConsoleException if server profile cannot be updated.
     */
    public void updateServerConfigInheritance(
        String serverName,
        Set toInherit, 
        Set notToInherit
    ) throws AMConsoleException {
        String[] param = {serverName};
        logEvent("ATTEMPT_MODIFY_SERVER_INHERITANCE", param);

        try {
            SSOToken ssoToken = getUserSSOToken();
            Map defaultValues = ServerConfiguration.getDefaults(ssoToken);
            Map svrProperties = ServerConfiguration.getServerInstance(
                ssoToken, serverName);
            
            if ((toInherit != null) && !toInherit.isEmpty()) {
                Set toRemove = new HashSet();
                for (Iterator i = toInherit.iterator(); i.hasNext(); ) {
                    String name = (String)i.next();
                    if (svrProperties.containsKey(name)) {
                        toRemove.add(name);
                    }
                }
                
                if (!toRemove.isEmpty()) {
                    ServerConfiguration.removeServerConfiguration(ssoToken, 
                        serverName, toRemove);
                }
            }
            
            if ((notToInherit != null) && !notToInherit.isEmpty()) {
                Map toAdd = new HashMap();
                for (Iterator i = notToInherit.iterator(); i.hasNext(); ) {
                    String name = (String)i.next();
                    if (!svrProperties.containsKey(name)) {
                        toAdd.put(name, defaultValues.get(name));
                    }
                }
                if (!toAdd.isEmpty()) {
                    try {
                        ServerConfiguration.setServerInstance(ssoToken, 
                            serverName, toAdd);
                    } catch (UnknownPropertyNameException ex) {
                        // ignore, this cannot happen because default values
                        // would have all valid property names
                    }
                }
            }
            logEvent("SUCCEED_MODIFY_SERVER_INHERITANCE", param);
        } catch (ConfigurationException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("CONFIGURATION_EXCEPTION_MODIFY_SERVER_INHERITANCE",
                params);
            throw new AMConsoleException(getErrorString(e));
        } catch (IOException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("IO_EXCEPTION_MODIFY_SERVER_INHERITANCE", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("SMS_EXCEPTION_MODIFY_SERVER_INHERITANCE", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e){
            String[] params = {serverName, e.getMessage()};
            logEvent("SSO_EXCEPTION_MODIFY_SERVER_INHERITANCE", params);
            throw new AMConsoleException(getErrorString(e));
        }        
    }
    
    /**
     * Returns the server configuration XML object.
     *
     * @param serverName Name of server.
     * @return the server configuration XML object.
     * @throws AMConsoleException if server configuration XML object cannot be
     *         return.
     */
    public ServerConfigXML getServerConfigObject(String serverName) 
        throws AMConsoleException {
        String[] param = {serverName};
        logEvent("ATTEMPT_GET_SERVER_CONFIG_XML", param);
        ServerConfigXML xmlObject = null;
        try {
            String xml = ServerConfiguration.getServerConfigXML(
                getUserSSOToken(), serverName);
            xmlObject = new ServerConfigXML(xml);
            logEvent("SUCCEED_GET_SERVER_CONFIG_XML", param);
        } catch (SMSException e) {
            String[] params = {serverName, e.getMessage()};
            logEvent("SMS_EXCEPTION_GET_SERVER_CONFIG_XML", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            String[] params = {serverName, e.getMessage()};
            logEvent("SSO_EXCEPTION_GET_SERVER_CONFIG_XML", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (Exception e) {
            String[] params = {serverName, e.getMessage()};
            logEvent("GENERIC_EXCEPTION_GET_SERVER_CONFIG_XML", params);
            throw new AMConsoleException(getErrorString(e));
        }
        return xmlObject;
    }
    
    /**
     * Sets the server configuration XML.
     *
     * @param serverName Name of server.
     * @param xml the server configuration XML.
     * @throws AMConsoleException if server configuration XML cannot be set.
     */
    public void setServerConfigXML(String serverName, String xml)
        throws AMConsoleException {
        String[] param = {serverName};
        logEvent("ATTEMPT_SET_SERVER_CONFIG_XML", param);
        try {
            ServerConfiguration.setServerConfigXML(
                getUserSSOToken(), serverName, xml);
            logEvent("SUCCEED_SET_SERVER_CONFIG_XML", param);
        } catch (SMSException e) {
            String[] params = {serverName, e.getMessage()};
            logEvent("SMS_EXCEPTION_SET_SERVER_CONFIG_XML", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            String[] params = {serverName, e.getMessage()};
            logEvent("SSO_EXCEPTION_SET_SERVER_CONFIG_XML", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (ConfigurationException e) {
            String[] params = {serverName, e.getMessage()};
            logEvent("SSO_EXCEPTION_SET_SERVER_CONFIG_XML", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }
}
