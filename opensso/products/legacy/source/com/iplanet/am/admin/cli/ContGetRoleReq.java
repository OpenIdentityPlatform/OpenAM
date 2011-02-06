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
 * $Id: ContGetRoleReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

class ContGetRoleReq extends SearchReq {
    private String level = "SCOPE_SUB";

    /**
     * Constructs a new ContGetRoleReq.
     *
     * @param targetDN the Container DN.
     */
    ContGetRoleReq(String targetDN) {
        super(targetDN);
    }

    /**
     * sets the value for level which tells the process() method to get roles
     * under one level or the entire sub tree.
     *
     * @param level if SCOPE_ONE get roles up to one level , if SCOPE_SUB get
     *        all the roles.
     */
    void setLevel(String level) {
        this.level = level;
    }

    /**
     * gets the value for level which tells the process() method to get roles
     * under one level or the entire sub tree.
     *
     * @return level if SCOPE_ONE get roles up to one level , if SCOPE_SUB
     *         get all the roles.
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription16") +
            " " + targetDN);
        prnWriter.println("   level = " + level);
        prnWriter.println("   filter = " + filter);
        prnWriter.println("   sizeLimit = " + sizeLimit);
        prnWriter.println("   timeLimit = " + timeLimit);
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * Prints all the Roles information based on the values if the
     * Role DNs set is empty than it prints all the Roles. If level is SCOPE_ONE
     * than it prints Role Information upto one level else it prints all the
     * information of the all the Roles.
     *
     * @param dpConnection        the AMStoreConnection.
     * @exception AdminException  Description of the Exception
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);
        writer.println(bundle.getString("container") + " " + "\n" +
            bundle.getString("getroles"));
        
        try {

            AMOrganizationalUnit orgUnit =
                dpConnection.getOrganizationalUnit(targetDN);
            AdminReq.writer.println(targetDN);
            int scope = (level.equals("SCOPE_SUB"))
                ? AMConstants.SCOPE_SUB : AMConstants.SCOPE_ONE;

            AMSearchControl searchCtrl = createSearchControl(scope);
            AMSearchResults searchResults1 = orgUnit.searchRoles(
                filter, searchCtrl);
            if (searchResults1.getErrorCode() != AMSearchResults.SUCCESS) {
                errorCode = searchResults1.getErrorCode();
            }
            Set roleDNs = searchResults1.getSearchResults();
            RoleUtils.printRoleInformation(prnUtl, roleDNs, dpConnection,
                bundle, AMObject.ROLE);

            AMSearchResults searchResults2 = orgUnit.searchFilteredRoles(
                filter, searchCtrl);
            if (searchResults2.getErrorCode() != AMSearchResults.SUCCESS) {
                errorCode = searchResults2.getErrorCode();
            }
            Set filteredRoleDNs = searchResults2.getSearchResults();
            RoleUtils.printRoleInformation(prnUtl, filteredRoleDNs,
                dpConnection, bundle, AMObject.FILTERED_ROLE);

            printSearchLimitError();
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
