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
 * $Id: RealmCreateRealmReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.OrganizationConfigManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class RealmCreateRealmReq extends AdminReq {
    private Map realmSvcMap = new HashMap();
    private String parentRealm = null;
    private String childRealm = null;
    
    /**
     * Constructs a new RealmCreateRealmReq.
     *
     * @param  targetDN the parent Realm DN. 
     */        
    RealmCreateRealmReq(String targetDN) {
        //
        //  a "slash" format path, rather than DN...
        //
        super(targetDN);
    }

    /**
     * Set the parent realm to which to create the realm
     *
     * @param parRealm the name of the parent Realm
     */

    void setParentRealm (String parRealm) {
        parentRealm = parRealm;
    }


    /**
     * Set the subrealm to create under the parent realm
     *
     * @param sbRealm the name of the subRealm
     */

    void setSubRealm (String subRealm) {
        childRealm = subRealm;
    }

    /**
     * adds the service's name and its avPair Map to the realmSvcMap.
     *
     * @param svcName the Name of the Service
     * @param svcAttrMap the service's AttributeValuePair map
     */
    void createRealmReq(String svcName, Map svcAttrMap) {
        realmSvcMap.put(svcName, svcAttrMap);
    }

    /**
     * converts this object into a string.
     *
     * @return String. 
     */
    public String toString() {
        //
        //  the realmSvcMap has (String)serviceName::->AVPair map
        //  entries
        //

        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(AdminReq.bundle.getString("requestdescription101") +
            " " + targetDN);
        if (!realmSvcMap.isEmpty()) {
            AdminUtils.printAttributeNameValuesMap(prnWriter, prnUtl,
                realmSvcMap);
        }
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("realm") + "\n" +
            bundle.getString("createRealm") + " " + childRealm + " " +
            bundle.getString("inrealm") + " " + parentRealm);

        String[] args = {childRealm, parentRealm};
        try {
            doLog(args, AdminUtils.CREATE_REALM_ATTEMPT);
            OrganizationConfigManager ocm =
                new OrganizationConfigManager(ssoToken, parentRealm);

            Set subRealms = ocm.getSubOrganizationNames();
            for (Iterator iter = subRealms.iterator(); iter.hasNext(); ) {
                String tmpS = (String)iter.next();
                if (childRealm.equals(tmpS)) {
                    throw new AdminException(
                        bundle.getString("realmExists") + " " + targetDN);
                }
            }

            ocm.createSubOrganization (childRealm, realmSvcMap);
            doLog(args, AdminUtils.CREATE_REALM);
        } catch (SMSException smse) {
            throw new AdminException(smse);
        }
    }
}
