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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest;

import static com.sun.identity.idsvcs.opensso.IdentityServicesImpl.asMap;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.*;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.core.rest.IdentityRestUtils.*;
import static org.forgerock.openam.core.rest.UserAttributeInfo.*;
import static org.forgerock.openam.rest.RestUtils.*;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.mail.MessagingException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idsvcs.AccessDenied;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.NeedMoreCredentials;
import com.sun.identity.idsvcs.ObjectNotFound;
import com.sun.identity.idsvcs.TokenExpired;
import com.sun.identity.idsvcs.opensso.GeneralAccessDeniedError;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;
import org.apache.commons.lang.RandomStringUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.forgerockrest.utils.MailServerLoader;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.security.whitelist.ValidGotoUrlExtractor;
import org.forgerock.openam.services.RestSecurity;
import org.forgerock.openam.services.RestSecurityProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.services.email.MailServer;
import org.forgerock.openam.services.email.MailServerImpl;
import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.TimeUtils;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class IdentityResourceV2 implements CollectionResourceProvider {

    private static final String AM_ENCRYPTION_PWD = "am.encryption.pwd";

    private static final String SEND_NOTIF_TAG = "IdentityResource.sendNotification() :: ";
    private static Debug debug = Debug.getInstance("frRest");

    public static final String USER_TYPE = "user";
    public static final String GROUP_TYPE = "group";
    public static final String AGENT_TYPE = "agent";

    private final static RedirectUrlValidator<String> URL_VALIDATOR =
            new RedirectUrlValidator<String>(ValidGotoUrlExtractor.getInstance());

    // TODO: filters, sorting, paged results.

    private final String objectType;
    private final RestSecurityProvider restSecurityProvider;

    private ServiceConfigManager mailmgr;
    private ServiceConfig mailscm;
    private Map<String, HashSet<String>> mailattrs;

    final static String MAIL_IMPL_CLASS = "forgerockMailServerImplClassName";
    final static String MAIL_SUBJECT = "forgerockEmailServiceSMTPSubject";
    final static String MAIL_MESSAGE = "forgerockEmailServiceSMTPMessage";

    final static String UNIVERSAL_ID = "universalid";
    final static String MAIL = "mail";
    final static String UNIVERSAL_ID_ABBREV = "uid";
    final static String USERNAME = "username";
    final static String EMAIL = "email";
    final static String TOKEN_ID = "tokenId";
    final static String CONFIRMATION_ID = "confirmationId";
    final static String CURRENT_PASSWORD = "currentpassword";
    final static String USER_PASSWORD = "userpassword";

    private final MailServerLoader mailServerLoader;

    private final IdentityResourceV1 identityResourceV1;

    private final IdentityServicesImpl identityServices;
    private final BaseURLProviderFactory baseURLProviderFactory;
    private final ConsoleConfigHandler configHandler;

    /**
     * Creates a backend
     */
    public IdentityResourceV2(String userType, MailServerLoader mailServerLoader, IdentityServicesImpl identityServices,
            CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider, ConsoleConfigHandler configHandler,
            BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        this(userType, null, null, mailServerLoader, identityServices, coreWrapper, restSecurityProvider,
                configHandler, baseURLProviderFactory, uiRolePredicates);
    }

    /**
     * Enum to lazy init the CTSPersistentStore variable in a thread safe manner.
     */
    private enum CTSHolder {
        INSTANCE;

        private final CTSPersistentStore cts;

        private CTSHolder() {
            cts = InjectorHolder.getInstance(CTSPersistentStore.class);
        }

        static CTSPersistentStore getCTS() {
            return INSTANCE.cts;
        }
    }

    // Constructor used for testing...
    IdentityResourceV2(String userType, ServiceConfigManager mailmgr, ServiceConfig mailscm,
            MailServerLoader mailServerLoader, IdentityServicesImpl identityServices, CoreWrapper coreWrapper,
            RestSecurityProvider restSecurityProvider, ConsoleConfigHandler configHandler,
            BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        this.objectType = userType;
        this.mailmgr = mailmgr;
        this.mailscm = mailscm;
        this.mailServerLoader = mailServerLoader;
        this.restSecurityProvider = restSecurityProvider;
        this.configHandler = configHandler;
        this.identityResourceV1 = new IdentityResourceV1(userType, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, uiRolePredicates);
        this.identityServices = identityServices;
        this.baseURLProviderFactory = baseURLProviderFactory;
    }

    private IdentityServicesImpl getIdentityServices() {
        return identityServices;
    }

    /*
     * package private for access by UserIdentityResourceV3
     */
    JsonValue addRoleInformation(Context context, String resourceId, JsonValue value) {
        return identityResourceV1.addRoleInformation(context, resourceId, value);
    }

    /**
     * Gets the user id from the session provided in the server context
     *
     * @param context Current Server Context
     */
    private Promise<ActionResponse, ResourceException> idFromSession(final Context context) {

        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        SSOToken ssotok;
        AMIdentity amIdentity;

        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            ssotok = mgr.createSSOToken(getCookieFromServerContext(context));
            amIdentity = new AMIdentity(ssotok);

            // build resource
            result.put("id", amIdentity.getName());
            result.put("realm", getRelativeRealmFromSession(context, amIdentity));
            result.put("dn", amIdentity.getUniversalId());
            result.put("successURL", ssotok.getProperty(ISAuthConstants.SUCCESS_URL,false));
            result.put("fullLoginURL", ssotok.getProperty(ISAuthConstants.FULL_LOGIN_URL,false));
            if (debug.messageEnabled()) {
                debug.message("IdentityResource.idFromSession() :: Retrieved ID for user={}", amIdentity.getName());
            }
            return newResultPromise(newActionResponse(result));

        } catch (SSOException e) {
            debug.error("IdentityResource.idFromSession() :: Cannot retrieve SSO Token", e);
            return new ForbiddenException("SSO Token cannot be retrieved.", e).asPromise();
        } catch (IdRepoException ex) {
            debug.error("IdentityResource.idFromSession() :: Cannot retrieve user from IdRepo", ex);
            return new ForbiddenException("Cannot retrieve id from session.", ex).asPromise();
        }
    }

    private String getRelativeRealmFromSession(Context context, AMIdentity amIdentity) {
        RealmContext realmContext = context.asContext(RealmContext.class);

        String sessionRealm = com.sun.identity.sm.DNMapper.orgNameToRealmName(amIdentity.getRealm());
        String baseRealm = realmContext.getDnsAliasRealm();
        if (sessionRealm.startsWith(baseRealm)) {
            String realm = sessionRealm.substring(baseRealm.length());
            if (!realm.startsWith("/")) {
                realm = "/" + realm;
            }
            return realm;
        }

        return sessionRealm;
    }

    /**
     * Generates a secure hash to use as token ID
     * @param resource string that will be used to create random hash
     * @return random string
     */
    static private String generateTokenID(String resource) {
        if (StringUtils.isBlank(resource)) {
            return null;
        }
        return Hash.hash(resource + RandomStringUtils.randomAlphanumeric(32));
    }

    /**
     * Generates a CTS REST Token, including realm information in its {@code CoreTokenField.STRING_ONE} field.
     *
     * @param resource The resource for which the tokenID will be generated
     * @param userId The user's ID, associated with the token
     * @param tokenLifeTimeSeconds Length of time from now in second for the token to remain valid
     * @param realmName The name of the realm in which this token is valid
     * @return the generated CTS REST token
     */
    private org.forgerock.openam.cts.api.tokens.Token generateToken(String resource, String userId,
            Long tokenLifeTimeSeconds, String realmName) {
        Calendar ttl = getCalendarInstance();
        org.forgerock.openam.cts.api.tokens.Token ctsToken = new org.forgerock.openam.cts.api.tokens.Token(
                generateTokenID(resource), TokenType.REST);
        if (!StringUtils.isBlank(userId)) {
            ctsToken.setUserId(userId);
        }
        ctsToken.setAttribute(CoreTokenField.STRING_ONE, realmName);
        ttl.setTimeInMillis(ttl.getTimeInMillis() + (tokenLifeTimeSeconds*1000));
        ctsToken.setExpiryTimestamp(ttl);
        return ctsToken;
    }

    /**
     * This method will create a confirmation email that contains a {@link org.forgerock.openam.cts.api.tokens.Token},
     * confirmationId and email that was provided in the request.
     * @param context Current Server Context
     * @param request Request from client to retrieve id
     */
    private Promise<ActionResponse, ResourceException> createRegistrationEmail(final Context context, final ActionRequest request, final String realm,
            final RestSecurity restSecurity) {

        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        final JsonValue jVal = request.getContent();
        String emailAddress = null;
        String confirmationLink;
        String tokenID;

        try {

            if (restSecurity == null) {
                if (debug.warningEnabled()) {
                    debug.warning("IdentityResource.createRegistrationEmail(): Rest Security not created. " +
                            "restSecurity={}", restSecurity);
                }
                throw new NotFoundException("Rest Security Service not created" );
            }
            if (!restSecurity.isSelfServiceRestEndpointEnabled()) {
                if (debug.warningEnabled()) {
                    debug.warning("IdentityResource.createRegistrationEmail(): Self-Registration set to : {}",
                            restSecurity.isSelfServiceRestEndpointEnabled());
                }
                throw new NotSupportedException("Legacy Self Service REST Endpoint is not enabled.");
            }
            if (!restSecurity.isSelfRegistration()) {
                if (debug.warningEnabled()) {
                    debug.warning("IdentityResource.createRegistrationEmail(): Self-Registration set to : {}",
                            restSecurity.isSelfRegistration());
                }
                throw new NotSupportedException("Self Registration is not enabled.");
            }

            // Get full deployment URL
            HttpContext header = context.asContext(HttpContext.class);
            String baseURL = baseURLProviderFactory.get(realm).getRootURL(header);

            // Get the email address provided from registration page
            emailAddress = jVal.get(EMAIL).asString();
            if (StringUtils.isBlank(emailAddress)){
                throw new BadRequestException("Email not provided");
            }

            String subject = jVal.get("subject").asString();
            String message = jVal.get("message").asString();

            // Retrieve email registration token life time
            Long tokenLifeTime = restSecurity.getSelfRegTLT();

            // Create CTS Token
            org.forgerock.openam.cts.api.tokens.Token ctsToken = generateToken(emailAddress, "anonymous",
                    tokenLifeTime, realm);

            // Store token in datastore
            CTSHolder.getCTS().createAsync(ctsToken);
            tokenID = ctsToken.getTokenId();

            // Create confirmationId
            String confirmationId = Hash.hash(tokenID + emailAddress + SystemProperties.get(AM_ENCRYPTION_PWD));

            // Build Confirmation URL
            String confURL = restSecurity.getSelfRegistrationConfirmationUrl();
            StringBuilder confURLBuilder = new StringBuilder(100);
            if (StringUtils.isEmpty(confURL)) {
                confURLBuilder.append(baseURL).append("/json/confirmation/register");
            } else if(confURL.startsWith("/")) {
                confURLBuilder.append(baseURL).append(confURL);
            } else {
                confURLBuilder.append(confURL);
            }

            confirmationLink = confURLBuilder.append("?confirmationId=").append(requestParamEncode(confirmationId))
                    .append("&email=").append(requestParamEncode(emailAddress))
                    .append("&tokenId=").append(requestParamEncode(tokenID))
                    .append("&realm=").append(realm)
                    .toString();

            // Send Registration
            sendNotification(emailAddress, subject, message, realm, confirmationLink);

            if (debug.messageEnabled()) {
                debug.message("IdentityResource.createRegistrationEmail() :: Sent notification to={} with subject={}. "
                                + "In realm={} for token ID={}", emailAddress, subject, realm, tokenID);
            }

            return newResultPromise(newActionResponse(result));
        } catch (BadRequestException be) {
            debug.warning("IdentityResource.createRegistrationEmail: Cannot send email to {}", emailAddress, be);
            return be.asPromise();
        } catch (NotFoundException nfe) {
            debug.warning("IdentityResource.createRegistrationEmail: Cannot send email to {}", emailAddress, nfe);
            return nfe.asPromise();
        } catch (NotSupportedException nse) {
            if (debug.warningEnabled()) {
                debug.warning("IdentityResource.createRegistrationEmail(): Operation not enabled. email={}",
                        emailAddress, nse);
            }
            return nse.asPromise();
        } catch (Exception e) {
            debug.error("IdentityResource.createRegistrationEmail: Cannot send email to {}", emailAddress,  e);
            return new NotFoundException("Email not sent").asPromise();
        }
    }

    /**
     * Sends email notification to end user
     * @param to Resource receiving notification
     * @param subject Notification subject
     * @param message Notification Message
     * @param confirmationLink Confirmation Link to be sent
     * @throws Exception when message cannot be sent
     */
    private void sendNotification(String to, String subject, String message,
                                  String realm, String confirmationLink) throws ResourceException {

        try {
            mailmgr = new ServiceConfigManager(RestUtils.getToken(),
                    MailServerImpl.SERVICE_NAME, MailServerImpl.SERVICE_VERSION);
            mailscm = mailmgr.getOrganizationConfig(realm,null);
            mailattrs = mailscm.getAttributes();

        } catch (SMSException smse) {
            if (debug.errorEnabled()) {
                debug.error("{} :: Cannot create service {}", SEND_NOTIF_TAG, MailServerImpl.SERVICE_NAME, smse);
            }
            throw new InternalServerErrorException("Cannot create the service: " + MailServerImpl.SERVICE_NAME, smse);

        } catch (SSOException ssoe) {
            if (debug.errorEnabled()) {
                debug.error("{} :: Invalid SSOToken ", SEND_NOTIF_TAG, ssoe);
            }
            throw new InternalServerErrorException("Cannot create the service: " + MailServerImpl.SERVICE_NAME, ssoe);
        }

        if (mailattrs == null || mailattrs.isEmpty()) {
            if (debug.errorEnabled()) {
                debug.error("{} :: no attrs set {}", SEND_NOTIF_TAG, mailattrs);
            }
            throw new NotFoundException("No service Config Manager found for realm " + realm);
        }

        // Get MailServer Implementation class
        String attr = mailattrs.get(MAIL_IMPL_CLASS).iterator().next();
        MailServer mailServer;
        try {
            mailServer = mailServerLoader.load(attr, realm);
        } catch (IllegalStateException e) {
            debug.error("{} :: Failed to load mail server implementation: {}", SEND_NOTIF_TAG, attr, e);
            throw new InternalServerErrorException("Failed to load mail server implementation: " + attr, e);
        }

        try {
            // Check if subject has not  been included
            if (StringUtils.isBlank(subject)){
                // Use default email service subject
                subject = mailattrs.get(MAIL_SUBJECT).iterator().next();
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("{} no subject found ", SEND_NOTIF_TAG, e);
            }
            subject = "";
        }
        try {
            // Check if Custom Message has been included
            if (StringUtils.isBlank(message)){
                // Use default email service message
                message = mailattrs.get(MAIL_MESSAGE).iterator().next();
            }
            message = message + System.getProperty("line.separator") + confirmationLink;
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("{} no message found", SEND_NOTIF_TAG , e);
            }
            message = confirmationLink;
        }

        // Send the emails via the implementation class
        try {
            mailServer.sendEmail(to, subject, message);
        } catch (MessagingException e) {
            if (debug.errorEnabled()) {
                debug.error("{} Failed to send mail", SEND_NOTIF_TAG, e);
            }
            throw new InternalServerErrorException("Failed to send mail", e);
        }
    }

    /**
     * Validates the current goto against the list of allowed gotos, and returns either the allowed
     * goto as sent in, or the server's default goto value.
     *
     * @param context Current Server Context
     * @param request Request from client to confirm registration
     */
    /* package private for access by UserIdentityResourceV3
     */
    Promise<ActionResponse, ResourceException> validateGoto(final Context context,
            final ActionRequest request) {

        final JsonValue jVal = request.getContent();
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));

        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            SSOToken ssoToken = mgr.createSSOToken(getCookieFromServerContext(context));

            String gotoURL = URL_VALIDATOR.getRedirectUrl(ssoToken.getProperty(ISAuthConstants.ORGANIZATION),
                    URL_VALIDATOR.getValueFromJson(jVal, RedirectUrlValidator.GOTO),
                    ssoToken.getProperty("successURL"));

            result.put("successURL", gotoURL);
            return newResultPromise(newActionResponse(result));
        } catch (SSOException ssoe){
            if (debug.errorEnabled()) {
                debug.error("IdentityResource.validateGoto() :: Invalid SSOToken.", ssoe);
            }
            return new ForbiddenException(ssoe.getMessage(), ssoe).asPromise();
        }
    }

    /**
     * Will validate confirmationId is correct
     * @param request Request from client to confirm registration
     */
    private Promise<ActionResponse, ResourceException> confirmationIdCheck(final ActionRequest request,
            final String realm) {
        final String METHOD = "IdentityResource.confirmationIdCheck";
        final JsonValue jVal = request.getContent();
        String tokenID = "";
        String confirmationId;
        String email;
        String username;
        //email or username value used to create confirmationId
        String hashComponent = null;
        String hashComponentAttr = null;
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));

        try{
            tokenID = jVal.get(TOKEN_ID).asString();
            confirmationId = jVal.get(CONFIRMATION_ID).asString();
            email = jVal.get(EMAIL).asString();
            username = jVal.get(USERNAME).asString();

            if (StringUtils.isBlank(confirmationId)) {

                if (debug.errorEnabled()) {
                    debug.error("{} :: Bad Request - confirmationId not found in request.", METHOD);
                }
                throw new BadRequestException("confirmationId not provided");
            }
            if (StringUtils.isBlank(email) && !StringUtils.isBlank(username)) {
                hashComponent = username;
                hashComponentAttr = USERNAME;
            }
            if (!StringUtils.isBlank(email) && StringUtils.isBlank(username)) {
                hashComponent = email;
                hashComponentAttr = EMAIL;
            }
            if (StringUtils.isBlank(hashComponent)) {
                if (debug.errorEnabled()) {
                    debug.error("{} :: Bad Request - hashComponent not found in request.", METHOD);
                }
                throw new BadRequestException("Required information not provided");
            }
            if (StringUtils.isBlank(tokenID)) {
                if (debug.errorEnabled()) {
                    debug.error("{} :: Bad Request - tokenID not found in request.", METHOD);
                }
                throw new BadRequestException("tokenId not provided");
            }

            validateToken(tokenID, realm, hashComponent, confirmationId);

            // build resource
            result.put(hashComponentAttr,hashComponent);
            result.put(TOKEN_ID, tokenID);
            result.put(CONFIRMATION_ID, confirmationId);

            if (debug.messageEnabled()) {
                debug.message("{} :: Confirmed for token '{}' with confirmation '{}'", METHOD, tokenID, confirmationId);
            }

            return newResultPromise(newActionResponse(result));

        } catch (BadRequestException bre) {
            debug.warning("{} :: Cannot confirm registration/forgotPassword for : {}", METHOD, hashComponent,  bre);
            return bre.asPromise();
        } catch (ResourceException re) {
            debug.warning("{} :: Resource error for : {}", METHOD, hashComponent, re);
            return re.asPromise();
        } catch (CoreTokenException cte) {
            debug.error("{} :: CTE error for : {}", METHOD, hashComponent, cte);
            return new InternalServerErrorException(cte).asPromise();
        }
    }

    /**
     * Validates a provided token against a selection of criteria to ensure that it's valid for the given
     * realm. This function is the validation equiv. of
     * {@link IdentityResourceV2#generateToken(String, String, Long, String)}.
     *
     * @param tokenID The token ID to retrieve from the store, against which to perform validation
     * @param realm The realm under which the current request is being made, must match the realm the token was
     *              generated by, not null
     * @param hashComponent The hash component used to created the confirmationId
     * @param confirmationId The confirmationId, not null
     * @throws NotFoundException If the token doesn't exist in the store
     * @throws CoreTokenException If there were unexpected issues communicating with the CTS
     * @throws BadRequestException If the realm or confirmationId were invalid for the token retrieved
     */
    private void validateToken(String tokenID, String realm, String hashComponent, String confirmationId)
            throws ResourceException, CoreTokenException {

        Reject.ifNull(realm);
        Reject.ifNull(confirmationId);

        //check expiry
        org.forgerock.openam.cts.api.tokens.Token ctsToken = CTSHolder.getCTS().read(tokenID);

        if (ctsToken == null || TimeUtils.toUnixTime(ctsToken.getExpiryTimestamp()) < TimeUtils.currentUnixTime()) {
            throw ResourceException.getException(HttpURLConnection.HTTP_GONE, "Cannot find tokenID: " + tokenID);
        }

        // check confirmationId
        if (!confirmationId.equalsIgnoreCase(Hash.hash(tokenID + hashComponent +
                SystemProperties.get(AM_ENCRYPTION_PWD)))) {
            debug.error("IdentityResource.validateToken: Invalid confirmationId : {}", confirmationId);
            throw new BadRequestException("Invalid confirmationId", null);
        }

        //check realm
        if (!realm.equals(ctsToken.getValue(CoreTokenField.STRING_ONE))) {
            debug.error("IdentityResource.validateToken: Invalid realm : {}", realm);
            throw new BadRequestException("Invalid realm", null);
        }

        if (debug.messageEnabled()) {
            debug.message("Validated token with ID={} in realm={} against confirmationId={}", tokenID, realm,
                    confirmationId );
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getResolvedRealm();
        RestSecurity restSecurity = restSecurityProvider.get(realm);

        final String action = request.getAction();
        if (action.equalsIgnoreCase("idFromSession")) {
            return idFromSession(context);
        } else if (action.equalsIgnoreCase("register")){
            return createRegistrationEmail(context,request, realm, restSecurity);
        } else if (action.equalsIgnoreCase("confirm")) {
            return confirmationIdCheck(request, realm);
        } else if (action.equalsIgnoreCase("anonymousCreate")) {
            return anonymousCreate(context, request, realm, restSecurity);
        } else if (action.equalsIgnoreCase("forgotPassword")) {
            return generateNewPasswordEmail(context, request, realm, restSecurity);
        } else if (action.equalsIgnoreCase("forgotPasswordReset")) {
            return identityResourceV1.anonymousUpdate(context, request, realm);
        } else if (action.equalsIgnoreCase("validateGoto")) {
            return validateGoto(context, request);
        } else { // for now this is the only case coming in, so fail if otherwise
            return RestUtils.generateUnsupportedOperation();
        }
    }

    /**
     * Generates the e-mail contents based on the incoming request.
     *
     * Will only send the e-mail if all the following conditions are true:
     *
     * - Forgotten Password service is enabled
     * - User exists
     * - User has an e-mail address in their profile
     * - E-mail service is correctly configured.
     *
     * @param context Non null.
     * @param request Non null.
     * @param realm Used as part of user lookup.
     * @param restSecurity Non null.
     */
    private Promise<ActionResponse, ResourceException> generateNewPasswordEmail(final Context context,
                         final ActionRequest request, final String realm, final RestSecurity restSecurity) {

        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        final JsonValue jsonBody = request.getContent();

        try {

            // Check to make sure forgotPassword enabled
            if (restSecurity == null) {
                if (debug.warningEnabled()) {
                    debug.warning("Rest Security not created. restSecurity={}", restSecurity);
                }
                throw getException(UNAVAILABLE, "Rest Security Service not created");
            }
            if (!restSecurity.isSelfServiceRestEndpointEnabled()) {
                if (debug.warningEnabled()) {
                    debug.warning("Forgot Password set to : {}", restSecurity.isSelfServiceRestEndpointEnabled());
                }
                throw getException(UNAVAILABLE, "Legacy Self Service REST Endpoint is not enabled.");
            }
            if (!restSecurity.isForgotPassword()) {
                if (debug.warningEnabled()) {
                    debug.warning("Forgot Password set to : {}", restSecurity.isForgotPassword());
                }
                throw getException(UNAVAILABLE, "Forgot password is not accessible.");
            }

            // Generate Admin Token
            SSOToken adminToken = getSSOToken(RestUtils.getToken().getTokenID().toString());

            Map<String, Set<String>> searchAttributes = getIdentityServicesAttributes(realm, objectType);
            searchAttributes.putAll(getAttributeFromRequest(jsonBody));

            List<String> searchResults = identityServices.search(new CrestQuery("*"), searchAttributes, adminToken);

            if (searchResults.isEmpty()) {
                throw new NotFoundException("User not found");

            } else if (searchResults.size() > 1) {
                throw new ConflictException("Multiple users found");

            } else {
                String username = searchResults.get(0);

                IdentityDetails identityDetails = identityServices.read(username,
                        getIdentityServicesAttributes(realm, objectType), adminToken);

                String email = null;
                String uid = null;
                for (Map.Entry<String, Set<String>> attribute : asMap(identityDetails.getAttributes()).entrySet()) {
                    String attributeName = attribute.getKey();
                    if (MAIL.equalsIgnoreCase(attributeName)) {
                        if (attribute.getValue() != null && !attribute.getValue().isEmpty()) {
                            email = attribute.getValue().iterator().next();
                        }
                    } else if (UNIVERSAL_ID.equalsIgnoreCase(attributeName)) {
                        if (attribute.getValue() != null && !attribute.getValue().isEmpty()) {
                            uid = attribute.getValue().iterator().next();
                        }
                    }
                }
                // Check to see if user is Active/Inactive
                if (!isUserActive(uid)) {
                    throw new ForbiddenException("Request is forbidden for this user");
                }
                // Check if email is provided
                if (email == null || email.isEmpty()) {
                    throw new BadRequestException("No email provided in profile.");
                }

                // Get full deployment URL
                HttpContext header = context.asContext(HttpContext.class);

                String baseURL = baseURLProviderFactory.get(realm).getRootURL(header);

                String subject = jsonBody.get("subject").asString();
                String message = jsonBody.get("message").asString();

                // Retrieve email registration token life time
                if (restSecurity == null) {
                    if (debug.warningEnabled()) {
                        debug.warning("Rest Security not created. restSecurity={}", restSecurity);
                    }
                    throw new NotFoundException("Rest Security Service not created");
                }
                Long tokenLifeTime = restSecurity.getForgotPassTLT();

                // Generate Token
                org.forgerock.openam.cts.api.tokens.Token ctsToken = generateToken(email, username,
                        tokenLifeTime, realm);

                // Store token in datastore
                CTSHolder.getCTS().createAsync(ctsToken);

                // Create confirmationId
                String confirmationId = Hash.hash(
                        ctsToken.getTokenId() + username + SystemProperties.get(AM_ENCRYPTION_PWD));

                // Build Confirmation URL
                String confURL = restSecurity.getForgotPasswordConfirmationUrl();
                StringBuilder confURLBuilder = new StringBuilder(100);
                if (StringUtils.isEmpty(confURL)) {
                    confURLBuilder.append(baseURL).append("/json/confirmation/forgotPassword");
                } else if(confURL.startsWith("/")) {
                    confURLBuilder.append(baseURL).append(confURL);
                }  else {
                    confURLBuilder.append(confURL);
                }
                String confirmationLink = confURLBuilder.append("?confirmationId=")
                        .append(requestParamEncode(confirmationId))
                        .append("&tokenId=").append(requestParamEncode(ctsToken.getTokenId()))
                        .append("&username=").append(requestParamEncode(username))
                        .append("&realm=").append(realm)
                        .toString();

                // Send Registration
                sendNotification(email, subject, message, realm, confirmationLink);

                String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

                if (debug.messageEnabled()) {
                    debug.message("IdentityResource.generateNewPasswordEmail :: ACTION of generate new password email "
                            + " for username={} in realm={} performed by principalName={}", username, realm,
                            principalName);
                }
            }
            return newResultPromise(newActionResponse(result));
        } catch (ResourceException re) {
            // Service not available, Neither or both Username/Email provided, User inactive
            debug.warning(re.getMessage(), re);
            return re.asPromise();
        } catch (Exception e) {
            // Intentional - all other errors are considered Internal Error.
            debug.error("Internal error", e);
            return new InternalServerErrorException("Failed to send mail", e).asPromise();
        }
    }

    private Map<String, Set<String>> getAttributeFromRequest(JsonValue jsonBody) throws BadRequestException {
        String username = jsonBody.get(USERNAME).asString();
        String email = jsonBody.get(EMAIL).asString();

        if (username != null && email != null) {
            throw new BadRequestException("Both username and email specified - only one allowed in request.");
        }

        if (username != null && !username.isEmpty()) {
            return Collections.singletonMap(UNIVERSAL_ID_ABBREV, Collections.singleton(username));
        }

        if (email != null && !email.isEmpty()) {
            return Collections.singletonMap(MAIL, Collections.singleton(email));
        }

        throw new BadRequestException("Username or email not provided in request");
    }

    /**
     * Updates an instance given a JSON object with User Attributes
     * @param admin Token that has administrative privileges
     * @param details Json Value containing details of user identity
     * @return A successful promise if the update was successful
     */
    private Promise<ActionResponse, ResourceException> updateInstance(SSOToken admin, final JsonValue details, final String realm) {
        JsonValue jVal = details;
        IdentityDetails newDtls;
        String resourceId = jVal.get(USERNAME).asString();

        try {
            newDtls = jsonValueToIdentityDetails(objectType, jVal, realm);

            if (newDtls.getAttributes() == null || newDtls.getAttributes().length < 1) {
                throw new BadRequestException("Illegal arguments: One or more required arguments is null or empty");
            }

            newDtls.setName(resourceId);

            // update resource with new details
            identityServices.update(newDtls, admin);
            debug.message("IdentityResource.updateInstance :: Anonymous UPDATE in realm={} for resourceId={}",
                    realm, resourceId);
            // read updated identity back to client
            IdentityDetails checkIdent = identityServices.read(resourceId,
                    getIdentityServicesAttributes(realm, objectType),
                    admin);
            // handle updated resource
            return newResultPromise(newActionResponse(identityDetailsToJsonValue(checkIdent)));
        } catch (final Exception e) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE in realm={} for resourceId={}", realm,
                    resourceId, e);
            return new NotFoundException(e.getMessage(), e).asPromise();
        }
    }


    private Promise<ActionResponse, ResourceException> anonymousCreate(final Context context,
            final ActionRequest request, final String realm, RestSecurity restSecurity) {

        final JsonValue jVal = request.getContent();
        String tokenID = null;
        String confirmationId;
        String email;

        try{
            if (!restSecurity.isSelfRegistration()) {
                throw new BadRequestException("Self-registration disabled");
            }

            tokenID = jVal.get(TOKEN_ID).asString();
            jVal.remove(TOKEN_ID);
            confirmationId = jVal.get(CONFIRMATION_ID).asString();
            jVal.remove(CONFIRMATION_ID);
            email = jVal.get(EMAIL).asString();

            if (email == null || email.isEmpty()) {
                throw new BadRequestException("Email not provided");
            }
            // Convert to IDRepo Attribute schema
            jVal.put("mail", email);

            if (confirmationId == null || confirmationId.isEmpty()) {
                throw new BadRequestException("confirmationId not provided");
            }

            if (tokenID == null || tokenID.isEmpty()) {
                throw new BadRequestException("tokenId not provided");
            }

            validateToken(tokenID, realm, email, confirmationId);

            // create an Identity
            SSOToken admin = RestUtils.getToken();
            final String finalTokenID = tokenID;

            return createInstance(admin, jVal, realm)
                    .thenAsync(new AsyncFunction<ActionResponse, ActionResponse, ResourceException>() {
                        @Override
                        public Promise<ActionResponse, ResourceException> apply(ActionResponse response) {
                            // Only remove the token if the create was successful, errors will be set in the handler.
                            try {
                                // Even though the generated token will eventually timeout, delete it after a successful read
                                // so that the completed registration request cannot be made again using the same token.
                                CTSHolder.getCTS().deleteAsync(finalTokenID);
                            } catch (DeleteFailedException e) {
                                // Catch this rather than letting it stop the process as it is possible that between successfully
                                // reading and deleting, the token has expired.
                                if (debug.messageEnabled()) {
                                    debug.message("IdentityResource.anonymousCreate: Deleting token " + finalTokenID +
                                            " after a successful read failed due to " + e.getMessage(), e);
                                }
                            } catch (CoreTokenException cte) { // For any unexpected CTS error
                                debug.error("IdentityResource.anonymousCreate(): CTS Error : " + cte.getMessage());
                                return new InternalServerErrorException(cte.getMessage(), cte).asPromise();
                            }
                            return newResultPromise(response);
                        }
                    });
        } catch (BadRequestException bre) {
            debug.warning("IdentityResource.anonymousCreate() :: Invalid Parameter", bre);
            return bre.asPromise();
        } catch (ResourceException re) {
            debug.warning("IdentityResource.anonymousCreate() :: Resource error", re);
            return re.asPromise();
        } catch (CoreTokenException cte) { // For any unexpected CTS error
            debug.error("IdentityResource.anonymousCreate() :: CTS error", cte);
            return new InternalServerErrorException(cte).asPromise();
        } catch (ServiceNotFoundException snfe) {
            // Failure from RestSecurity
            debug.error("IdentityResource.anonymousCreate() :: Internal error", snfe);
            return new InternalServerErrorException(snfe).asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(final Context context,
            final String resourceId, final ActionRequest request) {

        String action = request.getAction();

        if ("changePassword".equalsIgnoreCase(action)) {

            RealmContext realmContext = context.asContext(RealmContext.class);
            final String realm = realmContext.getResolvedRealm();

            JsonValue value = request.getContent();

            try {
                String userPassword = value.get(USER_PASSWORD).asString();
                if (StringUtils.isBlank(userPassword)) {
                    throw new BadRequestException("'" + USER_PASSWORD + "' attribute not set in JSON content.");
                }
                String currentPassword = value.get(CURRENT_PASSWORD).asString();
                if (StringUtils.isBlank(currentPassword)) {
                    throw new BadRequestException("'" + CURRENT_PASSWORD + "' attribute not set in JSON content.");
                }

                IdentityRestUtils.changePassword(context, realm, resourceId, currentPassword, userPassword);

                if (debug.messageEnabled()) {
                    String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
                    debug.message("IdentityResource.actionInstance :: ACTION of change password for " + resourceId
                            + " in realm " + realm + " performed by " + principalName);
                }
                return newResultPromise(newActionResponse(json(object())));
            } catch (ResourceException re) {
                debug.warning("Cannot change password! " + resourceId + ":" + re);
                return re.asPromise();
            }
        } else {
            return new NotSupportedException(action + " not supported for resource instances").asPromise();
        }
    }

    /**
     * Creates an a resource using a privileged token
     * @param admin Token that has administrative privileges
     * @param details resource details that needs to be created
     * @return A successful promise containing the identity details if the create was successful
     */
    private Promise<ActionResponse, ResourceException> createInstance(SSOToken admin, JsonValue details, final String realm) {

        JsonValue jVal = details;
        IdentityDetails identity = jsonValueToIdentityDetails(objectType, jVal, realm);
        final String resourceId = identity.getName();

        return attemptResourceCreation(realm, admin, identity, resourceId)
                .thenAsync(new AsyncFunction<IdentityDetails, ActionResponse, ResourceException>() {
                    @Override
                    public Promise<ActionResponse, ResourceException> apply(IdentityDetails dtls) {
                        if (dtls != null) {
                            debug.message("IdentityResource.createInstance :: Anonymous CREATE in realm={} " +
                                            "for resourceId={}", realm, resourceId);
                            return newResultPromise(newActionResponse(identityDetailsToJsonValue(dtls)));
                        } else {
                            return new NotFoundException(resourceId + " not found").asPromise();
                        }
                    }
                });
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(final Context context,
            final CreateRequest request) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getResolvedRealm();

        try {
            // anyone can create an account add
            SSOToken admin = getSSOToken(getCookieFromServerContext(context));

            final JsonValue jVal = request.getContent();
            String resourceId = request.getNewResourceId();

            IdentityDetails identity = jsonValueToIdentityDetails(objectType, jVal, realm);
            // check to see if request has included resource ID
            if (resourceId != null) {
                if (identity.getName() != null) {
                    if (!resourceId.equalsIgnoreCase(identity.getName())) {
                        ResourceException be = new BadRequestException("id in path does not match id in request body");
                        debug.error("IdentityResource.createInstance() :: Cannot CREATE ", be);
                        return be.asPromise();
                    }
                }
                identity.setName(resourceId);
            } else {
                resourceId = identity.getName();
            }

            UserAttributeInfo userAttributeInfo = configHandler.getConfig(realm, UserAttributeInfoBuilder.class);
            enforceWhiteList(context, request.getContent(), objectType,
                                                            userAttributeInfo.getValidCreationAttributes());

            final String id = resourceId;
            return attemptResourceCreation(realm, admin, identity, resourceId)
                    .thenAsync(new AsyncFunction<IdentityDetails, ResourceResponse, ResourceException>() {
                        @Override
                        public Promise<ResourceResponse, ResourceException> apply(IdentityDetails dtls) {
                            if (dtls != null) {
                                String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
                                debug.message("IdentityResource.createInstance :: CREATE of resourceId={} in realm={} "
                                        + "performed by principalName={}", id, realm, principalName);

                                ResourceResponse resource = newResourceResponse(id, "0", identityDetailsToJsonValue(dtls));
                                return newResultPromise(resource);
                            } else {
                                debug.error("IdentityResource.createInstance() :: Identity not found");
                                return new NotFoundException("Identity not found").asPromise();
                            }
                        }
                    });


        } catch (SSOException e) {
            return new ForbiddenException(e).asPromise();
        } catch (BadRequestException bre) {
            return bre.asPromise();
        }
    }

    private Promise<IdentityDetails, ResourceException> attemptResourceCreation(String realm, SSOToken admin,
            IdentityDetails identity, String resourceId) {

        IdentityDetails dtls = null;

        try {
            // Create the resource
            identityServices.create(identity, admin);
            // Read created resource
            dtls = identityServices.read(resourceId, getIdentityServicesAttributes(realm, objectType), admin);
            if (debug.messageEnabled()) {
                debug.message("IdentityResource.createInstance() :: Created resourceId={} in realm={} by AdminID={}",
                        resourceId, realm, admin.getTokenID());
            }
        } catch (final ObjectNotFound notFound) {
            debug.error("IdentityResource.createInstance() :: Cannot READ resourceId={} : Resource cannot be found.",
                    resourceId, notFound);
            return new NotFoundException("Resource not found.", notFound).asPromise();
        } catch (final TokenExpired tokenExpired) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE resourceId={} : Unauthorized", resourceId,
                    tokenExpired);
            return new PermanentException(401, "Unauthorized", null).asPromise();
        } catch (final NeedMoreCredentials needMoreCredentials) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE resourceId={} : Token is not authorized",
                    resourceId, needMoreCredentials);
            return new ForbiddenException("Token is not authorized", needMoreCredentials).asPromise();
        } catch (final GeneralAccessDeniedError accessDenied) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " + accessDenied);
            return new ForbiddenException().asPromise();
        } catch (GeneralFailure generalFailure) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    generalFailure);
            return new BadRequestException("Resource cannot be created: "
                    + generalFailure.getMessage(), generalFailure).asPromise();
        } catch (AccessDenied accessDenied) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    accessDenied);
            return new ForbiddenException("Token is not authorized: " + accessDenied.getMessage(), accessDenied)
                    .asPromise();
        } catch (ResourceException re) {
            debug.warning("IdentityResource.createInstance() :: Cannot CREATE resourceId={}", resourceId, re);
            return re.asPromise();
        } catch (final Exception e) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE resourceId={}", resourceId, e);
            return new NotFoundException(e.getMessage(), e).asPromise();
        }
        return newResultPromise(dtls);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context, final String resourceId, final DeleteRequest request) {
        return identityResourceV1.deleteInstance(context, resourceId, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(final Context context,
            final String resourceId, final PatchRequest request) {
        return identityResourceV1.patchInstance(context, resourceId, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context,
            final QueryRequest request, final QueryResourceHandler handler) {
        return identityResourceV1.queryCollection(context, request, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(final Context context,
            final String resourceId, final ReadRequest request) {
        return identityResourceV1.readInstance(context, resourceId, request);
    }

    private boolean checkValidPassword(String username, char[] password, String realm) throws BadRequestException {
        if (username == null || password == null) {
            throw new BadRequestException("Invalid Username or Password");
        }
        try {
            Callback[] callbacks = new Callback[2];
            NameCallback nc = new NameCallback("dummy");
            nc.setName(username);
            callbacks[0] = nc;
            PasswordCallback pc = new PasswordCallback("dummy", false);
            pc.setPassword(password);
            callbacks[1] = pc;
            AMIdentityRepository idRepo = new AMIdentityRepository(null, realm);
            return idRepo.authenticate(callbacks);
        } catch (Exception ex) {
            if (debug.messageEnabled()) {
                debug.message("Failed to verify password for username={}", username, ex.getMessage());
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context,
            final String resourceId, final UpdateRequest request) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getResolvedRealm();


        final JsonValue jVal = request.getContent();
        final String rev = request.getRevision();
        IdentityDetails dtls, newDtls;
        ResourceResponse resource;
        try {
            SSOToken token = getSSOToken(getCookieFromServerContext(context));
            // Retrieve details about user to be updated
            dtls = identityServices.read(resourceId, getIdentityServicesAttributes(realm, objectType), token);
            // Continue modifying the identity if read success

            boolean isUpdatingHisOwnPassword = SystemProperties.getAsBoolean(Constants.CASE_SENSITIVE_UUID) ?
                    token.getProperty(ISAuthConstants.USER_ID).equals(resourceId) :
                    token.getProperty(ISAuthConstants.USER_ID).equalsIgnoreCase(resourceId);

            // If the user wants to modify his password, he should use a different action.
            if (isUpdatingHisOwnPassword) {
                for (String key : jVal.keys()) {
                    if (USER_PASSWORD.equalsIgnoreCase(key)) {
                        return new BadRequestException("Cannot update user password via PUT. "
                                + "Use POST with _action=changePassword or _action=forgotPassword.").asPromise();
                    }
                }
            }

            newDtls = jsonValueToIdentityDetails(objectType, jVal, realm);

            if (newDtls.getAttributes() == null || newDtls.getAttributes().length < 1) {
                throw new BadRequestException("Illegal arguments: One or more required arguments is null or empty");
            }

            if (newDtls.getName() != null && !resourceId.equalsIgnoreCase(newDtls.getName())){
                throw new BadRequestException("id in path does not match id in request body");
            }
            newDtls.setName(resourceId);

            UserAttributeInfo userAttributeInfo = configHandler.getConfig(realm, UserAttributeInfoBuilder.class);

            // Handle attribute change when password is required
            // Get restSecurity for this realm
            RestSecurity restSecurity = restSecurityProvider.get(realm);
            // Make sure user is not admin and check to see if we are requiring a password to change any attributes
            Set<String> protectedUserAttributes = new HashSet<>();
            protectedUserAttributes.addAll(restSecurity.getProtectedUserAttributes());
            protectedUserAttributes.addAll(userAttributeInfo.getProtectedUpdateAttributes());

            if (!protectedUserAttributes.isEmpty() && !isAdmin(context)) {
                boolean hasReauthenticated = false;
                for (String protectedAttr : protectedUserAttributes) {
                    JsonValue jValAttr = jVal.get(protectedAttr);
                    if(!jValAttr.isNull()){
                        // If attribute is not available set newAttr variable to empty string for use in comparison
                        String newAttr = (jValAttr.isString()) ? jValAttr.asString() : "";
                        // Get the value of current attribute
                        String currentAttr = "";
                        Map<String, Set<String>> attrs = asMap(dtls.getAttributes());
                        for (Map.Entry<String, Set<String>> attribute : attrs.entrySet()) {
                            String attributeName = attribute.getKey();
                            if(protectedAttr.equalsIgnoreCase(attributeName)){
                                currentAttr = attribute.getValue().iterator().next();
                            }
                        }
                        // Compare newAttr and currentAttr
                        if (!currentAttr.equals(newAttr) && !hasReauthenticated) {
                            // check header to make sure that password is there then check to see if it's correct
                            String strCurrentPass = RestUtils.getMimeHeaderValue(context, CURRENT_PASSWORD);
                            if (strCurrentPass != null && !strCurrentPass.isEmpty() &&
                                    checkValidPassword(resourceId, strCurrentPass.toCharArray(), realm)) {
                                //set a boolean value so we know reauth has been done
                                hasReauthenticated = true;
                                //continue will allow attribute(s) change(s)
                            } else {
                                throw new BadRequestException("Must provide a valid confirmation password to change " +
                                        "protected attribute (" + protectedAttr + ") from '" + currentAttr + "' to '" +
                                        newAttr + "'");
                            }
                        }
                    }
                }
            }

            // update resource with new details
            identityServices.update(newDtls, token);
            String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
            debug.message("IdentityResource.updateInstance :: UPDATE of resourceId={} in realm={} performed " +
                    "by principalName={}", resourceId, realm, principalName);
            // read updated identity back to client
            IdentityDetails checkIdent = identityServices.read(dtls.getName(),
                    getIdentityServicesAttributes(realm, objectType), token);
            return newResultPromise(this.identityResourceV1.buildResourceResponse(resourceId, context, checkIdent));
        } catch (final ObjectNotFound onf) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={} : Could not find the " +
                    "resource", resourceId, onf);
            return new NotFoundException("Could not find the resource [ " + resourceId + " ] to update", onf)
                    .asPromise();
        } catch (final NeedMoreCredentials needMoreCredentials) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={} : Token is not authorized",
                    resourceId, needMoreCredentials);
            return new ForbiddenException("Token is not authorized", needMoreCredentials).asPromise();
        } catch (final TokenExpired tokenExpired) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={} : Unauthorized",
                    resourceId, tokenExpired);
            return new PermanentException(401, "Unauthorized", null).asPromise();
        } catch (final AccessDenied accessDenied) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={} : Access denied",
                    resourceId, accessDenied);
            return new ForbiddenException(accessDenied.getMessage(), accessDenied).asPromise();
        } catch (final GeneralFailure generalFailure) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={}", resourceId, generalFailure);
            return new BadRequestException(generalFailure.getMessage(), generalFailure).asPromise();
        } catch (BadRequestException bre){
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={}", resourceId, bre);
            return bre.asPromise();
        } catch (NotFoundException e) {
            debug.warning("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={} : Could not find the " +
                    "resource", resourceId, e);
            return new NotFoundException("Could not find the resource [ " + resourceId + " ] to update", e)
                    .asPromise();
        } catch (ResourceException re) {
            debug.warning("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={} ", resourceId, re);
            return re.asPromise();
        } catch (final Exception e) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={}", resourceId, e);
            return new NotFoundException(e.getMessage(), e).asPromise();
        }
    }

    private String requestParamEncode(String toEncode) throws UnsupportedEncodingException {
        if (toEncode != null && !toEncode.isEmpty()) {
            return URLEncoder.encode(toEncode, "UTF-8").replace("+", "%20");
        } else {
            return toEncode;
        }
    }

    /**
     * @return the object type
     */
    private String getObjectType() {
        return objectType;
    }
}
