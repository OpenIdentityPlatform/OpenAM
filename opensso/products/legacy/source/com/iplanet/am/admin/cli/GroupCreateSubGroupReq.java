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
 * $Id: GroupCreateSubGroupReq.java,v 1.2 2008/06/25 05:52:27 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMDynamicGroup;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStaticGroup;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class GroupCreateSubGroupReq extends AdminReq {
    protected Map values;
    protected String groupDN;
    protected String groupType;

    /**
     * Constructs a new GroupCreateSubGroupReq.
     *
     * @param  targetDN the Group DN. 
     */        
    GroupCreateSubGroupReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the Group DN and its avPair Map to the subGroupReq Map.
     *
     * @param subGroupDN the SubGroup DN
     * @param type of group to create.
     * @param avPair the Map which contains the attribute as key and value.
     */        
    void addSubGroupReq(String subGroupDN, String type, Map avPair) {
        groupType = type;
        groupDN = subGroupDN;
        values = avPair;
    }
        
    /**
     * converts this object into a string.
     *
     * @return String. 
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(bundle.getString("requestdescription20") +
            " " + targetDN);
        Map map = new HashMap();
        map.put(groupDN, values);
        AdminUtils.printAttributeNameValuesMap(prnWriter, prnUtl, map);
        prnWriter.flush();
        return stringWriter.toString();    
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("group") + " " + targetDN + "\n" +
                bundle.getString("creategroups"));
        }
                                                                                
        AdminReq.writer.println(bundle.getString("group") + " " + targetDN +
            "\n" + bundle.getString("creategroups"));

        Set groups = createGroups(dpConnection);
        for (Iterator iter = groups.iterator(); iter.hasNext(); ) {
            AMObject obj = (AMObject)iter.next();
            writer.println(obj.getDN());
        }

//        doLog(groups, "create-group");
        doLog(groups, AdminUtils.CREATE_GROUP);
    }

    protected Set createGroups(AMStoreConnection dpConnection)
        throws AdminException
    {
        try {
            String[] args = {groupDN};
            doLog(args, AdminUtils.CREATE_GROUP_ATTEMPT);
            AMStaticGroup grp = dpConnection.getStaticGroup(targetDN);
            return createGroup(grp);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    protected Set createGroup(AMStaticGroup group)
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
                
                if ((values != null) && (values.get(GROUP_FILTER_INFO) != null))
                {
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
