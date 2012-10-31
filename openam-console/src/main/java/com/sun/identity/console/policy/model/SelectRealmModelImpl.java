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
 * $Id: SelectRealmModelImpl.java,v 1.2 2008/06/25 05:43:08 qcheng Exp $
 *
 */

package com.sun.identity.console.policy.model;

import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * This implements <code>SelectRealmModel</code> where searching of realms
 * support is implemented.
 */
public class SelectRealmModelImpl
    extends AMModelBase
    implements SelectRealmModel
{
    /**
     * Creates a instance of this class.
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public SelectRealmModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns realms that have names matching with a filter.
     *
     * @param base Base realm name for this search. null indicates root
     *        suffix.
     * @param filter Filter string.
     * @return realms that have names matching with a filter.
     * @throws AMConsoleException if search fails.
     */
    public Set getRealmNames(String base, String filter)
        throws AMConsoleException
    {
        if ((base == null) || (base.length() == 0)) {
            base = getStartDN();
        }

        String[] param = {base};
        logEvent("ATTEMPT_GET_REALM_NAMES", param);

        try {
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                    getUserSSOToken(), base);
            logEvent("SUCCEED_GET_REALM_NAMES", param);
            return PolicyModelImpl.appendBaseDN(base,
                    orgMgr.getSubOrganizationNames(filter, true), filter, this);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {base, strError};
            logEvent("SMS_EXCEPTION_GET_REALM_NAMES", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Returns set of authentication instances.
     *
     * @param realmName Name of Realm.
     * @return set of authentication instances.
     * @throws AMConsoleException if authentication instances cannot be
     *         obtained.
     */
    public Set getAuthenticationInstances(String realmName)
        throws AMConsoleException {
        Set names = Collections.EMPTY_SET;
        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                    getUserSSOToken(), realmName);
            Set instances = mgr.getAuthenticationInstances();
            if ((instances != null) && !instances.isEmpty()) {
                names = new HashSet(instances.size());
                for (Iterator i = instances.iterator(); i.hasNext(); ) {
                    names.add(((AMAuthenticationInstance)i.next()).getName());
                }
            }
        } catch (AMConfigurationException e) {
            throw new AMConsoleException(getErrorString(e));
        }

        return names;
    }
}
