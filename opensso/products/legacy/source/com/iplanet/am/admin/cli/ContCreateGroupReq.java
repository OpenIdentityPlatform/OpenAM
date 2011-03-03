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
 * $Id: ContCreateGroupReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMDynamicGroup;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMGroupContainer;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.PrintUtils;
import com.sun.identity.common.admin.AdminInterfaceUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ContCreateGroupReq extends AdminReq {
    private Map staticGroups = new HashMap();
    private Map dynamicGroups = new HashMap();
    private Map assignableDynamicGroups = new HashMap();

    /**
     * Constructs a new ContCreateGroupReq.
     *
     * @param targetDN the Container DN. 
     */        
    ContCreateGroupReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the group dn's to a Set which holds all the group dn's.
     *
     * @param groupDN the DN of a group
     * @param type of group to create
     * @param avPairs the Map which contains the attribute as key and value.
     */
    void addGroupReq(String groupDN, String type, Map avPairs) {
        if (type.equals(GROUP_TYPE_ASSIGNABLE_DYNAMIC)) {
            assignableDynamicGroups.put(groupDN, avPairs);
        } else if (type.equals(GROUP_TYPE_DYNAMIC)) {
            dynamicGroups.put(groupDN, avPairs);
        } else {
            staticGroups.put(groupDN, avPairs);
        }
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription2") +
            " " + targetDN);

        if (!staticGroups.isEmpty()) {
            prnUtl.printSet(staticGroups.keySet(), 1);
        }

        if (!dynamicGroups.isEmpty()) {
            prnUtl.printSet(dynamicGroups.keySet(), 1);
        }

        if (!assignableDynamicGroups.isEmpty()) {
            prnUtl.printSet(assignableDynamicGroups.keySet(), 1);
        }

        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("container") + " " + targetDN +
                "\n" + bundle.getString("creategroups"));
        }

        writer.println(bundle.getString("container") + " " + targetDN +
            "\n" + bundle.getString("creategroups"));
        PrintUtils prnUtl = new PrintUtils(writer);

        String[] params = new String[2];
        try {
            AMOrganizationalUnit ou =
                dpConnection.getOrganizationalUnit(targetDN);
            AMGroupContainer groupContainer = getDefaultGroupContainer(
                dpConnection, ou);

            if (!staticGroups.isEmpty()) {
                logAttempts(staticGroups.keySet(), GROUP_TYPE_STATIC);
                Set set = groupContainer.createStaticGroups(
                    staticGroups.keySet());
                prnUtl.printSet(staticGroups.keySet(), 1);
                doLog(set, AdminUtils.CREATE_GROUP);
            }

            if (!dynamicGroups.isEmpty()) {
                Set groupNames = dynamicGroups.keySet();
                logAttempts(groupNames, GROUP_TYPE_DYNAMIC);
                String groupDN = (String)groupNames.iterator().next();
                Map values = (Map)dynamicGroups.get(groupDN);
                String strFilter = null;

                if ((values != null) && (values.get(GROUP_FILTER_INFO) != null))
                {
                    Set set = (Set)values.remove(GROUP_FILTER_INFO);
                    if ((set != null) && !set.isEmpty()) {
                        strFilter = (String)set.iterator().next();
                    }
                }

                Set setGroup = groupContainer.createDynamicGroups(
                    dynamicGroups);
                AMDynamicGroup group =
                    (AMDynamicGroup)setGroup.iterator().next();

                if (strFilter != null) {
                    group.setFilter(strFilter);
                    group.store();
                }

                prnUtl.printSet(groupNames, 1);
                doLog(group, AdminUtils.CREATE_GROUP);
            }

            if (!assignableDynamicGroups.isEmpty()) {
                logAttempts(assignableDynamicGroups.keySet(), 
                    GROUP_TYPE_ASSIGNABLE_DYNAMIC);
                Set set = groupContainer.createAssignableDynamicGroups(
                    assignableDynamicGroups.keySet());
                prnUtl.printSet(assignableDynamicGroups.keySet(), 1);
                doLog(set, AdminUtils.CREATE_GROUP);
            }

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(AdminReq.bundle.getString("statusmsg21"));
            }
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    private void logAttempts(Set groupNames, String groupType) {
        for (Iterator i = groupNames.iterator(); i.hasNext(); ) {
            String groupName = (String)i.next();
            String[] params = {groupName, groupType, targetDN};
            doLog(params, AdminUtils.CREATE_GROUP_ATTEMPT);
        }
    }

    private AMGroupContainer getDefaultGroupContainer(
        AMStoreConnection connection,
        AMOrganizationalUnit orgUnit)
        throws AdminException
    {
        AMGroupContainer groupContainer = null;

        try {
            String dn = AdminInterfaceUtils.getNamingAttribute(
                    AMObject.GROUP_CONTAINER, debug) +
                "=" + AdminInterfaceUtils.defaultGroupContainerName() + "," +
                orgUnit.getDN();
            groupContainer = connection.getGroupContainer(dn);

            if ((groupContainer == null) || !groupContainer.isExists()) {
                String[] strArray = {dn};
                String errorMesage = MessageFormat.format(
                    bundle.getString("defaultGroupContainerNoFound"), strArray);
                throw new AdminException(errorMesage);
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }

        return groupContainer;
    }
}
