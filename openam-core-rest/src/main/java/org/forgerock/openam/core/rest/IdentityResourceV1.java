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

import static com.sun.identity.idsvcs.opensso.IdentityServicesImpl.*;
import static org.forgerock.json.resource.ResourceException.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.core.rest.IdentityRestUtils.*;
import static org.forgerock.openam.core.rest.UserAttributeInfo.*;
import static org.forgerock.openam.rest.RestUtils.*;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.promise.Promises.*;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;

import org.apache.commons.lang.RandomStringUtils;
import org.forgerock.guava.common.collect.Sets;
import org.forgerock.guice.core.InjectorHolder;
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
import org.forgerock.openam.services.RestSecurity;
import org.forgerock.openam.services.RestSecurityProvider;
import org.forgerock.openam.services.email.MailServer;
import org.forgerock.openam.services.email.MailServerImpl;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.TimeUtils;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idsvcs.AccessDenied;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.NeedMoreCredentials;
import com.sun.identity.idsvcs.ObjectNotFound;
import com.sun.identity.idsvcs.TokenExpired;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class IdentityResourceV1 implements CollectionResourceProvider {

    private static final String AM_ENCRYPTION_PWD = "am.encryption.pwd";

    private static final String SEND_NOTIF_TAG = "IdentityResource.sendNotification() :: ";
    private static Debug debug = Debug.getInstance("frRest");
    public static final String USER_TYPE = "user";

    public static final String GROUP_TYPE = "group";
    public static final String AGENT_TYPE = "agent";

    // TODO: filters, sorting, paged results.

    private final String objectType;

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
    public static final String OLD_PASSWORD = "olduserpassword";

    private final RestSecurityProvider restSecurityProvider;
    private final Set<UiRolePredicate> uiRolePredicates;
    private final IdentityServicesImpl identityServices;
    private final ConsoleConfigHandler configHandler;

    private final MailServerLoader mailServerLoader;

    private final CoreWrapper coreWrapper;

    /**
     * Creates a backend
     */
    public IdentityResourceV1(String objectType, MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, Set<UiRolePredicate> uiRolePredicates) {
        this(objectType, null, null, mailServerLoader, identityServices, coreWrapper, restSecurityProvider,
                configHandler, uiRolePredicates);
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
    IdentityResourceV1(String objectType, ServiceConfigManager mailmgr, ServiceConfig mailscm,
            MailServerLoader mailServerLoader, IdentityServicesImpl identityServices, CoreWrapper coreWrapper,
            RestSecurityProvider restSecurityProvider, ConsoleConfigHandler configHandler, Set<UiRolePredicate> uiRolePredicates) {
        this.objectType = objectType;
        this.mailmgr = mailmgr;
        this.mailscm = mailscm;
        this.mailServerLoader = mailServerLoader;
        this.identityServices = identityServices;
        this.coreWrapper = coreWrapper;
        this.restSecurityProvider = restSecurityProvider;
        this.configHandler = configHandler;
        this.uiRolePredicates = uiRolePredicates;
    }

    /**
     * Gets the user id from the session provided in the server context
     *
     * @param context Current Server Context
     * @param request Request from client to retrieve id
     */
    private Promise<ActionResponse, ResourceException> idFromSession(final Context context, final ActionRequest request) {

        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        SSOToken ssotok;
        AMIdentity amIdentity;

        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            ssotok = mgr.createSSOToken(getCookieFromServerContext(context));
            amIdentity = new AMIdentity(ssotok);

            // build resource
            result.put("id", amIdentity.getName());
            result.put("realm", com.sun.identity.sm.DNMapper.orgNameToRealmName(amIdentity.getRealm()));
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
    private Promise<ActionResponse, ResourceException> createRegistrationEmail(final Context context,
            final ActionRequest request, final String realm, final RestSecurity restSecurity) {

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
            if (!restSecurity.isSelfRegistration()) {
                if (debug.warningEnabled()) {
                    debug.warning("IdentityResource.createRegistrationEmail(): Self-Registration set to : {}",
                            restSecurity.isSelfRegistration());
                }
                throw new NotSupportedException("Self Registration is not enabled.");
            }

            // Get full deployment URL
            HttpContext header = context.asContext(HttpContext.class);
            StringBuilder deploymentURL = RestUtils.getFullDeploymentURI(header.getPath());

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
            if (StringUtils.isBlank(confURL)) {
                confURLBuilder.append(deploymentURL.append("/json/confirmation/register").toString());
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
        } catch (BadRequestException | NotFoundException be) {
            debug.warning("IdentityResource.createRegistrationEmail: Cannot send email to {}", emailAddress, be);
            return be.asPromise();
        } catch (NotSupportedException nse) {
            debug.error("IdentityResource.createRegistrationEmail: Operation not enabled", nse);
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
     * Will validate confirmationId is correct
     * @param context Current Server Context
     * @param request Request from client to confirm registration
     */
    private Promise<ActionResponse, ResourceException> confirmationIdCheck(final Context context,
            final ActionRequest request, final String realm) {
        final String METHOD = "IdentityResource.confirmationIdCheck";
        final JsonValue jVal = request.getContent();
        String tokenID;
        String confirmationId;
        String email = null;
        String username = null;
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
    public Promise<ActionResponse, ResourceException> actionCollection(final Context context,
            final ActionRequest request) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getResolvedRealm();
        RestSecurity restSecurity = restSecurityProvider.get(realm);

        final String action = request.getAction();
        if (action.equalsIgnoreCase("idFromSession")) {
            return idFromSession(context, request);
        } else if (action.equalsIgnoreCase("register")){
            return createRegistrationEmail(context,request, realm, restSecurity);
        } else if (action.equalsIgnoreCase("confirm")) {
            return confirmationIdCheck(context, request, realm);
        } else if (action.equalsIgnoreCase("anonymousCreate")) {
            return anonymousCreate(context, request, realm, restSecurity);
        } else if (action.equalsIgnoreCase("forgotPassword")) {
            return generateNewPasswordEmail(context, request, realm, restSecurity);
        } else if (action.equalsIgnoreCase("forgotPasswordReset")) {
            return anonymousUpdate(context, request, realm);
        } else { // for now this is the only case coming in, so fail if otherwise
            return new NotSupportedException("Actions are not supported for resource instances").asPromise();
        }
    }

    /**
     * Uses an amAdmin SSOtoken to create an AMIdentity from the UID provided and checks
     * whether the AMIdentity in context is active/inactive
     * @param uid the universal identifier of the user
     * @return true is the user is active;false otherwise
     * @throws NotFoundException invalid SSOToken, invalid UID
     */
    private boolean isUserActive(String uid) throws NotFoundException {
        try {
            AMIdentity userIdentity = new AMIdentity(RestUtils.getToken(), uid);
            if (debug.messageEnabled()) {
                debug.message("IdentityResource.isUserActive() : UID={} isActive={}", uid, userIdentity.isActive());
            }
            return userIdentity.isActive();
        } catch (IdRepoException idre) {
            if (debug.errorEnabled()) {
                debug.error("IdentityResource.isUserActive() : Invalid UID={}", uid , idre);
            }
            throw new NotFoundException("Invalid UID, could not retrieved " + uid, idre);
        } catch (SSOException ssoe){
            if (debug.errorEnabled()) {
                debug.error("IdentityResource.isUserActive() : Invalid SSOToken", ssoe);
            }
            throw new NotFoundException("Invalid SSOToken " + ssoe.getMessage(), ssoe);
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
            if (!restSecurity.isForgotPassword()) {
                if (debug.warningEnabled()) {
                    debug.warning("Forgot Password set to : {}", restSecurity.isForgotPassword());
                }
                throw getException(UNAVAILABLE, "Forgot password is not accessible.");
            }

            // Generate Admin Token
            SSOToken adminToken = getSSOToken(RestUtils.getToken().getTokenID().toString());

            Map<String, Set<String>> searchAttributes = getIdentityServicesAttributes(realm);
            searchAttributes.putAll(getAttributeFromRequest(jsonBody));

            List searchResults = identityServices.search(new CrestQuery("*"), searchAttributes, adminToken);

            if (searchResults.isEmpty()) {
                throw new NotFoundException("User not found");

            } else if (searchResults.size() > 1) {
                throw new ConflictException("Multiple users found");

            } else {
                String username = (String) searchResults.get(0);

                IdentityDetails identityDetails = identityServices.read(username,
                        getIdentityServicesAttributes(realm), adminToken);

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
                StringBuilder deploymentURL = RestUtils.getFullDeploymentURI(header.getPath());

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
                org.forgerock.openam.cts.api.tokens.Token ctsToken = generateToken(email, username, tokenLifeTime,
                        realm);

                // Store token in datastore
                CTSHolder.getCTS().createAsync(ctsToken);

                // Create confirmationId
                String confirmationId = Hash.hash(
                        ctsToken.getTokenId() + username + SystemProperties.get(AM_ENCRYPTION_PWD));

                // Build Confirmation URL
                String confURL = restSecurity.getForgotPasswordConfirmationUrl();
                StringBuilder confURLBuilder = new StringBuilder(100);
                if (confURL == null || confURL.isEmpty()) {
                    confURLBuilder.append(deploymentURL.append("/json/confirmation/forgotPassword").toString());
                } else {
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
        } catch (NotFoundException e) {
            debug.warning("Could not find user", e);
            return e.asPromise();
        } catch (ResourceException re) {
            // Service not available, Neither or both Username/Email provided, User inactive
            debug.warning(re.getMessage(), re);
            return re.asPromise();
        } catch (Exception e) {
            // Intentional - all other errors are considered Internal Error.
            debug.error("Internal error : Failed to send mail", e);
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
     * Perform an anonymous update of a user's password using the provided token.
     *
     * The token must match a token placed in the CTS in order for the request
     * to proceed.
     *
     * @param context Non null
     * @param request Non null
     * @param realm Non null
     */
    private Promise<ActionResponse, ResourceException> anonymousUpdate(final Context context,
            final ActionRequest request, final String realm) {
        final String tokenID;
        String confirmationId;
        String username;
        String nwpassword;
        final JsonValue jVal = request.getContent();

        try{
            tokenID = jVal.get(TOKEN_ID).asString();
            jVal.remove(TOKEN_ID);
            confirmationId = jVal.get(CONFIRMATION_ID).asString();
            jVal.remove(CONFIRMATION_ID);
            username = jVal.get(USERNAME).asString();
            nwpassword =  jVal.get("userpassword").asString();

            if(username == null || username.isEmpty()){
                throw new BadRequestException("username not provided");
            }
            if(nwpassword == null || username.isEmpty()) {
                throw new BadRequestException("new password not provided");
            }

            validateToken(tokenID, realm, username, confirmationId);

            // update Identity
            SSOToken admin = RestUtils.getToken();

            // Update instance with new password value
            return updateInstance(admin, jVal, realm)
                    .thenAsync(new AsyncFunction<ActionResponse, ActionResponse, ResourceException>() {
                        @Override
                        public Promise<ActionResponse, ResourceException> apply(ActionResponse response) {
                            // Only remove the token if the update was successful, errors will be set in the handler.
                            try {
                                // Even though the generated token will eventually timeout, delete it after a successful read
                                // so that the reset password request cannot be made again using the same token.
                                CTSHolder.getCTS().deleteAsync(tokenID);
                            } catch (DeleteFailedException e) {
                                // Catch this rather than letting it stop the process as it is possible that between successfully
                                // reading and deleting, the token has expired.
                                if (debug.messageEnabled()) {
                                    debug.message("Deleting token " + tokenID + " after a successful " +
                                            "read failed due to " + e.getMessage(), e);
                                }
                            } catch (CoreTokenException cte) { // For any unexpected CTS error
                                debug.error("Error performing anonymousUpdate", cte);
                                return new InternalServerErrorException(cte.getMessage(), cte).asPromise();
                            }
                            return newResultPromise(response);
                        }
                    });
        } catch (BadRequestException bre) { // For any malformed request.
            debug.warning("Bad request received for anonymousUpdate " + bre.getMessage());
            return bre.asPromise();
        } catch (ResourceException re) {
            debug.warning("Error performing anonymousUpdate", re);
            return re.asPromise();
        } catch (CoreTokenException cte) { // For any unexpected CTS error
            debug.error("Error performing anonymousUpdate", cte);
            return new InternalServerErrorException(cte).asPromise();
        }
    }

    /**
     * Updates an instance given a JSON object with User Attributes
     * @param admin Token that has administrative privileges
     * @param details Json Value containing details of user identity
     * @return A successful promise if the update was successful
     */
    private Promise<ActionResponse, ResourceException> updateInstance(SSOToken admin, final JsonValue details,
            final String realm) {
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
            IdentityDetails checkIdent = identityServices.read(resourceId, getIdentityServicesAttributes(realm), admin);
            // handle updated resource
            return newResultPromise(newActionResponse(identityDetailsToJsonValue(checkIdent)));

        }  catch (ResourceException re) {
            debug.warning("IdentityResource.updateInstance() :: Cannot UPDATE in realm={} for resourceId={}", realm,
                    resourceId, re);
            return re.asPromise();
        } catch (final Exception e) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE in realm={} for resourceId={}", realm,
                    resourceId, e);
            return new NotFoundException(e.getMessage(), e).asPromise();
        }
    }


    private Promise<ActionResponse, ResourceException> anonymousCreate(final Context context,
            final ActionRequest request, final String realm, RestSecurity restSecurity) {

        final JsonValue jVal = request.getContent();
        String confirmationId;
        String email;

        try{
            if (!restSecurity.isSelfRegistration()) {
                throw new BadRequestException("Self-registration disabled");
            }

            final String tokenID = jVal.get(TOKEN_ID).asString();
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
            return createInstance(admin, jVal, realm)
                    .thenAsync(new AsyncFunction<ActionResponse, ActionResponse, ResourceException>() {
                        @Override
                        public Promise<ActionResponse, ResourceException> apply(ActionResponse response) {
                            // Only remove the token if the create was successful, errors will be set in the handler.
                            try {
                                // Even though the generated token will eventually timeout, delete it after a successful read
                                // so that the completed registration request cannot be made again using the same token.
                                CTSHolder.getCTS().deleteAsync(tokenID);
                            } catch (DeleteFailedException e) {
                                // Catch this rather than letting it stop the process as it is possible that between successfully
                                // reading and deleting, the token has expired.
                                if (debug.messageEnabled()) {
                                    debug.message("IdentityResource.anonymousCreate: Deleting token {} after a" +
                                            " successful read failed.", tokenID, e);
                                }
                            } catch (CoreTokenException cte) { // For any unexpected CTS error
                                debug.error("IdentityResource.anonymousCreate(): CTS Error", cte);
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
        return new NotSupportedException("Actions are not supported for resource instances").asPromise();
    }

    /**
     * Creates an a resource using a privileged token
     * @param admin Token that has administrative privileges
     * @param details resource details that needs to be created
     * @return A successful promise if the create was successful
     */
    private Promise<ActionResponse, ResourceException> createInstance(SSOToken admin, final JsonValue details,
            final String realm) {

        JsonValue jVal = details;
        IdentityDetails identity = jsonValueToIdentityDetails(objectType, jVal, realm);
        final String resourceId = identity.getName();

        return attemptResourceCreation(realm, admin, identity, resourceId)
                .thenAsync(new AsyncFunction<IdentityDetails, ActionResponse, ResourceException>() {
                    @Override
                    public Promise<ActionResponse, ResourceException> apply(IdentityDetails dtls) {
                        if (dtls != null) {
                            debug.message("IdentityResource.createInstance :: Anonymous CREATE in realm={} for resourceId={}",
                                    realm, resourceId);

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

            UserAttributeInfo userAttributeInfo = configHandler.getConfig(realm, UserAttributeInfoBuilder.class);
            enforceWhiteList(context, request.getContent(),
                                            objectType, userAttributeInfo.getValidCreationAttributes());

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

            final String id = resourceId;
            return attemptResourceCreation(realm, admin, identity, resourceId)
                    .thenAsync(new AsyncFunction<IdentityDetails, ResourceResponse, ResourceException>() {
                        @Override
                        public Promise<ResourceResponse, ResourceException> apply(IdentityDetails dtls) {
                            if (dtls != null) {
                                String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
                                debug.message("IdentityResource.createInstance :: CREATE of resourceId={} in realm={} performed by " +
                                        "principalName={}", id, realm, principalName);

                                return newResultPromise(newResourceResponse(id, "0", identityDetailsToJsonValue(dtls)));
                            } else {
                                debug.error("IdentityResource.createInstance :: Identity not found ");
                                return new NotFoundException("Identity not found").asPromise();
                            }
                        }
                    });
        } catch (SSOException e) {
            debug.error("IdentityResource.createInstance() :: failed.", e);
            return new NotFoundException(e.getMessage(), e).asPromise();
        } catch (BadRequestException bre) {
            return bre.asPromise();
        }
    }

    private Promise<IdentityDetails, ResourceException> attemptResourceCreation(String realm, SSOToken admin,
            IdentityDetails identity, String resourceId) {

        try {
            identityServices.create(identity, admin);
            IdentityDetails dtls = identityServices.read(resourceId, getIdentityServicesAttributes(realm), admin);
            if (debug.messageEnabled()) {
                debug.message("IdentityResource.createInstance() :: Created resourceId={} in realm={} by AdminID={}",
                        resourceId, realm, admin.getTokenID());
            }
            return newResultPromise(dtls);
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
        } catch (ResourceException re) {
            debug.warning("IdentityResource.createInstance() :: Cannot CREATE resourceId={}", resourceId, re);
            return re.asPromise();
        } catch (final Exception e) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE resourceId={}", resourceId, e);
            return new NotFoundException(e.getMessage(), e).asPromise();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(final Context context,
            final String resourceId, final DeleteRequest request) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getResolvedRealm();


        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        ResourceResponse resource;
        IdentityDetails dtls;

        try {
            SSOToken admin = getSSOToken(getCookieFromServerContext(context));

            // read to see if resource is available to user
            dtls = identityServices.read(resourceId, getIdentityServicesAttributes(realm), admin);

            // delete the resource
            identityServices.delete(dtls, admin);
            String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
            debug.message("IdentityResource.deleteInstance :: DELETE of resourceId={} in realm={} performed by " +
                    "principalName={}", resourceId, realm, principalName);

            result.put("success", "true");
            resource = newResourceResponse(resourceId, "0", result);
            return newResultPromise(resource);

        } catch (final NeedMoreCredentials ex) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE resourceId={} : User does not have enough" +
                    " privileges.", resourceId, ex);
            return new ForbiddenException(resourceId, ex).asPromise();
        } catch (final ObjectNotFound notFound) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE {} : Resource cannot be found.",
                    resourceId, notFound);
            return new NotFoundException("Resource cannot be found.", notFound).asPromise();
        } catch (final TokenExpired tokenExpired) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE resourceId={} : Unauthorized",
                    resourceId, tokenExpired);
            return new PermanentException(401, "Unauthorized", null).asPromise();
        } catch (final AccessDenied accessDenied) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE resourceId={} : Access denied" ,
                    resourceId, accessDenied);
            return new ForbiddenException(accessDenied).asPromise();
        } catch (final GeneralFailure generalFailure) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE resourceId={} : general failure",
                    resourceId, generalFailure);
            return new BadRequestException(generalFailure.getMessage(), generalFailure).asPromise();
        } catch (ForbiddenException ex) {
            debug.warning("IdentityResource.deleteInstance() :: Cannot DELETE resourceId={}: User does not have " +
                    "enough privileges.", resourceId, ex);
            return new ForbiddenException(resourceId, ex).asPromise();
        } catch (NotFoundException notFound) {
            debug.warning("IdentityResource.deleteInstance() :: Cannot DELETE resourceId={} : Resource cannot be found",
                    resourceId, notFound);
            return new NotFoundException("Resource cannot be found.", notFound).asPromise();
        } catch (ResourceException re) {
            debug.warning("IdentityResource.deleteInstance() :: Cannot DELETE resourceId={} : resource failure",
                    resourceId, re);
            result.put("success", "false");
            resource = newResourceResponse(resourceId, "0", result);
            return newResultPromise(resource);
        } catch (Exception e) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE resourceId={}", resourceId, e);
            result.put("success", "false");
            resource = newResourceResponse(resourceId, "0", result);
            return newResultPromise(resource);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(final Context context,
            final String resourceId, final PatchRequest request) {
        return new NotSupportedException("Patch operations are not supported").asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(final Context context,
            final QueryRequest request, final QueryResourceHandler handler) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getResolvedRealm();

        try {
            SSOToken admin = getSSOToken(getCookieFromServerContext(context));
            // This will only return 1 user..
            // getQueryFilter() is not implemented yet..returns dummy false value
            String queryId = request.getQueryId();
            if (queryId == null || queryId.isEmpty()) {
                queryId = "*";
            }
            List<String> users = identityServices.search(new CrestQuery(queryId),
                                                         getIdentityServicesAttributes(realm), admin);
            String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
            debug.message("IdentityResource.queryCollection :: QUERY performed on realm={}  by principalName={}", realm,
                    principalName);

            for (final String user : users) {
                JsonValue val = new JsonValue(user);
                ResourceResponse resource = newResourceResponse(user, "0", val);
                handler.handleResource(resource);
            }
        } catch (Exception ex) {

        }

        return newResultPromise(newQueryResponse());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(final Context context,
            final String resourceId, final ReadRequest request) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getResolvedRealm();

        IdentityDetails dtls;

        try {
            SSOToken admin = getSSOToken(getCookieFromServerContext(context));
            dtls = identityServices.read(resourceId, getIdentityServicesAttributes(realm), admin);
            String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);
            debug.message("IdentityResource.readInstance :: READ of resourceId={} in realm={} performed by " +
                    "principalName={}", resourceId, realm, principalName);
            return newResultPromise(buildResourceResponse(resourceId, context, dtls));
        } catch (final NeedMoreCredentials needMoreCredentials) {
            debug.error("IdentityResource.readInstance() :: Cannot READ resourceId={} : User does not have enough " +
                            "privileges.", resourceId,  needMoreCredentials);
            return new ForbiddenException("User does not have enough privileges.", needMoreCredentials).asPromise();
        } catch (final ObjectNotFound objectNotFound) {
            debug.error("IdentityResource.readInstance() :: Cannot READ resourceId={} : Resource cannot be found.",
                    resourceId, objectNotFound);
            return new NotFoundException("Resource cannot be found.", objectNotFound).asPromise();
        } catch (final TokenExpired tokenExpired) {
            debug.error("IdentityResource.readInstance() :: Cannot READ resourceId={} : Unauthorized", resourceId,
                    tokenExpired);
            return new PermanentException(401, "Unauthorized", null).asPromise();
        } catch (final AccessDenied accessDenied) {
            debug.error("IdentityResource.readInstance() :: Cannot READ resourceId={} : Access denied",
                    resourceId, accessDenied);
            return new ForbiddenException(accessDenied.getMessage(), accessDenied).asPromise();
        } catch (final GeneralFailure generalFailure) {
            debug.error("IdentityResource.readInstance() :: Cannot READ resourceId={}", resourceId, generalFailure);
            return new BadRequestException(generalFailure.getMessage(), generalFailure).asPromise();
        } catch (final Exception e) {
            debug.error("IdentityResource.readInstance() :: Cannot READ resourceId={}", resourceId, e);
            return new NotFoundException(e.getMessage(), e).asPromise();
        }
    }

    protected ResourceResponse buildResourceResponse(String resourceId, Context context,
            IdentityDetails identityDetails) {
        JsonValue content = addRoleInformation(context, resourceId, identityDetailsToJsonValue(identityDetails));
        return newResourceResponse(resourceId, String.valueOf(content.getObject().hashCode()), content);
    }
    
    /*
     * package private for visibility to IdentityResourceV2.
     */
    JsonValue addRoleInformation(Context context, String resourceId, JsonValue value) {
        if (authenticatedUserMatchesUserProfile(context, resourceId)) {
            Set<String> roles = Sets.newHashSet();
            for (UiRolePredicate predicate : uiRolePredicates) {
                if (predicate.apply(context)) {
                    roles.add(predicate.getRole());
                }
            }
            value.put("roles", roles);
        }
        return value;
    }

    private boolean authenticatedUserMatchesUserProfile(Context context, String resourceId) {
        try {
            SSOToken ssoToken = SSOTokenManager.getInstance().createSSOToken(getCookieFromServerContext(context));
            String requestRealm =
                    coreWrapper.convertRealmNameToOrgName(context.asContext(RealmContext.class).getResolvedRealm());
            return resourceId.equalsIgnoreCase(ssoToken.getProperty("UserId"))
                    && requestRealm.equalsIgnoreCase(ssoToken.getProperty(Constants.ORGANIZATION));
        } catch (SSOException e) {
            debug.error("IdentityResource::Failed to determine if requesting user is accessing own profile. " +
                    "resourceId={}", resourceId, e);
        }
        return false;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(final Context context,
            final String resourceId, final UpdateRequest request) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getResolvedRealm();

        final JsonValue jsonValue = request.getContent();
        final String rev = request.getRevision();
        IdentityDetails dtls, newDtls;
        ResourceResponse resource;
        try {
            SSOToken admin = getSSOToken(getCookieFromServerContext(context));
            // Retrieve details about user to be updated
            dtls = identityServices.read(resourceId, getIdentityServicesAttributes(realm), admin);

            //check first if the password is modified as part of the update request, so if necessary, the password can
            //be removed from the IdentityDetails object.
            if (!isAdmin(context)) {
                for (String attrName : jsonValue.keys()) {
                    if ("userpassword".equalsIgnoreCase(attrName)) {
                        String newPassword = jsonValue.get(attrName).asString();
                        if (!StringUtils.isBlank(newPassword)) {
                            String oldPassword = RestUtils.getMimeHeaderValue(context, OLD_PASSWORD);
                            if (StringUtils.isBlank(oldPassword)) {
                                throw new BadRequestException("The old password is missing from the request");
                            }
                            //This is an end-user trying to change the password, so let's change the password by
                            //verifying that the provided old password is correct. We also remove the password from the
                            //list of attributes to prevent the administrative password reset via the update call.
                            jsonValue.remove(attrName);
                            IdentityRestUtils.changePassword(context, realm, resourceId, oldPassword, newPassword);
                        }
                        break;
                    }
                }
            }
            newDtls = jsonValueToIdentityDetails(objectType, jsonValue, realm);
            if (newDtls.getName() != null && !resourceId.equalsIgnoreCase(newDtls.getName())) {
                throw new BadRequestException("id in path does not match id in request body");
            }
            newDtls.setName(resourceId);

            // update resource with new details
            identityServices.update(newDtls, admin);
            // read updated identity back to client
            IdentityDetails checkIdent = identityServices.read(dtls.getName(),
                                                                    getIdentityServicesAttributes(realm), admin);
            // handle updated resource
            resource = newResourceResponse(resourceId, "0", identityDetailsToJsonValue(checkIdent));
            return newResultPromise(resource);
        } catch (final ObjectNotFound onf) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={} : Could not find the " +
                            "resource", resourceId, onf);
            return new NotFoundException("Could not find the resource [ " + resourceId + " ] to update",
                    onf).asPromise();
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
        }  catch (NotFoundException e) {
            debug.warning("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={} : Could not find the " +
                    "resource", resourceId, e);
            return new NotFoundException("Could not find the resource [ " + resourceId + " ] to update", e).asPromise();
        } catch (ResourceException re) {
            debug.warning("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={} ", resourceId, re);
            return re.asPromise();
        } catch (SSOException ssoe) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={}", resourceId, ssoe);
            return new ForbiddenException(ssoe).asPromise();
        }catch (final Exception e) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE resourceId={}", resourceId, e);
            return new NotFoundException(e).asPromise();
        }
    }

    private Map<String, Set<String>> getIdentityServicesAttributes(String realm) {
        Map<String, Set<String>> identityServicesAttributes = new HashMap<>();
        identityServicesAttributes.put("objecttype", Collections.singleton(objectType));
        identityServicesAttributes.put("realm", Collections.singleton(realm));
        return identityServicesAttributes;
    }

    private String requestParamEncode(String toEncode) throws UnsupportedEncodingException {
        if (toEncode != null && !toEncode.isEmpty()) {
            return URLEncoder.encode(toEncode, "UTF-8").replace("+", "%20");
        } else {
            return toEncode;
        }
    }





}
