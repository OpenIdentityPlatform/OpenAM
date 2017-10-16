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

import static com.sun.identity.sm.AttributeSchema.Syntax.BOOLEAN;
import static com.sun.identity.sm.AttributeSchema.Syntax.DECIMAL;
import static com.sun.identity.sm.AttributeSchema.Syntax.DECIMAL_NUMBER;
import static com.sun.identity.sm.AttributeSchema.Syntax.DECIMAL_RANGE;
import static com.sun.identity.sm.AttributeSchema.Syntax.NUMBER;
import static com.sun.identity.sm.AttributeSchema.Syntax.NUMBER_RANGE;
import static com.sun.identity.sm.AttributeSchema.Syntax.PERCENT;
import static java.util.Arrays.asList;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openam.core.rest.sms.SmsJsonSchema.*;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.ACTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SMS_RESOURCE_PROVIDER;
import static org.forgerock.openam.rest.RestConstants.COLLECTION;
import static org.forgerock.openam.rest.RestConstants.NAME;
import static org.forgerock.util.i18n.LocalizableString.TRANSLATION_KEY_PREFIX;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.sun.identity.shared.encode.URLEncDec;
import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.models.ApiDescription;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.LocaleContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.util.promise.Promise;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * A base class for resource providers for the REST SMS services - provides common utility methods for
 * navigating SMS schemas. It implements basic functionality such as reading of schema, template and
 * creatable types, while allowing all of those mechanisms to be overridden by more specific subclasses.
 * @since 13.0.0
 */
public abstract class SmsResourceProvider implements Describable<ApiDescription, Request> {

    static final LocalizableString SCHEMA_DESCRIPTION =
            new LocalizableString(SMS_RESOURCE_PROVIDER + ACTION + "schema." + ApiDescriptorConstants.DESCRIPTION);
    static final LocalizableString TEMPLATE_DESCRIPTION =
            new LocalizableString(SMS_RESOURCE_PROVIDER + ACTION + "template." + ApiDescriptorConstants.DESCRIPTION);
    static final ClassLoader CLASS_LOADER = SmsResourceProvider.class.getClassLoader();

    /**
     * Contains the mapping of auto created authentication modules and their type so that
     * requests to the authentication module endpoint can check if they need to check the
     * special place that these auto created modules are stored.
     */
    static final BiMap<String, String> AUTO_CREATED_AUTHENTICATION_MODULES = HashBiMap.create(7);

    static {
        AUTO_CREATED_AUTHENTICATION_MODULES.put("hotp", "hotp");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("sae", "sae");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("oath", "oath");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("ldap", "ldap");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("datastore", "datastore");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("federation", "federation");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("wssauthmodule", "wssauth");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("amster", "amster");
    }

    public static final List<AttributeSchema.Syntax> NUMBER_SYNTAXES = asList(DECIMAL, PERCENT, DECIMAL_RANGE, DECIMAL_NUMBER);
    protected final String serviceName;
    protected final String serviceVersion;
    protected final List<ServiceSchema> subSchemaPath;
    protected final SchemaType type;
    protected final boolean hasInstanceName;
    protected final List<String> uriPath;
    protected final SmsJsonConverter converter;
    protected final Debug debug;
    protected final ServiceSchema schema;
    protected final AMResourceBundleCache resourceBundleCache;
    protected final Locale defaultLocale;

    SmsResourceProvider(ServiceSchema schema, SchemaType type, List<ServiceSchema> subSchemaPath, String uriPath,
            boolean serviceHasInstanceName, SmsJsonConverter converter, Debug debug,
            AMResourceBundleCache resourceBundleCache, Locale defaultLocale) {
        this.schema = schema;
        this.serviceName = schema.getServiceName();
        this.serviceVersion = schema.getVersion();
        this.type = type;
        this.subSchemaPath = subSchemaPath;
        this.uriPath = uriPath == null ? Collections.<String>emptyList() : asList(uriPath.split("/"));
        this.hasInstanceName = serviceHasInstanceName;
        this.converter = converter;
        this.debug = debug;
        this.resourceBundleCache = resourceBundleCache;
        this.defaultLocale = defaultLocale;
    }

    /**
     * Gets the realm from the underlying RealmContext.
     * @param context The Context for the request.
     * @return The resolved realm.
     */
    protected String realmFor(Context context) {
        return context.containsContext(RealmContext.class) ?
                context.asContext(RealmContext.class).getRealm().asPath() : null;
    }

