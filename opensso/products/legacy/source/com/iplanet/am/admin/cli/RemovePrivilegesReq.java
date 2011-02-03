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
 * $Id: RemovePrivilegesReq.java,v 1.2 2008/06/25 05:52:34 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.delegation.DelegationPrivilege;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;


class RemovePrivilegesReq extends AdminReq {
    private String realm;
    private String idName;
    private String idType;
    private Set setPrivileges;

    /**
     * Constructs a new <code>RemovePrivilegesReq</code> instance.
     *
     * @param realm Realm where Identity resides.
     * @param idName Name of the Identity.
     * @param idType Type of the Identity.
     * @param privileges Privilege names delimited by commas,
     */        
    RemovePrivilegesReq(
        String realm,
        String idName,
        String idType,
        Set privileges
    ) {
        super(realm);
        this.realm = realm;
        this.idName = idName;
        this.idType = idType;
        setPrivileges = privileges;
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
            AdminReq.bundle.getString("requestdescription136"), args));
        prnWriter.flush();
        return stringWriter.toString();    
    }
    
    void process(SSOToken ssoToken)
        throws AdminException {
        String[] args = {realm, idName, idType};
        AdminReq.writer.println(MessageFormat.format(
            AdminReq.bundle.getString("delegation-remove-privileges"), args));

        try {
            doLog(args, "ATTEMPT_REMOVE_PRIVILEGES");
            AMIdentity amid = new AMIdentity(ssoToken,
                idName, AdminXMLParser.convert2IdType(idType), realm, null);
            String uid = amid.getUniversalId();

            DelegationManager mgr = new DelegationManager(ssoToken, realm);
            Set privilegeObjects = mgr.getPrivileges();

            for (Iterator i = setPrivileges.iterator(); i.hasNext(); ){
                String name = (String)i.next();
                DelegationPrivilege dp = AdminUtils.getDelegationPrivilege(
                    name, privilegeObjects);
                boolean removed = false;

                if (dp != null) {
                    Set subjects = dp.getSubjects();
                    if (subjects.contains(uid)) {
                        subjects.remove(uid);
                        mgr.removePrivilege(name);
                        mgr.addPrivilege(dp);
                        removed = true;
                    }
                }

                if (!removed) {
                    String[] param = {name};
                    String msg = MessageFormat.format(
                        AdminReq.bundle.getString(
                            "delegation-does-not-has-privilege"), param);
                    throw new AdminException(msg);
                }
            }

            doLog(args, "REMOVE_PRIVILEGES");
        } catch (DelegationException e) {
            throw new AdminException(e);
        } catch (SSOException e) {
            throw new AdminException(e);
        }
    }
}

