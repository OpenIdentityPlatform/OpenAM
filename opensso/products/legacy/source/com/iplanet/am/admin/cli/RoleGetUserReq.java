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
 * $Id: RoleGetUserReq.java,v 1.2 2008/06/25 05:52:34 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;

class RoleGetUserReq extends GetUserReq {

    /**
     * Constructs a new RoleGetUserReq.
     *
     * @param targetDN   Description of the Parameter
     */
    RoleGetUserReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription56") +
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
     * This method prints all the Users information for a Role based on the
     * values if the User DNs set is empty than it prints all the Users.
     * If DNsOnly is true than it prints only the DNs of the Users else it
     * prints all the information of the all the Users.
     *
     * @param dpConnection AMStoreConnection.
     * @exception AdminException
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);
        
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(AdminReq.bundle.getString("role") + " " + targetDN +
                "\n" + bundle.getString("getusers"));
        }

        AdminReq.writer.println(AdminReq.bundle.getString("role") + " " +
            targetDN + "\n" + AdminReq.bundle.getString("getusers"));
        
        Set DNs = getUserDNs(dpConnection);
        AdminReq.writer.println(targetDN);

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
            AMRole role = dpConnection.getRole(targetDN);
            return role.getUserDNs();
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
