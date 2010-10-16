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
 * $Id: OrgCreateFilteredRoleReq.java,v 1.2 2008/06/25 05:52:29 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMFilteredRole;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class OrgCreateFilteredRoleReq extends OrgCreateRoleReq {
    /**
     * Constructs a new OrgCreateFilteredRoleReq.
     *
     * @param  targetDN the Organization DN. 
     */        
    OrgCreateFilteredRoleReq(String targetDN) {
        super(targetDN);
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("organization") + " " +
            targetDN + "\n" + bundle.getString("createroles"));

        String[] args = {roleDN, targetDN};
        doLog(args, AdminUtils.CREATE_ROLE_ATTEMPT);

        try {
            AMOrganization org = dpConnection.getOrganization(targetDN);
            Map map = new HashMap();
            String strFilterInfo = null;

            if ((values != null) && (values.get(ROLE_FILTER_INFO) != null)) {
                Set filterInfo = (Set)values.remove(ROLE_FILTER_INFO);

                if ((filterInfo != null) && !filterInfo.isEmpty()) {
                    strFilterInfo = (String)filterInfo.iterator().next();
                }
            }

            map.put(roleDN, values);
            Set rolesReqSet = org.createFilteredRoles(map);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(AdminReq.bundle.getString("statusmsg23"));
            }

            for (Iterator iter = rolesReqSet.iterator(); iter.hasNext(); ) {
                AMFilteredRole role = (AMFilteredRole)iter.next();
                if (strFilterInfo != null) {
                    role.setFilter(strFilterInfo);
                }
                AdminReq.writer.println(role.getDN());
            }

            doLog(rolesReqSet, AdminUtils.CREATE_ROLE);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
