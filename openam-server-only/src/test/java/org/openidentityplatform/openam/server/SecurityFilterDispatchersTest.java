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
 * Copyright 2026 3A Systems, LLC.
 */
package org.openidentityplatform.openam.server;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Pins the {@code <dispatcher>} declarations of the security filters in the shipped
 * {@code web.xml}. A {@code <filter-mapping>} that names no {@code <dispatcher>} applies to
 * REQUEST only, so a {@code RequestDispatcher} forward or include, an async dispatch or an
 * error-page dispatch reaches the mapped endpoint with the filter skipped; naming any type
 * replaces that default, so every type has to be listed.
 */
public class SecurityFilterDispatchersTest {

    private static final Set<String> EVERY_DISPATCHER_TYPE = new HashSet<>(
            Arrays.asList("REQUEST", "FORWARD", "INCLUDE", "ASYNC", "ERROR"));

    private Document webXml;

    @BeforeClass
    public void parseWebXml() throws Exception {
        File file = new File(System.getProperty("basedir", System.getProperty("user.dir")),
                "src/main/webapp/WEB-INF/web.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        webXml = factory.newDocumentBuilder().parse(file);
    }

    /**
     * Filters that authenticate or authorize the caller of a narrowly mapped endpoint.
     */
    @DataProvider
    public Object[][] securityFilters() {
        return new Object[][] {
            {"AuthNFilter"},
            {"AuthZFilter"},
            {"JAXRPCRequestFilter"},
            {"NotificationsWebSocketFilter"},
        };
    }

    @Test(dataProvider = "securityFilters")
    public void securityFilterRunsOnEveryDispatchType(String filterName) {
        List<Element> mappings = filterMappings(filterName);
        assertFalse(mappings.isEmpty(), filterName + " must have a <filter-mapping>");
        for (Element mapping : mappings) {
            assertEquals(childTexts(mapping, "dispatcher"), EVERY_DISPATCHER_TYPE,
                    filterName + " on " + childTexts(mapping, "url-pattern") + ": every <dispatcher> "
                    + "type must be declared, or a forward, include, async or error dispatch reaches "
                    + "the endpoint with the filter skipped");
        }
    }

    private List<Element> filterMappings(String filterName) {
        List<Element> result = new ArrayList<>();
        NodeList mappings = webXml.getElementsByTagNameNS("*", "filter-mapping");
        for (int i = 0; i < mappings.getLength(); i++) {
            Element mapping = (Element) mappings.item(i);
            if (childTexts(mapping, "filter-name").contains(filterName)) {
                result.add(mapping);
            }
        }
        return result;
    }

    private static Set<String> childTexts(Element parent, String childName) {
        Set<String> texts = new HashSet<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && childName.equals(child.getLocalName())) {
                texts.add(child.getTextContent().trim());
            }
        }
        return texts;
    }
}
