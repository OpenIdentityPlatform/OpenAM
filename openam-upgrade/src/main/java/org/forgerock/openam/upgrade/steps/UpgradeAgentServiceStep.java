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
 */

package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;

import javax.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;

import com.google.common.collect.ImmutableMap;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Updates the i18nKey attributes within the SubSchema elements of the Agent Service.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeAgentServiceStep extends AbstractUpgradeStep {


    public static final String AGENT_SERVICE = "AgentService";
    private static final String II8NKEY = "i18nKey";
    private static final String NAME = "name";
    private static final String TAG_SUB_SCHEMA = "SubSchema";
    private Map<String, String> subschemaIi8nKeys = new HashMap<>();

    @Inject
    public UpgradeAgentServiceStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public boolean isApplicable() {
        return true;
    }

    @Override
    public void initialize() throws UpgradeException {
        Document document = UpgradeServiceUtils.getServiceDefinitions(getAdminToken()).get(AGENT_SERVICE);
        NodeList subSchemas = document.getElementsByTagName(TAG_SUB_SCHEMA);
        for (int i = 0; i < subSchemas.getLength(); i++) {
            Element subSchema = (Element) subSchemas.item(i);
            subschemaIi8nKeys.put(subSchema.getAttribute(NAME), subSchema.getAttribute(II8NKEY));
        }
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            UpgradeProgress.reportStart("upgrade.agentservice.start");
            ServiceSchemaManager ssm = new ServiceSchemaManager("AgentService", getAdminToken());
            Document document = XMLUtils.toDOMDocument(ssm.getSchema(), DEBUG);
            NodeList subSchemas = document.getElementsByTagName(TAG_SUB_SCHEMA);
            for (int i = 0; i < subSchemas.getLength(); i++) {
                Element subSchema = (Element) subSchemas.item(i);
                String name = subSchema.getAttribute(NAME);
                String subschemaIi18nKey = subschemaIi8nKeys.get(name);
                if (!subSchema.getAttribute(II8NKEY).equals(subschemaIi18nKey)) {
                    subSchema.setAttribute(II8NKEY, subschemaIi18nKey);
                }
            }
            String updatedSchema = XMLUtils.print(document);
            DEBUG.message("Updating Agent Service schema with XML:\n{}", updatedSchema);
            ssm.replaceSchema(new ByteArrayInputStream(updatedSchema.getBytes(StandardCharsets.UTF_8)));
            UpgradeProgress.reportEnd("upgrade.success");
        } catch (SMSException | SSOException | IOException ex) {
            DEBUG.error("Unable to upgrade AgentService", ex);
            UpgradeProgress.reportEnd("upgrade.failed");
            throw new UpgradeException("An error occurred while trying to upgrade AgentService");
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        return BUNDLE.getString("upgrade.agentservice.short") + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        return tagSwapReport(ImmutableMap.of(LF, delimiter), "upgrade.agentservice.detailed");
    }
}
