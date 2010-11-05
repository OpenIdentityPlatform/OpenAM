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
 * $Id: PCGetUserReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMPeopleContainer;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMUser;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

class PCGetUserReq extends GetUserReq {
    /**
     * Constructs a new PCGetUserReq.
     *
     * @param targetDN Description of the Parameter
     */
    PCGetUserReq(String targetDN) {
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
        prnWriter.println(bundle.getString("requestdescription52") +
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
     * Prints all the Users information for a PeopleContainer based
     * on the values if the User DNs set is empty than it prints all the Users.
     * if DNsOnly is true than it prints only the DNs of the Users else it
     * prints all the information of the all the Users.
     *
     * @param dpConnection Access Management Store Connection.
     * @exception AdminException if user information cannot be obtained.
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);
        writer.println(bundle.getString("peoplecontainer") + " " + targetDN +
            "\n" + bundle.getString("getusers"));

        try {
            writer.println(targetDN);
            AMPeopleContainer pcr = dpConnection.getPeopleContainer(targetDN);
            boolean needValidation = false;

            if (userDNs.isEmpty()) {
                AMSearchControl searchCtrl = createSearchControl(
                    AMConstants.SCOPE_ONE);
                AMSearchResults searchResults = pcr.searchUsers(
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
                    (user.isExists() && AdminUtils.isChildOf(user, targetDN))
                ) {
                    UserUtils.printUserInformation(prnUtl, dn, dpConnection,
                        DNsOnly);
                }
            }

            printSearchLimitError();
        } catch (AMException dpe) {
            throw new AdminException(dpe);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
