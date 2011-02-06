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
 * $Id: GroupGetNestedGroupReq.java,v 1.2 2008/06/25 05:52:27 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMGroup;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStaticGroup;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class GroupGetNestedGroupReq extends AdminReq {
    private Set nestedGroupDNs = new HashSet();
    private boolean DNsOnly = true;

    GroupGetNestedGroupReq(String targetDN) {
        super(targetDN);
    }

    void setDNsOnly(boolean DNsOnly) {
        this.DNsOnly = DNsOnly;
    }

    void addNestedGroupDNs(String nestedGroupDN) {
        nestedGroupDNs.add(nestedGroupDN);
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription94") +
            " " + targetDN);
        prnWriter.println("   DNsOnly =" + DNsOnly);
        
        if (nestedGroupDNs.isEmpty()) {
            prnWriter.println("  DN set is empty");
        } else {
            prnUtl.printSet(nestedGroupDNs, 2);
        }
        
        prnWriter.flush();
        return stringWriter.toString();
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);

        AdminReq.writer.println(AdminReq.bundle.getString("group") + " " + 
            targetDN + "\n" + AdminReq.bundle.getString("getnestedgroups"));

        Set DNs = getNestedGroupDNs(dpConnection);

        if (nestedGroupDNs.isEmpty()) {
            for (Iterator iter = DNs.iterator(); iter.hasNext(); ) {
                String dn = iter.next().toString();
                printInformation(prnUtl, dn, dpConnection, DNsOnly);
            }
        } else {
            for (Iterator iter = nestedGroupDNs.iterator(); iter.hasNext(); ) {
                String dn = iter.next().toString();
                    
                if (DNs.contains(dn)) {
                    printInformation(prnUtl, dn, dpConnection, DNsOnly);
                }
            }
        }
    }

    protected Set getNestedGroupDNs(AMStoreConnection dpConnection)
        throws AdminException
    {
        try {
            AMStaticGroup grp = dpConnection.getStaticGroup(targetDN);
            return grp.getNestedGroupDNs();
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    void printInformation(
        PrintUtils prnUtl,
        String nestedGroupDN,
        AMStoreConnection connection,
        boolean isDNsOnly
    ) throws AdminException
    {
        try {
            AdminReq.writer.println("  " + nestedGroupDN);

            if (!isDNsOnly) {
                AMGroup group = null;
                int type = connection.getAMObjectType(nestedGroupDN);

                switch (type) {
                case AMObject.GROUP:
                case AMObject.STATIC_GROUP:
                    group = connection.getStaticGroup(nestedGroupDN);
                    break;
                case AMObject.DYNAMIC_GROUP:
                    group = connection.getDynamicGroup(nestedGroupDN);
                    break;
                case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                    group = connection.getAssignableDynamicGroup(nestedGroupDN);
                    break;
                }

                if (group != null) {
                    prnUtl.printAVPairs(group.getAttributes(), 2);
                }
            }
        } catch (AMException ame) {
            throw new AdminException(ame.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
