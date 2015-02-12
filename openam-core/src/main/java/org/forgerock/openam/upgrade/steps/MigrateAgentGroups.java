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
package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.upgrade.UpgradeServices.*;
import static org.forgerock.openam.utils.CollectionUtils.*;

import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * The agent group information is no longer stored in the labeledUri setting, hence existing agent groups needs to be
 * updated to migrate existing agent group membership to the new format ("agentgroup" attribute in the agent
 * serviceconfig).
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class MigrateAgentGroups extends AbstractUpgradeStep {

    private static final String AGENT_DATA = "%AGENT_DATA%";
    private static final String AGENT_GROUP = "agentgroup";
    private final Map<String, Set<String>> upgradableAgents = new HashMap<String, Set<String>>();

    @Inject
    public MigrateAgentGroups(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public boolean isApplicable() {
        return !upgradableAgents.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            final ServiceConfigManager scm = new ServiceConfigManager(getAdminToken(), IdConstants.AGENT_SERVICE,
                    "1.0");
            for (final String realm : getRealmNames()) {
                final ServiceConfig orgConfig = scm.getOrganizationConfig(realm, null);
                final Set<String> agentNames = orgConfig.getSubConfigNames();
                for (final String agentName : agentNames) {
                    final ServiceConfig agentConfig = orgConfig.getSubConfig(agentName);
                    final String agentGroupName = agentConfig.getLabeledUri();
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Agent: \"" + agentName + "\" Group: \"" + agentGroupName + "\"");
                    }
                    if (agentGroupName != null && !agentGroupName.isEmpty()) {
                        Set<String> agents = upgradableAgents.get(realm);
                        if (agents == null) {
                            agents = new HashSet<String>();
                            upgradableAgents.put(realm, agents);
                        }
                        agents.add(agentName);
                    }
                }
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("The following agents needs to be migrated: " + upgradableAgents);
            }
        } catch (final SMSException smse) {
            throw new UpgradeException("Unable to initialize agent group migration", smse);
        } catch (final SSOException ssoe) {
            throw new UpgradeException("Unable to initialize agent group migration", ssoe);
        }
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            final ServiceConfigManager scm = new ServiceConfigManager(getAdminToken(), IdConstants.AGENT_SERVICE,
                    "1.0");
            for (final Map.Entry<String, Set<String>> entry : upgradableAgents.entrySet()) {
                final String realm = entry.getKey();
                final Set<String> agents = entry.getValue();
                final ServiceConfig orgConfig = scm.getOrganizationConfig(realm, null);
                for (final String agentName : agents) {
                    UpgradeProgress.reportStart("upgrade.agent.group.start", agentName);
                    final ServiceConfig agentConfig = orgConfig.getSubConfig(agentName);
                    final String agentGroupName = agentConfig.getLabeledUri();
                    agentConfig.setAttributes(getAgentGroupAttribute(agentGroupName));
                    agentConfig.deleteLabeledUri(agentGroupName);
                    UpgradeProgress.reportEnd("upgrade.success");
                }
            }
        } catch (final SMSException smse) {
            UpgradeProgress.reportEnd("upgrade.failed");
            throw new UpgradeException("Unable to perform agent group migration", smse);
        } catch (final SSOException ssoe) {
            UpgradeProgress.reportEnd("upgrade.failed");
            throw new UpgradeException("Unable to perform agent group migration", ssoe);
        }
    }

    private Map<String, Set<String>> getAgentGroupAttribute(final String agentGroupName) {
        final Map<String, Set<String>> attr = new HashMap<String, Set<String>>(1);
        attr.put(AGENT_GROUP, asSet(agentGroupName));
        return attr;
    }

    @Override
    public String getShortReport(final String delimiter) {
        int count = 0;
        for (final Set<String> agents : upgradableAgents.values()) {
            count += agents.size();
        }
        return MessageFormat.format(BUNDLE.getString("upgrade.agent.group.short"), count) + delimiter;
    }

    @Override
    public String getDetailedReport(final String delimiter) {
        final Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, Set<String>> entry : upgradableAgents.entrySet()) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(": ").append(entry.getKey()).append(delimiter);
            for (final String agent : entry.getValue()) {
                sb.append(INDENT).append(agent).append(delimiter);
            }
        }
        tags.put(AGENT_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.agent.group.detailed");
    }
}
