/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

package org.forgerock.restlet.ext.openam.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.oauth2.provider.AbstractOpenAMAuthorizer;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.provider.impl.OpenAMUser;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.iplanet.sso.SSOException;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.policy.client.PolicyEvaluatorFactory;

/**
 * An OpenAMAuthorizer request for a Policy Decision.
 * <p/>
 * It use the remote {@link PolicyEvaluator}
 *
 */
public class OpenAMAuthorizer extends AbstractOpenAMAuthorizer {

    protected PolicyEvaluator pe;

    /**
     * Default constructor.
     */
    public OpenAMAuthorizer() {
        super();
        init();
    }

    /**
     * Constructor.
     * 
     * @param identifier
     *            The identifier unique within an application.
     */
    public OpenAMAuthorizer(String identifier) {
        super(identifier);
        init();
    }

    protected void init() {
        try {
            pe = PolicyEvaluatorFactory.getInstance().getPolicyEvaluator(WEB_AGENT_SERVICE);
        } catch (SSOException e) {
            OAuth2Utils.DEBUG.error("Error getting policy for authorizer: ", e );
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getL10NMessage(Locale
                    .getDefault()), e);
        } catch (PolicyException e) {
            OAuth2Utils.DEBUG.error("Error getting policy for authorizer: ", e );
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e
                    .getCompleteL10NMessage(Locale.getDefault()), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getPolicyDecision(OpenAMUser user, Request request, Response response)
            throws SSOException, PolicyException {
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        if (null != getIdentifier()) {
            Set<String> paramValue = new HashSet<String>(1);
            paramValue.add(getIdentifier());
            env.put("application_identifier", paramValue);
        }
        // env.put(Condition.REQUEST_AUTHENTICATED_TO_REALMS, "/");

        Set<String> actions = new HashSet<String>();
        actions.add(request.getMethod().getName());
        PolicyDecision pd =
                pe.getPolicyDecision(user.getToken(), request.getResourceRef().toString(), actions,
                        env);
        return pe.isAllowed(user.getToken(), request.getResourceRef().toString(), request
                .getMethod().getName(), env);
    }

    protected void createEnvironment(Request request, Map<String, Set<String>> env) {
        Set<String> paramValue = new HashSet<String>(1);
        paramValue.add(request.getClientInfo().getAddress());
        env.put("ip", paramValue);
    }
}
