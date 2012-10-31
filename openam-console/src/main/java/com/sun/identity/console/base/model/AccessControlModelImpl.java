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
 * $Id: AccessControlModelImpl.java,v 1.4 2008/06/25 05:42:50 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

public class AccessControlModelImpl
    implements AccessControlModel
{
    private SSOToken ssoToken = null;
    private Set serviceNames;

    public AccessControlModelImpl(SSOToken ssoToken) {
        this.ssoToken = ssoToken;
    }

    public AccessControlModelImpl(HttpServletRequest req) {
        try {
            ssoToken = AMAuthUtils.getSSOToken(req);
        } catch (SSOException e) {
            AMModelBase.debug.warning("AccessControlModelImpl.<init>", e);
        }
    }

    /**
     * Returns true if a page can be viewed.
     *
     * @param permissions Permissions associated to the page.
     * @param accessLevel Level of access i.e. either global or realm level.
     * @param realmName Currently view realm Name.
     * @param delegateUI true if this is a delegation administration page.
     * @return true if a page can be viewed.
     */
    public boolean canView(
        Set permissions,
        String accessLevel,
        String realmName,
        boolean delegateUI
    ) {
        boolean canView = false;
        if (ssoToken != null) {
            if (permissions.isEmpty()) {
                canView = true;
            } else {
                try {
                    DelegationEvaluator delegationEvaluator =
                        new DelegationEvaluator();
                    DelegationPermission delegationPermission =
                        new DelegationPermission();
                    delegationPermission.setVersion("*");
                    delegationPermission.setSubConfigName("default");

                    if ((accessLevel != null) &&
                        (accessLevel.trim().length() > 0)
                    ) {
                        delegationPermission.setConfigType(accessLevel);
                        delegationPermission.setOrganizationName("/");
                    } else {
                        delegationPermission.setOrganizationName(realmName);
                    }

                    if (delegateUI) {
                        Set actions = new HashSet();
                        actions.add(AMAdminConstants.PERMISSION_DELEGATE);
                        delegationPermission.setActions(actions);
                        canView = delegationEvaluator.isAllowed(
                            ssoToken, delegationPermission,
                            Collections.EMPTY_MAP);
                    }

                    if (!delegateUI || canView) {
                        for (Iterator i = permissions.iterator();
                            i.hasNext() && !canView;
                        ) {
                            String serviceName = (String)i.next();
                            canView = hasPermission(
                                delegationEvaluator, delegationPermission,
                                serviceName, AMAdminConstants.PERMISSION_READ);
                        }
                    }
                } catch (DelegationException e) {
                    AMModelBase.debug.error("AccessControlModelImpl.canView", e);
                } catch (SSOException e) {
                    AMModelBase.debug.error("AccessControlModelImpl.canView", e);
                }
            }
        }

        return canView;
    }

    private boolean hasPermission(
        DelegationEvaluator delegationEvaluator,
        DelegationPermission delegationPermission,
        String serviceName,
        String privilege
    ) throws DelegationException, SSOException {
        boolean hasP = false;
        Set actions = new HashSet();
        actions.add(privilege);
        delegationPermission.setActions(actions);

        if (serviceName.equals(ANY_SERVICE)) {
            Set services = getServiceNames();
            for (Iterator i = services.iterator(); i.hasNext() && !hasP; ) {
                String name = (String)i.next();
                delegationPermission.setServiceName(name);
                hasP = delegationEvaluator.isAllowed(
                    ssoToken, delegationPermission, Collections.EMPTY_MAP);
            }
        } else {
            delegationPermission.setServiceName(serviceName);
            hasP = delegationEvaluator.isAllowed(
                ssoToken, delegationPermission, Collections.EMPTY_MAP);
        }
        return hasP;
    }

    private Set getServiceNames() {
        if (serviceNames == null) {
            SSOToken adminSSOToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            try {
                ServiceManager sm = new ServiceManager(adminSSOToken);
                serviceNames = sm.getServiceNames();
            } catch (SSOException e) {
                AMModelBase.debug.error(
                    "AccessControlModelImpl.getServiceNames", e);
            } catch (SMSException e) {
                AMModelBase.debug.error(
                    "AccessControlModelImpl.getServiceNames", e);
            }
        }
        return serviceNames;
    }
}
