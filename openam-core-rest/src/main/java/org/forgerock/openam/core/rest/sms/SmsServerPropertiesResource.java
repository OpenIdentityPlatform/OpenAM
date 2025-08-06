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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.sms;

import static com.sun.identity.common.configuration.ServerConfigXML.ServerObject;
import static com.sun.identity.common.configuration.ServerConfiguration.getServerConfigXML;
import static com.sun.identity.common.configuration.ServerConfiguration.getServerID;
import static com.sun.identity.common.configuration.ServerConfiguration.getServerSite;
import static com.sun.identity.common.configuration.ServerConfiguration.getServers;
import static com.sun.identity.common.configuration.ServerConfiguration.removeServerConfiguration;
import static com.sun.identity.common.configuration.ServerConfiguration.setServerConfigXML;
import static com.sun.identity.common.configuration.ServerConfiguration.setServerInstance;
import static com.sun.identity.common.configuration.ServerConfiguration.setServerSite;
import static com.sun.identity.common.configuration.SiteConfiguration.getSites;
import static com.sun.identity.setup.SetupConstants.CONFIG_VAR_DEFAULT_SHARED_KEY;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.text.MessageFormat.format;
import static org.forgerock.api.enums.ParameterSource.PATH;
import static org.forgerock.api.models.Action.action;
import static org.forgerock.api.models.ApiDescription.apiDescription;
import static org.forgerock.api.models.Parameter.parameter;
import static org.forgerock.api.models.Reference.reference;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.core.rest.sms.SmsResourceProvider.SCHEMA_DESCRIPTION;
import static org.forgerock.openam.core.rest.sms.SmsResourceProvider.TEMPLATE_DESCRIPTION;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.CONSOLE;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SERVER_PROPERTIES;
import static org.forgerock.openam.rest.DescriptorUtils.fromResource;
import static org.forgerock.openam.utils.IOUtils.readStream;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.openam.utils.StringUtils.isEmpty;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.Update;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.promise.Promise;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.ServerConfigXML;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * A service to allow the modification of server properties
 */
@RequestHandler(@Handler(mvccSupported = false,
        resourceSchema = @org.forgerock.api.annotations.Schema(fromType = String.class)))
public class SmsServerPropertiesResource implements Describable<ApiDescription, Request> {

    private static final ClassLoader CLASS_LOADER = SmsServerPropertiesResource.class.getClassLoader();

    private static final String SCHEMA_NAME = "com-sun-identity-servers";
    private static final String SERVER_CONFIG = "serverconfig";
    private static final String DIRECTORY_CONFIG_SCHEMA = "/schema/json/server-directory-configuration.json";
    private static final String DIRECTORY_CONFIGURATION_TAB_NAME = "directoryConfiguration";
    private static final String ADVANCED_TAB_NAME = "advanced";
    private static final String GENERAL_TAB_NAME = "general";
    private static final String PARENT_SITE_PROPERTY = "singleChoiceSite";
    /**
     * The name of the defaults 'server'.
     */
    public static final String SERVER_DEFAULT_NAME = "server-default";
    private static final String SERVER_PARENT_SITE = "amconfig.header.site";
    private static final Map<String, String> syntaxRawToReal = new HashMap<>();
    //this list is to enable us to distinguish which attributes are in the "advanced" tab
    private static final List<String> allAttributeNamesInNamedTabs = new ArrayList<>();
    private static final String UNKNOWN_PROPS = "serverconfig.updated.with.invalid.properties";
    private static final String EMPTY_SELECTION = "[Empty]";
    private static final Schema ADVANCED_SCHEMA
            = fromResource("SmsServerPropertiesResource.advanced.schema.json", SmsServerPropertiesResource.class);

    static {
        syntaxRawToReal.put("true,false", "boolean");
        syntaxRawToReal.put("false,true", "boolean");
        syntaxRawToReal.put("integer", "number");
        syntaxRawToReal.put("on,off", "on,off");
        syntaxRawToReal.put("off,on", "on,off");
        syntaxRawToReal.put("", "string");
    }

    private final Debug logger;
    private final Properties syntaxProperties;
    private final String tabName;
    private final Schema schema;
    private final ApiDescription descriptor;
    private final boolean isServerDefault;

    @Inject
    public SmsServerPropertiesResource(@Named("ServerAttributeSyntax") Properties syntaxProperties,
            @Named("frRest") Debug logger, @Assisted String tabName, @Assisted boolean isServerDefault) {
        this.logger = logger;
        this.syntaxProperties = syntaxProperties;
        this.tabName = tabName;
        this.isServerDefault = isServerDefault;
        if (ADVANCED_TAB_NAME.equals(tabName)) {
            schema = ADVANCED_SCHEMA;
        } else if (DIRECTORY_CONFIGURATION_TAB_NAME.equalsIgnoreCase(tabName)) {
            schema = Schema.schema().schema(getDirectorySchema()).build();
        } else {
            schema = Schema.schema().schema(getSchema()).build();
        }
        this.descriptor = getApiDescriptor();
    }

