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

import static com.sun.identity.sm.SMSException.STATUS_NO_PERMISSION;
import static org.forgerock.api.enums.CreateMode.ID_FROM_CLIENT;
import static org.forgerock.api.enums.ParameterSource.PATH;
import static org.forgerock.api.models.Action.action;
import static org.forgerock.api.models.Create.create;
import static org.forgerock.api.models.Delete.delete;
import static org.forgerock.api.models.Items.items;
import static org.forgerock.api.models.Parameter.parameter;
import static org.forgerock.api.models.Query.query;
import static org.forgerock.api.models.Read.read;
import static org.forgerock.api.models.Update.update;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newQueryResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.rest.RestUtils.getCookieFromServerContext;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Update;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.guava.common.base.Strings;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
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
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.forgerockrest.utils.PrincipalRestUtils;
import org.forgerock.openam.rest.RestConstants;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openam.utils.RealmUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.promise.Promise;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;

@org.forgerock.api.annotations.RequestHandler(value = @Handler(
        mvccSupported = false,
        resourceSchema = @Schema(fromType = Object.class)
        ))
public class SmsRealmProvider implements RequestHandler, Describable<ApiDescription, Request> {

    private static final ClassLoader CLASS_LOADER = SmsRealmProvider.class.getClassLoader();
    private final static String SERVICE_NAMES = "serviceNames";
    private static final String ACTIVE_VALUE = "Active";
    private static final String INACTIVE_VALUE = "Inactive";
    protected static final String ACTIVE_ATTRIBUTE_NAME = "active";
    protected static final String ALIASES_ATTRIBUTE_NAME = "aliases";
    protected static final String REALM_NAME_ATTRIBUTE_NAME = "name";
    protected static final String PATH_ATTRIBUTE_NAME = "parentPath";
    private static final String PARENT_I18N_KEY = "a109";
    private static final String ACTIVE_I18N_KEY = "a108";
    private static final String ROOT_SERVICE = "";
    private static final String BAD_REQUEST_REALM_NAME_ERROR_MESSAGE
            = "Realm name specified in URL does not match realm name specified in JSON";
    private static final String SMS_REALM_NAME_NOT_FOUND = "sms-REALM_NAME_NOT_FOUND";

    private static final Debug debug = Debug.getInstance("frRest");

    // This blacklist also includes characters which upset LDAP.
    private final static Set<String> BLACKLIST_CHARACTERS = new TreeSet<>(asSet(
            "$", "&", "+", ",", "/", ":", ";", "=", "?", "@", " ", "#", "%", "<", ">", "\"", "\\"));

    private static final String ERR_NO_PARAMETER_PROVIDED = "No %s parameter provided";
    private static final String ERR_WRONG_PARAMETER_TYPE = "Wrong parameter type provided in %s. Expected %s";
    private static final String ERR_REALM_CANNOT_CONTAIN = "Realm names cannot contain: %s";
    private static final String ERR_ALIAS_CANNOT_CONTAIN = "Realm alias cannot contain: %s";
    private static final String JSON_NULL_STR = "null";

    public static final String REALM_REF_PARAMETER = "realmref";
    public static final String REALM_REF_ROUTE = "realms/{" + REALM_REF_PARAMETER + "}";
    private final SessionCache sessionCache;
    private final CoreWrapper coreWrapper;
    private RealmNormaliser realmNormaliser;

    private final ApiDescription description = ApiDescription.apiDescription().id("fake").version("v")
            .paths(Paths.paths().put("", VersionedPath.versionedPath()
                    .put(VersionedPath.UNVERSIONED, Resource.resource().parameter(parameter().name(REALM_REF_PARAMETER)
                            .type("string").source(ParameterSource.PATH).build())
                            .title(localizableString("title"))
                            .description(localizableString("description"))
                            .mvccSupported(false)
                            .resourceSchema(org.forgerock.api.models.Schema.schema().schema(getSchema()).build())
                            .delete(Delete.delete()
                                    .description(localizableString("delete.description")).build())
                            .query(query()
                                    .type(QueryType.FILTER)
                                    .description(localizableString("query.description"))
                                    .queryableFields().build())
                            .update(Update.update()
                                    .description(localizableString("update.description")).build())
                            .read(read()
                                    .description(localizableString("read.description")).build())
                            .items(items().pathParameter(parameter().name("realmname").type("string").source(PATH).build())
                                    .create(create().mode(ID_FROM_CLIENT).build())
                                    .build())
                            .action(action()
                                    .name("schema")
                                    .description(localizableString("action.schema.description"))
                                    .response(org.forgerock.api.models.Schema.schema().schema(getJsonSchemaSchema())
                                            .build()).build())
                            .action(action()
                                    .name("template")
                                    .description(localizableString("action.template.description"))
                                    .response(org.forgerock.api.models.Schema.schema().schema(getJsonSchemaSchema())
                                            .build()).build())
                            .build())
                    .build())
                    .build())
            .build();

