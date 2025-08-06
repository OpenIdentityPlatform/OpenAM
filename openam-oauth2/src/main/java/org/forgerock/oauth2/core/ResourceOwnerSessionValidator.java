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
<<<<<<< HEAD
 * Copyright 2014-2016 ForgeRock AS.
=======
 * Copyright 2014 ForgeRock AS.
 * Portions Copyrighted 2019 Open Source Solution Technology Corporation
>>>>>>> 675e7a8ddd... Issue #42  acr_values not working if the user is login in more than one chain (#101)
* Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.oauth2.core;

import static com.sun.identity.shared.DateUtils.stringToDate;
import static org.forgerock.oauth2.core.Utils.isEmpty;
import static org.forgerock.oauth2.core.Utils.splitResponseType;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.DeviceCode.USER_CODE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.UrlLocation.FRAGMENT;
import static org.forgerock.openam.oauth2.OAuth2Constants.UrlLocation.QUERY;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.openidconnect.Client.CONFIRMED_MAX_AGE;
import static org.forgerock.openidconnect.Client.MIN_DEFAULT_MAX_AGE;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.InvalidClientAuthZHeaderException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.core.DNWrapper;
import org.forgerock.openam.oauth2.ClientCredentials;
import org.forgerock.openam.oauth2.ClientCredentialsReader;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OpenAMAuthenticationMethod;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.ClientDAO;
import org.forgerock.openidconnect.OpenIdPrompt;
import org.forgerock.util.annotations.VisibleForTesting;
import org.owasp.esapi.errors.EncodingException;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.forgerock.openam.rest.jakarta.servlet.ServletUtils;

/**
 * Validates whether a resource owner has a current authenticated session.
 *
 * @since 12.0.0
 */
@Singleton
public class ResourceOwnerSessionValidator {

    private static final Debug logger = Debug.getInstance("OAuth2Provider");
    private static final String UNMATCHED_ACR_VALUE = "0";
    private final SSOTokenManager ssoTokenManager;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ClientDAO clientDAO;
    private final ClientCredentialsReader clientCredentialsReader;
    private RealmNormaliser realmNormaliser = new RealmNormaliser();
    private DNWrapper dnWrapper;

    @Inject
    public ResourceOwnerSessionValidator(DNWrapper dnWrapper, SSOTokenManager ssoTokenManager,
            OAuth2ProviderSettingsFactory providerSettingsFactory, ClientDAO clientDAO,
            ClientCredentialsReader clientCredentialsReader) {
        this.ssoTokenManager = ssoTokenManager;
        this.providerSettingsFactory = providerSettingsFactory;
        this.clientDAO = clientDAO;
        this.clientCredentialsReader = clientCredentialsReader;
        this.dnWrapper = dnWrapper;
    }

    /**
     * Checks if the request contains valid resource owner session.
     *
     * @param request The OAuth2 request.
     * @return The ResourceOwner.
     * @throws ResourceOwnerAuthenticationRequired If the resource owner needs to authenticate before the authorize
     *          request can be allowed.
     * @throws AccessDeniedException If resource owner authentication fails.
     * @throws BadRequestException If the request is malformed.
     * @throws InteractionRequiredException If the OpenID Connect prompt parameter enforces that the resource owner
     *          is not asked to authenticate, but the resource owner does not have a current authenticated session.
     * @throws LoginRequiredException If authenticating the resource owner fails.
     * @throws ServerException If the server is misconfigured.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
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

        SSOToken token = getResourceOwnerSession(request);
        try {
            if (token != null && ssoTokenManager.isValidToken(token)) {
                try {
                    // As the organization in the token is stored in lowercase, we need to lower case the auth2realm
                    String auth2Realm = dnWrapper.orgNameToDN(
                            realmNormaliser.normalise((String) request.getParameter("realm"))).toLowerCase();
                    String tokenRealm = token.getProperty("Organization");

                    // auth2Realm can't be null as we would have an error earlier
                    if (!auth2Realm.equals(tokenRealm)){
                        throw authenticationRequired(request);
                    }
                } catch (SSOException e) {
                    throw new AccessDeniedException(e);
                } catch (org.forgerock.json.resource.NotFoundException e) {
                    throw new NotFoundException(e.getMessage());
                }

                if (openIdPrompt.containsLogin()) {
                    throw authenticationRequired(request, token);
                }

                final String acrValuesStr = request.getParameter(ACR_VALUES);
                if (acrValuesStr != null) {
                    setCurrentAcr(token, request, acrValuesStr);
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

                    return new ResourceOwner(id.getName(), id, authTime);

                } catch (Exception e) { //Exception as chance of MANY exception types here.
                    logger.error("Error authenticating user against OpenAM: ", e);
                    throw new LoginRequiredException();
                }
            } else if (OAuth2Constants.TokenEndpoint.PASSWORD.equals(request.getParameter(GRANT_TYPE))
                    || OAuth2Constants.TokenEndpoint.CLIENT_CREDENTIALS.equals(request.getParameter(GRANT_TYPE))) {
                // If we're doing password grant type, the SSOToken will have been created and deleted again within
                // OpenAMResourceOwnerAuthenticator. The request will not have a session, and so the token will have
                // been null from the attempted creation in L148.
                return getResourceOwner(request.getToken(AccessToken.class));
            } else {
                if (openIdPrompt.containsNone()) {
                    logger.error("Not pre-authenticated and prompt parameter equals none.");
                    if (request.getParameter(OAuth2Constants.Params.RESPONSE_TYPE) != null) {
                        throw new InteractionRequiredException(Utils.isOpenIdConnectFragmentErrorType(splitResponseType(
                                request.<String>getParameter(RESPONSE_TYPE))) ? FRAGMENT : QUERY);

                    } else {
                        throw new InteractionRequiredException();
                    }
                } else if (!isRefreshToken(request)) {
                    throw authenticationRequired(request);
                } else {
                    return getResourceOwner(request.getToken(RefreshToken.class));
                }
            }
        } catch (SSOException | UnsupportedEncodingException | URISyntaxException e) {
            throw new AccessDeniedException(e);
        }
    }

    /**
     * Gets the resource owner's session from the OAuth2 request.
     *
     * @param request The OAuth2 request.
     * @return The resource owner's {@code SSOToken}.
     */
    public SSOToken getResourceOwnerSession(OAuth2Request request) {
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
        return token;
    }

