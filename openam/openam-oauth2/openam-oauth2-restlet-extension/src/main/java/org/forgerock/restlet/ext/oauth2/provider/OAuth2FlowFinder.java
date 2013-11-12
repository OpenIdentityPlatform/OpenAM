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
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.restlet.ext.oauth2.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.flow.*;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;

/**
 * Finds the proper OAuth 2 flow given the end point parameters.
 * <p/>
 * If it request for Authorization Endpoint then the response_type [code,token]
 * <p/>
 * If it request for Token Endpoint then the grant_type
 * [authorization_code,password
 * ,client_credentials,refresh_token,urn:ietf:params:
 * oauth:grant-type:saml2-bearer]
 */
public class OAuth2FlowFinder extends Finder {

    private final OAuth2Constants.EndpointType endpointType;

    /**
     * Constructor.
     * 
     * @param context
     *            The context.
     */
    public OAuth2FlowFinder(Context context, OAuth2Constants.EndpointType endpointType) {
        super(context, ErrorServerResource.class);
        this.endpointType = endpointType;
    }

    private final Map<String, Class<? extends AbstractFlow>> flowServerResources =
            new ConcurrentHashMap<String, Class<? extends AbstractFlow>>(6);

    /**
     * Creates a new instance of the {@link ServerResource} subclass designated
     * by the "targetClass" property. The default behavior is to invoke the
     * {@link #create(Class, org.restlet.Request, org.restlet.Response)} with
     * the "targetClass" property as a parameter.
     * 
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     * @return The created resource or ErrorServerResource.
     */
    public ServerResource create(Request request, Response response) {
        /*
         * If an authorization request is missing the "response_type" parameter,
         * or if the response type is not understood, the authorization server
         * MUST return an error response as described in Section 4.1.2.1.
         */
        switch (endpointType) {
        case AUTHORIZATION_ENDPOINT: {
            return create(AuthorizeServerResource.class, request, response);
        }
        case TOKEN_ENDPOINT: {
            return create(findTargetFlow(request, OAuth2Constants.Params.GRANT_TYPE), request, response);
        }
        default: {
            return create(findTargetFlow(request, null), request, response);
        }
        }
    }

    //public AbstractFlow create(Class<? extends AbstractFlow> targetClass, Request request,
    //       Response response) {
      public ServerResource create(Class<? extends ServerResource> targetClass, Request request,
                Response response) {
        AbstractFlow result = null;
        if (targetClass != null) {
            try {
                // Invoke the default constructor
                // result = targetClass.newInstance();
                result = (AbstractFlow) targetClass.newInstance();
                result.setEndpointType(endpointType);
            } catch (Exception e) {
                OAuth2Utils.DEBUG.warning("OAuth2FlowFinder::Exception while instantiating the target server resource.", e);
                OAuthProblemException.OAuthError.SERVER_ERROR.handle(request, e.getMessage())
                        .pushException();
                result = new ErrorServerResource();
                result.setEndpointType(endpointType);
            }
        }
        return result;
    }

    protected Class<? extends AbstractFlow> findTargetFlow(Request request, String propertyName) {
            Class<? extends AbstractFlow> targetClass = null;
        if (propertyName != null) {
            String type = OAuth2Utils.getRequestParameter(request, propertyName, String.class);
            if (type instanceof String && !type.isEmpty()) {
                targetClass = flowServerResources.get(type);
                if (targetClass == null) {
                    targetClass = ErrorServerResource.class;

                    OAuth2Utils.DEBUG.error("OAuth2FlowFinder::Unsupported grant type: Type is not supported: "
                            + type);
                    OAuthProblemException.OAuthError.UNSUPPORTED_GRANT_TYPE.handle(
                            request, "Grant type is not supported: " + type).pushException();
                }
            } else {
                targetClass = ErrorServerResource.class;

                OAuth2Utils.DEBUG.error("OAuth2FlowFinder::Type is not set");
                OAuthProblemException.OAuthError.INVALID_REQUEST.handle(
                        request, "Grant type is not set").pushException();

            }
        } else {

            OAuth2Utils.DEBUG.error("OAuth2FlowFinder::Type is not set");
            OAuthProblemException.OAuthError.INVALID_REQUEST.handle(
                    request, "Grant type is not set").pushException();

        }
        return targetClass;
    }

    public OAuth2FlowFinder supportAuthorizationCode() {
        flowServerResources.put(OAuth2Constants.TokeEndpoint.AUTHORIZATION_CODE,
                AuthorizationCodeServerResource.class);
        flowServerResources
                .put(OAuth2Constants.TokeEndpoint.REFRESH_TOKEN, RefreshTokenServerResource.class);
        return this;
    }

    public OAuth2FlowFinder supportImplicit() {
        return this;
    }

    public OAuth2FlowFinder supportClientCredentials() {
        flowServerResources.put(OAuth2Constants.TokeEndpoint.CLIENT_CREDENTIALS,
                ClientCredentialsServerResource.class);
        return this;
    }

    public OAuth2FlowFinder supportPassword() {
        flowServerResources.put(OAuth2Constants.TokeEndpoint.PASSWORD, PasswordServerResource.class);
        flowServerResources
                .put(OAuth2Constants.TokeEndpoint.REFRESH_TOKEN, RefreshTokenServerResource.class);
        return this;
    }

    public OAuth2FlowFinder supportSAML20() {
        flowServerResources.put(OAuth2Constants.TokeEndpoint.SAML2_BEARER, SAML20BearerServerResource.class);
        return this;
    }

}
