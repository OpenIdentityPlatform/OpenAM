/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PolicyDecisionUtils.java,v 1.3 2009/06/19 20:39:09 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.util;

import com.iplanet.sso.SSOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.policy.ActionDecision;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.ProxyPolicyEvaluatorFactory;
import com.sun.identity.policy.ProxyPolicyEvaluator;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.plugins.AuthLevelCondition;
import com.sun.identity.policy.plugins.AuthSchemeCondition;
import com.sun.identity.policy.plugins.AuthenticateToRealmCondition;
import com.sun.identity.policy.plugins.AuthenticateToServiceCondition;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.DNMapper;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The class provides some policy utility methods to be used by authentication
 * service for Resource/IP/Environment based authentication.
 */
public class PolicyDecisionUtils {
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String ALLOW = "allow";
    private static final String WEB_AGENT_SERVICE_NAME = 
        "iPlanetAMWebAgentService";
    public static final String AUTH_USER_ADVICE = "AuthUserConditionAdvice";
    public static final String AUTH_ROLE_ADVICE = "AuthRoleConditionAdvice";
    public static final String AUTH_REDIRECTION_ADVICE = 
        "AuthRedirectionConditionAdvice";
    private static Debug debug = Debug.getInstance("amPolicy");
    private static ProxyPolicyEvaluator pe;
    private static String errorMsg;
    private static Set actionNames = new HashSet();

    static {
        try {
            SSOToken defToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            actionNames.add(GET);
            actionNames.add(POST);
            pe = ProxyPolicyEvaluatorFactory.getInstance()
                .getProxyPolicyEvaluator(defToken, WEB_AGENT_SERVICE_NAME);
        } catch (PolicyException p) {
            debug.error("PolicyDecisionUtils: Unable to get PolicyEvaluator",
                p);
            errorMsg = p.getMessage();
        } catch (SSOException ssoe) {
            debug.error("PolicyDecisionUtils: Unable to get PolicyEvaluator",
                ssoe);
            errorMsg = ssoe.getMessage();
        }
    }

