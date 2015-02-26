/* The contents of this file are subject to the terms
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
 * $Id: CreateAgentProfile.java,v 1.8 2009/03/17 20:28:32 sridharev Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.agents;

import com.gargoylesoftware.htmlunit.WebClient;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.AgentsCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.TestCommon;
import java.net.URL;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Map;
import java.util.logging.Level;
import org.testng.Reporter;

/**
 * This class initialises the profile of agents with version 3.0 & above
 */

public class CreateAgentProfile extends TestCommon {

    private boolean executeAgainstOpenSSO;
    private String executeCdssoMode;
    private String logoutURL;
    private String strScriptURL;
    private String strHotSwapRB = "HotSwapProperties";
    private String strLocRB = "HeaderAttributeTests";
    private String strGblRB = "agentsGlobal";
    private String resource;
    private String agentProtocol;
    private String agentHost;
    private String agentPort;
    private AMIdentity amid;
    private IDMCommon idmc;
    private ResourceBundle rbg;
    private SSOToken admintoken;
    private String skew;

    /**
     * Instantiated different helper class objects
     */
    public CreateAgentProfile() 
    throws Exception{
        super("CreateAgentProfile");
        idmc = new IDMCommon();
        rbg = ResourceBundle.getBundle("agents" + fileseparator + strGblRB);
        executeAgainstOpenSSO = new Boolean(rbg.getString(strGblRB +
                ".executeAgainstOpenSSO")).booleanValue();
        executeCdssoMode = rbg.getString("com.sun.identity.agents." +
                "config.cdsso.enable");
        skew = rbg.getString("com.sun.identity.agents.config.cdsso.clock.skew");
        admintoken = getToken(adminUser, adminPassword, basedn);
        logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
        strScriptURL = rbg.getString(strGblRB + ".headerEvalScriptName");

        // Forming the Agent protocol, host & port
        String strLocalURL = strScriptURL;
        int iIndex;
        iIndex = strScriptURL.indexOf("://");
        agentProtocol = strLocalURL.substring(0, iIndex);
        strLocalURL = strLocalURL.substring(iIndex + 3, strLocalURL.length());
        iIndex = strLocalURL.indexOf(":");
        agentHost = strLocalURL.substring(0, iIndex);
        strLocalURL = strLocalURL.substring(iIndex + 1, strLocalURL.length());
        iIndex = strLocalURL.indexOf("/");
        agentPort = strLocalURL.substring(0, iIndex);
    }
  
