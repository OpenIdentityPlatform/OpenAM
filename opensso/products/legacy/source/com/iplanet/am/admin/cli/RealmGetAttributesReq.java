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
 * $Id: RealmGetAttributesReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.OrganizationConfigManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;


class RealmGetAttributesReq extends AdminReq {
    private String serviceName = null;
    private String realmPath = null;
  

    /**
     * Constructs a new RealmGetAttributesReq.
     *
     * @param  targetDN the Realm to delete. 
     */        
    RealmGetAttributesReq(String targetDN) {
        //
        //  a "slash" format path, rather than DN...
        //
        super(targetDN);
        realmPath = targetDN;
    }

    /**
     * Sets the service name to get the attributes for
     *
     * @param svcName
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription107") +
            " " + serviceName + " " +
            AdminReq.bundle.getString("inrealm") + " " + targetDN);
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("realm") + "\n" +
            bundle.getString("realmAttributes") + " " + serviceName + " " +
            bundle.getString("inrealm") + " " + targetDN);

        PrintUtils prnUtl = new PrintUtils(AdminReq.writer);

        try {
            OrganizationConfigManager ocm =
                new OrganizationConfigManager(ssoToken, realmPath);

            Map attrMap = ocm.getAttributes(serviceName);

            if (attrMap != null) {
                prnUtl.printAVPairs(attrMap, 1);
            }

        } catch (SMSException smse) {
            throw new AdminException(smse);
        }
    }
}
