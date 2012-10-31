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
 * $Id: ServicesModelImpl.java,v 1.3 2008/08/15 19:41:31 veiming Exp $
 *
 */

package com.sun.identity.console.realm.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class ServicesModelImpl
    extends AMModelBase
    implements ServicesModel
{
    private static SSOToken adminSSOToken =
        (SSOToken)AccessController.doPrivileged(AdminTokenAction.getInstance());
    private static Set SUPPORTED_SCHEMA_TYPE = new HashSet();
    static {
        SUPPORTED_SCHEMA_TYPE.add(SchemaType.ORGANIZATION);
        SUPPORTED_SCHEMA_TYPE.add(SchemaType.DYNAMIC);
    }
 
    /**
     * Creates a simple model using default resource bundle. 
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public ServicesModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns a map of assigned service name to its localized name under a
     * realm.
     *
     * @param realmName Name of Realm.
     * @return a map of assigned service name to its localized name under a
     *         realm.
     * @throws AMConsoleException if service names cannot be obtained.
     */
    public Map getAssignedServiceNames(String realmName)
        throws AMConsoleException {
        String[] param = {realmName};
        logEvent("ATTEMPT_GET_ASSIGNED_SERVICE_OF_REALM", param);

        try {
            OrganizationConfigManager orgCfgMgr = new OrganizationConfigManager(
                getUserSSOToken(), realmName);
            Set names = orgCfgMgr.getAssignedServices();
            if ((names == null) || names.isEmpty()) {
                names = new HashSet();
            }
            getIdentityServices(realmName, names);

            /*
             * Need to use adminSSOToken because policy admin does not
             * have the correct privileges.
             */
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                adminSSOToken, realmName);
            AMAdminUtils.removeAllCaseIgnore(
                names, mgr.getAuthenticationServiceNames());
            removeNonDisplayableServices(names, SUPPORTED_SCHEMA_TYPE);
            // remove auth configuration service too
            names.remove(AMAdminConstants.AUTH_CONFIG_SERVICE);
            names.remove(AMAdminConstants.CORE_AUTH_SERVICE);

            logEvent("SUCCEED_GET_ASSIGNED_SERVICE_OF_REALM", param);
            return mapNameToDisplayName(names);
        } catch (AMConfigurationException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, strError};
            logEvent("CONFIGURATION_EXCEPTION_GET_ASSIGNED_SERVICE_OF_REALM",
                paramsEx);
            throw new AMConsoleException(strError);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, strError};
            logEvent("SMS_EXCEPTION_GET_ASSIGNED_SERVICE_OF_REALM", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private void getIdentityServices(String realmName, Set names) {
        /*
         * It is ok that administrator such as policy administrator
         * does not have access to AMIdentityRepository. We just ignore it.
         */
        try {
            AMIdentityRepository repo = new AMIdentityRepository(
                getUserSSOToken(), realmName);
            AMIdentity realmIdentity = repo.getRealmIdentity();
            names.addAll(realmIdentity.getAssignedServices());
        } catch (SSOException e) {
            debug.warning("ServicesModelImpl.getIdentityServices", e);
        } catch (IdRepoException e) {
            debug.warning("ServicesModelImpl.getIdentityServices", e);
        }
    }

    /**
     * Returns a map of service name to its display name that can be assigned
     * to a realm.
     *
     * @param realmName Name of Realm.
     * @return a map of service name to its display name that can be assigned
     * to a realm.
     * @throws AMConsoleException if service names cannot be obtained.
     */
    public Map getAssignableServiceNames(String realmName)
        throws AMConsoleException {
        String[] param = {realmName};
        logEvent("ATTEMPT_GET_ASSIGNABLE_SERVICE_OF_REALM", param);

        try {
            OrganizationConfigManager orgCfgMgr = new OrganizationConfigManager(
                getUserSSOToken(), realmName);
            Set names = orgCfgMgr.getAssignableServices();
            addIdentityUnassignedServices(realmName, names);
            names.removeAll(orgCfgMgr.getAssignedServices());
            
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                getUserSSOToken(), realmName);
            AMAdminUtils.removeAllCaseIgnore(
                names, mgr.getAuthenticationServiceNames());
            removeNonDisplayableServices(names,SUPPORTED_SCHEMA_TYPE);
            names.remove(AMAdminConstants.CORE_AUTH_SERVICE);
            logEvent("SUCCEED_GET_ASSIGNABLE_SERVICE_OF_REALM", param);
            return mapNameToDisplayName(names);
        } catch (AMConfigurationException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, strError};
            logEvent("CONFIGURATION_EXCEPTION_GET_ASSIGNABLE_SERVICE_OF_REALM",
                paramsEx);
            if (debug.warningEnabled()) {
                debug.warning("ServicesModel.getAssignableServiceNames " +
                    strError);
            }
            throw new AMConsoleException("no.properties");
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, strError};
            logEvent("SMS_EXCEPTION_GET_ASSIGNABLE_SERVICE_OF_REALM", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private void addIdentityUnassignedServices(
        String realmName,
        Set names
    ) {
        /*
         * It is ok that administrator such as policy administrator
         * does not have access to AMIdentityRepository. We just ignore it.
         */
        try {
            AMIdentityRepository repo = new AMIdentityRepository(
                getUserSSOToken(), realmName);
            AMIdentity realmIdentity = repo.getRealmIdentity();
            names.addAll(realmIdentity.getAssignableServices());
        } catch (IdRepoException e) {
            debug.warning("ServicesModelImpl.addIdentityUnassignedServices",e);
        } catch (SSOException e) {
            debug.warning("ServicesModelImpl.addIdentityUnassignedServices",e);
        }
    }

    /**
     * Creates a mapping of the service name to the display name 
     * for the service.
     */
    private Map mapNameToDisplayName(Set names) {
        Map map = new HashMap(names.size() *2);
        AMViewConfig vConfig = AMViewConfig.getInstance();
        for (Iterator iter = names.iterator(); iter.hasNext(); ) {
            String name = (String)iter.next();
            /*
             * Have a way to hide policies in console by adding some entries
             * to amConsoleConfig.xml
             */
            if (vConfig.isServiceVisible(name)) {
                String displayName = getLocalizedServiceName(name);
                if (!name.equals(displayName)) {
                    map.put(name, displayName);
                }
            }
        }
        return map;
    }

    /**
     * Unassigns services from realm.
     *
     * @param realmName Name of Realm.
     * @param names Names of services that are to be unassigned.
     * @throws AMConsoleException if services cannot be unassigned.
     */
    public void unassignServices(String realmName, Set names)
        throws AMConsoleException {
        if ((names != null) && !names.isEmpty()) {
            if ((realmName == null) || (realmName.trim().length() == 0)) {
                realmName = "/";
            }

            String[] params = new String[2];
            params[0] = realmName;
            String curServiceName = "";

            try {
                OrganizationConfigManager scm = new OrganizationConfigManager(
                    getUserSSOToken(), realmName);
                AMIdentityRepository repo = new AMIdentityRepository(
                    getUserSSOToken(), realmName);
                AMIdentity realmIdentity = repo.getRealmIdentity();
                Set realmServices = realmIdentity.getAssignedServices();

                for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                    String name = (String)iter.next();
                    curServiceName = name;
                    params[1] = name;
                    logEvent("ATTEMPT_UNASSIGN_SERVICE_FROM_REALM", params);

                    if (realmServices.contains(name)) {
                        realmIdentity.unassignService(name);
                    } else {
                        scm.unassignService(name);
                    }

                    logEvent("SUCCEED_UNASSIGN_SERVICE_FROM_REALM", params);
                }
            } catch (SMSException e) {
                String strError = getErrorString(e);
                String[] paramsEx = {realmName, curServiceName, strError};
                logEvent("SMS_EXCEPTION_UNASSIGN_SERVICE_FROM_REALM", paramsEx);
                throw new AMConsoleException(strError);
            } catch (SSOException e) {
                String strError = getErrorString(e);
                String[] paramsEx = {realmName, curServiceName, strError};
                logEvent("SSO_EXCEPTION_UNASSIGN_SERVICE_FROM_REALM",
                    paramsEx);
                throw new AMConsoleException(strError);
            } catch (IdRepoException e) {
                String strError = getErrorString(e);
                String[] paramsEx = {realmName, curServiceName, strError};
                logEvent("IDREPO_EXCEPTION_UNASSIGN_SERVICE_FROM_REALM",
                    paramsEx);
                throw new AMConsoleException(strError);
            }
        }
    }

    private void removeNonDisplayableServices( 
        Set serviceNames, 
        Set schemaTypes
    ) {
        for (Iterator i = serviceNames.iterator(); i.hasNext(); ){
            String svcName = (String)i.next();
        
            if (getServicePropertiesViewBeanURL(svcName) == null) {
                boolean hasAttr = false;

                for (Iterator j = schemaTypes.iterator();
                    j.hasNext() && !hasAttr;
                ) { 
                    SchemaType type = (SchemaType)j.next();
                    Set displayable = AMAdminUtils.getDisplayableAttributeNames(
                        svcName, type);
                    hasAttr = !displayable.isEmpty();
                }
    
                if (!hasAttr) {
                    i.remove();
                }
            }
        }
    }
}
