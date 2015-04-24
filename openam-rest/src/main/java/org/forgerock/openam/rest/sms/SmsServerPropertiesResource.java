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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.*;

/**
 * A service to allow the modification of server properties
 */
public class SmsServerPropertiesResource implements SingletonResourceProvider {

    private static final String SERVER_DEFAULT_NAME = "server-default";
    public static final String SERVER_CONFIG_KEY = "serverconfig";
    public static final String SCHEMA_NAME = "com-sun-identity-servers";
    private static Properties syntax;
    private static Properties titles;
    private static Map<String, String> syntaxRawToReal = new HashMap<String, String>();

    private final Debug logger;

    static {
        syntaxRawToReal.put("true,false", "boolean");
        syntaxRawToReal.put("false,true", "boolean");
        syntaxRawToReal.put("integer", "number");
        syntaxRawToReal.put("on,off", "on,off");
        syntaxRawToReal.put("off,on", "on,off");
        syntaxRawToReal.put("", "string");
    }

    @Inject
    public SmsServerPropertiesResource(@Named("ServerAttributeSyntax") Properties syntaxProperties, @Named
            ("ServerAttributeTitles") Properties titleProperties, @Named("frRest") Debug logger) {
        this.syntax = syntaxProperties;
        this.titles = titleProperties;
        this.logger = logger;
    }

    @Override
    public void actionInstance(ServerContext serverContext, ActionRequest actionRequest, ResultHandler<JsonValue> resultHandler) {
        if (actionRequest.getAction().equals("template")) {
            resultHandler.handleResult(createTemplate(serverContext, resultHandler));
        } else {
            resultHandler.handleError(new NotSupportedException("Action not supported: " + actionRequest.getAction()));
        }
    }

    private JsonValue createTemplate(ServerContext serverContext, ResultHandler<JsonValue> resultHandler) {
        JsonValue result = json(object());
        Map<String, String> uriVariables = getUriTemplateVariables(serverContext);

        final String tabName = getTabName(uriVariables);
        if (tabName == null) {
            resultHandler.handleError(new BadRequestException("Tab name not specified."));
        }

        try {
            Document propertySheet = getPropertySheet(tabName);
            Map<String, Set<String>> options = getOptions(propertySheet);
            Set<String> sectionNames = getSectionNames(propertySheet);

            for (String sectionName : sectionNames) {
                final String sectionPath = "/_schema/properties/" + sectionName;
                result.putPermissive(new JsonPointer(sectionPath + "/title"), titles.getProperty(sectionName));
                Set<String> attributeNamesForSection = getDefaultValueNamesForSection(sectionName, propertySheet);
                for (String attributeName : attributeNamesForSection) {
                    final String path = sectionPath + "/" + attributeName;
                    String typeRealValue = syntaxRawToReal.get(syntax.getProperty(attributeName));

                    if (typeRealValue == null) {
                        if (options.get(attributeName) != null) {
                            result.putPermissive(new JsonPointer(path + "/type/enum"),
                                    options.get(attributeName).toArray());
                        }
                    } else if (typeRealValue.equals("on,off")) {
                        result.putPermissive(new JsonPointer(path + "/type/enum"), new String[]{"on", "off"});
                    } else {
                        result.putPermissive(new JsonPointer(path + "/type"), typeRealValue);
                    }

                    result.putPermissive(new JsonPointer(path + "/title"), titles.get("amconfig." + attributeName));
                }
            }
        } catch (IOException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        } catch (ParserConfigurationException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        } catch (SAXException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        } catch (XPathExpressionException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        }

        return result;
    }


    private Properties getAttributes(ServiceConfig serverConfig) throws IOException, SMSException, SSOException {
        Set<String> rawValues = (Set<String>) serverConfig.getAttributes().get("serverconfig");

        StringBuilder stringBuilder = new StringBuilder();
        for (String value : rawValues) {
            stringBuilder.append(value);
            stringBuilder.append("\n");
        }

        Properties properties = new Properties();
        properties.load(new StringReader(stringBuilder.toString()));
        return properties;
    }

    private HashSet<String> getDefaultValueNames(Document propertySheet) throws ParserConfigurationException, SAXException,
            IOException,
            XPathExpressionException {
        return getValues("/propertysheet/section/property/cc/@name", propertySheet);
    }