    /**
     * Performs Resource/IP/Environment based authentication. This method
     * is used by auth login viewbean.
     * @param resourceUrl Resource URL for policy evaluation.
     * @param realm The realm which is used in authentication.
     * @param  envParameters Environment map for policy evaluation.
     *   Keys of the map are Strings, values of the map are Set of Strings.
     * @throws PolicyException if policy processing error occurs.
     * @return a list which may be contain empty, one or two values.
     *  If the returned List size is two, first value is an instance of 
     *  <code>AuthContext.IndexType</code>, second value is a String which 
     *  indicates the value of the <code>AuthContext.IndexType</code>.
     *  If the returned List size is one, the value is a String which indicates 
     *  the redirection URL (this is the redirection advice case).
     *  If the return List is empty, it means that there is no policy advice for
     *  the resource to be accessed.
     *       
     */
    public static List doResourceIPEnvAuth(String resourceUrl, String realm,
        Map envParameters) throws PolicyException {
        ActionDecision decision = null;
                
        if (resourceUrl != null) {                    
            decision = getActionDecision(resourceUrl, envParameters);
            return getPolicyAdvice(decision, realm);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    private static ActionDecision getActionDecision(
        String url, Map envParameters) throws PolicyException {
        ActionDecision ad = null;
        if (pe != null) {
            PolicyDecision pd = null;
            try {
                pd = pe.getPolicyDecisionIgnoreSubjects(url, actionNames, 
                    envParameters);
            } catch (PolicyException e) {
                debug.error("PolicyDecisionUtils.getActionDecision()", e);
                return null;
            } catch (SSOException ssoe) {
                debug.error("PolicyDecisionUtils.getActionDecision()", ssoe);
                return null;
            }
            Map actionDecisions = pd.getActionDecisions();
            if (actionDecisions != null) {
                if ((ad = (ActionDecision) actionDecisions.get(GET)) == null) {
                    ad = (ActionDecision) actionDecisions.get(POST);
                }
            }
        } else {
            throw new PolicyException(errorMsg);
        }
                
        return ad;
    }
   
    /**
     * Returns the matching policy advice. The method finds the advice
     * for the specified realm first, if none found, return anyone 
     * realm/user/role/authlevel/service/module advice from the 
     * <code>ActionDecision</code>.
     */ 
    private static List getPolicyAdvice(ActionDecision ad, String realm) {
        Map advices;
        if (ad == null) {
            // Problem is policy evaluation?
            return Collections.EMPTY_LIST;
        }
        // Check is the resource is allowed
        Set values = ad.getValues();
        if (values.contains(ALLOW)) {
            return Collections.EMPTY_LIST;
        } else if ((advices = ad.getAdvices()) != null) {
            List answer = new ArrayList();
            if (debug.messageEnabled()) {
                debug.message("PolicyDecisionUtils: processActionDecision : " 
                     + advices.values().toString() + ", realm=" + realm);
            }       
            
            // check if realm equals root suffix
            if (realm == null) {
                realm = "/";
            }
            if  (!realm.startsWith("/")) {
                // convert DN to realm
                realm = DNMapper.orgNameToRealmName(realm);
            }
            // handle module/service/user/role/authlevel advice
            // TBD : may want handle composite advice later??
            StringBuffer sb = new StringBuffer();
            if (findAdviceValue(advices, AUTH_USER_ADVICE, realm, sb)) {
                answer.add(AuthContext.IndexType.USER);
                answer.add(sb.toString());
            } else if (findAdviceValue(advices, AUTH_ROLE_ADVICE, realm, sb)) {
                answer.add(AuthContext.IndexType.ROLE);
                answer.add(sb.toString());
            } else if (findAdviceValue(advices, AuthenticateToServiceCondition
                .AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE, realm, sb)) {
                answer.add(AuthContext.IndexType.SERVICE);
                answer.add(sb.toString());
            } else if (findAdviceValue(advices,
                AuthSchemeCondition.AUTH_SCHEME_CONDITION_ADVICE, realm, sb)) {
                answer.add(AuthContext.IndexType.MODULE_INSTANCE);
                answer.add(sb.toString());
            } else if (findAdviceValue(advices,
                AuthLevelCondition.AUTH_LEVEL_CONDITION_ADVICE, realm, sb)) {
                answer.add(AuthContext.IndexType.LEVEL);
                answer.add(sb.toString());
            } else if (findAdviceValue(advices, 
                AUTH_REDIRECTION_ADVICE, realm, sb)) {
                answer.add(sb.toString());
            } else {
                // there is no advice for this specific realm, just pick anyone.
                // That advice will be for a different realm
                String url = getOneAdviceAsRedirectURL(advices);
                if (url != null) {
                    answer.add(url);
                }
            }
            return answer;
        } else {
            // case without advices
            return Collections.EMPTY_LIST;
        }
    }
    
    // find first advice which matches the given advice type and realm
    private static boolean findAdviceValue(Map advices, String adviceType, 
        String realm, StringBuffer adviceValue) {
        String value = "";
        Set advice = (Set) advices.get(adviceType);
        if (advice != null) {
            Iterator items = advice.iterator();
            while  (items.hasNext()) {
                String item = (String) items.next();
                if (debug.messageEnabled()) {
                    debug.message("PolicyDecisionUtils.findAdviceValue: advice="
                        + item + ", realm=" + realm + ", type=" + adviceType);
                }
                // value contains two part : <realm>:<value>, e.g. /realm:4
                if (item.startsWith("/")) {
                    // realm present, remove string before ":" from the 
                    // advice to get actual value
                    int col = item.indexOf(":");
                    if (col != -1) {
                        String tmp = item.substring(0, col);
                        if (!tmp.equals(realm)) {
                            // This requires authentication at different realm,
                            // but we can't change realm once authentication 
                            // started, so ignore this advice
                            continue;
                        }
                        // ":" exists in the value
                        if (col != item.length() - 1) {
                            value = item.substring(col + 1);
                        } else {
                            // ":" is the last string, error advice case, ignore
                            continue;
                        }   
                    } else {
                        // no realm parameter
                        value = item;
                    }
                } else {
                    // no realm parameter
                    value = item;
                }
                // found first match, out of the loop
                break;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("PolicyUtils:findAdviceValue, return value=" + value);
        }
        if (value.length() != 0) {
            adviceValue.append(value);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns one advice from the advices as redirection URL.
     * returns null if no valid advice found.
     * The URL will redirect user to /UI/Login with different realm
     * for authentication.
     */
    private static String getOneAdviceAsRedirectURL(Map advices) {
        if ((advices == null) || advices.isEmpty()) {
            return null;
        }

        // loop through all advices to find 
        // realm/user/role/authlevel/service/module advice
        boolean found = false;
        String adviceType = null;
        String paramName = null;
        Iterator types = advices.keySet().iterator();
        while (types.hasNext()) {
            adviceType = (String) types.next();
            if (AuthenticateToRealmCondition.
                AUTHENTICATE_TO_REALM_CONDITION_ADVICE.equals(adviceType)) {
                // this is authenticate to realm
                found = true;
                break;
            } else if (AUTH_USER_ADVICE.equals(adviceType)) {
                paramName = ISAuthConstants.USER_PARAM;
                found = true;
                break;
            } else if (AUTH_ROLE_ADVICE.equals(adviceType)) {
                paramName = ISAuthConstants.ROLE_PARAM;
                found = true;
                break;
            } else if (AuthenticateToServiceCondition.
                AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE.equals(adviceType)) {
                paramName = ISAuthConstants.SERVICE_PARAM;
                found = true;
                break;
            } else if (AuthSchemeCondition.AUTH_SCHEME_CONDITION_ADVICE.
                equals(adviceType)) {
                paramName = ISAuthConstants.MODULE_PARAM;
                found = true;
                break;
            } else if (AuthLevelCondition.AUTH_LEVEL_CONDITION_ADVICE.
                equals(adviceType)) {
                paramName = ISAuthConstants.AUTH_LEVEL_PARAM;
                found = true;
                break;
            }
        }

        if (!found) {
            // no matching advice type found
            return null;
        }

        Set advice = (Set) advices.get(adviceType);
        if (advice != null) {
            // get first realm/module/role/user/service/authlevel advice 
            String item = (String) advice.iterator().next();     
            // value contains one or two part : <realm>[:<value>], e.g. /realm:4
            String realm = AMAuthUtils.getRealmFromRealmQualifiedData(item);
            String value = AMAuthUtils.getDataFromRealmQualifiedData(item);
            if (debug.messageEnabled()) {
                debug.message("PolicyDecisionUtils.getOneAdvice: advice=" + 
                    item + ", type=" + adviceType + ", realm=" + realm +
                    ", indexName=" + value);
            }
            if ((value == null) || (value.length() == 0)) {
                return null;
            }
            StringBuilder sb = new StringBuilder("/UI/Login");
            if (AuthenticateToRealmCondition.
                AUTHENTICATE_TO_REALM_CONDITION_ADVICE.equals(adviceType)) {
                sb.append("?realm=").append(value);
            } else {
                sb.append("?realm=").append(realm).append("&")
                  .append(paramName).append("=").append(value);
            }
            return sb.toString();
        } else {
            return null;
        }
    }
}
