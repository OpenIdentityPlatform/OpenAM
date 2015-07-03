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

package org.forgerock.openam.rest.sms;

import static com.sun.identity.sm.AttributeSchema.Syntax.*;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.openam.rest.sms.SmsJsonSchema.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.StringUtils;

/**
 * A base class for resource providers for the REST SMS services - provides common utility methods for
 * navigating SMS schemas.
 * @since 13.0.0
 */
abstract class SmsResourceProvider {

    /**
     * Contains the mapping of auto created authentication modules and their type so that
     * requests to the authentication module endpoint can check if they need to check the
     * special place that these auto created modules are stored.
     */
    private static final Map<String, String> AUTO_CREATED_AUTHENTICATION_MODULES = new HashMap<>();

    static {
        AUTO_CREATED_AUTHENTICATION_MODULES.put("hotp", "hotp");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("sae", "sae");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("oath", "oath");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("ldap", "ldap");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("datastore", "datastore");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("federation", "federation");
        AUTO_CREATED_AUTHENTICATION_MODULES.put("wssauthmodule", "wssauthmodule");
    }

    public static final List<AttributeSchema.Syntax> NUMBER_SYNTAXES = Arrays.asList(NUMBER, DECIMAL, PERCENT, NUMBER_RANGE, DECIMAL_RANGE, DECIMAL_NUMBER);
    static final String TEMPLATE = "template";
    static final String SCHEMA = "schema";
    protected final String serviceName;
    protected final String serviceVersion;
    protected final List<ServiceSchema> subSchemaPath;
    protected final SchemaType type;
    protected final boolean hasInstanceName;
    protected final List<String> uriPath;
    protected final SmsJsonConverter converter;
    protected final Debug debug;
    protected final ServiceSchema schema;

    SmsResourceProvider(ServiceSchema schema, SchemaType type, List<ServiceSchema> subSchemaPath, String uriPath,
            boolean serviceHasInstanceName, SmsJsonConverter converter, Debug debug) {
        this.schema = schema;
        this.serviceName = schema.getServiceName();
        this.serviceVersion = schema.getVersion();
        this.type = type;
        this.subSchemaPath = subSchemaPath;
        this.uriPath = uriPath == null ? Collections.<String>emptyList() : Arrays.asList(uriPath.split("/"));
        this.hasInstanceName = serviceHasInstanceName;
        this.converter = converter;
        this.debug = debug;
    }

