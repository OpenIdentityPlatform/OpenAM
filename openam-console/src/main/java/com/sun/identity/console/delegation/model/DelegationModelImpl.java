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
 * $Id: DelegationModelImpl.java,v 1.3 2008/06/25 05:42:53 qcheng Exp $
 *
 */

package com.sun.identity.console.delegation.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.delegation.DelegationPrivilege;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class DelegationModelImpl
    extends AMModelBase
    implements DelegationModel
{
    public DelegationModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns delegation subjects under a realm. Returning a set of 
     * universal ID of subject.
     *
     * @param realmName Name of realm.
     * @param pattern Wildcard for matching subject name.
     * @return delegation subjects under a realm.
     * @throws AMConsoleException if subject universal ID cannot be obtained.
     */
    public Set getSubjects(String realmName, String pattern)
        throws AMConsoleException {
        String[] params = {realmName, pattern};
        logEvent("ATTEMPT_GET_DELEGATION_SUBJECTS", params);
        try {
            DelegationManager mgr = new DelegationManager(
                getUserSSOToken(), realmName);
            Set results = mgr.getSubjects(pattern);
            logEvent("SUCCEED_GET_DELEGATION_SUBJECTS", params);
            return (results != null) ? results : Collections.EMPTY_SET;
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, pattern, strError};
            logEvent("SSO_EXCEPTION_GET_DELEGATION_SUBJECTS", params);
            debug.error("DelegationModelImpl.getSubjects", e);
            throw new AMConsoleException(strError);
        } catch (DelegationException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, pattern, strError};
            logEvent("DELEGATION_EXCEPTION_GET_DELEGATION_SUBJECTS", params);
            debug.error("DelegationModelImpl.getSubjects", e);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Returns a set of privileges of an identity.
     *
     * @param realmName Name of realm.
     * @param uid Universal ID of the identity.
     * @return a set of privileges of an identity.
     * @throws AMConsoleException if privilege cannot be determined.
     */
    public Set getPrivileges(String realmName, String uid)
        throws AMConsoleException {
        String[] params = {realmName, uid};
        logEvent("ATTEMPT_GET_PRIVILEGES_OF_DELEGATION_SUBJECT", params);

        try {
            DelegationManager mgr = new DelegationManager(
                getUserSSOToken(), realmName);
            Set results = mgr.getPrivileges(uid);
            logEvent("SUCCEED_GET_PRIVILEGES_OF_DELEGATION_SUBJECT", params);
            return (results != null) ? results : Collections.EMPTY_SET;
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, uid, strError};
            logEvent("SSO_EXCEPTION_GET_PRIVILEGES_OF_DELEGATION_SUBJECT",
                paramsEx);
            debug.error("DelegationModelImpl.getPrivileges", e);
            throw new AMConsoleException(strError);
        } catch (DelegationException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, uid, strError};
            logEvent(
                "DELEGATION_EXCEPTION_GET_PRIVILEGES_OF_DELEGATION_SUBJECT",
                paramsEx);
            debug.error("DelegationModelImpl.getPrivileges", e);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Set privileges of an identity.
     *
     * @param realmName Name of realm.
     * @param uid Universal ID of the identity.
     * @param privileges Map of privilege name to privilege value.
     * @throws AMConsoleException if privilege cannot be set.
     */
    public void setPrivileges(String realmName, String uid, Map privileges)
        throws AMConsoleException {
        String curPrivilegeName = null;
        try {
            DelegationManager mgr = new DelegationManager(
                getUserSSOToken(), realmName);
            Set privilegeObjects = mgr.getPrivileges();
            String[] params = new String[3];
            params[0] = realmName;
            params[1] = uid;

            for (Iterator i = privileges.keySet().iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                String strVal = (String)AMAdminUtils.getValue(
                    (Set)privileges.get(name));
                boolean bVal = strVal.equals(Boolean.TRUE.toString());

                params[2] = name;
                curPrivilegeName = name;
                DelegationPrivilege dp = getDelegationPrivilege(
                    name, privilegeObjects);

                if (dp != null) {
                    Set subjects = dp.getSubjects();
                    boolean modified = false;

                    if (bVal) {
                        if (!subjects.contains(uid)) {
                            subjects.add(uid);
                            modified = true;
                        }
                    } else {
                        if (subjects.contains(uid)) {
                            subjects.remove(uid);
                            modified = true;
                        }
                    }

                    if (modified) {
                        logEvent("ATTEMPT_MODIFY_DELEGATION_PRIVILEGE", params);
                        mgr.addPrivilege(dp);
                        logEvent("SUCCEED_MODIFY_DELEGATION_PRIVILEGE", params);
                    }
                } else if (bVal) {
                    Set subjects = new HashSet(2);
                    subjects.add(uid);
                    logEvent("ATTEMPT_MODIFY_DELEGATION_PRIVILEGE", params);
                    DelegationPrivilege newDp = new DelegationPrivilege(
                        name, subjects, realmName);
                    mgr.addPrivilege(newDp);
                    logEvent("SUCCEED_MODIFY_DELEGATION_PRIVILEGE", params);
                }
            }
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, uid, curPrivilegeName, strError};
            logEvent("SSO_EXCEPTION_MODIFY_DELEGATION_PRIVILEGE", paramsEx);
            throw new AMConsoleException(strError);
        } catch (DelegationException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, uid, curPrivilegeName, strError};
            logEvent("DELEGATION_EXCEPTION_MODIFY_DELEGATION_PRIVILEGE",
                paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private DelegationPrivilege getDelegationPrivilege(
        String name,
        Set privilegeObjects
    ) {
        DelegationPrivilege dp = null;
        for (Iterator i= privilegeObjects.iterator();
            i.hasNext() && (dp == null);
        ) {
            DelegationPrivilege p = (DelegationPrivilege)i.next();
            if (p.getName().equals(name)) {
                dp = p;
            }
        }
        return dp;
    }
}
