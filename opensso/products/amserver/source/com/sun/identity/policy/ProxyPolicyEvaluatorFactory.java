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
 * $Id: ProxyPolicyEvaluatorFactory.java,v 1.2 2008/06/25 05:43:44 qcheng Exp $
 *
 */


package com.sun.identity.policy;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.am.util.Cache;

/**
 * Factory class used to get ProxyPolicyEvaluator instances. 
 * This is a singleton.
 *
 * @supported.all.api
 */
public class ProxyPolicyEvaluatorFactory {

    private static final int CACHE_SIZE = 100;

    private static ProxyPolicyEvaluatorFactory proxyPolicyEvaluatorFactory;

    private Cache evaluatorCache;

    /**
     * Private constructor, disables instances being created from outside
     * of this class 
     */
    private ProxyPolicyEvaluatorFactory() {
        evaluatorCache = new Cache(CACHE_SIZE);
    }

    /**
     * Gets an instance of ProxyPolicyEvaluatorFactory. 
     *
     * @return proxy policy evaluator factory
     * @throws PolicyException any policy exception coming from policy frame 
     *                         work
     */
    synchronized public static ProxyPolicyEvaluatorFactory getInstance() 
        throws PolicyException
    {
        if (proxyPolicyEvaluatorFactory == null) {
            proxyPolicyEvaluatorFactory = new ProxyPolicyEvaluatorFactory();
        }
        return proxyPolicyEvaluatorFactory;
    }

    /**
     * Gets an instance of <code>ProxyPolicyEvaluator</code>.
     * Only privileged users can get <code>ProxyPolicyEvaluator</code>. 
     * Only top level admin, realm admin or policy admin can get
     * <code>ProxyPolicyEvaluator</code>.
     *
     * @param token sso token used to get the proxy policy evaluator
     * @param serviceType service type for which get the proxy policy 
     *                    evaluator 
     * @return proxy policy evaluator 
     * @throws SSOException if the token is invalid
     * @throws NoPermissionException if the token does not have privileges 
     *                               to get proxy policy evaluator
     * @throws NameNotFoundException if the serviceType is not found in
     *         registered service types
     *         
     * @throws PolicyException any policy exception coming from policy frame 
     *                         work
     */
    synchronized public ProxyPolicyEvaluator getProxyPolicyEvaluator(
        SSOToken token, String serviceType) 
        throws NoPermissionException, NameNotFoundException, 
        PolicyException, SSOException 
    {
        String key = token.getTokenID().toString() + ":" + serviceType;
        ProxyPolicyEvaluator ppe 
                = (ProxyPolicyEvaluator)evaluatorCache.get(key);
        if (ppe == null) {
            if (PolicyManager.debug.messageEnabled()) {
                PolicyManager.debug.message(
                        " Admin: " + token.getPrincipal().getName()
                        + " created proxy policy evaluator for "
                        + " for serviceType: " + serviceType);
            }
            ppe = new ProxyPolicyEvaluator(token, serviceType);
            evaluatorCache.put(key, ppe);
        }
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message(
                    " Admin: " + token.getPrincipal().getName()
                    + " gotproxy policy evaluator for "
                    + " for serviceType: " + serviceType);
        }
        return ppe;
    }
}
