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
 * $Id: ContGetRegisteredServicesReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

class ContGetRegisteredServicesReq extends AdminReq {
    /**
     * Constructs a new empty ContGetRegisteredServicesReq.
     */
    ContGetRegisteredServicesReq() {
        super();
    }

    /**
     * Constructs a new ContGetRegisteredServicesReq.
     *
     * @param targetDN the Container DN.
     */
    ContGetRegisteredServicesReq(String targetDN) {
        super(targetDN);
    }

    /**
     * converts this object into a string.
     *
     * @return String. the values of the Registered Service names in print
     *       format.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        prnWriter.println(bundle.getString("requestdescription15") +
            " "  + targetDN);
        prnWriter.println("   Registered Service Names\n ");
        prnWriter.flush();
        return stringWriter.toString();
    }

    /**
     * This method prints all the RegisteredServices information for an
     * Container.
     *
     * @param dpConnection the AMStoreConnection.
     * throws AdminException if the syntax of the DN is not correct.
     */
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);  

        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("container") + " " + targetDN +
                "\n" + bundle.getString("getregisteredservices"));
        }

        AdminReq.writer.println(bundle.getString("container") + " " +
            targetDN + "\n" + bundle.getString("getregisteredservices"));

        try {
            AMOrganizationalUnit orgUnit =
                dpConnection.getOrganizationalUnit(targetDN);
            Set serviceNames = orgUnit.getRegisteredServiceNames();
            prnUtl.printSet(serviceNames, 1);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}

