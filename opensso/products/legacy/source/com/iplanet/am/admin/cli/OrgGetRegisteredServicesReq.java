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
 * $Id: OrgGetRegisteredServicesReq.java,v 1.2 2008/06/25 05:52:30 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

class OrgGetRegisteredServicesReq extends AdminReq {

    /**
     * Constructs a new empty OrgGetRegisteredServicesReq.
     */
    OrgGetRegisteredServicesReq() {
        super();
    }

    /**
     * Constructs a new OrgGetRegisteredServicesReq.
     *
     * @param  targetDN the Organization DN. 
     */        
    OrgGetRegisteredServicesReq(String targetDN) {
        super(targetDN);
    }

    /**
     * converts this object into a string.
     *
     * @return String. the values of the Registered Service names in print 
     * format.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        prnWriter.println(AdminReq.bundle.getString("requestdescription39") +
            " " + targetDN);
        prnWriter.println("   Registered Service Names\n ");
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    /**
     * This method prints all the RegisteredServices information for an 
     * Organization.
     *
     * @param dpConnection the AMStoreConnection.
     * throws AdminException if the syntax of the DN is not correct.
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);  

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(AdminReq.bundle.getString("organization") + " " +
                targetDN + "\n" + bundle.getString("getregisteredservices"));
        }

        try {
            AMOrganization org = dpConnection.getOrganization(targetDN);
            Set serviceNames = org.getRegisteredServiceNames();

            AdminReq.writer.println(bundle.getString("organization") + " " +
                targetDN + "\n" +
                bundle.getString("getregisteredservices"));

            prnUtl.printSet(serviceNames, 1);
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
