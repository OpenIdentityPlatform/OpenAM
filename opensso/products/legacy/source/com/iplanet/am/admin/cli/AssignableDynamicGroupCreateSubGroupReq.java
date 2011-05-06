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
 * $Id: AssignableDynamicGroupCreateSubGroupReq.java,v 1.2 2008/06/25 05:52:24 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMAssignableDynamicGroup;
import com.iplanet.am.sdk.AMDynamicGroup;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class AssignableDynamicGroupCreateSubGroupReq extends GroupCreateSubGroupReq {

    AssignableDynamicGroupCreateSubGroupReq(String targetDN) {
        super(targetDN);
    }

    protected Set createGroups(AMStoreConnection dpConnection)
        throws AdminException
    {
        try {
            AMAssignableDynamicGroup grp =
                dpConnection.getAssignableDynamicGroup(targetDN);
            return createGroup(grp);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    protected Set createGroup(AMAssignableDynamicGroup group)
        throws AdminException
    {
        Set createdGroup = null;

        try {
            Map map = new HashMap();

            if (groupType.equals(GROUP_TYPE_ASSIGNABLE_DYNAMIC)) {
                map.put(groupDN, values);
                createdGroup = group.createAssignableDynamicGroups(map);
            } else if (groupType.equals(GROUP_TYPE_DYNAMIC)) {
                String strFilter = null;
                                                                                
                if ((values != null) && (values.get(GROUP_FILTER_INFO) != null)
                ) {
                    Set set = (Set)values.remove(GROUP_FILTER_INFO);
                                                                                
                    if ((set != null) && !set.isEmpty()) {
                        strFilter = (String)set.iterator().next();
                    }
                }

                map.put(groupDN, values);
                createdGroup = group.createDynamicGroups(map);
                                                                                
                if (strFilter != null) {
                    AMDynamicGroup g =
                        (AMDynamicGroup)createdGroup.iterator().next();
                    g.setFilter(strFilter);
                }
            } else {
                map.put(groupDN, values);
                createdGroup = group.createStaticGroups(map);
            }
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }

        return createdGroup;
    }
}
