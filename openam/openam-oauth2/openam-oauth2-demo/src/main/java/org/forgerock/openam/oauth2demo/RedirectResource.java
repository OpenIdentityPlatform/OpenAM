/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of company]"
 */

package org.forgerock.openam.oauth2demo;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.consumer.BearerOAuth2Proxy;
import org.forgerock.openam.oauth2.model.BearerToken;
import org.forgerock.restlet.ext.oauth2.consumer.BearerTokenExtractor;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.engine.util.Base64;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Redirector;
import org.restlet.routing.Template;

/**
 * A RedirectResource does ...
 */
public class RedirectResource extends Redirector {

    private BearerTokenExtractor helper = new BearerTokenExtractor();

    /**
     * Constructor for RedirectResource
     * 
     * @param context
     *            context of the redirector
     * @param targetTemplate
     *            template to display
     */
    public RedirectResource(Context context, String targetTemplate) {
        super(context, targetTemplate);
    }

    /**
     * RedirectResource Constructor
     * 
     * @param context
     *            context of the redirector
     * @param targetPattern
     *            pattern to use
     * @param mode
     *            mode to use
     */
    public RedirectResource(Context context, String targetPattern, int mode) {
        super(context, targetPattern, mode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Reference getTargetRef(Request request, Response response) {
        Reference target = null;
        if (Method.GET.equals(request.getMethod())) {
            try {
                // Extract token from Implicit flow or Exception
                BearerToken token =
                        helper.extractToken(OAuth2Utils.ParameterLocation.HTTP_FRAGMENT, request);

                // Extract exception from Authorization Code flow
                if (null == token) {
                    token = helper.extractToken(OAuth2Utils.ParameterLocation.HTTP_QUERY, request);
                }

                Form parameters = request.getResourceRef().getQueryAsForm();

                // Request access_token for the code
                if (null == token) {
                    String code = parameters.getFirstValue(OAuth2Constants.Params.CODE);
                    if (code instanceof String) {
                        BearerOAuth2Proxy proxy = BearerOAuth2Proxy.popOAuth2Proxy(getContext());
                        if (null != proxy) {
                            token = proxy.flowAuthorizationToken(code);
                        }
                    }
                }
                HttpServletRequest servletRequest = ServletUtils.getRequest(request);
                if (null != token && null != servletRequest) {
                    servletRequest.getSession(true)
                            .setAttribute(BearerToken.class.getName(), token);
                }

                String state = parameters.getFirstValue(OAuth2Constants.Params.STATE);
                if (state instanceof String) {
                    try {
                        String stateURL = new String(Base64.decode(state), "ISO-8859-1");
                        // Create the template
                        Template rt = new Template(stateURL);
                        rt.setLogger(getLogger());
                        // Return the formatted target URI
                        if (new Reference(stateURL).isRelative()) {
                            // Be sure to keep the resource's base reference.
                            target =
                                    new Reference(request.getResourceRef(), rt.format(request,
                                            response));
                        }

                        target = new Reference(rt.format(request, response));

                    } catch (UnsupportedEncodingException e) {
                        getLogger().log(Level.INFO, "Unsupported STATE encoding error", e);
                    } catch (IllegalArgumentException e) {
                        getLogger().log(Level.INFO, "Unable to decode the STATE token", e);
                    }
                }
            } catch (OAuthProblemException e) {
                target = super.getTargetRef(request, response);
                for (Parameter parameter : e.getErrorForm()) {
                    target.addQueryParameter(parameter);
                }
                e.printStackTrace();
            } catch (ResourceException e) {
                throw e;
            }
        }
        if (target == null) {
            target = super.getTargetRef(request, response);
        }

        return target;
    }
}
