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
 * $Id: UserRegisterServicesReq.java,v 1.2 2008/06/25 05:52:34 qcheng Exp $
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

class UserRegisterServicesReq extends AdminReq {

    private Set serviceSet = new HashSet();
    
    /**
     * Constructs a new UserRegisterServicesReq.
     *
     * @param  targetDN the User DN. 
     */        
    UserRegisterServicesReq(String targetDN) {
        super(targetDN);
    }

    void registerServicesReq(String serviceName) {
        serviceSet.add(serviceName);
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription59") +
            " " + targetDN);
        prnUtl.printSet(serviceSet, 1);
        prnWriter.flush();
        return stringWriter.toString();
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("user") + " " + targetDN + "\n" +
                bundle.getString("registeredservices"));
        }

        writer.println(bundle.getString("user") + " " + targetDN +
            "\n" + bundle.getString("registeredservices"));

        try {
            AMUser user = dpConnection.getUser(targetDN);
            doLogStringSet(serviceSet, user,
                AdminUtils.REGISTER_SERVICE_ATTEMPT);
            user.assignServices(serviceSet);

            PrintUtils prnUtl = new PrintUtils(AdminReq.writer);
            prnUtl.printSet(serviceSet, 1);

//            doLogStringSet(serviceSet, user, "register-service");
            doLogStringSet(serviceSet, user, AdminUtils.REGISTER_SERVICE);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
