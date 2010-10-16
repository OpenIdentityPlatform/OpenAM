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
 * $Id: OrgGetNumOfServicesReq.java,v 1.2 2008/06/25 05:52:30 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;

class OrgGetNumOfServicesReq extends AdminReq {
    /**
     * Constructs a new OrgGetNumOfServicesReq.
     */        
    OrgGetNumOfServicesReq() {
        super();
    }

    /**
     * Constructs a new OrgGetNumOfServicesReq.
     *
     * @param  targetDN the Organization DN. 
     */        
    OrgGetNumOfServicesReq(String targetDN) {
        super(targetDN);
    }
    
    /**
     * converts this object into a string.
     *
     * @return String. the total number of services in print format.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        prnWriter.println(
            "Request Description: Get Number of services from Organization " +
            targetDN);
        prnWriter.flush();
        return stringWriter.toString();    
    }   

    /**
     * This method prints the Number of Services for an Organization. 
     *
     * @param dpConnection the AMStoreConnection.
     * throws AdminException if the syntax of the DN is not correct.
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        try {
            AMOrganization org = dpConnection.getOrganization(targetDN);
            long numOfServices = org.getNumberOfServices();

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("organization") + " " +
                    targetDN + "\n" +
                    bundle.getString("numberofservices") + numOfServices);
            }

            AdminReq.writer.println(bundle.getString("organization") + " " +
                targetDN + "\n" + bundle.getString("numberofservices") +
                numOfServices);
            AdminReq.writer.flush();
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
