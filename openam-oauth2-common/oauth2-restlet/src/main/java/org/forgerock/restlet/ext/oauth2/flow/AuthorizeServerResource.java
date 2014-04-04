/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2012-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.restlet.ext.oauth2.flow;


import com.google.inject.Inject;
import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.AuthenticationRedirectRequiredException;
import org.forgerock.oauth2.core.Authorization;
import org.forgerock.oauth2.core.AuthorizationRequest;
import org.forgerock.oauth2.core.AuthorizationService;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ConsentRequiredException;
import org.forgerock.oauth2.core.ContextHandler;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.ResourceOwnerAuthorizationCodeAuthenticationHandler;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.forgerock.oauth2.core.UserConsentRequest;
import org.forgerock.oauth2.core.UserConsentResponse;
import org.forgerock.oauth2.reslet.ResourceOwnerAuthorizationCodeCredentialsExtractor;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.owasp.esapi.ESAPI;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.routing.Redirector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.forgerock.oauth2.core.AuthorizationRequest.createCodeAuthorizationRequest;
import static org.forgerock.oauth2.core.UserConsentResponse.createUserConsentResponse;
import static org.forgerock.oauth2.reslet.RestletUtils.getParameter;

public class AuthorizeServerResource extends AbstractFlow {

    private final ResourceOwnerAuthorizationCodeCredentialsExtractor resourceOwnerCredentialsExtractor;
    private final AuthorizationService authorizationService;
    private final ContextHandler contextHandler;

    @Inject
    public AuthorizeServerResource(
            final ResourceOwnerAuthorizationCodeCredentialsExtractor resourceOwnerCredentialsExtractor,
            final AuthorizationService authorizationService, final ContextHandler contextHandler) {
        this.resourceOwnerCredentialsExtractor = resourceOwnerCredentialsExtractor;
        this.authorizationService = authorizationService;
        this.contextHandler = contextHandler;
    }

