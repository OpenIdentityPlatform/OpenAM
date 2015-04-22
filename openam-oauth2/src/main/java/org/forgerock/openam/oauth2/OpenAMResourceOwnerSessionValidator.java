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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import static com.sun.identity.shared.DateUtils.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.forgerock.oauth2.core.OAuth2Constants.UrlLocation.*;
import static org.forgerock.oauth2.core.Utils.*;
import static org.forgerock.openidconnect.Client.*;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.oauth2.core.AuthenticationMethod;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.Utils;
import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.OpenIdPrompt;
import org.owasp.esapi.errors.EncodingException;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.ext.servlet.ServletUtils;

/**
 * Validates whether a resource owner has a current authenticated session.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMResourceOwnerSessionValidator implements ResourceOwnerSessionValidator {

    private static final Debug logger = Debug.getInstance("OAuth2Provider");
    private final SSOTokenManager ssoTokenManager;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OpenAMClientDAO clientDAO;
    private final ClientCredentialsReader clientCredentialsReader;

    @Inject
    public OpenAMResourceOwnerSessionValidator(SSOTokenManager ssoTokenManager,
                                               OAuth2ProviderSettingsFactory providerSettingsFactory,
                                               OpenAMClientDAO clientDAO,
                                               ClientCredentialsReader clientCredentialsReader) {
        this.ssoTokenManager = ssoTokenManager;
        this.providerSettingsFactory = providerSettingsFactory;
        this.clientDAO = clientDAO;
        this.clientCredentialsReader = clientCredentialsReader;
    }


    /**
     * {@inheritDoc}
     */
    public ResourceOwner validate(OAuth2Request request) throws ResourceOwnerAuthenticationRequired,
            AccessDeniedException, BadRequestException, InteractionRequiredException, LoginRequiredException,
            ServerException, NotFoundException {

        final OpenIdPrompt openIdPrompt = new OpenIdPrompt(request);

        if (!openIdPrompt.isValid()) {
            String message = "Invalid prompt parameter \"" + openIdPrompt.getOriginalValue() + "\"";
            logger.message(message);
            throw new BadRequestException(message);
        }

        SSOToken token = null;
        try {
            token = ssoTokenManager.createSSOToken(getHttpServletRequest(request.<Request>getRequest()));
        } catch (SSOException e) {
            logger.warning("Error authenticating user against OpenAM: ", e);
        }

        try {
            if (token == null) {
                token = ssoTokenManager.createSSOToken(request.getSession());
            }
        } catch (SSOException e) {
            logger.warning("Error authenticating user against OpenAM: ", e);
        }

        try {
            if (token != null) {
                if (openIdPrompt.containsLogin()) {
                    throw authenticationRequired(request, token);
                }

                try {
                    final long authTime = stringToDate(token.getProperty(ISAuthConstants.AUTH_INSTANT)).getTime();

                    if (isPastMaxAge(getMaxAge(request), authTime)) {
                        alterMaxAge(request);
                        throw authenticationRequired(request, token);
                    }

                    final AMIdentity id = IdUtils.getIdentity(
                            AccessController.doPrivileged(AdminTokenAction.getInstance()),
                            token.getProperty(Constants.UNIVERSAL_IDENTIFIER));

                    final String acrValuesStr = request.getParameter(ACR_VALUES);
                    if (acrValuesStr != null) {
                        setCurrentAcr(token, request, acrValuesStr);
                    }

                    return new OpenAMResourceOwner(token.getProperty(ISAuthConstants.USER_TOKEN), id, authTime);

                } catch (Exception e) { //Exception as chance of MANY exception types here.
                    logger.error("Error authenticating user against OpenAM: ", e);
                    throw new LoginRequiredException();
                }
            } else {
                if (openIdPrompt.containsNone()) {
                    logger.error("Not pre-authenticated and prompt parameter equals none.");
                    if (request.getParameter(OAuth2Constants.Params.RESPONSE_TYPE) != null) {
                        throw new InteractionRequiredException(Utils.isOpenIdConnectFragmentErrorType(splitResponseType(
                                        request.<String>getParameter(RESPONSE_TYPE))) ? FRAGMENT : QUERY);

                    } else {
                        throw new InteractionRequiredException();
                    }
                } else {
                    throw authenticationRequired(request);
                }
            }
        } catch (EncodingException e) {
            throw new AccessDeniedException(e);
        } catch (URISyntaxException e) {
            throw new AccessDeniedException(e);
        }
    }

    /**
     * After we are sent to authN we will come back to authZ, next time make sure
     * we don't fail to max_age again (in case it's only a few seconds),
     * otherwise we'll loop forever and ever...
     */
    private void alterMaxAge(OAuth2Request req) {
        final Request request = req.getRequest();
        Form query = request.getResourceRef().getQueryAsForm();
        Parameter param = query.getFirst(MAX_AGE);
        if (param == null) {
            param = new Parameter(MAX_AGE, CONFIRMED_MAX_AGE);
            query.add(param);
        } else {
            param.setValue(CONFIRMED_MAX_AGE);
        }

        request.getResourceRef().setQuery(query.getQueryString());
    }

    /**
     * maxAge in seconds, authTime in miliseconds, maxAge not in play if set to -1.
     */
    private boolean isPastMaxAge(long maxAge, long authTime) throws SSOException {
        return maxAge > -1 && maxAge <= System.currentTimeMillis() - authTime;
    }

    /**
     * Returns the max_age, set either as a client default (if enabled) or by request in ms, or -1 if not used.
     */
    private long getMaxAge(OAuth2Request request)
            throws URISyntaxException, AccessDeniedException, ServerException,
            NotFoundException, EncodingException, UnauthorizedClientException, ResourceOwnerAuthenticationRequired,
            SSOException, ParseException, ClientAuthenticationFailedException, InvalidClientException, InvalidRequestException {

        final ClientCredentials clientCredentials = clientCredentialsReader.extractCredentials(request, null);

        final String maxAgeStr = request.getParameter(MAX_AGE);
        long maxAge = -1;

        if (maxAgeStr != null) { //max_age is in seconds
            maxAge = Long.valueOf(maxAgeStr);
            if (maxAge < MIN_DEFAULT_MAX_AGE) { //default to the minimum default to avoid infinite redirects
                maxAge = MIN_DEFAULT_MAX_AGE;
            }
        } else { //default_max_age is also in seconds
            Client client = clientDAO.read(clientCredentials.getClientId(), request);
            if (client.getDefaultMaxAgeEnabled()) {
                maxAge = client.getDefaultMaxAge();
            }
        }

        return maxAge * 1000; //return as ms
    }

    /**
     * If the user is already logged in when the OAuth2 request comes in with an acr_values parameter, we
     * look to see if they've already matched one. If they have, we set the acr value on the request.
     */
    private void setCurrentAcr(SSOToken token, OAuth2Request request, String acrValuesStr)
            throws NotFoundException, ServerException, SSOException {
        String serviceUsed = token.getProperty(ISAuthConstants.SERVICE);
        Set<String> acrValues = new HashSet<String>(Arrays.asList(acrValuesStr.split("\\s+")));
        OAuth2ProviderSettings settings = providerSettingsFactory.get(request);
        Map<String, AuthenticationMethod> acrMap = settings.getAcrMapping();
        for (String acr : acrValues) {
            if (acrMap.containsKey(acr)) {
                if (serviceUsed.equals(acrMap.get(acr).getName())) {
                    final Request req = request.getRequest();
                    req.getResourceRef().addQueryParameter(OAuth2Constants.JWTTokenParams.ACR, acr);
                }
            }
        }
    }

    private ResourceOwnerAuthenticationRequired authenticationRequired(OAuth2Request request, SSOToken token)
            throws URISyntaxException, AccessDeniedException, ServerException, NotFoundException, EncodingException {
        try {
            ssoTokenManager.destroyToken(token);
        } catch (SSOException e) {
            logger.error("Error destroying SSOToken: ", e);
        }

        return authenticationRequired(request);
    }

    private ResourceOwnerAuthenticationRequired authenticationRequired(OAuth2Request request)
            throws AccessDeniedException, EncodingException, URISyntaxException, ServerException, NotFoundException {

        final Request req = request.getRequest();
        final String authURL = getAuthURL(getHttpServletRequest(req));
        final URI authURI = new URI(authURL);
        final Reference loginRef = new Reference(authURI);

        final String acrValuesStr = request.getParameter(ACR_VALUES);
        final String realm = request.getParameter(OAuth2Constants.Custom.REALM);
        final String moduleName = request.getParameter(MODULE);
        final String serviceName = request.getParameter(SERVICE);
        final String locale = request.getParameter(LOCALE);

        if (!isEmpty(realm)) {
            loginRef.addQueryParameter(OAuth2Constants.Custom.REALM, realm);
        }
        if (!isEmpty(locale)) {
            loginRef.addQueryParameter(LOCALE, locale);
        }

        // Prefer standard acr_values, then module, then service
        if (!isEmpty(acrValuesStr)) {
            final ACRValue chosen = chooseBestAcrValue(request, acrValuesStr.split("\\s+"));
            if (chosen != null) {
                loginRef.addQueryParameter(chosen.method.getIndexType().toString(), chosen.method.getName());

                // Adjust the GOTO url to indicate which acr value was actually chosen
                req.getResourceRef().addQueryParameter(OAuth2Constants.JWTTokenParams.ACR, chosen.acr);
            }
        } else if (!isEmpty(moduleName)) {
            loginRef.addQueryParameter(MODULE, moduleName);
        } else if (!isEmpty(serviceName)) {
            loginRef.addQueryParameter(SERVICE, serviceName);
        }

        removeLoginPrompt(req);

        loginRef.addQueryParameter(GOTO, req.getResourceRef().toString());

        return new ResourceOwnerAuthenticationRequired(loginRef.toUri());
    }

    /**
     * Searches through the supplied 'acr' values to find a matching authentication context configuration service for
     * this OpenID Connect client. If the client is not an OIDC client, or if no match is found, then {@code null} is
     * returned and the default login configuration for the realm will be used. Values will be tried in the order
     * passed, and the first matching value will be chosen.
     *
     * @param request the OAuth2 request that requires authentication.
     * @param acrValues the values of the acr_values parameter, in preference order.
     * @return the matching ACR value, or {@code null} if no match was found.
     */
    private ACRValue chooseBestAcrValue(final OAuth2Request request, final String...acrValues) throws ServerException, NotFoundException {

        final OAuth2ProviderSettings settings = providerSettingsFactory.get(request);

        final Map<String, AuthenticationMethod> mapping = settings.getAcrMapping();
        if (mapping != null) {
            for (String acrValue : acrValues) {
                final AuthenticationMethod method = mapping.get(acrValue);
                if (method instanceof OpenAMAuthenticationMethod) {
                    if (logger.messageEnabled()) {
                        logger.message("Picked ACR value [" + acrValue + "] -> " + method);
                    }
                    return new ACRValue(acrValue, (OpenAMAuthenticationMethod) method);
                }
            }
        }

        if (logger.messageEnabled()) {
            logger.message("No ACR value matched - using default login configuration");
        }
        return null;
    }

    /**
     * Represents an Authentication Context Class Reference (ACR) value. Each ACR has a name and an associated
     * concrete authentication method (AMR) determined from the client configuration.
     */
    private static class ACRValue {
        private final String acr;
        private final OpenAMAuthenticationMethod method;

        private ACRValue(final String acr, final OpenAMAuthenticationMethod method) {
            this.acr = acr;
            this.method = method;
        }
    }

    /**
     * Removes "login" from prompt query parameter.
     *
     * This needs to be done before redirecting the user to login so that an infinite redirect loop is avoided.
     */
    private void removeLoginPrompt(Request req) {
        Form query = req.getResourceRef().getQueryAsForm();
        Parameter param = query.getFirst(PROMPT);
        if (param != null && param.getSecond() != null) {
            String newValue = param.getSecond().toLowerCase().replace(OpenIdPrompt.PROMPT_LOGIN, "").trim();
            param.setSecond(newValue);
        }
        req.getResourceRef().setQuery(query.getQueryString());
    }

    /**
     * Derive full URL for login screen
     */
    private String getAuthURL(HttpServletRequest request) {
        final String uri = request.getRequestURI();
        String deploymentURI = uri;
        int firstSlashIndex = uri.indexOf("/");
        int secondSlashIndex = uri.indexOf("/", firstSlashIndex + 1);
        if (secondSlashIndex != -1) {
            deploymentURI = uri.substring(0, secondSlashIndex);
        }
        final StringBuffer sb = new StringBuffer(100);
        sb.append(request.getScheme()).append("://")
                .append(request.getServerName()).append(":")
                .append(request.getServerPort())
                .append(deploymentURI)
                .append("/UI/Login");
        return sb.toString();
    }

    /**
     * Hide static method call behind an instance method that can be overridden by unit tests.
     */
    HttpServletRequest getHttpServletRequest(Request req) {
        return ServletUtils.getRequest(req);
    }

}
