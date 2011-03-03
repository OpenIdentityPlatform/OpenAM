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
 * $Id: RealmGetSvcAttributesReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.AMIdentity;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Map;


class RealmGetSvcAttributesReq extends AdminReq {
    private String serviceName = null;
    private String realmPath = null;


    /**
     * Constructs a new RealmGetSvcAttributesReq.
     *
     * @param targetDN Realm name where service resides. 
     */        
    RealmGetSvcAttributesReq(String targetDN) {
        super(targetDN);
        realmPath = targetDN;
    }

    /**
     * Sets the service name to get the attributes for
     *
     * @param svcName Service Name.
     */
    void setServiceName(String svcName) {
        serviceName = svcName;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(AdminReq.bundle.getString("requestdescription108") +
            " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        String[] args = {serviceName, targetDN};
        AdminReq.writer.println(bundle.getString("realm"));
        AdminReq.writer.println(MessageFormat.format(
            bundle.getString("get-realm-service-attribute-info"), args));
        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);

        try {
            OrganizationConfigManager ocm =
                new OrganizationConfigManager(ssoToken, realmPath);

            CaseInsensitiveHashSet svcSet = new CaseInsensitiveHashSet();
            svcSet.addAll(ocm.getAssignedServices(true));
            boolean hasResults = false;
            boolean isAssigned = false;

            if (svcSet.contains(serviceName)) {
                isAssigned = true;
                Map attrMap = ocm.getServiceAttributes(serviceName);

                if ((attrMap != null) && !attrMap.isEmpty()) {
                    hasResults = true;
                    AdminReq.writer.println(bundle.getString("orgAttributes"));
                    prnUtl.printAVPairs(attrMap, 1);
                } else {
                    AdminReq.writer.println(MessageFormat.format(
                        bundle.getString(
                            "get-realm-service-attribute-no-results"), args));
                }
            }

            AMIdentityRepository amir =
                new AMIdentityRepository (ssoToken, realmPath);
            AMIdentity ai = amir.getRealmIdentity();
            svcSet.clear();
            svcSet.addAll(ai.getAssignedServices());

            if (svcSet.contains(serviceName)) {
                isAssigned = true;
                Map attrMap = ai.getServiceAttributes(serviceName);
                if ((attrMap != null) && !attrMap.isEmpty()) {
                    hasResults = true;
                    AdminReq.writer.println(bundle.getString(
                        "dynamicAttributes"));
                    prnUtl.printAVPairs(attrMap, 1);
                }
            }

            if (!isAssigned) {
                AdminReq.writer.println(MessageFormat.format(
                    bundle.getString("service-no-assigned-to-realm"), args));
            } else if (!hasResults) {
                AdminReq.writer.println(MessageFormat.format(
                    bundle.getString("get-realm-service-attribute-no-results"),
                        args));
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
