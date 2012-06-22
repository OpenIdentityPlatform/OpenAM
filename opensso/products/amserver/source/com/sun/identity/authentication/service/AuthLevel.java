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
 * $Id: AuthLevel.java,v 1.3 2008/06/25 05:42:04 qcheng Exp $
 *
 */


package com.sun.identity.authentication.service;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.authentication.AuthContext;

/**
 * This class is representing authentication level for associated auth module.
 * The authentication level is set separately for each method of authentication.
 * The value indicates how much to trust an authentication. 
 * Once a user has authenticated, this value is stored in the SSO token 
 * for the session. When the SSO token is presented to an application the user 
 * wants to access, the application uses the stored value to determine whether 
 * the level is sufficient to grant the user access. 
 * If the authentication level stored in an SSO token does not meet the 
 * minimum value required, the application can prompt the user to authenticate 
 * again through a service with a higher authentication level. 
 * The default value is 0.
 */
public class AuthLevel {
    String indexName;
    String orgDN;
    private static AuthD ad = AuthD.getAuth();
    private static Debug debug = ad.debug;
    
    int authLevel = 0;
    int numberOfModules = 1;
    Vector modList;
    AuthContext.IndexType indexType = null;
    String clientType = AuthUtils.getDefaultClientType();
    java.util.Locale userLocale;

    private static AMResourceBundleCache amCache =
        AMResourceBundleCache.getInstance();
    java.util.ResourceBundle rb = null;
    Map moduleMap = null;

    /**
     * Class constructor
     * @param  indexType <code>IndexType</code> defines the possible kinds of 
     *         "objects" or "resources" for which an authentication 
     *         can be performed.
     * @param indexName authentication index name.
     * @param orgDN associated organizational DN for authentication module.
     * @param clientType associated client type for authentication module.
     * @param  loc associated locale instance for authentication module.
     * @exception AuthException if an error occurred during instanciation.
     */
    public AuthLevel(
        AuthContext.IndexType indexType,
        String indexName,
        String orgDN,
        String clientType,
        java.util.Locale loc
    ) throws AuthException {
        try {
            debug.message("in auth level constructor");
            this.indexName = indexName;
            this.indexType = indexType;
            this.orgDN = orgDN;
            this.clientType = clientType;
            userLocale = loc;
            try {
                authLevel = Integer.parseInt(indexName);
            } catch (Exception ee) {
                throw new AuthException(
                    AMAuthErrorCode.INVALID_AUTH_LEVEL, null);
            }
            //level = 0;
            if (debug.messageEnabled()) {
                debug.message("indexType : " + indexType);        
                debug.message("indexName : " + indexName);        
                debug.message("orgDN     : " + orgDN);        
                debug.message("clientType: " + clientType);        
                debug.message("authLevel : " + authLevel);        
                debug.message("userLocale: " + userLocale);
            }
                                
            rb = amCache.getResBundle(ad.BUNDLE_NAME,userLocale);
            getAuthModulesConfig();

            debug.message("end auth level constructor");
        } catch (Exception e) {
            throw new AuthException(e);
        }
    }

    /**
     * Returns auth modules configuration
     */
    public void getAuthModulesConfig() {
        /*
        Set levelModulesSet = (HashSet)
            AMAuthConfigUtils.getAuthModules(authLevel,orgDN,clientType);
        */
        Set levelModulesSet =
            AuthUtils.getAuthModules(authLevel,orgDN, clientType);

        modList = new Vector();
        moduleMap = new HashMap();
        int i = 0;
        Iterator iter = levelModulesSet.iterator();
        while (iter.hasNext()) {
            //get the localized value
            String moduleName = (String) iter.next();
            String localizedName = getModuleLocalizedName(moduleName);
            moduleMap.put(localizedName,moduleName);
            modList.addElement(localizedName);
            i++;
        }

        numberOfModules = i;
    }

    /**
     * Returns configured number of authentication modules
     * @return configured number of authentication modules
     */
    public int getNumberOfAuthModules() {
        return numberOfModules;
    }

    /**
     * Returns configured <code>List</code> of authentication modules
     * @return configured <code>List</code> of authentication modules
     */
    public String[] getModuleList() {
        String[] moduleList = new String[modList.size()];
        modList.copyInto((Object[])moduleList);
        return moduleList;
    }

    /**
     * Returns associated authentication module with auth level 
     * @return associated authentication module with auth level 
     */
    public String getModuleName() {
        Enumeration vList = modList.elements();
        return ((String)moduleMap.get(vList.nextElement())); 
    }

    /**
     * Returns choice callback. Gets module matching the level and
     * generates choice callback.
     *
     * @return choice callback.
     * @throws AuthException
     */
    public Callback[] createChoiceCallback() throws AuthException {
        debug.message("In createChoiceCallback");
        String[] moduleList = getModuleList();
        Callback[] callbacks = new Callback[1];

        try {        
            ChoiceCallback choiceCallback = 
                new ChoiceCallback(Locale.getString(rb,"modulePrompt",debug),
                    moduleList,0,false);
            callbacks[0] = choiceCallback;
        } catch (IllegalArgumentException ie) {
            debug.error("Number of arguments not correct",ie);
            throw new AuthException("callbackError", null);
        } catch (Exception e) {
            debug.error("Error: " , e);
            throw new AuthException("callbackError", null);
        }

        if (debug.messageEnabled()) {
            debug.message("Callback is.. :" + callbacks[0]);
        }
        return callbacks;        
    }

    /**
     * Returns localized name of a module.
     *
     * @param moduleName name of module.
     * @return localized name of a module.
     */
    protected String getModuleLocalizedName(String moduleName) {
        return Locale.getString(rb, moduleName, debug);
    }

    /**
     * Returns a map containing localized module name and 
     * module name.
     *
     * @return module map with key the localized module name
     *               and value the module name.
     */
    protected Map getModuleMap() {
        return moduleMap;
    }
}
