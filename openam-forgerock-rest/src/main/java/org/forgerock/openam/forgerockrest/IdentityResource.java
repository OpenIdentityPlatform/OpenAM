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
 * Copyright 2012-2014 ForgeRock AS
 */

package org.forgerock.openam.forgerockrest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idsvcs.AccessDenied;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.idsvcs.CreateResponse;
import com.sun.identity.idsvcs.DeleteResponse;
import com.sun.identity.idsvcs.DuplicateObject;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.NeedMoreCredentials;
import com.sun.identity.idsvcs.ObjectNotFound;
import com.sun.identity.idsvcs.Token;
import com.sun.identity.idsvcs.TokenExpired;
import com.sun.identity.idsvcs.UpdateResponse;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.apache.commons.lang.RandomStringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.services.RestSecurity;
import org.forgerock.openam.services.email.MailServerImpl;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.openam.forgerockrest.RestUtils.getCookieFromServerContext;
import static org.forgerock.openam.forgerockrest.RestUtils.isAdmin;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class IdentityResource implements CollectionResourceProvider {

    private static Debug debug = Debug.getInstance("frRest");

    public static final String USER_TYPE = "user";
    public static final String GROUP_TYPE = "group";
    public static final String AGENT_TYPE = "agent";

    // TODO: filters, sorting, paged results.

    private final String userType;

    private ServiceConfigManager mailmgr;
    private ServiceConfig mailscm;
    Map<String, HashSet<String>> mailattrs;

    final static String MAIL_IMPL_CLASS = "forgerockMailServerImplClassName";
    final static String MAIL_SUBJECT = "forgerockEmailServiceSMTPSubject";
    final static String MAIL_MESSAGE = "forgerockEmailServiceSMTPMessage";

    final static private String UNIVERSAL_ID = "universalid";
    final static private String MAIL = "mail";

    final static String EMAIL = "email";
    final static String TOKEN_ID = "tokenId";
    final static String CONFIRMATION_ID = "confirmationId";

    /**
     * Creates a backend
     */
    @Inject
    public IdentityResource(final String userType) {
        this(userType, null, null);
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
    IdentityResource(String userType, ServiceConfigManager mailmgr, ServiceConfig mailscm) {
        this.userType = userType;
        this.mailmgr = mailmgr;
        this.mailscm = mailscm;
    }

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
            result.put("successURL", ssotok.getProperty(ISAuthConstants.SUCCESS_URL,false));
            result.put("fullLoginURL", ssotok.getProperty(ISAuthConstants.FULL_LOGIN_URL,false));
            handler.handleResult(result);

        } catch (SSOException e) {
            debug.error("IdentityResource.idFromSession() :: Cannot retrieve SSO Token: " + e);
            handler.handleError(new ForbiddenException("SSO Token cannot be retrieved.", e));
        } catch (IdRepoException ex) {
            debug.error("IdentityResource.idFromSession() :: Cannot retrieve user from IdRepo" + ex);
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

    private org.forgerock.openam.cts.api.tokens.Token generateToken(String resource, String userId,
                                                                    Long tokenLifeTimeSeconds) {
        Calendar ttl = Calendar.getInstance();
        org.forgerock.openam.cts.api.tokens.Token ctsToken = new org.forgerock.openam.cts.api.tokens.Token(
                generateTokenID(resource), TokenType.REST);
        if(userId != null && !userId.isEmpty()) {
            ctsToken.setUserId(userId);
        }
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
    private void createRegistrationEmail(final ServerContext context, final ActionRequest request, final String realm,
            final RestSecurity restSecurity, final ResultHandler<JsonValue> handler){


        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        final JsonValue jVal = request.getContent();
        String emailAddress = null;
        String confirmationLink;
        String tokenID;

        try {

            if(restSecurity == null){
                debug.warning("IdentityResource.createRegistrationEmail(): " +
                        "Rest Security not created. restSecurity = " + restSecurity);
                throw new NotFoundException("Rest Security Service not created" );
            }
            if(!restSecurity.isSelfRegistration()){
                debug.warning("IdentityResource.createRegistrationEmail(): Self-Registration set to :"
                        + restSecurity.isSelfRegistration());
                throw new NotFoundException("Self Registration is not accessible.");
            }
            // Get full deployment URL
            HttpContext header = null;
            header = context.asContext(HttpContext.class);
            StringBuilder deploymentURL = RestUtils.getFullDeploymentURI(header.getPath());

            // Get the email address provided from registration page
            emailAddress = jVal.get(EMAIL).asString();
            if(emailAddress == null || emailAddress.isEmpty()){
                throw new BadRequestException("Email not provided");
            }

            String subject = jVal.get("subject").asString();
            String message = jVal.get("message").asString();

            // Retrieve email registration token life time
            Long tokenLifeTime = restSecurity.getSelfRegTLT();

            // Create CTS Token
            org.forgerock.openam.cts.api.tokens.Token ctsToken = generateToken(emailAddress, "anonymous",
                    tokenLifeTime);

            // Store token in datastore
            CTSHolder.getCTS().create(ctsToken);
            tokenID = ctsToken.getTokenId();
            // Create confirmationId
            String confirmationId = Hash.hash(tokenID + emailAddress + SystemProperties.get("am.encryption.pwd"));

            // Build Confirmation URL
            String confURL = restSecurity.getSelfRegistrationConfirmationUrl();
            StringBuilder confURLBuilder = new StringBuilder(100);
            if(confURL == null || confURL.isEmpty()) {
                confURLBuilder.append(deploymentURL.append("/json/confirmation/register").toString());
            } else {
                confURLBuilder.append(confURL);
            }

            confirmationLink = confURLBuilder.append("?confirmationId=").append(confirmationId)
                    .append("&email=").append(emailAddress)
                    .append("&tokenId=").append(tokenID)
                    .toString();

            // Send Registration
            sendNotification(emailAddress, subject, message, realm, confirmationLink);
            handler.handleResult(result);
        } catch (BadRequestException be) {
            debug.error("IdentityResource.createRegistrationEmail: Cannot send email to : " + emailAddress
                    + be.getMessage());
            handler.handleError(be);
        } catch (NotFoundException nfe){
            debug.error("IdentityResource.createRegistrationEmail: Cannot send email to : " + emailAddress
                    + nfe.getMessage());
            handler.handleError(nfe);
        } catch (Exception e){
            debug.error("IdentityResource.createRegistrationEmail: Cannot send email to : " + emailAddress
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
    private void sendNotification(String to, String subject, String message, String realm, String confirmationLink) throws Exception{

        try {
            mailmgr = new ServiceConfigManager(RestUtils.getToken(),
                    MailServerImpl.SERVICE_NAME, MailServerImpl.SERVICE_VERSION);
            mailscm = mailmgr.getOrganizationConfig(realm,null);
            mailattrs = mailscm.getAttributes();

        } catch (SMSException smse) {
            debug.error("IdentityResource.sendNotification() :: Cannot create service " +
                    MailServerImpl.SERVICE_NAME + smse);
            throw new InternalServerErrorException("Cannot create the service: "+ MailServerImpl.SERVICE_NAME, smse);

        } catch (SSOException ssoe){
            debug.error("IdentityResource.sendNotification() :: Invalid SSOToken " + ssoe);
            throw new InternalServerErrorException("Cannot create the service: "+ MailServerImpl.SERVICE_NAME, ssoe);
        }

        if(mailattrs == null || mailattrs.isEmpty()){
            debug.error("IdentityResource.sendNotification() :: no attrs set"  + mailattrs );
            throw new NotFoundException("No service Config Manager found for realm " + realm);
        }

        // Get MailServer Implementation class
        String attr = mailattrs.get(MAIL_IMPL_CLASS).iterator().next();
        MailServerImpl mailServImpl = (MailServerImpl)Class.forName(attr).getDeclaredConstructor(String.class).newInstance(realm);

        try {
            // Check if subject has not  been included
            if(subject == null || subject.isEmpty()) {
                // Use default email service subject
                subject = mailattrs.get(MAIL_SUBJECT).iterator().next();
            }
        } catch (Exception e){
            debug.warning("IdentityResource.sendNotification() :: no subject found"  + e.getMessage());
            subject = "";
        }
        try {
            // Check if Custom Message has been included
            if(message == null || message.isEmpty()){
                // Use default email service message
                message = mailattrs.get(MAIL_MESSAGE).iterator().next();
            }
            message = message + System.getProperty("line.separator")  + confirmationLink;
        } catch (Exception e){
            debug.warning("IdentityResource.sendNotification() :: no message found"  + e.getMessage());
            message = confirmationLink;
        }
        // Send the emails via the implementation class
        mailServImpl.sendEmail(to, subject, message);
    }

    /**
     * Will validate confirmationId is correct
     * @param context Current Server Context
     * @param request Request from client to confirm registration
     * @param handler Result handler
     */
    private void confirmRegistration(final ServerContext context, final ActionRequest request,
                                     final ResultHandler<JsonValue> handler){
        final JsonValue jVal = request.getContent();
        String tokenID;
        String confirmationId;
        String email = null;
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));

        try{
            tokenID = jVal.get(TOKEN_ID).asString();
            confirmationId = jVal.get(CONFIRMATION_ID).asString();
            email = jVal.get(EMAIL).asString();

            if(email == null || email.isEmpty()){
                throw new BadRequestException("Email not provided");
            }
            if(confirmationId == null || confirmationId.isEmpty()){
                throw new BadRequestException("confirmationId not provided");
            }

            // Check Token is still in CTS
            // Check that tokenID is not expired
            if(CTSHolder.getCTS().read(tokenID) == null){
                throw new NotFoundException("Cannot find tokenID: " + tokenID);
            }

            // check confirmationId
            if(!confirmationId.equalsIgnoreCase(Hash.hash(
                    tokenID + email+ SystemProperties.get("am.encryption.pwd")))){
                debug.error("IdentityResource.confirmRegistration: Invalid confirmationId : "
                            + confirmationId);
                throw new BadRequestException("Invalid confirmationId", null);
            }
            // build resource
            result.put(EMAIL,email );
            result.put(TOKEN_ID, tokenID);
            result.put(CONFIRMATION_ID, confirmationId);
            handler.handleResult(result);

        } catch (BadRequestException be){
            debug.error("IdentityResource.confirmRegistration: Cannot confirm registration for : "
                    + email);
            handler.handleError(be);
        } catch (Exception e){
            debug.error("IdentityResource.confirmRegistration: Cannot confirm registration for : "
                    + email + e);
            handler.handleError(new NotFoundException(e.getMessage()));
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(final ServerContext context, final ActionRequest request,
                                 final ResultHandler<JsonValue> handler) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getRealm();
        RestSecurity restSecurity = new RestSecurity(realm);

        final String action = request.getAction();
        if (action.equalsIgnoreCase("idFromSession")) {
            idFromSession(context, request, handler);
        } else if(action.equalsIgnoreCase("register")){
            createRegistrationEmail(context,request, realm, restSecurity, handler);
        } else if(action.equalsIgnoreCase("confirm")) {
            confirmRegistration(context, request, handler);
        } else if(action.equalsIgnoreCase("anonymousCreate")) {
            anonymousCreate(context, request, realm, handler);
        } else if(action.equalsIgnoreCase("forgotPassword")){
            generateNewPasswordEmail(context, request, realm, restSecurity, handler);
        } else if(action.equalsIgnoreCase("forgotPasswordReset")){
            anonymousUpdate(context, request, realm, handler);
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
            debug.error("IdentityResource.isUserActive(): Invalid UID: " + uid + " Exception " + idr);
            throw new NotFoundException("Invalid UID, could not retrived " + uid);
        } catch (SSOException ssoe){
            debug.error("IdentityResource.isUserActive(): Invalid SSOToken" + " Exception " + ssoe);
            throw new NotFoundException("Invalid SSOToken " + ssoe.getMessage());
        }
    }

    private void generateNewPasswordEmail(final ServerContext context, final ActionRequest request, final String realm,
            final RestSecurity restSecurity, final ResultHandler<JsonValue> handler){
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        final JsonValue jVal = request.getContent();
        String username = null;

        IdentityServicesImpl idsvc;
        IdentityDetails dtls;

        try {

            // Check to make sure forgotPassword enabled
            if(restSecurity == null){
                debug.warning("IdentityResource.generateNewPasswordEmail(): " +
                        "Rest Security not created. restSecurity = " + restSecurity);
                throw new NotFoundException("Rest Security Service not created" );
            }
            if(!restSecurity.isForgotPassword()){
                debug.warning("IdentityResource.generateNewPasswordEmail(): Forgot Password set to : "
                        + restSecurity.isForgotPassword());
                throw new NotFoundException("Forgot password is not accessible.");
            }

            // Generate Admin Token
            SSOToken tok = RestUtils.getToken();
            Token admin = new Token();
            admin.setId(tok.getTokenID().toString());

            // Get the user id provided from forgot password page
            username = jVal.get("username").asString();
            if(username == null || username.isEmpty()){
                throw new BadRequestException("Username not provided");
            }

            // Look up Identity
            idsvc = new IdentityServicesImpl();
            dtls = idsvc.read(username, getIdentityServicesAttributes(realm), admin);

            String email = null;
            String uid = null;
            Attribute[] attrs = dtls.getAttributes();
            for (Attribute attribute : attrs){
                String attributeName = attribute.getName();
                if(MAIL.equalsIgnoreCase(attributeName)){
                    email = attribute.getValues()[0];
                } else if(UNIVERSAL_ID.equalsIgnoreCase(attributeName)){
                    uid = attribute.getValues()[0];
                }
            }
            // Check to see if user is Active/Inactive
            if(!isUserActive(uid)){
                throw new ForbiddenException("Request is forbidden for this user");
            }
            // Check if email is provided
            if(email == null || email.isEmpty()){
                throw new InternalServerErrorException("No email provided in profile.");
            }

            // Get full deployment URL
            HttpContext header;
            header = context.asContext(HttpContext.class);
            StringBuilder deploymentURL = RestUtils.getFullDeploymentURI(header.getPath());

            String subject = jVal.get("subject").asString();
            String message = jVal.get("message").asString();

            // Retrieve email registration token life time
            if(restSecurity == null){
                debug.warning("IdentityResource.generateNewPasswordEmail(): " +
                        "Rest Security not created. restSecurity = " + restSecurity);
                throw new NotFoundException("Rest Security Service not created" );
            }
            Long tokenLifeTime = restSecurity.getForgotPassTLT();

            // Generate Token
            org.forgerock.openam.cts.api.tokens.Token ctsToken = generateToken(email, username, tokenLifeTime);

            // Store token in datastore
            CTSHolder.getCTS().create(ctsToken);

            // Create confirmationId
            String confirmationId = Hash.hash(ctsToken.getTokenId() + username + SystemProperties.get("am.encryption.pwd"));

            String confirmationLink;
            // Build Confirmation URL
            String confURL = restSecurity.getForgotPasswordConfirmationUrl();
            StringBuilder confURLBuilder = new StringBuilder(100);
            if(confURL == null || confURL.isEmpty()) {
                confURLBuilder.append(deploymentURL.append("/json/confirmation/forgotPassword").toString());
            } else {
                confURLBuilder.append(confURL);
            }

            confirmationLink = confURLBuilder.append("?confirmationId=").append(confirmationId)
                    .append("&tokenId=").append(ctsToken.getTokenId())
                    .append("&username=").append(requestParamEncode(username))
                    .toString();

            // Send Registration
            sendNotification(email, subject, message, realm, confirmationLink);
            handler.handleResult(result);
        } catch (BadRequestException be) {
            debug.error("IdentityResource.generateNewPasswordEmail(): Cannot send email to : " + username
                    + " Exception " + be);
            handler.handleError(be);
        } catch (NotFoundException nfe){
            debug.error("IdentityResource.generateNewPasswordEmail(): Cannot send email to : " + username
                    + " Exception " + nfe);
            handler.handleError(nfe);
        } catch (ForbiddenException fe){
            debug.error("IdentityResource.generateNewPasswordEmail(): Cannot send email to : " + username
                    + " Exception " + fe);
            handler.handleError(fe);
        }catch (CoreTokenException cte){
            debug.error("IdentityResource.generateNewPasswordEmail(): Cannot send email to : " + username
                    + " Exception " + cte);
            handler.handleError(new NotFoundException("Email not sent"));
        } catch (InternalServerErrorException ise){
            debug.error("IdentityResource.generateNewPasswordEmail(): Cannot send email to : " + username
                    + " Exception " + ise);
            handler.handleError(ise);
        }catch (Exception e){
            debug.error("IdentityResource.generateNewPasswordEmail(): Cannot send email to : " + username
                    + " Exception " + e);
            handler.handleError(new NotFoundException("Email not sent"));
        }
    }

    private void anonymousUpdate(final ServerContext context, final ActionRequest request, final String realm,
            final ResultHandler<JsonValue> handler) {
        String tokenID;
        String confirmationId;
        String username;
        String nwpassword;
        final JsonValue jVal = request.getContent();

        try{
            tokenID = jVal.get(TOKEN_ID).asString();
            jVal.remove(TOKEN_ID);
            confirmationId = jVal.get(CONFIRMATION_ID).asString();
            jVal.remove(CONFIRMATION_ID);
            username = jVal.get("username").asString();
            nwpassword =  jVal.get("userpassword").asString();

            if(username == null || username.isEmpty()){
                throw new BadRequestException("username not provided");
            }
            if(nwpassword == null || username.isEmpty()) {
                throw new BadRequestException("new password not provided");
            }

            // Check that tokenID is not expired
            if(CTSHolder.getCTS().read(tokenID) == null){
                throw new NotFoundException("Cannot find tokenID: " + tokenID);
            }

            // check confirmationId
            if(!confirmationId.equalsIgnoreCase(Hash.hash(
                    tokenID + username + SystemProperties.get("am.encryption.pwd")))){
                debug.error("IdentityResource.anonymousUpdate(): Invalid confirmationId : "
                            + confirmationId);
                throw new BadRequestException("Invalid confirmationId", null);
            }
            // update Identity
            SSOToken tok = RestUtils.getToken();
            Token admin = new Token();
            admin.setId(tok.getTokenID().toString());

            // Update instance with new password value
            if (updateInstance(admin, jVal, realm, handler)) {
                // Only remove the token if the update was successful, errors will be set in the handler.
                try {
                    // Even though the generated token will eventually timeout, delete it after a successful read
                    // so that the reset password request cannot be made again using the same token.
                    CTSHolder.getCTS().delete(tokenID);
                } catch (DeleteFailedException e) {
                    // Catch this rather than letting it stop the process as it is possible that between successfully
                    // reading and deleting, the token has expired.
                    if (debug.messageEnabled()) {
                        debug.message("IdentityResource.anonymousUpdate(): Deleting token " + tokenID +
                                " after a successful read failed due to " + e.getMessage(), e);
                    }
                }
            }
        } catch (BadRequestException be){
            debug.error("IdentityResource.anonymousUpdate():" + be.getMessage());
            handler.handleError(be);
        } catch (CoreTokenException cte){
            debug.error("IdentityResource.anonymousUpdate():" + cte.getMessage());
            handler.handleError(new NotFoundException(cte.getMessage()));
        } catch (Exception e){
            debug.error("IdentityResource.anonymousUpdate():" + e.getMessage());
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
    private boolean updateInstance(Token admin, final JsonValue details, final String realm,
            final ResultHandler<JsonValue> handler){
        JsonValue jVal = details;
        IdentityDetails newDtls;
        IdentityServicesImpl idsvc;
        String resourceId = jVal.get("username").asString();

        boolean successfulUpdate = false;

        try {
            idsvc = new IdentityServicesImpl();
            newDtls = jsonValueToIdentityDetails(jVal, realm);
            newDtls.setName(resourceId);

            // update resource with new details
            UpdateResponse message = idsvc.update(newDtls, admin);
            // read updated identity back to client
            IdentityDetails checkIdent = idsvc.read(resourceId, getIdentityServicesAttributes(realm), admin);
            // handle updated resource
            handler.handleResult(identityDetailsToJsonValue(checkIdent));
            successfulUpdate = true;
        } catch (final Exception exception) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE! " +
                    exception);
            handler.handleError(new NotFoundException(exception.getMessage(), exception));
        }

        return successfulUpdate;
    }


    private void anonymousCreate(final ServerContext context, final ActionRequest request, final String realm,
            final ResultHandler<JsonValue> handler) {

        final JsonValue jVal = request.getContent();
        String tokenID = null;
        String confirmationId;
        String email;

        try{
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

            // Check Token is still in CTS
            // Check that tokenID is not expired
            if(CTSHolder.getCTS().read(tokenID) == null){
                throw new NotFoundException("Cannot find tokenID: " + tokenID);
            }

            // check confirmationId
            if(!confirmationId.equalsIgnoreCase(Hash.hash(
                    tokenID + email+ SystemProperties.get("am.encryption.pwd")))){
                debug.error("IdentityResource.anonymousCreate: Invalid confirmationId : "
                        + confirmationId);
                throw new BadRequestException("Invalid confirmationId", null);
            }

            // create an Identity
            SSOToken tok = RestUtils.getToken();
            Token admin = new Token();
            admin.setId(tok.getTokenID().toString());
            if (createInstance(admin, jVal, realm, handler)) {
                // Only remove the token if the create was successful, errors will be set in the handler.
                try {
                    // Even though the generated token will eventually timeout, delete it after a successful read
                    // so that the completed registration request cannot be made again using the same token.
                    CTSHolder.getCTS().delete(tokenID);
                } catch (DeleteFailedException e) {
                    // Catch this rather than letting it stop the process as it is possible that between successfully
                    // reading and deleting, the token has expired.
                    if (debug.messageEnabled()) {
                        debug.message("IdentityResource.anonymousCreate: Deleting token " + tokenID +
                                " after a successful read failed due to " + e.getMessage(), e);
                    }
                }
            }
        } catch (BadRequestException be){
            debug.error("IdentityResource.anonymousCreate() :: Invalid Parameter " + be);
            handler.handleError(be);
        } catch (NotFoundException nfe){
            debug.error("IdentityResource.anonymousCreate(): Invalid tokenID : " + tokenID);
            handler.handleError(nfe);
        } catch (Exception e){
            handler.handleError(new NotFoundException(e.getMessage()));
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
     *
     */
    private boolean createInstance(Token admin, final JsonValue details, final String realm,
            final ResultHandler<JsonValue> handler) {

        JsonValue jVal = details;
        IdentityDetails dtls, identity;
        IdentityServicesImpl idsvc;
        String resourceId = null;

        boolean successfulCreate = false;

        try {
            idsvc = new IdentityServicesImpl();
            identity = jsonValueToIdentityDetails(jVal, realm);
            resourceId = identity.getName();

            // Create the resource
            CreateResponse success = idsvc.create(identity, admin);
            // Read created resource
            dtls = idsvc.read(identity.getName(), getIdentityServicesAttributes(realm), admin);
            handler.handleResult(identityDetailsToJsonValue(dtls));
            successfulCreate = true;
        } catch (final ObjectNotFound notFound) {
            debug.error("IdentityResource.createInstance() :: Cannot READ " +
                    resourceId + ": Resource cannot be found." + notFound);
            handler.handleError(new NotFoundException("Resource not found.", notFound));
        } catch (final DuplicateObject duplicateObject) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    resourceId + ": Resource already exists!" + duplicateObject);
            handler.handleError(new NotFoundException("Resource already exists", duplicateObject));
        } catch (final TokenExpired tokenExpired) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        } catch (final NeedMoreCredentials needMoreCredentials) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    needMoreCredentials);
            handler.handleError(new ForbiddenException("Token is not authorized", needMoreCredentials));
        } catch (final Exception exception) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE! " +
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

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getRealm();

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
            identity = jsonValueToIdentityDetails(jVal, realm);
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
            dtls = idsvc.read(resourceId, getIdentityServicesAttributes(realm), admin);

            resource = new Resource(resourceId, "0", identityDetailsToJsonValue(dtls));
            handler.handleResult(resource);
        } catch (final ObjectNotFound notFound) {
            debug.error("IdentityResource.createInstance() :: Cannot READ " +
                    resourceId + ": Resource cannot be found." + notFound);
            handler.handleError(new NotFoundException("Resource not found.", notFound));
        } catch (final DuplicateObject duplicateObject) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    resourceId + ": Resource already exists!" + duplicateObject);
            handler.handleError(new NotFoundException("Resource already exists", duplicateObject));
        } catch (final TokenExpired tokenExpired) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        } catch (final NeedMoreCredentials needMoreCredentials) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    needMoreCredentials);
            handler.handleError(new ForbiddenException("Token is not authorized", needMoreCredentials));
        } catch(BadRequestException be) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    be);
            handler.handleError(be);
        } catch (final Exception exception) {
            debug.error("IdentityResource.createInstance() :: Cannot CREATE! " +
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

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getRealm();

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        Resource resource;
        IdentityDetails dtls;
        IdentityServicesImpl idsvc = new IdentityServicesImpl();

        try {

            // read to see if resource is available to user
            dtls = idsvc.read(resourceId, getIdentityServicesAttributes(realm), admin);

            // delete the resource
            DeleteResponse success = idsvc.delete(dtls, admin);

            result.put("success", "true");
            resource = new Resource(resourceId, "0", result);
            handler.handleResult(resource);

        } catch (final NeedMoreCredentials ex) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ": User does not have enough privileges.");
            handler.handleError(new ForbiddenException(resourceId, ex));
        } catch (final ObjectNotFound notFound) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ":" + notFound);
            handler.handleError(new NotFoundException("Resource cannot be found.", notFound));
        } catch (final TokenExpired tokenExpired) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        } catch (final AccessDenied accessDenied) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ":" + accessDenied);
            handler.handleError(new ForbiddenException(accessDenied.getMessage(), accessDenied));
        } catch (final GeneralFailure generalFailure) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    generalFailure.getMessage());
            handler.handleError(new BadRequestException(generalFailure.getMessage(), generalFailure));
        } catch (final Exception exception) {
            debug.error("IdentityResource.deleteInstance() :: Cannot DELETE! " +
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
            result.put("username", details.getName());
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
    private IdentityDetails jsonValueToIdentityDetails(final JsonValue jVal, final String realm) {

        IdentityDetails identity = new IdentityDetails();
        List<Attribute> identityAttrList = new ArrayList();

        try {
            identity.setType(userType); //set type ex. user
            identity.setRealm(realm); //set realm
            identity.setName(jVal.get("username").asString());//set name from JsonValue object

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
                debug.error("IdentityResource.jsonValueToIdentityDetails() :: " +
                        "Cannot Traverse JsonValue" + e);
            }
            Attribute[] attr = identityAttrList.toArray(new Attribute[identityAttrList.size()]);
            identity.setAttributes(attr);

        } catch (final Exception e) {
            debug.error("IdentityResource.jsonValueToIdentityDetails() ::" +
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

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getRealm();

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
            List<String> users = id.search(queryFilter, getIdentityServicesAttributes(realm), admin);

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

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getRealm();

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        IdentityServicesImpl idsvc;
        IdentityDetails dtls;
        Resource resource;

        try {
            idsvc = new IdentityServicesImpl();
            dtls = idsvc.read(resourceId, getIdentityServicesAttributes(realm), admin);
            resource = new Resource(resourceId, "0", identityDetailsToJsonValue(dtls));
            handler.handleResult(resource);
        } catch (final NeedMoreCredentials needMoreCredentials) {
            debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + needMoreCredentials);
            handler.handleError(new ForbiddenException("User does not have enough privileges.", needMoreCredentials));
        } catch (final ObjectNotFound objectNotFound) {
            debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + objectNotFound);
            handler.handleError(new NotFoundException("Resource cannot be found.", objectNotFound));
        } catch (final TokenExpired tokenExpired) {
            debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        } catch (final AccessDenied accessDenied) {
            debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + accessDenied);
            handler.handleError(new ForbiddenException(accessDenied.getMessage(), accessDenied));
        } catch (final GeneralFailure generalFailure) {
            debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    generalFailure);
            handler.handleError(new BadRequestException(generalFailure.getMessage(), generalFailure));
        } catch (final Exception exception) {
            debug.error("IdentityResource.readInstance() :: Cannot READ! " +
                    exception);
            handler.handleError(new NotFoundException(exception.getMessage(), exception));

        }
    }

    private boolean checkValidPassword(String username, char[] password, String realm) throws BadRequestException {
        if(username == null || password == null ){
            throw new BadRequestException("Invalid Username or Password");
        }
        try {
            AuthContext lc = new AuthContext(realm);
            lc.login();
            while (lc.hasMoreRequirements()) {
                Callback[] callbacks = lc.getRequirements();
                ArrayList missing = new ArrayList();
                // loop through the requires setting the needs..
                for (int i = 0; i < callbacks.length; i++) {
                    if (callbacks[i] instanceof NameCallback) {
                        NameCallback nc = (NameCallback) callbacks[i];
                        nc.setName(username);
                    } else if (callbacks[i] instanceof PasswordCallback) {
                        PasswordCallback pc = (PasswordCallback) callbacks[i];
                        pc.setPassword(password);
                    } else {
                        missing.add(callbacks[i]);
                    }
                }
                // there's missing requirements not filled by this
                if (missing.size() > 0) {
                    throw new BadRequestException("Insufficient Requirements");
                }
                lc.submitRequirements(callbacks);
            }

            // validate the password..
            if (lc.getStatus() == AuthContext.Status.SUCCESS) {
                lc.logout();
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {

        }
        return false;
    }

    private String getPasswordFromHeader(ServerContext context){
        List<String> headerList = null;
        String oldUserPasswordHeaderName = "olduserpassword";
        HttpContext header = null;

        try {
            header = context.asContext(HttpContext.class);
            if (header == null) {
                debug.error("IdentityResource.getPasswordFromHeader :: " +
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
            debug.error("IdentityResource.getPasswordFromHeader :: " +
                    "Cannot get olduserpassword from ServerContext!" + e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(final ServerContext context, final String resourceId, final UpdateRequest request,
            final ResultHandler<Resource> handler) {

        RealmContext realmContext = context.asContext(RealmContext.class);
        final String realm = realmContext.getRealm();

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        final JsonValue jVal = request.getNewContent();
        final String rev = request.getRevision();
        IdentityDetails dtls, newDtls;
        IdentityServicesImpl idsvc = new IdentityServicesImpl();
        Resource resource;
        try {
            // Retrieve details about user to be updated
            dtls = idsvc.read(resourceId, getIdentityServicesAttributes(realm), admin);
            // Continue modifying the identity if read success

            newDtls = jsonValueToIdentityDetails(jVal, realm);
            if(newDtls.getName() != null && !resourceId.equalsIgnoreCase(newDtls.getName())){
                throw new BadRequestException("id in path does not match id in request body");
            }
            newDtls.setName(resourceId);
            String userpass = jVal.get("userpassword").asString();
            // Check that the attribute userpassword is in the json object
            if(userpass != null && !userpass.isEmpty()) {
                // If so password reset attempt
                if(checkValidPassword(resourceId, userpass.toCharArray(), realm) || isAdmin(context)) {
                    // same password as before, update the attributes
                } else {
                    // check header to make sure that oldpassword is there check to see if it's correct
                    String strPass = getPasswordFromHeader(context);
                    if(strPass != null && !strPass.isEmpty() && checkValidPassword(resourceId, strPass.toCharArray(), realm)){
                        //continue will allow password change
                    } else{
                    	throw new BadRequestException("Invalid Password");
                    }
                }
            }

            // update resource with new details
            UpdateResponse message = idsvc.update(newDtls, admin);
            // read updated identity back to client
            IdentityDetails checkIdent = idsvc.read(dtls.getName(), getIdentityServicesAttributes(realm), admin);
            // handle updated resource
            resource = new Resource(resourceId, "0", identityDetailsToJsonValue(checkIdent));
            handler.handleResult(resource);
        } catch (final ObjectNotFound onf) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE! " +
                    onf);
            handler.handleError(new NotFoundException("Could not find the resource [ " + resourceId + " ] to update", onf));
        } catch (final NeedMoreCredentials needMoreCredentials) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    resourceId + ":" + needMoreCredentials);
            handler.handleError(new ForbiddenException("Token is not authorized", needMoreCredentials));
        } catch (final TokenExpired tokenExpired) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        } catch (final AccessDenied accessDenied) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    resourceId + ":" + accessDenied);
            handler.handleError(new ForbiddenException(accessDenied.getMessage(), accessDenied));
        } catch (final GeneralFailure generalFailure) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    generalFailure);
            handler.handleError(new BadRequestException(generalFailure.getMessage(), generalFailure));
        } catch (BadRequestException bre){
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE! "
                    + resourceId + ":" + bre);
            handler.handleError(bre);
        } catch (final Exception exception) {
            debug.error("IdentityResource.updateInstance() :: Cannot UPDATE! " +
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

    private String requestParamEncode(String toEncode) throws UnsupportedEncodingException {
        if (toEncode != null && !toEncode.isEmpty()) {
            return URLEncoder.encode(toEncode, "UTF-8").replace("+", "%20");
        } else {
            return toEncode;
        }
    }
}