    /**
     * Sets the specific properties with the desired values and the 
     * rest of the properties to the default values.
     */
    public void create(String agentId, String agentType)
    throws Exception {
        try{
            entering("create", null);
            Map map = new HashMap();
            Set set;
            
            // Properties common to both J2EE & Web agents
            // Server properties
            set = new HashSet();
            set.add("centralized");
            map.put("com.sun.identity.agents.config.repository.location", set);
            set = new HashSet();
            set.add("[0]=" + protocol + "://" + host + ":" + port + uri + 
                    "/UI/Login");
            map.put("com.sun.identity.agents.config.login.url", set);
            set = new HashSet();
            set.add("[0]=" + logoutURL);
            log(Level.FINE, "create" ,"logoutURL= " + logoutURL);
            map.put("com.sun.identity.agents.config.logout.url", set);
            
            set = new HashSet();
            set.add("[0]=" + protocol + "://" + host + ":" + port + uri + 
                    "/cdcservlet");
            map.put("com.sun.identity.agents.config.cdsso.cdcservlet.url", 
                    set);

            set = new HashSet();
            set.add(executeCdssoMode);
            map.put("com.sun.identity.agents.config.cdsso.enable", set);

             set = new HashSet();
             set.add("agentRootURL=" + agentProtocol + "://" + agentHost + ":"
                     + agentPort + "/");
             map.put("sunIdentityServerDeviceKeyValue" , set);

            //Agent properties
            set = new HashSet();
            set.add(agentHost);
            map.put("com.sun.identity.agents.config.fqdn.default", set);
            
            set = new HashSet();
            set.add("amAuthLog." + agentHost + "." + agentPort + ".log");
            map.put("com.sun.identity.agents.config.remote.logfile", set);

            // Setting common qatest properties
            set = new HashSet();
            set.add("HTTP_HEADER");
            map.put("com.sun.identity.agents.config.profile.attribute." + 
                    "fetch.mode", set);
            set = new HashSet();
            set.add("HTTP_HEADER");
            map.put("com.sun.identity.agents.config.session.attribute." + 
                    "fetch.mode", set);
            set = new HashSet();
            set.add("HTTP_HEADER");
            map.put("com.sun.identity.agents.config.response.attribute." + 
                    "fetch.mode", set);            
            
            // Setting J2EE/WEBLOGIC agent specific properties
            if (agentType.contains("J2EE") || agentType.contains("WEBLOGIC")) {
                set = new HashSet();
                set.add(agentProtocol + "://" + agentHost + ":" + agentPort 
                        + "/agentapp/notification");
                map.put("com.sun.identity.client.notification.url", set);

                //setup cdsso clock skew if CDSSO is enabled
                boolean isCdsso = new Boolean(executeCdssoMode).booleanValue();
                set = new HashSet();
                if(isCdsso) {
                    set.add(skew);
                } else {
                    set.add("0");
                }
                map.put("com.sun.identity.agents.config.cdsso.clock.skew", set);
                
                // Setting qatest specific properties
                set = new HashSet();
                set.add("[0] = Group");
                set.add("[1] = Role");
                map.put("com.sun.identity.agents.config.privileged." + 
                        "attribute.type", set);
                set = new HashSet();
                set.add("[Role] = true");
                set.add("[Group] = false");
                map.put("com.sun.identity.agents.config.privileged." + 
                        "attribute.tolowercase", set);                        
                
                // Profile attribute mapping
                set = new HashSet();
                set.add("[cn] = HTTP_PROFILE_CN");
                set.add("[nsrole] = HTTP_PROFILE_NSROLE");
                set.add("[iplanet-am-user-alias-list]=HTTP_PROFILE_ALIAS");
                map.put("com.sun.identity.agents.config.profile." + 
                        "attribute.mapping", set);                        
                
                // Session attribute mapping
                set = new HashSet();
                set.add("[sun.am.UniversalIdentifier] = " + 
                        "HTTP_SESSION_UNIVERSALIDENTIFIER");
                set.add("[MyProperty] = HTTP_SESSION_MYPROPERTY");
                map.put("com.sun.identity.agents.config.session." + 
                        "attribute.mapping", set);                        

                // Response attribute mapping
                set = new HashSet();
                set.add("[statSingle] = HTTP_RESPONSE_STATSINGLE");
                set.add("[statMultiple] = HTTP_RESPONSE_STATMULTIPLE");
                set.add("[cn] = HTTP_RESPONSE_CN");
                set.add("[mail] = HTTP_RESPONSE_MAIL");
                map.put("com.sun.identity.agents.config.response." + 
                        "attribute.mapping", set);                        
                
                // Setting access denied URI
                set = new HashSet();
                set.add("/agentsample/resources/accessdenied.html");
                map.put("com.sun.identity.agents.config.access.denied.uri", 
                        set);                        

                // Adding notenf.html to the list of not enforced URLs        
                set = new HashSet();
                set.add("[0] = /agentsample/public/*");
                set.add("[1] = /agentsample/images/*");
                set.add("[2] = /agentsample/styles/*");
                set.add("[3] = /agentsample/index.html");
                set.add("[4] = /agentsample");
                set.add("[5] = /agentsample/");
                set.add("[6] = /agentsample/resources/accessdenied.html");                
                set.add("[7] = /agentsample/resources/notenf.html");                                
                map.put("com.sun.identity.agents.config.notenforced.uri", set);                        

               // Setting default properties
                set = new HashSet();
                set.add("/agentapp/sunwCDSSORedirectURI");
                map.put("com.sun.identity.agents.config.cdsso.redirect.uri", 
                        set);
                set = new HashSet();
                set.add("/agentapp/sunwLegacySupportURI");
                map.put("com.sun.identity.agents.config.legacy.redirect.uri", 
                        set);
                set = new HashSet();
                set.add("[" + agentPort + "]=" + agentProtocol);
                map.put("com.sun.identity.agents.config.port.check.setting", 
                        set);
                set = new HashSet();
                set.add(protocol);
                map.put("com.iplanet.am.server.protocol", set);
                set = new HashSet();
                set.add(host);
                map.put("com.iplanet.am.server.host", set);
                set = new HashSet();
                set.add(port);
                map.put("com.iplanet.am.server.port", set);
                set = new HashSet();
                set.add("[0]=" + protocol + "://" + host + ":" + port + uri + 
                        "/cdcservlet");
                map.put("com.sun.identity.agents.config.cdsso.trusted." + 
                        "id.provider", set);
                set = new HashSet();
                set.add("ALL");
                map.put("com.sun.identity.agents.config.filter.mode", set);             
                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.httpsession.binding", 
                        set);                        
                set = new HashSet();
                set.add("message");
                map.put("com.iplanet.services.debug.level", set);                        
                
                // User attribute mapping
                set = new HashSet();
                set.add("USER_ID");
                map.put("com.sun.identity.agents.config.user.mapping.mode", 
                        set);                        
                set = new HashSet();
                set.add("employeenumber");
                map.put("com.sun.identity.agents.config.user.attribute.name", 
                        set);                        
                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.user.principal", set);                        
                set = new HashSet();
                set.add("UserToken");
                map.put("com.sun.identity.agents.config.user.token", 
                        set);                        
                set = new HashSet();
                set.add("LOG_NONE");
                map.put("com.sun.identity.agents.config.audit.accesstype", set);                        

                //J2EE agent properties
                set = new HashSet();
                set.add("REMOTE");
                map.put("com.sun.identity.agents.config.log.disposition", set);            

                set = new HashSet();
                set.add("Active");
                map.put("sunIdentityServerDeviceStatus", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.notenforced.ip." + 
                        "cache.enable", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.legacy.support.enable", 
                        set);            

                set = new HashSet();
                set.add("com.iplanet.services.util.JCEEncryption");
                map.put("com.iplanet.security.encryptor", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.logout.url.probe." + 
                        "enabled", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.login.url.probe." + 
                        "enabled", set);            

                set = new HashSet();
                set.add("ALL");
                map.put("com.sun.identity.agents.config.filter.mode", set);            


                set = new HashSet();
                set.add("/agentapp/sunwCDSSORedirectURI");
                map.put("com.sun.identity.agents.config.cdsso.redirect.uri", 
                        set);            

                set = new HashSet();
                set.add("USER_ID");
                map.put("com.sun.identity.agents.config.user.mapping.mode", 
                        set);                        

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.webservice.process." + 
                        "get.enable", set);            

                set = new HashSet();
                set.add("30");
                map.put("com.iplanet.am.session.client.polling.period", set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.notenforced.ip", set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.auth.handler", set);            

                set = new HashSet();
                set.add("serviceType=iPlanetAMWebAgentService|class=com.sun." + 
                        "identity.policy.plugins.HttpURLResourceName|" + 
                        "wildcard=*|delimiter=/|caseSensitive=false");
                map.put("com.sun.identity.policy.client.resourceComparators", 
                        set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.httpsession.binding", 
                        set);            

                set = new HashSet();
                set.add("en");
                map.put("com.sun.identity.agents.config.locale.language", set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.webservice.endpoint", 
                        set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.cookie.reset.enable", 
                        set);            

                set = new HashSet();
                set.add("[0]=/agentsample/authentication/login.html");
                map.put("com.sun.identity.agents.config.login.form", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.local.log.rotate", 
                        set);            

                set = new HashSet();
                set.add("WSAuthErrorContent.txt");
                map.put("com.sun.identity.agents.config.webservice.autherror." 
                        + "content", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.webservice.enable", 
                        set);            

                set = new HashSet();
                set.add("2000");
                map.put("com.sun.identity.agents.config.logout.url.probe." + 
                        "timeout", set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.policy.env.jsession." + 
                        "param", set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.cdsso.domain", set);            

                set = new HashSet();
                set.add("WSInternalErrorContent.txt");
                map.put("com.sun.identity.agents.config.webservice." + 
                        "internalerror.content", set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.policy.env.get.param", 
                        set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.change.notification." +
                        "enable", set);
                
                set = new HashSet();
                set.add("52428800");
                map.put("com.sun.identity.agents.config.local.log.size", set);            


                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.response.header", set);            


                set = new HashSet();
                set.add("1000");
                map.put("com.sun.identity.agents.config.notenforced.uri." + 
                        "cache.size", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.fqdn.check.enable", 
                        set);            

                set = new HashSet();
                set.add("[0]=AUTHENTICATED_USERS");
                map.put("com.sun.identity.agents.config.default.privileged." + 
                        "attribute", set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.bypass.principal", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.attribute.cookie.encode"
                        , set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.cookie.reset.path", 
                        set);            

                set = new HashSet();
                set.add("FormLoginContent.txt");
                map.put("com.sun.identity.agents.config.login.content.file", 
                        set);            

                set = new HashSet();
                set.add("subtree");
                map.put("com.sun.identity.policy.client.cacheMode", set);            

                set = new HashSet();
                set.add("|");
                map.put("com.sun.identity.agents.config.attribute.cookie." + 
                        "separator", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.login.url.prioritized", 
                        set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.privileged.session." + 
                        "attribute", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.amsso.cache.enable", 
                        set);            

                set = new HashSet();
                set.add("iPlanetAMWebAgentService|GET|allow|deny:" + 
                        "iPlanetAMWebAgentService|POST|allow|deny");
                map.put("com.sun.identity.policy.client.booleanActionValues", 
                        set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.privileged.attribute." +
                        "mapping.enable", set);            

                set = new HashSet();
                set.add("1");
                map.put("com.sun.identity.agents.polling.interval", set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.logout.application." + 
                        "handler", set);            

                set = new HashSet();
                set.add("2000");
                map.put("com.sun.identity.agents.config.login.url.probe." + 
                        "timeout", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.logout.url." + 
                        "prioritized", set);            

                set = new HashSet();
                set.add("[0]=Mozilla/4.7*");
                map.put("com.sun.identity.agents.config.legacy.user.agent", 
                        set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.notenforced.ip.invert", 
                        set);            

                set = new HashSet();
                set.add("0");
                map.put("com.sun.identity.agents.config.redirect.attempt.limit", 
                        set);            

                set = new HashSet();
                set.add("iPlanetDirectoryPro");
                map.put("com.iplanet.am.cookie.name", set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.cookie.reset.domain", 
                        set);            

                set = new HashSet();
                set.add("1");
                map.put("com.sun.identity.sm.cacheTime", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.notenforced.uri.invert",
                        set);            

                set = new HashSet();
                set.add("goto");
                map.put("com.sun.identity.agents.config.redirect.param", set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.logout.request.param", 
                        set);            

                set = new HashSet();
                set.add("1000");
                map.put("com.sun.identity.agents.config.notenforced.ip." + 
                        "cache.size", set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.policy.env.post.param", 
                        set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.verification.handler",
                        set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.logout.introspect." + 
                        "enabled", set);            

                set = new HashSet();
                set.add("0");
                map.put("com.sun.identity.agents.config.login.attempt.limit",
                        set);            

                set = new HashSet();
                set.add("PortCheckContent.txt");
                map.put("com.sun.identity.agents.config.port.check.file", set);            

                set = new HashSet();
                set.add("1");
                map.put("com.iplanet.am.sdk.remote.pollingTime", set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.logout.uri", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.change.notification." + 
                        "enable", set);
                
                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.notenforced.uri." + 
                        "cache.enable", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.user.principal", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.port.check.enable", 
                        set);            

                set = new HashSet();
                set.add("EEE, d MMM yyyy hh:mm:ss z");
                map.put("com.sun.identity.agents.config.attribute.date.format",
                        set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.cdsso.secure.enable",
                        set);            

                set = new HashSet();
                set.add("false");
                map.put("com.iplanet.am.session.client.polling.enable", set);            

                set = new HashSet();
                set.add("0");
                map.put("com.sun.identity.agents.config.load.interval", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.idm.remote.notification.enabled",
                        set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.logout.handler", set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.logout.entry.uri", set);            

                set = new HashSet();
                set.add("=US");
                map.put("com.sun.identity.agents.config.locale.country", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.login.use.internal",
                        set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.fqdn.mapping", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.sm.notification.enabled", set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.cookie.reset.name",
                        set);         

                if (agentType.contains("3.0WEBLOGIC")) {
                    set = new HashSet();
                    set.add("[id=manager,ou=group,dc=opensso,dc=java,dc=net]" +
                            "=am_manager_role");
                    set.add("[id=employee,ou=group,dc=opensso,dc=java,dc=net]" +
                            "=am_employee_role");
                    set.add("[id=manager,ou=role,dc=opensso,dc=java,dc=net]" +
                            "=am_manager_role");
                    set.add("[id=employee,ou=role,dc=opensso,dc=java,dc=net]" +
                            "=am_employee_role");
                    map.put("com.sun.identity.agents.config.privileged." + 
                            "attribute.mapping", set);            
                } else {
                    set = new HashSet();
                    set.add("[]=");
                    map.put("com.sun.identity.agents.config.privileged." + 
                            "attribute.mapping", set);            
                    set = new HashSet();
                    set.add("[0]=");
                    map.put("com.sun.identity.agents.config.login.error.uri",
                            set);                                
                }
                
            } else if (agentType.contains("WEB")) {
                set = new HashSet();
                set.add(agentProtocol + "://" + agentHost + ":" + agentPort + 
                        "/UpdateAgentCacheServlet?shortcircuit=false");
                map.put("com.sun.identity.client.notification.url", set);
                set = new HashSet();
                set.add(agentProtocol + "://" + agentHost + ":" + agentPort + 
                        "/amagent");
                map.put("com.sun.identity.agents.config.agenturi.prefix", set);

                // Setting qatest specific properties                
                // Profile attribute mapping
                set = new HashSet();
                set.add("[cn]=PROFILE_CN");
                set.add("[nsrole]=PROFILE_NSROLE");
                set.add("[iplanet-am-user-alias-list]=HTTP_PROFILE_ALIAS");
                map.put("com.sun.identity.agents.config.profile.attribute" + 
                        ".mapping", set);                        
                
                // Session attribute mapping
                set = new HashSet();
                set.add("[sun.am.UniversalIdentifier]=" + 
                        "SESSION_UNIVERSALIDENTIFIER");
                set.add("[MyProperty]=SESSION_MYPROPERTY");
                map.put("com.sun.identity.agents.config.session." + 
                        "attribute.mapping", set);                        

                // Response attribute mapping
                set = new HashSet();
                set.add("[statSingle]=RESPONSE_STATSINGLE");
                set.add("[statMultiple]=RESPONSE_STATMULTIPLE");
                set.add("[cn]=RESPONSE_CN");
                set.add("[mail]=RESPONSE_MAIL");
                map.put("com.sun.identity.agents.config.response." + 
                        "attribute.mapping", set);                        

                // Setting anonyumous user access
                set = new HashSet();
                set.add("anonymous");
                map.put("com.sun.identity.agents.config.anonymous.user.id", 
                        set);                        
                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.anonymous.user.enable",
                        set);                        
                
