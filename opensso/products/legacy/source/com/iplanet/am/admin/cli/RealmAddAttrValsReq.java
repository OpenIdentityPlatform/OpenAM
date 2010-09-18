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
 * $Id: RealmAddAttrValsReq.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
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


class RealmAddAttrValsReq extends AdminReq {
    private Set valueSet = null;
    private String realmPath = null;
    private String serviceName = null;
    private String attributeName = null;
    private Map avMap = new HashMap();


    /**
     * Constructs a new RealmAddAttrValsReq.
     *
     * @param  targetDN the parent Realm DN. 
     */        
    RealmAddAttrValsReq(String targetDN) {
        //
        //  a "slash" format path, rather than DN...
        //
        super(targetDN);
        realmPath = targetDN;
    }


    /**
     * sets the Service Name for this request
     *
     * @param svcName the name of the Service
     */
    void setServiceName(String svcName) {
        serviceName = svcName;
    }


    /**
     * sets the Attibute Name for this request
     *
     * @param attrName the name of the Attribute
     */
    void setAttrName(String attrName) {
        attributeName = attrName;
    }

    /**
     * sets the Value Set
     *
     * @param valSet the Set of Attribute Values to add
     */
    void setValueSet(Set valSet) {
        valueSet = valSet;
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
        prnWriter.println(AdminReq.bundle.getString("requestdescription111") +
            " " + targetDN);
        
        avMap.put(attributeName, valueSet);
        if ((avMap != null) && (!avMap.isEmpty())) {
            Set set = avMap.keySet();
            for (Iterator it=set.iterator(); it.hasNext(); ) {
                String key = (String)it.next();
                prnWriter.println("  " + key + " =");
                Set valSet = (Set)avMap.get(key);
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
            bundle.getString("realmAddAttrVal") + " " + attributeName +
            " " + bundle.getString("inservice") + " " + serviceName +
            " " + bundle.getString("inrealm") + targetDN);

        String[] args = {attributeName, serviceName, realmPath};
        try {
            doLog(args, AdminUtils.ADD_ATTRVALS_REALM_ATTEMPT);
            OrganizationConfigManager ocm =
                new OrganizationConfigManager(ssoToken, realmPath);

            ocm.addAttributeValues(serviceName, attributeName, valueSet);

//            doLog(args, "added-attrvals-realm");
            doLog(args, AdminUtils.ADD_ATTRVALS_REALM);

        } catch (SMSException smse) {
            throw new AdminException(smse);
        }
    }
}