    /**
     * Gets a {@link com.sun.identity.sm.ServiceConfigManager} using the {@link SSOToken} available from the request
     * context.
     * @param context The request's context.
     * @return A newly-constructed {@link ServiceConfigManager} for the appropriate {@link #serviceName} and
     * {@link #serviceVersion}.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     */
    protected ServiceConfigManager getServiceConfigManager(Context context) throws SSOException, SMSException {
        SSOToken ssoToken = context.asContext(SSOTokenContext.class).getCallerSSOToken();
        return new ServiceConfigManager(ssoToken, serviceName, serviceVersion);
    }

    /**
     * Gets the ServiceConfig parent of the parent of the config being addressed by the current request.
     * @param context The request context, from which the path variables can be retrieved.
     * @param scm The {@link com.sun.identity.sm.ServiceConfigManager}. See {@link #getServiceConfigManager(Context)}.
     * @return The ServiceConfig that was found.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     * @throws NotFoundException When some configuration in the parent path does not exist.
     */
    protected ServiceConfig parentSubConfigFor(Context context, ServiceConfigManager scm)
            throws SMSException, SSOException, NotFoundException {

        Map<String, String> uriTemplateVariables = getUriTemplateVariables(context);

        ServiceConfig config;
        if (type == SchemaType.GLOBAL) {
            config = scm.getGlobalConfig(hasInstanceName ? uriTemplateVariables.get("name") : null);
        } else {
            config = scm.getOrganizationConfig(realmFor(context), null);
            if (!SmsRequestHandler.USE_PARENT_PATH.equals(schema.getResourceName()) && !config.exists()) {
                throw new NotFoundException("Parent service does not exist.");
            }
        }

        for (int i = 0; i < subSchemaPath.size() - 1; i++) {
            ServiceSchema schema = subSchemaPath.get(i);
            String subConfigName = schema.getResourceName();

            boolean configNeedsToExist = true;
            if (subConfigName == null || SmsRequestHandler.USE_PARENT_PATH.equals(subConfigName)) {
                subConfigName = schema.getName();
                configNeedsToExist = false;
            }

            if (uriPath.contains("{" + subConfigName + "}")) {
                subConfigName = uriTemplateVariables.get(subConfigName);
                configNeedsToExist = true;
            }

            config = config.getSubConfig(subConfigName);

            if (configNeedsToExist && !config.exists()) {
                throw new NotFoundException("Parent subconfig of type " + subConfigName + " does not exist.");
            }
        }
        return config;
    }

    static Map<String, String> getUriTemplateVariables(Context context) {
        Map<String, String> uriTemplateVariables = new HashMap<>();
        Context c = context;
        while (c.containsContext(UriRouterContext.class)) {
            uriTemplateVariables.putAll(c.asContext(UriRouterContext.class).getUriTemplateVariables());
            c = c.getParent();
        }
        return uriTemplateVariables;
    }

    /**
     * Retrieves the {@link ServiceConfig} instance for the provided resource ID within the provided ServiceConfig
     * parent instance, and checks whether it exists.
     * @param context The request context.
     * @param resourceId The identifier for the config.
     * @param config The parent config instance.
     * @return The found instance.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     * @throws NotFoundException If the ServiceConfig does not exist.
     */
    protected ServiceConfig checkedInstanceSubConfig(Context context, String resourceId, ServiceConfig config)
            throws SSOException, SMSException, NotFoundException {
        if (config.getSubConfigNames().contains(resourceId)) {
            ServiceConfig subConfig = config.getSubConfig(resourceId);
            if (subConfig == null || !subConfig.getSchemaID().equals(lastSchemaNodeName()) || !subConfig.exists()) {
                throw new NotFoundException();
            }
            return subConfig;
        } else {
            /*
             * Use case: The default created auth modules on a fresh install aren't stored in the same
             * place as auth modules created by the user. Therefore if the auth module is not found in
             * the organisation schema we need to check if is one of these auth created modules.
             */
            if (!isDefaultCreatedAuthModule(context, resourceId) || !config.exists()) {
                throw new NotFoundException();
            }
            return config;
        }
    }

