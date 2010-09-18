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
 * $Id: PolicyEvaluatorFactory.java,v 1.2 2008/06/25 05:43:44 qcheng Exp $
 *
 */



package com.sun.identity.policy;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.am.util.Cache;

/** Factory class used to get PolicyEvaluator instances. 
 *  This is a singleton class.
 */
class PolicyEvaluatorFactory {

    private static final int CACHE_SIZE = 100;

    private static PolicyEvaluatorFactory policyEvaluatorFactory;

    private Cache evaluatorCache;

    /**
     * Private constructor, disables instances being created from outside
     * of this class 
     */
    private PolicyEvaluatorFactory() {
        evaluatorCache = new Cache(CACHE_SIZE);
    }

    /**
     * Gets an instance of PolicyEvaluatorFactory. 
     *
     * @return <code>PolicyEvaluatorFactory</code> object.
     * @exception PolicyException any policy exception coming from policy frame 
     *            work
     */
    synchronized public static PolicyEvaluatorFactory getInstance() 
        throws PolicyException
    {
        if (policyEvaluatorFactory == null) {
            policyEvaluatorFactory = new PolicyEvaluatorFactory();
        }
        return policyEvaluatorFactory;
    }

    /**
     * Gets an instance of PolicyEValuator
     *
     * @param token sso token used to get the policy evaluator
     * @param     serviceType service type for which get the proxy policy 
     *            evaluator 
     * @return    <code>PolicyEvaluator</code> 
     * @exception SSOException if the token is invalid
     * @exception NameNotFoundException if the <code>serviceType</code> is not 
     *            found in registered service types
     *         
     * @exception PolicyException any policy exception coming from policy frame 
     *            work
     */
    synchronized public PolicyEvaluator getPolicyEvaluator(
        SSOToken token, String serviceType) 
        throws NameNotFoundException, 
        PolicyException, SSOException 
    {
        PolicyEvaluator pe 
                = (PolicyEvaluator)evaluatorCache.get(serviceType);
        if (pe == null) {
            if (PolicyManager.debug.messageEnabled()) {
                PolicyManager.debug.message(
                        " User: " + token.getPrincipal().getName()
                        + " created policy evaluator (using " 
                        + " PolicyEvaluatorFactory)  for "  
                        + " for serviceType: " + serviceType);
            }
            pe = new PolicyEvaluator(serviceType);
            evaluatorCache.put(serviceType, pe);
            if (PolicyManager.debug.messageEnabled()) {
                PolicyManager.debug.message(
                        " User: " + token.getPrincipal().getName()
                        + " got policy evaluator "
                        + " (using PolicyEvaluatorFactory) for "
                        + " for serviceType: " + serviceType);
            }
        }
        return pe;
    }
}
