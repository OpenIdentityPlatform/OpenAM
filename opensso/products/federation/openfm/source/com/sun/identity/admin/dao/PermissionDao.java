/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PermissionDao.java,v 1.14 2009/12/11 18:06:36 farble1670 Exp $
 */
package com.sun.identity.admin.dao;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.admin.Token;
import com.sun.identity.admin.model.Permission;
import com.sun.identity.admin.model.RealmBean;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PermissionDao implements Serializable {

    private List<PermissionAction> permissionActions = new ArrayList<PermissionAction>();
    private static DelegationEvaluator delegationEvaluator;

    static {
        try {
            delegationEvaluator = new DelegationEvaluator();
        } catch (DelegationException de) {
            throw new RuntimeException(de);
        }
    }

    public void setPermissionActions(List<String> pas) {
        for (String line : pas) {
            PermissionAction pa = new PermissionAction(line);
            permissionActions.add(pa);
        }
    }

    public List<Permission> getPermissions(RealmBean realmBean) {
        List<Permission> permissions = new ArrayList<Permission>();

        for (PermissionAction pa : permissionActions) {
            if (isAllowed(realmBean, pa)) {
                permissions.add(pa.getPermission());
            }
        }

        return permissions;
    }

    private boolean isAllowed(RealmBean realmBean, PermissionAction pa) {
        try {
            DelegationPermission dp = pa.toDelegationPermission(realmBean);
            SSOToken t = new Token().getSSOToken();
            boolean allowed = delegationEvaluator.isAllowed(t, dp, Collections.EMPTY_MAP, true);
            return allowed;
        } catch (DelegationException de) {
            throw new RuntimeException(de);
        } catch (SSOException ssoe) {
            throw new RuntimeException(ssoe);
        }
    }
}
