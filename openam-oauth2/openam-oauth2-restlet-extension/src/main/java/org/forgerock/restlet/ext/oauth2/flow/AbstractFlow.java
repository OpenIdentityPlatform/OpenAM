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

package org.forgerock.restlet.ext.oauth2.flow;

import java.io.IOException;
import java.util.*;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.provider.Scope;
import org.forgerock.openam.oauth2.provider.impl.OpenAMIdentityVerifier;
import org.forgerock.openam.oauth2.provider.impl.OpenAMServerAuthorizer;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.ClientApplication;
import org.forgerock.openam.oauth2.model.SessionClient;
import org.forgerock.openam.oauth2.provider.ClientVerifier;
import org.forgerock.restlet.ext.oauth2.provider.OAuth2Client;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.restlet.ext.oauth2.representation.TemplateFactory;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.*;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Redirector;
import org.restlet.security.User;
import org.restlet.util.Series;

/**
 * Defines an abstract OAuth2 flow and the flow helper methods.
 */
public abstract class AbstractFlow extends ServerResource {

    protected OAuth2Constants.EndpointType endpointType;
    protected OAuth2Client client = null;
    protected User resourceOwner = null;
    protected SessionClient sessionClient = null;
    protected boolean issueRefreshToken = false;
    protected boolean fragment = false;

    protected static final int RESULT_VALID = 4;

    /**
     * If the {@link AbstractFlow#getCheckedScope} change the requested scope
     * then this value is true.
     */
    private boolean scopeChanged = false;

    private ClientVerifier clientVerifier = null;

    private OAuth2TokenStore tokenStore = null;

