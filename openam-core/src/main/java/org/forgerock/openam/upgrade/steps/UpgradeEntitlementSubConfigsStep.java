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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.upgrade.steps;

import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.sm.SMSUtils;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.forgerock.openam.utils.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static com.sun.identity.shared.xml.XMLUtils.getNodeAttributeValue;
import static com.sun.identity.shared.xml.XMLUtils.parseAttributeValuePairTags;
import org.forgerock.openam.guice.InjectorHolder;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;

/**
 * Upgrade step is responsible for updating existing application types with newly added actions.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeEntitlementSubConfigsStep extends AbstractUpgradeStep {

    private static final String ENTITLEMENTS_XML = "entitlement.xml";

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String APPLICATION_TYPE = "applicationType";

    private static final String AUDIT_REPORT = "upgrade.entitlementapps";
    private static final String AUDIT_MODIFIED_TYPE = "upgrade.entitlement.modified.type";
    private static final String AUDIT_MODIFIED_TYPE_START = "upgrade.entitlement.modified.type.start";
    private static final String AUDIT_UPGRADE_SUCCESS = "upgrade.success";
    private static final String AUDIT_UPGRADE_FAIL = "upgrade.failed";

    private final EntitlementConfiguration entitlementService;
    private final Map<String, Map<String, Boolean>> missingActions;

    @Inject
    public UpgradeEntitlementSubConfigsStep() {
        this.entitlementService = InjectorHolder.getInstance(EntitlementConfiguration.class);
        missingActions = new HashMap<String, Map<String, Boolean>>();
    }

    @Override
    public void initialize() throws UpgradeException {
        DEBUG.message("Initialising the upgrade entitlement sub-config step");

        final Document entitlementDoc = getEntitlementXML();
        final NodeList subConfigs = entitlementDoc.getElementsByTagName(SMSUtils.SUB_CONFIG);

        for (int idx = 0; idx < subConfigs.getLength(); idx++) {

            final Node subConfig = subConfigs.item(idx);
            final String id = getNodeAttributeValue(subConfig, ID);

            if (APPLICATION_TYPE.equals(id)) {
                captureMissingActions(subConfig);
            }
        }
    }

    /**
     * Compares the provided subconfig element's action list against what is currently present in the existing
     * application type definition and captures the missing entries.
     *
     * @param subConfig The new application type's XML representation.
     */
    private void captureMissingActions(final Node subConfig) {
        final String name = getNodeAttributeValue(subConfig, NAME);
        ApplicationType applicationType = getType(name);
        if (applicationType != null) {
            Map<String, Boolean> existingActions = applicationType.getActions();
            Map<String, Boolean> newActions = EntitlementUtils.getActions(parseAttributeValuePairTags(subConfig));
            if (!existingActions.equals(newActions)) {
                newActions.keySet().removeAll(existingActions.keySet());
                missingActions.put(name, newActions);
            }
        }
    }

    @Override
    public boolean isApplicable() {
        return !missingActions.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        if (!missingActions.isEmpty()) {
            addMissingActions();
        }
    }

    /**
     * Adds the missing actions to their corresponding application type's.
     *
     * @throws UpgradeException If there was an error while updating the application type.
     */
    private void addMissingActions() throws UpgradeException {
        for (final Map.Entry<String, Map<String, Boolean>> entry : missingActions.entrySet()) {
            final String name = entry.getKey();
            final Map<String, Boolean> actions = entry.getValue();

            try {
                UpgradeProgress.reportStart(AUDIT_MODIFIED_TYPE_START, name);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Modifying application type " + name + " ; adding actions: " + actions);
                }
                final ApplicationType type = getType(name);
                type.getActions().putAll(actions);
                entitlementService.storeApplicationType(type);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
            } catch (EntitlementException ee) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                throw new UpgradeException(ee);
            }
        }
    }

    /**
     * Retrieves the application type for the passed application type name.
     *
     * @param name
     *         the application type name
     *
     * @return an instance of ApplicationType associated with the name else null if the name is not present
     */
    private ApplicationType getType(String name) {
        for (final ApplicationType type : entitlementService.getApplicationTypes()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }

        return null;
    }

    @Override
    public String getShortReport(final String delimiter) {
        final StringBuilder builder = new StringBuilder();

        if (!missingActions.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_MODIFIED_TYPE));
            builder.append(delimiter);
        }

        return builder.toString();
    }

    @Override
    public String getDetailedReport(final String delimiter) {
        final StringBuilder builder = new StringBuilder();
        final Map<String, String> reportEntries = new HashMap<String, String>();

        if (!missingActions.isEmpty()) {
            builder.append(BUNDLE.getString(AUDIT_MODIFIED_TYPE)).append(": ").append(delimiter);
            for (final Map.Entry<String, Map<String, Boolean>> entry : missingActions.entrySet()) {
                builder.append(INDENT).append(entry.getKey()).append(delimiter);
                for (final String action : entry.getValue().keySet()) {
                    builder.append(INDENT).append(INDENT).append(action).append(delimiter);
                }
            }
        }

        reportEntries.put("%ENTITLEMENT_DATA%", builder.toString());
        reportEntries.put(LF, delimiter);

        return UpgradeServices.tagSwapReport(reportEntries, AUDIT_REPORT);
    }

    /**
     * Retrieves the XML document for entitlements.
     *
     * @return a document instance representing entitlements
     *
     * @throws UpgradeException
     *         should an error occur attempting to read the entitlement xml
     */
    protected Document getEntitlementXML() throws UpgradeException {
        InputStream serviceStream = null;
        final Document doc;

        try {
            DEBUG.message("Reading entitlements configuration file: " + ENTITLEMENTS_XML);
            serviceStream = getClass().getClassLoader().getResourceAsStream(ENTITLEMENTS_XML);
            doc = UpgradeUtils.parseServiceFile(serviceStream, getAdminToken());
        } finally {
            IOUtils.closeIfNotNull(serviceStream);
        }

        return doc;
    }
}
