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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import org.apache.commons.lang.StringUtils;
import org.forgerock.guava.common.base.Objects;
import org.forgerock.guava.common.collect.Sets;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.UpgradeUtils;

import javax.inject.Inject;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;

/**
 * The removal of the legacy STS involves removing SubSchema elements from the AgentService, SubSchema removal which
 * the UpgradeServiceSchemaStep does not currently support. Enhancement of this functionality, done under AME-8900,
 * resulted in ~200 functional test failures on a 12.0.2->13 upgraded OpenAM deployment. The presumption is that it
 * is very difficult to know unequivocally that remaining code does not rely upon removed Service SubSchema elements,
 * so this class will perform a more tactical removal, which will only address the removal of the four SubSchema elements
 * of the AgentService which pertain to the old sts. It will also remove the four Agent instances of these agent types which
 * the legacy STS creates automatically, and any user-created agent instances of these types. It will also remove a SharedAgent
 * with a name of agentAuth in the root realm, if it exists, provided that the three values for the AgentsAllowedToRead
 * attribute is 'wsc', 'wsp', and 'SecurityTokenService', as these values are set in the default-created instance, and the
 * presence of only these three attributes would seem to justify the conclusion that this agent has not be repurposed by
 * the OpenAM user.
 *
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeLegacySTSStep extends AbstractUpgradeStep {
    private static final String REPORT_DATA = "%REPORT_DATA%";
    private static final String COMMA = ",";
    private static final String LEGACY_STS_RELATED_SHARED_AGENT_NAME = "agentAuth";
    private static final String AGENTS_ALLOWED_TO_READ_ATTRIBUTE = "AgentsAllowedToRead";
    private static final Set<String> DEFAULT_STS_SHARED_AGENT_SHARE_SET = Sets.newHashSet("wsc", "wsp", "SecurityTokenService");
    private static final String SHARED_AGENT_SCHEMA_ID = "SharedAgent";
    private static final String ROOT_REALM = "/";
    private static final String FORWARD_SLASH = "/";
    private static final Set<String> TO_BE_REMOVED_SUB_SCHEMA_NAMES = Sets.newHashSet("WSCAgent", "STSAgent", "WSPAgent", "DiscoveryAgent");
    private final Set<String> subSchemasRequiringRemoval;
    private final Set<String> removedSubSchemas;
    private final Set<ToBeRemovedAgentState> agentsRequiringRemoval;
    private final Set<ToBeRemovedAgentState> removedAgents;
    private final Set<String> errorMessages;
    private boolean removeDefaultLegacySTSSharedAgent;

    @Inject
    public UpgradeLegacySTSStep(final PrivilegedAction<SSOToken> adminTokenAction,
                                   @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
        subSchemasRequiringRemoval = new HashSet<>();
        removedSubSchemas = new HashSet<>();
        agentsRequiringRemoval = new HashSet<>();
        removedAgents = new HashSet<>();
        errorMessages = new HashSet<>();
    }

    @Override
    public boolean isApplicable() {
        return !subSchemasRequiringRemoval.isEmpty() || !agentsRequiringRemoval.isEmpty() || removeDefaultLegacySTSSharedAgent;
    }

    @Override
    public void initialize() throws UpgradeException {
        determineSubSchemaRemovalState();
        determineToBeRemovedAgentInstances();
        determineDefaultLegacySTSSharedAgentRemoval();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            UpgradeProgress.reportStart("upgrade.legacy.sts.start");
            removeAgentInstances(agentsRequiringRemoval);
            performDefaultLegacySTSSharedAgentRemoval();
            performAgentSubSchemaRemoval();
            UpgradeProgress.reportEnd("upgrade.success");
        } catch (Exception e) {
            DEBUG.error("Unexpected exception caught in UpgradeLegacySTSStep#perform: " + e.getMessage(), e);
            UpgradeProgress.reportEnd("upgrade.failed");
            throw new UpgradeException("Upgrade of Legacy STS failed: " + e.getMessage(), e);
        }
    }

    /*
    This  method is called twice: once after initialize is called, but prior to perform, and then again, after perform
    is called. So both collections - the to-be-removed, and removed variants must be consulted, as appropriate.
     */
    @Override
    public String getShortReport(String delimiter) {
        StringBuilder builder = new StringBuilder();
        if (!removedSubSchemas.isEmpty() || !subSchemasRequiringRemoval.isEmpty()) {
            builder.append(MessageFormat.format(BUNDLE.getString("upgrade.legacy.sts.removed.agent.subshemas"),
                    (!removedSubSchemas.isEmpty() ? removedSubSchemas.size() : subSchemasRequiringRemoval.size())));
            builder.append(delimiter);
        }
        if (!removedAgents.isEmpty() || !agentsRequiringRemoval.isEmpty()) {
            builder.append(MessageFormat.format(BUNDLE.getString("upgrade.legacy.sts.removed.agent.instances"),
                    (!removedAgents.isEmpty() ? removedAgents.size() : agentsRequiringRemoval.size())));
            builder.append(delimiter);
        }
        return builder.toString();
    }

    /*
    This  method is called twice: once after initialize is called, but prior to perform, and then again, after perform
    is called. So both collections - the to-be-removed, and removed variants must be consulted, as appropriate.
     */
    @Override
    public String getDetailedReport(String delimiter) {
        final Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder builder = new StringBuilder();
        if (!removedSubSchemas.isEmpty() || !subSchemasRequiringRemoval.isEmpty()) {
            builder.append(MessageFormat.format(BUNDLE.getString("upgrade.legacy.sts.removed.agent.subshemas"),
                    (!removedSubSchemas.isEmpty() ? StringUtils.join(removedSubSchemas, COMMA) : StringUtils.join(subSchemasRequiringRemoval, COMMA))));
            builder.append(delimiter);
        }
        if (!removedAgents.isEmpty() || !agentsRequiringRemoval.isEmpty()) {
            builder.append(MessageFormat.format(BUNDLE.getString("upgrade.legacy.sts.removed.agent.instances"),
                    (!removedAgents.isEmpty() ? (delimiter + StringUtils.join(removedAgents, delimiter)) :
                            (delimiter + StringUtils.join(agentsRequiringRemoval, delimiter)))));
            builder.append(delimiter);
        }
        if (!errorMessages.isEmpty()) {
            builder.append(MessageFormat.format(BUNDLE.getString("upgrade.legacy.sts.error.messages"),
                    delimiter + StringUtils.join(errorMessages, delimiter)));
        }
        tags.put(REPORT_DATA, builder.toString());
        return tagSwapReport(tags, "upgrade.legacy.sts.report");
    }

    private void performAgentSubSchemaRemoval() throws UpgradeException {
        final String nullSubSchema = null;
        //I want to obtain the AgentService Schema, so a null SubSchema identifier is passed to getServiceSchema.
        final ServiceSchema serviceSchema =
                UpgradeUtils.getServiceSchema(IdConstants.AGENT_SERVICE, nullSubSchema, UpgradeUtils.SCHEMA_TYPE_ORGANIZATION, getAdminToken());
        if (serviceSchema == null) {
            throw new UpgradeException("Could not obtain ServiceSchema for AgentService. Legacy STS AgentService SubSchema elements cannot be removed.");
        }
        for (String subSchemaName : subSchemasRequiringRemoval) {
            UpgradeUtils.removeSubSchema(IdConstants.AGENT_SERVICE, subSchemaName, serviceSchema);
            removedSubSchemas.add(subSchemaName);
        }
    }

    private void determineSubSchemaRemovalState() throws UpgradeException {
        try {
            for (String subSchemaName : TO_BE_REMOVED_SUB_SCHEMA_NAMES) {
                if (UpgradeUtils.getServiceSchema(IdConstants.AGENT_SERVICE, subSchemaName, UpgradeUtils.SCHEMA_TYPE_ORGANIZATION, getAdminToken()) != null) {
                    subSchemasRequiringRemoval.add(subSchemaName);
                }
            }
        } catch (Exception ex) {
            DEBUG.error("An error occurred while attempting to determine which legacy STS related AgentService SubSchema elements " +
                    "require removal.", ex);
            throw new UpgradeException("Unable to determine which legacy STS related AgentService SubSchema elements require removal.");
        }
    }

    private void determineToBeRemovedAgentInstances() throws UpgradeException {
        final Set<String> realms = getRealmNames();
        for (String realm : realms) {
            populateToBeRemovedAgents(realm);
        }
    }

    private void determineDefaultLegacySTSSharedAgentRemoval() throws UpgradeException {
        try {
            ServiceConfig baseService = getOrganizationConfigForAgentService(ROOT_REALM);
            Set<String> subConfigNames = baseService.getSubConfigNames();
            if (subConfigNames.contains(LEGACY_STS_RELATED_SHARED_AGENT_NAME)) {
                final ServiceConfig agentInstance = baseService.getSubConfig(LEGACY_STS_RELATED_SHARED_AGENT_NAME);
                if (SHARED_AGENT_SCHEMA_ID.equals(agentInstance.getSchemaID())) {
                    Map<String, Set<String>> attributes = agentInstance.getAttributesWithoutDefaultsForRead();
                    if (attributes != null) {
                        Set<String> sharedSet = attributes.get(AGENTS_ALLOWED_TO_READ_ATTRIBUTE);
                        if ((sharedSet != null) && Sets.symmetricDifference(sharedSet, DEFAULT_STS_SHARED_AGENT_SHARE_SET).isEmpty()) {
                            removeDefaultLegacySTSSharedAgent = true;
                            agentsRequiringRemoval.add(new ToBeRemovedAgentState(LEGACY_STS_RELATED_SHARED_AGENT_NAME, ROOT_REALM, SHARED_AGENT_SCHEMA_ID));
                        }
                    }
                }
            }
        } catch (SMSException | SSOException e) {
            throw new UpgradeException("Could not determine whether to remove the legacy-sts SharedAgent called " +
                    LEGACY_STS_RELATED_SHARED_AGENT_NAME  + " in the root realm. Exception: " + e.getMessage());
        }
    }

    private void performDefaultLegacySTSSharedAgentRemoval() throws UpgradeException {
        try {
            if (removeDefaultLegacySTSSharedAgent) {
                final ServiceConfig baseService = getOrganizationConfigForAgentService(ROOT_REALM);
                if (baseService != null) {
                    baseService.removeSubConfig(LEGACY_STS_RELATED_SHARED_AGENT_NAME);
                    removedAgents.add(new ToBeRemovedAgentState(LEGACY_STS_RELATED_SHARED_AGENT_NAME, ROOT_REALM, SHARED_AGENT_SCHEMA_ID));
                } else {
                    errorMessages.add("When attempting to remove the shared agent associated with the legacy sts named "
                            + LEGACY_STS_RELATED_SHARED_AGENT_NAME + " no ServiceConfig could be obtained. Removal failed.");
                }
            }
        } catch (SMSException | SSOException e) {
            String message = "Exception caught removing the shared agent associated with the legacy sts named "
                    + LEGACY_STS_RELATED_SHARED_AGENT_NAME + ". Exception: " + e;
            DEBUG.error(message, e);
            throw new UpgradeException(message);
        }
    }

    private ServiceConfig getOrganizationConfigForAgentService(String realm) throws SMSException, SSOException {
        return new ServiceConfigManager(IdConstants.AGENT_SERVICE, getAdminToken()).getOrganizationConfig(realm, null);
    }

    private void populateToBeRemovedAgents(String realm) throws UpgradeException {
        try {
            ServiceConfig baseService = getOrganizationConfigForAgentService(realm);
            Set<String> subConfigNames = baseService.getSubConfigNames();
            for (String agentName : subConfigNames) {
                final ServiceConfig agentInstance = baseService.getSubConfig(agentName);
                if (TO_BE_REMOVED_SUB_SCHEMA_NAMES.contains(agentInstance.getSchemaID())) {
                    agentsRequiringRemoval.add(new ToBeRemovedAgentState(agentName, realm, agentInstance.getSchemaID()));
                }
            }
        } catch (SMSException | SSOException e) {
            throw new UpgradeException("Could not determine the legacy-sts-related agents to remove for realm "
                    + realm + ". Exception: " + e.getMessage());
        }
    }

    private void removeAgentInstances(Set<ToBeRemovedAgentState> toBeDeletedAgents) throws UpgradeException {
        ServiceConfig baseService;
        for (ToBeRemovedAgentState agentState : toBeDeletedAgents) {
            try {
                baseService = getOrganizationConfigForAgentService(agentState.agentRealm);
                if (baseService != null) {
                    baseService.removeSubConfig(agentState.agentName);
                    removedAgents.add(agentState);
                } else {
                    errorMessages.add("When attempting to remove " + agentState + " no ServiceConfig could be obtained. Removal failed.");
                }
            } catch (SMSException | SSOException e) {
                errorMessages.add(("When attempting to remove " + agentState + " encountered exception: " + e.getMessage()));
            }
        }
    }

    private static final class ToBeRemovedAgentState {
        private final String agentName;
        private final String agentRealm;
        private final String sunServiceId;

        public ToBeRemovedAgentState(String agentName, String agentRealm, String sunServiceId) {
            this.agentName = agentName;
            this.agentRealm = agentRealm;
            this.sunServiceId = sunServiceId;
        }

        @Override
        public String toString() {
            return INDENT + "Agent " + agentName + " in realm " + agentRealm + " of type " + sunServiceId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else {
                if (other instanceof ToBeRemovedAgentState) {
                    final ToBeRemovedAgentState otherAgentState = (ToBeRemovedAgentState)other;
                    return Objects.equal(agentName, otherAgentState.agentName) && Objects.equal(agentRealm, otherAgentState.agentRealm)
                            && Objects.equal(sunServiceId, otherAgentState.sunServiceId);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
}
