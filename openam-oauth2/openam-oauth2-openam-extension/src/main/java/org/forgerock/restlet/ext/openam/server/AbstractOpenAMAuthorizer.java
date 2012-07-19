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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.openam.server;

import java.util.Locale;

import org.forgerock.restlet.ext.openam.OpenAMUser;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.security.Authorizer;

import com.iplanet.sso.SSOException;
import com.sun.identity.policy.PolicyException;

/**
 * An AbstractOpenAMAuthorizer request for a Policy Decision. There are two
 * implementation because there is an internal
 * {@link com.sun.identity.policy.PolicyEvaluator} and a remote
 * {@link com.sun.identity.policy.client.PolicyEvaluator}
 * 
 * @author Laszlo Hordos
 */
public abstract class AbstractOpenAMAuthorizer extends Authorizer {

    public static final String WEB_AGENT_SERVICE = "iPlanetAMWebAgentService";

    /**
     * Default constructor.
     */
    public AbstractOpenAMAuthorizer() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param identifier
     *            The identifier unique within an application.
     */
    public AbstractOpenAMAuthorizer(String identifier) {
        super(identifier);
    }

    /**
     * Attempts to authorize the request.
     * <p/>
     * IF application_identifier=OAUTH2 THEN realm=/
     * 
     * @param request
     *            The request sent.
     * @param response
     *            The response to update.
     * @return True if the authorization succeeded.
     */
    @Override
    protected boolean authorize(Request request, Response response) {
        if (request.getClientInfo().getUser() instanceof OpenAMUser) {
            try {
                OpenAMUser user = (OpenAMUser) request.getClientInfo().getUser();
                return getPolicyDecision(user, request, response);
            } catch (SSOException e) {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e.getL10NMessage(Locale
                        .getDefault()), e);
            } catch (PolicyException e) {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e
                        .getCompleteL10NMessage(Locale.getDefault()), e);
            }
        }
        return false;
    }

    protected abstract boolean getPolicyDecision(OpenAMUser user, Request request, Response response)
            throws SSOException, PolicyException;
}
