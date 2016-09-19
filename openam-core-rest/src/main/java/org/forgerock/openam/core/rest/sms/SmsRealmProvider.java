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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.sms;

import static com.sun.identity.sm.SMSException.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.json.resource.http.HttpUtils.PROTOCOL_VERSION_1;
import static org.forgerock.openam.rest.RestUtils.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.forgerock.util.promise.Promises.*;
import static org.restlet.engine.header.HeaderConstants.HEADER_IF_NONE_MATCH;
import static com.sun.identity.sm.SMSException.STATUS_NO_PERMISSION;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CREATE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DELETE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.PATH_PARAM;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.READ_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SMS_REALM_PROVIDER;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TITLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.UPDATE_DESCRIPTION;
import static org.forgerock.openam.rest.DescriptorUtils.fromResource;
import static org.forgerock.openam.rest.RestUtils.getCookieFromServerContext;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;
import static org.forgerock.openam.utils.CollectionUtils.newList;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.Create;
import org.forgerock.api.annotations.Delete;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Parameter;
import org.forgerock.api.annotations.Query;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.QueryType;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.promise.Promise;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;

@org.forgerock.api.annotations.CollectionProvider(
        details = @Handler(
                title = SMS_REALM_PROVIDER + TITLE,
                description = SMS_REALM_PROVIDER + DESCRIPTION,
                mvccSupported = false,
                resourceSchema = @Schema(
                        schemaResource = "SmsRealmProvider.resource.schema.json")),
        pathParam = @Parameter(
                name = SmsRealmProvider.REALM_REF_PARAMETER,
                type = "string",
                description = SMS_REALM_PROVIDER + PATH_PARAM + DESCRIPTION))
public class SmsRealmProvider {

    private final static String SERVICE_NAMES = "serviceNames";
    private static final String ACTIVE_VALUE = "Active";
    private static final String INACTIVE_VALUE = "Inactive";
    static final String REALMS_PATH = "realms";
    protected static final String ACTIVE_ATTRIBUTE_NAME = "active";
    protected static final String ALIASES_ATTRIBUTE_NAME = "aliases";
    protected static final String REALM_NAME_ATTRIBUTE_NAME = "name";
    protected static final String PATH_ATTRIBUTE_NAME = "parentPath";
    private static final String ROOT_SERVICE = "";
    private static final String BAD_REQUEST_REALM_NAME_ERROR_MESSAGE
            = "Realm name specified in URL does not match realm name specified in JSON";

    private static final Debug debug = Debug.getInstance("frRest");

    // This blacklist also includes characters which upset LDAP.
    private final static Set<String> BLACKLIST_CHARACTERS = new TreeSet<>(asSet(
            "$", "&", "+", ",", "/", ":", ";", "=", "?", "@", " ", "#", "%", "<", ">", "\"", "\\"));

    private static final String ERR_NO_PARAMETER_PROVIDED = "No %s parameter provided";
    private static final String ERR_WRONG_PARAMETER_TYPE = "Wrong parameter type provided in %s. Expected %s";
    private static final String ERR_REALM_CANNOT_CONTAIN = "Realm names cannot contain: %s";
    private static final String ERR_ALIAS_CANNOT_CONTAIN = "Realm alias cannot contain: %s";
    private static final String ERR_REALM_NAME_DOES_NOT_MATCH_CONTENT = "Realm name in URL does not match realm name in request body";
    private static final String JSON_NULL_STR = "null";
    private static final org.forgerock.api.models.Schema RESOURCE_SCHEMA
            = fromResource("SmsRealmProvider.resource.schema.json", SmsRealmProvider.class);

    public static final String REALM_REF_PARAMETER = "realmref";
    // Lw== root %2F
    private final SessionCache sessionCache;
    private final CoreWrapper coreWrapper;
    private RealmNormaliser realmNormaliser;

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
     * @param notFoundPath The realm that may not have been found.
     */
    private ResourceException configureErrorMessage(final SMSException exception, String notFoundPath) {

        switch (exception.getErrorCode()) {
            case "sms-REALM_NAME_NOT_FOUND":
                return new NotFoundException("Realm cannot be read: " + notFoundPath, exception);
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
                return new NotFoundException("Realm cannot be read: " + notFoundPath, exception);
            case "sms-no-organization-schema":
                return new NotFoundException("Realm cannot be read: " + notFoundPath, exception);
            case "sms-attribute-values-does-not-match-schema":
                return new BadRequestException(exception.getMessage(), exception);
            default:
                return new BadRequestException(exception.getMessage(), exception);
        }
    }

