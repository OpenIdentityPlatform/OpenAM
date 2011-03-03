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
 * $Id: ContDeleteRoleReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.PrintUtils;
import java.io.PrintWriter;
import java.io.StringWriter;

class ContDeleteRoleReq extends AddDeleteReq {
    /**
     * Constructs a new ContDeleteRoleReq.
     *
     * @param targetDN the Container DN. 
     */        
    ContDeleteRoleReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription9") +
            " " + targetDN);

        if (DNSet.isEmpty()) {
            prnWriter.println("  DN set is empty");
        } else {
            prnUtl.printSet(DNSet, 1);
        }

        prnWriter.flush();
        return stringWriter.toString();    
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("container") + " " +
            targetDN + "\n" + bundle.getString("deleteroles"));

        try {
            doLogStringSet(DNSet, AdminUtils.DELETE_ROLE_ATTEMPT);

            AMOrganizationalUnit orgUnit =
                dpConnection.getOrganizationalUnit(targetDN);

            orgUnit.deleteRoles(DNSet);
            PrintUtils prnUtl = new PrintUtils(writer);
            prnUtl.printSet(DNSet, 1);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(AdminReq.bundle.getString("container") + " " + 
                    targetDN + "\n" + bundle.getString("deletedroles") +
                    ": " + DNSet);
            }

//            doLogStringSet(DNSet, "delete-role");
            doLogStringSet(DNSet, AdminUtils.DELETE_ROLE);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
