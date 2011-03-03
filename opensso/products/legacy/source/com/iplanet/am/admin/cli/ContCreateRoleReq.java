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
 * $Id: ContCreateRoleReq.java,v 1.2 2008/06/25 05:52:25 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.PrintUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


class ContCreateRoleReq extends AdminReq {
    protected Map values;
    protected String roleDN;

    /**
     * Constructs a new ContCreateRoleReq.
     *
     * @param targetDN the Container DN. 
     */        
    ContCreateRoleReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the role dn's and its avPair Map to roleReq Map.
     *
     * @param roleDN the DN of a group
     * @param avPair the Map which contains the attribute as key and value
     */
    void addRoleReq(String roleDN, Map avPair) {
        this.roleDN = roleDN;
        values = avPair;
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
        prnWriter.println(bundle.getString("requestdescription4") + " " +
            targetDN);
        Map map = new HashMap();
        map.put(roleDN, values);
        AdminUtils.printAttributeNameValuesMap(prnWriter, prnUtl, map);
        prnWriter.flush();
        return stringWriter.toString();    
    }

    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        AdminReq.writer.println(bundle.getString("container") + " " +
            targetDN + "\n" + bundle.getString("createroles"));

        try {
            String[] args = {roleDN, targetDN};
            doLog(args, AdminUtils.CREATE_ROLE_ATTEMPT);

            AMOrganizationalUnit orgUnit =
                dpConnection.getOrganizationalUnit(targetDN);
            Map map = new HashMap();
            map.put(roleDN, values);

            Set rolesReqSet = orgUnit.createRoles(map);

            if (AdminUtils.logEnabled()) {
                AdminUtils.log(AdminReq.bundle.getString("statusmsg18"));
            }

            for (Iterator iter = rolesReqSet.iterator(); iter.hasNext(); ) {
                AMRole role = (AMRole)iter.next();
                AdminReq.writer.println(role.getDN());
            }

            doLog(rolesReqSet, AdminUtils.CREATE_ROLE);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
