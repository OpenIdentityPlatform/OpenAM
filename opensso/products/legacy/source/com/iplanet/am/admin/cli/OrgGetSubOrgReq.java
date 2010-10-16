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
 * $Id: OrgGetSubOrgReq.java,v 1.2 2008/06/25 05:52:30 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.io.PrintWriter;
import java.io.StringWriter;

class OrgGetSubOrgReq extends SearchReq {
    private Set subOrgDNs = new HashSet();;
    private boolean DNsOnly = true;

    /**
     * Constructs a new OrgGetSubOrgReq.
     *
     * @param targetDN the Organization DN.
     */
    OrgGetSubOrgReq(String targetDN) {
        super(targetDN);
    }

    /**
     * sets the value for DNsOnly which tells the process() method to get only
     * the DNs or all the information.
     *
     * @param DNsOnly  The new dNsOnly value
     */
    void setDNsOnly(boolean DNsOnly) {
        this.DNsOnly = DNsOnly;
    }

    /**
     * gets the value of DNsOnly which tells the process() method to get only
     * the DNs or all the information.
     *
     * @return       DNsOnly if true get only DN's, if false get all the
     *      information.
     */
    boolean isDNsOnly() {
        return DNsOnly;
    }

    /**
     * adds the subOrg dn to Set subOrgDNs which holds all the subOrgnization
     * dn's.
     *
     * @param subOrgDN  the DN of a subOrganization
     */
    void addSubOrgDNs(String subOrgDN) {
        subOrgDNs.add(subOrgDN);
    }

    /**
     * gets the subOrgDNs set which contains all the subOrganization DN's whose
     * information should be retrieved.
     *
     * @return subOrgDNs which contains all the DN's of subOrganizations.
     */
    Set getSubOrgDNs() {
        return subOrgDNs;
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
        
        if (subOrgDNs.isEmpty()) {
            prnWriter.println("  DN set is empty");
        } else {
            prnUtl.printSet(subOrgDNs, 2);
        }
        
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * Prints all the subOrganization information based on the
     * values if the subOrgDNs set is empty than it prints all the
     * subOrganizations. If DNsOnly is true than it prints only the DNs of the
     * subOrganizations else it prints all the information of the all the
     * subOrganizations.
     *
     * @param dpConnection AMStoreConnection.
     * @exception AdminException
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);
        writer.println(bundle.getString("organization") + " " + targetDN +
            "\n" + bundle.getString("getsuborgs"));

        try {
            AMOrganization org = dpConnection.getOrganization(targetDN);
            writer.println(targetDN);
            boolean needContainerCheck = false;

            if (subOrgDNs.isEmpty()) {
                AMSearchControl searchCtrl = createSearchControl(
                    AMConstants.SCOPE_SUB);
                AMSearchResults searchResults= org.searchSubOrganizations(
                    filter, searchCtrl);
                subOrgDNs = searchResults.getSearchResults();
                errorCode = searchResults.getErrorCode();
            } else {
                needContainerCheck = true;
            }
            
             for (Iterator iter = subOrgDNs.iterator(); iter.hasNext(); ) {
                String dn = (String)iter.next();
                AMOrganization subOrg = org.getSubOrganization(dn);
                
                if (!needContainerCheck ||
                    (subOrg.isExists() &&
                        AdminUtils.isDescendantOf(subOrg, targetDN))
                ) {
                    AdminReq.writer.println("  " + dn);
                    
                    if (!DNsOnly) {
                        prnUtl.printAVPairs(subOrg.getAttributes(), 2);
                    }
                }
            }

            printSearchLimitError();
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