                // Setting access denied URL
                set = new HashSet();
                set.add(agentProtocol + "://" + agentHost + ":" + agentPort + 
                        "/accessdenied.html");
                map.put("com.sun.identity.agents.config.access.denied.url", 
                        set);                        

                // Adding notenf.html to the list of not enforced URLs        
                set = new HashSet();
                set.add("[0]=" + agentProtocol + "://" + agentHost + ":" + 
                        agentPort + "/notenf.html");
                log(Level.FINE, "create", "[0]=" + agentProtocol + 
                        "://" + agentHost + ":" + agentPort + "/notenf.html");
                map.put("com.sun.identity.agents.config.notenforced.url", set);                        

                // Setting default Web agent properties
                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.encode.url.special." + 
                        "chars.enable", set);            

                set = new HashSet();
                set.add("Active");
                map.put("sunIdentityServerDeviceStatus", set);            

                set = new HashSet();
                set.add("1");
                map.put("com.sun.identity.agents.config.policy.cache." + 
                        "polling.interval", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.domino.check.name." + 
                        "database", set);            

                set = new HashSet();
                set.add("60");
                map.put("com.sun.identity.agents.config.polling.interval", set);            

                set = new HashSet();
                set.add("30");
                map.put("com.sun.identity.agents.config.cleanup.interval", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.fetch.from.root." + 
                        "resource", set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.logout.cookie.reset",
                        set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.ignore.path.info", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.change.notification." +
                        "enable", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.notification.enable" , 
                        set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.ignore.preferred." + 
                        "naming.url", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.cookie.secure", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.iis.owa.enable", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.notenforced.url.invert",
                        set);            

                set = new HashSet();
                set.add("2");
                map.put("com.sun.identity.agents.config.auth.connection.timeout"
                        , set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.anonymous.user.enable",
                        set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.url.comparison.case." + 
                        "ignore", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.load.balancer.enable",
                        set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.notenforced.ip", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.domino.ltpa.enable",
                        set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.convert.mbyte.enable",
                        set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.notenforced.url." + 
                        "attributes.enable", set);            

                set = new HashSet();
                set.add("5");
                map.put("com.sun.identity.agents.config.poll.primary.server",
                        set);            

                set = new HashSet();
                set.add("HTTP_");
                map.put("com.sun.identity.agents.config.profile.attribute." + 
                        "cookie.prefix", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.cookie.reset.enable",
                        set);            

                set = new HashSet();
                set.add("anonymous");
                map.put("com.sun.identity.agents.config.anonymous.user.id",
                        set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.local.log.rotate", set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.cookie.reset", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.proxy.override." + 
                        "host.port", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.postdata.preserve." + 
                        "enable", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.sso.only", set);            

                set = new HashSet();
                set.add("10");
                map.put("com.sun.identity.agents.config.postcache.entry." + "" +
                        "lifetime", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.ignore.server.check",
                        set);            

                set = new HashSet();
                set.add("1");
                map.put("com.sun.identity.agents.config.sso.cache.polling." + 
                        "interval", set);            

                set = new HashSet();
                set.add("session");
                map.put("com.sun.identity.agents.config.userid.param.type", 
                        set);            

                set = new HashSet();
                set.add("[0]=");
                map.put("com.sun.identity.agents.config.cdsso.cookie.domain",
                        set);            

                set = new HashSet();
                set.add("300");
                map.put("com.sun.identity.agents.config.profile.attribute." + 
                        "cookie.maxage", set);            

                set = new HashSet();
                set.add("HIGH");
                map.put("com.sun.identity.agents.config.iis.filter.priority"
                        , set);            

                set = new HashSet();
                set.add("10");
                map.put("com.sun.identity.agents.config.local.log.size", set);            

                set = new HashSet();
                set.add("iPlanetDirectoryPro");
                map.put("com.sun.identity.agents.config.cookie.name", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.fqdn.check.enable", 
                        set);

                set = new HashSet();
                set.add("0");
                map.put("com.sun.identity.agents.config.policy.clock.skew",
                        set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.client.ip." + 
                        "validation.enable", set);            

                set = new HashSet();
                set.add("en_US");
                map.put("com.sun.identity.agents.config.locale", set);            

                set = new HashSet();
                set.add("true");
                map.put("com.sun.identity.agents.config.get.client.host." + 
                        "name", set);            

                set = new HashSet();
                set.add("[]=");
                map.put("com.sun.identity.agents.config.fqdn.mapping", set);            

                set = new HashSet();
                set.add("UserToken");
                map.put("com.sun.identity.agents.config.userid.param", set);            

                set = new HashSet();
                set.add("false");
                map.put("com.sun.identity.agents.config.iis.owa.enable." + 
                        "change.protocol", set);            

                set = new HashSet();
                set.add("LtpaToken");
                map.put("com.sun.identity.agents.config.domino.ltpa." + 
                        "config.name", set);            

                set = new HashSet();
                set.add("LOG_NONE");
                map.put("com.sun.identity.agents.config.audit.accesstype", set);            

                set = new HashSet();
                set.add("LtpaToken");
                map.put("com.sun.identity.agents.config.domino.ltpa." + 
                        "cookie.name", set);            

                set = new HashSet();
                set.add("10000000");
                map.put("com.sun.identity.agents.config.debug.file.size", set);            

                set = new HashSet();
                set.add("5");
                map.put("com.sun.identity.agents.config.remote.log.interval",
                        set);            
            }
            amid = idmc.getFirstAMIdentity(admintoken, agentId, 
                    idmc.getIdType("agentonly"), "/");
            idmc.modifyIdentity(amid, map);
        } catch (Exception e) {
            log(Level.SEVERE, "create", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("create");
    }    
}