    private ApiDescription getApiDescriptor() {
        Resource.Builder resourceBuilder = Resource.resource();
        if (!isServerDefault) {
            resourceBuilder.parameter(parameter().name("serverName").description(apiString("servername.description"))
                    .type("string").source(PATH).build());

        }
        return apiDescription().id("not-used").version("not-used")
                .paths(Paths.paths().put("", VersionedPath.versionedPath()
                        .put(VersionedPath.UNVERSIONED, resourceBuilder
                                .title(perServerApiString(tabName + ".title"))
                                .description(apiString(tabName + ".description"))
                                .mvccSupported(false)
                                .resourceSchema(schema)
                                .read(org.forgerock.api.models.Read.read().build())
                                .update(org.forgerock.api.models.Update.update().build())
                                .action(action().name("schema").description(SCHEMA_DESCRIPTION)
//                                        .response(Schema.schema()
//                                                .reference(reference().value("http://json-schema.org/draft-04/schema#")
                                                        .build())//.build()).build())
                                .build())
                        .build())
                .build()).build();
    }

    private LocalizableString apiString(String key) {
        return new LocalizableString(SERVER_PROPERTIES + key, CLASS_LOADER);
    }

    private LocalizableString perServerApiString(String key) {
        return apiString((isServerDefault ? "defaults." : "") + key);
    }

    private JsonValue getDirectorySchema() {
        try {
            String schema = readStream(getClass().getResourceAsStream(DIRECTORY_CONFIG_SCHEMA));
            JsonValue directoryConfigSchema = JsonValueBuilder.toJsonValue(schema);
            replacePropertyRecursive(directoryConfigSchema, "title");
            return directoryConfigSchema;
        } catch (IOException e) {
            logger.error("Error creating document builder", e);
            throw new IllegalStateException("Cannot load directory schema config", e);
        }
    }

    private void replacePropertyRecursive(JsonValue jsonValue, String property) {
        if (jsonValue.isDefined(property) && jsonValue.get(property).isString()) {
            String propValue = jsonValue.get(property).asString();
            try {
                jsonValue.put(property, consoleString(propValue));
            } catch (MissingResourceException e) {
                // Ignore - retain original value
            }
        }

        for (JsonValue child : jsonValue) {
            if (child.isMap()) {
                replacePropertyRecursive(child, property);
            }
        }
    }

    private LocalizableString consoleString(String propValue) {
        return new LocalizableString(CONSOLE + propValue, CLASS_LOADER);
    }

    private LocalizableString consoleStringWithDefault(String propValue, String defaultValue) {
        return new LocalizableString(CONSOLE + propValue, CLASS_LOADER, new LocalizableString(defaultValue));
    }

    private JsonValue getSchema() {
        JsonValue schema = json(object(field("type", "object")));
        try {
            Document propertySheet = getPropertySheet();
            Map<String, List<String>> options = getOptions(propertySheet);
            Map<String, List<LocalizableString>> optionLabels = getOptionLabels(propertySheet);
            List<String> sectionNames = getSectionNames(propertySheet);
            Set<String> optionalAttributes = getOptionalAttributes(propertySheet, tabName);
            Set<String> passwordFields = getPasswordFields(propertySheet);

            int sectionOrder = 0;
            for (String sectionName : sectionNames) {
                if (isServerDefault && SERVER_PARENT_SITE.equals(sectionName)) {
                    continue;
                }
                final String sectionPath = "/properties/" + sectionName;

                schema.putPermissive(new JsonPointer(sectionPath + "/title"), consoleString(sectionName));
                schema.putPermissive(new JsonPointer(sectionPath + "/type"), "object");
                schema.putPermissive(new JsonPointer(sectionPath + "/propertyOrder"), sectionOrder++);

                int attributeOrder = 0;

                for (SMSLabel label : getLabels(sectionName, propertySheet, options, optionLabels,
                        passwordFields)) {
                    String attributeName = getAttributeNameFromCcName(label.getLabelFor());
                    if (isServerDefault || isParentSiteAttribute(attributeName)) {
                        addDefaultSchema(schema, sectionPath, label, attributeOrder, optionalAttributes);
                    } else {
                        addServerSchema(schema, sectionPath, label, attributeOrder);
                    }
                    attributeOrder++;
                }
            }
        } catch (InternalServerErrorException | NotFoundException | XPathExpressionException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        }

        return schema;
    }

    private void addDefaultSchema(JsonValue template, String sectionPath, SMSLabel label, int attributeOrder,
            Set<String> optionalAttributes) {

        final String attributeName = getAttributeNameFromCcName(label.getLabelFor());
        if (Constants.AM_SERVICES_SECRET.equals(attributeName)) {
            return;
        }
        final LocalizableString title = label.getDisplayValue();
        String type = label.getType();
        final LocalizableString description = label.getDescription();
        final List<String> attributeOptions = label.getOptions();
        final List<LocalizableString> attributeOptionLabels = label.getOptionLabels();
        final boolean isParentSiteAttribute = isParentSiteAttribute(attributeName);
        final boolean isOptional = optionalAttributes.contains(attributeName) || isParentSiteAttribute;

        final String path = sectionPath + "/properties/" + attributeName;
        if (CollectionUtils.isNotEmpty(attributeOptions) || isParentSiteAttribute) {
            template.putPermissive(new JsonPointer(path + "/enum"), attributeOptions);
            template.putPermissive(new JsonPointer(path + "/options/enum_titles"), attributeOptionLabels);
            type = "string";
        }
        template.putPermissive(new JsonPointer(path + "/type"), type);

        template.putPermissive(new JsonPointer(path + "/title"), title);
        template.putPermissive(new JsonPointer(path + "/propertyOrder"), attributeOrder);
        template.putPermissive(new JsonPointer(path + "/required"), !isOptional);
        template.putPermissive(new JsonPointer(path + "/description"), description);

        if (label.isPasswordField()) {
            template.putPermissive(new JsonPointer(path + "/format"), "password");
        }

        allAttributeNamesInNamedTabs.add(attributeName);
    }

