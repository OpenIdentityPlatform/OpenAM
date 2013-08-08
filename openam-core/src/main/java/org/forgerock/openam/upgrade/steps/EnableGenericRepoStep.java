/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.upgrade.steps;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.HashMap;
import java.util.Map;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;

/**
 * A very simple step to reenable the Generic LDAPv3 data store (i.e. making it available on the admin console as an
 * option). In the future this step should not be necessary, instead the UpgradeServiceSchemaStep should be refactored
 * to have more complex support for subschema changes.
 *
 * @author Peter Major
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class EnableGenericRepoStep extends AbstractUpgradeStep {

    private boolean applicable = false;

    @Override
    public boolean isApplicable() {
        return applicable;
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            ServiceSchema genericSchema = getGenericLDAPv3Schema();
            if (genericSchema.getI18NKey() == null || genericSchema.getI18NKey().isEmpty()) {
                applicable = true;
            }
        } catch (Exception ex) {
            DEBUG.error("An error occurred while trying to check the current status of the generic LDAPv3 repo", ex);
            throw new UpgradeException(ex);
        }
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            ServiceSchema genericSchema = getGenericLDAPv3Schema();
            UpgradeProgress.reportStart("upgrade.genericrepo.start");
            genericSchema.setI18Nkey("a2039");
            UpgradeProgress.reportEnd("upgrade.success");
        } catch (Exception ex) {
            UpgradeProgress.reportEnd("upgrade.failed");
            DEBUG.error("An error occurred while trying to enable the generic LDAPv3 repo", ex);
            throw new UpgradeException(ex);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        return BUNDLE.getString("upgrade.genericrepo") + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tagSwap = new HashMap<String, String>(1);
        tagSwap.put(LF, delimiter);
        return UpgradeServices.tagSwapReport(tagSwap, "upgrade.genericreporeport");
    }

    private ServiceSchema getGenericLDAPv3Schema() throws SSOException, SMSException {
        ServiceSchemaManager ssm = new ServiceSchemaManager(IdConstants.REPO_SERVICE, getAdminToken());
        ServiceSchema organizationSchema = ssm.getOrganizationSchema();
        ServiceSchema genericSchema = organizationSchema.getSubSchema("LDAPv3");
        return genericSchema;
    }
}
