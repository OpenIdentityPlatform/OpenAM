/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: IdentityServicesImpl.java,v 1.20 2010/01/06 19:11:17 veiming Exp $
 *
 */

/**
 * Portions Copyrighted 2010-2013 ForgeRock AS
 */
package com.sun.identity.idsvcs.opensso;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idsvcs.InvalidToken;
import com.sun.identity.policy.PolicyException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idsvcs.AccessDenied;
import com.sun.identity.idsvcs.AccountExpired;
import com.sun.identity.idsvcs.CreateResponse;
import com.sun.identity.idsvcs.DeleteResponse;
import com.sun.identity.idsvcs.DuplicateObject;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.InvalidCredentials;
import com.sun.identity.idsvcs.InvalidPassword;
import com.sun.identity.idsvcs.LogResponse;
import com.sun.identity.idsvcs.LogoutResponse;
import com.sun.identity.idsvcs.MaximumSessionReached;
import com.sun.identity.idsvcs.NeedMoreCredentials;
import com.sun.identity.idsvcs.ObjectNotFound;
import com.sun.identity.idsvcs.OrgInactive;
import com.sun.identity.idsvcs.Token;
import com.sun.identity.idsvcs.UpdateResponse;
import com.sun.identity.idsvcs.UserDetails;
import com.sun.identity.idsvcs.UserInactive;
import com.sun.identity.idsvcs.UserLocked;
import com.sun.identity.idsvcs.UserNotFound;
import com.sun.identity.idsvcs.TokenExpired;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.log.AMLogException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.shared.Constants;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.idsvcs.ListWrapper;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;


import com.sun.identity.sm.SMSException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import com.sun.identity.shared.ldap.util.DN;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web Service to provide security based on authentication and authorization
 * support.
 */
