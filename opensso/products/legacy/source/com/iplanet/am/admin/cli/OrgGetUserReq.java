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
 * $Id: OrgGetUserReq.java,v 1.2 2008/06/25 05:52:30 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMUser;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

class OrgGetUserReq extends GetUserReq {

    /**
     * Constructs a new OrgGetUserReq.
     *
     * @param targetDN  the Organization DN.
     */
    OrgGetUserReq(String targetDN) {
        super(targetDN);
    }

    /**
     * converts this object into a string.
     *
     * @return String. the values of the dnset in print format.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter);
        prnWriter.println(AdminReq.bundle.getString("requestdescription41") +
            " " + targetDN);
        prnWriter.println("   DNsOnly = " + DNsOnly);
        prnWriter.println("   filter = " + filter);
        prnWriter.println("   sizeLimit = " + sizeLimit);
        prnWriter.println("   timeLimit = " + timeLimit);
        
        if (userDNs.isEmpty()) {
            prnWriter.println("  DN set is empty");
        } else {
            prnUtl.printSet(userDNs, 2);
        }
        
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * Prints all the Users information for an Organization based on
     * the values if the User DNs set is empty than it prints all the Users. If
     * DNsOnly is true than it prints only the DNs of the Users else it prints
     * all the information of the all the Users.
     *
     * @param dpConnection AMStoreConnection.
     * @exception AdminException
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);
        writer.println(bundle.getString("organization") + " " + targetDN +
            "\n" + bundle.getString("getusers"));

        try {
            writer.println(targetDN);
            AMOrganization org = dpConnection.getOrganization(targetDN);
            boolean needValidation = false;

            if (userDNs.isEmpty()) {
                AMSearchControl searchCtrl = createSearchControl(
                    AMConstants.SCOPE_SUB);
                AMSearchResults searchResults = org.searchUsers(
                    filter, searchCtrl);
                userDNs = searchResults.getSearchResults();
                errorCode = searchResults.getErrorCode();
            } else {
                needValidation = true;
            }

            for (Iterator iter = userDNs.iterator(); iter.hasNext(); ) {
                String dn = iter.next().toString();
                AMUser user = dpConnection.getUser(dn);
                                                                                
                if (!needValidation ||
                    (user.isExists() &&
                        AdminUtils.isDescendantOf(user, targetDN,
                            AMConstants.SCOPE_SUB))
                ) {
                    UserUtils.printUserInformation(prnUtl, dn, dpConnection,
                        DNsOnly);
                }
            }

            printSearchLimitError();

        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        } catch (Exception e) {
            throw new AdminException(e.toString());
        }
    }
}
