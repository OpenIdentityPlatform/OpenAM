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
 * Copyright 2012-2015 ForgeRock AS. 
 */
package org.forgerock.openam.forgerockrest;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.identity.sm.SMSException;
import com.sun.identity.shared.encode.Hash;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.apache.commons.lang.RandomStringUtils;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idsvcs.AccessDenied;
import com.sun.identity.idsvcs.CreateResponse;
import com.sun.identity.idsvcs.DeleteResponse;
import com.sun.identity.idsvcs.DuplicateObject;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.NeedMoreCredentials;
import com.sun.identity.idsvcs.ObjectNotFound;
import com.sun.identity.idsvcs.Token;
import com.sun.identity.idsvcs.UpdateResponse;
import com.sun.identity.idsvcs.TokenExpired;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;
import org.forgerock.json.fluent.JsonValue;
import static org.forgerock.json.fluent.JsonValue.*;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.*;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import static org.forgerock.openam.forgerockrest.RestUtils.getCookieFromServerContext;
import static org.forgerock.openam.forgerockrest.RestUtils.isAdmin;

import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.guice.InjectorHolder;
import org.forgerock.openam.services.RestSecurity;
import org.forgerock.openam.services.email.MailServer;
import org.forgerock.openam.services.email.MailServerImpl;
import org.forgerock.openam.utils.TimeUtils;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class IdentityResource implements CollectionResourceProvider {
    // TODO: filters, sorting, paged results.

    private static final String AM_ENCRYPTION_PWD = "am.encryption.pwd";

    private final List<Attribute> idSvcsAttrList;
    private String realm;
    private String userType;

    private ServiceConfigManager mailmgr;
    private ServiceConfig mailscm;
    Map<String, HashSet<String>> mailattrs;

    static private final CTSPersistentStore cts = InjectorHolder.getInstance(CTSPersistentStore.class);

    final static String MAIL_IMPL_CLASS = "forgerockMailServerImplClassName";
    final static String MAIL_SUBJECT = "forgerockEmailServiceSMTPSubject";
    final static String MAIL_MESSAGE = "forgerockEmailServiceSMTPMessage";

    final static private String UNIVERSAL_ID = "universalid";
    final static private String MAIL = "mail";
    final static String UNIVERSAL_ID_ABBREV = "uid";
    final static String USERNAME = "username";
    final static String EMAIL = "email";
    final static String TOKEN_ID = "tokenId";
    final static String CONFIRMATION_ID = "confirmationId";
    final static String CURRENT_PASSWORD = "currentpassword";
    private static final String USER_PASSWORD = "userpassword";

    private RestSecurity restSecurity = null;
    /**
     * Creates a backend
     */
    public IdentityResource(String userType, String realm) {
        String[] userval = {userType};
        String[] realmval = {realm};
        this.realm = realm;
        this.userType = userType;
        idSvcsAttrList = new ArrayList();
        idSvcsAttrList.add(new Attribute("objecttype", userval));
        idSvcsAttrList.add(new Attribute("realm", realmval));
        restSecurity = getRestSecurity(realm);
    }

    // Constructor used for testing...
    public IdentityResource(String userType, String realm, ServiceConfigManager mailmgr,
                            ServiceConfig mailscm,RestSecurity restSecurity){
        String[] userval = {userType};
        String[] realmval = {realm};
        this.realm = realm;
        this.userType = userType;
        idSvcsAttrList = new ArrayList();
        idSvcsAttrList.add(new Attribute("objecttype", userval));
        idSvcsAttrList.add(new Attribute("realm", realmval));
        this.mailmgr = mailmgr;
        this.mailscm = mailscm;
    }

    private static final Map<String, RestSecurity> REALM_REST_SECURITY_MAP = new ConcurrentHashMap<String, RestSecurity>();

    /**
     * Gets the user id from the session provided in the server context
     *
     * @param context Current Server Context
     * @param request Request from client to retrieve id
     * @param handler Result handler
     */
    private void idFromSession(final ServerContext context, final ActionRequest request,
                               final ResultHandler<JsonValue> handler) {

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
            handler.handleResult(result);

        } catch (SSOException e) {
            RestDispatcher.debug.error("IdentityResource.idFromSession() :: Cannot retrieve SSO Token: " + e);
            handler.handleError(new ForbiddenException("SSO Token cannot be retrieved.", e));
        } catch (IdRepoException ex) {
            RestDispatcher.debug.error("IdentityResource.idFromSession() :: Cannot retrieve user from IdRepo" + ex);
            handler.handleError(new ForbiddenException("Cannot retrieve id from session.", ex));
        }
    }

    /**
     * Generates a secure hash to use as token ID
     * @param resource string that will be used to create random hash
     * @return random string
     */
    static private String generateTokenID(String resource) {
        if(resource == null || resource.isEmpty()) {
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
        Calendar ttl = Calendar.getInstance();
        org.forgerock.openam.cts.api.tokens.Token ctsToken = new org.forgerock.openam.cts.api.tokens.Token(
                generateTokenID(resource), TokenType.REST);
        if(userId != null && !userId.isEmpty()) {
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
     * @param handler Result handler
     */
    private void createRegistrationEmail(final ServerContext context, final ActionRequest request,
                                         final ResultHandler<JsonValue> handler){


        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        final JsonValue jVal = request.getContent();
        String emailAddress = null;
        String confirmationLink;
        String tokenID;

        try {

            if(restSecurity == null){
                RestDispatcher.debug.warning("IdentityResource.createRegistrationEmail(): " +
                        "Rest Security not created. restSecurity = " + restSecurity);
                throw new NotFoundException("Rest Security Service not created" );
            }
            if(!restSecurity.isSelfRegistration()){
                RestDispatcher.debug.warning("IdentityResource.createRegistrationEmail(): Self-Registration set to :"
                        + restSecurity.isSelfRegistration());
                throw new NotFoundException("Self Registration is not accessible.");
            }
            // Get full deployment URL
            HttpContext header = null;
            header = context.asContext(HttpContext.class);
            StringBuilder deploymentURL = RestUtils.getFullDeploymentURI(header.getPath());

            // Get the email address provided from registration page
            emailAddress = jVal.get(EMAIL).asString();
            if (isNullOrEmpty(emailAddress)) {
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
            cts.create(ctsToken);
            tokenID = ctsToken.getTokenId();
            // Create confirmationId
            String confirmationId = Hash.hash(tokenID + emailAddress + SystemProperties.get(AM_ENCRYPTION_PWD));

            // Build Confirmation URL
            String confURL = restSecurity.getSelfRegistrationConfirmationUrl();
            StringBuilder confURLBuilder = new StringBuilder(100);
            if (isNullOrEmpty(confURL)) {
                confURLBuilder.append(deploymentURL.append("/json/confirmation/register").toString());
            } else {
                confURLBuilder.append(confURL);
            }

            confirmationLink = confURLBuilder.append("?confirmationId=").append(requestParamEncode(confirmationId))
                    .append("&email=").append(requestParamEncode(emailAddress))
                    .append("&tokenId=").append(requestParamEncode(tokenID))
                    .toString();

            if (RestDispatcher.debug.messageEnabled()) {
                RestDispatcher.debug.message("IdentityResource.createRegistrationEmail(): sending confirmationLink of "
                        + confirmationLink);
            }

            // Send Registration
            sendNotification(emailAddress, subject, message, confirmationLink);
            handler.handleResult(result);
        } catch (BadRequestException be) {
            RestDispatcher.debug.error("IdentityResource.createRegistrationEmail: Cannot send email to : " + emailAddress
                    + be.getMessage());
            handler.handleError(be);
        } catch (NotFoundException nfe){
            RestDispatcher.debug.error("IdentityResource.createRegistrationEmail: Cannot send email to : " + emailAddress
                    + nfe.getMessage());
            handler.handleError(nfe);
        } catch (Exception e){
            RestDispatcher.debug.error("IdentityResource.createRegistrationEmail: Cannot send email to : " + emailAddress
                    + e.getMessage());
            handler.handleError(new NotFoundException("Email not sent"));
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
    private void sendNotification(String to, String subject, String message, String confirmationLink) throws Exception{

        try {
            mailmgr = new ServiceConfigManager(RestUtils.getToken(),
                    MailServerImpl.SERVICE_NAME, MailServerImpl.SERVICE_VERSION);
            mailscm = mailmgr.getOrganizationConfig(realm,null);
            mailattrs = mailscm.getAttributes();

        } catch (SMSException smse) {
            RestDispatcher.debug.error("IdentityResource.sendNotification() :: Cannot create service " +
                    MailServerImpl.SERVICE_NAME + smse);
            throw new InternalServerErrorException("Cannot create the service: "+ MailServerImpl.SERVICE_NAME, smse);

        } catch (SSOException ssoe){
            RestDispatcher.debug.error("IdentityResource.sendNotification() :: Invalid SSOToken " + ssoe);
            throw new InternalServerErrorException("Cannot create the service: "+ MailServerImpl.SERVICE_NAME, ssoe);
        }

        if(mailattrs == null || mailattrs.isEmpty()){
            RestDispatcher.debug.error("IdentityResource.sendNotification() :: no attrs set"  + mailattrs );
            throw new NotFoundException("No service Config Manager found for realm " + realm);
        }

        // Get MailServer Implementation class
        String attr = mailattrs.get(MAIL_IMPL_CLASS).iterator().next();
        MailServer mailServer = (MailServer) Class.forName(attr).getDeclaredConstructor(String.class).newInstance(realm);

        try {
            // Check if subject has not  been included
            if (isNullOrEmpty(subject)) {
                // Use default email service subject
                subject = mailattrs.get(MAIL_SUBJECT).iterator().next();
            }
        } catch (Exception e){
            RestDispatcher.debug.warning("IdentityResource.sendNotification() :: no subject found"  + e.getMessage());
            subject = "";
        }
        try {
            // Check if Custom Message has been included
            if (isNullOrEmpty(message)) {
                // Use default email service message
                message = mailattrs.get(MAIL_MESSAGE).iterator().next();
            }
            message = message + System.getProperty("line.separator")  + confirmationLink;
        } catch (Exception e){
            RestDispatcher.debug.warning("IdentityResource.sendNotification() :: no message found"  + e.getMessage());
            message = confirmationLink;
        }
        // Send the emails via the implementation class
        mailServer.sendEmail(to, subject, message);
    }

    /**
     * Will validate confirmationId is correct
     * @param context Current Server Context
     * @param request Request from client to confirm registration
     * @param handler Result handler
     */
    private void confirmRegistration(final ServerContext context, final ActionRequest request,
                                     final ResultHandler<JsonValue> handler, final String realm){
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

            if (isNullOrEmpty(confirmationId)) {
                throw new BadRequestException("confirmationId not provided");
            }
            if (isNullOrEmpty(email) && !isNullOrEmpty(username)) {
                hashComponent = username;
                hashComponentAttr = USERNAME;
            }
            if (!isNullOrEmpty(email) && isNullOrEmpty(username)) {
                hashComponent = email;
                hashComponentAttr = EMAIL;
            }
            if (isNullOrEmpty(hashComponent)) {
                throw new BadRequestException("Required information not provided");
            }
            if (isNullOrEmpty(tokenID)) {
                throw new BadRequestException("tokenId not provided");
            }

            validateToken(tokenID, realm, email, confirmationId);

            // build resource
            result.put(hashComponentAttr,hashComponent);
            result.put(TOKEN_ID, tokenID);
            result.put(CONFIRMATION_ID, confirmationId);
            handler.handleResult(result);

        } catch (BadRequestException be){
            RestDispatcher.debug.error(METHOD + ": Cannot confirm registration/forgotPassword for : " + hashComponent, be);
            handler.handleError(be);
        } catch (Exception e){
            RestDispatcher.debug.error(METHOD + ": Cannot confirm registration/forgotPassword for : " + hashComponent, e);
            handler.handleError(new NotFoundException(e.getMessage()));
        }
    }


    /**
     * Validates a provided token against a selection of criteria to ensure that it's valid for the given
     * realm. This function is the validation equiv. of
     * {@link IdentityResource#generateToken(String, String, Long, String)}.
     *
     * @param tokenID The token ID to retrieve from the store, against which to perform validation
     * @param realm The realm under which the current request is being made, must match the realm the token was
     *              generated by
     * @param hashComponent The hash component used to created the confirmationId
     * @param confirmationId The confirmationId
     * @throws NotFoundException If the token doesn't exist in the store
     * @throws CoreTokenException If there were unexpected issues communicating with the CTS
     * @throws BadRequestException If the realm or confirmationId were invalid for the token retrieved
     */
    private void validateToken(String tokenID, String realm, String hashComponent, String confirmationId)
            throws NotFoundException, CoreTokenException, BadRequestException {

        //check expiry
        org.forgerock.openam.cts.api.tokens.Token ctsToken = cts.read(tokenID);

        if (ctsToken == null || TimeUtils.toUnixTime(ctsToken.getExpiryTimestamp()) < TimeUtils.currentUnixTime()) {
            throw new NotFoundException("Cannot find tokenID: " + tokenID);
        }

        // check confirmationId
        if (!confirmationId.equalsIgnoreCase(Hash.hash(tokenID + hashComponent +
                SystemProperties.get(AM_ENCRYPTION_PWD)))) {
            RestDispatcher.debug.error("IdentityResource.confirmRegistration: Invalid confirmationId : "
                    + confirmationId);
                throw new BadRequestException("Invalid confirmationId", null);
        }

        //check realm
        if (!realm.equals(ctsToken.getValue(CoreTokenField.STRING_ONE))) {
            RestDispatcher.debug.error("IdentityResource.confirmRegistration: Invalid realm : " + realm);
            throw new BadRequestException("Invalid realm", null);
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(final ServerContext context, final ActionRequest request,
                                 final ResultHandler<JsonValue> handler) {

        RestSecurity restSecurity = getRestSecurity(realm);

        final String action = request.getAction();
        if (action.equalsIgnoreCase("idFromSession")) {
            idFromSession(context, request, handler);
        } else if(action.equalsIgnoreCase("register")){
            createRegistrationEmail(context,request, handler);
        } else if(action.equalsIgnoreCase("confirm")) {
            confirmRegistration(context, request, handler, realm);
        } else if(action.equalsIgnoreCase("anonymousCreate")) {
            anonymousCreate(context, request, handler, restSecurity);
        } else if(action.equalsIgnoreCase("forgotPassword")){
            generateNewPasswordEmail(context, request, handler);
        } else if(action.equalsIgnoreCase("forgotPasswordReset")){
            anonymousUpdate(context, request, handler);
        } else { // for now this is the only case coming in, so fail if otherwise
            final ResourceException e =
                    new NotSupportedException("Actions are not supported for resource instances");
            handler.handleError(e);
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
            return userIdentity.isActive();
        } catch (IdRepoException idr) {
            RestDispatcher.debug.error("IdentityResource.isUserActive(): Invalid UID: " + uid + " Exception " + idr);
            throw new NotFoundException("Invalid UID, could not retrived " + uid);
        } catch (SSOException ssoe){
            RestDispatcher.debug.error("IdentityResource.isUserActive(): Invalid SSOToken" + " Exception " + ssoe);
            throw new NotFoundException("Invalid SSOToken " + ssoe.getMessage());
        }
    }

    private void generateNewPasswordEmail(final ServerContext context, final ActionRequest request,
                                     final ResultHandler<JsonValue> handler){
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        final JsonValue jsonBody = request.getContent();
        try {

            // Check to make sure forgotPassword enabled
            if (restSecurity == null) {
                RestDispatcher.debug.warning("IdentityResource.generateNewPasswordEmail(): " +
                        "Rest Security not created. restSecurity = " + restSecurity);
                throw new NotFoundException("Rest Security Service not created" );
            }
            if (!restSecurity.isForgotPassword()) {
                RestDispatcher.debug.warning("IdentityResource.generateNewPasswordEmail(): Forgot Password set to : "
                        + restSecurity.isForgotPassword());
                throw new NotFoundException("Forgot password is not accessible.");
            }

            // Generate Admin Token
            Token adminToken = new Token();
            adminToken.setId(RestUtils.getToken().getTokenID().toString());

            List<Attribute> searchAttributes = getIdentityServicesAttributes(realm);
            searchAttributes.add(getAttributeFromRequest(jsonBody));

            IdentityServicesImpl idsvc = new IdentityServicesImpl();
            List searchResults = idsvc.search(null, searchAttributes, adminToken);

            //only proceed if there is exactly one match
            if (searchResults.size() == 1) {
                String username = (String) searchResults.get(0);

                IdentityDetails identityDetails;
                identityDetails = idsvc.read(username, getIdentityServicesAttributes(realm), adminToken);
                String email = null;
                String uid = null;
                Attribute[] attrs = identityDetails.getAttributes();
                for (Attribute attribute : attrs) {
                    String attributeName = attribute.getName();
                    if (MAIL.equalsIgnoreCase(attributeName)) {
                        email = attribute.getValues()[0];
                    } else if (UNIVERSAL_ID.equalsIgnoreCase(attributeName)) {
                        uid = attribute.getValues()[0];
                    }
                 }
                // Check to see if user is Active/Inactive
                if (!isUserActive(uid)){
                    throw new ForbiddenException("Request is forbidden for this user");
                }
                // Check if email is provided
                if (email == null || email.isEmpty()) {
                    throw new InternalServerErrorException("No email provided in profile.");
                }

                // Get full deployment URL
                HttpContext header;
                header = context.asContext(HttpContext.class);
                StringBuilder deploymentURL = RestUtils.getFullDeploymentURI(header.getPath());

                String subject = jsonBody.get("subject").asString();
                String message = jsonBody.get("message").asString();

                // Retrieve email registration token life time
                if(restSecurity == null){
                    RestDispatcher.debug.warning("IdentityResource.generateNewPasswordEmail(): " +
                            "Rest Security not created. restSecurity = " + restSecurity);
                    throw new NotFoundException("Rest Security Service not created" );
                }
                Long tokenLifeTime = restSecurity.getForgotPassTLT();

                 // Generate Token
                 org.forgerock.openam.cts.api.tokens.Token ctsToken = generateToken(email, username, tokenLifeTime,
                         realm);

                 // Store token in datastore
                 cts.create(ctsToken);
    
                 // Create confirmationId
                String confirmationId = Hash.hash(ctsToken.getTokenId() + username +
                        SystemProperties.get(AM_ENCRYPTION_PWD));
    
                String confirmationLink;
                // Build Confirmation URL
                String confURL = restSecurity.getForgotPasswordConfirmationUrl();
                StringBuilder confURLBuilder = new StringBuilder(100);
                if(confURL == null || confURL.isEmpty()) {
                    confURLBuilder.append(deploymentURL.append("/json/confirmation/forgotPassword").toString());
                } else {
                    confURLBuilder.append(confURL);
                }
    
                confirmationLink = confURLBuilder.append("?confirmationId=").append(requestParamEncode(confirmationId))
                        .append("&tokenId=").append(requestParamEncode(ctsToken.getTokenId()))
                        .append("&username=").append(requestParamEncode(username))
                        .toString();
    
                // Send Registration
                sendNotification(email, subject,message, confirmationLink);
            }
            handler.handleResult(result);
        } catch (ResourceException re) {
            // Service not available, Neither or both Username/Email provided, User inactive
            RestDispatcher.debug.error(re.getMessage(), re);
            handler.handleError(re);
        } catch (ObjectNotFound onf) {
            // User not found
            RestDispatcher.debug.error("Could not find user", onf);
            handler.handleError(ResourceException.getException(ResourceException.NOT_FOUND, "User not found", onf));
        } catch (Exception e) {
            // Intentional - all other errors are considered Internal Error.
            RestDispatcher.debug.error("Internal error", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR, "Failed to send mail", e));
        }
    }

    private Attribute getAttributeFromRequest(JsonValue jsonBody) throws BadRequestException {
        String username = jsonBody.get(USERNAME).asString();
        String email = jsonBody.get(EMAIL).asString();

        if (username != null && email != null) {
            throw new BadRequestException("Both username and email specified - only one allowed in request.");
        }
 
        if (username != null && !username.isEmpty()) {
            return new Attribute(UNIVERSAL_ID_ABBREV, new String[] {username});
        }
 
        if (email != null && !email.isEmpty()) {
            return new Attribute(MAIL, new String[] {email});
         }

        throw new BadRequestException("Username or email not provided in request");
    }

    private void anonymousUpdate(final ServerContext context, final ActionRequest request,
                                 final ResultHandler<JsonValue> handler) {
        String tokenID;
        String confirmationId;
        String username;
        String newPassword;
        final JsonValue jVal = request.getContent();

        try{
            tokenID = jVal.get(TOKEN_ID).asString();
            confirmationId = jVal.get(CONFIRMATION_ID).asString();
            username = jVal.get(USERNAME).asString();
            newPassword =  jVal.get(USER_PASSWORD).asString();

            if(username == null || username.isEmpty()){
                throw new BadRequestException("username not provided");
            }
            if (newPassword == null || newPassword.isEmpty()) {
                throw new BadRequestException("new password not provided");
            }

            validateToken(tokenID, realm, username, confirmationId);

            // update Identity
            SSOToken tok = RestUtils.getToken();
            Token admin = new Token();
            admin.setId(tok.getTokenID().toString());

            // Update instance with new password value
            if (updateInstance(admin, json(object(field(USERNAME, username), field(USER_PASSWORD, newPassword))), handler)) {
                // Only remove the token if the update was successful, errors will be set in the handler.
                try {
                    // Even though the generated token will eventually timeout, delete it after a successful read
                    // so that the reset password request cannot be made again using the same token.
                    cts.delete(tokenID);
                } catch (DeleteFailedException e) {
                     // Catch this rather than letting it stop the process as it is possible that between successfully
                    // reading and deleting, the token has expired.
                    if (RestDispatcher.debug.messageEnabled()) {
                        RestDispatcher.debug.message("IdentityResource.anonymousUpdate(): Deleting token " + tokenID +
                             " after a successful read failed due to " + e.getMessage(), e);
                     }
                }
            }
        } catch (BadRequestException be){
            RestDispatcher.debug.error("IdentityResource.anonymousUpdate():" + be.getMessage());
            handler.handleError(be);
        } catch (CoreTokenException cte){
            RestDispatcher.debug.error("IdentityResource.anonymousUpdate():" + cte.getMessage());
            handler.handleError(new NotFoundException(cte.getMessage()));
        } catch (NotFoundException nfe){
            RestDispatcher.debug.error("IdentityResource.anonymousUpdate():" + nfe.getMessage());
            handler.handleError(ResourceException.getException(HttpURLConnection.HTTP_GONE, nfe.getMessage(), nfe));
        } catch (Exception e){
            RestDispatcher.debug.error("IdentityResource.anonymousUpdate():" + e.getMessage());
            handler.handleError(new NotFoundException(e.getMessage()));
        }

    }

    /**
     * Updates an instance given a JSON object with User Attributes
     * @param admin Token that has administrative privileges
     * @param details Json Value containing details of user identity
     * @param handler handles result of operation
     * @return true if the update was successful
     */
    private boolean updateInstance(Token admin, final JsonValue details, final ResultHandler<JsonValue> handler){
        JsonValue jVal = details;
        IdentityDetails newDtls;
        IdentityServicesImpl idsvc;
        String resourceId = jVal.get(USERNAME).asString();

        boolean successfulUpdate = false;

        try {
            idsvc = new IdentityServicesImpl();
            newDtls = jsonValueToIdentityDetails(jVal);
            newDtls.setName(resourceId);

            // update resource with new details
            UpdateResponse message = idsvc.update(newDtls, admin);
            // read updated identity back to client
            IdentityDetails checkIdent = idsvc.read(resourceId, idSvcsAttrList, admin);
            // handle updated resource
            handler.handleResult(identityDetailsToJsonValue(checkIdent));
            successfulUpdate = true;
        } catch (final Exception exception) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE! " +
                    exception);
            handler.handleError(new NotFoundException(exception.getMessage(), exception));
        }

        return successfulUpdate;
    }


    private void anonymousCreate(final ServerContext context, final ActionRequest request,
                                 final ResultHandler<JsonValue> handler, RestSecurity restSecurity) {

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

            if(email == null || email.isEmpty()){
                throw new BadRequestException("Email not provided");
            }
            // Convert to IDRepo Attribute schema
            jVal.put("mail",email);

            if(confirmationId == null || confirmationId.isEmpty()){
                throw new BadRequestException("confirmationId not provided");
            }

            validateToken(tokenID, realm, email, confirmationId);

            // create an Identity
            SSOToken tok = RestUtils.getToken();
            Token admin = new Token();
            admin.setId(tok.getTokenID().toString());
            if (createInstance(admin, jVal, handler)) {
                 // Only remove the token if the create was successful, errors will be set in the handler.
                 try {
                     // Even though the generated token will eventually timeout, delete it after a successful read
                     // so that the completed registration request cannot be made again using the same token.
                     cts.delete(tokenID);
                 } catch (DeleteFailedException e) {
                     // Catch this rather than letting it stop the process as it is possible that between successfully
                     // reading and deleting, the token has expired.
                     if (RestDispatcher.debug.messageEnabled()) {
                         RestDispatcher.debug.message("IdentityResource.anonymousCreate: Deleting token " + tokenID +
                                 " after a successful read failed due to " + e.getMessage(), e);
                     }
                 }
            }
        } catch (BadRequestException be){
            RestDispatcher.debug.error("IdentityResource.anonymousCreate() :: Invalid Parameter " + be);
            handler.handleError(be);
        } catch (NotFoundException nfe){
            RestDispatcher.debug.error("IdentityResource.anonymousCreate(): Invalid tokenID : " + tokenID);
            handler.handleError(nfe);
        } catch (ServiceNotFoundException e) {
            // Failure from RestSecurity
            RestDispatcher.debug.error("Internal error", e);
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR, e.getMessage(), e));
            return;
        } catch (CoreTokenException cte){ // For any unexpected CTS error
            RestDispatcher.debug.error("IdentityResource.anonymouseCreate(): CTS Error : " + cte.getMessage());
            handler.handleError(ResourceException.getException(ResourceException.INTERNAL_ERROR, cte.getMessage(),cte));
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(final ServerContext context, final String resourceId, final ActionRequest request,
                               final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    /**
     * Creates an a resource using a privileged token
     * @param admin Token that has administrative privileges
     * @param details resource details that needs to be created
     * @param handler handles result of operation
     * @return true if the create was successful
     */
    private boolean createInstance(Token admin, final JsonValue details, final ResultHandler<JsonValue> handler) {

        JsonValue jVal = details;
        IdentityDetails dtls, identity;
        IdentityServicesImpl idsvc;
        String resourceId = null;

        boolean successfulCreate = false;

        try {
            idsvc = new IdentityServicesImpl();
            identity = jsonValueToIdentityDetails(jVal);
            resourceId = identity.getName();

            // Create the resource
            CreateResponse success = idsvc.create(identity, admin);
            // Read created resource
            dtls = idsvc.read(identity.getName(), idSvcsAttrList, admin);
            handler.handleResult(identityDetailsToJsonValue(dtls));
            successfulCreate = true;
        } catch (final ObjectNotFound notFound) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot READ " +
                    resourceId + ": Resource cannot be found." + notFound);
            handler.handleError(new NotFoundException("Resource not found.", notFound));
        } catch (final DuplicateObject duplicateObject) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    resourceId + ": Resource already exists!" + duplicateObject);
            handler.handleError(new NotFoundException("Resource already exists", duplicateObject));
        } catch (final TokenExpired tokenExpired) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        } catch (final NeedMoreCredentials needMoreCredentials) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    needMoreCredentials);
            handler.handleError(new ForbiddenException("Token is not authorized", needMoreCredentials));
        } catch (final Exception exception) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE! " +
                    exception);
            handler.handleError(new NotFoundException(exception.getMessage(), exception));
        }

        return successfulCreate;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
                               final ResultHandler<Resource> handler) {
        // anyone can create an account add
        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        final JsonValue jVal = request.getContent();
        IdentityDetails dtls, identity;
        Resource resource;
        IdentityServicesImpl idsvc;
        String resourceId = request.getNewResourceId();

        try {
            idsvc = new IdentityServicesImpl();
            identity = jsonValueToIdentityDetails(jVal);
            // check to see if request has included resource ID
            if(resourceId != null ){
                if(identity.getName() != null){
                    if(!resourceId.equalsIgnoreCase(identity.getName())){
                        throw new BadRequestException("id in path does not match id in request body");
                    }
                }
                identity.setName(resourceId);
            } else {
                resourceId = identity.getName();
            }


            // Create the resource
            CreateResponse success = idsvc.create(identity, admin);
            // Read created resource
            dtls = idsvc.read(resourceId, idSvcsAttrList, admin);

            resource = new Resource(resourceId, "0", identityDetailsToJsonValue(dtls));
            handler.handleResult(resource);
        } catch (final ObjectNotFound notFound) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot READ " +
                    resourceId + ": Resource cannot be found." + notFound);
            handler.handleError(new NotFoundException("Resource not found.", notFound));
        } catch (final DuplicateObject duplicateObject) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    resourceId + ": Resource already exists!" + duplicateObject);
            handler.handleError(new NotFoundException("Resource already exists", duplicateObject));
        } catch (final TokenExpired tokenExpired) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        } catch (final NeedMoreCredentials needMoreCredentials) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    needMoreCredentials);
            handler.handleError(new ForbiddenException("Token is not authorized", needMoreCredentials));
        } catch(BadRequestException be) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    be);
            handler.handleError(be);
        } catch (final Exception exception) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE! " +
                    exception);
            handler.handleError(new NotFoundException(exception.getMessage(), exception));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(final ServerContext context, final String resourceId, final DeleteRequest request,
                               final ResultHandler<Resource> handler) {
        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        Resource resource;
        IdentityDetails dtls;
        IdentityServicesImpl idsvc = new IdentityServicesImpl();

        try {

            // read to see if resource is available to user
            dtls = idsvc.read(resourceId, idSvcsAttrList, admin);

            // delete the resource
            DeleteResponse success = idsvc.delete(dtls, admin);

            result.put("success", "true");
            resource = new Resource(resourceId, "0", result);
            handler.handleResult(resource);

        } catch (final NeedMoreCredentials ex) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ": User does not have enough privileges.");
            handler.handleError(new ForbiddenException(resourceId, ex));
        } catch (final ObjectNotFound notFound) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ":" + notFound);
            handler.handleError(new NotFoundException("Resource cannot be found.", notFound));
        } catch (final TokenExpired tokenExpired) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        } catch (final AccessDenied accessDenied) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ":" + accessDenied);
            handler.handleError(new ForbiddenException(accessDenied.getMessage(), accessDenied));
        } catch (final GeneralFailure generalFailure) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    generalFailure.getMessage());
            handler.handleError(new BadRequestException(generalFailure.getMessage(), generalFailure));
        } catch (final Exception exception) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE! " +
                    exception.getMessage());
            result.put("success", "false");
            resource = new Resource(resourceId, "0", result);
            handler.handleResult(resource);
        }
    }

    /**
     * Returns a JsonValue containing appropriate identity details
     *
     * @param details The IdentityDetails of a Resource
     * @return The JsonValue Object
     */
    private JsonValue identityDetailsToJsonValue(IdentityDetails details) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        try {
            result.put(USERNAME, details.getName());
            result.put("realm", details.getRealm());
            Attribute[] attrs = details.getAttributes();

            for (Attribute aix : attrs) {
                result.put(aix.getName(), aix.getValues());
            }
            return result;
        } catch (final Exception e) {
            throw new JsonValueException(result);
        }
    }

    /**
     * Returns an IdentityDetails from a JsonValue
     *
     * @param jVal The JsonValue Object to be converted
     * @return The IdentityDetails object
     */
    private IdentityDetails jsonValueToIdentityDetails(JsonValue jVal) {

        IdentityDetails identity = new IdentityDetails();
        List<Attribute> identityAttrList = new ArrayList();

        try {
            identity.setType(userType); //set type ex. user
            identity.setRealm(realm); //set realm
            identity.setName(jVal.get(USERNAME).asString());//set name from JsonValue object

            try {
                for (String s : jVal.keys()) {
                    JsonValue childValue = jVal.get(s);
                    if (childValue.isString()) {
                        String[] tArray = {childValue.asString()};
                        identityAttrList.add(new Attribute(s, tArray));
                    } else if (childValue.isList()) {
                        ArrayList<String> tList = (ArrayList<String>) childValue.getObject();
                        String[] tArray = tList.toArray(new String[tList.size()]);
                        identityAttrList.add(new Attribute(s, tArray));
                    }
                }
            } catch (Exception e) {
                RestDispatcher.debug.error("IdentityResource.jsonValueToIdentityDetails() :: " +
                        "Cannot Traverse JsonValue" + e);
            }
            Attribute[] attr = identityAttrList.toArray(new Attribute[identityAttrList.size()]);
            identity.setAttributes(attr);

        } catch (final Exception e) {
            RestDispatcher.debug.error("IdentityResource.jsonValueToIdentityDetails() ::" +
                    " Cannot convert JsonValue to IdentityDetails." + e);
            //deal with better exceptions
        }
        return identity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(final ServerContext context, final String resourceId, final PatchRequest request,
                              final ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
                                final QueryResultHandler handler) {

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));


        String queryFilter;

        try {
            // This will only return 1 user..
            // getQueryFilter() is not implemented yet..returns dummy false value
            queryFilter = request.getQueryId();
            if (queryFilter == null || queryFilter.isEmpty()) {
                queryFilter = "*";
            }
            IdentityServicesImpl id = new IdentityServicesImpl();
            List<String> users = id.search(queryFilter, idSvcsAttrList, admin);

            for (final String user : users) {
                JsonValue val = new JsonValue(user);
                Resource resource = new Resource(user, "0", val);
                handler.handleResource(resource);
            }
        } catch (Exception ex) {

        }

        handler.handleResult(new QueryResult());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(final ServerContext context, final String resourceId, final ReadRequest request,
                             final ResultHandler<Resource> handler) {

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        IdentityServicesImpl idsvc;
        IdentityDetails dtls;
        Resource resource;

        try {
            idsvc = new IdentityServicesImpl();
            dtls = idsvc.read(resourceId, idSvcsAttrList, admin);
            resource = new Resource(resourceId, "0", identityDetailsToJsonValue(dtls));
            handler.handleResult(resource);
        } catch (final NeedMoreCredentials needMoreCredentials) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + needMoreCredentials);
            handler.handleError(new ForbiddenException("User does not have enough privileges.", needMoreCredentials));
        } catch (final ObjectNotFound objectNotFound) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + objectNotFound);
            handler.handleError(new NotFoundException("Resource cannot be found.", objectNotFound));
        } catch (final TokenExpired tokenExpired) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        } catch (final AccessDenied accessDenied) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + accessDenied);
            handler.handleError(new ForbiddenException(accessDenied.getMessage(), accessDenied));
        } catch (final GeneralFailure generalFailure) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    generalFailure);
            handler.handleError(new BadRequestException(generalFailure.getMessage(), generalFailure));
        } catch (final Exception exception) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ! " +
                    exception);
            handler.handleError(new NotFoundException(exception.getMessage(), exception));

        }
    }

    private String getPasswordFromHeader(ServerContext context){
        List<String> headerList = null;
        String oldUserPasswordHeaderName = "olduserpassword";
        HttpContext header = null;

        try {
            header = context.asContext(HttpContext.class);
            if (header == null) {
                RestDispatcher.debug.error("IdentityResource.getPasswordFromHeader :: " +
                        "Cannot retrieve ServerContext as HttpContext");
                return null;
            }
            //get the oldusername from header directly
            headerList = header.getHeaders().get(oldUserPasswordHeaderName.toLowerCase());
            if (headerList != null && !headerList.isEmpty()) {
                for (String s : headerList) {
                    return (s != null && !s.isEmpty()) ? s : null;
                }
            }
        } catch (Exception e) {
            RestDispatcher.debug.error("IdentityResource.getPasswordFromHeader :: " +
                    "Cannot get olduserpassword from ServerContext!" + e);
        }
        return null;
    }

    @Override
    public void updateInstance(final ServerContext context, final String resourceId, final UpdateRequest request,
                               final ResultHandler<Resource> handler) {

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        final JsonValue jsonValue = request.getNewContent();
        IdentityDetails dtls, newDtls;
        IdentityServicesImpl idsvc = new IdentityServicesImpl();
        Resource resource;
        try {
            // Retrieve details about user to be updated
            dtls = idsvc.read(resourceId, idSvcsAttrList, admin);

            //check first if the password is modified as part of the update request, so if necessary, the password can
            //be removed from the IdentityDetails object.
            if (!isAdmin(context)) {
                for (String attrName : jsonValue.keys()) {
                    if ("userpassword".equalsIgnoreCase(attrName)) {
                        String newPassword = jsonValue.get(attrName).asString();
                        if (!isNullOrEmpty(newPassword)) {
                            String oldPassword = getPasswordFromHeader(context);
                            if (isNullOrEmpty(oldPassword)) {
                                throw new BadRequestException("The old password is missing from the request");
                            }
                            //This is an end-user trying to change the password, so let's change the password by
                            //verifying that the provided old password is correct. We also remove the password from the
                            //list of attributes to prevent the administrative password reset via the update call.
                            jsonValue.remove(attrName);
                            changePassword(context, handler, resourceId, oldPassword, newPassword);
                        }
                        break;
                    }
                }
            }
            newDtls = jsonValueToIdentityDetails(jsonValue);
            if (newDtls.getName() != null && !resourceId.equalsIgnoreCase(newDtls.getName())) {
                throw new BadRequestException("id in path does not match id in request body");
            }
            newDtls.setName(resourceId);

            // update resource with new details
            UpdateResponse message = idsvc.update(newDtls, admin);
            // read updated identity back to client
            IdentityDetails checkIdent = idsvc.read(dtls.getName(), idSvcsAttrList, admin);
            // handle updated resource
            resource = new Resource(resourceId, "0", identityDetailsToJsonValue(checkIdent));
            handler.handleResult(resource);
        } catch (final ObjectNotFound onf) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE! " +
                    onf);
            handler.handleError(new NotFoundException("Could not find the resource [ " + resourceId + " ] to update", onf));
        } catch (final NeedMoreCredentials needMoreCredentials) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    resourceId + ":" + needMoreCredentials);
            handler.handleError(new ForbiddenException("Token is not authorized", needMoreCredentials));
        } catch (final TokenExpired tokenExpired) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        } catch (final AccessDenied accessDenied) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    resourceId + ":" + accessDenied);
            handler.handleError(new ForbiddenException(accessDenied.getMessage(), accessDenied));
        } catch (final GeneralFailure generalFailure) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    generalFailure);
            handler.handleError(new BadRequestException(generalFailure.getMessage(), generalFailure));
        } catch (final ResourceException re){
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE! "
                    + resourceId + ":" + re);
            handler.handleError(re);
        } catch (final Exception exception) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE! " +
                    exception);
            handler.handleError(new NotFoundException(exception.getMessage(), exception));
        }
    }

   private List<Attribute> getIdentityServicesAttributes(String realm) {

       final List<Attribute> identityServicesAttributes = new ArrayList<Attribute>();

       String[] userTypeVal = {userType};
       String[] realmVal = {realm};
       identityServicesAttributes.add(new Attribute("objecttype", userTypeVal));
       identityServicesAttributes.add(new Attribute("realm", realmVal));

       return identityServicesAttributes;
   }

    private void changePassword(ServerContext serverContext, ResultHandler<Resource> handler, String username,
            String oldPassword, String newPassword) throws ResourceException {
        try {
            SSOTokenManager tokenManager = SSOTokenManager.getInstance();
            SSOToken requesterToken = tokenManager.createSSOToken(getCookieFromServerContext(serverContext));
            if (tokenManager.isValidToken(requesterToken)) {
                AMIdentity userIdentity = new AMIdentity(requesterToken, username, IdType.USER, realm, null);
                userIdentity.changePassword(oldPassword, newPassword);
            }
        } catch (SSOException ssoe) {
            RestDispatcher.debug.warning("IdentityResource.changePassword() :: SSOException occurred while changing "
                    + "the password for user: " + username, ssoe);
            throw new PermanentException(401, "An error occurred while trying to change the password", ssoe);
        } catch (IdRepoException ire) {
            if ("402".equals(ire.getErrorCode())) {
                throw new ForbiddenException("The user is not authorized to change the password");
            } else {
                RestDispatcher.debug.warning("IdentityResource.changePassword() :: IdRepoException occurred while "
                        + "changing the password for user: " + username, ire);
                throw new InternalServerErrorException("An error occurred while trying to change the password", ire);
            }
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
     * Retrieve cached realm's RestSecurity instance
     **/
    private RestSecurity getRestSecurity(String realm) {
        RestSecurity restSecurity = REALM_REST_SECURITY_MAP.get(realm);
        if (restSecurity == null) {
            synchronized(REALM_REST_SECURITY_MAP) {
                restSecurity = REALM_REST_SECURITY_MAP.get(realm);
                if (restSecurity == null) {
                    restSecurity = new RestSecurity(realm);
                    REALM_REST_SECURITY_MAP.put(realm, restSecurity);
                }
            }
        }
        return restSecurity;
    }

    private static boolean isNullOrEmpty(final String value) {
       return value == null || value.isEmpty();
   }
}
