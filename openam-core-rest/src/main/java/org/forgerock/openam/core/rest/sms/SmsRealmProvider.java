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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.sms;

import static com.sun.identity.sm.SMSException.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.rest.RestUtils.*;
import static org.forgerock.util.promise.Promises.*;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
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
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RestConstants;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openam.utils.RealmUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

public class SmsRealmProvider implements RequestHandler {
    private static final Debug debug = Debug.getInstance("frRest");

    private final static String SERVICE_NAMES = "serviceNames";
    private static final String ACTIVE_VALUE = "Active";
    private static final String INACTIVE_VALUE = "Inactive";
    private static final String ACTIVE_ATTRIBUTE_NAME = "active";
    private static final String ALIASES_ATTRIBUTE_NAME = "aliases";
    protected static final String REALM_NAME_ATTRIBUTE_NAME = "name";
    protected static final String PATH_ATTRIBUTE_NAME = "parentPath";
    private static final String PARENT_I18N_KEY = "a109";
    private static final String ACTIVE_I18N_KEY = "a108";
    private static final String ROOT_SERVICE = "";
    private static final String BAD_REQUEST_REALM_NAME_ERROR_MESSAGE
            = "Realm name specified in URL does not match realm name specified in JSON";
    private final SessionCache sessionCache;
    private final CoreWrapper coreWrapper;
    private RealmNormaliser realmNormaliser;

    // This blacklist also includes characters which upset LDAP.
    private final static Set<String> BLACKLIST_CHARACTERS = new TreeSet<>(CollectionUtils.asSet(
            "$", "&", "+", ",", "/", ":", ";", "=", "?", "@", " ", "#", "%", "<", ">", "\"", "\\"));

    public SmsRealmProvider(SessionCache sessionCache,
                            CoreWrapper coreWrapper,
                            RealmNormaliser realmNormaliser) {
        this.sessionCache = sessionCache;
        this.coreWrapper = coreWrapper;
        this.realmNormaliser = realmNormaliser;
    }

