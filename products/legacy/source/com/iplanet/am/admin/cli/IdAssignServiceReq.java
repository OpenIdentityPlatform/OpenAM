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
 * $Id: IdAssignServiceReq.java,v 1.2 2008/06/25 05:52:28 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


class IdAssignServiceReq extends AdminReq {
    private Map serviceAttrMap = null;
    private String serviceName = null;
    private String realmPath = null;
    private String idName = null;
    private IdType idType;

    /**
     * Constructs a new IdAssignServiceReq.
     *
     * @param  targetDN the parent Realm DN. 
     */        
    IdAssignServiceReq(String targetDN) {
        super(targetDN);
        realmPath = targetDN;
    }


    /**
     * Sets the Identity Name for this request.
     *
     * @param identName the Name of the Identity.
     */
    void setIdName(String identName) {
        idName = identName;
    }


    /**
     * Sets the Identity Type for this request.
     *
     * @param identType the Type of the Identity.
     */
    void setIdType(IdType identType) {
        idType = identType;
    }

    /**
     * Sets the service Name for this request.
     *
     * @param svcName the Name of the Service.
     */
    void setServiceName(String svcName) {
        serviceName = svcName;
    }


    /**
     * Sets the service attribute Map for this request.
     *
     * @param svcAttrMap the Map of attribute value pairs.
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription130") +
            " " + targetDN);
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
        AdminReq.writer.println(bundle.getString("identity") + "\n" +
            bundle.getString("addServiceId") + " " +
            serviceName + " " +
            bundle.getString("to") + " " +
            idName + " " +
            bundle.getString("of") + " " +
            idType.toString() + " " +
            bundle.getString("inrealm") + " " + realmPath);

        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);
        prnUtl.printAVPairs(serviceAttrMap, 1);

        String[] args = {serviceName, idType.toString(), idName, realmPath};

        try {
            AMIdentityRepository amir =
                new AMIdentityRepository (ssoToken, realmPath);

            //  see if services are supported for the given IdType
            Set set = amir.getAllowedIdOperations(idType);
            if (!set.contains(IdOperation.SERVICE)) {
                throw new AdminException(idType.toString() + " " +
                    bundle.getString("doesNotSupportServices"));
            }

            AMIdentity ai2use = new AMIdentity(ssoToken,
                idName, idType, realmPath, null);

            doLog(args, AdminUtils.ASSIGN_SERVICE_IDENTITY_ATTEMPT);
            ai2use.assignService(serviceName, serviceAttrMap);
            doLog(args, AdminUtils.ASSIGN_SERVICE_IDENTITY);
        } catch (IdRepoException ire) {
            throw new AdminException(ire);
        } catch (SSOException ssoex) {
            throw new AdminException(ssoex);
        }
    }
}
