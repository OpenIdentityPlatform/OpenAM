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
 * $Id: Gateway.java,v 1.6 2009/05/26 08:02:07 kiran_gonipati Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.util;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.share.AuthXMLTags;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.locale.L10NMessageImpl;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.policy.ActionDecision;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.ProxyPolicyEvaluatorFactory;
import com.sun.identity.policy.ProxyPolicyEvaluator;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.OrganizationConfigManager;


/**
 * The class determines the authentication module required for an URL
 * and forwards the request to the login URL
 */
public class Gateway extends HttpServlet {

    ServletConfig config = null;    
    /** Initializes the servlet.
    */  
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.config = config;
        
        try {
            gwServletUtilsMap = new HashMap();
            authD = AuthD.getAuth();
            defToken = authD.getSSOAuthSession();
            defTokenMgr = SSOTokenManager.getInstance();
                        
            // Retrieve all available auth services
            Iterator auths = authD.getAuthenticators();
            while (auths.hasNext()) {
                 String auth = (String) auths.next();
                 authenticators.add(auth);
            }
                        
            initGWServletUtilsMap(SMSEntry.getRootSuffix());
            actionNames.add(GET);
            actionNames.add(POST);
            pe = ProxyPolicyEvaluatorFactory.getInstance()
                .getProxyPolicyEvaluator(defToken, WEB_AGENT_SERVICE_NAME);
        } catch(Exception e) {
            debug.error("GatewayServlet: Unable to create PolicyEvaluator", e);
            throw new ServletException(e.getMessage());
        }
    }

    /**
     * Performs the HTTP GET operation. 
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * Performs the HTTP POST operation. 
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        // Obtain goto URL and check if there are auth parameters
        String authScheme = null;
        String authLevel = null;
        String gotoUrl = null;
        ActionDecision ad = null;
        Map advices = null;
        String orgName = null;

        // Check content length
        try {
            RequestUtils.checkContentLength(request);
        } catch (L10NMessageImpl e) {
            ISLocaleContext localeContext = new ISLocaleContext();
            localeContext.setLocale(request);
            java.util.Locale locale = localeContext.getLocale();
            if (debug.messageEnabled()) {
                debug.message("GatewayServlet: " + e.getL10NMessage(locale));
            }
            throw new ServletException(e.getL10NMessage(locale));
        }

        // Construct the default forwarding URL
        StringBuilder forwardUrl = new StringBuilder(200);
        forwardUrl.append(LOGIN_URL);
                
        String queryString = request.getQueryString();
        Enumeration paramNames = request.getParameterNames();
        while ((queryString != null) && paramNames.hasMoreElements()) {
             String key = (String) paramNames.nextElement();
             if (key.equalsIgnoreCase(GOTO_URL)) {
                 gotoUrl = request.getParameter(key);
             }
             else if (key.equalsIgnoreCase(AUTH_SCHEME)) {
                 authScheme = request.getParameter(key);
             }
             else if (key.equalsIgnoreCase(AUTH_LEVEL)) {
                 authLevel = request.getParameter(key);
             }
        }

        if (debug.messageEnabled()) {
             debug.message("GatewayServlet: queryString : "+queryString);
             debug.message("GatewayServlet: gotoUrl : "+gotoUrl);
        } 
                
        if (gotoUrl != null) {
            ad = getActionDecision(gotoUrl);
            if (ad != null) {
                advices = ad.getAdvices();
                orgName = getOrgNameFromAdvice(advices);
            }
        }
                        
        AuthServiceConfigInfo info = null;
        // Construct the forward URL
        if ((gotoUrl != null) && 
            ((authScheme == null) && (authLevel == null))) {
            if (debug.messageEnabled()) {
                debug.message("GatewayServlet: gotoUrl : "+gotoUrl);
            } 

            // we have only goto URL, hence find from policy if there are
            // any advices on authentication modules
            forwardUrl.append('?').append(queryString);
            String advice = getPolicyAdvice(ad) ;   
 
            info = getGWServletUtilsFromMap(advices);
            if (advice != null) {
                StringBuffer adv = new StringBuffer();
                int index1 = advice.indexOf("=");
                if (index1 != -1) {
                    adv = adv.append(advice.substring(0 , index1 + 1));
                    int index2 = advice.indexOf(":");
                    if (index2 != -1) {
                        orgName = advice.substring(index1 + 1 , index2);
                        adv = adv.append(advice.substring(index2 + 1));
                        advice = adv.toString();
                    }
                }
            }

            if (debug.messageEnabled()) {
                debug.message("GatewayServlet: advice from getPolicyAdvice(): "
                    + advice);
            }
                
            if (advice != null && advice.length() > 0) {
                forwardUrl.append('&').append(advice);
            }
        } else if ((authScheme != null) || (authLevel != null)) {
            // Either query string contains goto url & auth parameters
            // which could be auth level or module, or no goto url
            forwardUrl.append('?').append(queryString);
            if (authScheme != null) {
                info = getGWServletUtilsByScheme(orgName, authScheme);
            } else if (authLevel != null) {
                info = getGWServletUtilsByLevel(orgName, authLevel);
            }
        }
        // If module is Cert, redirect to Cert module URL
        String fUrl = forwardUrl.toString();

        if (debug.messageEnabled()) {
            debug.message("GatewayServlet >>> Need to change URL !");
            debug.message("OLD URL : "+fUrl);
        } 

        if ((info != null) && (info.getPortNumber() != null)) {
            fUrl = CERT_PROTOCOL + request.getServerName() +
                   ":" + info.getPortNumber() + SystemProperties.get(
                    AuthXMLTags.SERVER_DEPLOY_URI) + fUrl;
                        
            if ((orgName != null) && (fUrl.indexOf("org=") == -1)) {
                fUrl = fUrl + "&"+ORG_NAME+"=" + DNtoName(orgName);
            }

            response.sendRedirect(fUrl);                        
        } else {
            // Forward the request to Login servlet
            if ((orgName != null) && (fUrl.indexOf("org=") == -1)) {
                fUrl = fUrl + "&"+ORG_NAME+"=" + DNtoName(orgName);
            }
            // Forward the request to Login servlet
            RequestDispatcher dispatcher =
                config.getServletContext().getRequestDispatcher(fUrl);
            dispatcher.forward(request, response);
        }
        if (debug.messageEnabled()) {
            debug.message("New URL : "+fUrl);
        } 
    }

    String getPolicyAdvice(ActionDecision ad) {
        StringBuffer answer = new StringBuffer(30);
        processActionDecision(ad, answer);
        return (answer.toString().trim());
    }

    boolean processActionDecision(ActionDecision ad, StringBuffer answer) {
        Map advices;
        if (ad == null) {
            // Problem is policy evaluation?
            return (false);
        }
        // Check is the resource is allowed
        Set values = ad.getValues();
        if (values.contains(ALLOW)) {
            return (true);
        } else if ((advices = ad.getAdvices()) != null) {
            if (debug.messageEnabled()) {
                debug.message("GatewayServlet: processActionDecision : " 
                                 + advices.values().toString());
            }       
                
            if ((appendAdvice(AUTH_SCHEME, 
                             (Set) advices.get(AUTH_SCHEME_ADVICE), answer) ||
                 appendAdvice(AUTH_LEVEL, 
                             (Set) advices.get(AUTH_LEVEL_ADVICE), answer))) {
                return (true);
            }
        }
        return (false);
    }

    boolean appendAdvice(String prefix, Set advices, StringBuffer answer) {
        if (advices != null) {
            Iterator items = advices.iterator();
            if (items.hasNext()) {
                String item = (String) items.next();
                if (answer.length() != 0) {
                    answer.append('&');
                }
                        
                answer.append(prefix).append('=').append(item);
                return (true);
            }
        }
        return (false);
    }

    private GatewayServletUtils initGWServletUtilsMap(String orgName) {
        GatewayServletUtils utils = null;
        OrganizationConfigManager orgConfigMgr = 
            authD.getOrgConfigManager(orgName);
                
        try {
            Set registeredServices = orgConfigMgr.getAssignedServices();
            Iterator iter = registeredServices.iterator();
            while (iter.hasNext()) {
                 String service = (String)iter.next();
                 if (service.trim().indexOf("iPlanetAMAuth") == 0) {
                     int idx = service.lastIndexOf("Service");
                     String module = null;
                     if (idx > "iPlanetAMAuth".length()) {
                         module = service.substring("iPlanetAMAuth".length(), 
                            idx);
                     } else {
                         continue;
                     }
                     if (authenticators.contains(module)) {
                         utils = addGWServletUtilsToMap(orgName, module);
                     }
                }
            }
        } catch (Exception e) {
            debug.error("Error in GatewayServlet:initGWServletUtilsMap()");
            debug.error("", e);
        }
        return utils;
    }               

    private GatewayServletUtils addGWServletUtilsToMap
            (String orgName, String module) {
        GatewayServletUtils utils = null;
        String authService= AMAuthConfigUtils.getModuleServiceName(module);

        try {
            ServiceConfigManager scm = 
                      new ServiceConfigManager(authService, defToken);
            utils = new GatewayServletUtils(scm, module);
            utils.organizationConfigChanged (orgName);
            AuthServiceConfigInfo  info = utils.getAuthConfigInfo(orgName);
            if ((info != null) && (info.getPortNumber() != null)) {
                scm.addListener(utils);
                gwServletUtilsMap.put(authService, utils);
            } else {
                gwServletUtilsMap.put(authService, utils=null);
            }                       
        } catch(Exception e) {
            debug.error("GatewayServlet: "+
                        "Unable to add Auth Service Info : "+authService, e);
        }
        return utils;
    }  
              
    private AuthServiceConfigInfo getGWServletUtilsFromMap(Map advices) {
        String orgName = null;
        String module = null;
        String level = null;
        AuthServiceConfigInfo info = null;
                        
        if (advices != null) {
            orgName = getOrgNameFromAdvice(advices);        
            if ((module = getAuthSchemeFromAdvice(advices)) != null) {
                info = getGWServletUtilsByScheme(orgName, module);
            } else if ((level = getAuthLevelFromAdvice(advices)) != null) {
                info = getGWServletUtilsByLevel(orgName, level);
            }
        }

        return info;
    }  

    private ActionDecision getActionDecision(String url) {
        ActionDecision ad = null;
        if (pe != null) {
            PolicyDecision pd = null;
            try {
                HashMap envParameters = new HashMap();
                pd = pe.getPolicyDecisionIgnoreSubjects(url, actionNames, 
                    envParameters);
            } catch (Exception e) {
                debug.error("GatewayServlet: Error in getting policy decision.",
                    e);
                return (null);
            }
            Map actionDecisions = pd.getActionDecisions();
            if (actionDecisions != null) {
                if ((ad = (ActionDecision) actionDecisions.get(GET)) == null) {
                    ad = (ActionDecision) actionDecisions.get(POST);
                }
            }
        }
                
        return ad;
    }

    private String getOrgNameFromAdvice(Map advices) {
        String orgName = null;
                
        if (advices != null) {
            Set advice = 
                (Set)advices.get(PolicyEvaluator.ADVICING_ORGANIZATION); 
            if (advice != null) {
                Iterator items = advice.iterator();
                if (items.hasNext()) {
                    orgName = (String) items.next();
                }
            }
        }

        if (debug.messageEnabled()) {
            debug.message("GatewayServlet:getOrgName() : " + orgName);
        }       
        return (orgName);
    }

    private String getAuthSchemeFromAdvice(Map advices) {
        String authScheme = null;
                
        Set advice = (Set)advices.get(AUTH_SCHEME_ADVICE);      

        if (advice != null) {
            Iterator items = advice.iterator();
            if (items.hasNext()) {
                authScheme = (String) items.next();
            }
        }
        if (debug.messageEnabled()) {
            debug.message("GatewayServlet:getAuthScheme() : " + authScheme);
        }       
        return (authScheme);
    }

    private String getAuthLevelFromAdvice(Map advices) {
        String authLevel = null;
                
        Set advice = (Set)advices.get(AUTH_LEVEL_ADVICE);       

        if (advice != null) {
            Iterator items = advice.iterator();
            if (items.hasNext()) {
                authLevel = (String) items.next();
            }
        }
        if (debug.messageEnabled()) {
            debug.message("GatewayServlet:getAuthLevel() : " + authLevel);
        }       
        return (authLevel);
    }

    private AuthServiceConfigInfo getGWServletUtilsByScheme(
                                    String orgName, String scheme) {
        AuthServiceConfigInfo info = null;
        GatewayServletUtils util = null;
        String authService= AMAuthConfigUtils.getModuleServiceName(scheme);
        if (debug.messageEnabled()) {
            debug.message("GatewayServlet:getGWServletUtilsByScheme()");
            debug.message("OrgName : "+orgName);
            debug.message("Auth Scheme : "+scheme);
        }       

        util = (GatewayServletUtils) gwServletUtilsMap.get(authService); 
        if (util != null) {
            info = util.getAuthConfigInfo(orgName);
        }
                
        return info;
    }                               

    private AuthServiceConfigInfo getGWServletUtilsByLevel(
                                      String orgName, String level) {
        AuthServiceConfigInfo info = null;
        GatewayServletUtils util = null;
        Set keyset = gwServletUtilsMap.keySet();
        Iterator keys = keyset.iterator();
        if (debug.messageEnabled()) {
            debug.message("GatewayServlet:getGWServletUtilsByLevel()");
            debug.message("OrgName : "+orgName);
            debug.message("Auth Level : "+level);
            debug.message("No of entries in  GWServletUtilsMap : "
                +gwServletUtilsMap.size());                       
        }       
                
        while (keys.hasNext()) {
            String service = (String) keys.next();
            util = (GatewayServletUtils) gwServletUtilsMap.get(service);
                        
            if (util == null)
                continue;
                       
            if ((util.getAuthLevel(orgName) != null) &&
                 util.getAuthLevel(orgName).equals(level)) {
                 info = util.getAuthConfigInfo(orgName);
                 break;
            }
        }
                
        return info;
    }                               

    String DNtoName(String dn) {
        String ret = null;
        int a1 = dn.indexOf("=");
        int i2 = dn.indexOf(",");
        if (i2 == -1) {
            ret = dn.substring(a1+1).trim();
        }
        else {
            ret = dn.substring(a1+1, i2).trim();
        }
        return ret;
    }

    // Static variables
    private static final String GOTO_URL = "goto";
    private static final String LOGIN_URL = "/UI/Login";
    private static final String AUTH_LEVEL_ADVICE = "AuthLevelConditionAdvice";
    private static final String AUTH_SCHEME_ADVICE = 
        "AuthSchemeConditionAdvice";
    private static final String ORG_NAME = "org";
    private static final String AUTH_SCHEME = "module";
    private static final String AUTH_LEVEL = "authlevel";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String ALLOW = "allow";
    private static final String CERT_PROTOCOL = "https://";
    private static final String CERT_SCHEME = "Cert";
    private static final String WEB_AGENT_SERVICE_NAME = 
        "iPlanetAMWebAgentService";
    private static final String CERT_SERVICE_NAME = "iPlanetAMAuthCertService";

    private static Debug debug = Debug.getInstance("amGateway");
    private static AuthD authD;
    private static SSOTokenManager defTokenMgr;
    private static SSOToken defToken;
    private static ProxyPolicyEvaluator pe;
    private static HashMap gwServletUtilsMap;
    private static HashSet authenticators = new HashSet();
    private static Set actionNames = new HashSet();
}
