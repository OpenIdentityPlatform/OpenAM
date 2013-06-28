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

package org.forgerock.openam.oauth2.provider.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.provider.impl.OpenAMUser;
import org.forgerock.openam.oauth2.provider.AbstractOpenAMAuthorizer;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.iplanet.sso.SSOException;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.PolicyException;

/**
 * An OpenAMServerAuthorizer request for a Policy Decision.
 * <p/>
 * It use the remote {@link PolicyEvaluator}
 * 
 * @author Laszlo Hordos
 */
public class OpenAMServerAuthorizer extends AbstractOpenAMAuthorizer {

    protected PolicyEvaluator pe;

    /**
     * Default constructor.
     */
    public OpenAMServerAuthorizer() {
        super();
        init();
    }

    protected void init() {
        try {
            pe = new PolicyEvaluator(WEB_AGENT_SERVICE);
        } catch (SSOException e) {
            OAuth2Utils.DEBUG.error("OpenAMServerAuthorizer::Error creating policy evaluator: " + e );
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getL10NMessage(Locale
                    .getDefault()), e);
        } catch (PolicyException e) {
            OAuth2Utils.DEBUG.error("OpenAMServerAuthorizer::Error creating policy evaluator: " + e );
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e
                    .getCompleteL10NMessage(Locale.getDefault()), e);
        }
    }

    protected boolean getPolicyDecision(OpenAMUser user, Request request, Response response)
            throws SSOException, PolicyException {
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> paramValue = new HashSet<String>(1);
        paramValue.add(getIdentifier());
        env.put("application_identifier", paramValue);

        Set<String> actions = new HashSet<String>();
        actions.add(request.getMethod().getName());
        return pe.isAllowed(user.getToken(), request.getResourceRef().toString(), request
                .getMethod().getName(), env);
    }
}
