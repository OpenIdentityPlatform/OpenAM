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
 * Copyright 2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.docs.services;

import org.apache.commons.text.TextStringBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Generator {

    final HtmlConverter htmlConverter;

    final DocumentBuilder builder;

    final XPath xpath;

    final Locale BUNDLE_LOCALE = Locale.forLanguageTag("en");

    final String AUTH_CLASS_NAME_REGEX = "^(iPlanetAMAuth|sunAMAuth)(.*?)Service$";

    public Generator() throws Exception {
        htmlConverter = new HtmlConverter();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

        xpath = XPathFactory.newInstance().newXPath();
    }

    public static void main(String[] args) throws Exception {
        String serverPath = args[0];
        String targetPath = args[1];

        Generator generator = new Generator();

        generator.generateServicesDoc(serverPath, targetPath);

    }

    private void generateServicesDoc(String serverPath, String targetPath) throws Exception {

        try(WarClassLoader cl = new WarClassLoader(serverPath)) {

            Map<String, Document> xmlServicesMap = fetchServicesMapFromWar(cl);

            Path dirPath = Paths.get(targetPath);
            Files.createDirectories(dirPath);

            generateAuthModulesDoc(xmlServicesMap, cl, dirPath);

            generateDataStoreDoc(xmlServicesMap, cl, dirPath);
        }

    }

    private Map<String, Document> fetchServicesMapFromWar(WarClassLoader cl) throws Exception {
        Properties serviceNamesProps = cl.loadProperties("serviceNames.properties");
        String serviceNamesStr = serviceNamesProps.getProperty("serviceNames");
        String[] serviceNames = serviceNamesStr.split("\\s+");
        List<Exception> errors = new ArrayList<>();
        List<Document> xmlServices = Arrays.stream(serviceNames).map(String::trim)
                .map(s -> {
                    try(InputStream is = cl.getResourceAsStream(s)) {
                        if (is == null) {
                            return null;
                        }
                        return builder.parse(is);
                    } catch (Exception e) {
                        errors.add(e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!errors.isEmpty()) {
            String errMessage = "Errors occurred while parsing service files:" + errors;
            System.out.println(errMessage);
            throw new Exception(errMessage);
        }

        return xmlServices.stream().collect(Collectors.toMap(xmlService -> {
            Element service = (Element) xmlService.getElementsByTagName("Service").item(0);
            return service.getAttribute("name");
        }, xmlService -> xmlService, (existing, replacement) -> existing, LinkedHashMap::new));
    }


    private void generateAuthModulesDoc(Map<String, Document> xmlServicesMap, WarClassLoader cl, Path targetPath) throws Exception {

        Document iPlanetAMAuthService = xmlServicesMap.get("iPlanetAMAuthService");

        Map<String, String> authClassMap = getAuthClassMap(iPlanetAMAuthService);

        TextStringBuilder asciidoc = new TextStringBuilder();
        asciidoc.appendln(":table-caption!:").appendNewLine();
        asciidoc.appendln("[#chap-auth-modules]");
        asciidoc.append("== ").appendln("Authentication Modules Reference").appendNewLine();

        for(Map.Entry<String, Document> entry : xmlServicesMap.entrySet()) {
            Document xmlService = entry.getValue();
            NodeList schema = xmlService.getElementsByTagName("Schema");
            Element schemaElement = (Element) schema.item(0);
            if(!isAuthService(xmlService, authClassMap)) {
                continue;
            }

            String bundleName = schemaElement.getAttribute("i18nFileName");
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName, BUNDLE_LOCALE, cl);
            generateModuleDoc(schemaElement, bundle, asciidoc, authClassMap);
        }
        Path filePath = targetPath.resolve("chap-auth-modules.adoc");

        Files.write(filePath, asciidoc.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("File written to: " + filePath.toAbsolutePath());
    }

    private Map<String, String> getAuthClassMap(Document iPlanetAMAuthService) throws XPathExpressionException {
        Map<String, String> authClassMap = new HashMap<>();

        final String authModuleClassesXpath = "/ServicesConfiguration/Service/Schema/Global/AttributeSchema[1]/DefaultValues/Value";

        NodeList authClassNames = (NodeList) xpath.evaluate(authModuleClassesXpath, iPlanetAMAuthService, XPathConstants.NODESET);

        for (int i = 0; i < authClassNames.getLength(); i++) {
            String authClassName = authClassNames.item(i).getTextContent();
            String[] tokenized = authClassName.split("\\.");
            String classShortName = tokenized[tokenized.length - 1].toLowerCase();
            authClassMap.put(classShortName, authClassName);
        }
        return authClassMap;
    }

    private boolean isAuthService(Document xmlService, Map<String, String> authClassMap) {


        NodeList schema = xmlService.getElementsByTagName("Schema");
        Element schemaElement = (Element) schema.item(0);
        String serviceHierarchy = schemaElement.getAttribute("serviceHierarchy");
        if(!serviceHierarchy.startsWith("/DSAMEConfig/authentication/")) {
            return false;
        }
        Element service = (Element) xmlService.getElementsByTagName("Service").item(0);
        String serviceName = service.getAttribute("name");

        if (!serviceName.matches(AUTH_CLASS_NAME_REGEX)) {
            System.out.println(serviceName + " is not auth service");
            return false;
        }

        String authServiceClassFullName = getAuthClassName(serviceName, authClassMap);
        if(authServiceClassFullName == null) {
            System.out.println(serviceName + " is not auth module");
            return false;
        }
        return true;
    }

    private String getAuthClassName(String serviceName, Map<String, String> authClassMap) {

        String authServiceClassName = serviceName.replaceAll(AUTH_CLASS_NAME_REGEX, "$2").toLowerCase();
        return authClassMap.get(authServiceClassName);
    }

    private void generateModuleDoc(Element schemaElement, ResourceBundle bundle, TextStringBuilder asciidoc, Map<String, String> authClassMap) {

        String moduleNameKey = schemaElement.getAttribute("i18nKey");
        String moduleName = bundle.getString(moduleNameKey);
        asciidoc.append(String.format("[#%s-module-ref]", moduleName.toLowerCase().replace(" ", "-"))).appendNewLine();
        asciidoc.append("=== ").append(moduleName)
                .appendNewLine().appendNewLine();

        String serviceName = ((Element) schemaElement.getParentNode()).getAttribute("name");

        String className = getAuthClassName(serviceName, authClassMap);
        String classLink = String.format("link:../apidocs/index.html?%s.html[%s, window=\\_blank]",
                className.replaceAll("\\.", "/"), className);
        asciidoc.appendln(String.format("Java class: `%s`", classLink))
                .appendNewLine();

        asciidoc.appendln(String.format("`ssoadm` service name: `%s`", ((Element) schemaElement.getParentNode()).getAttribute("name")));
        asciidoc.appendNewLine();

        Element orgElement = (Element) schemaElement.getElementsByTagName("Organization").item(0);
        NodeList attributes = orgElement.getElementsByTagName("AttributeSchema");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attrElement = (Element) attributes.item(i);
            printAttributeElement(bundle, asciidoc, attrElement);
        }
        System.out.printf("generated doc for %s module%n", moduleName);
    }

    private void generateDataStoreDoc(Map<String, Document> xmlServicesMap, WarClassLoader cl, Path targetPath) throws Exception {
        TextStringBuilder asciidoc = new TextStringBuilder();
        asciidoc.appendln(":table-caption!:").appendNewLine();
        asciidoc.appendln("[#chap-user-data-stores]");
        asciidoc.append("== ").appendln("User Data Stores Reference").appendNewLine();

        Document xmlService = xmlServicesMap.get("sunIdentityRepositoryService");
        NodeList schema = xmlService.getElementsByTagName("Schema");
        Element schemaElement = (Element) schema.item(0);
        String bundleName = schemaElement.getAttribute("i18nFileName");

        ResourceBundle bundle = ResourceBundle.getBundle(bundleName, BUNDLE_LOCALE, cl);

        String expression = "/ServicesConfiguration/Service/Schema/Organization/SubSchema";
        NodeList dataStoreList = (NodeList) xpath.evaluate(expression, xmlService, XPathConstants.NODESET);

        for (int i = 0; i < dataStoreList.getLength(); i++) {
            Element dataStore = (Element)dataStoreList.item(i);

            String i18nKey = dataStore.getAttribute("i18nKey");
            String dataStoreName;
            if(i18nKey.trim().isEmpty()) {
                dataStoreName = dataStore.getAttribute("name");
            } else if(bundle.containsKey(i18nKey)) {
                dataStoreName = bundle.getString(i18nKey);
            } else {
                dataStoreName = i18nKey;
            }
            asciidoc.appendln(String.format("[#%s-datastore-ref]", dataStoreName.toLowerCase().replace(" ", "-")));
            asciidoc.append("=== ").append(dataStoreName)
                    .appendNewLine().appendNewLine();

            NodeList attributes = dataStore.getElementsByTagName("AttributeSchema");
            for (int j = 0; j < attributes.getLength(); j++) {
                Element attrElement = (Element) attributes.item(j);
                printAttributeElement(bundle, asciidoc, attrElement);
            }
        }

        Path filePath = targetPath.resolve("chap-user-data-stores.adoc");

        Files.write(filePath, asciidoc.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("File written to: " + filePath.toAbsolutePath());
    }



    private void printAttributeElement(ResourceBundle bundle, TextStringBuilder asciidoc, Element attrElement) {
        String type = attrElement.getAttribute("type");
        if (type.equals("validator")) {
            return;
        }
        String i18Key = attrElement.getAttribute("i18nKey");
        if ("".equals(i18Key.trim())) {
            return;
        }
        String attrName = i18Key;
        if(bundle.containsKey(i18Key)) {
             attrName = bundle.getString(i18Key);
        }
        asciidoc.append(attrName).append("::").appendNewLine()
                .append("+").appendNewLine().appendln("--");
        if (bundle.containsKey(i18Key.concat(".help"))) {
            asciidoc.appendNewLine();
            String attrHelp = bundle.getString(i18Key.concat(".help"));
            htmlConverter.convertToAsciidoc(attrHelp, asciidoc);
            asciidoc.appendNewLine();
        }
        if (bundle.containsKey(i18Key.concat(".help.txt"))) {

            String attrHelpTxt = bundle.getString(i18Key.concat(".help.txt"));
            asciidoc.appendNewLine();
            htmlConverter.convertToAsciidoc(attrHelpTxt, asciidoc);
            asciidoc.appendNewLine();
        }
        asciidoc.appendNewLine();
        asciidoc.appendln(String.format("`ssoadm` attribute: `%s`", attrElement.getAttribute("name")));
        asciidoc.appendNewLine();
        asciidoc.appendln("--");
        asciidoc.appendNewLine();

    }
}