    /**
     * Gets the realm from the underlying RealmContext.
     * @param context The ServerContext for the request.
     * @return The resolved realm.
     */
    protected String realmFor(ServerContext context) {
        return context.asContext(RealmContext.class).getResolvedRealm();
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
    protected ServiceConfigManager getServiceConfigManager(ServerContext context) throws SSOException, SMSException {
        SSOToken ssoToken = context.asContext(SSOTokenContext.class).getCallerSSOToken();
        return new ServiceConfigManager(ssoToken, serviceName, serviceVersion);
    }

    /**
     * Gets the ServiceConfig parent of the parent of the config being addressed by the current request.
     * @param context The request context, from which the path variables can be retrieved.
     * @param scm The {@link com.sun.identity.sm.ServiceConfigManager}. See {@link #getServiceConfigManager(ServerContext)}.
     * @return The ServiceConfig that was found.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     */
    protected ServiceConfig parentSubConfigFor(ServerContext context, ServiceConfigManager scm)
            throws SMSException, SSOException {
        String name = null;
        Map<String, String> uriTemplateVariables = context.asContext(RouterContext.class).getUriTemplateVariables();
        if (hasInstanceName) {
            name = uriTemplateVariables.get("name");
        }
        ServiceConfig config = type == SchemaType.GLOBAL ?
                scm.getGlobalConfig(name) : scm.getOrganizationConfig(realmFor(context), null);
        for (int i = 0; i < subSchemaPath.size() - 1; i++) {
            ServiceSchema schema = subSchemaPath.get(i);
            String pathFragment = schema.getResourceName();
            if (pathFragment == null || "USE-PARENT".equals(pathFragment)) {
                pathFragment = schema.getName();
            }
            if (uriPath.contains("{" + pathFragment + "}")) {
                pathFragment = uriTemplateVariables.get(pathFragment);
            }
            config = config.getSubConfig(pathFragment);
        }
        return config;
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
    protected ServiceConfig checkedInstanceSubConfig(ServerContext context, String resourceId, ServiceConfig config)
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

    private boolean isDefaultCreatedAuthModule(ServerContext context, String resourceId) throws SSOException,
            SMSException {
        String lastedMatchedUri = context.asContext(RouterContext.class).getMatchedUri();
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

    protected void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        if (request.getAction().equals(TEMPLATE)) {
            handler.handleResult(converter.toJson(schema.getAttributeDefaults()));
        } else if (SCHEMA.equals(request.getAction())) {
            handler.handleResult(createSchema(context));
        } else {
            handler.handleError(new NotSupportedException("Action not supported: " + request.getAction()));
        }
    }

    protected JsonValue createSchema(ServerContext context) {
        JsonValue result = json(object());

        Map<String, String> attributeSectionMap = getAttributeNameToSection(schema);
        ResourceBundle console = ResourceBundle.getBundle("amConsole");
        String serviceType = schema.getServiceType().getType();

        String sectionOrder = getConsoleString(console, "sections." + serviceName + "." + serviceType);
        List<String> sections = new ArrayList<>();
        if (StringUtils.isNotEmpty(sectionOrder)) {
            sections.addAll(Arrays.asList(sectionOrder.split("\\s+")));
        }

        addAttributeSchema(result, "/" + PROPERTIES + "/", schema, sections, attributeSectionMap, console, serviceType,
                context);

        return result;
    }

    protected void addAttributeSchema(JsonValue result, String path, ServiceSchema schema, List<String> sections,
            Map<String, String> attributeSectionMap, ResourceBundle console, String serviceType, ServerContext context) {

        ResourceBundle i18n = ResourceBundle.getBundle(schema.getI18NFileName());
        NumberFormat sectionFormat = new DecimalFormat("00");

        for (AttributeSchema attribute : (Set<AttributeSchema>) schema.getAttributeSchemas()) {
            String i18NKey = attribute.getI18NKey();
            if (i18NKey != null && i18NKey.length() > 0) {
                String attributePath = attribute.getResourceName();
                if (!sections.isEmpty()) {
                    String section = attributeSectionMap.get(attribute.getName());
                    String sectionLabel = "section.label." + serviceName + "." + serviceType + "." + section;
                    attributePath = section + "/" + PROPERTIES + "/" + attributePath;
                    result.putPermissive(new JsonPointer(path + section + "/" + TYPE), OBJECT_TYPE);

                    result.putPermissive(new JsonPointer(path + section + "/" + TITLE),
                            getConsoleString(console, sectionLabel));
                    result.putPermissive(new JsonPointer(path + section + "/" + PROPERTY_ORDER),
                            "z" + sectionFormat.format(sections.indexOf(section)));
                }
                result.addPermissive(new JsonPointer(path + attributePath + "/" + TITLE),
                        i18n.getString(i18NKey));

                result.addPermissive(new JsonPointer(path + attributePath + "/" + DESCRIPTION), getSchemaDescription(i18n, i18NKey));
                result.addPermissive(new JsonPointer(path + attributePath + "/" + PROPERTY_ORDER), i18NKey);
                result.addPermissive(new JsonPointer(path + attributePath + "/" + REQUIRED), !attribute.isOptional());
                addType(result, path + attributePath, attribute, i18n, context);
            }
        }
    }

    static String getSchemaDescription(ResourceBundle i18n, String i18NKey) {
        StringBuilder description = new StringBuilder();
        if (i18n.containsKey(i18NKey + ".help")) {
            description.append(i18n.getString(i18NKey + ".help"));
        }
        if (i18n.containsKey(i18NKey + ".help.txt")) {
            if (description.length() > 0) {
                description.append("<br><br>");
            }
            description.append(i18n.getString(i18NKey + ".help.txt"));
        }
        return description.toString();
    }

    protected String getConsoleString(ResourceBundle console, String key) {
        try {
            return console.getString(key);
        } catch (MissingResourceException e) {
            return "";
        }
    }

    private void addType(JsonValue result, String pointer, AttributeSchema attribute, ResourceBundle i18n, ServerContext context) {
        String type = null;
        AttributeSchema.Type attributeType = attribute.getType();
        AttributeSchema.Syntax syntax = attribute.getSyntax();
        if (attributeType == AttributeSchema.Type.LIST && (
                attribute.getUIType() == AttributeSchema.UIType.GLOBALMAPLIST ||
                attribute.getUIType() == AttributeSchema.UIType.MAPLIST)) {
            type = OBJECT_TYPE;
            JsonValue fieldType = json(object());
            if (attribute.hasChoiceValues()) {
                addEnumChoices(fieldType, attribute, i18n, context);
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
                addEnumChoices(result.get(new JsonPointer(pointer + "/" + ITEMS)), attribute, i18n, context);
            }
        } else if (attributeType.equals(AttributeSchema.Type.MULTIPLE_CHOICE)) {
            type = ARRAY_TYPE;
            result.addPermissive(new JsonPointer(pointer + "/" + ITEMS),
                    object(field(TYPE, getTypeFromSyntax(attribute.getSyntax()))));
            addEnumChoices(result.get(new JsonPointer(pointer + "/" + ITEMS)), attribute, i18n, context);
        } else if (attributeType.equals(AttributeSchema.Type.SINGLE_CHOICE)) {
            addEnumChoices(result.get(new JsonPointer(pointer)), attribute, i18n, context);
        } else {
            type = getTypeFromSyntax(syntax);
        }
        if (type != null) {
            result.addPermissive(new JsonPointer(pointer + "/" + TYPE), type);
        }
    }

    private void addEnumChoices(JsonValue jsonValue, AttributeSchema attribute, ResourceBundle i18n,
            ServerContext context) {
        List<String> values = new ArrayList<String>();
        List<String> descriptions = new ArrayList<String>();
        Map environment = type == SchemaType.GLOBAL ? Collections.emptyMap() :
                Collections.singletonMap(Constants.ORGANIZATION_NAME, realmFor(context));
        Map<String, String> valuesMap = attribute.getChoiceValuesMap(environment);
        for (Map.Entry<String, String> value : valuesMap.entrySet()) {
            values.add(value.getKey());
            if (i18n.containsKey(value.getValue())) {
                descriptions.add(i18n.getString(value.getValue()));
            } else {
                descriptions.add(value.getKey());
            }
        }
        jsonValue.add(ENUM, values);
        jsonValue.putPermissive(new JsonPointer("options/enum_titles"), descriptions);
    }

    private String getTypeFromSyntax(AttributeSchema.Syntax syntax) {
        String type;
        if (syntax == BOOLEAN) {
            type = BOOLEAN_TYPE;
        } else if (NUMBER_SYNTAXES.contains(syntax)) {
            type = NUMBER_TYPE;
        } else {
            type = STRING_TYPE;
        }
        return type;
    }

    protected HashMap<String, String> getAttributeNameToSection(ServiceSchema schema) {
        HashMap<String, String> result = new HashMap<String, String>();

        String serviceSectionFilename = schema.getServiceName() + ".section.properties";
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

}
