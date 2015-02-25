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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.ServicesDefaultValues;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.opendj.ldap.ConnectionFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;

/**
 * Migrates the list of valid goto domains from iPlanetAMAuthService to validationService and also updates the
 * delegation policies so the agent accounts can access the new service (necessary for DAS).
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class MigrateValidGotoSetting extends AbstractUpgradeStep {

    private static final String LEGACY_GOTO_DOMAINS_SETTING = "iplanet-am-auth-valid-goto-domains";
    private static final String GOTO_RESOURCES = "openam-auth-valid-goto-resources";
    private static final String VALIDATION_SERVICE = "validationService";
    private static final String HIDDEN_REALM = "/sunamhiddenrealmdelegationservicepermissions";
    private static final String DELEGATION_POLICY_NAME = "AgentAccessToValidationService";
    private static final String DELEGATION_POLICY_FILE = "/WEB-INF/template/sms/validationServiceDelegationPolicy.xml";
    private static final String GOTO_DATA = "%GOTO_DATA%";
    private final Map<String, Set<String>> changes = new HashMap<String, Set<String>>();
    private boolean delegationPolicyFound = false;

    @Inject
    public MigrateValidGotoSetting(PrivilegedAction<SSOToken> adminTokenAction,
                                   @Named(DataLayerConstants.DATA_LAYER_BINDING) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public boolean isApplicable() {
        return !delegationPolicyFound;
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            final PolicyManager pm = new PolicyManager(getAdminToken(), HIDDEN_REALM);
            if (pm.getPolicyNames(DELEGATION_POLICY_NAME).isEmpty()) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Unable to find the delegation policy in the hidden realm, looking for existing goto"
                            + " domain values.");
                }
                //The delegation policy is not defined yet in the configuration, we need to migrate the goto domains.
                final ServiceConfigManager scm = new ServiceConfigManager(ISAuthConstants.AUTH_SERVICE_NAME,
                        getAdminToken());
                for (final String realm : getRealmNames()) {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Looking for valid goto URLs in realm " + realm);
                    }
                    final ServiceConfig organizationConfig = scm.getOrganizationConfig(realm, null);
                    final Map<String, Set<String>> attrs = organizationConfig.getAttributesWithoutDefaults();
                    final Set<String> validDomains = attrs.get(LEGACY_GOTO_DOMAINS_SETTING);
                    if (validDomains != null && !validDomains.isEmpty()) {
                        changes.put(realm, validDomains);
                    }
                }
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Found the following existing goto URL domains in realms: " + changes);
                }
            } else {
                delegationPolicyFound = true;
            }
        } catch (final NameNotFoundException nnfe) {
            throw new UpgradeException("Unable to find hidden realm", nnfe);
        } catch (final PolicyException pe) {
            throw new UpgradeException("Unexpected error occurred while retrieving policies from the hidden realm", pe);
        } catch (final SMSException smse) {
            throw new UpgradeException("An error occurred while checking for old valid goto domains", smse);
        } catch (final SSOException ssoe) {
            throw new UpgradeException("An error occurred while checking for old valid goto domains", ssoe);
        }
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            if (!changes.isEmpty()) {
                final ServiceConfigManager validationService = new ServiceConfigManager(VALIDATION_SERVICE,
                        getAdminToken());
                final ServiceConfigManager authService = new ServiceConfigManager(ISAuthConstants.AUTH_SERVICE_NAME,
                        getAdminToken());
                for (final Map.Entry<String, Set<String>> entry : changes.entrySet()) {
                    final String realm = entry.getKey();
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Starting to migrate goto domains for realm: " + realm);
                    }
                    UpgradeProgress.reportStart("upgrade.goto.migrate.start", realm);
                    validationService.createOrganizationConfig(realm, getAttrMap(GOTO_RESOURCES, entry.getValue()));

                    //The settings now are migrated, we should now clear up the legacy settings
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Removing old goto domains from iPlanetAMAuthService");
                    }
                    final ServiceConfig organizationConfig = authService.getOrganizationConfig(realm, null);
                    organizationConfig.setAttributes(getAttrMap(LEGACY_GOTO_DOMAINS_SETTING, Collections.EMPTY_SET));
                    UpgradeProgress.reportEnd("upgrade.success");
                }
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Attempting to create the delegation policy in the hidden realm");
            }
            UpgradeProgress.reportStart("upgrade.goto.policy.start");
            final PolicyManager pm = new PolicyManager(getAdminToken(), HIDDEN_REALM);
            String policy = AMSetupServlet.readFile(DELEGATION_POLICY_FILE).toString();
            policy = ServicesDefaultValues.tagSwap(policy, true);
            //Adding the delegation privileges to allow agent accounts to read the new validationService.
            PolicyUtils.createPolicies(pm, new ByteArrayInputStream(policy.getBytes()));
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Delegation policy successfully created under the hidden realm");
            }
            UpgradeProgress.reportEnd("upgrade.success");
        } catch (final IOException ioe) {
            UpgradeProgress.reportEnd("upgrade.failed");
            throw new UpgradeException("An IO error occurred while reading the delegation policy", ioe);
        } catch (final PolicyException pe) {
            UpgradeProgress.reportEnd("upgrade.failed");
            throw new UpgradeException("An unexpected error occurred while importing the delegation policy", pe);
        } catch (final SMSException smse) {
            UpgradeProgress.reportEnd("upgrade.failed");
            throw new UpgradeException("An error occurred while migrating the valid goto domain setting", smse);
        } catch (final SSOException ssoe) {
            UpgradeProgress.reportEnd("upgrade.failed");
            throw new UpgradeException("An error occurred while migrating the valid goto domain setting", ssoe);
        }
    }

    private Map<String, Set<String>> getAttrMap(final String attrName, final Set<String> values) {
        final Map<String, Set<String>> ret = new HashMap<String, Set<String>>(1);
        ret.put(attrName, values);
        return ret;
    }

    @Override
    public String getShortReport(String delimiter) {
        return BUNDLE.getString("upgrade.goto.migrate.short") + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        final Map<String, String> tagSwap = new HashMap<String, String>(2);
        tagSwap.put(LF, delimiter);
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : changes.entrySet()) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(": ").append(entry.getKey()).append(delimiter);
            for (String validDomain : entry.getValue()) {
                sb.append(INDENT).append(validDomain).append(delimiter);
            }
        }
        tagSwap.put(GOTO_DATA, sb.toString());
        return UpgradeServices.tagSwapReport(tagSwap, "upgrade.gotoreport");
    }
}