    /**
     * Developers should note that some user-agents do not support the inclusion
     * of a fragment component in the HTTP "Location" response header field.
     * Such clients will require using other methods for redirecting the client
     * than a 3xx redirection response. For example, returning an HTML page
     * which includes a 'continue' button with an action linked to the
     * redirection URI.
     * <p/>
     * If TLS is not available, the authorization server SHOULD warn the
     * resource owner about the insecure endpoint prior to redirection.
     *
     * @return
     */
    @Get()
    public Representation represent() {

        final String clientId = getQueryValue("client_id");
        final String redirectUri = getQueryValue("redirect_uri");
        final String scope = getQueryValue("scope");
        final String state = getQueryValue("state");
        final String responseType = getQueryValue("response_type");
        String prompt = getQueryValue("prompt");

        if (prompt == null || prompt.isEmpty()) {
            prompt = getQueryValue("_prompt");
        }

        final ResourceOwnerAuthorizationCodeAuthenticationHandler authenticationHandler;
//        try {
            authenticationHandler = resourceOwnerCredentialsExtractor.extract(getRequest());
//        }

        try {
            final AuthorizationRequest authorizationRequest = createCodeAuthorizationRequest()
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scope(scope)
                    .state(state)
                    .responseType(responseType)
                    .prompt(prompt)
                    .authenticationHandler(authenticationHandler)
                    .context(contextHandler.createContext(ServletUtils.getRequest(getRequest())))
                    .locale(OAuth2Utils.getLocale(getRequest()))
                    .build();

            final UserConsentRequest userConsentRequest = authorizationService.requestAuthorization(authorizationRequest);

            if (!userConsentRequest.isConsentRequired()) {
                Map<String, Object> attrs = getRequest().getAttributes();
                attrs.put(OAuth2Constants.Custom.DECISION, OAuth2Constants.Custom.ALLOW);
                getRequest().setAttributes(attrs);
                Representation rep = new JsonRepresentation(new HashMap<String, Object>());
                return represent(rep);
            }

            return getPage("authorize.ftl", getDataModel(userConsentRequest.getDisplayName(),
                    userConsentRequest.getDisplayDescription(), userConsentRequest.getScopeDescription()));

        } catch (IllegalArgumentException e) {
            //TODO log
//            OAuth2Utils.DEBUG.error("AbstractFlow::Invalid parameters in request: " + sb.toString());
            if (e.getMessage().contains("client_id")) {
                throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(null, e.getMessage());
            }
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (ConsentRequiredException e) {
            throw OAuthProblemException.OAuthError.CONSENT_REQUIRED.handle(getRequest(), e.getMessage());
        } catch (InvalidClientException e) {
            throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(getRequest(), e.getMessage());
        } catch (InvalidGrantException e) {
            throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest(), e.getMessage());
        } catch (UnsupportedResponseTypeException e) {
            throw OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(getRequest(), e.getMessage());
        } catch (InvalidRequestException e) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(null, e.getMessage());
        } catch (RedirectUriMismatchException e) {
            throw OAuthProblemException.OAuthError.REDIRECT_URI_MISMATCH.handle(null, e.getMessage());
        } catch (AuthenticationRedirectRequiredException e) {
            throw OAuthProblemException.OAuthError.REDIRECT_TEMPORARY.handle(getRequest())
                    .redirectUri(e.getRedirectUri());
        } catch (BadRequestException e) {
            throw OAuthProblemException.OAuthError.BAD_REQUEST.handle(getRequest(), e.getMessage());
        } catch (InteractionRequiredException e) {
            throw OAuthProblemException.OAuthError.INTERACTION_REQUIRED.handle(getRequest(), e.getMessage());
        } catch (LoginRequiredException e) {
            throw OAuthProblemException.OAuthError.LOGIN_REQUIRED.handle(getRequest(), e.getMessage());
        } catch (AccessDeniedException e) {
            throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest(), e.getMessage());
        } catch (OAuth2Exception e) {
            //CATCH ALL
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(), e.getMessage());
        }
    }

    protected Map<String, Object> getDataModel(final String displayName, final String displayDescription,
            final Set<String> displayScope) {
        Map<String, Object> data = new HashMap<String, Object>(getRequest().getAttributes());
        data.put("target", getRequest().getResourceRef().toString());

        data.put("display_name", ESAPI.encoder().encodeForHTML(displayName));
        data.put("display_description", ESAPI.encoder().encodeForHTML(displayDescription));
        data.put("display_scope", encodeListForHTML(displayScope));
        return data;
    }

    private Set<String> encodeListForHTML(final Set<String> dirtyList) {
        final Set<String> htmlEncodedList = new LinkedHashSet<String>();

        for (String scope : dirtyList){
            htmlEncodedList.add(ESAPI.encoder().encodeForHTML(scope));
        }

        return htmlEncodedList;
    }

    @Post()
    public Representation represent(Representation entity) {

        final boolean consentGiven = "allow".equalsIgnoreCase(getParameter(getRequest(), "decision"));

        final boolean saveConsent = "on".equalsIgnoreCase(getParameter(getRequest(), "save_consent"));

        final String scope = getParameter(getRequest(), "scope");

        final String state = getParameter(getRequest(), "state");

        final String nonce = getParameter(getRequest(), "nonce");

        final String responseType = getParameter(getRequest(), "response_type");

        final String redirectUri = getParameter(getRequest(), "redirect_uri");
        final String clientId = getParameter(getRequest(), "client_id");

        final ResourceOwnerAuthorizationCodeAuthenticationHandler authenticationHandler;
//        try {
        authenticationHandler = resourceOwnerCredentialsExtractor.extract(getRequest());
//        }

        try {
            final UserConsentResponse userConsentResponse = createUserConsentResponse()
                    .consentGiven(consentGiven)
                    .saveConsent(saveConsent)
                    .scope(scope)
                    .state(state)
                    .nonce(nonce)
                    .responseType(responseType)
                    .redirectUri(redirectUri)
                    .clientId(clientId)
                    .context(contextHandler.createContext(ServletUtils.getRequest(getRequest())))
                    .authenticationHandler(authenticationHandler)
                    .build();

            final Authorization authorization = authorizationService.authorize(userConsentResponse);

            final Form tokenForm = toForm(authorization);

            final Reference redirectReference = new Reference(redirectUri);

            if (authorization.isFragment()) {
                redirectReference.setFragment(tokenForm.getQueryString());
            } else {
                final Iterator<Parameter> iter = tokenForm.iterator();
                while (iter.hasNext()) {
                    redirectReference.addQueryParameter(iter.next());
                }
            }

            final Redirector dispatcher = new Redirector(getContext(), redirectReference.toString(),
                    Redirector.MODE_CLIENT_FOUND);
            dispatcher.handle(getRequest(), getResponse());

            return getResponseEntity();

        } catch (IllegalArgumentException e) {
            //TODO log
//            OAuth2Utils.DEBUG.error("AbstractFlow::Invalid parameters in request: " + sb.toString());
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), e.getMessage());
        } catch (AccessDeniedException e) {
            throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest(), e.getMessage());
        } catch (InvalidGrantException e) {
            throw OAuthProblemException.OAuthError.INVALID_GRANT.handle(getRequest(), e.getMessage());
        } catch (InvalidRequestException e) {
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(null, e.getMessage());
        } catch (RedirectUriMismatchException e) {
            throw OAuthProblemException.OAuthError.REDIRECT_URI_MISMATCH.handle(null, e.getMessage());
        } catch (UnsupportedResponseTypeException e) {
            throw OAuthProblemException.OAuthError.UNSUPPORTED_RESPONSE_TYPE.handle(getRequest(), e.getMessage());
        } catch (OAuth2Exception e) {
            //CATCH ALL
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(), e.getMessage());
        }
    }

    protected Form toForm(final Authorization authorization) {

        final Form result = new Form();
        for (final Map.Entry<String, String> entry : authorization.getTokens().entrySet()) {
            final Parameter p = new Parameter(entry.getKey(), entry.getValue());
            if (!result.contains(p)) {
                result.add(p);
            }
        }
        return result;
    }
}
