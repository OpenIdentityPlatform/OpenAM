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
 * $Id: OrgModifyRoleReq.java,v 1.2 2008/06/25 05:52:30 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMFilteredRole;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class OrgModifyRoleReq extends AdminReq {
    private Map values = new HashMap();
    private String roleDN;

    /**
     * Constructs a new OrgModifyPcReq.
     *
     * @param  targetDN the Organization DN. 
     */        
    OrgModifyRoleReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the role dn's and its avPair Map to values Map.
     *
     * @param modifyDN the DN of a role
     * @param avPair the Map which contains the attribute as key and value.
     */        
    void addRoleReq(String modifyDN, Map avPair) {
        roleDN = modifyDN;
        values = avPair;
    }
        
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log("role DN: " + roleDN);
        }

        writer.println(bundle.getString(getType()) + " " +
            roleDN + "\n" + bundle.getString("modifyrole"));

        try {
            AMRole role = null;
            int roleType = RoleUtils.getRoleType(roleDN, dpConnection);

            if (roleType == AMObject.FILTERED_ROLE) {
                AMFilteredRole filteredRole =
                    dpConnection.getFilteredRole(roleDN);

                if ((values != null) && (values.get(ROLE_FILTER_INFO) != null))
                {
                    Set set = (Set)values.remove(ROLE_FILTER_INFO);

                    if ((set != null) && !set.isEmpty()) {
                        filteredRole.setFilter((String)set.iterator().next());
                    }
                }

                role = filteredRole;
            } else {
                role = dpConnection.getRole(roleDN);
            }

            doLog(role, AdminUtils.MODIFY_ROLE_ATTEMPT);
            role.setAttributes(values);
            role.store();
//            doLog(role, "modify-role");
            doLog(role, AdminUtils.MODIFY_ROLE);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    protected String getType() {
        return "organization";
    }
}