    public AbstractFlow(){
    }
    protected boolean checkIfRefreshTokenIsRequired(Request request){
        OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(request);
        issueRefreshToken = settings.getRefreshTokensEnabledState();
        return issueRefreshToken;
    }
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
                    "Toekn store is not initialised");
        }
        return tokenStore;
    }

    /**
     * After the call of {@link AbstractFlow#getCheckedScope} it return true if
     * the requested scope was changed.
     * 
     * @return
     */
    public boolean isScopeChanged() {
        return scopeChanged;
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
        validateOptionalParameters();
        validateNotAllowedParameters();
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
        validateOptionalParameters();
        validateNotAllowedParameters();
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
            doError(exception.getStatus());

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

    /**
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-5.2">5.2.
     *      Error Response</a>
     */
    public Representation doError(OAuthProblemException exception) {
        doError(Status.CLIENT_ERROR_BAD_REQUEST);
        return new JacksonRepresentation<Map>(exception.getErrorMessage());
    }

    /**
     * Error Response
     * <p/>
     * If the request fails due to a missing, invalid, or mismatching
     * redirection URI, or if the client identifier is missing or invalid, the
     * authorization server SHOULD inform the resource owner of the error, and
     * MUST NOT automatically redirect the user-agent to the invalid redirection
     * URI.
     * 
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-4.1.2.1">4.1.2.1.
     *      Error Response</a>
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-4.2.2.1">4.2.2.1.
     *      Error Response</a>
     */
    public Representation doError(OAuthProblemException exception, Reference redirect) {
        if (null != redirect) {
            return null;
        } else {
            // TODO make null safe and configure the error page
            return getPage("error.ftl", exception.getErrorMessage());
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

    protected User getAuthenticatedResourceOwner() throws OAuthProblemException {
        //authenticate the resource owner
        OpenAMIdentityVerifier identityVerifier = new OpenAMIdentityVerifier(getContext());
        int verified = identityVerifier.verify(getRequest(), getResponse());
        if (verified != RESULT_VALID){
            OAuth2Utils.DEBUG.warning("AuthorizeServerResource.represent(): Unable to login resource owner.");
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(), "Resource Owner unable to login");
        }
        OpenAMServerAuthorizer authorizer = new OpenAMServerAuthorizer();
        if (authorizer.authorize(getRequest(), getResponse()) == false){
            OAuth2Utils.DEBUG.error("The authorization server can not authorize the resource owner.");
            throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest(),
                    "The authorization server can not authorize the resource owner.");
        }
        if (getRequest().getClientInfo().getUser() != null) {
            return getRequest().getClientInfo().getUser();
        }
        OAuth2Utils.DEBUG.error("The authorization server can not authenticate the resource owner.");
        throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest(),
                "The authorization server can not authenticate the resource owner.");
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

    protected Map<String, Object> getDataModel(Set<String> scopes) {
        Map<String, Object> data = new HashMap<String, Object>(getRequest().getAttributes());
        data.put("target", getRequest().getResourceRef().toString());
        Set<String> displayNames = client.getClient().getDisplayName();
        Set<String> displayDescriptions = client.getClient().getDisplayDescription();
        Set<String> allScopes = client.getClient().getAllowedGrantScopes();
        String locale = OAuth2Utils.getLocale(getRequest());
        String displayName = "";
        String displayDescription = "";
        List<String> displayScope = new ArrayList<String>();

        //get the localized display name
        displayName = getDisplayParameter(locale, displayNames);
        displayDescription = getDisplayParameter(locale, displayDescriptions);

        //get the scope descriptions
        displayScope = getScopeDescriptionsForLocale(scopes, allScopes, locale);

        data.put("display_name", displayName);
        data.put("display_description", displayDescription);
        data.put("display_scope", displayScope);
        return data;
    }

    private String getDisplayParameter(String locale, Set<String> displayNames){
        Set<String> names = new HashSet<String>();
        String defaultName = null;
        final String DELIMITER = "|";
        for (String name : displayNames){
            if (name.contains(DELIMITER)){
                int locationOfDelimiter = name.indexOf(DELIMITER);
                if (name.substring(locationOfDelimiter).equalsIgnoreCase(locale)){
                    return name.substring(locationOfDelimiter+1, name.length());
                }
            } else {
                defaultName = name;
            }
        }

        return defaultName;
    }

    private List<String> getScopeDescriptionsForLocale(Set<String> scopes,
                                                       Set<String>scopesWithDescriptions,
                                                       String locale){
        final String DELIMITER = "\\|";
        List<String> list = new LinkedList<String>();
        for (String scope: scopes){
            for (String scopeDescription : scopesWithDescriptions){
                String[] parts = scopeDescription.split(DELIMITER);
                if (parts != null && parts[0].equalsIgnoreCase(scope)){
                    //no description or locale
                    if (parts.length == 1){
                        continue;
                    } else if (parts.length == 2){
                        //no locale add description
                        list.add(parts[1]);
                    } else if (parts.length == 3){
                        //locale and description
                        if (parts[1].equalsIgnoreCase(locale)){
                            list.add(parts[2]);
                        } else {
                            //not the right locale
                            continue;
                        }

                    } else {
                        OAuth2Utils.DEBUG.warning("Scope was input into the client settings in the wrong format for scope: " + scopeDescription);
                        continue;
                    }

                }

            }

        }
        return list;
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

    public Set<String> getCheckedScope(String requestedScope, Set<String> maximumScope,
            Set<String> defaultScope) {
        if (null == requestedScope) {
            return defaultScope;
        } else {
            Set<String> intersect =
                    new TreeSet<String>(OAuth2Utils.split(requestedScope, OAuth2Utils
                            .getScopeDelimiter(getContext())));
            Set<String> scopes = null;
            scopes = OAuth2Utils.parseScope(maximumScope);
            if (intersect.retainAll(scopes)) {
                OAuth2Utils.DEBUG.warning("AbstractFlow::Scope is different then requested");
                scopeChanged = true;
                return intersect;
            } else {
                scopeChanged = false;
                return intersect;
            }
        }
    }

    protected String[] getRequiredParameters() {
        return null;
    }

    protected void validateOptionalParameters() throws OAuthProblemException {
    }

    protected void validateNotAllowedParameters() throws OAuthProblemException {
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

            OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(getRequest());
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

    protected Set<String> executeRefreshTokenScopePlugin(String scopeRequest, Set<String> maxScope){
        Set<String> checkedScope = null;
        Set<String> requestedScopeSet = null;
        String pluginClass = null;
        Scope scopeClass = null;
        try {
            requestedScopeSet =
                    new TreeSet<String>(OAuth2Utils.split(scopeRequest, OAuth2Utils
                            .getScopeDelimiter(getContext())));
            OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(getRequest());
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
            checkedScope = scopeClass.scopeRequestedForRefreshToken(requestedScopeSet,
                    maxScope,
                    OAuth2Utils.parseScope(client.getClient().getAllowedGrantScopes()),
                    OAuth2Utils.parseScope(client.getClient().getDefaultGrantScopes()));
        } else {
            OAuth2Utils.DEBUG.error("AbstractFlow::No setting set for scope plugin class");
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "No setting set for scope plugin class");
        }

        return checkedScope;
    }

    protected Set<String> executeAuthorizationPageScopePlugin(String scopeRequest){
        Set<String> checkedScope = null;
        Set<String> requestedScopeSet = null;
        String pluginClass = null;
        Scope scopeClass = null;
        try {
            requestedScopeSet =
                    new TreeSet<String>(OAuth2Utils.split(scopeRequest, OAuth2Utils
                            .getScopeDelimiter(getContext())));
            OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(getRequest());
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
            checkedScope = scopeClass.scopeToPresentOnAuthorizationPage(requestedScopeSet,
                    OAuth2Utils.parseScope(client.getClient().getAllowedGrantScopes()),
                    OAuth2Utils.parseScope(client.getClient().getDefaultGrantScopes()));
        } else {
            OAuth2Utils.DEBUG.error("AbstractFlow::No setting set for scope plugin class");
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "No setting set for scope plugin class");
        }

        return checkedScope;
    }

    protected Map<String, Object> executeExtraDataScopePlugin(Map<String, String> data, CoreToken token){
        Map<String, Object> jsonData = null;
        String pluginClass = null;
        Scope scopeClass = null;
        try {
            OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(getRequest());
            pluginClass = settings.getScopeImplementationClass();
            if (pluginClass != null && !pluginClass.isEmpty()){
                scopeClass = (Scope) Class.forName(pluginClass).newInstance();
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("AbstractFlow::Exception during scope execution", e);
            jsonData = null;
            scopeClass = null;
        }

        // Validate the granted scope
        if (scopeClass != null && pluginClass != null){
            jsonData = scopeClass.extraDataToReturnForTokenEndpoint(data, token);
        } else {
            OAuth2Utils.DEBUG.error("AbstractFlow::No setting set for scope plugin class");
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "No setting set for scope plugin class");
        }

        return jsonData;
    }

    protected Map<String, String> executeAuthorizationExtraDataScopePlugin(Map<String, String> data, Map<String, CoreToken> token){
        Map<String, String> jsonData = null;
        String pluginClass = null;
        Scope scopeClass = null;
        try {
            OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(getRequest());
            pluginClass = settings.getScopeImplementationClass();
            if (pluginClass != null && !pluginClass.isEmpty()){
                scopeClass = (Scope) Class.forName(pluginClass).newInstance();
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("AbstractFlow::Exception during scope execution", e);
            jsonData = null;
            scopeClass = null;
        }

        // Validate the granted scope
        if (scopeClass != null && pluginClass != null){
            jsonData = scopeClass.extraDataToReturnForAuthorizeEndpoint(data, token);
        } else {
            OAuth2Utils.DEBUG.error("AbstractFlow::No setting set for scope plugin class");
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(null, "No setting set for scope plugin class");
        }

        return jsonData;
    }

    protected Map<String,String> getResponseTypes(String realm){
        OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(getRequest());
        Set<String> responseTypeSet = settings.getResponseTypes();
        if (responseTypeSet == null || responseTypeSet.isEmpty()){
            OAuth2Utils.DEBUG.error("AbstractFlow.getResponseType(): No response types for realm: " + realm);
            throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), "Invlaid Response Type");
        }
        Map<String, String> responseTypes = new HashMap<String, String>();
        for (String responseType : responseTypeSet){
            String[] parts = responseType.split("\\|");
            if (parts.length != 2){
                OAuth2Utils.DEBUG.error("AbstractFlow.getResponseType(): Response type wrong format for realm: " + realm);
                continue;
            }
            responseTypes.put(parts[0], parts[1]);
        }
        return responseTypes;
    }

}
