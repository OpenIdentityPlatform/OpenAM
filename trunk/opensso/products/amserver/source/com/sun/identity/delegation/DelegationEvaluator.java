/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DelegationEvaluator.java,v 1.16 2009/12/07 19:46:44 veiming Exp $
 *
 */

package com.sun.identity.delegation;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.interfaces.DelegationInterface;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.common.DNUtils;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.sm.DNMapper;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import javax.security.auth.Subject;

/**
 * The <code>DelegationEvaluator</code> class provides interfaces to evaluate
 * access permissions for an administrator.
 */

public class DelegationEvaluator {

    static final Debug debug = DelegationManager.debug;

    private static AMIdentity privilegedUser;

    // Provide allow permission for super admin during install
    private static boolean installTime = Boolean.valueOf(
         SystemProperties.get("com.sun.identity.security.amadmin",
         "false")).booleanValue();
    static Set adminUserSet = new HashSet();
    static AMIdentity adminUserId; 

    static {
        try {
            String adminUser = SystemProperties.get(
                "com.sun.identity.authentication.super.user");
            if (adminUser != null) {
                adminUserSet.add(DNUtils.normalizeDN(adminUser));
                adminUserId = new AMIdentity(DelegationManager.getAdminToken(), 
                    adminUser, IdType.USER, "/", null);
            }
        } catch (Exception e) {
            debug.error("DelegationEvaluator:", e);
        }
        // Register for notifications
        SMServiceListener listener = SMServiceListener.getInstance();
        listener.registerForNotifications();
    }
 
    private DelegationInterface pluginInstance = null;

    static {
        try {
            privilegedUser = new AMIdentity(DelegationManager.getAdminToken());
        } catch (Exception e) {
            debug.error("DelegationEvaluator:", e);
        }
    }

    /**
     * Constructor of <code>DelegationEvaluator</code> to get access control
     * permissions for users.
     *
     * @throws DelegationException for any abnormal condition
     */
    public DelegationEvaluator() throws DelegationException {
        if (debug.messageEnabled()) {
            debug.message("Instantiated a DelegationEvaluator.");
        }
    }

    public boolean isAllowed(
        SSOToken token,
        DelegationPermission permission,
        Map envParameters,
        boolean bSubResource
    ) throws SSOException, DelegationException {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            PrivilegeManager.superAdminSubject, "/");
        if (!ec.migratedToEntitlementService()) {
            return false;
        }

        try {
            AMIdentity user = new AMIdentity(token);
            if (((privilegedUser != null) && user.equals(privilegedUser)) ||
                (installTime && adminUserSet.contains(
                DNUtils.normalizeDN(token.getPrincipal().getName()))) ||
                user.equals(adminUserId) ) {
                return true;
            }
        } catch (IdRepoException ide) {
            throw (new DelegationException(ide.getMessage()));
        }

        if (!bSubResource) {
            return isAllowed(token, permission, envParameters);
        }

        StringBuilder buff = new StringBuilder();
        buff.append("sms://");
        if (permission.orgName != null) {
            buff.append(permission.orgName).append("/");
        }
        if (permission.getServiceName() != null) {
            buff.append(permission.getServiceName()).append("/");
        }
        if (permission.getVersion() != null) {
            buff.append(permission.getVersion()).append("/");
        }
        if (permission.getConfigType() != null) {
            buff.append(permission.getConfigType()).append("/");
        }
        if (permission.getSubConfigName() != null) {
            buff.append(permission.getSubConfigName());
        }
        String resource = buff.toString();
        try {
            Subject userSubject = SubjectUtils.createSubject(token);
            Evaluator eval = new Evaluator(PrivilegeManager.superAdminSubject,
                DelegationManager.DELEGATION_SERVICE);
            List<Entitlement> results = eval.evaluate(
                DNMapper.orgNameToDN(PolicyManager.DELEGATION_REALM),
                userSubject, resource, envParameters, true);
            List<String> copiedActions = new ArrayList<String>();
            copiedActions.addAll(permission.getActions());

            for (Entitlement e : results) {
                for (int i = copiedActions.size() - 1; i >= 0; --i) {
                    String action = copiedActions.get(i);
                    Boolean result = e.getActionValue(action);
                    if ((result != null) && result) {
                        copiedActions.remove(i);
                    }
                }
                if (copiedActions.isEmpty()) {
                    return true;
                }
            }
            return false;
        } catch (EntitlementException ex) {
            debug.error("DelegationEvaluator.isAllowed", ex);
            throw new DelegationException(ex);
        }
    }

    /**
     * Returns a boolean value indicating if a user has the specified
     * permission.
     * @param token sso token of the user evaluating permission
     * @param permission delegation permission to be evaluated
     * @param envParameters run-time environment parameters
     * 
     * @return the result of the evaluation as a boolean value
     * 
     * @throws SSOException if single-sign-on token invalid or expired
     * @throws DelegationException for any other abnormal condition
     * 
     */
    public boolean isAllowed(SSOToken token, DelegationPermission permission,
            Map envParameters) throws SSOException, DelegationException {

        boolean result = false;
        
        if ((permission != null) && (token != null)) {
            AMIdentity user = null;
            try {
                user = new AMIdentity(token);
                if (((privilegedUser != null) && user.equals(privilegedUser)) ||
                    (installTime && adminUserSet.contains(
                    DNUtils.normalizeDN(token.getPrincipal().getName()))) ||
                    user.equals(adminUserId) ) {
                    result = true;
                } else {
                    if (pluginInstance == null) {
                        pluginInstance = 
                            DelegationManager.getDelegationPlugin();
                        if (pluginInstance == null) {
                            throw new DelegationException(ResBundleUtils.rbName,
                                "no_plugin_specified", null, null);
                        }
                    }
                    result = pluginInstance.isAllowed(
                        token, permission, envParameters);
                }
            } catch (IdRepoException ide) {
                throw (new DelegationException(ide.getMessage()));
            }
        }
        if (debug.messageEnabled()) {
            debug.message("isAllowed() returns " + result + 
                " for user:token.getPrincipal().getName() " +
                token.getPrincipal().getName()+
                " for permission " + permission);
        }
        return result;
    }

    /**
     * Returns a set of permissions that a user has.
     * @param token  <code>SSOToken</code> of the user requesting permissions
     * @param orgName The name of the realm in which a user's delegation 
     * permissions are evaluated.
     * 
     * @return a <code>Set</code> of permissions that a user has
     * 
     * @throws SSOException  if single-sign-on token invalid or expired
     * @throws DelegationException for any other abnormal condition
     */

    public Set getPermissions(SSOToken token, String orgName)
            throws SSOException, DelegationException {
        if (pluginInstance != null) {
            String name = DNMapper.orgNameToDN(orgName);
            return pluginInstance.getPermissions(token, name);
        } else {
            throw new DelegationException(ResBundleUtils.rbName,
                    "no_plugin_specified", null, null);
        }
    }
}
