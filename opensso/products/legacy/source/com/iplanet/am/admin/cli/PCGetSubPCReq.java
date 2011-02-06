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
 * $Id: PCGetSubPCReq.java,v 1.2 2008/06/25 05:52:31 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;
 
import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMPeopleContainer;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.io.StringWriter;
import java.io.PrintWriter;

class PCGetSubPCReq extends SearchReq {
     
    private Set subPCDNs = new HashSet();
    private String level = "SCOPE_SUB";
    private boolean DNsOnly = true;
 
    /**
     * Constructs a new PCGetSubPCReq.
     *
     * @param targetDN the PeopleContainer DN.
     */
    PCGetSubPCReq(String targetDN) {
        super(targetDN);
    }
 
    /**
     * sets the value for level  which tells the process() method to get
     * groups under one level or the entire sub tree.
     *
     * @param level if SCOPE_ONE get Sub PeopleContainers up to one level , if
     *        SCOPE_SUB get all the Sub PeopleContainers.
     */
    void setLevel(String level) {
        this.level = level;
    }
     
    /**
     * gets the value for level  which tells the process() method to get
     * groups under one level or the entire sub tree.
     *
     * @return level if SCOPE_ONE get Sub PeopleContainers up to one level , if
     *         SCOPE_SUB get all the Sub PeopleContainers.
     */
    String getLevel() {
        return level;
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
     * gets the value of DNsOnly which tells the process() method to get
     * only the DNs or all the information.
     *
     * @return DNsOnly if true get only DN's, if false get all the information.
     */
    boolean isDNsOnly() {
        return DNsOnly;
    }

    /**
     * adds the sub PeopleContainer dn to Set subPCDNs which holds all the 
     * PeopleContainer dn's.
     * @param  subPCDN the DN of a subOrganization
     * @return none.
     */
    void addSubPCDNs(String subPCDN) {
        subPCDNs.add(subPCDN);
    }
 
    /**
     * gets the subPCDNs set which contains all the sub PeopleContainer DN's 
     * whose information should be retrieved.
     *
     * @return subPCDNs which contains all the DN's of PeopleContainer.
     */
    Set getSubPCDNs() {
        return subPCDNs;
    }
     
    /**
     * converts this object into a string.
     * @param none.
     * @return String. the values of the dnset in print format.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter);
        prnWriter.println(AdminReq.bundle.getString("requestdescription51") +
            " " + targetDN);
        prnWriter.println("  level = " + level);
        prnWriter.println("  DNsOnly = " + DNsOnly);
        prnWriter.println("  filter = " + filter);

        if(subPCDNs.isEmpty()) {
            prnWriter.println("  DN set is empty");
        } else {
            prnUtl.printSet(subPCDNs,2);
        }
        
        prnWriter.flush();
        return stringWriter.toString();
    }
     
    /**
     * This code takes each dn from the subPCDNs set and gets
     * the PeopleContainer object and gets its SubPeopleContainers
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
        writer.println(bundle.getString("peoplecontainer") + " " + targetDN +
            "\n" + bundle.getString("getpeoplecontainers"));

        try {
            writer.println(targetDN);
            AMPeopleContainer pcr = dpConnection.getPeopleContainer(targetDN);
            boolean needContainerCheck = false;
            int scope = level.equals("SCOPE_SUB") ?
                AMConstants.SCOPE_SUB : AMConstants.SCOPE_ONE;
 
            if (subPCDNs.isEmpty()) {
                subPCDNs = pcr.searchSubPeopleContainers(filter, scope);
            } else {
                needContainerCheck = true;
            }
            
            for (Iterator iter = subPCDNs.iterator(); iter.hasNext(); ) {
                String dn = (String)iter.next();
                AMPeopleContainer pc = dpConnection.getPeopleContainer(dn);
                
                if (!needContainerCheck ||
                    (pc.isExists() &&
                        AdminUtils.isDescendantOf(pc, targetDN, scope))
                ) {
                    AdminReq.writer.println("  " + dn);

                    if (!DNsOnly) {
                        prnUtl.printAVPairs(pcr.getAttributes(), 2);
                    }
                }
            }
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}