    /**
     * Create an amAdmin SSOToken
     *
     * @return SSOToken adminSSOtoken
     */
    private SSOToken getSSOToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Throws an appropriate HTTP status code
     *
     * @param exception SMSException to be mapped to HTTP Status code
     */
    private ResourceException configureErrorMessage(final SMSException exception) {

        switch (exception.getErrorCode()) {
            case "sms-REALM_NAME_NOT_FOUND":
                return new NotFoundException(exception.getMessage(), exception);
            case "sms-INVALID_SSO_TOKEN":
                return new PermanentException(401, "Unauthorized-Invalid SSO Token", exception);
            case "sms-organization_already_exists1":
                return new ConflictException(exception.getMessage(), exception);
            case "sms-invalid-org-name":
                return new BadRequestException(exception.getMessage(), exception);
            case "sms-cannot_delete_rootsuffix":
                return new PermanentException(401, "Unauthorized-Cannot delete root suffix", exception);
            case "sms-entries-exists":
                return new ConflictException(exception.getMessage(), exception);
            case "sms-SMSSchema_service_notfound":
                return new NotFoundException(exception.getMessage(), exception);
            case "sms-no-organization-schema":
                return new NotFoundException(exception.getMessage(), exception);
            case "sms-attribute-values-does-not-match-schema":
                return new BadRequestException(exception.getMessage(), exception);
            default:
                return new BadRequestException(exception.getMessage(), exception);
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        switch (request.getAction()) {
            case RestConstants.TEMPLATE:
                return newResultPromise(newActionResponse(json(object(
                        field(PATH_ATTRIBUTE_NAME, "/"), field(ACTIVE_ATTRIBUTE_NAME, true)))));
            case RestConstants.SCHEMA:
                return newResultPromise(newActionResponse(getSchema()));
            default:
            return new NotSupportedException("Action not supported: " + request.getAction()).asPromise();
        }
    }

    private JsonValue getSchema() {
        // TODO locale
        ResourceBundle consoleI18N = ResourceBundle.getBundle("amConsole");
        ResourceBundle idRepoI18N = ResourceBundle.getBundle("amIdRepoService");
        return json(object(field("type", "object"), field("properties", object(
                field(REALM_NAME_ATTRIBUTE_NAME, object(
                        field("type", "string"),
                        field("title", consoleI18N.getString("authDomain.attribute.label.name")),
                        field("required", true)
                )),
                field(PATH_ATTRIBUTE_NAME, object(
                        field("type", "string"),
                        field("title", consoleI18N.getString("realm.parent.label")),
                        field("required", true)
                )),
                field(ALIASES_ATTRIBUTE_NAME, object(
                        field("type", "array"),
                        field("items", object(field("type", "string"))),
                        field("title", idRepoI18N.getString(PARENT_I18N_KEY)),
                        field("description", SmsResourceProvider.getSchemaDescription(idRepoI18N, PARENT_I18N_KEY)),
                        field("required", true)
                )),
                field(ACTIVE_ATTRIBUTE_NAME, object(
                        field("type", "boolean"),
                        field("title", idRepoI18N.getString(ACTIVE_I18N_KEY)),
                        field("description", SmsResourceProvider.getSchemaDescription(idRepoI18N, ACTIVE_I18N_KEY)),
                        field("required", true)
                ))
        ))));
    }

    private boolean containsBlacklistedCharacters(String realmName) {
        for (String blacklistCharacter : BLACKLIST_CHARACTERS) {
            if (realmName.contains(blacklistCharacter)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context serverContext,
            CreateRequest createRequest) {

        final JsonValue jsonContent = createRequest.getContent();
        final String realmName = jsonContent.get(REALM_NAME_ATTRIBUTE_NAME).asString();

        try {
            if (StringUtils.isBlank(realmName)) {
                throw new BadRequestException("No realm name provided");
            }

            if (containsBlacklistedCharacters(realmName)) {
                throw new BadRequestException("Realm names cannot contain: " + BLACKLIST_CHARACTERS.toString());
            }

            RealmContext realmContext = serverContext.asContext(RealmContext.class);
            StringBuilder realmPath = new StringBuilder(realmContext.getResolvedRealm());

            String location = jsonContent.get(new JsonPointer(PATH_ATTRIBUTE_NAME)).asString();

            if (realmPath.length() > 1) {
                if (realmPath.charAt(realmPath.length() - 1) != '/' && !location.startsWith("/")) {
                    realmPath.append('/');
                }

                realmPath.append(location);
            } else {
                realmPath = new StringBuilder(location);
            }

            if (realmPath.charAt(realmPath.length() - 1) != '/') {
                realmPath.append('/');
            }

            realmPath.append(realmName);
            String path = realmPath.toString();

            String parentRealm = RealmUtils.getParentRealm(path);
            String childRealm = RealmUtils.getChildRealm(path);

            OrganizationConfigManager realmManager = new OrganizationConfigManager(getUserSsoToken(serverContext), parentRealm);

            Map<String, Map<String, Set>> serviceAttributes = new HashMap<>();
            serviceAttributes.put(IdConstants.REPO_SERVICE, getAttributeMap(jsonContent));
            realmManager.createSubOrganization(childRealm, serviceAttributes);

            if (debug.messageEnabled()) {
                debug.message("RealmResource.createInstance :: CREATE of realm {} in realm {} performed by {}",
                        childRealm, parentRealm, PrincipalRestUtils.getPrincipalNameFromServerContext(serverContext));
            }

            JsonValue jsonValue = getJsonValue(path, parentRealm);
            return newResultPromise(getResource(jsonValue));
        } catch (SMSException e) {
            return configureErrorMessage(e).asPromise();
        } catch (SSOException sso) {
            debug.error("RealmResource.createInstance() : Cannot CREATE " + realmName, sso);
            return new PermanentException(401, "Access Denied", null).asPromise();
        } catch (BadRequestException fe) {
            debug.error("RealmResource.createInstance() : Cannot CREATE " + realmName, fe);
            return fe.asPromise();
        }
    }

    private SSOToken getUserSsoToken(Context serverContext) throws SSOException {
        final SSOTokenManager mgr = SSOTokenManager.getInstance();
        return mgr.createSSOToken(getCookieFromServerContext(serverContext));
    }

    /**
     * Creates a Map from JsonValue content
     *
     * @param realmDetails Payload that is from request
     * @return Map of new attributes for realm realm
     */
    private Map<String, Set> getAttributeMap(JsonValue realmDetails) {
        Map<String, Set> attributes = new HashMap<>();

        String activeValue;
        if (realmDetails.get("active").defaultTo(true).asBoolean()) {
            activeValue = ACTIVE_VALUE;
        } else {
            activeValue = INACTIVE_VALUE;
        }

        attributes.put(IdConstants.ORGANIZATION_STATUS_ATTR, CollectionUtils.asSet(activeValue));
        attributes.put(IdConstants.ORGANIZATION_ALIAS_ATTR, new HashSet<>(realmDetails.get("aliases").asCollection(String.class)));

        return attributes;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context serverContext,
            DeleteRequest request) {
        RealmContext realmContext = serverContext.asContext(RealmContext.class);
        String realmPath = realmContext.getResolvedRealm();

        try {
            OrganizationConfigManager realmManager = new OrganizationConfigManager(getSSOToken(), realmPath);
            final ResourceResponse resource = getResource(getJsonValue(realmPath));
            realmManager.deleteSubOrganization(null, false);
            String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(serverContext);
            debug.message("RealmResource.deleteInstance :: DELETE of realm " + realmPath + " performed by " + principalName);
            return newResultPromise(resource);
        } catch (SMSException smse) {
            ResourceException exception = configureErrorMessage(smse);
            if (exception instanceof NotFoundException) {
                debug.warning("RealmResource.deleteInstance() : Cannot find {}", realmPath, smse);
                return exception.asPromise();
            } else if (exception instanceof ForbiddenException || exception instanceof PermanentException
                    || exception instanceof ConflictException || exception instanceof BadRequestException) {
                debug.warning("RealmResource.deleteInstance() : Cannot DELETE {}", realmPath, smse);
                return exception.asPromise();
            } else {
                return new BadRequestException(exception.getMessage(), exception).asPromise();
            }
        } catch (Exception e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        return new NotSupportedException("Method not supported.").asPromise();
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        if (!"true".equals(request.getQueryFilter().toString())) {
            return new NotSupportedException("Query not supported: " + request.getQueryFilter()).asPromise();
        }
        if (request.getPagedResultsCookie() != null || request.getPagedResultsOffset() > 0 ||
                request.getPageSize() > 0) {
            return new NotSupportedException("Query paging not currently supported").asPromise();
        }

        final String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        try {
            final SessionID sessionID = new SessionID(getUserSsoToken(context).getTokenID().toString());
            final String realmPath =
                    coreWrapper.convertOrgNameToRealmName(sessionCache.getSession(sessionID).getClientDomain());

            final OrganizationConfigManager ocm = new OrganizationConfigManager(getUserSsoToken(context), realmPath);

            //Return realm query is being performed on
            handler.handleResource(getResource(getJsonValue(realmPath)));

            for (final Object subRealmRelativePath : ocm.getSubOrganizationNames("*", true)) {
                String realmName;
                if (realmPath.endsWith("/")) {
                    realmName = realmPath + subRealmRelativePath;
                } else {
                    realmName = realmPath + "/" + subRealmRelativePath;
                }
                handler.handleResource(getResource(getJsonValue(realmName)));
            }
            debug.message("RealmResource :: QUERY : performed by {}", principalName);
            return newResultPromise(newQueryResponse());
        } catch (SSOException ex) {
            debug.error("RealmResource :: QUERY by " + principalName + " failed : " + ex);
            return new ForbiddenException().asPromise();
        } catch (SessionException ex) {
            debug.error("RealmResource :: QUERY by " + principalName + " failed : " + ex);
            return new InternalServerErrorException().asPromise();
        } catch (SMSException ex) {
            debug.error("RealmResource :: QUERY by " + principalName + " failed :" + ex);
            switch (ex.getExceptionCode()) {
                case STATUS_NO_PERMISSION:
                    // This exception will be thrown if permission to read realms from SMS has not been delegated
                    return new ForbiddenException().asPromise();
                default:
                    return new InternalServerErrorException().asPromise();
            }
        }
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        RealmContext realmContext = context.asContext(RealmContext.class);
        String realmPath = realmContext.getResolvedRealm();

        if(!request.getResourcePath().isEmpty()) {
            //if the resource path is not empty, the realm has not resolved correctly
            return new NotFoundException("Realm \"" + RealmUtils.concatenateRealmPath(RealmUtils.cleanRealm(realmPath),
                    RealmUtils.cleanRealm(request.getResourcePath())) + "\" is not a valid realm.").asPromise();
        }

        try {
            JsonValue jsonResponse = getJsonValue(realmPath);
            if (debug.messageEnabled()) {
                debug.message("RealmResource.readInstance :: READ : Successfully read realm, " +
                        realmPath + " performed by " + PrincipalRestUtils.getPrincipalNameFromServerContext(context));
            }
            return newResultPromise(getResource(jsonResponse));
        } catch (SMSException smse) {
            ResourceException exception = configureErrorMessage(smse);
            if (exception instanceof NotFoundException) {
                debug.warning("RealmResource.readInstance() : Cannot find {}", realmPath, smse);
                return exception.asPromise();
            } else if (exception instanceof ForbiddenException || exception instanceof PermanentException
                    || exception instanceof ConflictException || exception instanceof BadRequestException) {
                debug.warning("RealmResource.readInstance() : Cannot READ {}", realmPath, smse);
                return exception.asPromise();
            } else {
                return new BadRequestException(exception.getMessage(), exception).asPromise();
            }
        } catch (Exception e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        }
    }

    private JsonValue getJsonValue(String realmPath) throws SMSException {
        OrganizationConfigManager realmManager = new OrganizationConfigManager(getSSOToken(), realmPath);
        String realmName = getRealmName(realmManager);
        int pathLastSlash = realmPath.lastIndexOf('/');
        String parentPath = null;
        if (!realmName.equals("/") && pathLastSlash == 0) {
            parentPath = "/";
        } else if (!realmName.equals("/")) {
            parentPath = realmPath.substring(0, pathLastSlash);
        }
        return getJsonValue(realmManager, realmName, parentPath);
    }

    private JsonValue getJsonValue(String realmPath, String parentPath)
            throws SMSException {
        OrganizationConfigManager realmManager = new OrganizationConfigManager(getSSOToken(), realmPath);
        String realmName = getRealmName(realmManager);
        return getJsonValue(realmManager, realmName, parentPath);
    }

    private JsonValue getJsonValue(OrganizationConfigManager realmManager, String realmName, String parentPath)
            throws SMSException {
        return json(object(
                field(PATH_ATTRIBUTE_NAME, parentPath),
                field(ACTIVE_ATTRIBUTE_NAME, isActive(realmManager)),
                field(REALM_NAME_ATTRIBUTE_NAME, realmName),
                field(ALIASES_ATTRIBUTE_NAME, getAliases(realmManager))));
    }

    private ResourceResponse getResource(JsonValue jsonValue) {
        return newResourceResponse(jsonValue.get(REALM_NAME_ATTRIBUTE_NAME).asString(),
                String.valueOf(jsonValue.getObject().hashCode()), jsonValue);
    }

    private String getRealmName(OrganizationConfigManager realmManager) {
        String realmName = realmManager.getOrganizationName();

        String[] realmParts = realmName.split("/");
        if (realmParts.length > 0) {
            return realmParts[realmParts.length - 1];
        } else {
            return realmName;
        }
    }

    private boolean isActive(OrganizationConfigManager realmManager) throws SMSException {
        Set result = (Set) realmManager.getAttributes(ROOT_SERVICE).get("sunidentityrepositoryservice-sunOrganizationStatus");
        return result.contains(ACTIVE_VALUE);
    }

    private Set<String> getAliases(OrganizationConfigManager realmManager) throws SMSException {
        Set<String> result = (Set<String>) realmManager.getAttributes(ROOT_SERVICE).get("sunidentityrepositoryservice-sunOrganizationAliases");

        return result == null ? (Set) Collections.emptySet() : result;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        RealmContext realmContext = context.asContext(RealmContext.class);
        String realmPath = realmContext.getResolvedRealm();

        try {
            checkValues(request.getContent());
        } catch (BadRequestException e) {
            debug.error("RealmResource.updateInstance() : Cannot UPDATE " + realmPath, e);
            return new BadRequestException("Invalid attribute values").asPromise();
        }

        // protect against attempts to change a realm that does not exist as this results in unexpected behaviour
        try {
            String requestPath = getExpectedPathFromRequestContext(request);
            if (!realmPath.equals(requestPath)) {
                return new BadRequestException(BAD_REQUEST_REALM_NAME_ERROR_MESSAGE).asPromise();
            }
        } catch (org.forgerock.oauth2.core.exceptions.NotFoundException e) {
            return new BadRequestException(BAD_REQUEST_REALM_NAME_ERROR_MESSAGE).asPromise();
        }

        final JsonValue realmDetails = request.getContent();

        try {
            hasPermission(context);

            OrganizationConfigManager realmManager = new OrganizationConfigManager(getSSOToken(), realmPath);
            realmManager.setAttributes(IdConstants.REPO_SERVICE, getAttributeMap(realmDetails));

            final List<Object> newServiceNames = realmDetails.get(SERVICE_NAMES).asList();
            if (newServiceNames != null) {
                assignServices(realmManager, newServiceNames);
            }

            debug.message("RealmResource.updateInstance :: UPDATE of realm " + realmPath + " performed by " +
                    PrincipalRestUtils.getPrincipalNameFromServerContext(context));

            return newResultPromise(getResource(getJsonValue(realmPath)));
        } catch (SMSException e) {
            debug.error("RealmResource.updateInstance() : Cannot UPDATE " + realmPath, e);
            return configureErrorMessage(e).asPromise();
        } catch (SSOException | ForbiddenException | IdRepoException e) {
            debug.error("RealmResource.updateInstance() : Cannot UPDATE " + realmPath, e);
            return new PermanentException(401, "Access Denied", null).asPromise();
        }
    }

    protected String getExpectedPathFromRequestContext(UpdateRequest request)
            throws org.forgerock.oauth2.core.exceptions.NotFoundException {
        String contextPath = request.getContent().get(PATH_ATTRIBUTE_NAME).asString();
        String realmName = request.getContent().get(REALM_NAME_ATTRIBUTE_NAME).asString();

        if (contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.lastIndexOf('/'));
        }
        if (!realmName.startsWith("/")) {
            realmName = "/" + realmName;
        }
        return realmNormaliser.normalise(contextPath + realmName);
    }

    private void checkValues(JsonValue content) throws BadRequestException {
        if (!content.get(new JsonPointer(ACTIVE_ATTRIBUTE_NAME)).isBoolean()) {
            throw new BadRequestException(ACTIVE_ATTRIBUTE_NAME + " should be boolean");
        }
    }

    /**
     * Assigns Services to a realm
     *
     * @param realmManager    Organization Configuration Manager
     * @param newServiceNames List of service names to be assigned/unassigned
     * @throws SMSException
     */
    private void assignServices(OrganizationConfigManager realmManager, List newServiceNames)
            throws SMSException {
        try {
            Set<String> existingServices = realmManager.getAssignedServices();
            Set<String> allServices = new HashSet(newServiceNames.size() + existingServices.size());

            allServices.addAll(existingServices);
            allServices.addAll(newServiceNames);

            for (String serviceName : allServices) {
                if (newServiceNames.contains(serviceName) && !existingServices.contains(serviceName)) {
                    realmManager.assignService(serviceName, null);
                } else if (!newServiceNames.contains(serviceName) && existingServices.contains(serviceName)) {
                    realmManager.unassignService(serviceName);
                }
            }
        } catch (SMSException e) {
            debug.error("RealmResource.assignServices() : Unable to assign services");
            throw e;
        }
    }

}
