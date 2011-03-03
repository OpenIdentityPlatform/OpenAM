/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMAuthUtils.java,v 1.8 2009/01/09 02:24:56 madan_ranganath Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.util;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.common.DateUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;



/**
 * This class provides utility methods to Policy and Administration console
 * service to get realm qualified Authentication data.
 */
public class AMAuthUtils {
    private static Debug utilDebug = Debug.getInstance("amAMAuthUtils");
    
    private AMAuthUtils() {
    }
    
    /**
     * Returns the set of all authenticated Realm names.
     *
     * @param token valid user <code>SSOToken</code>
     * @return Set containing String values representing Realm names.
     * @throws SSOException if <code>token.getProperty()</code> fails.
     */
    public static Set getAuthenticatedRealms(SSOToken token)
    throws SSOException {
        Set returnRealms = new HashSet();
        String ssoRealm = token.getProperty(ISAuthConstants.ORGANIZATION);
        returnRealms.add(DNMapper.orgNameToRealmName(ssoRealm));
        Set realmsFromScheme =
        parseData(token.getProperty(ISAuthConstants.AUTH_TYPE), true);
        returnRealms.addAll(realmsFromScheme);
        Set realmsFromLevel =
        parseData(token.getProperty(ISAuthConstants.AUTH_LEVEL), true);
        returnRealms.addAll(realmsFromLevel);
        Set realmsFromService =
        parseData(token.getProperty(ISAuthConstants.SERVICE), true);
        returnRealms.addAll(realmsFromService);
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Realms from SSO Org : " + ssoRealm );
            utilDebug.message("Realms from Auth Type : " + realmsFromScheme );
            utilDebug.message("Realms from Auth Level : " + realmsFromLevel );
            utilDebug.message("Realms from Service : " + realmsFromService );
            utilDebug.message("Return getAuthenticatedRealms : "
            + returnRealms);
        }
        return returnRealms;
    }
    
    /**
     * Returns the set of all authenticated Scheme names.
     *
     * @param token valid user <code>SSOToken</code>
     * @return Set containing String values representing Scheme names.
     * @throws SSOException if <code>token.getProperty()</code> fails.
     */
    public static Set getAuthenticatedSchemes(SSOToken token)
    throws SSOException {
        return (parseData(token.getProperty(ISAuthConstants.AUTH_TYPE), false));
    }
    
    /**
     * Returns the set of all authenticated Service names.
     *
     * @param token valid user <code>SSOToken</code>
     * @return Set containing String values representing Service names.
     * @throws SSOException if <code>token.getProperty()</code> fails.
     */
    public static Set getAuthenticatedServices(SSOToken token)
    throws SSOException {
        return (parseData(token.getProperty(ISAuthConstants.SERVICE), false));
    }
    
    /**
     * Returns the set of all authenticated levels.
     *
     * @param token valid user <code>SSOToken</code>
     * @return Set containing String values representing levels.
     * @throws SSOException if <code>token.getProperty()</code> fails.
     */
    public static Set getAuthenticatedLevels(SSOToken token)
    throws SSOException {
        return (parseData(token.getProperty(ISAuthConstants.AUTH_LEVEL),false));
    }
    
    /**
     * Returns the set of all authenticated realm qualified scheme names.
     *
     * @param token valid user <code>SSOToken</code>
     * @return Set containing String values representing
     * realm qualified scheme names.
     * @throws SSOException if <code>token.getProperty()</code> fails.
     */
    public static Set getRealmQualifiedAuthenticatedSchemes(SSOToken token)
    throws SSOException {
        return (parseRealmData(token.getProperty(ISAuthConstants.AUTH_TYPE),
            token.getProperty(ISAuthConstants.ORGANIZATION)));
    }
    
    /**
     * Returns the set of all authenticated realm qualified service names.
     *
     * @param token valid user <code>SSOToken</code>
     * @return Set containing String values representing
     * realm qualified service names.
     * @throws SSOException if <code>token.getProperty()</code> fails.
     */
    public static Set getRealmQualifiedAuthenticatedServices(SSOToken token)
    throws SSOException {
        return (parseRealmData(token.getProperty(ISAuthConstants.SERVICE),
            token.getProperty(ISAuthConstants.ORGANIZATION)));
    }
    
    /**
     * Returns the set of all authenticated realm qualified authentication
     * levels.
     *
     * @param token valid user <code>SSOToken</code>
     * @return Set containing String values representing
     * realm qualified authentication levels.
     * @throws SSOException if <code>token.getProperty()</code> fails.
     */
    public static Set getRealmQualifiedAuthenticatedLevels(SSOToken token)
    throws SSOException {
        return (parseRealmData(token.getProperty(ISAuthConstants.AUTH_LEVEL),
            token.getProperty(ISAuthConstants.ORGANIZATION)));
    }
    
    /**
     * Returns the given data in Realm qualified format.
     *
     * @param realm valid Realm
     * @param data data which qualifies for Realm qualified data. This could
     * be authentication scheme or authentication level or service.
     * @return String representing realm qualified authentication data.
     */
    public static String toRealmQualifiedAuthnData(String realm, String data) {
        String realmQualifedData = data;
        if (realm != null && realm.length() != 0) {
            realmQualifedData = 
                realm.trim() + ISAuthConstants.COLON + data.trim();
        }
        return realmQualifedData;
    }
    
    /**
     * Returns the Realm name from Realm qualified data.
     *
     * @param realmQualifedData Realm qualified data. This could be Realm
     * qualified authentication scheme or authentication level or service.
     * @return String representing realm name.
     */
    public static String getRealmFromRealmQualifiedData(
    String realmQualifedData) {
        String realm = null;
        if (realmQualifedData != null && realmQualifedData.length() != 0) {
            int index = realmQualifedData.indexOf(ISAuthConstants.COLON);
            if (index != -1) {
                realm = realmQualifedData.substring(0, index).trim();
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("realmQualifedData : " + realmQualifedData );
            utilDebug.message("RealmFromRealmQualifiedData : " + realm );
        }
        return realm;
    }
    
    /**
     * Returns the data from Realm qualified data. This could be authentication
     * scheme or authentication level or service.
     *
     * @param realmQualifedData Realm qualified data. This could be Realm
     * qualified authentication scheme or authentication level or service.
     * @return String representing data. This could be authentication
     * scheme or authentication level or service.
     */
    public static String getDataFromRealmQualifiedData(
    String realmQualifedData){
        String data = null;
        if (realmQualifedData != null && realmQualifedData.length() != 0) {
            int index = realmQualifedData.indexOf(ISAuthConstants.COLON);
            if (index != -1) {
                data = realmQualifedData.substring(index + 1).trim();
            } else {
                data = realmQualifedData;
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("realmQualifedData : " + realmQualifedData );
            utilDebug.message("DataFromRealmQualifiedData : " + data );
        }
        return data;
    }
    
    /**
     * Returns the set of all authenticated Realm names or Scheme names or
     * levels or Service names.
     *
     * @param data Realm qualified data. This could be Realm
     * qualified authentication scheme or authentication level or service.
     * @param realm Boolean indicator to get Realm names if true; otherwise
     * get schemes or levels or services names.
     * @return the set of all authenticated Realm names or Scheme names or
     * levels or Service names.
     */
    private static Set parseData(String data, boolean realm) {
        Set returnData = Collections.EMPTY_SET;
        if (data != null && data.length() != 0) {
            StringTokenizer stz = new StringTokenizer(data,
            ISAuthConstants.PIPE_SEPARATOR);
            returnData = new HashSet();
            while (stz.hasMoreTokens()) {
                String nameValue = (String)stz.nextToken();
                int index = nameValue.indexOf(ISAuthConstants.COLON);
                if ((index == -1) && (realm)){
                    continue;
                } else if (index == -1) {
                    returnData.add(nameValue);
                    continue;
                }
                String name = nameValue.substring(0, index).trim();
                String value = nameValue.substring(index + 1).trim();
                if (realm) {
                    returnData.add(name);
                } else {
                    returnData.add(value);
                }
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("parseData:Input data : " + data );
            utilDebug.message("parseData:returnData : " + returnData );
        }
        return returnData;
    }
    
    /**
     * Returns the set of all authenticated realm qualified Scheme names or
     * levels or Service names.
     *
     * @param data Realm qualified data. This could be Realm
     * qualified authentication scheme or authentication level or service.
     * @param orgDN SSOToken's org DN.
     * @return the set of all authenticated realm qualified Scheme names or
     * levels or Service names.
     */
    private static Set parseRealmData(String data, String orgDN) {
        Set returnData = Collections.EMPTY_SET;
        String realm = DNMapper.orgNameToRealmName(orgDN);
        if (data != null && data.length() != 0) {
            StringTokenizer stz = new StringTokenizer(data,
            ISAuthConstants.PIPE_SEPARATOR);
            returnData = new HashSet();
            while (stz.hasMoreTokens()) {
                String realmData = (String)stz.nextToken();
                if (realmData != null && realmData.length() != 0) {
                    int index = realmData.indexOf(ISAuthConstants.COLON);
                    if (index == -1) {
                        realmData = toRealmQualifiedAuthnData(realm, realmData);
                   }
                   returnData.add(realmData);
                }
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("parseRealmData:Input data : " + data );
            utilDebug.message("parseRealmData:returnData : " + returnData );
        }
        return returnData;
    }
    
     /**
     * Returns a <code>Map<code> with all Auth Module instance names as key
     * and the time the module was authenticated as value.
     *
     * @param ssoToken valid user's single sign on token.
     * @return Map containing module instace auth time.
     */
    public static Map getModuleAuthTimeMap(SSOToken ssoToken) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AMAuthUtils.getModuleAuthTimeMap : ssoToken = "
                +ssoToken.getTokenID());
        }
        String moduleAuthTime = null;
        try {
            moduleAuthTime = ssoToken.getProperty(ISAuthConstants.
                MODULE_AUTH_TIME);
        } catch (SSOException ssoExp) {
            utilDebug.warning("AMAuthUtils.getModuleAuthTimeMap :" 
                 + "Cannot get Module Auth Time from SSO Token");
        }
        Map moduleTimeMap = new HashMap();
        if ((moduleAuthTime == null) || (moduleAuthTime.length()==0)) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AMAuthUtils.getModuleAuthTimeMap : " + 
                    "MODULE_AUTH_TIME not set in SSO Token ");
            }
            try {
                String authType = ssoToken.getProperty(ISAuthConstants.
                    AUTH_TYPE);
                String authInstant = ssoToken.getProperty(
                    ISAuthConstants.AUTH_INSTANT);
                StringTokenizer tokenizer = new StringTokenizer(authType,
                    ISAuthConstants.PIPE_SEPARATOR);
                while (tokenizer.hasMoreTokens()) {
                    String moduleName = (String)tokenizer.nextToken();
                    moduleTimeMap.put(moduleName, authInstant);
                }
            } catch (SSOException ssoExp) {
                utilDebug.error("AMAuthUtils.getModuleAuthTimeMap : Cannot "
                    + "get Auth type/instant from SSO Token", ssoExp);
            }
        } else {
            StringTokenizer tokenizer = new StringTokenizer(moduleAuthTime,
                ISAuthConstants.PIPE_SEPARATOR);
            while (tokenizer.hasMoreTokens()) {
                StringTokenizer elemToken = new StringTokenizer((String)
                    tokenizer.nextToken(), "+");
                while (elemToken.hasMoreTokens()) {
                    String moduleName = (String)elemToken.nextToken();
                    String authTime = (String)elemToken.nextToken();
                    moduleTimeMap.put(moduleName, authTime);
                }
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AMAuthUtils.getModuleAuthTimeMap : "
                + "moduleTimeMap " + "= " + moduleTimeMap);
        }
        return moduleTimeMap;
    }
    
    /**
     * Returns time at which the particular authentication occured
     * @param ssoToken valid user <code>SSOToken</code>
     * @param authType valid Authentication Type.
     * @param authValue valid Authentication value.
     * @return long value of authentication time.
     */
    public static long getAuthInstant(SSOToken ssoToken, String authType,
        String authValue){
        // Refreshing the SSOToken
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            manager.refreshSession(ssoToken);
        } catch (SSOException ssoExp) {
            utilDebug.warning("AMAuthUtils.getAuthInstant : Cannot refresh "
                + "the SSO Token");
        }
        long retTime = 0;
        AuthContext.IndexType indexType = AuthUtils.getIndexType(authType);
        if (indexType == AuthContext.IndexType.MODULE_INSTANCE) {
            Map moduleTimeMap = getModuleAuthTimeMap(ssoToken);
            String strDate = (String) moduleTimeMap.get(authValue);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AMAuthUtils.getAuthInstant : "
                    + "date from getAuthInstant = " + strDate);
            }
            if ((strDate != null) && (strDate.length() != 0)) {
                Date dt = null;
                try {
                    dt = DateUtils.stringToDate(strDate);
                } catch (java.text.ParseException parseExp) {
                    utilDebug.message("AMAuthUtils.getAuthInstant : "
                        + "Cannot parse Date");
                }
                if (dt != null) {
                    retTime = dt.getTime();
                }
            }
        }
        return retTime;
    }

    /**
     * Returns the list of configured module instances that could be
     * used by HTTP Basic
     * @param realmName  Realm Name
     * @return the list of configured module instances that could be
     * used by HTTP Basic
     */
    public static List getModuleInstancesForHttpBasic(String realmName) {
       
        List moduleInstances = new ArrayList();

        addModInstanceNames(realmName,"DataStore", moduleInstances);
        addModInstanceNames(realmName,"LDAP", moduleInstances);
        addModInstanceNames(realmName,"AD", moduleInstances);
        addModInstanceNames(realmName,"JDBC", moduleInstances);
        
        //return the choice values map
        return (moduleInstances);
    }

    private static void addModInstanceNames(String realmName,
                                            String moduleType,
                                            List modInstances) {
        try {
            SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            AMAuthenticationManager amAM = new
                AMAuthenticationManager(adminToken, realmName);
            Set instanceNames = amAM.getModuleInstanceNames(moduleType);
            modInstances.addAll(instanceNames);
           } catch (AMConfigurationException exp) {
            utilDebug.error("AMAuthUtils.addModInstanceNames: Error while"
                            + " trying to get auth module instance names " +
                            "for auth type" + moduleType);
        }
    }
}
