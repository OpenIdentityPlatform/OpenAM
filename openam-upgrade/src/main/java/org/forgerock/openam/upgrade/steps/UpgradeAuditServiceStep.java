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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedAction;

import javax.inject.Inject;

import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * Updates the i18nFileName attributes within SubSchema elements in the AuditService service.
 *
 * TODO: The detection and the actual operation should be moved to UpgradeServiceSchemaStep.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeAuditServiceStep extends AbstractUpgradeStep {

    private static final String OLD_I18N_FILE_NAME = "commonAudit";
    private static final String NEW_I18N_FILE_NAME = "audit";
    public static final String I18N_FILE_NAME = "i18nFileName";
    private boolean isApplicable = false;

    @Inject
    public UpgradeAuditServiceStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public boolean isApplicable() {
        return isApplicable;
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(AuditConstants.SERVICE_NAME, getAdminToken());
            final ServiceSchema globalSchema = ssm.getGlobalSchema();
            final ServiceSchema subSchema = globalSchema.getSubSchema("CSV");
            DEBUG.message("i18nFileName found in CSV subschema was: {}", subSchema.getI18NFileName());
            if (OLD_I18N_FILE_NAME.equals(subSchema.getI18NFileName())) {
                isApplicable = true;
            }
        } catch (ServiceNotFoundException snfe) {
            DEBUG.message("Audit service definition not found in old configuration");
        } catch (SMSException | SSOException ex) {
            DEBUG.error("An unexpected error occurred while checking Audit Service", ex);
            throw new UpgradeException("Failed to initialize AuditService upgrade step", ex);
        }
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            UpgradeProgress.reportStart("upgrade.auditservice.start");
            ServiceSchemaManager ssm = new ServiceSchemaManager(AuditConstants.SERVICE_NAME, getAdminToken());
            final Document document = XMLUtils.toDOMDocument(ssm.getSchema(), DEBUG);
            final NodeList subSchemas = document.getElementsByTagName("SubSchema");

            for (int i = 0 ; i < subSchemas.getLength(); i++) {
                final Element subSchema = (Element) subSchemas.item(i);
                if (subSchema.hasAttribute(I18N_FILE_NAME)
                        && OLD_I18N_FILE_NAME.equals(subSchema.getAttribute(I18N_FILE_NAME))) {
                    subSchema.setAttribute(I18N_FILE_NAME, NEW_I18N_FILE_NAME);
                }
            }

            final String updatedSchema = XMLUtils.print(document);
            DEBUG.message("Updating Audit Service schema with XML:\n{}", updatedSchema);
            ssm.replaceSchema(new ByteArrayInputStream(updatedSchema.getBytes(StandardCharsets.UTF_8)));
            UpgradeProgress.reportEnd("upgrade.success");
        } catch (SMSException | SSOException | IOException ex) {
            DEBUG.error("Unable to upgrade AuditService", ex);
            UpgradeProgress.reportEnd("upgrade.failed");
            throw new UpgradeException("An error occurred while trying to upgrade AuditService");
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        return BUNDLE.getString("upgrade.auditservice.short") + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        return tagSwapReport(ImmutableMap.of(LF, delimiter), "upgrade.auditservice.detailed");
    }
}