    private ResourceOwner getResourceOwner(IntrospectableToken token) {
        return new ResourceOwner(token.getResourceOwnerId(), IdUtils.getIdentity(token.getResourceOwnerId(),
                token.getRealm()), TimeUnit.SECONDS.toMillis(token.getAuthTimeSeconds()));
    }

    private boolean isRefreshToken(OAuth2Request request) {
        return StringUtils.isEqualTo(request.<String>getParameter(OAuth2Constants.Params.GRANT_TYPE), OAuth2Constants.Params.REFRESH_TOKEN);
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
        return maxAge > -1 && maxAge <= currentTimeMillis() - authTime;
    }

    /**
     * Returns the max_age, set either as a client default (if enabled) or by request in ms, or -1 if not used.
     */
    private long getMaxAge(OAuth2Request request)
            throws URISyntaxException, AccessDeniedException, ServerException,
            NotFoundException, EncodingException, UnauthorizedClientException, ResourceOwnerAuthenticationRequired,
            SSOException, ParseException, InvalidClientAuthZHeaderException, InvalidClientException, InvalidRequestException {

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
            throws NotFoundException, ServerException, SSOException, AccessDeniedException,
            UnsupportedEncodingException, URISyntaxException, ResourceOwnerAuthenticationRequired {
        Set<String> serviceUsedSet = AMAuthUtils.getAuthenticatedServices(token);
        Set<String> acrValues = new HashSet<>(Arrays.asList(acrValuesStr.split("\\s+")));
        OAuth2ProviderSettings settings = providerSettingsFactory.get(request);
        Map<String, AuthenticationMethod> acrMap = settings.getAcrMapping();
        final Request req = request.getRequest();

        String matchedAcr = UNMATCHED_ACR_VALUE;
        for (String serviceUsed : serviceUsedSet) {
            for (String acr : acrValues) {
                if (acrMap.containsKey(acr)) {
                    if (serviceUsed.equals(acrMap.get(acr).getName())) {
                        matchedAcr = acr;
                        break;
                    }
                }
            }
        }
        req.getAttributes().put(OAuth2Constants.JWTTokenParams.ACR, matchedAcr);
    }

    private ResourceOwnerAuthenticationRequired authenticationRequired(OAuth2Request request, SSOToken token)
            throws URISyntaxException, AccessDeniedException, ServerException, NotFoundException,
            UnsupportedEncodingException {
        try {
            ssoTokenManager.destroyToken(token);
        } catch (SSOException e) {
            logger.error("Error destroying SSOToken: ", e);
        }

        return authenticationRequired(request);
    }

    private ResourceOwnerAuthenticationRequired authenticationRequired(OAuth2Request request)
            throws AccessDeniedException, URISyntaxException, ServerException, NotFoundException,
            UnsupportedEncodingException {
        OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        Template loginUrlTemplate = providerSettings.getCustomLoginUrlTemplate();

        removeLoginPrompt(request.<Request>getRequest());

        String gotoUrl = request.<Request>getRequest().getResourceRef().toString();
        if (request.getParameter(USER_CODE) != null) {
            gotoUrl += (gotoUrl.indexOf('?') > -1 ? "&" : "?") + USER_CODE + "=" + request.getParameter(USER_CODE);
        }
        String acrValues = request.getParameter(ACR_VALUES);
        String realm = request.getParameter(OAuth2Constants.Custom.REALM);
        String moduleName = request.getParameter(MODULE);
        String serviceName = request.getParameter(SERVICE);
        String locale = getRequestLocale(request);

        URI loginUrl;
        if (loginUrlTemplate != null) {
            loginUrl = buildCustomLoginUrl(loginUrlTemplate, gotoUrl, acrValues, realm, moduleName, serviceName,
                    locale);
        } else {
            loginUrl = buildDefaultLoginUrl(request, gotoUrl, acrValues, realm, moduleName, serviceName, locale);
        }
        return new ResourceOwnerAuthenticationRequired(loginUrl);
    }

    private String getRequestLocale(OAuth2Request request) {
        final String locale = request.getParameter(LOCALE);
        final String uiLocale = request.getParameter(UI_LOCALES);
        if (!isEmpty(uiLocale)) {
            return uiLocale;
        } else {
            return locale;
        }
    }

    private URI buildDefaultLoginUrl(OAuth2Request request, String gotoUrl, String acrValues, String realm,
            String moduleName, String serviceName, String locale) throws URISyntaxException, ServerException,
            NotFoundException {

        final Request req = request.getRequest();
        final String authURL = getAuthURL(getHttpServletRequest(req));
        final URI authURI = new URI(authURL);
        final Reference loginRef = new Reference(authURI);

        if (!isEmpty(realm)) {
            loginRef.addQueryParameter(OAuth2Constants.Custom.REALM, realm);
        }
        if (!isEmpty(locale)) {
            loginRef.addQueryParameter(LOCALE, locale);
        }

        // Prefer standard acr_values, then module, then service
        if (!isEmpty(acrValues)) {
            final ResourceOwnerSessionValidator.ACRValue chosen = chooseBestAcrValue(request, acrValues.split("\\s+"));
            if (chosen != null) {
                loginRef.addQueryParameter(chosen.method.getIndexType().toString(), chosen.method.getName());
            }
        } else if (!isEmpty(moduleName)) {
            loginRef.addQueryParameter(MODULE, moduleName);
        } else if (!isEmpty(serviceName)) {
            loginRef.addQueryParameter(SERVICE, serviceName);
        }

        loginRef.addQueryParameter(GOTO, gotoUrl);

        return loginRef.toUri();
    }

    private URI buildCustomLoginUrl(Template loginUrlTemplate, String gotoUrl, String acrValues, String realm,
            String moduleName, String serviceName, String locale) throws ServerException, UnsupportedEncodingException {

        Map<String, String> templateData = new HashMap<>();
        templateData.put("goto", URLEncoder.encode(gotoUrl, StandardCharsets.UTF_8.toString()));
        templateData.put("acrValues",
                acrValues != null ? URLEncoder.encode(acrValues, StandardCharsets.UTF_8.toString()) : null);
        templateData.put("realm", realm);
        templateData.put("module", moduleName);
        templateData.put("service", serviceName);
        templateData.put("locale", locale);

        try {
            StringWriter loginUrlWriter = new StringWriter();
            loginUrlTemplate.process(templateData, loginUrlWriter);
            return URI.create(loginUrlWriter.toString());
        } catch (IOException | TemplateException e) {
            logger.error("Failed to template custom login url", e);
            throw new ServerException("Failed to template custom login url");
        }
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
    private ResourceOwnerSessionValidator.ACRValue chooseBestAcrValue(final OAuth2Request request, final String...acrValues) throws ServerException,
            NotFoundException {

        final OAuth2ProviderSettings settings = providerSettingsFactory.get(request);

        final Map<String, AuthenticationMethod> mapping = settings.getAcrMapping();
        if (mapping != null) {
            for (String acrValue : acrValues) {
                final AuthenticationMethod method = mapping.get(acrValue);
                if (method instanceof OpenAMAuthenticationMethod) {
                    if (logger.messageEnabled()) {
                        logger.message("Picked ACR value [" + acrValue + "] -> " + method);
                    }
                    return new ResourceOwnerSessionValidator.ACRValue(acrValue, (OpenAMAuthenticationMethod) method);
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
        if (param != null && param.getValue() != null) {
            String newValue = param.getValue().toLowerCase().replace(OpenIdPrompt.PROMPT_LOGIN, "").trim();
            param.setValue(newValue);
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
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + deploymentURI
                + "/UI/Login";
    }

    /**
     * Hide static method call behind an instance method that can be overridden by unit tests.
     */
    @VisibleForTesting
    HttpServletRequest getHttpServletRequest(Request req) {
        return ServletUtils.getRequest(req);
    }
}
