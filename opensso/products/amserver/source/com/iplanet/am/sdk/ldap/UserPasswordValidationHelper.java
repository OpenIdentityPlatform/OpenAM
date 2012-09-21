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
 * $Id: UserPasswordValidationHelper.java,v 1.3 2008/06/25 05:41:26 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.ldap;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.AMServiceUtils;
import com.iplanet.am.sdk.AMUserPasswordValidation;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * This class provides the some of functionality for generating CallBacks for
 * plugins implementing the <code>AMUserPasswordValidation</code> class. It
 * primarily servers as a wrapper to the AMUserPasswordValidation plugin
 * instance.
 * 
 * <p>
 * It contains the functionality to retrieve the configured plugins and
 * generating call backs to them.
 * 
 * <p>
 * <b>Note:</b> This class does not implement the plugin., nor does any kind of
 * validation.
 */
public class UserPasswordValidationHelper implements AMConstants {

    // Map to store the plugin instances. To avoid instantiating multiple times.
    // Note: As these plugin map instances are created only once, any changes
    // to the implementation plugin class will only be reflected if server is
    // restarted.
    private static Map pluginsCache = Collections.EMPTY_MAP;

    private Debug debug = CommonUtils.debug;

    private AMUserPasswordValidation plugin = null;

    private String orgdn;

    public UserPasswordValidationHelper(SSOToken token, String orgDN)
            throws AMException {
        this.orgdn = orgDN;
        // Verify if plugin is present for the orgDN or else obtain global one.
        String pluginClass = getOrgUserPasswordValidationClass(orgDN);
        if (pluginClass != null && pluginClass.length() != 0) {
            // Get the plugin instance from the plugin cache.
            if (pluginsCache == Collections.EMPTY_MAP) {
                pluginsCache = new Hashtable();
            } else {
                plugin = (AMUserPasswordValidation) pluginsCache
                        .get(pluginClass);
            }

            if (plugin == null) { // Not instantiated earlier
                // Create a new plugin instance
                plugin = instantiateClass(token, pluginClass);
                pluginsCache.put(pluginClass, plugin);
            }
        }
    }

    public void validate(Map attributes) throws AMException {
        if (plugin != null) { // Other wise plugin is not valid
            // Validate the userID
            String userNamingAttr = NamingAttributeManager
                    .getNamingAttribute(AMObject.USER);
            Set uidValue = (Set) attributes.get(userNamingAttr);
            if (uidValue != null && !uidValue.isEmpty()) {
                String userID = (String) uidValue.iterator().next();

                // Invoke the plugin
                try {
                    Map envMap = new HashMap(2);
                    envMap.put(Constants.ORGANIZATION_NAME, orgdn);
                    plugin.validateUserID(userID, envMap);
                } catch (AMException ame) {
                    debug.error("AMUserPasswordValidationImpl:validate() "
                            + "User Name validation Failed" + ame);
                    throw ame;
                }
            }
            // Validate the password
            Set passwordValue = (Set) attributes.get(USER_PASSWORD_ATTRIBUTE);
            if (passwordValue != null && !passwordValue.isEmpty()) {
                String password = (String) passwordValue.iterator().next();

                // Invoke the plugin
                Map passMap = new HashMap(2);
                passMap.put(com.sun.identity.shared.Constants.ORGANIZATION_NAME,
                    orgdn);
                plugin.validatePassword(password, passMap);
            }
        }
    }

    private String getOrgUserPasswordValidationClass(String orgDN) {
        // Obtain the ServiceConfig
        try {
            // Get the org config
            SSOToken internalToken = CommonUtils.getInternalToken();
            // FIXME & TODO
            // Remove dependency on AMServiceUtils
            ServiceConfig sc = AMServiceUtils.getOrgConfig(internalToken,
                    orgDN, ADMINISTRATION_SERVICE);
            if (sc != null) {
                Map attributes = sc.getAttributes();
                Set value = (Set) attributes
                        .get(USERID_PASSWORD_VALIDATION_CLASS);
                return ((value == null || value.isEmpty()) ? null
                        : (String) value.iterator().next());
            } else {
                return getGlobalUserValiadationClass();
            }
        } catch (Exception ee) {
            return getGlobalUserValiadationClass();
        }
    }

    private String getGlobalUserValiadationClass() {
        // Org Config may not exist. Get default values
        if (debug.messageEnabled()) {
            debug.message("AMUserPasswordValidationImplImpl."
                    + "getGlobalUserValiadationClass() Organization config for "
                    + "service (" + ADMINISTRATION_SERVICE + ","
                    + USERID_PASSWORD_VALIDATION_CLASS
                    + ") not found. "
                    + "Obtaining default service config values ..");
        }
        try {
            // FIXME & TODO
            // Remove dependency on AMServiceUtils
            Map defaultValues = AMServiceUtils.getServiceConfig(CommonUtils
                    .getInternalToken(), ADMINISTRATION_SERVICE,
                    SchemaType.ORGANIZATION);
            if (defaultValues != null) {
                Set value = (Set) defaultValues
                        .get(USERID_PASSWORD_VALIDATION_CLASS);
                return ((value == null || value.isEmpty()) ? null
                        : (String) value.iterator().next());
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("AMUserPasswordValidationImplImpl."
                        + "getGlobalUserValiadationClass(): Unable to get " 
                        + "UserID & Password validation plugin information",
                        e);
            }
        }
        return null;
    }

    private AMUserPasswordValidation instantiateClass(SSOToken token,
            String className) throws AMException {
        try {
            return ((AMUserPasswordValidation) Class.forName(className)
                    .newInstance());
        } catch (InstantiationException e1) {
            debug.error("AMUserPasswordValidationImpl.instantiateClass(): "
                    + "Unable to instantiate class: " + className, e1);
            Object args[] = { className };
            throw new AMException(AMSDKBundle.getString("164", args), "164",
                    args);
        } catch (IllegalAccessException e1) {
            debug.error("AMUserPasswordValidationImpl.instantiateClass(): "
                    + "The class is " + className + " unaccessable: ", e1);
            Object args[] = { className };
            throw new AMException(AMSDKBundle.getString("164", args), "164",
                    args);
        } catch (ClassNotFoundException e1) {
            debug.error("AMUserPasswordValidationImpl.instantiateClass(): "
                    + "Unable to locate class " + className, e1);
            Object args[] = { className };
            throw new AMException(AMSDKBundle.getString("164", args), "164",
                    args);
        }
    }
}
