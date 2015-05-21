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
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */
package com.sun.identity.idsvcs.opensso;

import java.net.MalformedURLException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoDuplicateObjectException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.idsvcs.AccessDenied;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdServicesException;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.ListWrapper;
import com.sun.identity.idsvcs.LogResponse;
import com.sun.identity.idsvcs.NeedMoreCredentials;
import com.sun.identity.idsvcs.ObjectNotFound;
import com.sun.identity.idsvcs.Token;
import com.sun.identity.idsvcs.TokenExpired;
import com.sun.identity.idsvcs.UserDetails;
import com.sun.identity.log.AMLogException;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.sm.SMSException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.errors.IdentityServicesExceptionMappingHandler;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

/**
 * Web Service to provide security based on authentication and authorization support.
 */
public class IdentityServicesImpl implements com.sun.identity.idsvcs.IdentityServicesImpl {

    private final ExceptionMappingHandler<IdRepoException, IdServicesException> idServicesErrorHandler =
            InjectorHolder.getInstance(IdentityServicesExceptionMappingHandler.class);

    private static Debug debug = Debug.getInstance("amIdentityServices");

    /**
     * Creates a new {@code AMIdentity} in the identity repository with the
     * details specified in {@code identity}.
     *
     * @param identity The identity details.
     * @param admin The admin token.
     * @throws ResourceException If a problem occurs.
     */
    public void create(IdentityDetails identity, SSOToken admin) throws ResourceException {
        Reject.ifNull(identity, admin);

        // Obtain identity details & verify
        String idName = identity.getName();
        String idType = identity.getType();
        String realm = identity.getRealm();
        if (StringUtils.isEmpty(idName)) {
            // TODO: add a message to the exception
            throw new BadRequestException("Identity name not provided");
        }
        if (StringUtils.isEmpty(idType)) {
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
                throw new UnsupportedOperationException("Unsupported: Type: " + idType + " Operation: CREATE");
            }

            // Obtain creation attributes
            Map<String, Set<String>> idAttrs = asMap(identity.getAttributes());

            // Create the identity, special case of Agents to merge
            // and validate the attributes
            AMIdentity amIdentity;
            if (isTypeAgent(objectIdType)) {
                createAgent(idAttrs, objectIdType, idType, idName, realm, admin);
            } else {
                // Create other identites like User, Group, Role, etc.
                amIdentity = repo.createIdentity(objectIdType, idName, idAttrs);

                // Process roles, groups & memberships
                if (IdType.USER.equals(objectIdType)) {

                    Set<String> roles = asSet(identity.getRoleList());
                    if (roles != null && !roles.isEmpty()) {
                        if (!isOperationSupported(repo, IdType.ROLE, IdOperation.EDIT)) {
                            // TODO: localize message
                            throw new UnsupportedOperationException("Unsupported: Type: " + IdType.ROLE
                                    + " Operation: EDIT");
                        }
                        for (String roleName : roles) {
                            AMIdentity role = fetchAMIdentity(repo, IdType.ROLE, roleName, false);
                            if (role != null) {
                                role.addMember(amIdentity);
                                role.store();
                            }
                        }
                    }

                    Set<String> groups = asSet(identity.getGroupList());
                    if (groups != null && !groups.isEmpty()) {
                        if (!isOperationSupported(repo, IdType.GROUP, IdOperation.EDIT)) {
                            // TODO: localize message
                            throw new UnsupportedOperationException("Unsupported: Type: " + IdType.GROUP
                                    + " Operation: EDIT");
                        }
                        for (String groupName : groups) {
                            AMIdentity group = fetchAMIdentity(repo, IdType.GROUP, groupName, false);
                            if (group != null) {
                                group.addMember(amIdentity);
                                group.store();
                            }
                        }
                    }
                }

                if (IdType.GROUP.equals(objectIdType) || IdType.ROLE.equals(objectIdType)) {

                    Set<String> members = asSet(identity.getMemberList());
                    if (members != null) {
                        if (IdType.GROUP.equals(objectIdType)
                                && !isOperationSupported(repo, IdType.GROUP, IdOperation.EDIT)) {
                            throw new ForbiddenException("Token is not authorized");
                        }
                        if (IdType.ROLE.equals(objectIdType) &&
                                !isOperationSupported(repo, IdType.ROLE, IdOperation.EDIT)) {
                            throw new ForbiddenException("Token is not authorized");
                        }
                        for (String memberName : members) {
                            AMIdentity user = fetchAMIdentity(repo, IdType.USER, memberName, false);
                            if (user != null) {
                                amIdentity.addMember(user);
                            }
                        }
                        amIdentity.store();
                    }
                }
            }
        } catch (IdRepoDuplicateObjectException ex) {
            throw new ConflictException("Resource already exists", ex);
        } catch (IdRepoException e) {
            debug.error("IdentityServicesImpl:create", e);
            if (IdRepoBundle.ACCESS_DENIED.equals(e.getErrorCode())) {
                throw new ForbiddenException(e.getMessage());
            } else {
                throw new NotFoundException(e.getMessage());
            }
        } catch (SSOException | SMSException | ConfigurationException | MalformedURLException
                | UnsupportedOperationException e) {
            debug.error("IdentityServicesImpl:create", e);
            throw new NotFoundException(e.getMessage());
        } catch (ObjectNotFound e) {
            debug.error("IdentityServicesImpl:create", e);
            throw new NotFoundException(e.getMessage());
        }
    }

    /**
     * Updates an {@code AMIdentity} in the identity repository with the
     * details specified in {@code identity}.
     *
     * @param identity The updated identity details.
     * @param admin The admin token.
     * @throws ResourceException If a problem occurs.
     */
    public void update(IdentityDetails identity, SSOToken admin) throws ResourceException {
        String idName = identity.getName();
        String idType = identity.getType();
        String realm = identity.getRealm();

        if (StringUtils.isEmpty(idName)) {
            // TODO: add a message to the exception
            throw new BadRequestException("");
        }

        if (StringUtils.isEmpty(idType)) {
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
                throw new ForbiddenException("");
            }

            AMIdentity amIdentity = getAMIdentity(admin, repo, idType, idName);

            if (amIdentity == null) {
                String msg = "Object \'" + idName + "\' of type \'" + idType + "\' not found.'";
                throw new NotFoundException(msg);
            }

            if (isSpecialUser(amIdentity)) {
                throw new ForbiddenException("Cannot update attributes for this user.");
            }

            Map<String, Set<String>> attrs = asMap(identity.getAttributes());

            if (attrs != null && !attrs.isEmpty()) {
                Map<String, Set<String>> idAttrs = new HashMap<>();
                Set<String> removeAttrs = new HashSet<>();

                for (Map.Entry<String, Set<String>> entry : attrs.entrySet()) {
                    String attrName = entry.getKey();
                    Set<String> attrValues = entry.getValue();

                    if (attrValues != null && !attrValues.isEmpty()) {
                        // attribute to add or modify
                        idAttrs.put(attrName, attrValues);
                    } else {
                        // attribute to remove
                        removeAttrs.add(attrName);
                    }
                }

                boolean storeNeeded = false;
                if (!idAttrs.isEmpty()) {
                    amIdentity.setAttributes(idAttrs);
                    storeNeeded = true;
                }

                if (!removeAttrs.isEmpty()) {
                    amIdentity.removeAttributes(removeAttrs);
                    storeNeeded = true;
                }

                if (storeNeeded) {
                    amIdentity.store();
                }
            }

            if (IdType.USER.equals(objectIdType)) {
                Set<String> roles = asSet(identity.getRoleList());

                if (!roles.isEmpty()) {
                    setMemberships(repo, amIdentity, roles, IdType.ROLE);
                }

                Set<String> groups = asSet(identity.getGroupList());

                if (!groups.isEmpty()) {
                    setMemberships(repo, amIdentity, groups, IdType.GROUP);
                }
            }

            if (IdType.GROUP.equals(objectIdType) || IdType.ROLE.equals(objectIdType)) {
                Set<String> members = asSet(identity.getMemberList());
                if (!members.isEmpty()) {
                    setMembers(repo, amIdentity, members, IdType.USER);
                }
            }
        } catch (IdRepoException ex) {
            debug.error("IdentityServicesImpl:update", ex);
            throw convertToResourceException(idServicesErrorHandler.handleError(ex));
        } catch (SSOException ex) {
            debug.error("IdentityServicesImpl:update", ex);
            throw new BadRequestException(ex.getMessage());
        } catch (ObjectNotFound e) {
            debug.error("IdentityServicesImpl:update", e);
            throw new NotFoundException(e.getMessage());
        }
    }

    /**
     * Deletes an {@code AMIdentity} from the identity repository that match
     * the details specified in {@code identity}.
     *
     * @param identity The identity to delete.
     * @param admin The admin token.
     * @throws ResourceException If a problem occurs.
     */
    public void delete(IdentityDetails identity, SSOToken admin) throws ResourceException {
        if (identity == null) {
            throw new BadRequestException("delete failed: identity object not specified.");
        }

        String name = identity.getName();
        String identityType = identity.getType();
        String realm = identity.getRealm();

        if (name == null) {
            throw new NotFoundException("delete failed: null object name.");
        }

        if (realm == null) {
            realm = "/";
        }

        try {
            AMIdentity amIdentity = getAMIdentity(admin, identityType, name, realm);

            if (amIdentity != null) {
                if (isSpecialUser(amIdentity)) {
                    throw new ForbiddenException("Cannot delete user.");
                }

                AMIdentityRepository repo = getRepo(admin, realm);
                IdType idType = amIdentity.getType();

                if (IdType.GROUP.equals(idType) || IdType.ROLE.equals(idType)) {
                    // First remove users from memberships
                    Set<AMIdentity> members = getMembers(amIdentity, IdType.USER);
                    for (AMIdentity member : members) {
                        try {
                            removeMember(repo, amIdentity, member);
                        } catch (IdRepoException ex) {
                            //ignore this, member maybe already removed.
                        }
                    }
                }

                deleteAMIdentity(repo, amIdentity);
            } else {
                String msg = "Object \'" + name + "\' of type \'" + identityType + "\' was not found.";
                throw new NotFoundException(msg);
            }
        } catch (IdRepoException ex) {
            debug.error("IdentityServicesImpl:delete", ex);
            throw convertToResourceException(idServicesErrorHandler.handleError(ex));
        } catch (SSOException ex) {
            debug.error("IdentityServicesImpl:delete", ex);
            throw new BadRequestException(ex.getMessage());
        } catch (ObjectNotFound e) {
            debug.error("IdentityServicesImpl:delete", e);
            throw new NotFoundException(e.getMessage());
        }
    }

    /**
     * Searches the identity repository to find all {@code AMIdentity}s that
     * match the provided search criteria.
     *
     * @param filter The search filter.
     * @param searchModifiers The search attributes.
     * @param admin The admin token.
     * @return A {@code List} of identity names.
     * @throws ResourceException If a problem occurs.
     */
    public List<String> search(String filter, Map<String, Set<String>> searchModifiers, SSOToken admin)
            throws ResourceException {
        List<String> rv = new ArrayList<>();

        try {
            String realm = "/";
            String objectType = "User";
            if (searchModifiers != null) {
                realm = attractValues("realm", searchModifiers, "/");
                objectType = attractValues("objecttype", searchModifiers, "User");
            }

            AMIdentityRepository repo = getRepo(admin, realm);
            IdType idType = getIdType(objectType);

            if (idType != null) {
                if (StringUtils.isEmpty(filter)) {
                    filter = "*";
                }

                List<AMIdentity> objList = fetchAMIdentities(idType, filter, false, repo, searchModifiers);
                if (objList != null && !objList.isEmpty()) {
                    List<String> names = getNames(realm, idType, objList);
                    if (!names.isEmpty()) {
                        for (String name : names) {
                            rv.add(name);
                        }
                    }
                }
            } else {
                debug.error("IdentityServicesImpl:search unsupported IdType" + objectType);
                throw new BadRequestException("search unsupported IdType: " + objectType);
            }
        } catch (IdRepoException e) {
            debug.error("IdentityServicesImpl:search", e);
            throw new InternalServerErrorException(e.getMessage());
        } catch (SSOException e) {
            debug.error("IdentityServicesImpl:search", e);
            throw new InternalServerErrorException(e.getMessage());
        } catch (ObjectNotFound e) {
            debug.error("IdentityServicesImpl:search", e);
            throw new NotFoundException(e.getMessage());
        }

        return rv;
    }

    private ResourceException convertToResourceException(IdServicesException exception) {
        if (exception.getClass().isAssignableFrom(ObjectNotFound.class)) {
            return new NotFoundException(exception.getMessage());
        } else if (exception.getClass().isAssignableFrom(AccessDenied.class)) {
            return new ForbiddenException(exception.getMessage());
        } else {
            return new InternalServerErrorException(exception.getMessage());
        }
    }

    @Override
    public LogResponse log(Token app, Token subject, String logName, String message) throws AccessDenied, TokenExpired,
            GeneralFailure {
        if (app == null) {
            throw new AccessDenied("No logging application token specified");
        }

        SSOToken appToken;
        SSOToken subjectToken;
        appToken = getSSOToken(app);
        subjectToken = subject == null ? appToken : getSSOToken(subject);

        try {
            LogRecord logRecord = new LogRecord(java.util.logging.Level.INFO, message, subjectToken);
            //TODO Support internationalization via a resource bundle specification
            Logger logger = (Logger) Logger.getLogger(logName);
            logger.log(logRecord, appToken);
            logger.flush();
        } catch (AMLogException e) {
            debug.error("IdentityServicesImpl:log", e);
            throw new GeneralFailure(e.getMessage());
        }
        return new LogResponse();
    }

    @Override
    public UserDetails attributes(String[] attributeNames, Token subject, Boolean refresh) throws TokenExpired,
            GeneralFailure, AccessDenied {
        List<String> attrNames = null;
        if (attributeNames != null && attributeNames.length > 0) {
            attrNames = new ArrayList<>();
            attrNames.addAll(Arrays.asList(attributeNames));
        }
        return attributes(attrNames, subject, refresh);
    }

    private UserDetails attributes(List<String> attributeNames, Token subject, Boolean refresh) throws TokenExpired,
            GeneralFailure, AccessDenied {
        UserDetails details = new UserDetails();
        try {
            SSOToken ssoToken = getSSOToken(subject);
            if (refresh != null && refresh) {
                SSOTokenManager.getInstance().refreshSession(ssoToken);
            }
            Map<String, Set<String>> sessionAttributes = new HashMap<>();
            Set<String> s;
            if (attributeNames != null) {
                String propertyNext;
                for (String attrNext : attributeNames) {
                    s = new HashSet<>();
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
                        if (propertyNext != null && !propertyNext.isEmpty()) {
                            s.add(propertyNext);
                        }
                    }
                    if (!s.isEmpty()) {
                        sessionAttributes.put(attrNext, s);
                    }
                }
            }
            // Obtain user memberships (roles and groups)
            AMIdentity userIdentity = IdUtils.getIdentity(ssoToken);
            if (isSpecialUser(userIdentity)) {
                throw new AccessDenied(
                    "Cannot retrieve attributes for this user.");
            }

            // Determine the types that can have members
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMIdentityRepository idrepo = new AMIdentityRepository(userIdentity.getRealm(), adminToken);
            Set<IdType> supportedTypes = idrepo.getSupportedIdTypes();
            Set<IdType> membersTypes = new HashSet<>();
            for (IdType type : supportedTypes) {
                if (type.canHaveMembers().contains(userIdentity.getType())) {
                    membersTypes.add(type);
                }
            }

            // Determine the roles and groups
            List<String> roles = new ArrayList<>();
            for (IdType type : membersTypes) {
                try {
                    Set<AMIdentity> memberships = userIdentity.getMemberships(type);
                    for (AMIdentity membership : memberships) {
                        roles.add(membership.getUniversalId());
                    }
                } catch (IdRepoException ire) {
                    debug.message("IdentityServicesImpl:attributes", ire);
                    // Ignore and continue
                }
            }
            String[] r = new String[roles.size()];
            details.setRoles(roles.toArray(r));

            Map<String, Set<String>> userAttributes;
            if (attributeNames != null) {
                Set<String> attrNames = new HashSet<>(attributeNames);
                userAttributes = userIdentity.getAttributes(attrNames);
            } else {
                userAttributes = userIdentity.getAttributes();
            }
            if (userAttributes != null) {
                for (Map.Entry<String, Set<String>> entry : sessionAttributes.entrySet()) {
                    if (userAttributes.keySet().contains(entry.getKey())) {
                       userAttributes.get(entry.getKey()).addAll(entry.getValue());
                    } else {
                        userAttributes.put(entry.getKey(), entry.getValue());
                    }
                }
            } else {
                userAttributes = sessionAttributes;
            }
            List<Attribute> attributes = new ArrayList<>(userAttributes.size());
            for (String name : userAttributes.keySet()) {
                Attribute attribute = new Attribute();
                attribute.setName(name);
                Set<String> value = userAttributes.get(name);
                if (value != null && !value.isEmpty()) {
                    List<String> valueList = new ArrayList<>(value.size());
                    // Convert the set to a List of String
                    for (String next : value) {
                        if (next != null) {
                            valueList.add(next);
                        }
                    }
                    String[] v = new String[valueList.size()];
                    attribute.setValues(valueList.toArray(v));
                    attributes.add(attribute);
                }
            }
            Attribute[] a = new Attribute[attributes.size()];
            details.setAttributes(attributes.toArray(a));
        } catch (IdRepoException e) {
            debug.error("IdentityServicesImpl:attributes", e);
            throw new GeneralFailure(e.getMessage());
        } catch (SSOException e) {
            debug.error("IdentityServicesImpl:attributes", e);
            throw new GeneralFailure(e.getMessage());
        }
        catch (TokenExpired e) {
            debug.warning("IdentityServicesImpl:attributes original error", e);
            throw new TokenExpired("Cannot retrieve Token.");
        }

        //TODO handle token translation
        details.setToken(subject);
        return details;
    }

    @Override
    public IdentityDetails read(String name, Attribute[] attributes, Token admin) throws IdServicesException {
        List<Attribute> attrList = null;
        
        if (name == null ||  name.isEmpty() || name.equals("null")) {
            debug.error("IdentityServicesImpl:read identity not found");
            throw new ObjectNotFound(name);
        }

        if (attributes != null && attributes.length > 0) {
            attrList = new ArrayList<>();
            attrList.addAll(Arrays.asList(attributes));
        }

        return read(name, attrList, getSSOToken(admin));
    }

    private IdentityDetails read(String name, List<Attribute> attributes, SSOToken admin) throws IdServicesException {
        return read(name, asMap(attributes), admin);
    }

    public IdentityDetails read(String name, Map<String, Set<String>> attributes, SSOToken admin)
            throws IdServicesException {
        IdentityDetails rv = null;
        String realm = null;
        String repoRealm;
        String identityType = null;
        List<String> attrsToGet = null;

        if (attributes != null) {
            for (Attribute attr : asAttributeArray(attributes)) {
                String attrName = attr.getName();

                if ("realm".equalsIgnoreCase(attrName)) {
                    String[] values = attr.getValues();

                    if (values != null && values.length > 0) {
                        realm = values[0];
                    }
                } else if ("objecttype".equalsIgnoreCase(attrName)) {
                    String[] values = attr.getValues();

                    if (values != null && values.length > 0) {
                        identityType = values[0];
                    }
                } else {
                    if (attrsToGet == null) {
                        attrsToGet = new ArrayList<>();
                    }

                    attrsToGet.add(attrName);
                }
            }
        }

        if (StringUtils.isEmpty(realm)) {
            repoRealm = "/";
        } else {
            repoRealm = realm;
        }

        if (StringUtils.isEmpty(identityType)) {
            identityType = "User";
        }

        try {
            AMIdentity amIdentity = getAMIdentity(admin, identityType, name, repoRealm);

            if (amIdentity == null) {
                debug.error("IdentityServicesImpl:read identity not found");
                throw new ObjectNotFound(name);
            }

            if (isSpecialUser(amIdentity)) {
                throw new AccessDenied("Cannot retrieve attributes for this user.");
            }

            rv = convertToIdentityDetails(amIdentity, attrsToGet);

            if (!StringUtils.isEmpty(realm)) {
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

    @Override
    public String getCookieNameForToken() throws GeneralFailure {
        return SystemProperties.get(Constants.AM_COOKIE_NAME, "iPlanetDirectoryPro");
    }

    @Override
    public String[] getCookieNamesToForward() throws GeneralFailure {
        String[] cookies;
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
        return cookies;
    }

    private boolean isTypeAgent(IdType objectIdType) {
        return objectIdType.equals(IdType.AGENT) || objectIdType.equals(IdType.AGENTONLY)
                || objectIdType.equals(IdType.AGENTGROUP);
    }

    /**
     * To be backward compatible, look for 'AgentType' attribute
     * in the attribute map which is passed as a parameter and if
     * not present/sent, check if the IdType.AGENTONLY or AGENT
     * and then assume that it is '2.2_Agent' type to create
     * that agent under the 2.2_Agent node.
     **/
    private void createAgent(Map<String, Set<String>> idAttrs, IdType objectIdType, String idType, String idName,
            String realm, SSOToken adminToken) throws SMSException, SSOException, ConfigurationException,
            IdRepoException, MalformedURLException {

        String agentType;
        String serverUrl = null;
        String agentUrl = null;

        final String AGENT_TYPE = "agenttype";
        final String SERVER_URL = "serverurl";
        final String AGENT_URL = "agenturl";
        final String DEFAULT_AGENT_TYPE = "2.2_Agent";

        Set<String> set = idAttrs.remove(AGENT_TYPE);
        if (set != null && !set.isEmpty()) {
            agentType = set.iterator().next();
        } else if (objectIdType.equals(IdType.AGENTONLY) || objectIdType.equals(IdType.AGENT)) {
            agentType = DEFAULT_AGENT_TYPE;
        } else {
            throw new UnsupportedOperationException("Unsupported: Agent Type required for " + idType);
        }

        set = idAttrs.remove(SERVER_URL);
        if (set != null && !set.isEmpty()) {
            serverUrl = set.iterator().next();
        }

        set = idAttrs.remove(AGENT_URL);
        if (set != null && !set.isEmpty()) {
            agentUrl = set.iterator().next();
        }

        if (agentType.equals(AgentConfiguration.AGENT_TYPE_WEB) || agentType.equals(AgentConfiguration.AGENT_TYPE_J2EE)) {
            if (StringUtils.isBlank(agentUrl)) {
                throw new MalformedURLException("Agent type requires agenturl to be configured.");
            } else if (StringUtils.isBlank(serverUrl)) {
                throw new MalformedURLException("Agent type requires serverurl to be configured.");
            }
        }

        if (objectIdType.equals(IdType.AGENT) || objectIdType.equals(IdType.AGENTONLY)) {
            if (StringUtils.isBlank(serverUrl) || StringUtils.isBlank(agentUrl)) {
                AgentConfiguration.createAgent(adminToken, realm, idName, agentType, idAttrs);
            } else {
                AgentConfiguration.createAgent(adminToken, realm, idName, agentType, idAttrs, serverUrl, agentUrl);
            }
        } else {
            if (StringUtils.isBlank(serverUrl) || StringUtils.isBlank(agentUrl)) {
                AgentConfiguration.createAgentGroup(adminToken, realm, idName, agentType, idAttrs);
            } else {
                AgentConfiguration.createAgentGroup(adminToken, realm, idName, agentType, idAttrs, serverUrl, agentUrl);
            }
        }
    }

    private boolean isSpecialUser(AMIdentity identity) {
        Set<AMIdentity> specialUsers = getSpecialUsers(identity.getRealm());
        return specialUsers != null && specialUsers.contains(identity);
    }

    private Set<AMIdentity> getSpecialUsers(String realmName) {
        SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        try {
            AMIdentityRepository repo = new AMIdentityRepository(realmName, adminToken);
            IdSearchResults results = repo.getSpecialIdentities(IdType.USER);
            return results.getSearchResults();
        } catch (IdRepoException | SSOException e) {
            debug.warning("AMModelBase.getSpecialUsers", e);
        }
        return Collections.emptySet();
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
     * Maps a IdRepoException to appropriate exception.
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
    private void mapIdRepoException(IdRepoException exception) throws IdServicesException {
        throw idServicesErrorHandler.handleError(exception);
    }

    private AMIdentityRepository getRepo(SSOToken token, String realm) {
        if (StringUtils.isEmpty(realm)) {
            realm = "/";
        }
        return new AMIdentityRepository(realm, token);
    }

    private IdType getIdType(String objectType) {
        try {
            return IdUtils.getType(objectType);
        } catch (IdRepoException ioe) {
            // Ignore exception
        }
        return null;
    }

    private List<String> getNames(String realm, IdType idType, List<AMIdentity> objList) throws SSOException,
            IdRepoException {
        List<String> names = new ArrayList<String>();

        if (objList != null && !objList.isEmpty()) {
            for (AMIdentity identity : objList) {
                if (identity != null) {
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

    private boolean isOperationSupported(AMIdentityRepository repo, IdType idType, IdOperation operation) {
        try {
            Set ops = repo.getAllowedIdOperations(idType);
            if (ops != null) {
                return ops.contains(operation);
            }
        } catch (IdRepoException | SSOException ex) {
            // Ignore
        }
        return false;
    }

    private Set<AMIdentity> getMembers(AMIdentity amIdentity, IdType type) throws IdRepoException, SSOException {
        return amIdentity.getMembers(type);
    }

    private Set<String> getMemberNames(AMIdentity amIdentity, IdType type) throws IdRepoException, SSOException {
        Set<String> rv = null;
        Set<AMIdentity> members = getMembers(amIdentity, type);
        if (members != null) {
            rv = new HashSet<>();
            for (AMIdentity member : members) {
                rv.add(member.getName());
            }
        }
        return rv;
    }

    private List<AMIdentity> fetchAMIdentities(IdType type, String identity, boolean fetchAllAttrs,
            AMIdentityRepository repo, Map searchModifiers) throws IdRepoException, ObjectNotFound, SSOException {
        IdSearchControl searchControl = new IdSearchControl();
        IdSearchResults searchResults;
        List<AMIdentity> identities;

        if (isOperationSupported(repo, type, IdOperation.READ)) {
            Set<AMIdentity> resultSet;

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
            identities = new ArrayList<>(resultSet);
        } else {
            // A list is expected back
        	/*
        	 * TODO: throw an exception instead of returning an empty list
        	 */
            identities = new ArrayList<>();
        }

        return identities;
    }

    private AMIdentity fetchAMIdentity(AMIdentityRepository repo, IdType type, String identity, boolean fetchAllAttrs)
            throws IdRepoException, ObjectNotFound, SSOException {
        AMIdentity rv = null;
        List<AMIdentity> identities = fetchAMIdentities(type, identity, fetchAllAttrs, repo, null);
        if (identities != null && !identities.isEmpty()) {
            rv = identities.get(0);
        }
        return rv;
    }

    private AMIdentity getAMIdentity(SSOToken ssoToken, AMIdentityRepository repo, String guid, IdType idType)
            throws IdRepoException, SSOException {
        if (isOperationSupported(repo, idType, IdOperation.READ)) {
            try {
                if (DN.isDN(guid)) {
                    return new AMIdentity(ssoToken, guid);
                } else {
                    return new AMIdentity(ssoToken, guid, idType, repo.getRealmIdentity().getRealm(), null);
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
        return null;
    }

    private String attractValues(String name, Map<String, Set<String>> map, String defaultValue) {
        Set<String> set = map.get(name);
        if (set != null && !set.isEmpty()) {
            String value = set.iterator().next().trim();
            map.remove(name);
            return value.isEmpty() ? defaultValue : value;
        } else {
            return defaultValue;
        }
    }

    private AMIdentity getAMIdentity(SSOToken ssoToken, String objectType, String id, String realm)
            throws IdRepoException, SSOException, ObjectNotFound {
        AMIdentityRepository repo = getRepo(ssoToken, realm);
        return getAMIdentity(ssoToken, repo, objectType, id);
    }

    private AMIdentity getAMIdentity(SSOToken ssoToken, AMIdentityRepository repo, String objectType, String id)
            throws IdRepoException, SSOException, ObjectNotFound {
        AMIdentity rv = null;
        IdType idType = null;

        if (objectType != null) {
            idType = getIdType(objectType);
        }

        if (idType != null) {
            // First assume id is a universal id
            rv = getAMIdentity(ssoToken, repo, id, idType);

            if (rv == null) {
                // Not found through id lookup, try name lookup
                rv = fetchAMIdentity(repo, idType, id, true);
            }
        }

        return rv;
    }

    private void addMember(AMIdentityRepository repo, AMIdentity amIdentity, AMIdentity member)
            throws IdRepoException, SSOException, ForbiddenException {
        if (!member.isMember(amIdentity)) {
            if (isOperationSupported(repo, amIdentity.getType(), IdOperation.EDIT)) {
                amIdentity.addMember(member);
            } else {
                // TODO: Add message to exception
                throw new ForbiddenException("");
            }
        }
    }

    private void removeMember(AMIdentityRepository repo, AMIdentity amIdentity, AMIdentity member)
            throws IdRepoException, SSOException, ForbiddenException {
        if (member.isMember(amIdentity)) {
            IdType type = amIdentity.getType();
            if (isOperationSupported(repo, type, IdOperation.EDIT)) {
                amIdentity.removeMember(member);
            } else {
                // TODO: Add message to exception
                throw new ForbiddenException("");
            }
        }
    }

    private void deleteAMIdentity(AMIdentityRepository repo, AMIdentity amIdentity) throws IdRepoException,
            SSOException, ForbiddenException {
        Set<AMIdentity> identities = new HashSet<>();
        identities.add(amIdentity);
        deleteAMIdentities(repo, amIdentity.getType(), identities);
    }

    private void deleteAMIdentities(AMIdentityRepository repo, IdType type, Set<AMIdentity> identities)
            throws IdRepoException, SSOException, ForbiddenException {
        if (isOperationSupported(repo, type, IdOperation.DELETE)) {
            repo.deleteIdentities(identities);
        } else {
            // TODO: add message to exception
            throw new ForbiddenException("");
        }
    }

    private void setMembers(AMIdentityRepository repo, AMIdentity amIdentity, Set<String> members, IdType idType)
            throws IdRepoException, SSOException, ObjectNotFound, ForbiddenException {
        Set<String> membershipsToAdd = members;
        Set<String> membershipsToRemove = null;
        Set<String> currentMembers = getMemberNames(amIdentity, idType);

        if ((currentMembers != null) && (currentMembers.size() > 0)) {
            membershipsToRemove = removeAllIgnoreCase(currentMembers, members);
            membershipsToAdd = removeAllIgnoreCase(members, currentMembers);
        }

        if (membershipsToRemove != null) {
            for (String memberName : membershipsToRemove) {
                AMIdentity identity = fetchAMIdentity(repo, idType, memberName, false);
                if (identity != null) {
                    removeMember(repo, amIdentity, identity);
                }
            }
        }

        if (membershipsToAdd != null) {
            for (String memberName : membershipsToAdd) {
                AMIdentity identity = fetchAMIdentity(repo, idType, memberName, false);
                if (identity != null) {
                    addMember(repo, amIdentity, identity);
                }
            }
        }
    }

    private void setMemberships(AMIdentityRepository repo, AMIdentity amIdentity, Set<String> memberships,
            IdType idType) throws IdRepoException, SSOException, ObjectNotFound, ForbiddenException {
        Set<String> membershipsToAdd = memberships;
        Set<String> membershipsToRemove = null;
        Set<String> currentMemberships = getMembershipNames(amIdentity, idType);

        if (currentMemberships != null && !currentMemberships.isEmpty()) {
            membershipsToRemove = removeAllIgnoreCase(currentMemberships, memberships);
            membershipsToAdd = removeAllIgnoreCase(memberships, currentMemberships);
        }

        if (membershipsToRemove != null) {
            for (String idName : membershipsToRemove) {
                AMIdentity container = fetchAMIdentity(repo, idType, idName, false);
                removeMember(repo, container, amIdentity);
            }
        }

        if (membershipsToAdd != null) {
            for (String idName : membershipsToAdd) {
                AMIdentity container = fetchAMIdentity(repo, idType, idName, false);
                addMember(repo, container, amIdentity);
            }
        }
    }

    private Set<String> removeAllIgnoreCase(Set<String> src, Set<String> removeSet) {
        if (src == null || src.isEmpty()) {
            return new HashSet<>();
        } else {
            Set<String> result = new HashSet<>(src);
            if (removeSet != null && !removeSet.isEmpty()) {
                Map<String, String> upcaseSrc = new HashMap<>(src.size());
                for (String s : src) {
                    upcaseSrc.put(s.toUpperCase(), s);
                }

                for (String s : removeSet) {
                    s = upcaseSrc.get(s.toUpperCase());
                    if (s != null) {
                        result.remove(s);
                    }
                }
            }
            return result;
        }
    }

    private Set<AMIdentity> getMemberships(AMIdentity amIdentity, IdType idType) throws SSOException {
        try {
            return amIdentity.getMemberships(idType);
        } catch (IdRepoException ex) {
            // This can be thrown if the identity is not a member
            // in any object of idType.
            return new HashSet<>(0);
        }
    }

    private Set<String> getMembershipNames(AMIdentity amIdentity, IdType idType) throws SSOException {
        Set<AMIdentity> memberships = getMemberships(amIdentity, idType);
        Set<String> rv = new HashSet<>(memberships.size());
        for (AMIdentity membership : memberships) {
            rv.add(membership.getName());
        }
        return rv;
    }

    private IdentityDetails convertToIdentityDetails(AMIdentity amIdentity, List<String> attrList)
            throws IdRepoException, SSOException {
        IdentityDetails rv = null;

        if (amIdentity != null) {
            IdType idType = amIdentity.getType();
            Map<String, Set<String>> attrs;
            boolean addUniversalId = false;

            rv = new IdentityDetails();
            rv.setName(amIdentity.getName());
            rv.setType(amIdentity.getType().getName());
            rv.setRealm(amIdentity.getRealm());

            if (IdType.USER.equals(idType)) {
                Set<AMIdentity> roles = amIdentity.getMemberships(IdType.ROLE);

                if (roles != null && !roles.isEmpty()) {
                    AMIdentity[] rolesFound = new AMIdentity[roles.size()];
                    String[] roleNames = new String[rolesFound.length];

                    roles.toArray(rolesFound);

                    for (int i = 0; i < rolesFound.length; i++) {
                        roleNames[i] = rolesFound[i].getName();
                    }

                    rv.setRoleList(new ListWrapper(roleNames));
                }

                Set<AMIdentity> groups = amIdentity.getMemberships(IdType.GROUP);

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
                Set<AMIdentity> members = amIdentity.getMembers(IdType.USER);

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
                Set<String> attrNames = new HashSet<>();
                for (String attrName : attrList) {
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
                    attrs = new HashMap<>();
                }

                Set<String> uidValue = new HashSet<>();

                uidValue.add(amIdentity.getUniversalId());
                attrs.put("universalid", uidValue);
            }

            if (attrs != null) {
                rv.setAttributes(asAttributeArray(attrs));
            }
        }
        return rv;
    }

    private SSOToken getSSOToken(Token admin) throws TokenExpired {
        try {
            if (admin == null) {
                throw new TokenExpired("Token is NULL");
            }
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            return mgr.createSSOToken(admin.getId());
        } catch (SSOException ex) {
            // throw TokenExpired exception
            throw new TokenExpired(ex.getMessage());
        }
    }

    private static Map<String, Set<String>> asMap(List<Attribute> attributes) {
        Map<String, Set<String>> map = new HashMap<>();
        if (attributes != null) {
            for (Attribute attribute : attributes) {
                map.put(attribute.getName(), new HashSet<>(Arrays.asList(attribute.getValues())));
            }
        }
        return map;
    }

    public static Map<String, Set<String>> asMap(Attribute... attributes) {
        if (attributes == null) {
            return new HashMap<>();
        }
        return asMap(Arrays.asList(attributes));
    }

    private static Set<String> asSet(ListWrapper list) {
        if (list == null) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(list.getElements()));
    }

    public static Attribute[] asAttributeArray(Map<String, Set<String>> attributes) {
        List<Attribute> attributesArray = new ArrayList<>();
        if (attributes != null) {
            for (Map.Entry<String, Set<String>> attribute : attributes.entrySet()) {
                attributesArray.add(new Attribute(attribute.getKey(), attribute.getValue().toArray(new String[0])));
            }
        }
        return attributesArray.toArray(new Attribute[0]);
    }
}
