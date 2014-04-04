/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS. All rights reserved.
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

package org.forgerock.restlet.ext.oauth2.flow;

import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.Scope;
import org.forgerock.openam.oauth2.OAuth2ConfigurationFactory;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.openam.oauth2.provider.ClientVerifier;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.restlet.ext.oauth2.provider.OAuth2Client;
import org.forgerock.restlet.ext.oauth2.representation.TemplateFactory;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CacheDirective;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Redirector;
import org.restlet.util.Series;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Defines an abstract OAuth2 flow and the flow helper methods.
 */
public abstract class AbstractFlow extends ServerResource {

    protected OAuth2Constants.EndpointType endpointType;
    protected OAuth2Client client = null;
    protected boolean fragment = false;

    private ClientVerifier clientVerifier = null;

    private OAuth2TokenStore tokenStore = null;

    public ClientVerifier getClientVerifier() throws OAuthProblemException {
        if (null == clientVerifier) {
            OAuth2Utils.DEBUG.error("AbstractFlow::ClientVerifier is not initialised");
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(),
                    "ClientVerifier is not initialised");
        }
        return clientVerifier;
    }

    public OAuth2TokenStore getTokenStore() throws OAuthProblemException {
        if (null == tokenStore) {
            OAuth2Utils.DEBUG.error("AbstractFlow::Token store is not initialised");
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(),
                    "Token store is not initialised");
        }
        return tokenStore;
    }

    /**
     * Set-up method that can be overridden in order to initialize the state of
     * the resource. By default it does nothing.
     * 
     * @see #init(Context, Request, Response)
     */
    protected void doInit() throws ResourceException {
        clientVerifier = OAuth2Utils.getClientVerifier(getContext());
        tokenStore = OAuth2Utils.getTokenStore(getContext());
    }

    /**
     * Handles a call by first verifying the optional request conditions and
     * continue the processing if possible. Note that in order to evaluate those
     * conditions, {@link #getInfo()} or
     * {@link #getInfo(org.restlet.representation.Variant)} methods might be
     * invoked.
     * 
     * @return The response entity.
     * @throws ResourceException
     */
    protected Representation doConditionalHandle() throws ResourceException {
        validateMethod();
        validateContentType();
        validateRequiredParameters();
        fragment = false;
        // -------------------------------------
        // Add Cache-Control: no-store
        // Pragma: no-cache
        // -------------------------------------
        getResponse().getCacheDirectives().add(CacheDirective.noStore());
        Series<Header> additionalHeaders =
                (Series<Header>) getResponse().getAttributes().get(
                        HeaderConstants.ATTRIBUTE_HEADERS);
        if (additionalHeaders == null) {
            additionalHeaders = new Series<Header>(Header.class);
            getResponse().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, additionalHeaders);
        }
        additionalHeaders.add(HeaderConstants.HEADER_PRAGMA, HeaderConstants.CACHE_NO_CACHE);
        return super.doConditionalHandle();
    }

    /**
     * Effectively handles a call without content negotiation of the response
     * entity. The default behavior is to dispatch the call to one of the
     * {@link #get()}, {@link #post(org.restlet.representation.Representation)},
     * {@link #put(org.restlet.representation.Representation)},
     * {@link #delete()}, {@link #head()} or {@link #options()} methods.
     * 
     * @return The response entity.
     * @throws org.restlet.resource.ResourceException
     * 
     */
    protected Representation doHandle() throws ResourceException {
        validateMethod();
        validateContentType();
        validateRequiredParameters();
        return super.doHandle();
    }

    public void setEndpointType(OAuth2Constants.EndpointType endpointType) {
        this.endpointType = endpointType;
    }

    /**
     * Invoked when an error or an exception is caught during initialization,
     * handling or releasing. By default, updates the responses's status with
     * the result of
     * {@link org.restlet.service.StatusService#getStatus(Throwable, org.restlet.resource.Resource)}
     * .
     * 
     * @param throwable
     *            The caught error or exception.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        if (throwable instanceof OAuthProblemException) {
            OAuthProblemException exception = (OAuthProblemException) throwable;

            if (exception.getStatus().equals(Status.REDIRECTION_TEMPORARY)){
                Redirector redirector =
                        new Redirector(new Context(), exception.getRedirectUri().toString(), Redirector.MODE_CLIENT_PERMANENT);
                redirector.handle(getRequest(), getResponse());
                return;
            } else {
                doError(exception.getStatus());
            }

            switch (endpointType) {
            case TOKEN_ENDPOINT: {
                getResponse()
                        .setEntity(new JacksonRepresentation<Map>(exception.getErrorMessage()));
                break;
            }
            case AUTHORIZATION_ENDPOINT: {
                Redirector dispatcher = null;
                if (fragment){
                    dispatcher =
                            OAuth2Utils.ParameterLocation.HTTP_FRAGMENT.getRedirector(getContext(),
                                    exception);
                } else {
                    dispatcher =
                            OAuth2Utils.ParameterLocation.HTTP_QUERY.getRedirector(getContext(),
                                    exception);
                }

                if (null != dispatcher) {
                    dispatcher.handle(getRequest(), getResponse());
                } else {
                    // TODO Introduce new method
                    Representation result = getPage("error.ftl", exception.getErrorMessage());
                    if (null != result) {
                        getResponse().setEntity(result);
                    }
                }
            break;
            }
            default: {
                errorPage(exception);
            }
            }
        } else {
            // TODO Use custom StatusServer to set the proper status
            doCatch(OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(), throwable
                    .getMessage()));
            // super.doCatch(throwable);
        }
    }

    public void errorPage(OAuthProblemException exception) {
        Representation result = getPage("error.ftl", exception.getErrorMessage());
        if (null != result) {
            getResponse().setEntity(result);
        }
    }

    // TODO Use flexible util to detect the client-agent and select the proper
    // display
    protected Representation getPage(String templateName, Object dataModel) {
        String display =
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Custom.DISPLAY, String.class);
        OAuth2Constants.DisplayType displayType = OAuth2Constants.DisplayType.PAGE;
        if (OAuth2Utils.isNotBlank(display)) {
            try {
                displayType = Enum.valueOf(OAuth2Constants.DisplayType.class, display.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        Representation r = null;
        if (display != null && display.equalsIgnoreCase(OAuth2Constants.DisplayType.POPUP.name())){
            Representation popup = getPage(displayType.getFolder(), "authorize.ftl", dataModel);

            try {
                ((Map)dataModel).put("htmlCode", popup.getText());
            } catch (IOException e) {
                OAuth2Utils.DEBUG.error("AbstractFlow::Server can not serve the content of authorization page");
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(),
                        "Server can not serve the content of authorization page");
            }
            r = getPage(displayType.getFolder(), "popup.ftl", dataModel);
        } else {
            r = getPage(displayType.getFolder(), templateName, dataModel);
        }
        if (null != r) {
            return r;
        }
        OAuth2Utils.DEBUG.error("AbstractFlow::Server can not serve the content of authorization page");
        throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(),
                "Server can not serve the content of authorization page");
    }

    protected Representation getPage(String display, String templateName, Object dataModel) {
        TemplateRepresentation result = null;
        Object factory = getContext().getAttributes().get(TemplateFactory.class.getName());
        String reference =
                "templates/" + (null != display ? display : OAuth2Constants.DisplayType.PAGE.getFolder())
                        + "/" + templateName;
        if (factory instanceof TemplateFactory) {
            result = ((TemplateFactory) factory).getTemplateRepresentation(reference);
        } else {
            factory = TemplateFactory.newInstance(getContext());
            getContext().getAttributes().put(TemplateFactory.class.getName(), factory);
            result = ((TemplateFactory) factory).getTemplateRepresentation(reference);
        }
        if (null != result) {
            result.setDataModel(dataModel);
        }
        return result;
    }

    /**
     * Validate the {@code redirectionURI} and return an object used in the
     * session.
     * <p/>
     * Throws {@link OAuthProblemException.OAuthError#REDIRECT_URI_MISMATCH}
     * 
     * @return
     * @throws OAuthProblemException
     */
    protected OAuth2Client validateRemoteClient() {
        switch (endpointType) {
            case AUTHORIZATION_ENDPOINT: {
                String client_id =
                        OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.CLIENT_ID,
                                String.class);
                ClientApplication client = null;
                client = getClientVerifier().findClient(client_id, getRequest());
                if (null != client) {
                    return new OAuth2Client(client);
                } else {
                    /*
                    * unauthorized_client The client is not authorized to request
                    * an authorization code using this method.
                    */
                    OAuth2Utils.DEBUG.error("AbstractFlow::Unauthorized client accessing authorize endpoint");
                    throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(null);
                }
            }
            case TOKEN_ENDPOINT: {
            return getAuthenticatedClient();
        }
        default: {
            return null;
        }
        }
    }

    protected OAuth2Client getAuthenticatedClient() throws OAuthProblemException {
        if (getRequest().getClientInfo().getUser() != null
                && getRequest().getClientInfo().isAuthenticated()) {
            if (getRequest().getClientInfo().getUser() instanceof OAuth2Client) {
                return (OAuth2Client) getRequest().getClientInfo().getUser();
            }
        }
        OAuth2Utils.DEBUG.error("The authorization server can not authenticate the client.");
        throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest(),
                "The authorization server can not authenticate the client.");
    }

    protected void validateMethod() throws OAuthProblemException {
        switch (endpointType) {
        case AUTHORIZATION_ENDPOINT: {
            if (!(Method.POST.equals(getRequest().getMethod()) || Method.GET.equals(getRequest()
                    .getMethod()))) {
                throw OAuthProblemException.OAuthError.METHOD_NOT_ALLOWED
                        .handle(getRequest(), "Required Method: GET or POST found: "
                                + getRequest().getMethod().getName());
            }
            break;
        }

        case TOKEN_ENDPOINT: {
            if (!Method.POST.equals(getRequest().getMethod())) {
                throw OAuthProblemException.OAuthError.METHOD_NOT_ALLOWED.handle(getRequest(),
                        "Required Method: POST found: " + getRequest().getMethod().getName());
            }
            break;
        }
        default: {

        }
        }
    }

    protected void validateContentType() throws OAuthProblemException {
        switch (endpointType) {
        case AUTHORIZATION_ENDPOINT: {
            if (!(null == getRequest().getEntity() || getRequest().getEntity() instanceof EmptyRepresentation)
                    && !MediaType.APPLICATION_WWW_FORM.equals(getRequest().getEntity()
                            .getMediaType())) {
                OAuth2Utils.DEBUG.error("AbstractFlow::Invalid Content Type for authorization endpoint");
                throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                        "Invalid Content Type");
            }
        }
        case TOKEN_ENDPOINT: {
            if (!(null == getRequest().getEntity() || getRequest().getEntity() instanceof EmptyRepresentation)
                    && !MediaType.APPLICATION_WWW_FORM.equals(getRequest().getEntity()
                            .getMediaType())) {
                OAuth2Utils.DEBUG.error("AbstractFlow::Invalid Content Type for token endpoint");
                throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                        "Invalid Content Type");
            }
        }
        default: {

        }
        }
    }

    protected void validateRequiredParameters() throws OAuthProblemException {
        String[] required = getRequiredParameters();
        boolean isClientID = false;
        if (required != null && required.length > 0) {
            StringBuilder sb = null;
            for (String s : required) {
                String str = OAuth2Utils.getRequestParameter(getRequest(), s, String.class);
                if (str == null || str.isEmpty()) {
                    if (null == sb) {
                        sb = new StringBuilder("Missing parameters: ");
                    }
                    sb.append(s).append(" ");
                    if (s.equalsIgnoreCase(OAuth2Constants.Params.CLIENT_ID)){
                        isClientID = true;
                    }
                }
            }
            if (null != sb && !isClientID) {
                OAuth2Utils.DEBUG.error("AbstractFlow::Invlaid parameters in request: " + sb.toString());
                throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), sb
                        .toString());
            } else if (null != sb && isClientID) {
                OAuth2Utils.DEBUG.error("AbstractFlow::Invlaid parameters in request: " + sb.toString());
                throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(null, sb
                        .toString());
            }
        }
    }

    protected String[] getRequiredParameters() {
        return null;
    }

    protected Set<String> executeAccessTokenScopePlugin(String scopeRequest){
        Set<String> checkedScope = null;
        Set<String> requestedScopeSet = null;
        String pluginClass = null;
        Scope scopeClass = null;
        try {
            requestedScopeSet =
                    new TreeSet<String>(OAuth2Utils.split(scopeRequest, OAuth2Utils
                            .getScopeDelimiter(getContext())));

            OAuth2ProviderSettings settings = OAuth2ConfigurationFactory.Holder.getConfigurationFactory().getOAuth2ProviderSettings(getRequest());
            pluginClass = settings.getScopeImplementationClass();
            if (pluginClass != null && !pluginClass.isEmpty()){
                scopeClass = (Scope) Class.forName(pluginClass).newInstance();
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("AbstractFlow::Exception during scope execution", e);
            checkedScope = null;
            scopeClass = null;
        }
        // Validate the granted scope
        if (scopeClass != null && pluginClass != null){
            checkedScope = scopeClass.scopeRequestedForAccessToken(requestedScopeSet,
                    OAuth2Utils.parseScope(client.getClient().getAllowedGrantScopes()),
                    OAuth2Utils.parseScope(client.getClient().getDefaultGrantScopes()));
        } else {
            OAuth2Utils.DEBUG.error("AbstractFlow::No setting set for scope plugin class");
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "No setting set for scope plugin class");
        }

        return checkedScope;
    }
    
    protected String getGrantType() {
    	return OAuth2Utils.getRequestParameter(getRequest(), OAuth2Constants.Params.GRANT_TYPE, String.class);
    }

}