public class IdentityServicesImpl
    implements com.sun.identity.idsvcs.IdentityServicesImpl
{
    // Debug
    private static Debug debug = Debug.getInstance("amIdentityServices");

    private static Pattern RESOURCE_PATTERN =
        Pattern.compile("service://(.+?)/\\?(resource=)?(.+)",
        Pattern.CASE_INSENSITIVE);

    /**
     * Attempt to authenticate using simple user/password credentials.
     * @param username Subject's user name.
     * @param password Subject's password
     * @param uri Subject's context such as module, organization, etc.
     * @return Subject's token if authenticated.
     * @throws UserNotFound if user not found.
     * @throws InvalidPassword if password is invalid.
     * @throws NeedMoreCredentials if additional credentials are needed for
     * authentication.
     * @throws InvalidCredentials if credentials are invalid.
     * @throws GeneralFailure on other errors.
     */
    public Token authenticate(String username, String password, String uri, String client)
        throws UserNotFound, InvalidPassword, NeedMoreCredentials,
        InvalidCredentials, OrgInactive, UserInactive, AccountExpired,
        UserLocked, GeneralFailure, MaximumSessionReached, RemoteException {


        assert username != null && password != null;
        Token ret = null;
        try {
            // Parse the URL to get realm, module, service, etc
            String realm = null;
            AuthContext.IndexType authIndexType = null;
            String authIndexValue = null;

            if (uri != null) {
                StringTokenizer st = new StringTokenizer(uri, "&");
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    int idx = s.indexOf("=");
                    if ((idx != -1) && (idx != (s.length() -1))) {
                        String k = s.substring(0, idx);
                        String v = s.substring(idx+1);
                        if (k.equals("realm") && (realm == null)) {
                            realm = v;
                        } else if (k.equals("module") &&
                            (authIndexType == null)) {
                            authIndexType =
                                AuthContext.IndexType.MODULE_INSTANCE;
                            authIndexValue = v;
                        } else if (k.equals("service") &&
                            (authIndexType == null)) {
                            authIndexType = AuthContext.IndexType.SERVICE;
                            authIndexValue = v;
                        }
                    }
                }
            }
            if (realm == null) {
                realm = "/";
            }

            AuthContext lc = new AuthContext(realm);
            if (client != null && !client.isEmpty()) {
                lc.setClientHostName(client);
            }
            if (authIndexType != null) {
                lc.login(authIndexType, authIndexValue);
            } else {
                lc.login();
            }
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
                        pc.setPassword(password.toCharArray());
                    } else {
                        missing.add(callbacks[i]);
                    }
                }
                // there's missing requirements not filled by this
                if (missing.size() > 0) {
                    // need add the missing later..
                    throw new InvalidCredentials("");
                }
                lc.submitRequirements(callbacks);
            }

            // Without this property defined the default will be false which is 
            // backwards compatable.
            boolean useGenericAuthenticationException =
                    Boolean.parseBoolean(SystemProperties.get(
                    Constants.GENERIC_SOAP_REST_AUTHENTICATION_EXCEPTION, "false"));

            if (debug.messageEnabled() && useGenericAuthenticationException) {
                debug.message("IdentityServicesImpl:authenticate returning an InvalidCredentials exception for invalid passwords.");
            }

            // validate the password..
            if (lc.getStatus() != AuthContext.Status.SUCCESS) {
                String ec = lc.getErrorCode();
                String em = lc.getErrorMessage();
                if(ec.equals(AMAuthErrorCode.AUTH_INVALID_PASSWORD)) {
                    if (useGenericAuthenticationException) {
                        // We can't use the error message as it is for invalid password
                        throw new InvalidCredentials("");
                    } else {
                        throw new InvalidPassword(em);
                    }
                } else if (ec.equals(AMAuthErrorCode.AUTH_PROFILE_ERROR) ||
                    ec.equals(AMAuthErrorCode.AUTH_USER_NOT_FOUND)) {
                    throw new UserNotFound(em);
                } else if (ec.equals(AMAuthErrorCode.AUTH_USER_INACTIVE)) {
                    throw new UserInactive(em);
                } else if (ec.equals(AMAuthErrorCode.AUTH_USER_LOCKED)) {
                    throw new UserLocked(em);
                } else if (ec.equals(AMAuthErrorCode.AUTH_ACCOUNT_EXPIRED)) {
                    throw new AccountExpired(em);
                } else if (ec.equals(AMAuthErrorCode.AUTH_LOGIN_FAILED)) {
                    if (useGenericAuthenticationException) {
                        // We can't use the error message to be consistent with the invalid password case                        
                        throw new InvalidCredentials("");
                    } else {
                        throw new InvalidCredentials(em);
                    }
                } else if (ec.equals(AMAuthErrorCode.AUTH_MAX_SESSION_REACHED)) {
                    throw new MaximumSessionReached(em);
                } else if (ec.equals(AMAuthErrorCode.AUTH_ERROR)) {
                    throw new GeneralFailure(em);
                }
            } else {
                try {
                    // package up the token for transport..
                    ret = new Token();
                    String id = lc.getSSOToken().getTokenID().toString();
                    ret.setId(id);
                } catch (Exception e) {
                    debug.error("IdentityServicesImpl:authContext: " +
                        "Unable to get SSOToken", e);
                    // we're going to throw a generic error
                    // because the system is likely down..
                    throw new GeneralFailure(e.getMessage());
                }
            }
        } catch (AuthLoginException le) {
            debug.error("IdentityServicesImpl:authContext AuthException", le);
            if (le.getErrorCode().equals(AMAuthErrorCode.AUTH_ORG_INACTIVE)) {
                throw new OrgInactive(le.getL10NMessage(Locale.getDefault()));
            }
            // we're going to throw a generic error
            // because the system is likely down..
            throw new GeneralFailure(le.getMessage());
        }
        return ret;
    }

    /**
     * Close session referenced by the subject token.
     * @param subject Token identifying the session to close.
     * @throws GeneralFailure errors.
     */
    public LogoutResponse logout(Token subject)
        throws GeneralFailure, RemoteException
    {
        try {
            SSOToken ssoToken = getSSOToken(subject);
            if (ssoToken != null) {
                AuthUtils.logout(ssoToken.getTokenID().toString(), null, null);
            }
        } catch (TokenExpired te) {
            debug.error("IdentityServicesImpl:logout : " + te.getMessage());
            throw new GeneralFailure(te.getMessage());
        } catch (SSOException ex) {
            debug.error("IdentityServicesImpl:logout : " + ex.getMessage());
            throw new GeneralFailure(ex.getMessage());
        }

        return new LogoutResponse();
    }

    /**
     * Attempt to authorize the subject for the optional action on the
     * requested URI.
     * @param uri URI for which authorization is required
     * @param action Optional action for which subject is being authorized
     * @param subject Token identifying subject to be authorized
     * @return boolean <code>true</code> if allowed; <code>false</code>
     * otherwise
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     */
    public boolean authorize(String uri, String action, Token subject)
        throws NeedMoreCredentials, TokenExpired, GeneralFailure,
        RemoteException {

        boolean isAllowed = false;
        // Check policy
        try {
            // create the SSOToken
            SSOToken ssoToken = getSSOToken(subject);

            // Evaluate policy
            String serviceName = "iPlanetAMWebAgentService";
            String resource = uri;
            // Check if service name is encoded in uri
            // Format of uri with service name:
            //   service://<sevicename>/?resource=<resourcename>
            Matcher matcher = RESOURCE_PATTERN.matcher(uri);
            if (matcher.matches()) {
                serviceName = matcher.group(1);
                resource = matcher.group(matcher.groupCount());
            }

            PolicyEvaluator pe = new PolicyEvaluator(serviceName);
            if ((action == null) || (action.length() == 0)) {
                action = "GET";
            }
            // Evaluate policy decisions
            if (pe.isAllowed(ssoToken, resource, action)) {
                isAllowed = true;
            }
        } catch (SSOException e) {
            debug.error("IdentityServicesImpl:authorize", e);
            throw new TokenExpired(e.getMessage());
        } catch (PolicyException ex) {
            debug.error("IdentityServicesImpl:authorize", ex);
            throw new GeneralFailure(ex.getMessage());
        }

        return isAllowed;
    }

    /**
     * Logs a message on behalf of the authenticated app.
     *
     * @param app         Token corresponding to the authenticated application.
     * @param subject     Optional token identifying the subject for which the
     * log record pertains.
     * @param logName     Identifier for the log file, e.g. "MyApp.access"
     * @param message     String containing the message to be logged
     * @throws AccessDenied   if app token is not specified
     * @throws GeneralFailure on error
     */
    public LogResponse log(Token app, Token subject, String logName,
        String message) throws AccessDenied, TokenExpired, GeneralFailure,
        RemoteException {
        if (app == null) {
            throw new AccessDenied("No logging application token specified");
        }

        SSOToken appToken = null;
        SSOToken subjectToken = null;
        appToken = getSSOToken(app);
        subjectToken = (subject == null) ? appToken : getSSOToken(subject);

        try {
            LogRecord logRecord = new LogRecord(
                java.util.logging.Level.INFO, message, subjectToken);
            // todo Support internationalization via a resource bundle
            // specification
            Logger logger = (Logger) Logger.getLogger(logName);
            logger.log(logRecord, appToken);
            logger.flush();
        } catch (AMLogException e) {
            debug.error("IdentityServicesImpl:log", e);
            throw new GeneralFailure(e.getMessage());
        }
        return new LogResponse();
    }


    /**
     * Retrieve user details (roles, attributes) for the subject.
     * @param attributeNames Optional array of attributes to be returned
     * @param subject Token for subject.
     * @return User details for the subject.
     * @throws TokenExpired when Token has expired.
     * @throws GeneralFailure on other errors.
     * @throws AccessDenied if reading of attributes for the user is disallowed.
     */
    public UserDetails attributes(String[] attributeNames, Token subject, Boolean refresh)
        throws TokenExpired, GeneralFailure, RemoteException, AccessDenied {
        List attrNames = null;
        if ((attributeNames != null) && (attributeNames.length > 0)) {
            attrNames = new ArrayList();
            attrNames.addAll(Arrays.asList(attributeNames));
        }
        return attributes(attrNames, subject, refresh);
    }

    /**
     * Retrieve user details (roles, attributes) for the subject.
     * @param attributeNames Optional list of attributes to be returned
     * @param subject Token for subject.
     * @return User details for the subject.
     * @throws TokenExpired when Token has expired.
     * @throws GeneralFailure on other errors.
     */
    public UserDetails attributes(List<String> attributeNames, Token subject, Boolean refresh)
        throws TokenExpired, GeneralFailure, RemoteException, AccessDenied {
        UserDetails details = new UserDetails();
        try {
            SSOToken ssoToken = getSSOToken(subject);
            if (refresh != null && refresh) {
                SSOTokenManager.getInstance().refreshSession(ssoToken);
            }
            Map<String, Set<String>> sessionAttributes = new HashMap<String, Set<String>>();
            Set<String> s = null;
            if (attributeNames != null) {
                String propertyNext = null;
                for (String attrNext : attributeNames) {
                    s = new HashSet<String>();
                    if (attrNext.equalsIgnoreCase("idletime")) {
                        s.add(Long.toString(ssoToken.getIdleTime()));
                    } else if (attrNext.equalsIgnoreCase("timeleft")) {
                        s.add(Long.toString(ssoToken.getTimeLeft()));
                    } else if (attrNext.equalsIgnoreCase("maxsessiontime")) {
                        s.add(Long.toString(ssoToken.getMaxSessionTime()));
                    } else if (attrNext.equalsIgnoreCase("maxidletime")) {
                        s.add(Long.toString(ssoToken.getMaxIdleTime()));
                    } else {
                        propertyNext = ssoToken.getProperty(attrNext);
                        s.add(propertyNext);
                    }
                    sessionAttributes.put(attrNext, s);
                }
            }
            // Obtain user memberships (roles and groups)
            AMIdentity userIdentity = IdUtils.getIdentity(ssoToken);
            if (isSpecialUser(userIdentity)) {
                throw new AccessDenied(
                    "Cannot retrieve attributes for this user.");
            }

            // Determine the types that can have members
            SSOToken adminToken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            AMIdentityRepository idrepo = new AMIdentityRepository(
                adminToken, userIdentity.getRealm());
            Set supportedTypes = idrepo.getSupportedIdTypes();
            Set membersTypes = new HashSet();
            for (Iterator its = supportedTypes.iterator(); its.hasNext();) {
                IdType type = (IdType) its.next();
                if (type.canHaveMembers().contains(userIdentity.getType())) {
                    membersTypes.add(type);
                }
            }

            // Determine the roles and groups
            List roles = new ArrayList();
            for (Iterator items = membersTypes.iterator(); items.hasNext();) {
                IdType type = (IdType) items.next();
                try {
                    Set mems = userIdentity.getMemberships(type);
                    for (Iterator rs = mems.iterator(); rs.hasNext();) {
                        AMIdentity mem = (AMIdentity) rs.next();
                        roles.add(mem.getUniversalId());
                    }
                } catch (IdRepoException ire) {
                    if (debug.messageEnabled()) {
                        debug.message("IdentityServicesImpl:attributes", ire);
                    }
                    // Ignore and continue
                }
            }
            String[] r = new String[roles.size()];
            details.setRoles((String[]) roles.toArray(r));

            Map userAttributes = null;
            if (attributeNames != null) {
                Set attrNames = new HashSet(attributeNames);
                userAttributes = userIdentity.getAttributes(attrNames);
            } else {
                userAttributes = userIdentity.getAttributes();
            }
            String name = null;
            Iterator it= null;
            Set value = null;
            if (userAttributes != null && sessionAttributes != null) {
                for (it = sessionAttributes.keySet().iterator();
                    it.hasNext();) {
                    name = (String) it.next();
                    value = (Set) sessionAttributes.get(name);
                    if (userAttributes.keySet().contains(name)) {
                       ((Set) userAttributes.get(name)).addAll(value);
                    } else {
                        userAttributes.put(name,value);
                    }
                }
            } else if (sessionAttributes != null) {
                userAttributes = sessionAttributes;
            }
            if (userAttributes != null) {
                List attributes = new ArrayList(userAttributes.size());
                for (it = userAttributes.keySet().iterator();
                    it.hasNext();) {
                    Attribute attribute = new Attribute();
                    name = it.next().toString();
                    attribute.setName(name);
                    value = (Set) userAttributes.get(name);
                    List valueList = new ArrayList(value.size());
                    // Convert the set to a List of String
                    if (value != null) {
                        for (Iterator valueIt = value.iterator();
                            valueIt.hasNext();) {
                            Object next = valueIt.next();
                            if (next != null) {
                                valueList.add(next.toString());
                            }
                        }
                    }
                    String[] v = new String[valueList.size()];
                    attribute.setValues((String[]) valueList.toArray(v));
                    attributes.add(attribute);
                }
                Attribute[] a = new Attribute[attributes.size()];
                details.setAttributes((Attribute[]) attributes.toArray(a));
            }
        } catch (IdRepoException e) {
            debug.error("IdentityServicesImpl:attributes", e);
            throw new GeneralFailure(e.getMessage());
        } catch (SSOException e) {
            debug.error("IdentityServicesImpl:attributes", e);
            throw new GeneralFailure(e.getMessage());
        }

        // todo handle token translation
        details.setToken(subject);
        return details;
    }


    /**
     * Retrieve a list of identities which match the input criteria.
     *
     * @param filter Optional filter to use as search against identity names.
     * @param admin Token identifying the administrator to be used to authorize
     * the request.
     * @param attributes Optional list of Attribute objects which provide
     * additional search criteria for the search.
     * @return List The list of identities matching the input criteria.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     */
    public String[] search(
        String filter,
        Attribute[] attributes,
        Token admin
    ) throws TokenExpired, GeneralFailure, RemoteException
    {
        String[] rv = null;
        List searchAttrsList = null;

        if ((attributes != null) && (attributes.length > 0)) {
            searchAttrsList = new ArrayList();
            searchAttrsList.addAll(Arrays.asList(attributes));
        }

        List identities = search(filter, searchAttrsList, admin);

        if (!identities.isEmpty()) {
            rv = new String[identities.size()];
            identities.toArray(rv);
        }

        return rv;
    }

    private String attractValues(
        String name,
        Map<String, Set<String>> map,
        String defaultValue
    ) {
        Set<String> set = map.get(name);
        if ((set != null) && !set.isEmpty()) {
            String value= set.iterator().next().trim();
            map.remove(name);
            return (value.length() == 0) ? defaultValue : value;
        } else {
            return defaultValue;
        }

    }

    public List search(String filter, List attributes, Token admin)
        throws TokenExpired, GeneralFailure, RemoteException
    {
        List rv = Collections.EMPTY_LIST;

        try {
            String realm = "/";
            String objectType = "User";
            Map searchModifiers = attributesToMap(attributes);
            if (searchModifiers != null) {
                realm = attractValues("realm", searchModifiers, "/");
                objectType = attractValues("objecttype", searchModifiers,
                    "User");
            }

            AMIdentityRepository repo = getRepo(admin, realm);
            IdType idType = getIdType(objectType);

            if ((idType != null)) {
                if ((filter == null) || (filter.length() == 0)) {
                    filter = "*";
                }

                List<AMIdentity> objList = fetchAMIdentities(
                    idType, filter, false, repo, searchModifiers);
                if ((objList != null) && !objList.isEmpty()) {
                    List<String> names = getNames(realm, idType, objList);

                    if (!names.isEmpty()) {
                        rv = new ArrayList();
                        for (String name : names) {
                            rv.add(name);
                        }
                    }
                }
            } else {
                debug.error("IdentityServicesImpl:search unsupported IdType" +
                    objectType);
                throw new GeneralFailure("search unsupported IdType: " +
                    objectType);
            }
        } catch (IdRepoException e) {
            debug.error("IdentityServicesImpl:search", e);
            throw new GeneralFailure(e.getMessage());
        } catch (SSOException e) {
            debug.error("IdentityServicesImpl:search", e);
            throw new GeneralFailure(e.getMessage());
        }

        return rv;
    }

    private List<String> getNames(
        String realm,
        IdType idType,
        List<AMIdentity> objList
    ) throws SSOException, IdRepoException {
        List<String> names = new ArrayList<String>();

        if ((objList != null) && !objList.isEmpty()) {
            for (AMIdentity identity : objList) {
                if (identityExists(identity)) {
                    names.add(identity.getName());
                }
            }
        }

        if (idType.equals(IdType.USER)) {
            Set<String> specialUserNames = getSpecialUserNames(realm);
            names.removeAll(specialUserNames);
        }
        return names;
    }

    private boolean isSpecialUser(AMIdentity identity) {
        Set<AMIdentity> specialUsers = getSpecialUsers(identity.getRealm());
        return (specialUsers != null) ? specialUsers.contains(identity) :
            false;
    }

    private Set<AMIdentity> getSpecialUsers(String realmName) {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction. getInstance());

        try {
            AMIdentityRepository repo = new AMIdentityRepository(
                adminToken, realmName);
            IdSearchResults results = repo.getSpecialIdentities(IdType.USER);
            return results.getSearchResults();
        } catch (IdRepoException e) {
            debug.warning("AMModelBase.getSpecialUsers", e);
        } catch (SSOException e) {
            debug.warning("AMModelBase.getSpecialUsers", e);
        }

        return Collections.EMPTY_SET;
    }

    private Set<String> getSpecialUserNames(String realmName) {
        Set<String> identities = new HashSet<String>();
        Set<AMIdentity> set = getSpecialUsers(realmName);
        if (set != null) {
            for (AMIdentity amid : set) {
                identities.add(amid.getName());
            }
        }
        return identities;
    }


    /**
     * Creates an identity object with the specified attributes.
     *
     * @param admin Token identifying the administrator to be used to authorize
     * the request.
     * @param identity object containing the attributes of the object
     * to be created.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws DuplicateObject if an object matching the name, type and
     * realm already exists.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     */
    public CreateResponse create(IdentityDetails identity, Token admin)
        throws NeedMoreCredentials, DuplicateObject,
        TokenExpired, GeneralFailure {

        // Verify valid information is provided
        assert (identity != null);
        assert (admin != null);

        // Obtain identity details & verify
        String idName = identity.getName();
        String idType = identity.getType();
        String realm = identity.getRealm();
        if ((idName == null) || (idName.length() < 1)) {
            // TODO: add a message to the exception
            throw new GeneralFailure("Identity name not provided");
        }
        if ((idType == null) || (idType.length() < 1)) {
            idType = "user";
        }
        if (realm == null) {
            realm = "/";
        }

        try {
            // Obtain IdRepo to create validate IdType & operations
            IdType objectIdType = getIdType(idType);
            AMIdentityRepository repo = getRepo(admin, realm);

            if (!isOperationSupported(repo, objectIdType, IdOperation.CREATE)) {
                // TODO: add message to exception
                throw new UnsupportedOperationException("Unsupported: " +
                    "Type: " + idType + " Operation: CREATE");
            }

            // Obtain creation attributes
            Map<String, Set> idAttrs = attributesToMap(
                identity.getAttributes());

            // Create the identity, special case of Agents to merge
            // and validate the attributes
            AMIdentity amIdentity = null;
            if (objectIdType.equals(IdType.AGENT) ||
                objectIdType.equals(IdType.AGENTONLY) ||
                objectIdType.equals(IdType.AGENTGROUP)) {
                // Get agenttype, serverurl & agenturl
                String agentType = null, serverUrl = null, agentUrl = null;
                /*
                 * To be backward compatible, look for 'AgentType' attribute
                 * in the attribute map which is passed as a parameter and if
                 * not present/sent, check if the IdType.AGENTONLY or AGENT
                 * and then assume that it is '2.2_Agent' type to create 
                 * that agent under the 2.2_Agent node.
                 */
                Set set = idAttrs.get("agenttype");
                if ((set != null) && !set.isEmpty()) {
                    agentType = set.iterator().next().toString();
                    idAttrs.remove("agenttype");
                } else if (objectIdType.equals(IdType.AGENTONLY) ||
                    objectIdType.equals(IdType.AGENT)) {
                    agentType = "2.2_Agent";
                } else {
                    throw new UnsupportedOperationException("Unsupported: " +
                        "Agent Type required for " + idType);
                }
                set = idAttrs.get("serverurl");
                if ((set != null) && !set.isEmpty()) {
                    serverUrl = set.iterator().next().toString();
                    idAttrs.remove("serverurl");
                }
                set = idAttrs.get("agenturl");
                if ((set != null) && !set.isEmpty()) {
                    agentUrl = set.iterator().next().toString();
                    idAttrs.remove("agenturl");
                }
                if (objectIdType.equals(IdType.AGENT) ||
                    objectIdType.equals(IdType.AGENTONLY)) {
                    if ((serverUrl == null) || (serverUrl.length() == 0) ||
                        (agentUrl == null) || (agentUrl.length() == 0)) {
                        AgentConfiguration.createAgent(getSSOToken(admin),
                            realm, idName, agentType, idAttrs);
                    } else {
                        AgentConfiguration.createAgent(getSSOToken(admin),
                            realm, idName, agentType, idAttrs, serverUrl,
                            agentUrl);
                    }
                } else {
                    if ((serverUrl == null) || (serverUrl.length() == 0) ||
                        (agentUrl == null) || (agentUrl.length() == 0)) {
                        AgentConfiguration.createAgentGroup(getSSOToken(admin),
                            realm, idName, agentType, idAttrs);
                    } else {
                        AgentConfiguration.createAgentGroup(getSSOToken(admin),
                            realm, idName, agentType, idAttrs, serverUrl,
                            agentUrl);
                    }
                }
            } else {
                // Create other identites like User, Group, Role, etc.
                amIdentity = repo.createIdentity(objectIdType, idName, idAttrs);

                // Process roles, groups & memberships
                if (objectIdType.equals(IdType.USER)) {
                    ListWrapper roles = identity.getRoleList();

                    if (roles != null && roles.getElements() != null && roles.getElements().length > 0) {
                        if (!isOperationSupported(repo, IdType.ROLE,
                            IdOperation.EDIT)) {
                            // TODO: localize message
                            throw new UnsupportedOperationException(
                                "Unsupported: " + "Type: " + IdType.ROLE +
                                " Operation: EDIT");
                        }

                        String[] roleNames = roles.getElements();
                        for (int i = 0; i < roleNames.length; i++) {
                            AMIdentity role = fetchAMIdentity(repo, IdType.ROLE,
                                roleNames[i], false);

                            if (identityExists(role)) {
                                role.addMember(amIdentity);
                                role.store();
                            }
                        }
                    }

                    ListWrapper groups = identity.getGroupList();

                    if (groups != null && groups.getElements() != null && groups.getElements().length > 0) {
                        if (!isOperationSupported(repo, IdType.GROUP,
                            IdOperation.EDIT)) {
                            // TODO: localize message
                            throw new UnsupportedOperationException(
                                "Unsupported: " + "Type: " + IdType.GROUP +
                                " Operation: EDIT");
                        }

                        String[] groupNames = groups.getElements();
                        for (int i = 0; i < groupNames.length; i++) {
                            AMIdentity group = fetchAMIdentity(repo,
                                IdType.GROUP, groupNames[i], false);
                            if (identityExists(group)) {
                                group.addMember(amIdentity);
                                group.store();
                            }
                        }
                    }
                }

                if (objectIdType.equals(IdType.GROUP) ||
                    objectIdType.equals(IdType.ROLE)) {
                    ListWrapper members = identity.getMemberList();

                    if (members != null) {
                        if (objectIdType.equals(IdType.GROUP) &&
                            !isOperationSupported(repo, IdType.GROUP,
                            IdOperation.EDIT)) {
                            // TODO: Add message to exception
                            throw new NeedMoreCredentials("");
                        }

                        if (objectIdType.equals(IdType.ROLE) &&
                            !isOperationSupported(repo, IdType.ROLE,
                            IdOperation.EDIT)) {
                            // TODO: Add message to exception
                            throw new NeedMoreCredentials("");
                        }

                        String[] memberNames = members.getElements();
                        for (int i = 0; i < memberNames.length; i++) {
                            AMIdentity user = fetchAMIdentity(repo, IdType.USER,
                                memberNames[i], false);
                            if (identityExists(user)) {
                                amIdentity.addMember(user);
                            }
                        }
                        amIdentity.store();
                    }
                }
            }
        } catch (IdRepoException ex) {
            debug.error("IdentityServicesImpl:create", ex);
            throw new GeneralFailure(ex.getMessage());
        } catch (SSOException ex) {
            debug.error("IdentityServicesImpl:create", ex);
            throw new GeneralFailure(ex.getMessage());
        } catch (SMSException ex) {
            debug.error("IdentityServicesImpl:create", ex);
            throw new GeneralFailure(ex.getMessage());
        } catch (ConfigurationException ex) {
            debug.error("IdentityServicesImpl:create", ex);
            throw new GeneralFailure(ex.getMessage());
        } catch (MalformedURLException ex) {
            debug.error("IdentityServicesImpl:create", ex);
            throw new GeneralFailure(ex.getMessage());
        }

        return new CreateResponse();
    }


    /**
     * Retrieves an identity object matching input criteria.
     *
     * @param name The name of identity to retrieve.
     * @param attributes Attribute objects specifying criteria for the object
     * to retrieve.
     * @param admin Token identifying the administrator to be used to authorize
     * the request.
     * @return IdentityDetails of the subject.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws ObjectNotFound if no subject is found that matches the input
     * criteria.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     * @throws AccessDenied if reading of attributes for the user is disallowed.
     */
    public IdentityDetails read(String name, Attribute[] attributes, Token admin)
        throws NeedMoreCredentials, ObjectNotFound, TokenExpired,
        GeneralFailure, AccessDenied
    {
        List attrList = null;

        if ((attributes != null) && (attributes.length > 0)) {
            attrList = new ArrayList();
            attrList.addAll(Arrays.asList(attributes));
        }

        return read(name, attrList, admin);
    }

    public IdentityDetails read(String name, List attributes, Token admin)
        throws NeedMoreCredentials, ObjectNotFound, TokenExpired,
        GeneralFailure, AccessDenied
    {
        IdentityDetails rv = null;
        String realm = null;
        String repoRealm = null;
        String identityType = null;
        List attrsToGet = null;

        if (attributes != null) {
            for (int i = 0; i < attributes.size(); i++) {
                Attribute attr = (Attribute)attributes.get(i);
                String attrName = attr.getName();

                if ("realm".equalsIgnoreCase(attrName)) {
                    String[] values = attr.getValues();

                    if ((values != null) && (values.length > 0)) {
                        realm = values[0];
                    }
                } else if ("objecttype".equalsIgnoreCase(attrName)) {
                    String[] values = attr.getValues();

                    if ((values != null) && (values.length > 0)) {
                        identityType = values[0];
                    }
                } else {
                    if (attrsToGet == null) {
                        attrsToGet = new ArrayList();
                    }

                    attrsToGet.add(attrName);
                }
            }
        }

        if ((realm == null) || (realm.length() < 1)) {
            repoRealm = "/";
        } else {
            repoRealm = realm;
        }

        if ((identityType == null) || (identityType.length() < 1)) {
            identityType = "User";
        }

        try {
            AMIdentity amIdentity = getAMIdentity(admin, identityType, name,
                                                  repoRealm);

            if (!identityExists(amIdentity)) {
                debug.error("IdentityServicesImpl:read identity not found");
                throw new ObjectNotFound(name);
            }

            if (isSpecialUser(amIdentity)) {
                throw new AccessDenied(
                        "Cannot retrieve attributes for this user.");
            }

            rv = convertToIdentityDetails(amIdentity, attrsToGet);

            if ((realm != null) && (realm.length()> 0)) {
                // use the realm specified by the request
                rv.setRealm(realm);
            }
        } catch (IdRepoException e) {
            debug.error("IdentityServicesImpl:read", e);
            mapIdRepoException(e);
        } catch (SSOException e) {
            debug.error("IdentityServicesImpl:read", e);
            throw new GeneralFailure(e.getMessage());
        }

        return rv;
    }

    /**
     * Updates an identity object with the specified attributes.
     *
     * @param admin Token identifying the administrator to be used to authorize
     * the request.
     * @param identity object containing the attributes of the object to be
     * updated.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws ObjectNotFound if an object matching the name, type and realm
     * cannot be found.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     * @throws AccessDenied if reading of attributes for the user is disallowed
     */
    public UpdateResponse update(IdentityDetails identity, Token admin)
        throws NeedMoreCredentials, ObjectNotFound, TokenExpired,
        GeneralFailure, AccessDenied
    {
        String idName = identity.getName();
        String idType = identity.getType();
        String realm = identity.getRealm();

        if ((idName == null) || (idName.length() < 1)) {
            // TODO: add a message to the exception
            throw new GeneralFailure("");
        }

        if ((idType == null) || (idType.length() < 1)) {
            idType="user";
        }

        if (realm == null) {
            realm = "";
        }

        try {
            IdType objectIdType = getIdType(idType);
            AMIdentityRepository repo = getRepo(admin, realm);

            if (!isOperationSupported(repo, objectIdType, IdOperation.EDIT)) {
                // TODO: add message to exception
                throw new NeedMoreCredentials("");
            }

            AMIdentity amIdentity = getAMIdentity(getSSOToken(admin),
                repo, idType, idName);

            if (!identityExists(amIdentity)) {
                String msg = "Object \'" + idName + "\' of type \'" +
                    idType + "\' not found.'";
                throw new ObjectNotFound(msg);
            }

            if (isSpecialUser(amIdentity)) {
                throw new AccessDenied(
                    "Cannot update attributes for this user.");
            }

            Attribute[] attrs = identity.getAttributes();

            if ((attrs != null) && (attrs.length > 0)) {
                Map idAttrs = null;
                Set removeAttrs = null;

                for (int i = 0; i < attrs.length; i++) {
                    String attrName = attrs[i].getName();
                    String[] attrValues = attrs[i].getValues();

                    if ((attrValues != null) && (attrValues.length > 0)) {
                        // attribute to add or modify
                        Set idAttrValues = new HashSet(attrValues.length);
                        idAttrValues.addAll(Arrays.asList(attrValues));

                        if (idAttrs == null) {
                            idAttrs = new HashMap();
                        }

                        idAttrs.put(attrName, idAttrValues);
                    } else {
                        // attribute to remove
                        if (removeAttrs == null) {
                            removeAttrs = new HashSet();
                        }

                        removeAttrs.add(attrName);
                    }
                }

                boolean storeNeeded = false;
                if (idAttrs != null) {
                    amIdentity.setAttributes(idAttrs);
                    storeNeeded = true;
                }

                if (removeAttrs != null) {
                    amIdentity.removeAttributes(removeAttrs);
                    storeNeeded = true;
                }

                if (storeNeeded) {
                    // throws IdRepoException, SSOException
                    amIdentity.store();
                }
            }

            if (objectIdType.equals(IdType.USER)) {
                ListWrapper roles = identity.getRoleList();

                if (roles != null) {
                    String[] roleNames = roles.getElements();
                    Set roleMemberships = new HashSet(roleNames.length);
                    roleMemberships.addAll(Arrays.asList(roleNames));

                    setMemberships(repo, amIdentity, roleMemberships, IdType.ROLE);
                }

                ListWrapper groups = identity.getGroupList();

                if (groups != null) {
                    String[] groupNames = groups.getElements();
                    Set groupMemberships = new HashSet(groupNames.length);
                    groupMemberships.addAll(Arrays.asList(groupNames));

                    setMemberships(repo, amIdentity, groupMemberships, IdType.GROUP);
                }
            }

            if (objectIdType.equals(IdType.GROUP) ||
                objectIdType.equals(IdType.ROLE))
            {
                ListWrapper members = identity.getMemberList();

                if (members != null) {
                    String[] memberNames = members.getElements();
                    Set memberships = new HashSet(memberNames.length);
                    memberships.addAll(Arrays.asList(memberNames));

                    setMembers(repo, amIdentity, memberships, IdType.USER);
                }
            }
        } catch (IdRepoException ex) {
            debug.error("IdentityServicesImpl:update", ex);
            mapIdRepoException(ex);
        } catch (SSOException ex) {
            debug.error("IdentityServicesImpl:update", ex);
            throw new GeneralFailure(ex.getMessage());
        }

        return new UpdateResponse();
    }

    /**
     * Maps a IdRepoException to appropriate exceptioin.
     *
     * @param exception IdRepoException that needs to be mapped
     * @return boolean true if the identity object was deleted.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws ObjectNotFound if no subject is found that matches the input criteria.
     * @throws TokenExpired when subject's token has expired.
     * @throws AccessDenied when permission to preform action is denied
     * @throws GeneralFailure on other errors.
     */
    private void mapIdRepoException(IdRepoException exception) throws NeedMoreCredentials,
            ObjectNotFound, TokenExpired, GeneralFailure, AccessDenied {
        if(exception.getErrorCode().equalsIgnoreCase("402")){
            throw new AccessDenied(exception.getMessage());
        } else{
            throw new GeneralFailure(exception.getMessage());
        } //Need to add other cases when found

    }

    /**
     * Deletes an identity object matching input criteria.
     *
     * @param admin Token identifying the administrator to be used to authorize
     * the request.
     * @param identity Identity Details of the Subject
     * @return boolean true if the identity object was deleted.
     * @throws NeedMoreCredentials when more credentials are required for
     * authorization.
     * @throws ObjectNotFound if no subject is found that matches the input criteria.
     * @throws TokenExpired when subject's token has expired.
     * @throws GeneralFailure on other errors.
     */
    public DeleteResponse delete(IdentityDetails identity, Token admin)
        throws NeedMoreCredentials, ObjectNotFound, TokenExpired,
        GeneralFailure, AccessDenied
    {
        if (identity == null) {
            throw new GeneralFailure("delete failed: identity object not specified.");
        }

        String name = identity.getName();
        String identityType = identity.getType();
        String realm = identity.getRealm();

        if (name == null) {
            throw new ObjectNotFound("delete failed: null object name.");
        }

        if (realm == null) {
            realm = "/";
        }

        try {
            AMIdentity amIdentity = getAMIdentity(admin, identityType, name,
                                                  realm);

            if (identityExists(amIdentity)) {
                if (isSpecialUser(amIdentity)) {
                    throw new AccessDenied("Cannot delete user.");
                }

                AMIdentityRepository repo = getRepo(admin, realm);
                IdType idType = amIdentity.getType();

                if (idType.equals(IdType.GROUP) || idType.equals(IdType.ROLE)) {
                    // First remove users from memberships
                    Set members = getMembers(amIdentity, IdType.USER);
                    Iterator iter = members.iterator();

                    while (iter.hasNext()) {
                        try {
                            AMIdentity member = (AMIdentity)iter.next();
                            removeMember(repo, amIdentity, member);
                        } catch (IdRepoException ex) {
                            //ignore this, member maybe already removed.
                        }
                    }
                }

                deleteAMIdentity(repo, amIdentity);
            } else {
                String msg = "Object \'" + name + "\' of type \'" + identityType +
                             "\' was not found.";

                throw new ObjectNotFound(msg);
            }
        } catch (IdRepoException ex) {
            debug.error("IdentityServicesImpl:delete", ex);
            mapIdRepoException(ex);
        } catch (SSOException ex) {
            debug.error("IdentityServicesImpl:delete", ex);
            throw new GeneralFailure(ex.getMessage());
        }

        return new DeleteResponse();
    }

    private boolean identityExists(AMIdentity amIdentity)
        throws SSOException, IdRepoException
    {
        return ((amIdentity != null) && amIdentity.isExists());
    }

    private AMIdentityRepository getRepo(SSOToken token, String realm)
    	throws IdRepoException, TokenExpired
    {
    	if ((realm == null) || (realm.length() == 0)) {
    		realm = "/";
    	}
        try {
    	    return new AMIdentityRepository(token, realm);
        } catch (SSOException ssoe) {
            throw (new TokenExpired(ssoe.getMessage()));
        }
    }

    private AMIdentityRepository getRepo(Token admin, String realm)
    	throws IdRepoException, TokenExpired
    {
    	SSOToken ssoToken = getSSOToken(admin);
    	return getRepo(ssoToken, realm);
    }

    private IdType getIdType(String objectType)
    {
        try {
            return (com.sun.identity.idm.IdUtils.getType(objectType));
        } catch (IdRepoException ioe) {
            // Ignore exception
        }
        return (null);
    }

    private boolean isOperationSupported(AMIdentityRepository repo,
        IdType idType, IdOperation operation)
    {
        boolean rv = false;

        try {
            Set ops = repo.getAllowedIdOperations(idType);

            if (ops != null) {
                rv = ops.contains(operation);
            }
        } catch (IdRepoException ex) {
            // Ignore
        } catch (SSOException ex) {
            // Ignore
        }

        return rv;
    }

    private Set getMembers(AMIdentity amIdentity, IdType type)
        throws IdRepoException, SSOException
    {
        return amIdentity.getMembers(type);
    }

    private Set getMemberNames(AMIdentity amIdentity, IdType type)
        throws IdRepoException, SSOException
    {
        Set rv = null;
        Set members = getMembers(amIdentity, type);

        if (members != null) {
            rv = new HashSet();

            Iterator iter = members.iterator();
            while (iter.hasNext()) {
                AMIdentity nextIdentity = (AMIdentity)iter.next();

                rv.add(nextIdentity.getName());
            }
        }

        return rv;
    }

    private List fetchAMIdentities(IdType type, String identity,
                                   boolean fetchAllAttrs,
                                   AMIdentityRepository repo,
                                   Map searchModifiers)
        throws GeneralFailure, IdRepoException
    {
        IdSearchControl searchControl = new IdSearchControl();
        IdSearchResults searchResults = null;
        List identities = null;

        if (isOperationSupported(repo, type, IdOperation.READ)) {
            try {
                Set resultSet;

                if (fetchAllAttrs) {
                    searchControl.setAllReturnAttributes(true);
                } else {
                    searchControl.setAllReturnAttributes(false);
                }

                if (searchModifiers != null) {
                    searchControl.setSearchModifiers(IdSearchOpModifier.AND,
                                                     searchModifiers);
                }

                searchResults = repo.searchIdentities(type, identity, searchControl);
                resultSet = searchResults.getSearchResults();
                identities = new ArrayList(resultSet);
            } catch (Exception e) {
                debug.error("IdentityServicesImpl:fetchAMIdentities", e);
                throw new GeneralFailure(e.getMessage());
            }
        } else {
            // A list is expected back
        	/*
        	 * TODO: throw an exception instead of returning an empty list
        	 */
            identities = new ArrayList();
        }

        return identities;
    }

    private AMIdentity fetchAMIdentity(AMIdentityRepository repo, IdType type,
                                       String identity, boolean fetchAllAttrs)
        throws GeneralFailure, IdRepoException
    {
        AMIdentity rv = null;
        List identities = fetchAMIdentities(type, identity,
            fetchAllAttrs, repo, null);

        if ((identities != null) && (identities.size() > 0)) {
            rv = (AMIdentity)identities.get(0);
        }

        return rv;
    }

    private AMIdentity getAMIdentity(SSOToken ssoToken,
        AMIdentityRepository repo, String guid, IdType idType)
        throws IdRepoException, SSOException
    {
        AMIdentity rv = null;

        if (isOperationSupported(repo, idType, IdOperation.READ)) {
            try {
                if (DN.isDN(guid)) {
                    rv = new AMIdentity(ssoToken, guid);
                } else {
                    rv = new AMIdentity(ssoToken, guid, idType,
                        repo.getRealmIdentity().getRealm(), null);
                }
            } catch (IdRepoException ex) {
                String errCode = ex.getErrorCode();

                // If it is error code 215, ignore the error as this indicates
                // an invalid uid.
                if (!"215".equals(errCode)) {
                    throw ex;
                }
            }
        }

        return rv;
    }

    private AMIdentity getAMIdentity(Token admin, String objectType, String id,
                                     String realm)
        throws GeneralFailure, IdRepoException, TokenExpired
    {
        SSOToken ssoToken = getSSOToken(admin);
        AMIdentityRepository repo = getRepo(ssoToken, realm);
        try {
            return getAMIdentity(ssoToken, repo, objectType, id);
        } catch (SSOException ssoe) {
            throw (new TokenExpired(ssoe.getMessage()));
        }
    }

    private AMIdentity getAMIdentity(SSOToken ssoToken,
        AMIdentityRepository repo, String objectType, String id)
    	throws GeneralFailure, IdRepoException, SSOException
    {
    	AMIdentity rv = null;
    	IdType idType = null;

    	if (objectType != null) {
            idType = getIdType(objectType);
        }

        if (idType != null) {
            // First assume id is a universal id
            rv = getAMIdentity(ssoToken, repo, id, idType);

            if (!identityExists(rv)) {
                // Not found through id lookup, try name lookup
                rv = fetchAMIdentity(repo, idType, id, true);
            }
        } else {
        }

    	return rv;
    }

    private void addMember(AMIdentityRepository repo, AMIdentity amIdentity,
                           AMIdentity member)
        throws NeedMoreCredentials, IdRepoException, SSOException
    {
        if (!member.isMember(amIdentity)) {
            if (isOperationSupported(repo, amIdentity.getType(),
                IdOperation.EDIT)) {
                amIdentity.addMember(member);
            } else {
                // TODO: Add message to exception
                throw new NeedMoreCredentials("");
            }
        }
    }

    private void removeMember(AMIdentityRepository repo, AMIdentity amIdentity,
    	AMIdentity member)
        throws NeedMoreCredentials, IdRepoException, SSOException
    {
        if (member.isMember(amIdentity)) {
            IdType type = amIdentity.getType();

            if (isOperationSupported(repo, type, IdOperation.EDIT)) {
                amIdentity.removeMember(member);
            } else {
            	// TODO: Add message to exception
            	throw new NeedMoreCredentials("");
            }
        }
    }

    private void deleteAMIdentities(AMIdentityRepository repo,
    	IdType type, Set identities)
    	throws NeedMoreCredentials, IdRepoException, SSOException
    {
    	if (isOperationSupported(repo, type, IdOperation.DELETE)) {
    		repo.deleteIdentities(identities);
    	} else {
    		// TODO: add message to exception
    		throw new NeedMoreCredentials("");
        }
    }

    private void deleteAMIdentity(AMIdentityRepository repo,
    	AMIdentity amIdentity)
    	throws NeedMoreCredentials, IdRepoException, SSOException
    {
    	Set identities = new HashSet();

    	identities.add(amIdentity);
    	deleteAMIdentities(repo, amIdentity.getType(), identities);
    }

    private Set getMemberships(AMIdentity amIdentity, IdType idType)
        throws SSOException
    {
        Set memberships;

        try {
            memberships = amIdentity.getMemberships(idType);
        } catch (IdRepoException ex) {
            // This can be thrown if the identity is not a member
            // in any object of idType.
            memberships = new HashSet(0);
        }

        return memberships;
    }

    private Set getMembershipNames(AMIdentity amIdentity, IdType idType)
        throws SSOException
    {
        Set rv = null;
        Set memberships = getMemberships(amIdentity, idType);

        rv = new HashSet(memberships.size());

        Iterator iter = memberships.iterator();
        while (iter.hasNext()) {
            AMIdentity container = (AMIdentity)iter.next();

            rv.add(container.getName());
        }

        return rv;
    }

    private void setMemberships(AMIdentityRepository repo,
        AMIdentity amIdentity, Set memberships, IdType idType)
        throws IdRepoException, SSOException, GeneralFailure,
        NeedMoreCredentials
    {
        Set membershipsToAdd = memberships;
        Set membershipsToRemove = null;
        Set currentMemberships = getMembershipNames(amIdentity, idType);

        if ((currentMemberships != null) && (currentMemberships.size() > 0)) {
            membershipsToRemove = removeAllIgnoreCase(currentMemberships,
                                                      memberships);
            membershipsToAdd = removeAllIgnoreCase(memberships,
                                                   currentMemberships);
        }

        if (membershipsToRemove != null) {
            Iterator it = membershipsToRemove.iterator();

            while (it.hasNext()) {
                String idName = (String)it.next();

                AMIdentity container = fetchAMIdentity(repo, idType, idName, false);

                removeMember(repo, container, amIdentity);
            }
        }

        if (membershipsToAdd != null) {
            Iterator it = membershipsToAdd.iterator();

            while (it.hasNext()) {
                String idName = (String)it.next();

                AMIdentity container = fetchAMIdentity(repo, idType, idName, false);

                addMember(repo, container, amIdentity);
            }
        }
    }

    private void setMembers(AMIdentityRepository repo, AMIdentity amIdentity,
        Set members, IdType idType) throws IdRepoException, SSOException,
        GeneralFailure, NeedMoreCredentials
    {
        Set membershipsToAdd = members;
        Set membershipsToRemove = null;
        Set currentMembers = getMemberNames(amIdentity, idType);

        if ((currentMembers != null) && (currentMembers.size() > 0)) {
            membershipsToRemove = removeAllIgnoreCase(currentMembers, members);
            membershipsToAdd = removeAllIgnoreCase(members, currentMembers);
        }

        if (membershipsToRemove != null) {
            Iterator iter = membershipsToRemove.iterator();

            while (iter.hasNext()) {
                String memberName = (String)iter.next();
                AMIdentity identity = fetchAMIdentity(repo, idType,
                                                      memberName, false);

                if (identity != null) {
                    removeMember(repo, amIdentity, identity);
                }
            }
        }

        if (membershipsToAdd != null) {
            Iterator iter = membershipsToAdd.iterator();

            while (iter.hasNext()) {
                String memberName = (String)iter.next();
                AMIdentity identity = fetchAMIdentity(repo, idType,
                                                      memberName, false);

                if (identity != null) {
                    addMember(repo, amIdentity, identity);
                }
            }
        }
    }

    private Set removeAllIgnoreCase(Set src, Set removeSet)
    {
        Set result;

        if ((src == null) || (src.size() < 1)) {
            result = new HashSet(0);
        } else {
            result = new HashSet(src);

            if ((removeSet != null) && (removeSet.size() > 0)) {
                Map upcaseSrc = new HashMap(src.size());
                Iterator it;

                it = src.iterator();

                String s;
                while (it.hasNext()) {
                    s = (String)it.next();
                    upcaseSrc.put(s.toUpperCase(), s);
                }

                it = removeSet.iterator();
                while (it.hasNext()) {
                    s = (String)upcaseSrc.get(((String)it.next()).toUpperCase());

                    if (s != null) {
                        result.remove(s);
                    }
                }
            }
        }

        return result;
    }

    private IdentityDetails convertToIdentityDetails(AMIdentity amIdentity,
                                                     List attrList)
        throws IdRepoException, SSOException
    {
        IdentityDetails rv = null;

        if (amIdentity != null) {
            IdType idType = amIdentity.getType();
            Map attrs = null;
            boolean addUniversalId = false;

            rv = new IdentityDetails();
            rv.setName(amIdentity.getName());
            rv.setType(amIdentity.getType().getName());
            rv.setRealm(amIdentity.getRealm());

            if (IdType.USER.equals(idType)) {
                Set roles = amIdentity.getMemberships(IdType.ROLE);

                if ((roles != null) && (roles.size() > 0)) {
                    AMIdentity[] rolesFound = new AMIdentity[roles.size()];
                    String[] roleNames = new String[rolesFound.length];

                    roles.toArray(rolesFound);

                    for (int i = 0; i < rolesFound.length; i++) {
                        roleNames[i] = rolesFound[i].getName();
                    }

                    rv.setRoleList(new ListWrapper(roleNames));
                }

                Set groups = amIdentity.getMemberships(IdType.GROUP);

                if ((groups != null) && (groups.size() > 0)) {
                    AMIdentity[] groupsFound = new AMIdentity[groups.size()];
                    String[] groupNames = new String[groupsFound.length];

                    groups.toArray(groupsFound);

                    for (int i = 0; i < groupsFound.length; i++) {
                        groupNames[i] = groupsFound[i].getName();
                    }

                    rv.setGroupList(new ListWrapper(groupNames));
                }
            }

            if (IdType.GROUP.equals(idType) || IdType.ROLE.equals(idType)) {
                Set members = amIdentity.getMembers(IdType.USER);

                if ((members != null) && (members.size() > 0)) {
                    AMIdentity[] membersFound = new AMIdentity[members.size()];
                    String[] memberNames = new String[membersFound.length];

                    members.toArray(membersFound);

                    for (int i = 0; i < membersFound.length; i++) {
                        memberNames[i] = membersFound[i].getName();
                    }

                    rv.setMemberList(new ListWrapper(memberNames));
                }
            }

            if (attrList != null) {
                Set attrNames = new HashSet();
                Iterator attrIter = attrList.iterator();

                while (attrIter.hasNext()) {
                    String attrName = (String)attrIter.next();

                    if ("universalid".equalsIgnoreCase(attrName)) {
                        addUniversalId = true;
                    } else {
                        if (!attrNames.contains(attrName)) {
                            attrNames.add(attrName);
                        }
                    }
                }

                attrs = amIdentity.getAttributes(attrNames);
            } else {
                attrs = amIdentity.getAttributes();
                addUniversalId = true;
            }

            if (addUniversalId) {
                if (attrs == null) {
                    attrs = new HashMap();
                }

                Set uidValue = new HashSet();

                uidValue.add(amIdentity.getUniversalId());
                attrs.put("universalid", uidValue);
            }

            if (attrs != null) {
                List attributes = new ArrayList(attrs.size());
                Iterator keyIter = attrs.keySet().iterator();

                while (keyIter.hasNext()) {
                    String key = keyIter.next().toString();

                    Attribute attribute = new Attribute();
                    attribute.setName(key);

                    Set value = (Set)attrs.get(key);
                    List valueList = null;

                    // Convert the set to a List of String
                    if (value != null) {
                        valueList = new ArrayList(value.size());

                        Iterator valueIter = value.iterator();
                        while (valueIter.hasNext()) {
                            Object next = valueIter.next();

                            if (next != null) {
                                valueList.add(next.toString());
                            }
                        }
                    }

                    if (valueList != null) {
                        String[] valueArray = new String[valueList.size()];

                        attribute.setValues(
                            (String[])valueList.toArray(valueArray));
                        attributes.add(attribute);
                    }
                }

                Attribute[] attrArray = new Attribute[attributes.size()];

                rv.setAttributes((Attribute[])attributes.toArray(attrArray));
            }
        }

        return rv;
    }

    private SSOToken getSSOToken(Token admin) throws TokenExpired {
        SSOToken token = null;
        try {
            if (admin == null) {
                throw (new TokenExpired("Token is NULL"));
            }
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            token = mgr.createSSOToken(admin.getId());
        } catch (SSOException ex) {
            // throw TokenExpired exception
            throw (new TokenExpired(ex.getMessage()));
        }
        return (token);
    }

    private Map<String, Set> attributesToMap(List attrs) {
        if (attrs != null) {
            Attribute[] attributes = new Attribute[attrs.size()];
            attributes = (Attribute[]) attrs.toArray(attributes);
            return (attributesToMap(attributes));
        }
        return (Collections.EMPTY_MAP);
    }

    private Map<String, Set> attributesToMap(Attribute[] attrs) {
        Map<String, Set> idAttrs = new CaseInsensitiveHashMap();
        if (attrs != null) {
            for (int i = 0; i < attrs.length; i++) {
                String attrName = attrs[i].getName();
                String[] attrValues = attrs[i].getValues();
                Set idAttrValues = null;
                if ((attrValues != null) && (attrValues.length > 0)) {
                    idAttrValues = new HashSet(attrValues.length);
                    idAttrValues.addAll(Arrays.asList(attrValues));
                } else {
                    idAttrValues = Collections.EMPTY_SET;
                }
                idAttrs.put(attrName, idAttrValues);
            }
        } else {
            idAttrs = new HashMap(1);
        }
        return (idAttrs);
    }

    public boolean isTokenValid(Token token)
        throws InvalidToken, GeneralFailure, TokenExpired, RemoteException {
        // Validate the token
        if (token == null) {
            return (false);
        }
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            SSOToken t = mgr.createSSOToken(token.getId());
            mgr.validateToken(t);
            mgr.refreshSession(t);
        } catch (SSOException e) {
            // Token is not valid
            debug.message("IdentityServicesImpl.isTokenValid() : " + e.getMessage());
            return false;
        }
        return (true);
    }

    public String getCookieNameForToken() throws GeneralFailure,
        RemoteException {
        return (SystemProperties.get(Constants.AM_COOKIE_NAME,
            "iPlanetDirectoryPro"));
    }

    public String[] getCookieNamesToForward() throws GeneralFailure,
        RemoteException {
        String[] cookies = null;
        String ssoTokenCookie = getCookieNameForToken();
        String lbCookieName = SystemProperties.get(
            Constants.AM_LB_COOKIE_NAME);
        if (lbCookieName == null) {
            cookies = new String[1];
        } else {
            cookies = new String[2];
            cookies[1] = lbCookieName;
        }
        cookies[0] = ssoTokenCookie;
        return (cookies);

    }
}
