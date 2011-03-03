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
 * $Id: GroupGetUserReq.java,v 1.2 2008/06/25 05:52:27 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMStaticGroup;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;

class GroupGetUserReq extends GetUserReq {

    /**
     * Constructs a new GroupGetUserReq.
     *
     * @param targetDN  Description of the Parameter
     */
    GroupGetUserReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription24") +
            " " + targetDN);
        prnWriter.println("   DNsOnly =" + DNsOnly);
        
        if (userDNs.isEmpty()) {
            prnWriter.println("  DN set is empty");
        } else {
            prnUtl.printSet(userDNs, 2);
        }
        
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * Prints all the users information for a group based on the values if the
     * user DNs set is empty than it prints all the users. If
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

        if (AdminUtils.logEnabled() && (AdminUtils.debugEnabled)) {
            AdminUtils.log(AdminReq.bundle.getString("getusers") +
                AdminReq.bundle.getString("group") + targetDN);
        }

        AdminReq.writer.println(AdminReq.bundle.getString("group") + " " + 
            targetDN + "\n" + AdminReq.bundle.getString("getusers"));

        Set DNs = getUserDNs(dpConnection);

        if (userDNs.isEmpty()) {
            for (Iterator iter = DNs.iterator(); iter.hasNext(); ) {
                String dn = iter.next().toString();
                UserUtils.printUserInformation(prnUtl, dn, dpConnection,
                    DNsOnly);
            }
        } else {
            for (Iterator iter = userDNs.iterator(); iter.hasNext(); ) {
                String dn = iter.next().toString();
                    
                if (DNs.contains(dn)) {
                    UserUtils.printUserInformation(prnUtl, dn, dpConnection,
                        DNsOnly);
                }
            }
        }
    }

    protected Set getUserDNs(AMStoreConnection dpConnection)
        throws AdminException
    {
        try {
            AMStaticGroup grp = dpConnection.getStaticGroup(targetDN);
            return grp.getUserDNs();
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
