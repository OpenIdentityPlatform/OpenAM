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
 * $Id: ContCreateGroupContainerReq.java,v 1.2 2008/06/25 05:52:24 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.PrintUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class ContCreateGroupContainerReq extends AdminReq {
    private Map mapRequest = new HashMap(); 

    /**
     * Constructs a new instance of create group container request.
     *
     * @param targetDN distinguished name where group container is going
     *        to be created. 
     */        
    ContCreateGroupContainerReq(String targetDN) {
        super(targetDN);
    }

    /**
     * Adds group container distinguished name and its attribute-values to
     * request map.
     *
     * @param dn group container distinguished name.
     * @param values attribute-values map.
     */
    void addGroupContainerRequest(String dn, Map values) {
        mapRequest.put(dn, values);
    }

    /**
     * Returns string equivalent of this instance.
     *
     * @return string equivalent of this instance. 
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter); 
        prnWriter.println(bundle.getString("requestdescription75") + " " +
            targetDN);
        AdminUtils.printAttributeNameValuesMap(prnWriter, prnUtl, mapRequest);
        prnWriter.flush();
        return stringWriter.toString();
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("container") + " " + targetDN +
                "\n" + bundle.getString("creategroupcontainers"));
        }
                                                                                
        writer.println(bundle.getString("container") + " " + targetDN +
            "\n" + bundle.getString("creategroupcontainers"));

        Set roleNames = mapRequest.keySet();
        for (Iterator i = roleNames.iterator(); i.hasNext(); ) {
            String roleName = (String)i.next();
            String[] params = {roleName, targetDN};
            doLog(params, AdminUtils.CREATE_GROUP_CONTAINER_ATTEMPT);
        }

        try {
            AMOrganizationalUnit ou =
                dpConnection.getOrganizationalUnit(targetDN);

            Set set = ou.createGroupContainers(mapRequest);
            printAMObjectDN(set);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(AdminReq.bundle.getString("statusmsg39"));
            }

            doLog(set, AdminUtils.CREATE_GROUP_CONTAINER);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
