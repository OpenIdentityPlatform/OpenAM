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
 * $Id: ContRegisterServicesReq.java,v 1.2 2008/06/25 05:52:26 qcheng Exp $
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class ContRegisterServicesReq extends AdminReq {

    private Set serviceSet = new HashSet();
    
    /**
     * Constructs a new empty ContRegisterServicesReq.
     */
    ContRegisterServicesReq() {
        super();    
    }
        
    /**
     * Constructs a new ContRegisterServicesReq.
     *
     * @param  targetDN the Container DN. 
     */        
    ContRegisterServicesReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the container service's to a Set to register services.
     *
     * @param  contService the Service of the container
     */
    void registerServicesReq(String contService) {
        serviceSet.add(contService);
    }

    /**
     * gets the serviceSet set which contains all the services to register.
     *
     * @return serviceSet which contains all the DN's of the group
     */
    Set getRegisterContReq() {
        return serviceSet;
    }
    
    /**
     * converts this object into a string.
     *
     * @return String. 
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(AdminReq.bundle.getString("requestdescription57") +
            " " + targetDN);
        prnUtl.printSet(serviceSet, 1);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("container") + " " +
            targetDN + "\n" + bundle.getString("registeredservices"));

        try {
            AMOrganizationalUnit orgUnit =
                dpConnection.getOrganizationalUnit(targetDN);

            for (Iterator iter = serviceSet.iterator(); iter.hasNext(); ) {
                String serviceName = (String)iter.next();
                doLog(serviceName, orgUnit,
                    AdminUtils.REGISTER_SERVICE_ATTEMPT);
                orgUnit.registerService(serviceName, false, false);
                AdminReq.writer.println(serviceName);
//                doLog(serviceName, orgUnit, "register-service");
                doLog(serviceName, orgUnit, AdminUtils.REGISTER_SERVICE);
            }
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
