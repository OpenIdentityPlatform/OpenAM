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
 * $Id: ContGetGroupReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMGroupContainer;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class ContGetGroupReq extends SearchReq {
    private String level = "SCOPE_SUB";

    /**
     * Constructs a new ContGetGroupReq.
     *
     * @param targetDN  the Container DN.
     */
    ContGetGroupReq(String targetDN) {
        super(targetDN);
    }

    /**
     * sets the value for level which tells the process() method to get groups
     * under one level or the entire sub tree.
     *
     * @param level  if SCOPE_ONE get groups up to one level , if SCOPE_SUB get
     *      all the groups.
     */
    void setLevel(String level) {
        this.level = level;
    }

    /**
     * gets the value for level which tells the process() method to get groups
     * under one level or the entire sub tree.
     *
     * @return level if SCOPE_ONE get groups up to one level , if SCOPE_SUB
     *      get all the groups.
     */
    String getLevel() {
        return level;
    }

    /**
     * converts this object into a string.
     *
     * @return String. the values of the dnset in print format.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        prnWriter.println(bundle.getString("requestdescription12") +
            " " + targetDN);
        prnWriter.println("   level = " + level);
        prnWriter.println("   filter = " + filter);
        prnWriter.println("   sizeLimit = " + sizeLimit);
        prnWriter.println("   timeLimit = " + timeLimit);
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * This method prints all the Groups information for an Container based on
     * the values if the Group DNs set is empty than it prints all the Groups.
     * if level is SCOPE_ONE than it prints Group Information upto one level
     * else it prints all the information of the all the Groups.
     *
     * @param dpConnection AMStoreConnection.
     * @exception AdminException
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        writer.println(bundle.getString("container") + " " + targetDN + "\n" +
            bundle.getString("getgroups"));

        try {
            AMOrganizationalUnit orgUnit =
                dpConnection.getOrganizationalUnit(targetDN);
            int scope = level.equals("SCOPE_SUB") 
                ? AMConstants.SCOPE_SUB : AMConstants.SCOPE_ONE;
            AMSearchControl searchCtrl = createSearchControl(scope);

            if (scope == AMConstants.SCOPE_ONE) {
                Set groupContainers =
                    orgUnit.getGroupContainers(AMConstants.SCOPE_ONE);

                if ((groupContainers != null) && !groupContainers.isEmpty()) {
                    for (Iterator iter = groupContainers.iterator();
                        iter.hasNext();
                    ) {
                        String gcDN = (String)iter.next();
                        AdminReq.writer.println(gcDN);
                        AMGroupContainer gc =
                            dpConnection.getGroupContainer(gcDN);
                        searchGroups(dpConnection, gc, searchCtrl);
                    }
                }
            } else {
                searchGroups(dpConnection, orgUnit, searchCtrl);
            }

            printSearchLimitError();
        } catch (AMException ame) {
            throw new AdminException(ame.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }

    private void searchGroups(
        AMStoreConnection dpConnection,
        AMOrganizationalUnit ou,
        AMSearchControl searchCtrl
    ) throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);

        try {
            AMSearchResults searchResults1 = ou.searchStaticGroups(
                filter, searchCtrl);
            if (searchResults1.getErrorCode() != AMSearchResults.SUCCESS) {
                errorCode = searchResults1.getErrorCode();
            }
            Set staticGroups = searchResults1.getSearchResults();

            AMSearchResults searchResults2 =
                ou.searchDynamicGroups(filter, searchCtrl);
            if (searchResults2.getErrorCode() != AMSearchResults.SUCCESS) {
                errorCode = searchResults2.getErrorCode();
            }
            Set dynamicGroups = searchResults2.getSearchResults();

            AMSearchResults searchResults3 =
                ou.searchAssignableDynamicGroups(filter, searchCtrl);
            if (searchResults3.getErrorCode() != AMSearchResults.SUCCESS) {
                errorCode = searchResults3.getErrorCode();
            }
            Set assignableDynamicGroups = searchResults3.getSearchResults();

            GroupUtils.printGroupInformation(prnUtl, staticGroups, dpConnection,
                bundle, AMObject.STATIC_GROUP);
            GroupUtils.printGroupInformation(prnUtl, dynamicGroups,
                dpConnection, bundle, AMObject.DYNAMIC_GROUP);
            GroupUtils.printGroupInformation(prnUtl, assignableDynamicGroups,
                dpConnection, bundle, AMObject.ASSIGNABLE_DYNAMIC_GROUP);
        } catch (AMException ame) {
            throw new AdminException(ame.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }

    private void searchGroups(
        AMStoreConnection dpConnection,
        AMGroupContainer groupContainer,
        AMSearchControl searchCtrl
    )  throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);

        try {
            AMSearchResults searchResults = groupContainer.searchGroups(
                filter, null, searchCtrl);
            Set groupDNs = searchResults.getSearchResults();
            if (searchResults.getErrorCode() != AMSearchResults.SUCCESS) {
                errorCode = searchResults.getErrorCode();
            }

            if ((groupDNs != null) && !groupDNs.isEmpty()) {
                Set staticGroups = new HashSet();
                Set dynamicGroups = new HashSet();
                Set assignableDynamicGroups = new HashSet();

                for (Iterator iter = groupDNs.iterator(); iter.hasNext(); ) {
                    String dn = (String)iter.next();
                    int groupType = GroupUtils.getGroupType(
                        dn, dpConnection, bundle);

                    switch (groupType) {
                    case AMObject.GROUP:
                        staticGroups.add(dn);
                        break;
                    case AMObject.DYNAMIC_GROUP:
                        dynamicGroups.add(dn);
                        break;
                    case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                        assignableDynamicGroups.add(dn);
                        break;
                    }
                }

                GroupUtils.printGroupInformation(prnUtl, staticGroups,
                    dpConnection, bundle, AMObject.STATIC_GROUP);
                GroupUtils.printGroupInformation(prnUtl, dynamicGroups,
                    dpConnection, bundle, AMObject.DYNAMIC_GROUP);
                GroupUtils.printGroupInformation(prnUtl,
                    assignableDynamicGroups, dpConnection, bundle,
                    AMObject.ASSIGNABLE_DYNAMIC_GROUP);
            }
        } catch (AMException ame) {
            throw new AdminException(ame.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}

