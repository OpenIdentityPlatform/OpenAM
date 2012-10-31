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
 * $Id: DelegationConfigNode.java,v 1.3 2008/07/10 23:27:22 veiming Exp $
 *
 */

package com.sun.identity.console.delegation.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.sm.DNMapper;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

public class DelegationConfigNode {
    private String viewbeanName;
    private Map permissions = new HashMap();
    private Set actionhandlers;
    private Set tables;
    private Set staticTexts;

    DelegationConfigNode(String viewbeanName) {
        this.viewbeanName = viewbeanName;
    }

    void setPermissions(String name, Set perm) {
        permissions.put(name, perm);
    }

    void setActionHandlers(Set set) {
        actionhandlers = set;
    }

    void setStaticTexts(Set set) {
        staticTexts = set;
    }

    void setTables(Set set) {
        tables = set;
    }

    boolean hasPermission(
        String realmName,
        String serviceName,
        String action,
        AMModel model
    ) throws DelegationException {
        return hasPermission(realmName, serviceName, action, 
            model.getUserSSOToken());
    }

    boolean hasPermission(
        String realmName,
        String serviceName,
        String action,
        SSOToken ssoToken
    ) throws DelegationException {
        if (realmName == null) {
            try {
                realmName = DNMapper.orgNameToRealmName(
                    ssoToken.getProperty(Constants.ORGANIZATION));
            } catch (SSOException e) {
                throw new DelegationException(e);
            }
        }

        DelegationEvaluator delegationEvaluator = new DelegationEvaluator();
        DelegationPermission delegationPermission = getDelegationPermission(
            realmName, action);
        boolean allowed = false;

        if (serviceName != null) {
            allowed = isAllowed(delegationEvaluator, delegationPermission,
                ssoToken, serviceName);
        } else {
            Set actions = (Set)permissions.get(
                AMAdminConstants.PERMISSION_MODIFY);
            for (Iterator i = actions.iterator(); i.hasNext() && !allowed; ) {
                allowed = isAllowed(delegationEvaluator, delegationPermission,
                    ssoToken, (String)i.next());
            }
        }

        return allowed;
    }

    private DelegationPermission getDelegationPermission(
        String realmName,
        String privilege
    ) throws DelegationException {
        DelegationPermission delegationPermission =
            new DelegationPermission();
        delegationPermission.setOrganizationName(realmName);
        delegationPermission.setVersion("*");
        Set actions = new HashSet(2);
        actions.add(privilege);
        delegationPermission.setActions(actions);
        return delegationPermission;
    }

    boolean configureButtonsAndTables(
        String realmName,
        String serviceName,
        AMModel model,
        AMViewBeanBase viewbean
    ) throws DelegationException {
        boolean allowed = hasPermission(realmName, serviceName,
            AMAdminConstants.PERMISSION_MODIFY, model);

        if (!allowed) {
            blankStaticTexts(viewbean);
            disableTables(viewbean);
            disableActionhandlers(viewbean);
        }

        return allowed;
    }

    private void blankStaticTexts(AMViewBeanBase viewbean) {
        if (staticTexts != null) {
            for (Iterator i = staticTexts.iterator(); i.hasNext(); ) {
                try {
                    viewbean.addBlankTextField((String)i.next());
                } catch (IllegalArgumentException e) {
                    AMModelBase.debug.warning(
                        "DelegationConfigNode.blanksStaticTexts" +
                         e.getMessage());
                }
            }
        }
    }

    private void disableTables(AMViewBeanBase viewbean) {
        if (tables != null) {
            for (Iterator i = tables.iterator(); i.hasNext(); ) {
                try {
                    viewbean.hideTableSelectionIcons((String)i.next());
                } catch (IllegalArgumentException e) {
                    AMModelBase.debug.warning(
                        "DelegationConfigNode.disableTables" +
                        e.getMessage());
                }
            }
        }
    }

    private void disableActionhandlers(AMViewBeanBase viewBean) {
        if (actionhandlers != null) {
            for (Iterator i = actionhandlers.iterator(); i.hasNext(); ) {
                try {
                    viewBean.disableButton((String)i.next(), true);
                } catch (IllegalArgumentException e) {
                    AMModelBase.debug.warning(
                        "DelegationConfigNode.configureButtons" +
                        e.getMessage());
                }
            }
        }
    }

    boolean isAllowed(
        DelegationEvaluator delegationEvaluator,
        DelegationPermission delegationPermission,
        SSOToken ssoToken,
        String serviceName
    ) {
        boolean isAllowed = false;
        try {
            delegationPermission.setServiceName(serviceName);
            isAllowed = delegationEvaluator.isAllowed(
                ssoToken, delegationPermission, Collections.EMPTY_MAP);
        } catch (SSOException e) {
            AMModelBase.debug.error("DelegationConfigNode.isAllowed", e);
        } catch (DelegationException e) {
            AMModelBase.debug.error("DelegationConfigNode.isAllowed", e);
        }
        return isAllowed;
    }
}
