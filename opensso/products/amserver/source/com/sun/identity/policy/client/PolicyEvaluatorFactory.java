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
 * $Id: PolicyEvaluatorFactory.java,v 1.3 2008/06/25 05:43:46 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.client;

import com.sun.identity.shared.debug.Debug;
import java.util.HashMap;
import java.util.Map;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.security.AppSSOTokenProvider;
import com.iplanet.sso.SSOException;

/**
 * This class acts as a factory to get an instance of 
 * <code>com.sun.idenity.policy.client.PolicyEvaluator</code>
 *
 * @supported.all.api
 */
public class PolicyEvaluatorFactory {

    static Debug debug = Debug.getInstance("amRemotePolicy");
    private static PolicyEvaluatorFactory factory; //singleton instance
    private Map evaluatorsCache;

    /**
     * Constructs a policy evaluator factory
     */
    private PolicyEvaluatorFactory() {
	evaluatorsCache = new HashMap(10);
        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluatorFactory():"
                    + "created singleton instance");
        }
    }

    /**
     * Returns an instance of 
     * <code>com.sun.identity.policy.client.PolicyEvaluatorFactory</code>
     *
     * @return an instance of 
     * <code>com.sun.identity.policy.client.PolicyEvaluatorFactory</code> 
     */
    synchronized public static PolicyEvaluatorFactory getInstance() {
        if (factory == null) {
            factory = new PolicyEvaluatorFactory();
        }
	return factory;
    }

    /**
     * Returns an instance of 
     * <code>com.sun.identity.policy.client.PolicyEvaluator</code>
     *
     * @param serviceName name of the service for which to get the
     *            <code>PolicyEvaluator</code>.
     * @return an instance of <code>PolicyEvaluator</code>.
     * @throws PolicyException if creation of evaluator fails.
     * @throws SSOException if application single sign on token is invalid
     */
    public PolicyEvaluator getPolicyEvaluator(String serviceName)
            throws PolicyException, SSOException
    {
	return getPolicyEvaluator(serviceName, 
                null); //null appSSOTokenProvider
    }

    /**
     * Returns an instance of 
     * <code>com.sun.identity.policy.client.PolicyEvaluator</code>
     *
     * @param serviceName name of the service for which to get the
     *        <code>com.sun.identity.policy.client.PolicyEvaluator</code> 
     * @param appSSOTokenProvider application single sign on token Provider
     * @return an instance of 
     *         <code>com.sun.identity.policy.client.PolicyEvaluator</code> 
     * @throws PolicyException if creation of evaluator fails.
     * @throws SSOException if application single sign on token is invalid.
     */
    synchronized public PolicyEvaluator getPolicyEvaluator(
	String serviceName,
	AppSSOTokenProvider appSSOTokenProvider)
	throws PolicyException, SSOException
    {
        PolicyEvaluator pe = null;
        if (serviceName == null) {
            if (debug.warningEnabled()) {
                debug.warning("PolicyEvaluatorFactory.getPolicyEvaluator():"
                        + "serviceName is null");
            }
            return null;
        } //else do the following

        Map appTokenEvaluatorsMap = (Map)evaluatorsCache.get(serviceName);
        if (appTokenEvaluatorsMap == null) {
            appTokenEvaluatorsMap = new HashMap(5);
            evaluatorsCache.put(serviceName, appTokenEvaluatorsMap);
        }
        pe = (PolicyEvaluator)appTokenEvaluatorsMap.get(appSSOTokenProvider);
        if ( pe == null) {
            if (debug.messageEnabled()) {
                debug.message("PolicyEvaluatorFactory.getPolicyEvaluator():"
                        + "serviceName=" + serviceName
                        + ":appSSOTokenProvider=" +appSSOTokenProvider
                        + ":creating new PolicyEvaluator");
            }
            pe = new PolicyEvaluator(serviceName, appSSOTokenProvider);
            appTokenEvaluatorsMap.put(appSSOTokenProvider, pe);
        } else {
            if (debug.messageEnabled()) {
                debug.message("PolicyEvaluatorFactory.getPolicyEvaluator():"
                        + "serviceName=" + serviceName
                        + ":appSSOTokenProvider=" +appSSOTokenProvider
                        + ":returning PolicyEvaluator from cache");
            }
        }
        return pe;
    }
}
