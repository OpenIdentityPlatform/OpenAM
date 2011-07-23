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
 * $Id: RealmUnassignServiceReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.AMIdentity;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;


class RealmUnassignServiceReq extends AdminReq {
    private String serviceName = null;
    private String realmPath = null;
  

    /**
     * Constructs a new RealmUnassignServiceReq.
     *
     * @param  targetDN the Realm to delete. 
     */        
    RealmUnassignServiceReq(String targetDN) {
        //
        //  a "slash" format path, rather than DN...
        //
        super(targetDN);
        realmPath = targetDN;
    }

    /**
     * Sets the service name to unassign
     *
     * @param recDelete
     */
    void setServiceName(String svcName) {
        serviceName = svcName;
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription106") +
            " " + serviceName + " " +
            AdminReq.bundle.getString("fromrealm") + " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("realm") + "\n" +
            bundle.getString("realmUnassignSvc") + " " + serviceName +
            " " + bundle.getString("fromrealm") + " " + targetDN);

        String[] args = {serviceName, realmPath};
        try {
            OrganizationConfigManager ocm =
                new OrganizationConfigManager(ssoToken, realmPath);

            AMIdentityRepository amir =
                new AMIdentityRepository (ssoToken, realmPath);
            AMIdentity ai = amir.getRealmIdentity();

            //
            //  get the OrgConfigMgr's and AMIdentity's lists
            //  of assigned services
            //

            Set ocmSvcSet = ocm.getAssignedServices(false);
            Set aiSvcSet = ai.getAssignedServices();

            boolean did_not_do_unassign = true;
            if (ocmSvcSet.contains(serviceName)) {
                doLog(args, AdminUtils.UNASSIGN_SERVICE_FROM_ORGCONFIG_ATTEMPT);
                ocm.unassignService(serviceName);
//                doLog(args, "unassigned-service-from-realm");
                doLog(args, AdminUtils.UNASSIGN_SERVICE_FROM_ORGCONFIG);
                did_not_do_unassign = false;
            }

            if (aiSvcSet.contains(serviceName)) {
                doLog(args, AdminUtils.UNASSIGN_SERVICE_FROM_REALM_ATTEMPT);
                ai.unassignService(serviceName);
                doLog(args, AdminUtils.UNASSIGN_SERVICE_FROM_REALM);
                did_not_do_unassign = false;
            }

            //
            //  if not in either the org config's or realm's list of
            //  assigned services, then log it.
            //
            if (did_not_do_unassign) {
                doLog(args,
                    AdminUtils.UNASSIGN_SERVICE_NOTIN_ORGCONFIG_OR_REALM);
            }


        } catch (SMSException smse) {
            throw new AdminException(smse);
        } catch (IdRepoException idre) {
            throw new AdminException(idre);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
