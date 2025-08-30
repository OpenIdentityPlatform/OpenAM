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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.sms;

import com.sun.identity.shared.xml.XMLUtils;
import java.util.Collections;
import org.forgerock.openam.utils.CollectionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jakarta.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Responsible for reading the amConsoleConfig.xml configuration file and
 * parsing relevant sections to expose to the service-exposing layers in the
 * SMS REST system.
 */
@Singleton
public class SmsConsoleServiceConfig {

    private static final String CONFIG_FILENAME = "amConsoleConfig.xml";

    private Map<String, Set<String>> services = new HashMap<>();
    private final Set<String> hiddenServices;

    private static final String SERVICES_CONFIG = "servicesconfig";
    private static final String HIDDEN_SERVICES = "hidden";

    /**
     * Generates the singleton SmsConsoleServiceConfig by reading and parsing the
     * appropriate xml config file.
     */
    public SmsConsoleServiceConfig() {
        Document doc = parseDocument(CONFIG_FILENAME);
        configServices(doc);
        hiddenServices = Collections.unmodifiableSet(services.get(HIDDEN_SERVICES));
    }

    /**
     * Returns true if the provided service name is not in the list of
     * hidden services.
     *
     * @param serviceName Service identifier to check.
     * @return True if the service is not declared hidden.
     */
    public boolean isServiceVisible(String serviceName) {
        return !CollectionUtils.isEmpty(hiddenServices) && !hiddenServices.contains(serviceName);
    }

    private Document parseDocument(String fileName) {
        Document document = null;
        InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        try {
            DocumentBuilder documentBuilder = XMLUtils.getSafeDocumentBuilder(false);
            document = documentBuilder.parse(is);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }

        return document;
    }

    private void configServices(Document doc) {
        NodeList nodes = doc.getElementsByTagName(SERVICES_CONFIG);

        if ((nodes != null) && (nodes.getLength() == 1)) {
            Node root = nodes.item(0);
            NodeList children = root.getChildNodes();

            if (children != null) {
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);

                    if (child.getNodeName().equals(HIDDEN_SERVICES)) {
                        Set<String> set = new HashSet<>();
                        String names = getAttribute(child, "names");
                        StringTokenizer st = new StringTokenizer(names, ",");
                        while (st.hasMoreTokens()) {
                            set.add(st.nextToken().trim());
                        }
                        services.put(HIDDEN_SERVICES, set);
                    }
                }
            }
        }
    }

    private String getAttribute(Node node, String attrName) {
        String value = null;
        NamedNodeMap attrs = node.getAttributes();
        Node nodeID = attrs.getNamedItem(attrName);
        if (nodeID != null) {
            value = nodeID.getNodeValue();
            value = value.trim();
        }
        return value;
    }
}
