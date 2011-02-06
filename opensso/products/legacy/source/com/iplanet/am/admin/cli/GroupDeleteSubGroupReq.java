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
 * $Id: GroupDeleteSubGroupReq.java,v 1.2 2008/06/25 05:52:27 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

class GroupDeleteSubGroupReq extends AddDeleteReq {
    protected boolean recursiveDelete = false;

    /**
     * Constructs a new GroupDeleteSubGroupReq.
     *
     * @param  targetDN the Group DN. 
     */        
    GroupDeleteSubGroupReq(String targetDN) {
        super(targetDN);
    }
    
    /**
     * Set the value for true or false for recursive deltes.
     *
     * @param recDelete.
     */
    void setRecursiveDelete(boolean recDelete) {
        recursiveDelete = recDelete;
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
        prnWriter.println(bundle.getString("requestdescription21") + targetDN);

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
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("group") + " " + targetDN + "\n" +
                bundle.getString("deletegroups"));
        }
                                                                                
        writer.println(bundle.getString("group") + " " + targetDN +
            "\n" + bundle.getString("deletegroups"));

        for (Iterator iter = DNSet.iterator(); iter.hasNext(); ) {
            String dn = (String)iter.next();
            GroupUtils.deleteGroup(this, dn, dpConnection, bundle,
                recursiveDelete);
            writer.println(dn);
            }
    }
}
