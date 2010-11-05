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
 * $Id: UserUnregisterServicesReq.java,v 1.2 2008/06/25 05:52:34 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMUser;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

class UserUnregisterServicesReq extends AdminReq {

    private Set serviceSet = new HashSet();

    /**
     * Constructs a new UserUnregisterServicesReq.
     * @param  targetDN the User DN. 
     */        
    UserUnregisterServicesReq(String targetDN) {
        super(targetDN);
    }

    /**
     * Adds the org service's to a Set to unregister services.
     * @param  orgService the Service of the org
     */
    void unregisterServicesReq(String orgService) {
        serviceSet.add(orgService);
    }

    /**
     * Converts this object into a string.
     * @return String. the values of the dnset in print format.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(bundle.getString("requestdescription60") +
            " " + targetDN);
        prnUtl.printSet(serviceSet,1);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("organization") + " " + targetDN +
                "\n" + bundle.getString("unregisteredservices"));
        }

        writer.println(bundle.getString("user") + " " + targetDN +
            "\n" + bundle.getString("unregisteredservices"));

        try {
            AMUser user = dpConnection.getUser(targetDN);
            doLogStringSet(serviceSet, user,
                AdminUtils.UNREGISTER_SERVICE_ATTEMPT);
            user.unassignServices(serviceSet);

            PrintUtils prnUtl = new PrintUtils(writer);
            prnUtl.printSet(serviceSet, 1);

//            doLogStringSet(serviceSet, user, "unregister-service");
            doLogStringSet(serviceSet, user, AdminUtils.UNREGISTER_SERVICE);
        } catch (AMException dpe) {
            throw new AdminException(dpe);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
