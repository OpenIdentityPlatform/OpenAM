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
 * $Id: CompositeAdvices.java,v 1.6 2008/08/19 19:08:53 veiming Exp $
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

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

import com.sun.identity.policy.plugins.AuthSchemeCondition;
import com.sun.identity.policy.plugins.AuthenticateToServiceCondition;
import com.sun.identity.policy.plugins.AuthenticateToRealmCondition;

/**
 * This class allows the authentication services of OpenSSO to 
 * decouple the advice handling mechanism of the agents. 
 * This allows user to introduce and manage custom advices by solely 
 * writing OpenSSO side plug-ins. 
 * Users are not required to make changes on the agent side. 
 * Such advices are honored automatically by the composite advice 
 * handling mechanism. A benefit of composite advice is that you can 
 * incorporate a custom advice type without having to make changes to an 
 * agent deployment.  
 */
public class CompositeAdvices {
    String indexName;
    String orgDN;
    private static AuthD ad = AuthD.getAuth();
    private static Debug debug = ad.debug;
    int numberOfModules = 0;
    Vector modList;
    String clientType = AuthUtils.getDefaultClientType();
    java.util.Locale userLocale;
    private static AMResourceBundleCache amCache =
        AMResourceBundleCache.getInstance();
    java.util.ResourceBundle rb = null;
    Map moduleMap = null;
    int type;

    /**
     * Default class constructor for class
     * @param indexName authentication index name.
     * @param orgDN associated organizational DN for authentication module.
     * @param clientType associated client type for authentication module.
     * @param  loc associated locale instance for authentication module.
     * @exception AuthException if an error occurred during instanciation.
     */
    public CompositeAdvices(
        String indexName,
        String orgDN,
        String clientType,
        java.util.Locale loc
    ) throws AuthException {
        try {
            debug.message("in CompositeAdvices constructor");
            this.indexName = indexName;
            this.orgDN = orgDN;
            this.clientType = clientType;
            userLocale = loc;
            
            if (debug.messageEnabled()) {
                debug.message("indexName : " + indexName);
                debug.message("orgDN     : " + orgDN);
                debug.message("clientType: " + clientType);
                debug.message("userLocale: " + userLocale);
            }
            
            rb = amCache.getResBundle(ad.BUNDLE_NAME,userLocale);
            Map authInstances = 
            AuthUtils.processCompositeAdviceXML(indexName,orgDN,clientType);
            Set moduleInstances = null;
            if (authInstances.get(
                AuthenticateToRealmCondition.
                    AUTHENTICATE_TO_REALM_CONDITION_ADVICE) != null) {
                this.type = AuthUtils.REALM;
                moduleInstances = (Set)authInstances.get(
                    AuthenticateToRealmCondition.
                        AUTHENTICATE_TO_REALM_CONDITION_ADVICE);
            } else if (authInstances.get(
                AuthenticateToServiceCondition.
                    AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE) != null) {
                this.type = AuthUtils.SERVICE;
                moduleInstances = (Set)authInstances.get(
                    AuthenticateToServiceCondition.
                        AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE);
            } else if (authInstances.get(
                AuthSchemeCondition.AUTH_SCHEME_CONDITION_ADVICE) != null) {
                this.type = AuthUtils.MODULE;
                moduleInstances = (Set)authInstances.get(
                    AuthSchemeCondition.AUTH_SCHEME_CONDITION_ADVICE);
            }
            if ((moduleInstances != null) && (!moduleInstances.isEmpty())) {
                getAuthModulesConfig(moduleInstances);
            }
            debug.message("end CompositeAdvices constructor");
        } catch (Exception e) {
            throw new AuthException(e);
        }
    }
    
    private void getAuthModulesConfig(Set returnModuleInstances) {
        modList = new Vector();
        moduleMap = new HashMap();
        int i = 0;
        Iterator iter = returnModuleInstances.iterator();
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
    
    private String[] getModuleList() {
        String[] moduleList = new String[modList.size()];
        modList.copyInto((Object[])moduleList);
        
        return moduleList;
    }

    /**
     * Returns configured <code>List</code> of authentication modules
     * @return configured <code>List</code> of authentication modules
     */
    public String getModuleName() {
        Enumeration vList = modList.elements();
        return ((String)moduleMap.get(vList.nextElement()));
    }
    
    /**
     * Returns array of choice callback.
     * Get module matching the level and generate choice callback.
     * @return array of choice callback.
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
     * Returns a map containing localized module name and module name.
     *
     * @return module map with key the localized module name
     *         and value the module name.
     */
    protected Map getModuleMap() {
        return moduleMap;
    }

    /** 
     * Returns a type indicating the type of authentication required.
     *
     * @return an integer type indicating the type of authentication required.
     */
    protected int getType() {
        return type;
    }   

}
