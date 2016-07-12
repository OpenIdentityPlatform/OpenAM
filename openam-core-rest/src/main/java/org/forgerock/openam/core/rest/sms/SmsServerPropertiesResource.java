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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openam.utils.IOUtils.readStream;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.openam.utils.StringUtils.isEmpty;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.annotations.Action;
import org.forgerock.json.resource.annotations.Read;
import org.forgerock.json.resource.annotations.RequestHandler;
import org.forgerock.json.resource.annotations.Update;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.BundleUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
@RequestHandler
public class SmsServerPropertiesResource {

    private static final String SCHEMA_NAME = "com-sun-identity-servers";
    private static final String AM_CONSOLE_CONFIG_XML = "amConsoleConfig.xml";
    private static final String SERVER_CONFIG = "serverconfig";
    private static final String DIRECTORY_CONFIG_SCHEMA = "/schema/json/server-directory-configuration.json";
    private static final String DIRECTORY_CONFIGURATION_TAB_NAME = "directoryconfiguration";
    private static final String ADVANCED_TAB_NAME = "advanced";
    private static final String GENERAL_TAB_NAME = "general";
    private static final String PARENT_SITE_PROPERTY = "singleChoiceSite";
    private static final String SERVER_DEFAULT_NAME = "server-default";
    private static final String SERVER_PARENT_SITE = "amconfig.header.site";
    private static final Map<String, String> syntaxRawToReal = new HashMap<>();
    //this list is to enable us to distinguish which attributes are in the "advanced" tab
    private static final List<String> allAttributeNamesInNamedTabs = new ArrayList<>();
    private static final String UNKNOWN_PROPS = "serverconfig.updated.with.invalid.properties";
    private static final String EMPTY_SELECTION = "[Empty]";

    private final Debug logger;
    private final Properties syntaxProperties;

    static {
        syntaxRawToReal.put("true,false", "boolean");
        syntaxRawToReal.put("false,true", "boolean");
        syntaxRawToReal.put("integer", "number");
        syntaxRawToReal.put("on,off", "on,off");
        syntaxRawToReal.put("off,on", "on,off");
        syntaxRawToReal.put("", "string");
    }

    @Inject
    public SmsServerPropertiesResource(@Named("ServerAttributeSyntax") Properties syntaxProperties,
            @Named("frRest") Debug logger) {
        this.logger = logger;
        this.syntaxProperties = syntaxProperties;
    }

    private JsonValue getDirectorySchema(ResourceBundle titleBundle, Debug logger) {
        try {
            String schema = readStream(getClass().getResourceAsStream(DIRECTORY_CONFIG_SCHEMA));
            JsonValue directoryConfigSchema = JsonValueBuilder.toJsonValue(schema);
            replacePropertyRecursive(directoryConfigSchema, titleBundle, "title");
            return directoryConfigSchema;
        } catch (IOException e) {
            logger.error("Error creating document builder", e);
        }
        return null;
    }

    private void replacePropertyRecursive(JsonValue jsonValue, ResourceBundle bundle, String property) {
        if (jsonValue.isDefined(property) && jsonValue.get(property).isString()) {
            String propValue = jsonValue.get(property).asString();
            try {
                jsonValue.put(property, bundle.getString(propValue));
            } catch (MissingResourceException e) {
                // Ignore - retain original value
            }
        }

        for (JsonValue child : jsonValue) {
            if (child.isMap()) {
                replacePropertyRecursive(child, bundle, property);
            }
        }
    }

