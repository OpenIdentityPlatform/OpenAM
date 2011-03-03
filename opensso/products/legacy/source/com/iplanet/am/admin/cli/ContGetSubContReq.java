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
 * $Id: ContGetSubContReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.PrintUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class ContGetSubContReq extends SearchReq {
    private Set subContDNs = new HashSet();
    private boolean DNsOnly = true;

    /**
     * Constructs a new ContGetSubContReq.
     *
     * @param targetDN  the Container DN.
     */
    ContGetSubContReq(String targetDN) {
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
     * @return   DNsOnly if true get only DN's, if false get all the
     *      information.
     */
    boolean isDNsOnly() {
        return DNsOnly;
    }

    /**
     * adds the subCont dn to Set subContDNs which holds all the subContainer
     * dn's.
     *
     * @param subContDN  the DN of a subContainer
     */
    void addSubContDNs(String subContDN) {
        subContDNs.add(subContDN);
    }

    /**
     * gets the subContDNs set which contains all the subContainer DN's whose
     * information should be retrieved.
     *
     * @return   subContDNs which contains all the DN's of subContainers.
     */
    Set getSubContDNs() {
        return subContDNs;
    }

    /**
     * converts this object into a string.
     *
     * @return   String. the values of the dnset in print format.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter);
        prnWriter.println(AdminReq.bundle.getString("requestdescription17") +
            " " + targetDN);
        prnWriter.println("   DNsOnly = " + DNsOnly);
        prnWriter.println("   filter = " + filter);
        prnWriter.println("   sizeLimit = " + sizeLimit);
        prnWriter.println("   timeLimit = " + timeLimit);
        
        if (subContDNs.isEmpty()) {
            prnWriter.println("  DN set is empty");
        } else {
            prnUtl.printSet(subContDNs, 2);
        }
        
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * Prints all the subContainer information based on the values
     * if the subContDNs set is empty than it prints all the subContainers. If
     * DNsOnly is true than it prints only the DNs of the subContainers else it
     * prints all the information of the all the subContainers.
     *
     * @param dpConnection AMStoreConnection.
     * @throws AdminException if the syntax of the DN is not correct.
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);
        writer.println(bundle.getString("container") + " " + targetDN + "\n" +
            bundle.getString("getsubcontainers"));

        try {
            boolean needValidation = false;
            AMOrganizationalUnit orgUnit =
                dpConnection.getOrganizationalUnit(targetDN);

            if (subContDNs.isEmpty()) {
                AMSearchControl searchCtrl = createSearchControl(
                    AMConstants.SCOPE_ONE);
                AMSearchResults searchResults =
                    orgUnit.searchSubOrganizationalUnits(filter, searchCtrl);
                subContDNs = searchResults.getSearchResults();
                errorCode = searchResults.getErrorCode();
            } else {
                needValidation = true;
            }
            
            for (Iterator iter = subContDNs.iterator(); iter.hasNext(); ) {
                String dn = (String)iter.next();
                AMOrganizationalUnit ou = orgUnit.getSubOrganizationalUnit(dn);
                
                if (!needValidation ||
                    (ou.isExists() && AdminUtils.isChildOf(ou, targetDN))
                ) {
                    AdminReq.writer.println("  " + dn);
                    
                    if (!DNsOnly) {
                        prnUtl.printAVPairs(ou.getAttributes(), 2);
                    }
                }
            }

            printSearchLimitError();
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
