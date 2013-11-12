/**
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
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.AuthUtils;

import com.sun.identity.sm.DNMapper;

import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import javax.security.auth.login.LoginException;

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
 */
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
     * Evaluates a simple privilege of boolean type. The privilege indicates
     * if the user identified by the <code>principalName</code> 
     * can perform specified action on the specified resource.
     *
     * @param principalName principal name for whom to compute the privilege.
     * @param realm realm of the user principal "/" separated format 
     * @param resourceName name of the resource for which to compute 
     *                     policy result.
     * @param actionName name of the action the user is trying to perform on
     * the resource
     * @param env run time environment parameters
     *
     * @return the result of the evaluation as a boolean value
     *
     * @throws PolicyException exception form policy framework 
     * @throws SSOException if single sign on token is invalid
     * 
     */
    public boolean isAllowed(String principalName, String realm,  
        String resourceName, String actionName, Map env) 
        throws PolicyException, SSOException 
    {
        SSOToken token = getProxyToken(realm, principalName);
        boolean allowed = policyEvaluator.isAllowed(token, resourceName,
                actionName, env);
        return allowed;
    }

     /**
     * Evaluates a simple privilege of boolean type. The privilege indicates
     * if the user identified by the <code>principalName</code> 
     * can perform specified action on the specified resource.
     *
     * @param principalName principal name for whom to compute the privilege.
     * @param resourceName name of the resource for which to compute 
     *                     policy result.
     * @param actionName name of the action the user is trying to perform on
     * the resource
     * @param env run time environment parameters
     *
     * @return the result of the evaluation as a boolean value
     *
     * @throws PolicyException exception form policy framework 
     * @throws SSOException if single sign on token is invalid
     * 
     */
    public boolean isAllowed(String principalName, String resourceName, 
        String actionName, Map env) throws PolicyException, SSOException 
    {
        return isAllowed(principalName, null, resourceName, actionName, env);
    }

    /**
     * Gets policy decision for the user identified by the
     * <code>principalName</code> for the given resource
     *
     * @param principalName principal name for whom to compute the policy 
     *                      decision
     * @param realm realm of the user principal "/" separated format 
     * @param resourceName name of the resource for which to compute policy 
     *                      decision
     * @param env run time environment parameters
     *
     * @return the policy decision for the principal for the given resource
     *
     * @throws PolicyException exception form policy framework
     * @throws SSOException if single sign on token is invalid
     * 
     */
    public PolicyDecision getPolicyDecision(String principalName, String realm,
        String resourceName, Map env) 
        throws PolicyException, SSOException 
    {
        SSOToken token = getProxyToken(realm, principalName);
        PolicyDecision pd = policyEvaluator.getPolicyDecision(
                token, resourceName, null, env); //null actionNames
        // Let us log all policy evaluation results
        if (PolicyUtils.logStatus) {
            String decision = pd.toString();
            if (decision != null && decision.length() != 0) {
                String[] objs = { adminToken.getPrincipal().getName(), 
                                principalName, resourceName,
                                decision };
                PolicyUtils.logAccessMessage("PROXIED_POLICY_EVALUATION",
                            objs, adminToken);
            }
        }

        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message(
                    " Admin: " + adminToken.getPrincipal().getName()
                    + " got policy decision "
                    + " for principal: " + token.getPrincipal().getName() 
                    + " for resourceName:" + resourceName
                    + " for serviceType :" + serviceType
                    + " is " + pd);
        }

        return pd;
    }
    /**
     * Gets policy decision for the user identified by the
     * <code>principalName</code> for the given resource
     *
     * @param principalName principal name for whom to compute the policy 
     *                      decision
     * @param resourceName name of the resource for which to compute policy 
     *                      decision
     * @param env run time environment parameters
     *
     * @return the policy decision for the principal for the given resource
     *
     * @throws PolicyException exception form policy framework
     * @throws SSOException if single sign on token is invalid
     * 
     */
    public PolicyDecision getPolicyDecision(String principalName, 
        String resourceName, Map env) 
        throws PolicyException, SSOException 
    {
        return getPolicyDecision(principalName, null, resourceName, env);
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

    /**
     * Gets protected resources for a user identified by the
     * <code>principalName</code>.  Conditions defined  in the policies are
     * ignored while computing protected resources. 
     * Only resources that are  sub resources of the  given 
     * <code>rootResource</code> or equal to the given <code>rootResource</code>
     * would be returned.
     * If all policies applicable to a resource are 
     * only referral policies, no <code>ProtectedResource</code> would be
     * returned for such a resource.
     *
     * @param principalName principal name for whom to compute the privilege.
     * @param rootResource  only resources that are sub resources of the  
     *                      given <code>rootResource</code> or equal to the
     *                      given <code>rootResource</code> would be returned.
     *                      If <code>PolicyEvaluator.ALL_RESOURCES</code> is 
     *                      passed as <code>rootResource</code>, resources under
     *                      all root  resources of the service 
     *                      type are considered while computing protected 
     *                      resources.
     *
     * @return set of protected resources. The set contains
     *         <code>ProtectedResource</code> objects. 
     *
     * @throws PolicyException exception form policy framework
     * @throws SSOException if single sign on token is invalid
     * @see ProtectedResource
     * 
     */
    public Set getProtectedResourcesIgnoreConditions(String principalName, 
        String rootResource)  throws PolicyException, SSOException 
    {
            return getProtectedResourcesIgnoreConditions(principalName, null, 
            rootResource);
    }

    /**
     * Gets protected resources for a user identified by the
     * <code>principalName</code>.  Conditions defined  in the policies are
     * ignored while computing protected resources. 
     * Only resources that are  sub resources of the  given 
     * <code>rootResource</code> or equal to the given <code>rootResource</code>
     * would be returned.
     * If all policies applicable to a resource are 
     * only referral policies, no <code>ProtectedResource</code> would be
     * returned for such a resource.
     *
     * @param principalName principal name for whom to compute the privilege.
     * @param realm realm of the user principal "/" separated format 
     * @param rootResource  only resources that are sub resources of the  
     *                      given <code>rootResource</code> or equal to the
     *                      given <code>rootResource</code> would be returned.
     *                      If <code>PolicyEvaluator.ALL_RESOURCES</code> is 
     *                      passed as <code>rootResource</code>, resources under
     *                      all root  resources of the service 
     *                      type are considered while computing protected 
     *                      resources.
     *
     * @return set of protected resources. The set contains
     *         <code>ProtectedResource</code> objects. 
     *
     * @throws PolicyException exception form policy framework
     * @throws SSOException if single sign on token is invalid
     * @see ProtectedResource
     * 
     */
    public Set getProtectedResourcesIgnoreConditions(String principalName, 
        String realm, String rootResource)  
        throws PolicyException, SSOException 
    {
        SSOToken token = getProxyToken(realm, principalName);
        return policyEvaluator.getProtectedResourcesIgnoreConditions(
                token, rootResource);
    }

    /**
     * Gets proxy session token
     * @param principalName user to proxy as
     * @return proxy session token
     * @throws PolicyException if proxy session token can not be obtained
     * @throws SSOException if the session token of the administrative user
     * is invalid
     */
    private SSOToken getProxyToken(String realm, String principalName) 
        throws PolicyException, SSOException 
    {
        if ((realm == null) || realm.trim().equals("")) {
            realm = "/"; // set it to root org
        }
        SSOTokenManager.getInstance().validateToken(adminToken);
        SSOToken token = null;
        boolean proxyPermission = false;

        try {
            String orgDN = DNMapper.orgNameToDN(realm);
            if (PolicyManager.debug.messageEnabled()) {
                PolicyManager.debug.message("ProxyPolicyEvaluator."+
                    "getProxyToken:principalName, orgDN="+principalName+
                    ","+orgDN);
            }
            
            AuthContextLocal ac = AuthUtils.getAuthContext(orgDN);
            ac.login(
                com.sun.identity.authentication.AuthContext.IndexType.USER, 
                    principalName, true);
            token = ac.getSSOToken();
        } catch (AuthException ae) {
            throw new PolicyException(ae);
        } catch (LoginException le) {
            throw new PolicyException(le);
        } 
        if (token ==  null) {
            throw new SSOException(new PolicyException(ResBundleUtils.rbName,
                    "can_not_get_proxy_sso_token", null, null));
        }

        try {
            Set actionNames = new HashSet();
            actionNames.add("MODIFY");
            DelegationEvaluator de = new DelegationEvaluator();
            DelegationPermission permission =
                new DelegationPermission("/", "iPlanetAMPolicyService",
                    "1.0", "organization", "default",
                    actionNames, null);
            proxyPermission = de.isAllowed(adminToken, permission, null);
            if (PolicyManager.debug.messageEnabled()) {
                PolicyManager.debug.message("proxyPermission after delegation "
                    +"check:" +proxyPermission);
            }  
            if (!proxyPermission) {
                SSOTokenManager.getInstance().destroyToken(token);
                if (PolicyManager.debug.warningEnabled()) {
                    PolicyManager.debug.warning("Admin : " 
                            + adminToken.getPrincipal().getName()
                            + " can not create proxy sso token for user "
                            + principalName);

                }
                throw new PolicyException(ResBundleUtils.rbName,
                        "no_permission_to_create_proxy_sso_token", null, null);
            }
        } catch(DelegationException de) {
            throw new PolicyException(de);        
        }
        return token;
    }

}