    boolean isDefaultCreatedAuthModule(Context context, String resourceId) throws SSOException,
            SMSException {
        String lastedMatchedUri = context.asContext(UriRouterContext.class).getMatchedUri();
        return AMAuthenticationManager.getAuthenticationServiceNames().contains(serviceName)
                && AUTO_CREATED_AUTHENTICATION_MODULES.containsKey(resourceId.toLowerCase())
                && AUTO_CREATED_AUTHENTICATION_MODULES.get(resourceId.toLowerCase()).equalsIgnoreCase(lastedMatchedUri);
    }

    /**
     * Gets the name of the last schema node in the {@link #subSchemaPath}.
     */
    protected String lastSchemaNodeName() {
        return schema.getName();
    }

    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> schema(Context context) {
        return newActionResponse(createSchema(Optional.of(context))).asPromise();
    }

    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> template() {
        return newActionResponse(createTemplate()).asPromise();
    }

    /**
     * Creates json response with attribute defaults when the service has global or default/realm schema.
     *
     * @return json response data; empty json if the service has only dynamic schema
     */
    protected JsonValue createTemplate() {
        if (serviceHasDefaultOrGlobalSchema()) {
            //when retrieving the template we don't want to validate the attributes
            return converter.toJson(schema.getAttributeDefaults(), false);
        }
        // Dynamic attributes default values will be added to the JSON response in the child class SmsSingletonProvider
        return json(object());
    }

    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> getType(Context context) {
        try {
            return newActionResponse(getTypeValue(context)).asPromise();
        } catch (SMSException | SSOException e) {
            return new InternalServerErrorException("Could not get service schema", e).asPromise();
        }
    }

    protected JsonValue getTypeValue(Context context) throws SSOException, SMSException {
        String resourceId = schema.getResourceName();
        for (int i = subSchemaPath.size() - 1; i >= 0 && SmsRequestHandler.USE_PARENT_PATH.equals(resourceId); i--) {
            resourceId = subSchemaPath.get(i).getResourceName();
        }
        if (SmsRequestHandler.USE_PARENT_PATH.equals(resourceId)) {
            SSOToken ssoToken = context.asContext(SSOTokenContext.class).getCallerSSOToken();
                resourceId = new ServiceSchemaManager(ssoToken, serviceName, serviceVersion).getResourceName();
        }
        return json(object(
                field(ResourceResponse.FIELD_CONTENT_ID, resourceId),
                field(NAME, getI18NName()),
                field(COLLECTION, schema.supportsMultipleConfigurations())));
    }

    LocalizableString getI18NName() {
        String i18nKey = schema.getI18NKey();
        String i18nName = schema.getName();
        if (StringUtils.isEmpty(i18nName)) {
            i18nName = schema.getServiceName();
        }
        if (StringUtils.isNotEmpty(i18nKey)) {
            return getSchemaI18N(i18nKey, new LocalizableString(i18nName));
        }
        return new LocalizableString(i18nName);
    }

    private LocalizableString getSchemaI18N(String i18nKey, LocalizableString defaultValue) {
        String i18NFileName = schema.getI18NFileName();
        return new LocalizableString(TRANSLATION_KEY_PREFIX + i18NFileName + "#"+ i18nKey, CLASS_LOADER, defaultValue);
    }

    private LocalizableString getConsoleI18N(String i18nKey, LocalizableString defaultValue) {
        return new LocalizableString(TRANSLATION_KEY_PREFIX + "amConsole" + "#" + URLEncDec.encode(i18nKey), CLASS_LOADER, defaultValue);
    }

    JsonValue createSchema(Optional<Context> context) {
        JsonValue result = json(object(field("type", "object")));
        addGlobalSchema(context, result);
        addOrganisationSchema(context, result);
        addDynamicSchema(context, result);
        return result;
    }

    /**
     * Add the global attribute schema to the given {@link JsonValue} result.
     *  @param context The request context.
     * @param result The response body {@link JsonValue}.
     */
    protected void addGlobalSchema(Optional<Context> context, JsonValue result) {
        if (schema.getServiceType().equals(SchemaType.GLOBAL)) {
            addAttributeSchema(result, "/" + PROPERTIES + "/", schema, context);
        }
    }

