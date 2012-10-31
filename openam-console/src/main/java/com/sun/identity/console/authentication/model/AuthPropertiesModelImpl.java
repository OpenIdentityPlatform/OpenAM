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
 * $Id: AuthPropertiesModelImpl.java,v 1.3 2008/07/09 02:04:49 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.authentication.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */ 

public  class AuthPropertiesModelImpl extends AMModelBase
    implements AuthPropertiesModel {

    private static final String CORE_AUTH_SERVICE = "iPlanetAMAuthService";

    private String currentRealm = null;
    /**
     * Creates a model instance for configuring the core auth properties.
     *
     * @param req The <code>HttpServletRequest</code> object.
     * @param map of user information.
     */
    public AuthPropertiesModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
        currentRealm = (String)map.get(AMAdminConstants.CURRENT_REALM); 
        if (currentRealm == null) {
            currentRealm = "/";
        }
    }

    public void setCurrentRealm(String realm) {
        currentRealm = realm;
    }

    /**
     * Returns the values for the core authenticaion service attributes. 
     *
     * @return Map property values.
     */
    public Map getValues() throws AMConsoleException {
        String[] params = {currentRealm, CORE_AUTH_SERVICE, "*"};
        logEvent("ATTEMPT_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM", params);
        Map attrs = null;

        try {
            ServiceConfig config = getCoreAuthServiceConfig();
            attrs = config.getAttributes();
            logEvent("SUCCEED_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM", params);
            if ((attrs == null) || attrs.isEmpty()) {
                debug.warning("no attributes were returned for Core ...");
            }
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, CORE_AUTH_SERVICE, strError};
            logEvent("SMS_EXCEPTION_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
                paramsEx);
            throw new AMConsoleException(strError);
        }

        return (attrs == null) ? Collections.EMPTY_MAP : attrs;
    }

    /**
     * To get the service name from the instance name:
     *   1) get a handle to the AMAuthenicationInstance object
     *   2) from the AuthInstance object get the type of instance
     *   3) Use the instance type to get the schema for that type
     *   4) from the schema get the service name
     */
    public String getServiceName(String instance) {
        String name = null;
        try {
            AMAuthenticationManager mgr =
                new AMAuthenticationManager(getUserSSOToken(), currentRealm);

            AMAuthenticationInstance inst = 
                mgr.getAuthenticationInstance(instance);

            if (inst != null) {
                AMAuthenticationSchema schema = 
                mgr.getAuthenticationSchema(inst.getType());

                name = schema.getServiceName();
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("AuthPropertiesModel.getServiceName, " +
                        " the requested instance " + instance +
                        " does not exist.");
                }
            }
        } catch (AMConfigurationException ace) {
            if (debug.warningEnabled()) {
                debug.warning(
                    "problem getting service name for " + instance, ace);
            }
        }
        return name;
    }

    /**
     * Returns true if there are attributes for a authentication type.
     *
     * @param type Authtentication type.
     * @return true if there are attributes for a authentication type.
     */
    public boolean hasAuthAttributes(String type) {
        boolean has = false;
        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                getUserSSOToken(), currentRealm);
            AMAuthenticationSchema schema = mgr.getAuthenticationSchema(type);
            has = !schema.getAttributeSchemas().isEmpty();
        } catch (AMConfigurationException e) {
            debug.warning("AuthPropertiesModelImpl.hasAuthAttributes", e);
        }
        return has;
    }

    public Map getAuthTypes() {
        Map authAndLocalizedTypes = Collections.EMPTY_MAP;
        try {
            logEvent("ATTEMPT_GET_AUTH_TYPE", getServerInstanceForLogMsg());
            AMAuthenticationManager mgr = 
                new AMAuthenticationManager(getUserSSOToken(), "/");
            Set types = mgr.getAuthenticationTypes();
            authAndLocalizedTypes = new HashMap(types.size());
            for (Iterator iter=types.iterator(); iter.hasNext();) {
                String authType = (String)iter.next();
                AMAuthenticationSchema schema =
                    mgr.getAuthenticationSchema(authType);
                String svcName = schema.getServiceName();
                String localizedName = (svcName!=null && svcName.length()>0) ?
                    getLocalizedServiceName(svcName) : authType;
                authAndLocalizedTypes.put(authType, localizedName);
            }
            logEvent("SUCCEED_GET_AUTH_TYPE", getServerInstanceForLogMsg());
        } catch (AMConfigurationException e) {
            String strError = getErrorString(e);
            String[] paramEx = {strError};
            logEvent("SMS_EXCEPTION_GET_AUTH_TYPE", paramEx);
            debug.warning("AuthPropertiesModelImpl.getAuthTypes", e);
        }

        return authAndLocalizedTypes;
    }

    public Set getAuthInstances() {
        Set instances = null;

        if (currentRealm !=null) {
            String[] param = {currentRealm};
            logEvent("ATTEMPT_GET_AUTH_INSTANCE", param);
            try {
                AMAuthenticationManager mgr = new AMAuthenticationManager(
                    getUserSSOToken(), currentRealm);
                instances = mgr.getAuthenticationInstances();
                logEvent("SUCCEED_GET_AUTH_INSTANCE", param);
            } catch (AMConfigurationException e) {
                String strError = getErrorString(e);
                String[] paramsEx = {currentRealm, strError};
                logEvent("AUTH_CONFIG_EXCEPTION_GET_AUTH_INSTANCE", paramsEx);
                debug.warning("AuthPropertiesModelImpl.getAuthInstances", e);
            }
        }

        return (instances == null) ? Collections.EMPTY_SET : instances;
    }

    public void removeAuthInstance(Set names) throws AMConsoleException {
        StringBuilder errorList = new StringBuilder();
        String message = null;

        try {
            String[] params = new String[2];
            params[0] = currentRealm;
            AMAuthenticationManager mgr =
                new AMAuthenticationManager(getUserSSOToken(), currentRealm);

            for (Iterator i=names.iterator(); i.hasNext(); ) {
                String instance = (String)i.next();
                params[1] = instance;
                logEvent("ATTEMPT_REMOVE_AUTH_INSTANCE", params);

                try {
                    mgr.deleteAuthenticationInstance(instance);
                    logEvent("SUCCEED_REMOVE_AUTH_INSTANCE", params);
                } catch(AMConfigurationException e) {
                    String strError = getErrorString(e);
                    String[] paramsEx = {currentRealm, instance, strError};
                    logEvent(
                        "AUTH_CONFIG_EXCEPTION_REMOVE_AUTH_INSTANCE", paramsEx);
                    debug.warning("failed to delete", e);
                    message = e.getMessage();
                    if (errorList.length() > 0) {
                        errorList.append(", ");
                    } 
                    errorList.append(instance);
                }
            }
        } catch (AMConfigurationException ace) {
            String strError = getErrorString(ace);
            String[] paramsEx = {currentRealm, "*", strError};
            logEvent("AUTH_CONFIG_EXCEPTION_REMOVE_AUTH_INSTANCE", paramsEx);
            debug.error("cant delete auth instance: " ,ace);
            throw new AMConsoleException(strError);
        }

        if (errorList.length() > 0) {
            String[] tmp = { errorList.toString(),  message };
            throw new AMConsoleException(MessageFormat.format(
                getLocalizedString("authentication.instance.delete.failed"),
                (Object[])tmp) );
        }
    }

    public void createAuthInstance(String name, String type)
        throws AMConsoleException 
    {
        String[] params = {currentRealm, name, type};
        logEvent("ATTEMPT_CREATE_AUTH_INSTANCE", params);

        try {
            AMAuthenticationManager mgr = 
                new AMAuthenticationManager(getUserSSOToken(), currentRealm);

            AMAuthenticationSchema as = mgr.getAuthenticationSchema(type);
            mgr.createAuthenticationInstance(
                name,type, as.getAttributeValues());        
            logEvent("SUCCEED_CREATE_AUTH_INSTANCE", params);
        } catch (AMConfigurationException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, name, type, strError};
            logEvent("AUTH_CONFIG_EXCEPTION_CREATE_AUTH_INSTANCE", paramsEx);

            debug.warning("AuthPropertiesModelImpl.createAuthInstance ", e);
            throw new AMConsoleException(strError);
        }
    }

    public void setValues(Map modifiedValues) throws AMConsoleException {
        String[] params = {currentRealm, CORE_AUTH_SERVICE};
        logEvent("ATTEMPT_MODIFY_AUTH_INSTANCE", params);
        try {
            ServiceConfig sc = getCoreAuthServiceConfig();
            sc.setAttributes(modifiedValues);
            logEvent("SUCCEED_MODIFY_AUTH_INSTANCE", params);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, CORE_AUTH_SERVICE, strError};
            logEvent("SMS_EXCEPTION_MODIFY_AUTH_INSTANCE", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, CORE_AUTH_SERVICE, strError};
            logEvent("SSO_EXCEPTION_MODIFY_AUTH_INSTANCE", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private ServiceConfig getCoreAuthServiceConfig()
        throws SMSException
    {
        OrganizationConfigManager scm = new OrganizationConfigManager(
            getUserSSOToken(), currentRealm);
        return scm.getServiceConfig(CORE_AUTH_SERVICE);
    }

    public Map getInstanceValues(String name) {
        Map values = null;
        String[] params = {currentRealm, name};
        logEvent("ATTEMPT_GET_AUTH_INSTANCE_PROFILE", params);

        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                getUserSSOToken(), currentRealm);
            AMAuthenticationInstance ai = mgr.getAuthenticationInstance(name);
            values = ai.getAttributeValues();
            logEvent("SUCCEED_GET_AUTH_INSTANCE_PROFILE", params);
        } catch (AMConfigurationException e) {
            String[] paramsEx = {currentRealm, name, getErrorString(e)};
            logEvent("AUTH_CONFIGURATION_EXCEPTION_GET_AUTH_INSTANCE_PROFILE",
                paramsEx);
            debug.warning("AuthPropertiesModelImpl.getInstanceValues", e);
        } 
        return (values == null) ? Collections.EMPTY_MAP : values;
    }

    public void setInstanceValues(String instance, Map values)
        throws AMConsoleException 
    {
        String[] params = {currentRealm, instance};
        logEvent("ATTEMPT_MODIFY_AUTH_INSTANCE_PROFILE", params);
        try {
            AMAuthenticationManager mgr =
                new AMAuthenticationManager(getUserSSOToken(), currentRealm);
            AMAuthenticationInstance ai = mgr.getAuthenticationInstance(
                instance);
            ai.setAttributeValues(values);
            logEvent("SUCCEED_MODIFY_AUTH_INSTANCE_PROFILE", params);
        } catch (AMConfigurationException e) {
            debug.warning("AuthPropertiesModelImpl.setInstanceValues", e);
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, instance, strError};
            logEvent(
                "AUTH_CONFIGURATION_EXCEPTION_MODIFY_AUTH_INSTANCE_PROFILE",
                paramsEx);
            throw new AMConsoleException(strError);
        } catch (SMSException e) {
            debug.warning("AuthPropertiesModelImpl.setInstanceValues", e);
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, instance, strError};
            logEvent("SMS_EXCEPTION_MODIFY_AUTH_INSTANCE_PROFILE", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            debug.warning("AuthPropertiesModelImpl.setInstanceValues", e);
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, instance, strError};
            logEvent("SSO_EXCEPTION_MODIFY_AUTH_INSTANCE_PROFILE", paramsEx);
            throw new AMConsoleException(strError);
        }
    }
}
