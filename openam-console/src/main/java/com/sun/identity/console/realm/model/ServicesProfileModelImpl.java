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
 * $Id: ServicesProfileModelImpl.java,v 1.2 2008/06/25 05:43:13 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.realm.model;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.OrganizationConfigManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class ServicesProfileModelImpl
    extends AMServiceProfileModelImpl
    implements ServicesProfileModel {
    private static SSOToken adminSSOToken =
        (SSOToken)AccessController.doPrivileged(AdminTokenAction.getInstance());

    private static Set DISPLAY_SCHEMA_SERVICE_TYPE = new HashSet(4);

    private String currentRealm;

    static {
        DISPLAY_SCHEMA_SERVICE_TYPE.add(SchemaType.ORGANIZATION);
        DISPLAY_SCHEMA_SERVICE_TYPE.add(SchemaType.DYNAMIC);
    }

    public ServicesProfileModelImpl(
        HttpServletRequest req,
        String serviceName,
        Map map
    ) throws AMConsoleException {
        super(req, serviceName, map);
        currentRealm = (String)map.get(AMAdminConstants.CURRENT_REALM);
        if (currentRealm == null) {
            currentRealm = "/";
        }
    }

    public Set getDisplaySchemaTypes() {
        return DISPLAY_SCHEMA_SERVICE_TYPE;
    }

    /**
     * Assigns service to a realm.
     *
     * @param map Map of attribute name to Set of attribute values.
     * @throws AMConsoleException if values cannot be set.
     */
    public void assignService(Map map)
        throws AMConsoleException {
        String[] params = {currentRealm, serviceName};
        logEvent("ATTEMPT_ASSIGN_SERVICE_TO_REALM", params);

        try {
            AMIdentityRepository repo = new AMIdentityRepository(
                 getUserSSOToken(), currentRealm);
            AMIdentity realmIdentity = repo.getRealmIdentity();
            Set servicesFromIdRepo = realmIdentity.getAssignableServices();

            if (servicesFromIdRepo.contains(serviceName)) {
                realmIdentity.assignService(serviceName, map);
            } else {
                OrganizationConfigManager orgCfgMgr =
                    new OrganizationConfigManager(
                    getUserSSOToken(), currentRealm);
                orgCfgMgr.assignService(serviceName, map);
            }
            logEvent("SUCCEED_ASSIGN_SERVICE_TO_REALM", params);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, serviceName, strError};
            logEvent("SSO_EXCEPTION_ASSIGN_SERVICE_TO_REALM", paramsEx);
            throw new AMConsoleException(strError);
        } catch (IdRepoException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, serviceName, strError};
            logEvent("IDREPO_EXCEPTION_ASSIGN_SERVICE_TO_REALM", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, serviceName, strError};
            logEvent("SMS_EXCEPTION_ASSIGN_SERVICE_TO_REALM", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Returns attribute values of the service profile.
     *
     * @return attribute values of the service profile.
     */
    public Map getAttributeValues() {
        Map map = null;
        String[] params = {currentRealm, serviceName, "*"};
        logEvent("ATTEMPT_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM", params);

        try {
            AMIdentityRepository repo = new AMIdentityRepository(
                 getUserSSOToken(), currentRealm);
            AMIdentity realmIdentity = repo.getRealmIdentity();
            Set servicesFromIdRepo = realmIdentity.getAssignedServices();

            if (servicesFromIdRepo.contains(serviceName)) {
                map = realmIdentity.getServiceAttributes(serviceName);
            } else {
                OrganizationConfigManager orgCfgMgr =
                    new OrganizationConfigManager(
                    getUserSSOToken(), currentRealm);
                map = orgCfgMgr.getServiceAttributes(serviceName);
            }
            logEvent("SUCCEED_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
                params);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, serviceName, strError};
            logEvent("SSO_EXCEPTION_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
                paramsEx);
            debug.error("ServicesProfileModelImpl.getAttributeValues", e);
        } catch (IdRepoException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, serviceName, strError};
            logEvent(
                "IDREPO_EXCEPTION_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
                paramsEx);
            debug.error("ServicesProfileModelImpl.getAttributeValues", e);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, serviceName, strError};
            logEvent("SMS_EXCEPTION_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
                paramsEx);
            debug.error("ServicesProfileModelImpl.getAttributeValues", e);
        }

        return (map != null) ? map : Collections.EMPTY_MAP;
    }

    /**
     * Set attributes of an service.
     *
     * @param attrValues Map of attribute name to its values.
     * @throws AMConsoleException if values cannot be set.
     */
    public void setAttributes(Map attrValues)
        throws AMConsoleException {
        String[] params = {currentRealm, serviceName};
        logEvent("ATTEMPT_MODIFY_SERVICE_UNDER_REALM", params);

        try {
            AMIdentityRepository repo = new AMIdentityRepository(
                 getUserSSOToken(), currentRealm);
            AMIdentity realmIdentity = repo.getRealmIdentity();
            Set servicesFromIdRepo = realmIdentity.getAssignedServices();

            if (servicesFromIdRepo.contains(serviceName)) {
                realmIdentity.modifyService(serviceName, attrValues);
            } else {
                OrganizationConfigManager orgCfgMgr =
                    new OrganizationConfigManager(
                        getUserSSOToken(), currentRealm);
                orgCfgMgr.modifyService(serviceName, attrValues);
            }
            logEvent("SUCCEED_MODIFY_SERVICE_UNDER_REALM", params);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, serviceName, strError};
            logEvent("SSO_EXCEPTION_MODIFY_SERVICE_UNDER_REALM", paramsEx);
            throw new AMConsoleException(strError);
        } catch (IdRepoException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, serviceName, strError};
            logEvent("IDREPO_EXCEPTION_MODIFY_SERVICE_UNDER_REALM", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, serviceName, strError};
            logEvent("SMS_EXCEPTION_MODIFY_SERVICE_UNDER_REALM", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Returns defaults attribute values.
     *
     * @return defaults attribute values.
     */
    public Map getDefaultAttributeValues() {
        return super.getAttributeValues();
    }

    /**
     * Returns true if a service has displayable organizational attributes.
     *
     * @return true if a service has displayable organizational attributes.
     */
    public boolean hasOrganizationAttributes() {
        Set o = AMAdminUtils.getDisplayableAttributeNames(
            serviceName, SchemaType.ORGANIZATION);
        Set d = AMAdminUtils.getDisplayableAttributeNames(
            serviceName, SchemaType.DYNAMIC);
 
        return (!o.isEmpty() || !d.isEmpty());
    }
}