    /**
     * Add the organisation attribute schema to the given {@link JsonValue} result. The organisation attribute schema
     * will be added at the root of the JSON response if the request is for realm based schema, but should be added
     * under "defaults" when the request is for global schema, see {@link SmsGlobalSingletonProvider}.
     *  @param context The request context.
     * @param result The response body {@link JsonValue}.
     */
    protected void addOrganisationSchema(Optional<Context> context, JsonValue result) {
        if (schema.getServiceType().equals(SchemaType.ORGANIZATION)) {
            addAttributeSchema(result, "/" + PROPERTIES + "/", schema, context);
        }
    }

    /**
     * Add the dynamic attribute schema to the given {@link JsonValue} result.
     *  @param context The request context.
     * @param result The response body {@link JsonValue}.
     */
    protected void addDynamicSchema(Optional<Context> context, JsonValue result) {
        // Dynamic schema will be added in SmsSingletonProvider
    }

    /**
     * Add the global attributes to the given {@link JsonValue} result.
     *
     * @param config The SMS config from which to read the attributes.
     * @param result The response body {@link JsonValue}.
     */
    @SuppressWarnings("unchecked")
    protected void addGlobalAttributes(ServiceConfig config, JsonValue result) {
        if (schema.getServiceType().equals(SchemaType.GLOBAL) && config != null) {
            converter.toJson(config.getAttributes(), false, result);
        }
    }

    /**
     * Add the organisation attributes to the given {@link JsonValue} result. The organisation attributes will be
     * added at the root of the JSON response if the request is for realm based attributes, but should be added
     * under "defaults" when the request is for global attributes, see {@link SmsGlobalSingletonProvider}.
     *
     * @param realm The realm/organisation where the attributes are stored.
     * @param config The SMS config from which to read the attributes.
     * @param result The response body {@link JsonValue}.
     */
    @SuppressWarnings("unchecked")
    protected void addOrganisationAttributes(String realm, ServiceConfig config, JsonValue result) {
        if (schema.getServiceType().equals(SchemaType.ORGANIZATION) && config != null) {
            converter.toJson(realm, config.getAttributes(), false, result);
        }
    }

    /**
     * Add the dynamic attributes to the given {@link JsonValue} result.
     *
     * @param realm The realm/organisation where the attributes are stored.
     * @param result The response body {@link JsonValue}.
     */
    protected void addDynamicAttributes(String realm, JsonValue result) {
        // Dynamic attributes will be added in SmsSingletonProvider
    }

    /**
     * Returns the JsonValue representation of the ServiceConfig using the {@link #converter}. Adds a {@code _id}
     * property for the name of the config.
     */
    protected final JsonValue getJsonValue(String realm, ServiceConfig config, Context context) throws
            InternalServerErrorException {
        return getJsonValue(realm, config, context, null, false);
    }

    /**
     * Returns the JsonValue representation of the ServiceConfig using the {@link #converter}. Adds a {@code _id}
     * property for the name of the config.
     */
    protected final JsonValue getJsonValue(String realm, ServiceConfig config, Context context,
            String authModuleResourceName, boolean autoCreatedAuthModule) throws InternalServerErrorException {
        JsonValue value = json(object());

        addGlobalAttributes(config, value);
        addOrganisationAttributes(realm, config, value);
        addDynamicAttributes(realm, value);

        String id = (null != config) ? config.getName() : "";
        if (autoCreatedAuthModule && StringUtils.isEmpty(id)) {
            id = AUTO_CREATED_AUTHENTICATION_MODULES.inverse().get(authModuleResourceName);
        }
        value.add("_id", id);
        try {
            value.add("_type", getTypeValue(context).getObject());
        } catch (SSOException | SMSException e) {
            debug.error("Error reading type for " + authModuleResourceName, e);
            throw new InternalServerErrorException();
        }
        return value;
    }

    protected boolean serviceHasDefaultOrGlobalSchema() {
        return !schema.getServiceType().equals(SchemaType.DYNAMIC);
    }

