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
 * $Id: GroupGetSubGroupReq.java,v 1.2 2008/06/25 05:52:27 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStaticGroup;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.io.StringWriter;
import java.io.PrintWriter;

class GroupGetSubGroupReq extends AdminReq {

    private Set subGroupDNs = new HashSet();
    private String level = "SCOPE_SUB";
    private boolean DNsOnly = true;

    /**
     * Constructs a new GroupGetSubGroupReq.
     *
     * @param  targetDN the Group DN. 
     */ 
    GroupGetSubGroupReq(String targetDN) {
        super(targetDN);
    }

    /**
     * sets the value for level  which tells the process() method to get 
     * groups under one level or the entire sub tree.
     *
     * @param level if SCOPE_ONE get Sub Groups up to one level, if
     *        SCOPE_SUB get all the Sub Groups. 
     */
    void setLevel(String level) {
        this.level = level;
    }
    
    /**
     * sets the value for DNsOnly  which tells the process() method to get 
     * only the DNs or all the information.
     *
     * @param  DNsOnly if true only DN's , if false all the information. 
     */
    void setDNsOnly(boolean DNsOnly) {
        this.DNsOnly = DNsOnly;
    }
    
    /**
     * adds the subGroup dn to Set subGroupDNs which holds all the subGroup
     * dn's.
     *
     * @param  subGroupDN the DN of a subGroup
     */
    void addSubGroupDNs(String subGroupDN) {
        subGroupDNs.add(subGroupDN);
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription23") +
            " " + targetDN);
        prnWriter.println("  level = " + level);
        prnWriter.println("  DNsOnly = " + DNsOnly);

        if (subGroupDNs.isEmpty()) {
            prnWriter.println("  DN set is empty");
        } else {
            prnUtl.printSet(subGroupDNs,2);
        }

        prnWriter.flush();
        return stringWriter.toString();    
    }
   
    /**
     * This code takes each dn from the subGroupDNs set and gets
     * the Group object and gets its SubGroups
     * based on the level and prints the information based on the
     * value of DNsONly. 
     *
     * @param dpConnection the AMStoreConnection
     * throws AdminException if any error occurs while getting the information.
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(AdminReq.bundle.getString("getsubgroups") +
                targetDN);
        }

        AdminReq.writer.println(AdminReq.bundle.getString("group") + " " +
            targetDN + "\n" + AdminReq.bundle.getString("getsubgroups"));

        try {
            boolean needValidation = false;
            int scope = (level.equals("SCOPE_SUB"))
                ? AMConstants.SCOPE_SUB : AMConstants.SCOPE_ONE;

            if (subGroupDNs.isEmpty()) {
                subGroupDNs = getSubGroups(dpConnection, scope);
            } else {
                needValidation = true;
            }

            for (Iterator iter = subGroupDNs.iterator(); iter.hasNext(); ) {
                String dn = (String)iter.next();
                AMObject gp = GroupUtils.getGroupObject(
                    dn, dpConnection, bundle);

                if (!needValidation ||
                    (gp.isExists() &&
                        AdminUtils.isDescendantOf(gp, targetDN, scope))
                ) {
                    AdminReq.writer.println("  " + dn);

                    if (!DNsOnly) {
                        prnUtl.printAVPairs(gp.getAttributes() ,2);
                    }
                }
            }
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    protected Set getSubGroups(AMStoreConnection dpConnection, int scope) 
        throws AdminException
    {
        try {
            AMStaticGroup grp = dpConnection.getStaticGroup(targetDN);
            return grp.searchGroups("*", scope);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