    private void addServerSchema(JsonValue template, String sectionPath, SMSLabel label, int attributeOrder) {
        final LocalizableString title = label.getDisplayValue();
        String type = label.getType();
        final LocalizableString description = label.getDescription();
        final String attributeName = getAttributeNameFromCcName(label.getLabelFor());
        final List<String> attributeOptions = label.getOptions();
        final List<LocalizableString> attributeOptionLabels = label.getOptionLabels();
        final String propertyPath = sectionPath + "/properties/" + attributeName;
        final String valuePath = propertyPath + "/properties/value";
        final String inheritedPath = propertyPath + "/properties/inherited";

        template.putPermissive(new JsonPointer(propertyPath + "/title"), title);
        template.putPermissive(new JsonPointer(propertyPath + "/type"), "object");
        template.putPermissive(new JsonPointer(propertyPath + "/propertyOrder"), attributeOrder);
        template.putPermissive(new JsonPointer(propertyPath + "/description"), description);

        if (CollectionUtils.isNotEmpty(attributeOptions)) {
            template.putPermissive(new JsonPointer(valuePath + "/enum"), attributeOptions);
            template.putPermissive(new JsonPointer(valuePath + "/options/enum_titles"), attributeOptionLabels);
            type = "string";
        }
        template.putPermissive(new JsonPointer(valuePath + "/type"), type);
        template.putPermissive(new JsonPointer(valuePath + "/required"), false);

        if (label.isPasswordField()) {
            template.putPermissive(new JsonPointer(valuePath + "/format"), "password");
        }

        template.putPermissive(new JsonPointer(inheritedPath + "/type"), "boolean");
        template.putPermissive(new JsonPointer(inheritedPath + "/required"), true);

        allAttributeNamesInNamedTabs.add(attributeName);
    }

    private List<String> getAttributeOptions(Map<String, List<String>> options, String attributeName, String syntax) {
        List<String> attributeOptions;
        if (syntax != null && syntax.equals("on,off")) {
            final List<String> onOffOptions = new ArrayList<>();
            onOffOptions.add("on");
            onOffOptions.add("off");
            attributeOptions = onOffOptions;
        } else {
            attributeOptions = options.get(attributeName);
        }

        return attributeOptions;
    }

    private List<LocalizableString> getAttributeOptionsLabels(Map<String, List<LocalizableString>> options,
            String attributeName, String syntax) {
        List<LocalizableString> attributeOptions;
        if (syntax != null && syntax.equals("on,off")) {
            attributeOptions = new ArrayList<>();
            attributeOptions.add(consoleString("i18nOn"));
            attributeOptions.add(consoleString("i18nOff"));
        } else {
            attributeOptions = options.get(attributeName);
        }

        return attributeOptions;
    }

    private Set<String> getOptionalAttributes(Document propertySheet, String tabName) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        Set<String> optionalValues = new HashSet<>();

        try {
            String expression = "//propertysheet/section/property[@required='false']/cc/@name";
            NodeList optionalValuesList = (NodeList) xPath.compile(expression).evaluate(propertySheet,
                    XPathConstants.NODESET);

            for (int i = 0; i < optionalValuesList.getLength(); i++) {
                optionalValues.add(optionalValuesList.item(i).getNodeValue());
            }

        } catch (XPathExpressionException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        }