    protected void addAttributeSchema(JsonValue result, String path, ServiceSchema schemas, Optional<Context> context) {
        Map<String, String> attributeSectionMap = getAttributeNameToSection(schemas);
        String serviceType = schemas.getServiceType().getType();
        List<String> sections = getSections(attributeSectionMap, serviceType);

        for (AttributeSchema attribute : schemas.getAttributeSchemas()) {
            String i18NKey = attribute.getI18NKey();
            if (i18NKey != null && i18NKey.length() > 0) {
                String attributePath = attribute.getResourceName();
                if (!sections.isEmpty()) {
                    String section = attributeSectionMap.get(attribute.getName());
                    if (section != null) {
                        String sectionLabel = "section.label." + serviceName + "." + serviceType + "." + section;
                        attributePath = section + "/" + PROPERTIES + "/" + attributePath;
                        result.putPermissive(new JsonPointer(path + section + "/" + TYPE), OBJECT_TYPE);
                        result.putPermissive(new JsonPointer(path + section + "/" + TITLE),
                                getTitle(sectionLabel));
                        result.putPermissive(new JsonPointer(path + section + "/" + PROPERTY_ORDER),
                                sections.indexOf(section));
                    }
                }

                Integer propertyOrder = attribute.getOrder();
                result.addPermissive(new JsonPointer(path + attributePath + "/" + TITLE), getSchemaI18N(i18NKey, null));
                result.addPermissive(new JsonPointer(path + attributePath + "/" + DESCRIPTION),
                        getSchemaDescription(i18NKey));
                result.addPermissive(new JsonPointer(path + attributePath + "/" + PROPERTY_ORDER), propertyOrder);
                result.addPermissive(new JsonPointer(path + attributePath + "/" + REQUIRED), !attribute.isOptional());
                addType(result, path + attributePath, attribute, context);
                addExampleValue(result, path, attribute, attributePath);
            }
        }
    }

    private LocalizableString getTitle(String title) {
        return getConsoleI18N(title, getSchemaI18N(title, null));
    }

    private List<String> getSections(Map<String, String> attributeSectionMap, String serviceType) {

        List<String> sections = new ArrayList<>();

        try {
            ResourceBundle console = ResourceBundle.getBundle("amConsole");
            sections.addAll(asList(console.getString("sections." + serviceName + "." + serviceType).split("\\s+")));
        } catch (MissingResourceException e) {
            // ignore - no sections
        }

        if (sections.isEmpty()) {
            for (String attributeSection : attributeSectionMap.values()) {
                if (!sections.contains(attributeSection)) {
                    sections.add(attributeSection);
                }
            }
        }
        return sections;
    }

    private void addExampleValue(JsonValue result, String path, AttributeSchema attribute, String attributePath) {
        final Iterator iterator = attribute.getExampleValues().iterator();
        String exampleValue = "";
        if (iterator.hasNext()) {
            exampleValue = (String) iterator.next();
        }
        result.addPermissive(new JsonPointer(path + attributePath + "/" + EXAMPLE_VALUE), exampleValue);
    }

    LocalizableString getSchemaDescription(String i18NKey) {
        final LocalizableString help = getSchemaI18N(i18NKey + ".help", new LocalizableString(""));
        final LocalizableString helpTxt = getSchemaI18N(i18NKey + ".help.txt", new LocalizableString(""));
        return getSchemaDescription(help, helpTxt);
    }

    static LocalizableString getSchemaDescription(final LocalizableString help, final LocalizableString helpTxt) {
        return new LocalizableString("help description") {
            @Override
            public String toTranslatedString(PreferredLocales locales) {
                String helpValue = help.toTranslatedString(locales);
                String helpTxtValue = helpTxt.toTranslatedString(locales);
                if (helpValue.isEmpty()) {
                    return helpTxtValue;
                } else if (helpTxtValue.isEmpty()) {
                    return helpValue;
                }
                return helpValue + "<br><br>" + helpTxtValue;
            }
        };
    }

