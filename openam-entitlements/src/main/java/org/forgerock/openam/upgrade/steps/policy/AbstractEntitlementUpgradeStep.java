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

package org.forgerock.openam.upgrade.steps.policy;

import com.iplanet.sso.SSOToken;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.forgerock.openam.upgrade.steps.AbstractUpgradeStep;
import org.forgerock.openam.utils.IOUtils;
import org.w3c.dom.Document;

import javax.inject.Inject;
import java.io.InputStream;
import java.security.PrivilegedAction;

/**
 * An abstract class for common functionality in entitlement steps.
 *
 * @since 13.0.0
 */
public abstract class AbstractEntitlementUpgradeStep extends AbstractUpgradeStep {

    protected static final String ENTITLEMENTS_XML = "entitlement.xml";
    protected static final String ROOT_REALM = "/";
    protected static final String ID = "id";
    protected static final String NAME = "name";

    protected static final String AUDIT_UPGRADE_SUCCESS = "upgrade.success";
    protected static final String AUDIT_UPGRADE_FAIL = "upgrade.failed";
    protected static final String AUDIT_REALM = "upgrade.realm";

    @Inject
    public AbstractEntitlementUpgradeStep(
            final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    /**
     * Retrieves the XML document for entitlements.
     *
     * @return a document instance representing entitlements
     *
     * @throws UpgradeException should an error occur attempting to read the entitlement xml
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
