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
 * $Id: OrgCreateSubContReq.java,v 1.2 2008/06/25 05:52:29 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.PrintUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class OrgCreateSubContReq extends AdminReq {
    private Map subContReq = new HashMap();

    /**
     * Constructs a new OrgCreateSubContReq.
     *
     * @param targetDN the Container DN. 
     */        
    OrgCreateSubContReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the subCont DN and its avPair Map to the subContReq Map.
     *
     * @param subContDN the SubContainer DN
     * @param avPair the Map which contains the attribute as key and value
     */        
    void addSubContReq(String subContDN, Map avPair) {
        subContReq.put(subContDN, avPair);
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
        prnWriter.println(bundle.getString("requestdescription73") + " " +
            targetDN);
        AdminUtils.printAttributeNameValuesMap(prnWriter, prnUtl, subContReq);
        prnWriter.flush();
        return stringWriter.toString();    
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("organization") + " " + targetDN +
                "\n" + bundle.getString("createsubcontainers"));
        }
                                                                                
        writer.println(bundle.getString("organization") + " " + targetDN +
            "\n" + bundle.getString("createsubcontainers"));

        Set contNames = subContReq.keySet();
        for (Iterator i = contNames.iterator(); i.hasNext(); ) {
            String contName = (String)i.next();
            String[] params = {contName, targetDN};
            doLog(params, AdminUtils.CREATE_CONTAINER_ATTEMPT);
        }

        try {
            AMOrganization org = dpConnection.getOrganization(targetDN);
            Set subContsReqSet = org.createOrganizationalUnits(subContReq);
            printAMObjectDN(subContsReqSet);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(AdminReq.bundle.getString("statusmsg37"));
            }

            doLog(subContsReqSet, AdminUtils.CREATE_CONTAINER);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