    private void addType(JsonValue result, String pointer, AttributeSchema attribute, Optional<Context> context) {
        String type = null;
        AttributeSchema.Type attributeType = attribute.getType();
        AttributeSchema.Syntax syntax = attribute.getSyntax();
        if (attributeType == AttributeSchema.Type.LIST && (
                attribute.getUIType() == AttributeSchema.UIType.GLOBALMAPLIST ||
                attribute.getUIType() == AttributeSchema.UIType.MAPLIST)) {
            type = OBJECT_TYPE;
            JsonValue fieldType = json(object());
            if (attribute.hasChoiceValues()) {
                addEnumChoices(fieldType, attribute, context);
            } else {
                fieldType.add(TYPE, STRING_TYPE);
            }
            result.addPermissive(new JsonPointer(pointer + "/" + PATTERN_PROPERTIES),
                    object(field(".*", fieldType.getObject())));
        } else if (attributeType == AttributeSchema.Type.LIST) {
            type = ARRAY_TYPE;
            result.addPermissive(new JsonPointer(pointer + "/" + ITEMS),
                    object(field(TYPE, getTypeFromSyntax(attribute.getSyntax()))));
            if (attribute.hasChoiceValues()) {
                addEnumChoices(result.get(new JsonPointer(pointer + "/" + ITEMS)), attribute, context);
            }
        } else if (attributeType.equals(AttributeSchema.Type.MULTIPLE_CHOICE)) {
            type = ARRAY_TYPE;
            result.addPermissive(new JsonPointer(pointer + "/" + ITEMS),
                    object(field(TYPE, getTypeFromSyntax(attribute.getSyntax()))));
            addEnumChoices(result.get(new JsonPointer(pointer + "/" + ITEMS)), attribute, context);
        } else if (attributeType.equals(AttributeSchema.Type.SINGLE_CHOICE)) {
            addEnumChoices(result.get(new JsonPointer(pointer)), attribute, context);
            type = getTypeFromSyntax(syntax);
        } else {
            type = getTypeFromSyntax(syntax);
        }
        if (type != null) {
            result.addPermissive(new JsonPointer(pointer + "/" + TYPE), type);
        } else {
            debug.warning("Could not find type for attribute {}", attribute);
        }
        if (AttributeSchema.Syntax.PASSWORD.equals(syntax)) {
            result.addPermissive(new JsonPointer(pointer + "/" + FORMAT), PASSWORD_TYPE);
        }
    }

    private void addEnumChoices(JsonValue jsonValue, AttributeSchema attribute, Optional<Context> context) {
        if (context.isPresent()) {
            List<String> values = new ArrayList<String>();
            List<LocalizableString> descriptions = new ArrayList<>();
            Map environment = type == SchemaType.GLOBAL ? Collections.emptyMap() :
                    Collections.singletonMap(Constants.ORGANIZATION_NAME, realmFor(context.get()));
            Map<String, String> valuesMap = attribute.getChoiceValuesMap(environment);
            for (Map.Entry<String, String> value : valuesMap.entrySet()) {
                values.add(value.getKey());
                if (AttributeSchema.UIType.SCRIPTSELECT.equals(attribute.getUIType())
                        || AttributeSchema.UIType.GLOBALSCRIPTSELECT.equals(attribute.getUIType())) {
                    descriptions.add(getConsoleI18N(value.getValue(), new LocalizableString(value.getValue())));
                } else {
                    descriptions.add(getConsoleI18N(value.getValue() == null ? value.getKey() : value.getValue(),
                            new LocalizableString(value.getKey())));
                }
            }
            jsonValue.add(ENUM, values);
            jsonValue.putPermissive(new JsonPointer("options/enum_titles"), descriptions);
        }
    }

    private String getTypeFromSyntax(AttributeSchema.Syntax syntax) {
        String type;
        if (syntax == BOOLEAN) {
            type = BOOLEAN_TYPE;
        } else if (syntax == NUMBER_RANGE || syntax == NUMBER) {
            type = INTEGER;
        } else if (NUMBER_SYNTAXES.contains(syntax)) {
            type = NUMBER_TYPE;
        } else {
            type = STRING_TYPE;
        }
        return type;
    }

    protected Map<String, String> getAttributeNameToSection(ServiceSchema schema) {
        Map<String, String> result = new LinkedHashMap<>();

        String serviceSectionFilename = schema.getName() != null ? schema.getName() : schema.getServiceName();
        serviceSectionFilename = serviceSectionFilename + ".section.properties";

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(serviceSectionFilename);

        if (inputStream != null) {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                while ((line = reader.readLine()) != null) {
                    if (!(line.matches("^\\#.*") || line.isEmpty())) {
                        String[] attributeValue = line.split("=");
                        final String sectionName = attributeValue[0];
                        result.put(attributeValue[1], sectionName);
                    }
                }
            } catch (IOException e) {
                if (debug.errorEnabled()) {
                    debug.error("Error reading section properties file", e);
                }
            }
        }
        return result;
    }

    protected Locale getLocale(Context context) {
        return context.asContext(LocaleContext.class).getLocale();
    }
}
