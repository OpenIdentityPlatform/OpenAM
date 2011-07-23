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
 * $Id: RealmModifyServiceReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


class RealmModifyServiceReq extends AdminReq {
    private Map serviceAttrMap;
    private String serviceName = null;
    private String realmPath = null;


    /**
     * Constructs a new RealmModifyServiceReq.
     *
     * @param  targetDN the parent Realm DN. 
     */        
    RealmModifyServiceReq(String targetDN) {
        //
        //  a "slash" format path, rather than DN...
        //
        super(targetDN);
        realmPath = targetDN;
    }

    /**
     * sets the service Name for this request
     *
     * @param svcName the Name of the Service
     */
    void setServiceName(String svcName) {
        serviceName = svcName;
    }

    /**
     * sets the service attribute Map for this request
     *
     * @param svcAttrMap the Map of attribute value pairs
     */
    void setAttrMap(Map svcAttrMap) {
        serviceAttrMap = svcAttrMap;
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription110") +
            " " + targetDN);
//        if ((serviceAttrMap != null) && (!serviceAttrMap.isEmpty())) {
//            AdminUtils.printAttributeNameValuesMap(prnWriter, prnUtl,
//                serviceAttrMap);
//        }
        if ((serviceAttrMap != null) && (!serviceAttrMap.isEmpty())) {
            Set set = serviceAttrMap.keySet();
            for (Iterator it=set.iterator(); it.hasNext(); ) {
                String key = (String)it.next();
                prnWriter.println("  " + key + " =");
                Set valSet = (Set)serviceAttrMap.get(key);
                for (Iterator it2=valSet.iterator(); it2.hasNext(); ) {
                    String val = (String)it2.next();
                    prnWriter.println("    " + val);
                }
            }
        }
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("realm") + "\n" +
            bundle.getString("realmModifySvc") + " " + serviceName +
            bundle.getString("inrealm") + " " + realmPath);
        
        String[] args = {serviceName, realmPath};
        try {
            OrganizationConfigManager ocm =
                new OrganizationConfigManager(ssoToken, realmPath);
        
            AMIdentityRepository amir =
                new AMIdentityRepository (ssoToken, realmPath);
            AMIdentity ai = amir.getRealmIdentity();

            //
            //  get the OrgConfigMgr's and AMIdentity's list of
            //  assigned services
            //

            Set ocmSvcSet = ocm.getAssignedServices(true);
            Set aiSvcSet = ai.getAssignedServices();

            boolean did_not_do_modify = true;
            if (ocmSvcSet.contains(serviceName)) {
                doLog(args, AdminUtils.MODIFY_SERVICE_ORGCONFIG_ATTEMPT);
                ocm.modifyService(serviceName, serviceAttrMap);
                doLog(args, AdminUtils.MODIFY_SERVICE_ORGCONFIG);
                did_not_do_modify = false;
            }

            if (aiSvcSet.contains(serviceName)) {
                doLog(args, AdminUtils.MODIFY_SERVICE_REALM_ATTEMPT);
                ai.modifyService(serviceName, serviceAttrMap);
//                doLog(args, "modified-service-realm");
                doLog(args, AdminUtils.MODIFY_SERVICE_REALM);
                did_not_do_modify = false;
            }

            //
            //  if not in either the org config's or realm's list of
            //  assigned services, then log it.
            //
            if (did_not_do_modify) {
                doLog(args,
                    AdminUtils.MODIFY_SERVICE_NOTIN_ORGCONFIG_OR_REALM);
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
