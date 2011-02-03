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
 * $Id: DefaultDiscoAuthorizer.java,v 1.2 2008/06/25 05:49:56 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco.plugins;

import java.util.Map;

import com.sun.identity.liberty.ws.interfaces.Authorizer;
import com.sun.identity.liberty.ws.disco.common.DiscoUtils;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.liberty.ws.disco.jaxb.*;
import com.sun.identity.policy.PolicyEvaluator;
import com.iplanet.sso.SSOToken;

/**
 * This class <code>DefaultDiscoAuthorizer</code> provides a default
 * implementation of the <code>Authorizer</code> interface.
 */
public class DefaultDiscoAuthorizer implements Authorizer {

    /**
     * Separator for resource.
     */
    public static final String RESOURCE_SEPERATOR = ";";

    PolicyEvaluator pe = null;
    
    /**
     * Default Constructor.
     */
    public DefaultDiscoAuthorizer() {
        DiscoUtils.debug.message("in DefaultDiscoAuthorizer.constructor");
        try {
            pe = new PolicyEvaluator("sunIdentityServerDiscoveryService");
        } catch (Exception e) {
            DiscoUtils.debug.error("DefaultDiscoAuthorizer.constructor: "
                + "Exception obtaining PolicyEvaluator: ", e);
        }
    }

    /**
     * Checks if the WSC is authorized to query or modify the select data.
     * @param credential credential of a WSC.
     *      In this implmentation, credential is the SSOToken of the WSC.
     * @param action request action.
     *      In this implementation, action is either
     *      <code>DiscoConstants.ACTION_LOOKUP</code> or
     *      <code>DiscoConstants.ACTION_UPDATE</code>.
     * @param data Object who is being accessed.
     *      In this implementation, data is of type ResourceOfferingType.
     * @param env A Map contains information useful for policy evaluation.
     *      The following key is defined and its value should be passed in:
     *      Key: <code>USER_ID</code>
     *      Value: id of the user whose resource is being accessed.
     *      In this implementation, the value is the userDN.
     *      Key: <code>AUTH_TYPE</code>
     *      Value: The authentication mechanism WSC used.
     *      Key: <code>MESSAGE</code>
     *      Value:
     *      <code>com.sun.identity.liberty.ws.soapbinding.Message</code>.
     * @return true if the WSC is authorized.
     */
    public boolean isAuthorized(Object credential, String action, 
    Object data, java.util.Map env)
    {
        DiscoUtils.debug.message("DefaultDiscoAuthorizer.isAuthorized.");
        if (pe == null) {
            DiscoUtils.debug.error("DefaultDiscoAuthorizer.isAuthorized: null "
                + "Policy Evaluator.");
            return false;
        }

        // another alternative to this method is not to check input and call
        // policy evaluator directly. The benefit of checking input first is
        // to obtain more meaningful error message.
        if (!checkInput(credential, action, data, env)) {
            return false;
        }

        String resource = null;
        try {
            ServiceInstanceType instance =
                ((ResourceOfferingType) data).getServiceInstance();
            resource = instance.getServiceType() + RESOURCE_SEPERATOR +
                        instance.getProviderID();
        } catch (Exception e) {
            DiscoUtils.debug.error("DefaultDiscoAuthorizer.isAuthorized: "
                + "Exception occured when constucting policy resource:", e);
            return false;
        }

        boolean result = false;
        try {
            result = pe.isAllowed((SSOToken) credential, resource, action, env);
        } catch (Exception e) {
            DiscoUtils.debug.error("DefaultDiscoAuthorizer.isAuthorized: "
                + "Exception occured during policy evaluation: ", e);
            result = false;
        }
        return result;
    }

    private boolean checkInput(Object credential, String action, Object data,
                                        Map env)
    {
        if ((credential == null) || !(credential instanceof SSOToken)) {
            DiscoUtils.debug.error("DefaultDiscoAuthorizer.checkInput: null or "
                + "wrong credential.");
            return false;
        }

        if ((action == null) || (!action.equals(DiscoConstants.ACTION_LOOKUP) &&
                                !action.equals(DiscoConstants.ACTION_UPDATE)))
        {
            DiscoUtils.debug.error("DefaultDiscoAuthorizer.checkInput: null or "
                + "wrong action.");
            return false;
        }

        if ((data == null) || !(data instanceof ResourceOfferingType)) {
            DiscoUtils.debug.error("DefaultDiscoAuthorizer.checkInput: null or "
                + "wrong data.");
            return false;
        }

        if ((env == null) || !env.containsKey(USER_ID) ||
            !env.containsKey(AUTH_TYPE) || !env.containsKey(MESSAGE))
        {
            DiscoUtils.debug.error("DefaultDiscoAuthorizer.checkInput: null or "
                + "missing env data.");
            return false;
        }
        return true;
    }

    /**
     * Returns authorization decision for the given action(query or modify)
     * and to the given select data. Currently this method always returns null.
     * @param credential credential of a <code>WSC</code>.
     * @param action request action.
     * @param data Object who is being accessed.
     * @param env A Map contains information useful for policy evaluation.
     *          The following key is defined and its value should be passed in:
     *          Key: <code>USER_ID</code>
     *          Value: id of the user whose resource is being accessed.
     *          Key: <code>AUTH_TYPE</code>
     *          Value: The authentication mechanism <code>WSC</code> used.
     *          Key: <code>MESSAGE</code>
     *          Value:
     *          <code>com.sun.identity.liberty.ws.soapbinding.Message</code>.
     * @return <code>AuthorizationDecision</code> object contains authorization
     *             decision information for the given resource.
     * @exception Exception
     */
    public Object getAuthorizationDecision(
                  Object credential,
                  String action,
                  Object data,
                  java.util.Map env)
    throws Exception {
        return null;
    }

}
