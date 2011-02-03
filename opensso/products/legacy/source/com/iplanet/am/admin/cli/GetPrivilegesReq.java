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
 * $Id: GetPrivilegesReq.java,v 1.2 2008/06/25 05:52:27 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.delegation.DelegationPrivilege;
import com.sun.identity.sm.DNMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;


class GetPrivilegesReq extends AdminReq {
    private String realm;
    private String idName;
    private String idType;

    /**
     * Constructs a new <code>GetPrivilegesReq</code> instance.
     *
     * @param realm Realm where Identity resides.
     * @param idName Name of the Identity.
     * @param idType Type of the Identity.
     */        
    GetPrivilegesReq(String realm, String idName, String idType) {
        super(realm);
        this.realm = realm;
        this.idName = idName;
        this.idType = idType;
    }

    /**
     * Returns string equivalent of this object.
     *
     * @return String equivalent of this object. 
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);

        String[] args = {realm, idName, idType};
        prnWriter.println(MessageFormat.format(
            AdminReq.bundle.getString("requestdescription134"), args));
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException {
        String[] args = {realm, idName, idType};
        AdminReq.writer.println(MessageFormat.format(
            AdminReq.bundle.getString("delegation-get-privileges"), args));

        try {
            doLog(args, "ATTEMPT_GET_PRIVILEGES");
            DelegationManager mgr = new DelegationManager(
                ssoToken, realm);
            AMIdentity amid = new AMIdentity(ssoToken,
                idName, AdminXMLParser.convert2IdType(idType), realm, null);
            Set results = mgr.getPrivileges(amid.getUniversalId());
            doLog(args, "GET_PRIVILEGES");

            if ((results != null) && !results.isEmpty()) {
                for (Iterator i = results.iterator(); i.hasNext(); ) {
                    DelegationPrivilege p = (DelegationPrivilege)i.next();
                    AdminReq.writer.println(" " + p.getName());
                }
            } else {
                AdminReq.writer.println(
                    AdminReq.bundle.getString("delegation-no-privileges"));
            }
        } catch (DelegationException e) {
            throw new AdminException(e);
        } catch (SSOException e) {
            throw new AdminException(e);
        }
    }
}