        return optionalValues;
    }

    private String getCcNameFromAttributeName(String defaultValueName) {
        return "csc".concat(defaultValueName.replace('.', '-'));
    }

    private String getAttributeNameFromCcName(String ccName) {
        return ccName.replaceFirst("csc", "").replaceAll("-", ".");
    }

    @Action(operationDescription = @Operation)
    public Promise<ActionResponse, ResourceException> schema(ActionRequest request, Context serverContext) {
        Map<String, String> uriVariables = getUriTemplateVariables(serverContext);

        final String serverId = uriVariables.get("serverName");
        if (!isServerDefault && serverId == null) {
            return new BadRequestException("Server name not specified.").asPromise();
        }

        try {
            String serverUrl = getServerUrl(getSsoToken(serverContext), serverId);
            ServiceConfig serverConfigs = getServerConfigs(serverContext);
            if (!serverConfigs.getSubConfigNames().contains(serverUrl)) {
                return new NotFoundException("Unknown server ID: " + serverId).asPromise();
            }
        } catch (SSOException | SMSException e) {
            logger.error("Error getting server config", e);
        } catch (NotFoundException e) {
            logger.warning("Error getting server schema", e);
            return e.asPromise();
        }

        JsonValue schema = this.schema.getSchema();
        if (GENERAL_TAB_NAME.equals(tabName) && !isServerDefault) {
            schema = schema.copy();
            addSiteOptions(serverContext, schema, request);
        }

        return newResultPromise(newActionResponse(schema));
    }

    private void addSiteOptions(Context serverContext, JsonValue schema, ActionRequest request) {
        try {
            ResourceBundle titleBundle = request.getPreferredLocales()
                    .getBundleInPreferredLocale("amConsole", getClass().getClassLoader());

            List<String> sites = new ArrayList<>(getSites(getSsoToken(serverContext)));
            List<String> siteTitles = new ArrayList<>(sites);
            sites.add(0, EMPTY_SELECTION);
            siteTitles.add(0, titleBundle.getString(EMPTY_SELECTION));
            JsonValue parentSite = schema.get("properties").get(SERVER_PARENT_SITE).get("properties")
                    .get(PARENT_SITE_PROPERTY);
            parentSite.put("enum", sites);
            parentSite.get("options").put("enum_titles", siteTitles);
        } catch (SSOException | SMSException e) {
            logger.error("Error getting available sites", e);
        }
    }

    private Properties getAttributes(ServiceConfig serverConfig) throws InternalServerErrorException {
        Set<String> rawValues = serverConfig.getAttributes().get(SERVER_CONFIG);

        StringBuilder stringBuilder = new StringBuilder();
        for (String value : rawValues) {
            stringBuilder.append(value);
            stringBuilder.append("\n");
        }

        Properties properties = new Properties();
        try {
            properties.load(new StringReader(stringBuilder.toString()));
        } catch (IOException e) {
            throw new InternalServerErrorException("Unable to load server attributes", e);
        }
        return properties;
    }

    private List<SMSLabel> getLabels(String sectionName, Document propertySheet,
            Map<String, List<String>> options, Map<String, List<LocalizableString>> optionLabels,
            Set<String> passwordFields)
            throws XPathExpressionException {

        String expression = "/propertysheet/section[@defaultValue='" + sectionName + "']"
                + "/property/label/@*[name()='defaultValue' or name()='labelFor']";
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList labels = (NodeList) xPath.compile(expression).evaluate(propertySheet, XPathConstants.NODESET);
        List<SMSLabel> allLabels = new ArrayList<>();
        for (int i = 0; i < labels.getLength() - 1; i = i + 2) {
            String defaultValue = labels.item(i).getNodeValue();
            String labelFor = labels.item(i + 1).getNodeValue();
            LocalizableString displayValue = consoleString(defaultValue);
            String defaultHelpValue = defaultValue.replaceFirst("amconfig\\.", "amconfig.help.");
            LocalizableString description = consoleStringWithDefault(defaultHelpValue, "");

            final String attributeName = getAttributeNameFromCcName(labelFor);
            final String type = getType(attributeName);
            final List<String> attributeOptions = getAttributeOptions(options, attributeName, type);
            final List<LocalizableString> attributeOptionLabels = getAttributeOptionsLabels(optionLabels, attributeName,
                    type);
            final boolean isPasswordField = passwordFields.contains(attributeName);

            allLabels.add(new SMSLabel(defaultValue, labelFor, displayValue,
                    description, type, attributeOptions, attributeOptionLabels, isPasswordField));
        }

        return allLabels;
    }

    private List<String> getAttributeNames(String tabName) throws NotFoundException, InternalServerErrorException {
        Document propertySheet = getPropertySheet();
        return getValues("/propertysheet/section/property/cc/@name", propertySheet);
    }

    List<String> getAttributeNamesForSection(String sectionName, Document propertySheet)
            throws InternalServerErrorException {
        return getValues("/propertysheet/section[@defaultValue='" + sectionName + "']/property/cc/@name", propertySheet);
    }

    private List<String> getValues(String expression, Document propertySheet) throws InternalServerErrorException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        List<String> defaultValueNames = new ArrayList<>();
        NodeList defaultValues;
        try {
            defaultValues = (NodeList) xPath.compile(expression).evaluate(propertySheet, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new InternalServerErrorException("Failed to compile xpath: " + expression, e);
        }
        for (int i = 0; i < defaultValues.getLength(); i++) {
            String nodeValue = defaultValues.item(i).getNodeValue().replace('-', '.');
            if (nodeValue.substring(0, 3).equals("csc")) {
                nodeValue = nodeValue.substring(3, nodeValue.length());
            }
            defaultValueNames.add(nodeValue);
        }
        return defaultValueNames;
    }

    private List<String> getSectionNames(Document propertySheet) throws InternalServerErrorException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "//propertysheet/section/@defaultValue";
        List<String> sectionNames = new ArrayList<>();
        NodeList defaultValues;
        try {
            defaultValues = (NodeList) xPath.compile(expression).evaluate(propertySheet, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new InternalServerErrorException("Failed to compile xpath: " + expression, e);
        }
        for (int i = 0; i < defaultValues.getLength(); i++) {
            final String nodeValue = defaultValues.item(i).getNodeValue();
            sectionNames.add(nodeValue);
        }

        return sectionNames;
    }

    private Map<String, List<LocalizableString>> getOptionLabels(Document propertySheet) {
        Map<String, List<String>> options = getOptions(propertySheet, "@label");

        Map<String, List<LocalizableString>> allOptionLabels = new HashMap<>();
        for (String attributeName : options.keySet()) {
            List<LocalizableString> optionLabels = new ArrayList<>();
            for (String option : options.get(attributeName)) {
                optionLabels.add(consoleString(option));
            }
            allOptionLabels.put(attributeName, optionLabels);
        }
        return allOptionLabels;
    }

    private Map<String, List<String>> getOptions(Document propertySheet) {
        return getOptions(propertySheet, "@value");
    }

    private Map<String, List<String>> getOptions(Document propertySheet, String expressionAttribute) {
        Map<String, List<String>> radioOptions = new HashMap<>();
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();

            List<String> attributeNamesForTab = getAttributeNames(tabName);
            for (String attributeName : attributeNamesForTab) {
                String convertedName = getCcNameFromAttributeName(attributeName);
                String expression = "//propertysheet/section/property/cc[@name='" + convertedName + "']/option/" +
                        expressionAttribute;
                NodeList optionsList = (NodeList) xPath.compile(expression).evaluate(propertySheet, XPathConstants.NODESET);
                List<String> options = new ArrayList<>();
                for (int i = 0; i < optionsList.getLength(); i++) {
                    options.add(optionsList.item(i).getNodeValue());
                }

                if (!options.isEmpty()) {
                    radioOptions.put(attributeName, options);
                }
            }
        } catch (XPathExpressionException | NotFoundException | InternalServerErrorException e) {
            logger.error("Error reading property sheet", e);
        }
        return radioOptions;
    }

    private Set<String> getPasswordFields(Document propertySheet) {
        Set<String> passwordFields = new HashSet<>();
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "//propertysheet/section/property/cc";
            NodeList ccList = (NodeList) xPath.compile(expression).evaluate(propertySheet, XPathConstants.NODESET);

            for (int i = 0; i < ccList.getLength(); i++) {
                NamedNodeMap attributes = ccList.item(i).getAttributes();
                String ccName = attributes.getNamedItem("name").getNodeValue();
                String tagClass = attributes.getNamedItem("tagclass").getNodeValue();
                if ("com.sun.web.ui.taglib.html.CCPasswordTag".equals(tagClass)) {
                    passwordFields.add(getAttributeNameFromCcName(ccName));
                }
            }
        } catch (XPathExpressionException e) {
            logger.error("Error reading property sheet", e);
        }
        return passwordFields;
    }

    private Document getPropertySheet() throws NotFoundException, InternalServerErrorException {
        InputStream resourceStream = getInputStream(tabName);

        if (resourceStream == null) {
            //try with an uppercase first letter
            resourceStream = getInputStream(tabName.substring(0, 1).toUpperCase() + tabName.substring(1));
        }

        if (resourceStream == null) {
            //try all in caps
            resourceStream = getInputStream(tabName.toUpperCase());
        }

        if (resourceStream == null) {
            throw new NotFoundException("Unrecognised server properties section " + tabName);
        }

        try {
            DocumentBuilder dBuilder = XMLUtils.getSafeDocumentBuilder(false);
            return dBuilder.parse(resourceStream);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new InternalServerErrorException("Unable to parse propertySheet for " + tabName, e);
        }
    }

    private InputStream getInputStream(String propertySheetName) {

        final String propertyFileName = "propertyServerEdit" + propertySheetName + ".xml";
        return this.getClass().getClassLoader().getResourceAsStream
                ("/com/sun/identity/console/" + propertyFileName);
    }

    protected ServiceConfig getServerConfigs(Context serverContext) throws SMSException, SSOException {
        SSOToken ssoToken = serverContext.asContext(SSOTokenContext.class).getCallerSSOToken();
        return getServerConfigs(ssoToken);
    }

    protected ServiceConfig getServerConfigs(SSOToken ssoToken) throws SMSException, SSOException {
        ServiceConfigManager scm = new ServiceConfigManager(ssoToken, "iPlanetAMPlatformService", "1.0");
        ServiceConfig config = scm.getGlobalConfig(null);
        return config.getSubConfig(SCHEMA_NAME);
    }

    @Read(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> read(Context serverContext) {
        Map<String, String> uriVariables = getUriTemplateVariables(serverContext);

        final String serverId = getServerName(uriVariables);
        if (!isServerDefault && serverId == null) {
            return new BadRequestException("Server name not specified.").asPromise();
        }

        try {
            SSOToken token = getSsoToken(serverContext);
            String serverUrl = getServerUrl(token, serverId);
            ServiceConfig serverConfigs = getServerConfigs(serverContext);
            ServiceConfig defaultConfig = serverConfigs.getSubConfig(SERVER_DEFAULT_NAME);
            ServiceConfig serverConfig = isServerDefault ? defaultConfig : serverConfigs.getSubConfig(serverUrl);

            if (serverConfig == null) {
                return new BadRequestException("Unknown Server " + serverId).asPromise();
            }

            JsonValue result = json(object());

            if (isServerDefault) {
                addDefaultAttributes(result, defaultConfig);
            } else if (tabName.equalsIgnoreCase(DIRECTORY_CONFIGURATION_TAB_NAME)) {
                addDirectoryConfiguration(result, token, serverUrl);
            } else {
                addServerAttributes(result, defaultConfig, serverConfig, token, serverUrl);
            }

            return newResultPromise(newResourceResponse(serverId + "/properties/" + tabName, valueOf(result
                    .hashCode()), result));

        } catch (InternalServerErrorException e) {
            logger.error("Error reading properties for server " + serverId + " and tab " + tabName, e);
            return e.asPromise();
        } catch (NotFoundException e) {
            logger.warning("Error reading properties for server " + serverId + " and tab " + tabName, e);
            return e.asPromise();
        } catch (SMSException | SSOException e) {
            logger.error("Error reading properties for server " + serverId + " and tab " + tabName, e);
        }

        return new BadRequestException("Error reading properties file for " + tabName).asPromise();
    }

    private List<String> getAttributeNamesForTab(ServiceConfig serverConfig)
            throws NotFoundException, InternalServerErrorException {
        return tabName.equalsIgnoreCase(ADVANCED_TAB_NAME) ?
                getAdvancedTabAttributeNames(serverConfig) : getAttributeNames(tabName);
    }

    private void addDirectoryConfiguration(JsonValue result, SSOToken token, String serverUrl) throws SMSException {
        ServerConfigXML serverConfig = getServerConfig(token, serverUrl);
        String path = "directoryConfiguration/";
        result.addPermissive(new JsonPointer(path + "minConnectionPool"), serverConfig.getSMSServerGroup().minPool);
        result.addPermissive(new JsonPointer(path + "maxConnectionPool"), serverConfig.getSMSServerGroup().maxPool);

        List<ServerConfigXML.DirUserObject> bindInfo = serverConfig.getSMSServerGroup().dsUsers;
        if (CollectionUtils.isNotEmpty(bindInfo)) {
            result.addPermissive(new JsonPointer(path + "bindDn"), bindInfo.get(0).dn);
            result.addPermissive(new JsonPointer(path + "bindPassword"), CONFIG_VAR_DEFAULT_SHARED_KEY);
        }

        List<Map<String, String>> servers = new ArrayList<>();
        List<ServerConfigXML.ServerObject> serverHosts = serverConfig.getSMSServerGroup().hosts;
        if (CollectionUtils.isNotEmpty(serverHosts)) {
            for (ServerConfigXML.ServerObject hostInfo : serverHosts) {
                Map<String, String> server = new HashMap<>();
                server.put("serverName", hostInfo.name);
                server.put("hostName", hostInfo.host);
                server.put("portNumber", hostInfo.port);
                server.put("connectionType", hostInfo.type);
                servers.add(server);
            }
        }
        result.addPermissive(new JsonPointer("directoryServers"), servers);
    }

    private void addDefaultAttributes(JsonValue result, ServiceConfig defaultConfig)
            throws InternalServerErrorException, NotFoundException {

        Properties defaultAttributes = getAttributes(defaultConfig);
        List<String> attributeNamesForTab = getAttributeNamesForTab(defaultConfig);
        Map<String, String> attributeNamesToSections = getAttributeNamesToSections();

        for (String attributeName : attributeNamesForTab) {
            String sectionName = attributeNamesToSections.get(attributeName);
            String attributePath = (sectionName == null ? "" : sectionName + "/") + attributeName;
            Object defaultAttribute = getValue(defaultAttributes, attributeName);

            if (defaultAttribute != null) {
                result.putPermissive(new JsonPointer(attributePath), defaultAttribute);
            }
        }
    }

    private void addServerAttributes(JsonValue result, ServiceConfig defaultConfig, ServiceConfig serverConfig,
            SSOToken token, String serverUrl) throws InternalServerErrorException, NotFoundException {

        Properties defaultAttributes = getAttributes(defaultConfig);
        Properties serverAttributes = getAttributes(serverConfig);
        List<String> attributeNamesForTab = getAttributeNamesForTab(serverConfig);
        Map<String, String> attributeNamesToSections = getAttributeNamesToSections();

        for (String attributeName : attributeNamesForTab) {
            Object serverAttribute = getValue(serverAttributes, attributeName);

            if (ADVANCED_TAB_NAME.equals(tabName)) {
                result.putPermissive(new JsonPointer(attributeName), serverAttribute);
            } else if (isParentSiteAttribute(attributeName)) {
                try {
                    String site = getServerSite(token, serverUrl);
                    result.putPermissive(new JsonPointer(SERVER_PARENT_SITE + "/" + attributeName),
                            isEmpty(site) ? EMPTY_SELECTION : site);
                } catch (SMSException | SSOException e) {
                    throw new InternalServerErrorException("Unable to read site for server " + serverUrl, e);
                }
            } else {
                Object defaultAttribute = getValue(defaultAttributes, attributeName);
                String sectionName = attributeNamesToSections.get(attributeName);
                String attributePath = (sectionName == null ? "" : sectionName + "/") + attributeName;
                String valuePath = attributePath + "/value";
                String inheritedPath = attributePath + "/inherited";

                if (serverAttribute != null) {
                    result.putPermissive(new JsonPointer(valuePath), serverAttribute);
                    result.putPermissive(new JsonPointer(inheritedPath), false);
                } else {
                    result.putPermissive(new JsonPointer(valuePath), defaultAttribute);
                    result.putPermissive(new JsonPointer(inheritedPath), true);
                }
            }
        }
    }

    private Object getValue(Properties attributes, String attributeName) {
        final String type = getType(attributeName);
        final String value = (String) attributes.get(attributeName);
        if (isBlank(value)) {
            return value;
        }
        if ("number".equals(type)) {
            return parseInt(value);
        }
        if ("boolean".equals(type)) {
            return parseBoolean(value);
        }
        return value;
    }

    private String getType(String attributeName) {
        final String syntax = syntaxProperties.getProperty(attributeName);
        final String syntaxProperty = syntax == null ? "" : syntax;
        return syntaxRawToReal.get(syntaxProperty);
    }

    private Map<String, String> getAttributeNamesToSections() throws NotFoundException,
            InternalServerErrorException {

        Map<String, String> attributeNameToSectionName = new HashMap<>();
        Document propertySheet = getPropertySheet();
        List<String> sectionNames = getSectionNames(propertySheet);
        for (String sectionName : sectionNames) {
            for (String attributeName : getAttributeNamesForSection(sectionName, propertySheet)) {
                attributeNameToSectionName.put(attributeName, sectionName);
            }
        }

        return attributeNameToSectionName;
    }

    private List<String> getAdvancedTabAttributeNames(ServiceConfig serverConfig) {
        List<String> attributeNamesForTab;
        Set<String> allAttributeNames = (Set<String>) serverConfig.getAttributes().get(SERVER_CONFIG);

        attributeNamesForTab = new ArrayList<>();
        for (String attributeRawValue : allAttributeNames) {
            String attributeName = attributeRawValue.split("=")[0];
            if (!allAttributeNamesInNamedTabs.contains(attributeName)) {
                attributeNamesForTab.add(attributeName);
            }
        }

        attributeNamesForTab.remove(Constants.AM_SERVER_PROTOCOL);
        attributeNamesForTab.remove(Constants.AM_SERVER_HOST);
        attributeNamesForTab.remove(Constants.AM_SERVER_PORT);
        attributeNamesForTab.remove(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        attributeNamesForTab.remove(Constants.SERVER_MODE);
        attributeNamesForTab.remove(SetupConstants.AMC_OVERRIDE_PROPERTY);
        return attributeNamesForTab;
    }

    private Map<String, String> getUriTemplateVariables(Context serverContext) {
        Map<String, String> uriVariable = new HashMap<>();
        for (Context context = serverContext; context != null; context = context.getParent()) {
            if (context instanceof UriRouterContext) {
                uriVariable.putAll(((UriRouterContext) context).getUriTemplateVariables());
            }
        }
        return uriVariable;
    }

    private String getServerName(Map<String, String> uriVariables) {
        return uriVariables.get("serverName");
    }

    @Update(operationDescription = @Operation)
    public Promise<ResourceResponse, ResourceException> update(Context serverContext, UpdateRequest request) {
        Map<String, String> uriVariables = getUriTemplateVariables(serverContext);

        final String serverId = getServerName(uriVariables);
        if (!isServerDefault && serverId == null) {
            return new BadRequestException("Server name not specified.").asPromise();
        }

        try {
            SSOToken token = getSsoToken(serverContext);
            String serverUrl = getServerUrl(token, serverId);
            boolean isAdvancedTab = ADVANCED_TAB_NAME.equalsIgnoreCase(tabName);

            if (isServerDefault) {
                updateServerDefaults(request.toJsonValue().get("content"), token, isAdvancedTab);
            } else if (DIRECTORY_CONFIGURATION_TAB_NAME.equals(tabName)) {
                updateDirectoryConfiguration(request.toJsonValue().get("content"), token, serverUrl);
            } else {
                updateServerInstance(request.toJsonValue().get("content"), token, serverUrl);
            }

            return read(serverContext);
        } catch (SSOException | SMSException e) {
            logger.error("Error updating properties for server " + serverId + " and tab " + tabName, e);
        } catch (InternalServerErrorException e) {
            logger.error("Error updating properties for server " + serverId + " and tab " + tabName, e);
            return e.asPromise();
        } catch (NotFoundException e) {
            logger.warning("Error updating properties for server " + serverId + " and tab " + tabName, e);
            return e.asPromise();
        } catch (ConfigurationException e) {
            logger.error("Invalid property", e);
        } catch (UnknownPropertyNameException e) {
            logger.warning("Unknown property found.", e);
            if (tabName.equals(ADVANCED_TAB_NAME)) {
                return read(serverContext);
            }
            ResourceBundle bundle = request.getPreferredLocales().getBundleInPreferredLocale("amConsole",
                    getClass().getClassLoader());
            return new BadRequestException(
                    format(bundle.getString(UNKNOWN_PROPS), e.getMessage())).asPromise();
        }

        return new BadRequestException("Error updating values for " + tabName).asPromise();
    }

    private void updateServerDefaults(JsonValue content, SSOToken token, boolean advancedConfig) throws SMSException,
            InternalServerErrorException, SSOException, UnknownPropertyNameException, ConfigurationException {

        Map<String, String> attributeValues = new HashMap<>();
        if (advancedConfig) {
            addAttributeValues(content, attributeValues);
            removeUnusedAdvancedAttributes(token, attributeValues.keySet(), SERVER_DEFAULT_NAME);
        } else {
            for (String sectionName : content.keys()) {
                addAttributeValues(content.get(sectionName), attributeValues);
            }
        }

        try {
            setServerInstance(token, SERVER_DEFAULT_NAME, attributeValues);
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to update server properties", e);
        }
    }

    private void updateDirectoryConfiguration(JsonValue content, SSOToken token, String serverUrl) throws SMSException,
            ConfigurationException, SSOException {

        ServerConfigXML serverConfig = getServerConfig(token, serverUrl);
        ServerConfigXML.ServerGroup serverGroup = serverConfig.getSMSServerGroup();

        if (content.isDefined("directoryConfiguration")) {
            JsonValue config = content.get("directoryConfiguration");
            serverGroup.minPool = config.get("minConnectionPool").asInteger();
            serverGroup.maxPool = config.get("maxConnectionPool").asInteger();

            List<ServerConfigXML.DirUserObject> bindInfo = serverGroup.dsUsers;
            if (CollectionUtils.isNotEmpty(bindInfo)) {
                bindInfo.get(0).dn = config.get("bindDn").asString();
                String bindPassword = config.get("bindPassword").asString();
                if (!CONFIG_VAR_DEFAULT_SHARED_KEY.equals(bindPassword)) {
                    bindInfo.get(0).password = Crypt.encode(bindPassword);
                }
            }
        }

        List<ServerObject> servers = new ArrayList<>();
        if (content.isDefined("directoryServers")) {
            for (JsonValue server : content.get("directoryServers")) {
                ServerConfigXML.ServerObject serverObject = new ServerConfigXML.ServerObject();
                serverObject.name = server.get("serverName").asString();
                serverObject.host = server.get("hostName").asString();
                serverObject.port = server.get("portNumber").asString();
                serverObject.type = server.get("connectionType").asString();
                servers.add(serverObject);
            }
        }
        serverGroup.hosts = servers;
        setServerConfigXML(token, serverUrl, serverConfig.toXML());
    }

    private void updateServerInstance(JsonValue content, SSOToken token, String serverUrl)
            throws SMSException, SSOException, UnknownPropertyNameException, ConfigurationException,
            InternalServerErrorException {

        Map<String, String> attributeValues = new HashMap<>();
        Set<String> inheritedAttributeNames = new HashSet<>();

        if (ADVANCED_TAB_NAME.equals(tabName)) {
            addAttributeValues(content, attributeValues);
            removeUnusedAdvancedAttributes(token, attributeValues.keySet(), serverUrl);
        } else {
            for (String sectionName : content.keys()) {
                addAttributesAndInheritanceValues(content.get(sectionName), attributeValues, inheritedAttributeNames);
            }
        }
        try {
            if (!inheritedAttributeNames.isEmpty()) {
                removeServerConfiguration(token, serverUrl, inheritedAttributeNames);
            }
            setServerInstance(token, serverUrl, attributeValues);
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to update server properties", e);
        }

        if (GENERAL_TAB_NAME.equals(tabName) && content.isDefined(SERVER_PARENT_SITE)
                && content.get(SERVER_PARENT_SITE).isDefined(PARENT_SITE_PROPERTY)) {

            String site = content.get(SERVER_PARENT_SITE).get(PARENT_SITE_PROPERTY).asString();
            setServerSite(token, serverUrl, EMPTY_SELECTION.equals(site) ? "" : site);
        }
    }

    private void addAttributeValues(JsonValue attributes, Map<String, String> attributeValues) {
        for (String attributeName : attributes.keys()) {
            attributeValues.put(attributeName, valueOf(attributes.get(attributeName).getObject()));
        }
    }

    private void addAttributesAndInheritanceValues(JsonValue attributes, Map<String, String> attributeValues,
            Set<String> inheritedAttributeNames) {

        for (String attributeName : attributes.keys()) {
            if (PARENT_SITE_PROPERTY.equals(attributeName)) {
                continue;
            }
            JsonValue attribute = attributes.get(attributeName);
            if (attribute.get("inherited").asBoolean()) {
                inheritedAttributeNames.add(attributeName);
            } else {
                attributeValues.put(attributeName, valueOf(attribute.get("value").getObject()));
            }
        }
    }

    private void removeUnusedAdvancedAttributes(SSOToken token, Set<String> newAttributeNames, String serverName)
            throws SSOException, SMSException, InternalServerErrorException {

        ServiceConfig serviceConfig = getServerConfigs(token).getSubConfig(serverName);
        List<String> attributesToRemove = getAdvancedTabAttributeNames(serviceConfig);
        attributesToRemove.removeAll(newAttributeNames);
        try {
            removeServerConfiguration(token, serverName, attributesToRemove);
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to remove server configuration", e);
        }
    }

    private String getServerUrl(SSOToken token, String serverId) throws NotFoundException, SSOException, SMSException {
        if (isServerDefault) {
            return SERVER_DEFAULT_NAME;
        }
        Set<String> serverUrls = getServers(token);
        for (String serverUrl : serverUrls) {
            String id = getServerID(token, serverUrl);
            if (serverId.equals(id)) {
                return serverUrl;
            }
        }
        throw new NotFoundException("Cannot find server with ID: " + serverId);
    }

    private SSOToken getSsoToken(Context context) throws SSOException {
        return context.asContext(SSOTokenContext.class).getCallerSSOToken();
    }

    private ServerConfigXML getServerConfig(SSOToken token, String serverUrl) throws SMSException {
        try {
            return new ServerConfigXML(getServerConfigXML(token, serverUrl));
        } catch (Exception e) {
            throw new SMSException(e.getMessage());
        }
    }

    private boolean isParentSiteAttribute(String attributeName) {
        return GENERAL_TAB_NAME.equals(tabName) && PARENT_SITE_PROPERTY.equals(attributeName);
    }

    @Override
    public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
        return descriptor;
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        return descriptor;
    }

    @Override
    public void addDescriptorListener(Listener listener) {
        // no-op
    }

    @Override
    public void removeDescriptorListener(Listener listener) {
        // no-op
    }

    private class SMSLabel {
        private final String defaultValue;
        private final String labelFor;
        private final LocalizableString displayValue;
        private final LocalizableString description;
        private final String type;
        private final List<LocalizableString> optionLabels;
        private final List<String> options;
        private final boolean isPasswordField;

        public SMSLabel(String defaultValue, String labelFor, LocalizableString displayValue, LocalizableString description, String type,
                List<String> options, List<LocalizableString> optionLabels, boolean isPasswordField) {
            this.defaultValue = defaultValue;
            this.labelFor = labelFor;
            this.displayValue = displayValue;
            this.description = description;
            this.type = type;
            this.options = options;
            this.optionLabels = optionLabels;
            this.isPasswordField = isPasswordField;
        }

        public List<String> getOptions() {
            return options;
        }

        public List<LocalizableString> getOptionLabels() {
            return optionLabels;
        }

        public String getType() {
            return type;
        }

        public LocalizableString getDisplayValue() {
            return displayValue;
        }

        public LocalizableString getDescription() {
            return description;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getLabelFor() {
            return labelFor;
        }

        public boolean isPasswordField() {
            return isPasswordField;
        }
    }
}
