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
 * $Id: AuthConfigurationModelImpl.java,v 1.3 2008/07/10 23:27:22 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.authentication.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AuthConfigurationEntry;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.sm.SMSException;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.sm.OrganizationConfigManager;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

/**
 * This class is responsible for handling authentication configuration 
 * operations such as creating/deleting configurations; updating the auth
 * instances that define the configuration, the success url, the failure
 * url, etc... After instantiating the class , <i>initialize</i> should be
 * invoked to set the name of the realm and the configuration begin acted on.
 */ 
public  class AuthConfigurationModelImpl extends AMModelBase
    implements AuthConfigurationModel {

    // TBD : Push these values to AMAuthConfigUtils
    private static final String REQUIRED = "REQUIRED";
    private static final String OPTIONAL = "OPTIONAL";
    private static final String SUFFICIENT = "SUFFICIENT";
    private static final String REQUISITE = "REQUISITE";

    // configuration entry attributes
    private static final String SUCCESS_URL = 
        "iplanet-am-auth-login-success-url";
    private static final String FAILURE_URL = 
        "iplanet-am-auth-login-failure-url";
    private static final String POST_PROCESS_CLASS = 
        "iplanet-am-auth-post-login-process-class";
    private static final String AUTH_CONFIG_ATTR = 
        "iplanet-am-auth-configuration";

    private static final int DEFAULT_PRIORITY = 0;

    public static final String CONFIG_NAME = "authConfigurationEntryName";
    private String currentRealm = null;
    private Map configData = null;
    private List entryList = null;
    private String xmlValue = null;
    private OrganizationConfigManager ocm = null;

    /**
     * Creates a model instance for configuring the core auth properties.
     *
     * @param req The <code>HttpServletRequest</code> object.
     * @param map of user information.
     */
    public AuthConfigurationModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
        currentRealm = (String)map.get(AMAdminConstants.CURRENT_REALM);
    }

    /**
     * Returns a map of the attributes that make up an auth config entry.
     * The map will contain the success and failure url lists, the post
     * processing class, and the list of auth modules that make up the 
     * auth configuration entry.
     *  
     * @return Map of attribute values.
     * @throws AMConsoleException if there is an error retrieving the data
     *    from the entry.
     */ 
    public Map getValues() throws AMConsoleException {
        return configData;
    }

    /** 
     * Returns all the named authentication configurations in the 
     * given realm.
     *
     * @param ssoToken Single Sign On token.
     * @param realmName where the configuration will be created
     * @return set of named authentication configurations
     */
    public static Set getNamedConfigurations(
        SSOToken ssoToken,
        String realmName
    ) {
        Set configurations = null;
        String errorMsg = null;
        try {
            configurations = AMAuthConfigUtils.getAllNamedConfig(
                realmName, ssoToken);
        } catch (SMSException e) { 
            debug.error("AuthConfigurationModel.getNamedConfigurations", e);
            errorMsg = e.getMessage();
        } catch (SSOException e) {
            debug.error("AuthConfigurationModel.getNamedConfigurations", e);
            errorMsg = e.getMessage();
        }

        if (errorMsg != null) {
            configurations = Collections.EMPTY_SET;
        }
         
        return configurations;
    }

    /** 
     * Creates a new named authentication configuration object. This object
     * will be used by the various objects for authentication.
     *
     * @param name used to reference the configuration.
     * @throws AMConsoleException if the configuration cannot be created.
     */
    public void createAuthConfiguration(String name)
        throws AMConsoleException 
    {
        if ((name == null) || (name.length() == 0)) {
            throw new AMConsoleException(
               getLocalizedString("authentication.config.missing.name"));
        }

        String[] params = {currentRealm, name};
        logEvent("ATTEMPT_CREATE_AUTH_CONFIG", params);

        String errorMsg = null;
        try {
            AMAuthConfigUtils.createNamedConfig(
                name, 0, new HashMap(), currentRealm, getUserSSOToken());
            logEvent("SUCCEED_CREATE_AUTH_CONFIG", params);
        } catch (SMSException e) {
            errorMsg = getErrorString(e);
            String[] paramsEx = {currentRealm, name, errorMsg};
            logEvent("SMS_EXCEPTION_CREATE_AUTH_CONFIG", paramsEx);
            debug.warning("problem creating auth instance", e);
        } catch (SSOException e) {
            errorMsg = getErrorString(e);
            String[] paramsEx = {currentRealm, name, errorMsg};
            logEvent("SSO_EXCEPTION_CREATE_AUTH_CONFIG", paramsEx);
            debug.warning("problem creating auth instance", e);
        } catch (AMConfigurationException e) {
            errorMsg = getErrorString(e);
            String[] paramsEx = {currentRealm, name, errorMsg};
            logEvent("AUTH_CONFIGURATION_EXCEPTION_CREATE_AUTH_CONFIG",
                paramsEx);
            debug.warning("problem creating auth instance", e);
        }

        // pass the error message back to the view bean...
        if (errorMsg != null) {
            throw new AMConsoleException(errorMsg);
        }
    }

    /**
     * Deletes the named authentication configuration object.
     *  
     * @param realm name where configuration is locate.
     * @param names names of entries.
     */
     public void deleteAuthConfiguration(String realm, Set names)
         throws AMConsoleException
    {
        StringBuilder errorList = new StringBuilder();
            String message = null;

        for (Iterator i=names.iterator(); i.hasNext(); ) {
            String config = (String)i.next();
            message = null;
            try {
                AMAuthConfigUtils.removeNamedConfig(
                    config, realm, getUserSSOToken());
            } catch(AMConfigurationException e) {
                debug.warning("failed to delete", e);
                message = e.getMessage();
                errorList.append(config);
            } catch (SMSException e) {
                debug.warning("failed to delete", e);
                message = e.getMessage();
                errorList.append(config);
            } catch (SSOException e) {
                debug.warning("failed to delete", e);
                message = e.getMessage();
                errorList.append(config);
            }
            if (message != null) {
                if (errorList.length() > 0) {
                    errorList.append(", ");
                }
            }
        }

        if (errorList.length() > 0) {
            String[] tmp = { errorList.toString(),  message };
            throw new AMConsoleException(MessageFormat.format(
                getLocalizedString("authentication.config.delete.failed"),
                (Object[])tmp) );
        }
    }

    /**
     * NOTE: This will be removed once the authconfig service is 
     * automagically registered when a realm is created.
     * verifying Auth config service exist for the current realm
     */
    private void verifyConfigurationService(String currentRealm) {
        if (ocm == null) {
            try {
                ocm = new OrganizationConfigManager(
                  getUserSSOToken(), currentRealm);
            } catch (SMSException sms) {
                debug.error("error getting config manager", sms);
            }
        }

        try {
            ocm.getServiceConfig(AMAdminConstants.AUTH_CONFIG_SERVICE);
        } catch (SMSException sms) {
            try {
                ocm.addServiceConfig(
                    AMAdminConstants.AUTH_CONFIG_SERVICE, new HashMap());
            } catch (SMSException x) {
                debug.message("the service is already registered");
            }
        }
    }

    /**
     * Sets the specified attributes in the auth config entry.  The data 
     * will not be committed to the data store until <code>store()</code>
     * is called.
     * 
     * @param data that will be set in the config entry.
     */
    public void setValues(Map data) {
        if (configData != null && !configData.isEmpty()) {
            configData.putAll(data);   
        }
    }

    /**
     * Returns the value for the post processing class set for the current 
     * configuration object. If the value is not present the emptry string
     * will be returned.
     *
     * @param realm name where configuration is locate.
     * @param config name of entry.
     * @return value of postprocessing class
     */
    public String getPostProcessingClass(String realm, String config) {
        initialize(realm, config);        
        String value = null;
        if ((configData != null) && (!configData.isEmpty()) ) {
            Set tmp = (Set)configData.get(POST_PROCESS_CLASS);
            if ((tmp != null) && (!tmp.isEmpty()) ) {
                value = (String)tmp.iterator().next();
            }
        }
        return (value == null) ? "" : value;
    }

    /**
     * Gets the module flag for the given module index in the current
     * authentication configuration attributes
     *
     * @param idx the index of the module to retrieve Flag from.
     * @return the module flag
     */
    public String getModuleFlag(int idx) {
        String flag = null;
        AuthConfigurationEntry entry = (AuthConfigurationEntry)
            entryList.get(idx);
        if (entry != null ) {
            flag = entry.getControlFlag();
        }
        return flag;
    }

    /**
     * Gets the module options string for the given module index in the
     * current authentication configuration attributes
     *
     * @param idx the index of the module to retrieve Flag from.
     * @return the module options string
     */
    public String getModuleOptions(int idx) {
        String options = null;
        AuthConfigurationEntry entry = (AuthConfigurationEntry)
            entryList.get(idx);
        if (entry != null) {
            options = entry.getOptions();
        }
        return options;
    }

    /**
     * Gets the module name for the given module index in the current
     * authentication configuration attributes
     *
     * @param idx the index of the module to retrieve name from.
     * @return the module name
     */
    public String getModuleName(int idx) {
        String name = null;
        AuthConfigurationEntry entry = (AuthConfigurationEntry)
            entryList.get(idx);
        if (entry != null) {
            name = entry.getLoginModuleName();
        }
        return name;
    }

    /**
     * Returns the number of entries in the current configuration.
     * 
     * @return number of entries
     */
    public int getNumberEntries() {
        return (entryList != null) ? entryList.size() : 0;
    }

    /**
     * Returns the xml blob that represents the auth entries which 
     * make up the specified auth configuration.
     *
     * @param realm name where configuration exists.
     * @param config name of the configuration object.
     * @return String xml blob.
     */
    public String getXMLValue(String realm, String config) {
        if ((xmlValue == null) || (xmlValue.length() < 1) ) {
            initialize(realm, config);
        }
        return xmlValue ;
    }

    /**
     * Sets the xml blob that represents the auth entries which 
     * make up the specified auth configuration.
     *
     * @param value of the xml blob.
     */
    public void setXMLValue(String value) {
        xmlValue = value;        

        entryList = new ArrayList(
            AMAuthConfigUtils.xmlToAuthConfigurationEntry(value));

        // set the xml blob in the data map
        Set s = new HashSet(2);
        s.add(value);
        configData.put(AUTH_CONFIG_ATTR, s);
    }

    /**
     * Remove the entries selected in the UI. This is done by creating a new
     * List of the selected entries, then removing that List from the 
     * original list of all the entries.
     *
     * @param entries array of index id's
     */
    public void removeAuthEntries(Integer[] entries) {
        List removeList = new ArrayList(entries.length * 2);
        for (int i=0; i < entries.length; i++ ) {
            removeList.add(entryList.get(entries[i].intValue()));
        }
        entryList.removeAll(removeList);
        xmlValue = 
            AMAuthConfigUtils.authConfigurationEntryToXMLString(entryList);
    }



    /**
     * Add a new entry to the configuration. Copy the last element in the list, 
     * and add it to the configuration. This is not permanent, the value still
     * needs to be stored/saved.
     *
     * @param newEntry
    public static String createXMLValue(List entries) {
        // entryList will be null if this is the first entry created.
        if (entryList == null) {
            entryList = new ArrayList();
        }
        entryList.add(newEntry);

        return AMAuthConfigUtils.authConfigurationEntryToXMLString(entries);
    }
     */

    /**
     * Stores locally the list of authentication entries for the auth config
     * object. 
     *
     * @param entries list of <code>AuthConfigurationEntry</code> objects.
     */
    public void setEntries(List entries) {
        if ((entries != null) && (!entries.isEmpty())) {
            entryList = entries;
            xmlValue =         
                AMAuthConfigUtils.authConfigurationEntryToXMLString(entryList);
            // set the xml blob in the data map
            Set s = new HashSet(2);
            s.add(xmlValue);
            configData.put(AUTH_CONFIG_ATTR, s);
        }
    }

    /** 
     * Sets the value for the post authentication properties class.
     *
     * @param className name of the class used for post authentication 
     *        processing
     */
    public void setPostAuthPropertiesClass(String className) {
        Set tmp = new HashSet(2);
        tmp.add(className);
        configData.put(POST_PROCESS_CLASS, tmp);
    }

    /**
     * Save the value for the given realm and configuration.
     *
     * @param realm name where configuration is locate.
     * @param config name of entry.
     */
    public void store(String realm, String config) 
        throws AMConsoleException
    {
        String errorMsg = null;
        String[] params = {realm, config};
        logEvent("ATTEMPT_MODIFY_AUTH_CONFIG_PROFILE", params);

        try {
            AMAuthConfigUtils.replaceNamedConfig(config, DEFAULT_PRIORITY,
                configData, realm, getUserSSOToken());
            logEvent("SUCCEED_MODIFY_AUTH_CONFIG_PROFILE", params);
        } catch (SSOException e) {
            errorMsg = getErrorString(e);
            String[] paramsEx = {realm, config, errorMsg};
            logEvent("SSO_EXCEPTION_MODIFY_AUTH_CONFIG_PROFILE", paramsEx);
        } catch (SMSException e) {
            errorMsg = getErrorString(e);
            String[] paramsEx = {realm, config, errorMsg};
            logEvent("SMS_EXCEPTION_MODIFY_AUTH_CONFIG_PROFILE", paramsEx);
        } catch (AMConfigurationException e) {
            errorMsg = getErrorString(e);
            String[] paramsEx = {realm, config, errorMsg};
            logEvent("AUTH_CONFIGURATION_EXCEPTION_MODIFY_AUTH_CONFIG_PROFILE",
                paramsEx);
        }

        if (errorMsg != null) {
            throw new AMConsoleException(errorMsg);
        }
    }

    /**
     * Used to restore the config data settings to their original values.
     *
     * @param realm name where configuration is locate.
     * @param config name of entry.
     */
    public void reset(String realm, String config) {
        configData = null;
        xmlValue = null;

        initialize(realm, config);
    }

    /**
     * Initialize configuration entry information for the given realm
     * and named configuration.
     *
     * @param realm name where configuration is locate.
     * @param config name of entry.
     */
    public void initialize(String realm, String config) {
        verifyConfigurationService(realm);

        try {
            if (configData == null) {
                String[] params = {realm, config};
                logEvent("ATTEMPT_GET_AUTH_CONFIG_PROFILE", params);
                configData = AMAuthConfigUtils.getNamedConfig(
                    config, realm, getUserSSOToken());
                logEvent("SUCCEED_GET_AUTH_CONFIG_PROFILE", params);
            }
        } catch(SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, config, strError};
            logEvent("SSO_EXCEPTION_GET_AUTH_CONFIG_PROFILE", paramsEx);
            debug.warning("AuthConfigurationModelImpl.initialize", e);
            configData = Collections.EMPTY_MAP;
        } catch(AMConfigurationException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, config, strError};
            logEvent("AUTH_CONFIGURATION_EXCEPTION_GET_AUTH_CONFIG_PROFILE",
                paramsEx);
            debug.error("AuthConfigurationModelImpl.initialize", e);
            configData = Collections.EMPTY_MAP;
        } catch(SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, config, strError};
            logEvent("SMS_EXCEPTION_GET_AUTH_CONFIG_PROFILE", paramsEx);
            debug.error("AuthConfigurationModelImpl.initialize", e);
            configData = Collections.EMPTY_MAP;
        }

        if ((configData != null) && !configData.isEmpty() && (xmlValue ==null)){
            Set tmp = (Set)configData.get(AUTH_CONFIG_ATTR);
            if ( (tmp != null) && (!tmp.isEmpty()) ) {
                xmlValue = (String)tmp.iterator().next();
                entryList = new ArrayList(
                    AMAuthConfigUtils.xmlToAuthConfigurationEntry(xmlValue));
            }
        }
    }

    /**
     * Returns a <code>Map</code> object containing the 
     * <code>AuthenticationEntry</code> criteria flags. The key is the value
     * and the display string is the value.
     *
     * @return map of criteria values.
     */
    public Map getCriteriaMap() {
        Map m = new HashMap(8);
        m.put(REQUIRED,
            getLocalizedString("authentication.config.required.label"));
        m.put(OPTIONAL,
            getLocalizedString("authentication.config.optional.label"));
        m.put(SUFFICIENT,
            getLocalizedString("authentication.config.sufficient.label"));
        m.put(REQUISITE,
            getLocalizedString("authentication.config.requisite.label"));

        return m;
    }
}
