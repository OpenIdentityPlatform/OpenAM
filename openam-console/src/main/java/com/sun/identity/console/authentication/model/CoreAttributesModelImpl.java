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
 * $Id: CoreAttributesModelImpl.java,v 1.2 2008/06/25 05:42:46 qcheng Exp $
 *
 */

package com.sun.identity.console.authentication.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class CoreAttributesModelImpl
    extends AMServiceProfileModelImpl
    implements CoreAttributesModel
{
    private String currentRealm = null;
    private static Set DISPLAY_SCHEMA_SERVICE_TYPE = new HashSet(4);
    private static final String AUTH_SERVICE_NAME = "iPlanetAMAuthService";

    static {
        DISPLAY_SCHEMA_SERVICE_TYPE.add(SchemaType.ORGANIZATION);
        DISPLAY_SCHEMA_SERVICE_TYPE.add(SchemaType.DYNAMIC);
    }

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param serviceName Name of Service.
     * @param map of user information
     */
    public CoreAttributesModelImpl(
        HttpServletRequest req,
        String serviceName,
        Map map
    ) throws AMConsoleException 
    {
        super(req, serviceName, map);
        currentRealm = (String)map.get(AMAdminConstants.CURRENT_REALM);
        if (currentRealm == null) {
            debug.warning("resetting realm in CoreAttributeModel");
            currentRealm = "/";
        }
    }

    public Set getDisplaySchemaTypes() {
        return DISPLAY_SCHEMA_SERVICE_TYPE;
    }

    /**
     * Set attribute values.
     *
     * @param map Map of attribute name to Set of attribute values.
     * @throws AMConsoleException if values cannot be set.
     */
    public void setAttributeValues(Map map)
        throws AMConsoleException
    {
        String[] params = {currentRealm, AUTH_SERVICE_NAME};
        logEvent("ATTEMPT_MODIFY_AUTH_INSTANCE", params);

        try {
            OrganizationConfigManager scm = new OrganizationConfigManager(
                getUserSSOToken(), currentRealm);
            ServiceConfig config = scm.getServiceConfig(AUTH_SERVICE_NAME);
            config.setAttributes(map);
            logEvent("SUCCEED_MODIFY_AUTH_INSTANCE", params);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, AUTH_SERVICE_NAME, strError};
            logEvent("SMS_EXCEPTION_MODIFY_AUTH_INSTANCE", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, AUTH_SERVICE_NAME, strError};
            logEvent("SSO_EXCEPTION_MODIFY_AUTH_INSTANCE", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Returns attributes values.
     *
     * @return attributes values.
     */
    public Map getAttributeValues() {
        Map attrs = null;
        String[] param = {currentRealm};
        logEvent("ATTEMPT_GET_AUTH_PROFILE_IN_REALM", param);

        try {
            OrganizationConfigManager scm = new OrganizationConfigManager(
                getUserSSOToken(), currentRealm);
            ServiceConfig config = scm.getServiceConfig(AUTH_SERVICE_NAME);
            attrs = config.getAttributes();
            if ((attrs == null) || attrs.isEmpty()) {
                debug.warning(
                    "no attributes were returned for Core Auth ...");
            }
            logEvent("SUCCEED_GET_AUTH_PROFILE_IN_REALM", param);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {currentRealm, strError};
            logEvent("SMS_CONFIGURATION_EXCEPTION_GET_AUTH_PROFILE_IN_REALM",
                paramsEx);
            debug.error("CoreAttributesModelImpl.getAttributeValues", e);
        }

        return (attrs == null) ? Collections.EMPTY_MAP : attrs;
    }
}
