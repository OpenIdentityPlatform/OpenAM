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

package org.openidentityplatform.openam.docs.authmodules;

import org.apache.commons.text.TextStringBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class Generator {

    HtmlConverter htmlConverter = new HtmlConverter();

    public static void main(String[] args) throws Exception {

        Generator generator = new Generator();
        String authModulesPath = args[0];
        String targetPath = args[1];
        generator.generateAsciiDoc(authModulesPath, targetPath);
    }

    private void generateAsciiDoc(String authModulesPath, String targetPath) throws IOException {
        Path rootDir = Paths.get(authModulesPath); // Start directory
        String pattern = "glob:**/resources/amAuth*.xml"; // Recursive match
        TextStringBuilder asciidoc = new TextStringBuilder();
        asciidoc.append("[#chap-auth-modules]").appendNewLine();
        asciidoc.append("== ").append("Authentication Modules Reference").appendNewLine().appendNewLine();

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
        try (Stream<Path> paths = Files.walk(rootDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(matcher::matches)
                    .forEach(p -> {
                        try {
                            generateDescriptionBySchema(p, asciidoc);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        // Define the target directory and file
        Path dirPath = Paths.get(targetPath);
        Path filePath = dirPath.resolve("chap-auth-modules.adoc");
        Files.createDirectories(dirPath);

        Files.write(filePath, asciidoc.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("File written to: " + filePath.toAbsolutePath());

    }

    public void generateDescriptionBySchema(Path p, TextStringBuilder asciidoc) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        Document doc = builder.parse(p.toFile());

        doc.getDocumentElement().normalize();

        NodeList schema = doc.getElementsByTagName("Schema");
        Element schemaElement = (Element) schema.item(0);
        String bundleName = schemaElement.getAttribute("i18nFileName");
        URL[] urls = {p.getParent().toUri().toURL()};
        try(URLClassLoader loader = new URLClassLoader(urls)) {
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag("en"), loader);
            generateModuleDoc(schemaElement, bundle, asciidoc);
        }
    }

    private void generateModuleDoc(Element schemaElement, ResourceBundle bundle, TextStringBuilder asciidoc) throws Exception {

        String moduleNameKey = schemaElement.getAttribute("i18nKey");
        String moduleName = bundle.getString(moduleNameKey);
        asciidoc.append(String.format("[#%s-module-ref]", moduleName.toLowerCase().replace(" ", "-"))).appendNewLine();
        asciidoc.append("=== ").append(moduleName)
                .appendNewLine().appendNewLine();

        asciidoc.append(String.format("`ssoadm` service name: `%s`", ((Element) schemaElement.getParentNode()).getAttribute("name")));
        asciidoc.appendNewLine().appendNewLine();

        Element orgElement = (Element) schemaElement.getElementsByTagName("Organization").item(0);
        NodeList attributes = orgElement.getElementsByTagName("AttributeSchema");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attrElement = (Element) attributes.item(i);
            String type = attrElement.getAttribute("type");
            if (type.equals("validator")) {
                continue;
            }
            String i18Key = attrElement.getAttribute("i18nKey");
            if ("".equals(i18Key.trim())) {
                continue;
            }
            String attrName = bundle.getString(i18Key);
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
            asciidoc.append(System.lineSeparator());
            asciidoc.append(String.format("`ssoadm` attribute: `%s`", attrElement.getAttribute("name")));
            asciidoc.appendNewLine();
            asciidoc.append("--");
            asciidoc.appendNewLine();
        }
        System.out.printf("generated doc for %s module%n", moduleName);
    }


}