    public SmsRealmProvider(SessionCache sessionCache,
                            CoreWrapper coreWrapper,
                            RealmNormaliser realmNormaliser) {
        this.sessionCache = sessionCache;
        this.coreWrapper = coreWrapper;
        this.realmNormaliser = realmNormaliser;
    }

    private JsonValue getJsonSchemaSchema() {
        return json(object(field("$ref", "http://json-schema.org/schema#"), field("type", "object")));
    }

    private LocalizableString localizableString(String key) {
        return new LocalizableString("i18n:api-descriptor/SmsRealmProvider#" + key, CLASS_LOADER);
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
                        field("description", getSchemaDescription(PARENT_I18N_KEY)),
                        field("required", true)
                )),
                field(ACTIVE_ATTRIBUTE_NAME, object(
                        field("type", "boolean"),
                        field("title", idRepoI18N.getString(ACTIVE_I18N_KEY)),
                        field("description", getSchemaDescription(ACTIVE_I18N_KEY)),
                        field("required", true)
                ))
        ))));
    }

    private LocalizableString getSchemaDescription(String i18NKey) {
        final LocalizableString help = getSchemaI18N(i18NKey + ".help", new LocalizableString(""));
        final LocalizableString helpTxt = getSchemaI18N(i18NKey + ".help.txt", new LocalizableString(""));
        return SmsResourceProvider.getSchemaDescription(help, helpTxt);
    }

    private LocalizableString getSchemaI18N(String key, LocalizableString defaultValue) {
        return new LocalizableString(LocalizableString.TRANSLATION_KEY_PREFIX + "amIdRepoService#" + key,
                getClass().getClassLoader(), defaultValue);
    }

    private Realm getRealm(Context context) throws RealmLookupException {
        Map<String, String> vars = context.asContext(UriRouterContext.class).getUriTemplateVariables();
        String realmPath = "";
        if (vars.containsKey(REALM_REF_PARAMETER)) {
            realmPath = vars.get(REALM_REF_PARAMETER);
        }
        return Realm.of(realmPath);
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

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context serverContext,
            DeleteRequest request) {

        String realmPath = null;

        try {
            realmPath = getRealm(serverContext).asPath();

            //To determine whether the realm to delete exists we have to check if the request.getResourcePath() is empty or not.
            //It gets emptied if the realm was found.
            String realmName = request.getResourcePath();
            if (!Strings.isNullOrEmpty(realmName)){
                throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME, SMS_REALM_NAME_NOT_FOUND, new String[]{realmName});
            }

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

        String realmPath = null;

        try {
            realmPath = getRealm(context).asPath();
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
        return result == null || result.contains(ACTIVE_VALUE);
    }

    private Set<String> getAliases(OrganizationConfigManager realmManager) throws SMSException {
        Set<String> result = (Set<String>) realmManager.getAttributes(ROOT_SERVICE).get("sunidentityrepositoryservice-sunOrganizationAliases");
        return result == null ? (Set) Collections.emptySet() : result;
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {

        String realmPath = null;

        try {
            realmPath = getRealm(context).asPath();
            checkValues(request.getContent());
        } catch (BadRequestException e) {
            debug.error("RealmResource.updateInstance() : Cannot UPDATE " + realmPath, e);
            return new BadRequestException("Invalid attribute values").asPromise();
        } catch (RealmLookupException e) {
            return new BadRequestException("Specified Realm is in valid").asPromise();
        }

        // protect against attempts to change a realm that does not exist as this results in unexpected behaviour
        try {
            String requestPath = getExpectedPathFromRequestContext(request);
            if (!realmPath.equals(requestPath)) {
                return new BadRequestException(BAD_REQUEST_REALM_NAME_ERROR_MESSAGE).asPromise();
            }
        } catch (NotFoundException e) {
            return new BadRequestException(BAD_REQUEST_REALM_NAME_ERROR_MESSAGE).asPromise();
        }

        final JsonValue realmDetails = request.getContent();

        try {
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

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context serverContext,
            CreateRequest createRequest) {

        final JsonValue jsonContent = createRequest.getContent();
        String realmName = null;

        try {

            jsonContentValidation(jsonContent);

            StringBuilder realmPath = new StringBuilder(getRealm(serverContext).asPath());

            if (realmPath.charAt(realmPath.length() - 1) != '/') {
                realmPath.append('/');
            }

            realmName = jsonContent.get(REALM_NAME_ATTRIBUTE_NAME).asString();
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
        } catch (RealmLookupException e) {
            return new NotFoundException("Specified Realm is not a valid realm.").asPromise();
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
        return getJsonValue(realmManager, realmName, parentPath);
    }

    @Override
    public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
        return description;
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        return description;
    }

    @Override
    public void addDescriptorListener(Listener listener) {

    }

    @Override
    public void removeDescriptorListener(Listener listener) {

    }
}
