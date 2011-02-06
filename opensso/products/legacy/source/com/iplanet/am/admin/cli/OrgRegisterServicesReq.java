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
 * $Id: OrgRegisterServicesReq.java,v 1.3 2009/01/28 05:35:11 ww203982 Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.sun.identity.shared.ldap.util.DN;

class OrgRegisterServicesReq extends AdminReq {

    private Set serviceSet = new HashSet();
    
    /**
     * Constructs a new OrgRegisterServicesReq.
     *
     * @param  targetDN the Organization DN. 
     */        
    OrgRegisterServicesReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the org service's to a Set to register services.
     *
     * @param orgService the Service of the org
     */
    void registerServicesReq(String orgService) {
        serviceSet.add(orgService);
    }

    /**
     * gets the serviceSet set which contains all the services to register.
     *
     * @return serviceSet which contains all the DN's of the group
     */
    Set getRegisterOrgReq() {
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription44") +
            " " + targetDN);
        prnUtl.printSet(serviceSet, 1);
        prnWriter.flush();
        return stringWriter.toString();    
    }

    private Set getRegisterableServices(AMStoreConnection dpConnection,
        AMOrganization org)
        throws AdminException
    {
        Set serviceNames = null;
        String defaultOrg = SystemProperties.get("com.iplanet.am.defaultOrg");
        boolean isTopLevelOrg = (new DN(defaultOrg)).equals(new DN(targetDN));

        if (!isTopLevelOrg) {
            try {
                AMOrganization parentOrg = dpConnection.getOrganization(
                    org.getParentDN());
                serviceNames = parentOrg.getRegisteredServiceNames();
            } catch (AMException ame) {
                throw new AdminException(ame.toString());
            } catch (SSOException ssoe) {
                throw new AdminException(ssoe.toString());
            }
        }

        return serviceNames;
    }
    
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("organization") + " " +
            targetDN + "\n" + bundle.getString("registeredservices"));

        try {
            AMOrganization org = dpConnection.getOrganization(targetDN);
            Set registerableServices = getRegisterableServices(
                dpConnection, org);

            // services to be registered for this org
            for (Iterator iter = serviceSet.iterator(); iter.hasNext(); ) {
                String serviceName = (String)iter.next();

                if ((registerableServices == null) ||
                    registerableServices.contains(serviceName)
                ) {
                    doLog(serviceName, org,
                        AdminUtils.REGISTER_SERVICE_ATTEMPT);
                    org.registerService(serviceName, false, false);
                    AdminReq.writer.println(serviceName);
//                    doLog(serviceName, org, "register-service");
                    doLog(serviceName, org, AdminUtils.REGISTER_SERVICE);
                } else {
                    throw new AdminException(serviceName + " " +
                        AdminReq.bundle.getString("unableToRegisterService"));
                }
            }
        } catch (AMException dpe) {
            throw new AdminException(dpe.toString());
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe.toString());
        }
    }
}
