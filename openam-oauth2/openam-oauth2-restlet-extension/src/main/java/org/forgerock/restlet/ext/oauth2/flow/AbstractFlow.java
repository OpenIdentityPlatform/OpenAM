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

package org.forgerock.restlet.ext.oauth2.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.forgerock.restlet.ext.oauth2.OAuth2;
import org.forgerock.restlet.ext.oauth2.OAuth2Utils;
import org.forgerock.restlet.ext.oauth2.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.model.ClientApplication;
import org.forgerock.restlet.ext.oauth2.model.SessionClient;
import org.forgerock.restlet.ext.oauth2.provider.ClientVerifier;
import org.forgerock.restlet.ext.oauth2.provider.OAuth2Client;
import org.forgerock.restlet.ext.oauth2.provider.OAuth2TokenStore;
import org.forgerock.restlet.ext.oauth2.representation.TemplateFactory;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CacheDirective;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
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
import org.restlet.security.User;
import org.restlet.util.Series;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public abstract class AbstractFlow extends ServerResource {

    protected OAuth2.EndpointType endpointType;
    protected OAuth2Client client = null;
    protected User resourceOwner = null;
    protected SessionClient sessionClient = null;

    /**
     * If the {@link AbstractFlow#getCheckedScope} change the requested scope
     * then this value is true.
     */
    private boolean scopeChanged = false;

    private ClientVerifier clientVerifier = null;

    private OAuth2TokenStore tokenStore = null;

    public ClientVerifier getClientVerifier() throws OAuthProblemException {
        if (null == clientVerifier) {
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(),
                    "ClientVerifier is not initialised");
        }
        return clientVerifier;
    }

    public OAuth2TokenStore getTokenStore() throws OAuthProblemException {
        if (null == tokenStore) {
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(),
                    "ClientVerifier is not initialised");
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

    public void setEndpointType(OAuth2.EndpointType endpointType) {
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
                if (this instanceof AuthorizationCodeServerResource) {
                    Redirector dispatcher =
                            OAuth2Utils.ParameterLocation.HTTP_QUERY.getRedirector(getContext(),
                                    exception);
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
                } else if (this instanceof ImplicitGrantServerResource) {
                    Redirector dispatcher =
                            OAuth2Utils.ParameterLocation.HTTP_FRAGMENT.getRedirector(getContext(),
                                    exception);
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
                } else if (this instanceof ErrorServerResource) {
                    Redirector dispatcher = null;// OAuth2Utils.ParameterLocation.HTTP_FRAGMENT.getRedirector(getContext(),
                                                 // exception);
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
                OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Custom.DISPLAY, String.class);
        OAuth2.DisplayType displayType = OAuth2.DisplayType.PAGE;
        if (OAuth2Utils.isNotBlank(display)) {
            try {
                displayType = Enum.valueOf(OAuth2.DisplayType.class, display.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        Representation r = getPage(displayType.getFolder(), templateName, dataModel);
        if (null != r) {
            return r;
        }
        throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(getRequest(),
                "Server can not serve the content of authorization page");
    }

    protected Representation getPage(String display, String templateName, Object dataModel) {
        TemplateRepresentation result = null;
        Object factory = getContext().getAttributes().get(TemplateFactory.class.getName());
        String reference =
                "templates/" + (null != display ? display : OAuth2.DisplayType.PAGE.getFolder())
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
                    OAuth2Utils.getRequestParameter(getRequest(), OAuth2.Params.CLIENT_ID,
                            String.class);
            ClientApplication client = getClientVerifier().findClient(client_id);
            if (null != client) {
                return new OAuth2Client(client);
            } else {
                /*
                 * unauthorized_client The client is not authorized to request
                 * an authorization code using this method.
                 */
                throw OAuthProblemException.OAuthError.INVALID_CLIENT.handle(getRequest());
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
        if (getRequest().getClientInfo().getUser() != null
                && getRequest().getClientInfo().isAuthenticated()) {
            return getRequest().getClientInfo().getUser();
        }
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
        throw OAuthProblemException.OAuthError.ACCESS_DENIED.handle(getRequest(),
                "The authorization server can not authenticate the client.");
    }

    protected Map<String, Object> getDataModel() {
        Map<String, Object> data = new HashMap<String, Object>(getRequest().getAttributes());
        data.put("target", getRequest().getResourceRef().toString());
        return data;
    }

    protected void validateMethod() throws OAuthProblemException {
        switch (endpointType) {
        case AUTHORIZATION_ENDPOINT: {
            if (!(Method.POST.equals(getRequest().getMethod()) || Method.GET.equals(getRequest()
                    .getMethod()))) {
                throw OAuthProblemException.OAuthError.INVALID_REQUEST
                        .handle(getRequest(), "Required Method: GET or POST found: "
                                + getRequest().getMethod().getName());
            }
            break;
        }

        case TOKEN_ENDPOINT: {
            if (!Method.POST.equals(getRequest().getMethod())) {
                throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
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
                throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(),
                        "Invalid Content Type");
            }
        }
        case TOKEN_ENDPOINT: {
            if (!(null == getRequest().getEntity() || getRequest().getEntity() instanceof EmptyRepresentation)
                    && !MediaType.APPLICATION_WWW_FORM.equals(getRequest().getEntity()
                            .getMediaType())) {
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
        if (required != null && required.length > 0) {
            StringBuilder sb = null;
            for (String s : required) {
                if (!getRequest().getAttributes().containsKey(s)) {
                    if (null == sb) {
                        sb = new StringBuilder("Missing parameters: ");
                    }
                    sb.append(s).append(" ");
                }
            }
            if (null != sb) {
                throw OAuthProblemException.OAuthError.INVALID_REQUEST.handle(getRequest(), sb
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
            if (intersect.retainAll(maximumScope)) {
                // TODO Log not allowed scope was requested and was modified
                scopeChanged = true;
                return intersect;
            } else {
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

}