    @Action(operationDescription = @Operation())
    public Promise<ActionResponse, ResourceException> template() {
        return newResultPromise(newActionResponse(json(object(field(PATH_ATTRIBUTE_NAME, "/"), field(ACTIVE_ATTRIBUTE_NAME, true)))));
    }

    @Action(operationDescription = @Operation())
    public Promise<ActionResponse, ResourceException> schema() {
        return newResultPromise(newActionResponse(RESOURCE_SCHEMA.getSchema()));
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

        attributes.put(IdConstants.ORGANIZATION_STATUS_ATTR, asSet(activeValue));
        attributes.put(IdConstants.ORGANIZATION_ALIAS_ATTR,
                new HashSet<>(realmDetails.get(ALIASES_ATTRIBUTE_NAME).asCollection(String.class)));

        return attributes;
    }

    @Delete(operationDescription = @Operation(
        errors = {
                @ApiError(
                        code = 500,
                        description = SMS_REALM_PROVIDER + "error.unexpected.server.error." + DESCRIPTION)},
        description = SMS_REALM_PROVIDER + DELETE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> handleDelete(String realmPath, Context serverContext) {
        String decodedPath = new String(Base64.decode(realmPath));
        try {

            OrganizationConfigManager realmManager = new OrganizationConfigManager(getSSOToken(), decodedPath);
            final ResourceResponse resource = getResource(getJsonValue(decodedPath));
            realmManager.deleteSubOrganization(null, false);
            String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(serverContext);
            debug.message("RealmResource.deleteInstance :: DELETE of realm " + decodedPath + " performed by " + principalName);
            return newResultPromise(resource);
        } catch (SMSException smse) {
            ResourceException exception = configureErrorMessage(smse, decodedPath);
            if (exception instanceof NotFoundException) {
                debug.warning("RealmResource.deleteInstance() : Cannot find {}", decodedPath, smse);
                return exception.asPromise();
            } else if (exception instanceof ForbiddenException || exception instanceof PermanentException
                    || exception instanceof ConflictException || exception instanceof BadRequestException) {
                debug.warning("RealmResource.deleteInstance() : Cannot DELETE {}", decodedPath, smse);
                return exception.asPromise();
            } else {
                return new BadRequestException(exception.getMessage(), exception).asPromise();
            }
        } catch (Exception e) {
            return new BadRequestException(e.getMessage(), e).asPromise();
        }
    }

    @Query(operationDescription = @Operation(), type = QueryType.FILTER)
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

    @Read(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = SMS_REALM_PROVIDER + "error.unexpected.server.error." + DESCRIPTION)},
            description = SMS_REALM_PROVIDER + READ_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> handleRead(String realmPath, Context context) {

        String decodedPath = new String(Base64.decode(realmPath));
        try {
            JsonValue jsonResponse = getJsonValue(decodedPath);
            if (debug.messageEnabled()) {
                debug.message("RealmResource.readInstance :: READ : Successfully read realm, " +
                        decodedPath + " performed by " + PrincipalRestUtils.getPrincipalNameFromServerContext(context));
            }
            return newResultPromise(getResource(jsonResponse));
        } catch (SMSException smse) {
            ResourceException exception = configureErrorMessage(smse, decodedPath);
            if (exception instanceof NotFoundException) {
                debug.warning("RealmResource.readInstance() : Cannot find {}", decodedPath, smse);
                return exception.asPromise();
            } else if (exception instanceof ForbiddenException || exception instanceof PermanentException
                    || exception instanceof ConflictException || exception instanceof BadRequestException) {
                debug.warning("RealmResource.readInstance() : Cannot READ {}", decodedPath, smse);
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
        return getJsonValue(realmManager, realmName, parentPath, realmPath);
    }

    private JsonValue getJsonValue(OrganizationConfigManager realmManager, String realmName, String parentPath,
            String realmPath)
            throws SMSException {
        return json(object(
                field(ResourceResponse.FIELD_CONTENT_ID, Base64.encode(realmPath.getBytes(StandardCharsets.UTF_8))),
                field(PATH_ATTRIBUTE_NAME, parentPath),
                field(ACTIVE_ATTRIBUTE_NAME, isActive(realmManager)),
                field(REALM_NAME_ATTRIBUTE_NAME, realmName),
                field(ALIASES_ATTRIBUTE_NAME, newList(getAliases(realmManager)))));
    }

    private ResourceResponse getResource(JsonValue jsonValue) {
        return newResourceResponse(jsonValue.get(ResourceResponse.FIELD_CONTENT_ID).asString(),
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
        return result == null || result.contains(ACTIVE_VALUE);
    }

    private Set<String> getAliases(OrganizationConfigManager realmManager) throws SMSException {
        Set<String> result = (Set<String>) realmManager.getAttributes(ROOT_SERVICE).get("sunidentityrepositoryservice-sunOrganizationAliases");
        return result == null ? (Set) Collections.emptySet() : result;
    }

    @Update(operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 500,
                            description = SMS_REALM_PROVIDER + "error.unexpected.server.error." + DESCRIPTION)},
            description = SMS_REALM_PROVIDER + UPDATE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> handleUpdate(String realmPath, Context context, UpdateRequest request) {
        String decodedPath = new String(Base64.decode(realmPath));
        try {
            checkValues(request.getContent());
        } catch (BadRequestException e) {
            debug.error("RealmResource.updateInstance() : Cannot UPDATE " + decodedPath, e);
            return new BadRequestException("Invalid attribute values").asPromise();
        }

        try {
            String requestPath = getExpectedPathFromRequestContext(request);
            if (!decodedPath.equals(requestPath)) {
                return new BadRequestException(BAD_REQUEST_REALM_NAME_ERROR_MESSAGE).asPromise();
            }
        } catch (NotFoundException e) {
            return new BadRequestException(BAD_REQUEST_REALM_NAME_ERROR_MESSAGE).asPromise();
        }

        final JsonValue realmDetails = request.getContent();

        try {
            OrganizationConfigManager realmManager = new OrganizationConfigManager(getSSOToken(), decodedPath);
            realmManager.setAttributes(IdConstants.REPO_SERVICE, getAttributeMap(realmDetails));

            final List<Object> newServiceNames = realmDetails.get(SERVICE_NAMES).asList();
            if (newServiceNames != null) {
                assignServices(realmManager, newServiceNames);
            }

            debug.message("RealmResource.updateInstance :: UPDATE of realm " + decodedPath + " performed by " +
                    PrincipalRestUtils.getPrincipalNameFromServerContext(context));

            return newResultPromise(getResource(getJsonValue(decodedPath)));
        } catch (SMSException e) {
            debug.error("RealmResource.updateInstance() : Cannot UPDATE " + decodedPath, e);
            return configureErrorMessage(e, decodedPath).asPromise();
        }
    }

    protected String getExpectedPathFromRequestContext(UpdateRequest request)
            throws NotFoundException {
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

    @Create(modes = CreateMode.ID_FROM_SERVER, operationDescription = @Operation(
            errors = {
                    @ApiError(
                            code = 400,
                            description = SMS_REALM_PROVIDER + "error.unexpected.bad.request." + DESCRIPTION),
                    @ApiError(
                            code = 500,
                            description = SMS_REALM_PROVIDER + "error.unexpected.server.error." + DESCRIPTION)},
            description = SMS_REALM_PROVIDER + CREATE_DESCRIPTION))
    public Promise<ResourceResponse, ResourceException> handleCreate(Context serverContext, CreateRequest createRequest) {
        if (createRequest.getNewResourceId() != null) {
            return new NotSupportedException("Cannot provide ID for Realm resource").asPromise();
        }
        final JsonValue jsonContent = createRequest.getContent();

        try {
            jsonContentValidation(jsonContent);
        } catch (BadRequestException e) {
            debug.error("RealmResource.createInstance() : Cannot CREATE " + jsonContent.getObject(), e);
            return e.asPromise();
        }

        String realmName = jsonContent.get(REALM_NAME_ATTRIBUTE_NAME).asString();
        String parentPath = jsonContent.get(PATH_ATTRIBUTE_NAME).asString();
        String fullPath = parentPath.equals("/") ? parentPath + realmName : parentPath + "/" + realmName;

        try {

            OrganizationConfigManager realmManager = new OrganizationConfigManager(getUserSsoToken(serverContext), parentPath);

            Map<String, Map<String, Set>> serviceAttributes = new HashMap<>();
            serviceAttributes.put(IdConstants.REPO_SERVICE, getAttributeMap(jsonContent));
            realmManager.createSubOrganization(realmName, serviceAttributes);

            if (debug.messageEnabled()) {
                debug.message("RealmResource.createInstance :: CREATE of realm {} in realm {} performed by {}",
                        realmName, parentPath, PrincipalRestUtils.getPrincipalNameFromServerContext(serverContext));
            }

            JsonValue jsonValue = getJsonValue(fullPath, parentPath);
            return newResultPromise(getResource(jsonValue));
        } catch (SMSException e) {
            if (isContractConformantUserProvidedIdCreate(serverContext, createRequest)) {
                return new PreconditionFailedException("Unable to create Realm: " + e.getMessage()).asPromise();
            } else {
                return configureErrorMessage(e, parentPath).asPromise();
            }
        } catch (SSOException sso) {
            debug.error("RealmResource.createInstance() : Cannot CREATE " + realmName, sso);
            return new PermanentException(401, "Access Denied", null).asPromise();
        }
    }

    private void jsonContentValidation(JsonValue jsonContent) throws BadRequestException {
        validateRealmName(jsonContent);
        validateParentPath(jsonContent);
        validateActiveFlag(jsonContent);
        validateRealmAliases(jsonContent);
    }

    private void validateRealmName(JsonValue jsonContent) throws BadRequestException {
        JsonValue realmName = jsonContent.get(REALM_NAME_ATTRIBUTE_NAME);
        checkArgument(realmName.isNotNull(), ERR_NO_PARAMETER_PROVIDED, REALM_NAME_ATTRIBUTE_NAME);
        checkArgument(StringUtils.isNotBlank(realmName.asString()), ERR_NO_PARAMETER_PROVIDED, REALM_NAME_ATTRIBUTE_NAME);
        checkArgument(!containsBlacklistedCharacters(realmName.asString()), ERR_REALM_CANNOT_CONTAIN, BLACKLIST_CHARACTERS.toString());
    }

    private void validateParentPath(JsonValue jsonContent) throws BadRequestException {
        JsonValue parentPath = jsonContent.get(PATH_ATTRIBUTE_NAME);
        checkArgument(parentPath.isNotNull(), ERR_NO_PARAMETER_PROVIDED, PATH_ATTRIBUTE_NAME);
        checkArgument(StringUtils.isNotBlank(parentPath.asString()), ERR_NO_PARAMETER_PROVIDED, PATH_ATTRIBUTE_NAME);
    }

    private void validateActiveFlag(JsonValue jsonContent) throws BadRequestException {
        JsonValue active = jsonContent.get(ACTIVE_ATTRIBUTE_NAME);
        checkArgument(active.isNotNull(), ERR_NO_PARAMETER_PROVIDED, ACTIVE_ATTRIBUTE_NAME);
        checkArgument(!active.toString().equals(JSON_NULL_STR), ERR_NO_PARAMETER_PROVIDED, ACTIVE_ATTRIBUTE_NAME);
        checkArgument(active.isBoolean(), ERR_WRONG_PARAMETER_TYPE , ACTIVE_ATTRIBUTE_NAME, Boolean.TYPE.toString());
    }

    private void validateRealmAliases(JsonValue jsonContent) throws BadRequestException {
        JsonValue aliasJsonValue = jsonContent.get(ALIASES_ATTRIBUTE_NAME);
        checkArgument(aliasJsonValue.isNotNull(), ERR_NO_PARAMETER_PROVIDED, ALIASES_ATTRIBUTE_NAME);
        checkArgument(!aliasJsonValue.toString().equals(JSON_NULL_STR), ERR_NO_PARAMETER_PROVIDED, ALIASES_ATTRIBUTE_NAME);
        checkArgument(aliasJsonValue.isCollection(), ERR_WRONG_PARAMETER_TYPE , ALIASES_ATTRIBUTE_NAME, "list");

        try {
            List<String> aliases = aliasJsonValue.asList(String.class);
            if (isNotEmpty(aliases)) {
                for (String alias : aliases) {
                    checkArgument(StringUtils.isNotBlank(alias), ERR_NO_PARAMETER_PROVIDED, ALIASES_ATTRIBUTE_NAME);
                    checkArgument(!containsBlacklistedCharacters(alias), ERR_ALIAS_CANNOT_CONTAIN, BLACKLIST_CHARACTERS.toString());
                }
            }
        } catch (JsonValueException e) {
            //should not come here
            throw new BadRequestException(e);
        }
    }

    private void checkArgument(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) throws BadRequestException {
        if(!expression) {
            throw new BadRequestException(String.format(errorMessageTemplate, errorMessageArgs));
        }
    }

    private boolean containsBlacklistedCharacters(String realmName) {
        for (String blacklistCharacter : BLACKLIST_CHARACTERS) {
            if (realmName.contains(blacklistCharacter)) {
                return true;
            }
        }
        return false;
    }

    private JsonValue getJsonValue(String realmPath, String parentPath)
            throws SMSException {
        OrganizationConfigManager realmManager = new OrganizationConfigManager(getSSOToken(), realmPath);
        String realmName = getRealmName(realmManager);
        return getJsonValue(realmManager, realmName, parentPath, realmPath);
    }
}