    private JsonValue getSchema(ResourceBundle titleBundle, boolean isDefault, String tabName) {
        JsonValue schema = json(object(field("type", "object")));
        if (!getTabNames().contains(tabName)) {
            return null;
        }
        try {
            Document propertySheet = getPropertySheet(tabName);
            Map<String, List<String>> options = getOptions(propertySheet, tabName);
            Map<String, List<String>> optionLabels = getOptionLabels(propertySheet, tabName, titleBundle);
            List<String> sectionNames = getSectionNames(propertySheet);
            Set<String> optionalAttributes = getOptionalAttributes(propertySheet, tabName);
            Set<String> passwordFields = getPasswordFields(propertySheet);

            int sectionOrder = 0;
            for (String sectionName : sectionNames) {
                if (isDefault && SERVER_PARENT_SITE.equals(sectionName)) {
                    continue;
                }
                final String sectionPath = "/properties/" + sectionName;

                String title = BundleUtils.getStringWithDefault(titleBundle, sectionName, sectionName);
                schema.putPermissive(new JsonPointer(sectionPath + "/title"), title);
                schema.putPermissive(new JsonPointer(sectionPath + "/type"), "object");
                schema.putPermissive(new JsonPointer(sectionPath + "/propertyOrder"), sectionOrder++);

                int attributeOrder = 0;

                for (SMSLabel label : getLabels(sectionName, propertySheet, titleBundle, options, optionLabels,
                        passwordFields)) {
                    String attributeName = getAttributeNameFromCcName(label.getLabelFor());
                    if (isDefault || isParentSiteAttribute(tabName, attributeName)) {
                        addDefaultSchema(schema, sectionPath, label, attributeOrder, optionalAttributes, tabName);
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
            Set<String> optionalAttributes, String tabName) {

        final String attributeName = getAttributeNameFromCcName(label.getLabelFor());
        if (Constants.AM_SERVICES_SECRET.equals(attributeName)) {
            return;
        }
        final String title = label.getDisplayValue();
        final String type = label.getType();
        final String description = label.getDescription();
        final List<String> attributeOptions = label.getOptions();
        final List<String> attributeOptionLabels = label.getOptionLabels();
        final boolean isParentSiteAttribute = isParentSiteAttribute(tabName, attributeName);
        final boolean isOptional = optionalAttributes.contains(attributeName) || isParentSiteAttribute;

        final String path = sectionPath + "/properties/" + attributeName;
        if (CollectionUtils.isNotEmpty(attributeOptions) || isParentSiteAttribute) {
            template.putPermissive(new JsonPointer(path + "/enum"), attributeOptions);
            template.putPermissive(new JsonPointer(path + "/options/enum_titles"), attributeOptionLabels);
        } else {
            template.putPermissive(new JsonPointer(path + "/type"), type);
        }

        template.putPermissive(new JsonPointer(path + "/title"), title);
        template.putPermissive(new JsonPointer(path + "/propertyOrder"), attributeOrder);
        template.putPermissive(new JsonPointer(path + "/required"), !isOptional);

        if (isNotBlank(description)) {
            template.putPermissive(new JsonPointer(path + "/description"), description);
        }

        if (label.isPasswordField()) {
            template.putPermissive(new JsonPointer(path + "/format"), "password");
        }

        allAttributeNamesInNamedTabs.add(attributeName);
    }

    private void addServerSchema(JsonValue template, String sectionPath, SMSLabel label, int attributeOrder) {
        final String title = label.getDisplayValue();
        final String type = label.getType();
        final String description = label.getDescription();
        final String attributeName = getAttributeNameFromCcName(label.getLabelFor());
        final List<String> attributeOptions = label.getOptions();
        final List<String> attributeOptionLabels = label.getOptionLabels();
        final String propertyPath = sectionPath + "/properties/" + attributeName;
        final String valuePath = propertyPath + "/properties/value";
        final String inheritedPath = propertyPath + "/properties/inherited";

        template.putPermissive(new JsonPointer(propertyPath + "/title"), title);
        template.putPermissive(new JsonPointer(propertyPath + "/type"), "object");
        template.putPermissive(new JsonPointer(propertyPath + "/propertyOrder"), attributeOrder);
        if (isNotBlank(description)) {
            template.putPermissive(new JsonPointer(propertyPath + "/description"), description);
        }

        if (CollectionUtils.isNotEmpty(attributeOptions)) {
            template.putPermissive(new JsonPointer(valuePath + "/enum"), attributeOptions);
            template.putPermissive(new JsonPointer(valuePath + "/options/enum_titles"), attributeOptionLabels);
        } else {
            template.putPermissive(new JsonPointer(valuePath + "/type"), type);
        }
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

    private List<String> getAttributeOptionsLabels(Map<String, List<String>> options, String attributeName, String
            syntax) {
        List<String> attributeOptions;
        if (syntax != null && syntax.equals("on,off")) {
            final List<String> onOffOptions = new ArrayList<>();
            onOffOptions.add("On");
            onOffOptions.add("Off");
            attributeOptions = onOffOptions;
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

    private Set<String> getTabNames() {
        Set<String> tabNames = new HashSet<>();
        try {
            String result = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(AM_CONSOLE_CONFIG_XML));
            Matcher matcher = Pattern.compile(".*ServerEdit(.*)ViewBean.*").matcher(result);
            while (matcher.find()) {
                tabNames.add(matcher.group(1).toLowerCase());
            }
        } catch (IOException e) {
            logger.error("Error getting tab names", e);
        }
        return tabNames;
    }

    @Action
    public Promise<ActionResponse, ResourceException> schema(ActionRequest request, Context serverContext) {
        Map<String, String> uriVariables = getUriTemplateVariables(serverContext);

        final String serverId = uriVariables.get("serverName");
        if (serverId == null) {
            return new BadRequestException("Server name not specified.").asPromise();
        }

        String serverUrl = "";
        try {
            serverUrl = getServerUrl(getSsoToken(serverContext), serverId);
            ServiceConfig serverConfigs = getServerConfigs(serverContext);
            if (!serverConfigs.getSubConfigNames().contains(serverUrl)) {
                return new BadRequestException("Unknown server ID: " + serverId).asPromise();
            }
        } catch (SSOException | SMSException e) {
            logger.error("Error getting server config", e);
        } catch (NotFoundException e) {
            logger.warning("Error getting server schema", e);
            return e.asPromise();
        }

        final String tabName = getTabName(uriVariables);
        if (tabName == null) {
            return new BadRequestException("Tab name not specified.").asPromise();
        }

        JsonValue schema;
        boolean isServerDefault = serverUrl.equalsIgnoreCase(SERVER_DEFAULT_NAME);

        ResourceBundle titleBundle = request.getPreferredLocales()
                .getBundleInPreferredLocale("amConsole", getClass().getClassLoader());

        if (ADVANCED_TAB_NAME.equals(tabName)) {
            schema = getAdvancedSchema(serverContext, serverUrl);
        } else if (DIRECTORY_CONFIGURATION_TAB_NAME.equalsIgnoreCase(tabName)) {
            schema = getDirectorySchema(titleBundle, logger);
        } else if (isServerDefault) {
            schema = getSchema(titleBundle, true, tabName);
        } else {
            schema = getSchema(titleBundle, false, tabName);
            addSiteOptions(tabName, serverContext, schema, titleBundle);
        }

        if (schema == null) {
            return new BadRequestException("Unknown tab: " + tabName).asPromise();
        }

        return newResultPromise(newActionResponse(schema));
    }

    private void addSiteOptions(String tabName, Context serverContext, JsonValue schema, ResourceBundle titleBundle) {
        if (GENERAL_TAB_NAME.equals(tabName)) {
            try {
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
    }

    private Properties getAttributes(ServiceConfig serverConfig) throws InternalServerErrorException {
        Set<String> rawValues = (Set<String>) serverConfig.getAttributes().get(SERVER_CONFIG);

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

    private List<SMSLabel> getLabels(String sectionName, Document propertySheet, ResourceBundle titleBundle,
            Map<String, List<String>> options, Map<String, List<String>> optionLabels, Set<String> passwordFields)
            throws XPathExpressionException {

        String expression = "/propertysheet/section[@defaultValue='" + sectionName + "']/property/label/@*[name()='defaultValue' or name()='labelFor']";
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList labels = (NodeList) xPath.compile(expression).evaluate(propertySheet, XPathConstants.NODESET);
        List<SMSLabel> allLabels = new ArrayList<>();
        for (int i = 0; i < labels.getLength() - 1; i = i + 2) {
            String defaultValue = labels.item(i).getNodeValue();
            String labelFor = labels.item(i + 1).getNodeValue();
            String displayValue = BundleUtils.getStringWithDefault(titleBundle, defaultValue, defaultValue);
            String defaultHelpValue = defaultValue.replaceFirst("amconfig\\.", "amconfig.help.");
            String description = BundleUtils.getStringWithDefault(titleBundle, defaultHelpValue, "");

            final String attributeName = getAttributeNameFromCcName(labelFor);
            final String type = getType(attributeName);
            final List<String> attributeOptions = getAttributeOptions(options, attributeName, type);
            final List<String> attributeOptionLabels = getAttributeOptionsLabels(optionLabels, attributeName, type);
            final boolean isPasswordField = passwordFields.contains(attributeName);

            allLabels.add(new SMSLabel(defaultValue, labelFor, displayValue,
                    description, type, attributeOptions, attributeOptionLabels, isPasswordField));
        }

        return allLabels;
    }

    private JsonValue getAdvancedSchema(Context serverContext, String serverName) {
        JsonValue template = json(object());
        try {
            ServiceConfig serverConfigs = getServerConfigs(serverContext);
            final ServiceConfig serverConfig = serverConfigs.getSubConfig(serverName);
            List<String> advancedAttributeNames = getAdvancedTabAttributeNames(serverConfig);

            List<SMSLabel> labels = new ArrayList<>();
            for (String attributeName : advancedAttributeNames) {
                labels.add(new SMSLabel(null, attributeName, attributeName, null, "string", null, null, false));
            }

            template.putPermissive(new JsonPointer("/type"), "object");
            int attributeOrder = 0;

            for (SMSLabel label : labels) {
                final String title = label.getDisplayValue();
                final String type = label.getType();
                final String attributeName = label.getDisplayValue();
                final List<String> attributeOptions = label.getOptions();
                final List<String> attributeOptionLabels = label.getOptionLabels();

                final String path = "/properties/" + attributeName;
                if (attributeOptions != null && !attributeOptions.isEmpty()) {
                    template.putPermissive(new JsonPointer(path + "/enum"), attributeOptions);
                    template.putPermissive(new JsonPointer(path + "/options/enum_titles"), attributeOptionLabels);
                } else {
                    template.putPermissive(new JsonPointer(path + "/type"), type);
                }

                template.putPermissive(new JsonPointer(path + "/title"), title);
                template.putPermissive(new JsonPointer(path + "/propertyOrder"), attributeOrder++);
                template.putPermissive(new JsonPointer(path + "/required"), false);
            }
        } catch (SSOException | SMSException e) {
            logger.error("Error getting advanced tab schema", e);
        }

        return template;
    }

    private List<String> getAttributeNames(String tabName) throws NotFoundException, InternalServerErrorException {
        Document propertySheet = getPropertySheet(tabName);
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

    private Map<String, List<String>> getOptionLabels(Document propertySheet, String tabName, ResourceBundle bundle) {
        Map<String, List<String>> options = getOptions(propertySheet, tabName, "@label");

        Map<String, List<String>> allOptionLabels = new HashMap<>();
        for (String attributeName : options.keySet()) {
            List<String> optionLabels = new ArrayList<>();
            for (String option : options.get(attributeName)) {
                optionLabels.add(bundle.getString(option));
            }
            allOptionLabels.put(attributeName, optionLabels);
        }
        return allOptionLabels;
    }

    private Map<String, List<String>> getOptions(Document propertySheet, String tabName) {
        return getOptions(propertySheet, tabName, "@value");
    }

    private Map<String, List<String>> getOptions(Document propertySheet, String tabName, String expressionAttribute) {
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

    private Document getPropertySheet(String tabName) throws NotFoundException, InternalServerErrorException {
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

    private InputStream getInputStream(String tabName) {

        final String propertyFileName = "propertyServerEdit" + tabName + ".xml";
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

    @Read
    public Promise<ResourceResponse, ResourceException> read(Context serverContext) {
        Map<String, String> uriVariables = getUriTemplateVariables(serverContext);

        final String tabName = getTabName(uriVariables);
        if (tabName == null) {
            return new BadRequestException("Tab name not specified.").asPromise();
        }

        final String serverId = getServerName(uriVariables);
        if (serverId == null) {
            return new BadRequestException("Server name not specified.").asPromise();
        }

        try {
            SSOToken token = getSsoToken(serverContext);
            String serverUrl = getServerUrl(token, serverId);
            ServiceConfig serverConfigs = getServerConfigs(serverContext);
            boolean isServerDefault = serverUrl.equalsIgnoreCase(SERVER_DEFAULT_NAME);
            ServiceConfig defaultConfig = serverConfigs.getSubConfig(SERVER_DEFAULT_NAME);
            ServiceConfig serverConfig = isServerDefault ? defaultConfig : serverConfigs.getSubConfig(serverUrl);

            if (serverConfig == null) {
                return new BadRequestException("Unknown Server " + serverId).asPromise();
            }

            JsonValue result = json(object());

            if (isServerDefault) {
                addDefaultAttributes(result, defaultConfig, tabName);
            } else if (tabName.equalsIgnoreCase(DIRECTORY_CONFIGURATION_TAB_NAME)) {
                addDirectoryConfiguration(result, token, serverUrl);
            } else {
                addServerAttributes(result, defaultConfig, serverConfig, tabName, token, serverUrl);
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

    private List<String> getAttributeNamesForTab(ServiceConfig serverConfig, String tabName)
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

    private void addDefaultAttributes(JsonValue result, ServiceConfig defaultConfig, String tabName)
            throws InternalServerErrorException, NotFoundException {

        Properties defaultAttributes = getAttributes(defaultConfig);
        List<String> attributeNamesForTab = getAttributeNamesForTab(defaultConfig, tabName);
        Map<String, String> attributeNamesToSections = getAttributeNamesToSections(tabName);

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
            String tabName, SSOToken token, String serverUrl) throws InternalServerErrorException, NotFoundException {

        Properties defaultAttributes = getAttributes(defaultConfig);
        Properties serverAttributes = getAttributes(serverConfig);
        List<String> attributeNamesForTab = getAttributeNamesForTab(serverConfig, tabName);
        Map<String, String> attributeNamesToSections = getAttributeNamesToSections(tabName);

        for (String attributeName : attributeNamesForTab) {
            Object serverAttribute = getValue(serverAttributes, attributeName);

            if (ADVANCED_TAB_NAME.equals(tabName)) {
                result.putPermissive(new JsonPointer(attributeName), serverAttribute);
            } else if (isParentSiteAttribute(tabName, attributeName)) {
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

    private Map<String, String> getAttributeNamesToSections(String tabName) throws NotFoundException,
            InternalServerErrorException {

        Map<String, String> attributeNameToSectionName = new HashMap<>();
        Document propertySheet = getPropertySheet(tabName);
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
        return serverContext.asContext(UriRouterContext.class).getUriTemplateVariables();
    }

    private String getTabName(Map<String, String> uriVariables) {
        String tabName = uriVariables.get("tab");
        return tabName == null ? null : tabName.toLowerCase();
    }

    private String getServerName(Map<String, String> uriVariables) {
        return uriVariables.get("serverName");
    }

    @Update
    public Promise<ResourceResponse, ResourceException> update(Context serverContext, UpdateRequest request) {
        Map<String, String> uriVariables = getUriTemplateVariables(serverContext);

        final String tabName = getTabName(uriVariables);
        if (tabName == null) {
            return new BadRequestException("Tab name not specified.").asPromise();
        }

        final String serverId = getServerName(uriVariables);
        if (serverId == null) {
            return new BadRequestException("Server name not specified.").asPromise();
        }

        try {
            SSOToken token = getSsoToken(serverContext);
            String serverUrl = getServerUrl(token, serverId);
            boolean isServerDefault = SERVER_DEFAULT_NAME.equalsIgnoreCase(serverUrl);
            boolean isAdvancedTab = ADVANCED_TAB_NAME.equalsIgnoreCase(tabName);

            if (isServerDefault) {
                updateServerDefaults(request.toJsonValue().get("content"), token, isAdvancedTab);
            } else if (DIRECTORY_CONFIGURATION_TAB_NAME.equals(tabName)) {
                updateDirectoryConfiguration(request.toJsonValue().get("content"), token, serverUrl);
            } else {
                updateServerInstance(request.toJsonValue().get("content"), token, serverUrl, tabName);
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

    private void updateServerInstance(JsonValue content, SSOToken token, String serverUrl, String tabName)
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
        if (SERVER_DEFAULT_NAME.equals(serverId)) {
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

    private boolean isParentSiteAttribute(String tabName, String attributeName) {
        return GENERAL_TAB_NAME.equals(tabName) && PARENT_SITE_PROPERTY.equals(attributeName);
    }

    private class SMSLabel {
        private final String defaultValue;
        private final String labelFor;
        private final String displayValue;
        private final String description;
        private final String type;
        private final List<String> optionLabels;
        private final List<String> options;
        private final boolean isPasswordField;

        public SMSLabel(String defaultValue, String labelFor, String displayValue, String description, String type,
                List<String> options, List<String> optionLabels, boolean isPasswordField) {
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

        public List<String> getOptionLabels() {
            return optionLabels;
        }

        public String getType() {
            return type;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public String getDescription() {
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
