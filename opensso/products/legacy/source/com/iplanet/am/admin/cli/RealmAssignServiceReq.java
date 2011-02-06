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
 * $Id: RealmAssignServiceReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


class RealmAssignServiceReq extends AdminReq {
    private Map assignSvcMap = new HashMap();
    private String realmPath = null;


    /**
     * Constructs a new RealmAssignServiceReq.
     *
     * @param  targetDN the parent Realm DN. 
     */        
    RealmAssignServiceReq(String targetDN) {
        //
        //  a "slash" format path, rather than DN...
        //
        super(targetDN);
        realmPath = targetDN;
    }

    /**
     * adds the service's name and its avPair Map to the realmSvcMap.
     *
     * @param svcName the Name of the Service
     * @param svcAttrMap the service's AttributeValuePair map
     */
    void createAssignSvcReq(String svcName, Map svcAttrMap) {
        assignSvcMap.put(svcName, svcAttrMap);
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription105") +
            " " + targetDN);
        AdminUtils.printAttributeNameValuesMap(prnWriter, prnUtl, assignSvcMap);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("realm") + "\n" +
            bundle.getString("realmAssignSvc") + " " + realmPath);

        try {
            OrganizationConfigManager ocm =
                new OrganizationConfigManager(ssoToken, realmPath);
        
            AMIdentityRepository amir =
                new AMIdentityRepository (ssoToken, realmPath);
            AMIdentity ai = amir.getRealmIdentity();

            PrintUtils prnUtl = new PrintUtils(AdminReq.writer);

            //
            //  get the OrgConfigMgr's and AMIdentity's
            //  lists of assignable services
            //

            Set ocmSvcSet = ocm.getAssignableServices();
            Set aiSvcSet = ai.getAssignableServices();

            //
            //  see how many services are in the map
            //
            //  if in corresponding set of assignable services,
            //  then it can be assigned.
            //
            int svcCnt = assignSvcMap.size();
            Set svcNames = assignSvcMap.keySet();
            for (Iterator iter = svcNames.iterator(); iter.hasNext(); ) {
                String svcName = (String)iter.next();
                String[] args = {svcName, realmPath};
                Map avs = (Map)assignSvcMap.get(svcName);
                if (ocmSvcSet.contains(svcName)) {
                    doLog(args, AdminUtils.ASSIGN_SERVICE_TO_REALM_ATTEMPT);
                    ocm.assignService(svcName, avs);
//                    doLog(args, "assigned-service-to-realm");
                    doLog(args, AdminUtils.ASSIGN_SERVICE_TO_REALM);
                } else {
                    doLog(args, AdminUtils.ASSIGN_SERVICE_TO_REALM_NOTINLIST);
                }

                if (aiSvcSet.contains(svcName)) {
                    doLog(args,
                        AdminUtils.ASSIGN_SERVICE_TO_ORGCONFIG_ATTEMPT);
                    ai.assignService(svcName, avs);
                    doLog(args, AdminUtils.ASSIGN_SERVICE_TO_ORGCONFIG);
                } else {
                    doLog(args,
                        AdminUtils.ASSIGN_SERVICE_TO_ORGCONFIG_NOTINLIST);
                }
            }
            prnUtl.printSet(svcNames, 1);

        } catch (SMSException smse) {
            throw new AdminException(smse);
        } catch (IdRepoException idre) {
            throw new AdminException(idre);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
