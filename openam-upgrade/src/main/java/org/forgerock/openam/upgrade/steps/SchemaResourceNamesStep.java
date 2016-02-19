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

package org.forgerock.openam.upgrade.steps;

import static com.sun.identity.sm.SMSUtils.*;
import static org.forgerock.openam.upgrade.UpgradeServices.*;
import static org.forgerock.openam.utils.Time.*;

import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.forgerock.guava.common.base.Joiner;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.VersionUtils;
import org.forgerock.util.Function;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * Applies the resourceName attributes from all schema files in one go, rather than letting them be
 * updated one attribute at a time (which would trigger a mass of notifications in the SMS).
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class SchemaResourceNamesStep extends AbstractUpgradeStep {

    private static final String PROGRESS = "upgrade.resourcenames.progress";
    private static final int AM_13 = 1300;

    private Map<String, Function<Document, Boolean, XPathExpressionException>> serviceModifications;
    private XPath xpath = XPathFactory.newInstance().newXPath();

    @Inject
    public SchemaResourceNamesStep(PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public void initialize() throws UpgradeException {
        if (VersionUtils.isCurrentVersionLessThan(AM_13, true)) {
            Map<String, Document> serviceXmlContent = UpgradeServiceUtils.getServiceDefinitions(getAdminToken());
            serviceModifications = new HashMap<>();
            for (Map.Entry<String, Document> service : serviceXmlContent.entrySet()) {
                try {
                    DEBUG.message("Finding resource names in {}", service.getKey());
                    ServiceModifier modifier = new ServiceModifier();
                    NodeList nodes = (NodeList) xpath.evaluate("//*[@" + RESOURCE_NAME + "]",
                            service.getValue().getDocumentElement(), XPathConstants.NODESET);
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element element = (Element) nodes.item(i);
                        ElementModifier target = buildPath(element, modifier);
                        target.resourceNameModifier = new ResourceNameModifier(element.getAttribute(RESOURCE_NAME));
                    }
                    if (!modifier.modifiers.isEmpty()) {
                        serviceModifications.put(service.getKey(), modifier);
                    }
                } catch (XPathExpressionException e) {
                    throw new UpgradeException(e);
                }
            }
        }
    }

    private ElementModifier buildPath(Element target, ServiceModifier modifier) {
        switch (target.getTagName()) {
            case SERVICE:
                return modifier.forService(SERVICE + "[" +
                        "@" + NAME + "='" + target.getAttribute(NAME) + "' and " +
                        "@" + VERSION + "='" + target.getAttribute(VERSION) + "']");
            case SCHEMA_ATTRIBUTE:
                return buildPath((Element) target.getParentNode(), modifier)
                        .forPath(SCHEMA_ATTRIBUTE + "[@" + NAME + "='" + target.getAttribute(NAME) + "']");
            case SUB_SCHEMA:
                return buildPath((Element) target.getParentNode(), modifier)
                        .forPath(SUB_SCHEMA + "[@" + NAME + "='" + target.getAttribute(NAME) + "']");
            default:
                return buildPath((Element) target.getParentNode(), modifier)
                        .forName(target.getTagName());
        }
    }

    @Override
    public boolean isApplicable() {
        return serviceModifications != null;
    }

    @Override
    public void perform() throws UpgradeException {
        for (Map.Entry<String, Function<Document, Boolean, XPathExpressionException>> service :
                serviceModifications.entrySet()) {
            try {
                long startTime = currentTimeMillis();
                UpgradeProgress.reportStart(PROGRESS, service.getKey());
                DEBUG.message("Found resource names for {}. Applying now.", service.getKey());
                new ServiceSchemaManager(service.getKey(), getAdminToken()).modifySchema(service.getValue());
                DEBUG.message("Completed ({}ms)", currentTimeMillis() - startTime);
                UpgradeProgress.reportEnd("upgrade.success");
            } catch (XPathExpressionException | SMSException | SSOException e) {
                throw new UpgradeException(e);
            }
        }
    }

    private class ServiceModifier implements Function<Document, Boolean, XPathExpressionException> {
        private final Map<String, ElementModifier> modifiers = new HashMap<>();

        ElementModifier forService(String path) {
            if (!modifiers.containsKey(path)) {
                ElementModifier modifier = new XpathElementModifier(path);
                modifiers.put(path, modifier);
            }
            return modifiers.get(path);
        }

        @Override
        public Boolean apply(Document document) throws XPathExpressionException {
            boolean result = false;
            Element documentElement = document.getDocumentElement();
            for (ElementModifier modifier : modifiers.values()) {
                Boolean modificationResult = modifier.apply(documentElement);
                result = result || modificationResult;
            }
            return result;
        }
    }

    private abstract class ElementModifier implements Function<Element, Boolean, XPathExpressionException> {
        private final Map<String, ElementModifier> modifiers = new HashMap<>();
        private ResourceNameModifier resourceNameModifier;

        ElementModifier forName(String name) {
            if (!modifiers.containsKey(name)) {
                modifiers.put(name, new NamedElementModifier(name));
            }
            return modifiers.get(name);
        }

        ElementModifier forPath(String path) {
            if (!modifiers.containsKey(path)) {
                modifiers.put(path, new XpathElementModifier(path));
            }
            return modifiers.get(path);
        }

        @Override
        public Boolean apply(Element parent) throws XPathExpressionException {
            boolean result = false;
            Element element = getElement(parent);
            for (ElementModifier modifier : modifiers.values()) {
                Boolean modificationResult = modifier.apply(element);
                result = result || modificationResult;
            }
            if (resourceNameModifier != null) {
                Boolean modificationResult = resourceNameModifier.apply(element);
                result = result || modificationResult;
            }
            return result;
        }

        protected abstract Element getElement(Element parent) throws XPathExpressionException;
    }

    private class XpathElementModifier extends ElementModifier {
        private final String path;

        XpathElementModifier(String path) {
            this.path = path;
        }
        @Override
        protected Element getElement(Element parent) throws XPathExpressionException {
            return (Element) xpath.evaluate(path, parent, XPathConstants.NODE);
        }
    }

    private class NamedElementModifier extends ElementModifier {
        private final String name;

        NamedElementModifier(String name) {
            this.name = name;
        }
        @Override
        protected Element getElement(Element parent) throws XPathExpressionException {
            return (Element) parent.getElementsByTagName(name).item(0);
        }
    }

    private class ResourceNameModifier implements Function<Element, Boolean, XPathExpressionException> {

        private final String resourceName;

        private ResourceNameModifier(String resourceName) {
            this.resourceName = resourceName;
        }

        @Override
        public Boolean apply(Element element) throws XPathExpressionException {
            if (!resourceName.equals(element.getAttribute(RESOURCE_NAME))) {
                element.setAttribute(RESOURCE_NAME, resourceName);
                return true;
            }
            return false;
        }
    }


    @Override
    public String getShortReport(String delimiter) {
        return MessageFormat.format(BUNDLE.getString("upgrade.resourcenames.short"), serviceModifications.size())
                + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);
        TreeSet<String> services = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        services.addAll(serviceModifications.keySet());
        tags.put("%SERVICES%", Joiner.on(delimiter).join(services));
        return tagSwapReport(tags, "upgrade.resourcenames.long");
    }
}
