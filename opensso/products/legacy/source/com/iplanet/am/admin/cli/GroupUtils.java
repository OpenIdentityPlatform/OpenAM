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
 * $Id: GroupUtils.java,v 1.2 2008/06/25 05:52:28 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMAssignableDynamicGroup;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMDynamicGroup;
import com.iplanet.am.sdk.AMGroup;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStaticGroup;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.PrintUtils;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * The <code>AdminUtils</code> class provides common group helper methods.
 */
class GroupUtils {
    /**
     * Returns group type.
     *
     * @param dn Distinguished name of group object.
     * @param connection Store connection object.
     * @param bundle resource bundle
     * @return group type.
     */
    static int getGroupType(String dn, AMStoreConnection connection,
        ResourceBundle bundle)
        throws AdminException
    {
        int groupType = -1;
        try {
            groupType = connection.getAMObjectType(dn);

            switch (groupType) {
            case AMObject.GROUP:
            case AMObject.DYNAMIC_GROUP:
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                break;
            default:
                throw new AdminException(bundle.getString(
                    "invalidStaticGroupDN"));
            }
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }

        return groupType;
    }

    /**
     * Returns a group object.
     *
     * @param dn Distinguished name of group object.
     * @param connection Store connection object.
     * @param bundle resource bundle.
     */
    static AMObject getGroupObject(String dn, AMStoreConnection connection,
        ResourceBundle bundle)
        throws AdminException
    {
        AMObject object = null;
        int groupType = getGroupType(dn, connection, bundle);

        try {
            switch (groupType) {
            case AMObject.GROUP:
                object = connection.getStaticGroup(dn);
                break;
            case AMObject.DYNAMIC_GROUP:
                object = connection.getDynamicGroup(dn);
                break;
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                object = connection.getAssignableDynamicGroup(dn);
                break;
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }

        return object;
    }

    /**
     * Deletes a group.
     *
     * @param request administration request object.
     * @param dn Distinguished name of group object.
     * @param connection Store connection object.
     * @param bundle resource bundle.
     * @param recursive true to do recursive delete.
     */
    static void deleteGroup(AdminReq request, String dn,
        AMStoreConnection connection, ResourceBundle bundle, boolean recursive)
        throws AdminException
    {
        int groupType = getGroupType(dn, connection, bundle);

        try {
            switch (groupType) {
            case AMObject.GROUP:
                AMStaticGroup staticGroup = connection.getStaticGroup(dn);
                request.doLog(staticGroup, AdminUtils.DELETE_GROUP_ATTEMPT);
                staticGroup.delete(recursive);
//                request.doLog(staticGroup, "delete-group");
                request.doLog(staticGroup, AdminUtils.DELETE_GROUP);
                break;
            case AMObject.DYNAMIC_GROUP:
                AMDynamicGroup dynGroup = connection.getDynamicGroup(dn);
                request.doLog(dynGroup, AdminUtils.DELETE_GROUP_ATTEMPT);
                dynGroup.delete(recursive);
//                request.doLog(dynGroup, "delete-group");
                request.doLog(dynGroup, AdminUtils.DELETE_GROUP);
                break;
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                AMAssignableDynamicGroup assignDynGroup =
                    connection.getAssignableDynamicGroup(dn);
                request.doLog(assignDynGroup, AdminUtils.DELETE_GROUP_ATTEMPT);
                assignDynGroup.delete(recursive);
//                request.doLog(assignDynGroup, "delete-group");
                request.doLog(assignDynGroup, AdminUtils.DELETE_GROUP);
                break;
            }
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    /**
     * Returns group object based on group type.
     *
     * @param dn distinguished name of group.
     * @param connection Store connection object.
     * @param groupType type of group.
     * @return group object based on group type.
     */
    static AMGroup getGroup(String dn, AMStoreConnection connection,
        int groupType)
        throws AdminException
    {
        AMGroup group = null;
        
        try {
            switch (groupType) {
            case AMObject.STATIC_GROUP:
                group = connection.getStaticGroup(dn);
                break;
            case AMObject.DYNAMIC_GROUP:
                group = connection.getDynamicGroup(dn);
                break;
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                group = connection.getAssignableDynamicGroup(dn);
                break;
            }
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
        
        return group;        
    }

    /**
     * Prints group information on line.
     *
     * @param prnUtl Print writer.
     * @param groupDNs Set of group distinguished names.
     * @param connection Store connection object.
     * @param bundle Resource bundle.
     * @param groupType type of group.
     */
    static void printGroupInformation(PrintUtils prnUtl, Set groupDNs,
        AMStoreConnection connection, ResourceBundle bundle, int groupType)
        throws AdminException
    {
        try {
            for (Iterator iter = groupDNs.iterator(); iter.hasNext(); ) {
                String dn = (String)iter.next();
                AMGroup group = getGroup(dn, connection, groupType);
                Map values = group.getAttributes();
                AdminReq.writer.println("  " + dn);
                prnUtl.printAVPairs(values, 2);

                if (AdminUtils.logEnabled()) {
                    AdminUtils.log(bundle.getString("statusmsg27"));
                    prnUtl.printAVPairs(values, 2);
                }
            }
        } catch (AMException ame) {
            throw new AdminException(ame.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}

