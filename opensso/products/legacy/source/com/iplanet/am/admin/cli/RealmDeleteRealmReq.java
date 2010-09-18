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
 * $Id: RealmDeleteRealmReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.OrganizationConfigManager;
import java.io.PrintWriter;
import java.io.StringWriter;

class RealmDeleteRealmReq extends AdminReq {
    private boolean recursiveDelete = false;
    private String realmPath = null;
    
    /**
     * Constructs a new RealmDeleteRealmReq.
     *
     * @param  targetDN the Realm to delete. 
     */        
    RealmDeleteRealmReq(String targetDN) {
        //
        //  a "slash" format path, rather than DN...
        //
        super(targetDN);
        realmPath = targetDN;
    }

    /**
     * Sets the value for true or false for recursive deletes
     *
     * @param recDelete
     */
    void setRecursiveDelete(boolean recDelete) {
        recursiveDelete = recDelete;
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
        if (recursiveDelete) {
            prnWriter.println(AdminReq.bundle.getString(
                    "requestdescription100r") + " " + targetDN);
        } else {
            prnWriter.println(AdminReq.bundle.getString(
                    "requestdescription100n") + " " + targetDN);
        }
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        String tempS = "deleteRealm";
        String tempSrnr = "Nonrecursively";
        if (recursiveDelete) {
            tempSrnr = "Recursively";
        }

        AdminReq.writer.println(bundle.getString(tempSrnr) + " " +
            bundle.getString(tempS) + " " + targetDN);

        String[] args = {bundle.getString(tempSrnr), realmPath};
        try {
            doLog(args, AdminUtils.DELETE_REALM_ATTEMPT);
            OrganizationConfigManager ocm =
                new OrganizationConfigManager(ssoToken, realmPath);

            ocm.deleteSubOrganization (null, recursiveDelete);

            doLog(args, AdminUtils.DELETE_REALM);
        } catch (SMSException smse) {
            throw new AdminException(smse);
        }
    }
}
