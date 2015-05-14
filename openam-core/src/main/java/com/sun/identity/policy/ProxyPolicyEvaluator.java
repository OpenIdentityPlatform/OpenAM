/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ProxyPolicyEvaluator.java,v 1.4 2009/01/28 05:35:01 ww203982 Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */

package com.sun.identity.policy;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.ldap.util.DN;

/**
 * Class that lets a priviliged user to compute policy results for 
 * another user.
 * Only privileged users can get <code>ProxyPolicyEvaluator</code>
 *  - only top level administrator, realm level policy administrator, 
 * realm administrator or realm policy administrator can get
 * <code>ProxyPolicyEvaluator</code>. Top level administrator can compute policy
 * results for any user. Realm administrator or policy administrator can
 * compute policy results only for users who are members of the realm
 * (including sub realm) that they manage. If they try to compute policys
 * result for any other user, they would get a <code>PolicyException</code>.
 * This class can be used only within the web container running policy server.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class ProxyPolicyEvaluator {

    private SSOToken adminToken;
    private String serviceType;
    private PolicyEvaluator policyEvaluator;
    private static String baseDNString;
    private static DN baseDN;

    static {
        baseDNString = com.sun.identity.sm.ServiceManager.getBaseDN();
        baseDN= new DN(baseDNString);
    }

    /**
     * Constructs a <code>ProxyPolicyEvaluator</code> instance.
     * Only privileged users can create <code>ProxyPolicyEvaluator</code>.
     *
     * @param token single sign on token used to construct the proxy policy
     *        evaluator.
     * @param serviceType service type for which construct the proxy policy 
     *                    evaluator 
     * @throws NoPermissionException if the token does not have privileges 
     *         to create proxy policy evaluator
     * @throws NameNotFoundException if the serviceType is not found in
     *         registered service types
     * @throws PolicyException any policy exception coming from policy 
     *                         framework
     * @throws SSOException if the token is invalid
     */
    ProxyPolicyEvaluator(SSOToken token, String serviceType) 
        throws NoPermissionException, NameNotFoundException, 
        PolicyException, SSOException 
    {
        SSOTokenManager.getInstance().validateToken(token);
        this.adminToken = token;
        this.serviceType = serviceType;
        this.policyEvaluator 
                = PolicyEvaluatorFactory.getInstance()
                .getPolicyEvaluator(token, serviceType);
    }

    /**
     * Gets policy decision for a resource, skipping subject evaluation. 
     * Conditions would be evaluated and would include applicable advices 
     * in policy decisions. Hence, you could get details such as
     * <code>AuthLevel</code>, <code>AuthScheme</code> that would be required to
     * access the resource.  
     *
     * @param resourceName name of the resource for which to compute policy 
     *                      decision
     * @param actionNames names of the actions the user is trying to perform on
     *                   the resource
     *
     * @param env run time environment parameters
     *
     * @return the policy decision for the principal for the given resource
     *
     * @throws PolicyException exception form policy framework
     * @throws SSOException if single sign on token is invalid
     * 
     */
    public PolicyDecision getPolicyDecisionIgnoreSubjects(String resourceName, 
            Set actionNames, Map env) throws PolicyException, SSOException 
    {
        PolicyDecision pd = policyEvaluator.getPolicyDecisionIgnoreSubjects(
                resourceName, actionNames, env); 
        // Let us log all policy evaluation results
        if (PolicyUtils.logStatus) {
            String decision = pd.toString();
            if (decision != null && decision.length() != 0) {
                String[] objs =
                    {adminToken.getPrincipal().getName(), resourceName,
                        decision};
                PolicyUtils.logAccessMessage(
                     "PROXIED_POLICY_EVALUATION_IGNORING_SUBJECTS", 
                      objs, adminToken);
            }
        }

        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message(
                    " Admin: " + adminToken.getPrincipal().getName()
                    + " got policy decision "
                    + " ignoring subjects "
                    + " for resourceName:" + resourceName
                    + " for serviceType :" + serviceType
                    + " is " + pd);
        }

        return pd;
    }
}