    HashSet<String> getDefaultValueNamesForSection(String sectionName, Document propertySheet) throws
            ParserConfigurationException,
            SAXException,
            IOException, XPathExpressionException {
        return getValues("/propertysheet/section[@defaultValue='" + sectionName + "']/property/cc/@name", propertySheet);
    }

    private HashSet<String> getValues(String expression, Document propertySheet) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        HashSet<String> defaultValueNames = new HashSet<String>();
        NodeList defaultValues = (NodeList) xPath.compile(expression).evaluate(propertySheet, XPathConstants.NODESET);
        for (int i = 0; i < defaultValues.getLength(); i++) {
            String nodeValue = defaultValues.item(i).getNodeValue().replace('-', '.');
            if (nodeValue.substring(0, 3).equals("csc")) {
                nodeValue = nodeValue.substring(3, nodeValue.length());
            }
            defaultValueNames.add(nodeValue);
        }
        return defaultValueNames;
    }

    private HashSet<String> getSectionNames(Document propertySheet) throws ParserConfigurationException, SAXException,
            IOException,
            XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "//propertysheet/section/@defaultValue";
        HashSet<String> sectionNames = new HashSet<String>();
        NodeList defaultValues = (NodeList) xPath.compile(expression).evaluate(propertySheet, XPathConstants.NODESET);
        for (int i = 0; i < defaultValues.getLength(); i++) {
            final String nodeValue = defaultValues.item(i).getNodeValue();
            sectionNames.add(nodeValue);
        }

        return sectionNames;
    }

    private Map<String, Set<String>> getOptions(Document propertySheet) {
        Map<String, Set<String>> radioOptions = new HashMap<String, Set<String>>();
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();

            Set<String> attributeNamesForTab = getDefaultValueNames(propertySheet);
            for (String defaultValueName : attributeNamesForTab) {
                String convertedName = "csc".concat(defaultValueName.replace('.', '-'));
                String expression = "//propertysheet/section/property/cc[@name='" + convertedName + "']/option/@value";
                NodeList optionsList = (NodeList) xPath.compile(expression).evaluate(propertySheet, XPathConstants.NODESET);
                HashSet<String> options = new HashSet<String>();
                for (int i = 0; i < optionsList.getLength(); i++) {
                    options.add(optionsList.item(i).getNodeValue());
                }

                if (!options.isEmpty()) {
                    radioOptions.put(defaultValueName, options);
                }
            }
        } catch (ParserConfigurationException e) {
            logger.error("Error reading property sheet", e);
        } catch (SAXException e) {
            logger.error("Error reading property sheet", e);
        } catch (IOException e) {
            logger.error("Error reading property sheet", e);
        } catch (XPathExpressionException e) {
            logger.error("Error reading property sheet", e);
        }
        return radioOptions;
    }

    private Document getPropertySheet(String tabName) throws ParserConfigurationException, SAXException, IOException {
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
            throw new IOException("Unable to locate propertySheet for " + tabName);
        }

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        return dBuilder.parse(resourceStream);
    }

    private InputStream getInputStream(String tabName) {
        final String propertyFileName = "propertyServerEdit" + tabName + ".xml";
        return this.getClass().getClassLoader().getResourceAsStream
                ("com/sun/identity/console/" + propertyFileName);
    }

    protected ServiceConfigManager getServiceConfigManager(ServerContext context) throws SSOException, SMSException {
        SSOToken ssoToken = context.asContext(SSOTokenContext.class).getCallerSSOToken();
        return new ServiceConfigManager(ssoToken, "iPlanetAMPlatformService", "1.0");
    }

    protected ServiceConfig getServerConfigs(ServiceConfigManager scm)
            throws SMSException, SSOException {
        ServiceConfig config = scm.getGlobalConfig(null);
        return config.getSubConfig(SCHEMA_NAME);
    }

    @Override
    public void patchInstance(ServerContext serverContext, PatchRequest patchRequest, ResultHandler<Resource> resultHandler) {
        resultHandler.handleError(new NotSupportedException());
    }

    @Override
    public void readInstance(ServerContext serverContext, ReadRequest readRequest, ResultHandler<Resource> resultHandler) {
        Map<String, String> uriVariables = getUriTemplateVariables(serverContext);

        final String tabName = getTabName(uriVariables);
        if (tabName == null) {
            resultHandler.handleError(new BadRequestException("Tab name not specified."));
        }

        final String serverName = getServerName(uriVariables);
        if (serverName == null) {
            resultHandler.handleError(new BadRequestException("Server name not specified."));
        }

        try {
            Document propertySheet = getPropertySheet(tabName);

            ServiceConfigManager scm = getServiceConfigManager(serverContext);
            ServiceConfig serverConfigs = getServerConfigs(scm);

            Properties defaultAttributes = getAttributes(serverConfigs.getSubConfig(SERVER_DEFAULT_NAME));
            final ServiceConfig subConfig = serverConfigs.getSubConfig(serverName);

            if (subConfig == null) {
                resultHandler.handleError(new BadRequestException("Unknown Server " + serverName));
                return;
            }

            Properties serverSpecificAttributes = getAttributes(subConfig);
            Map<String, String> defaultSection = new HashMap<String, String>();

            JsonValue value = json(object(
                    field("default", defaultSection)));

            Set<String> attributeNamesForTab = getDefaultValueNames(propertySheet);
            for (String attributeName : attributeNamesForTab) {
                final String defaultAttribute = (String) defaultAttributes.get(attributeName);
                if (defaultAttribute != null) {
                    defaultSection.put(attributeName, (String) defaultAttributes.get(attributeName));
                }

                final String serverSpecificAttribute = (String) serverSpecificAttributes.get(attributeName);
                if (serverSpecificAttribute != null) {
                    value.add(attributeName, serverSpecificAttribute);
                }
            }

            resultHandler.handleResult(new Resource(serverName + "/properties/" + tabName, String.valueOf(value
                    .hashCode()), value));
        } catch (SMSException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        } catch (SSOException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        } catch (ParserConfigurationException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        } catch (SAXException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        } catch (IOException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        } catch (XPathExpressionException e) {
            logger.error("Error reading property sheet for tab " + tabName, e);
        }
    }

    private Map<String, String> getUriTemplateVariables(ServerContext serverContext) {
        return serverContext.asContext(RouterContext.class).getUriTemplateVariables();
    }

    private String getTabName(Map<String, String> uriVariables) {
        return uriVariables.get("tab");
    }

    private String getServerName(Map<String, String> uriVariables) {
        return uriVariables.get("serverName");
    }

    @Override
    public void updateInstance(ServerContext serverContext, UpdateRequest updateRequest, ResultHandler<Resource> resultHandler) {
        Map<String, String> uriVariables = getUriTemplateVariables(serverContext);

        final String tabName = getTabName(uriVariables);
        if (tabName == null) {
            resultHandler.handleError(new BadRequestException("Tab name not specified."));
        }

        final String serverName = getServerName(uriVariables);
        if (serverName == null) {
            resultHandler.handleError(new BadRequestException("Server name not specified."));
        }

        try {
            ServiceConfigManager scm = getServiceConfigManager(serverContext);
            ServiceConfig serverConfigs = getServerConfigs(scm);
            final ServiceConfig serverConfig = serverConfigs.getSubConfig(serverName);

            final JsonValue jsonValue = updateRequest.toJsonValue();
            final Map newAttributeValues = (Map) ((Map) jsonValue.getObject()).get("content");
            Set<String> attributesToBeAlteredNames = newAttributeValues.keySet();

            final Map allAttributes = serverConfig.getAttributes();
            Set<String> currentAttributes = (Set) allAttributes.get("serverconfig");
            Set<String> newAttributes = new HashSet<String>();

            for (String attribute : currentAttributes) {
                String attributeName = attribute.split("=")[0];

                if (attributesToBeAlteredNames.contains(attributeName)) {
                    newAttributes.add(attributeName + "=" + newAttributeValues.get(attributeName));
                } else {
                    newAttributes.add(attribute);
                }
            }

            allAttributes.put("serverconfig", newAttributes);
            serverConfig.setAttributes(allAttributes);

            resultHandler.handleResult(new Resource(tabName, String.valueOf(jsonValue.get("/content")),
                    jsonValue));
        } catch (SSOException e) {
            logger.error("Error getting SSOToken", e);
        } catch (SMSException e) {
            logger.error("Error getting service config manager", e);
        }
    }
}
