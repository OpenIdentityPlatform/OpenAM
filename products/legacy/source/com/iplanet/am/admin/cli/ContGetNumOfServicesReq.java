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
 * $Id: ContGetNumOfServicesReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;

class ContGetNumOfServicesReq extends AdminReq {
    /**
     * Constructs a new ContGetNumOfServicesReq.
     */
    ContGetNumOfServicesReq() {
        super();
    }

    /**
     * Constructs a new ContGetNumOfServicesReq.
     *
     * @param  targetDN the Container DN. 
     */        
    ContGetNumOfServicesReq(String targetDN) {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription13") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * This method prints the Number of Services for a Container.
     *
     * @param dpConnection AMStoreConnection.
     * @exception AdminException 
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        try {
            AMOrganizationalUnit orgUnit = dpConnection.getOrganizationalUnit(
                targetDN);
            long numOfServices = orgUnit.getNumberOfServices();

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(AdminReq.bundle.getString("container") + " " + 
                    targetDN + "\n" + 
                    AdminReq.bundle.getString("numberofservices") + 
                    numOfServices);
            }

            AdminReq.writer.println(AdminReq.bundle.getString("container") + 
                " " + targetDN + "\n" + 
                AdminReq.bundle.getString("numberofservices") + numOfServices);
            AdminReq.writer.flush();
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
