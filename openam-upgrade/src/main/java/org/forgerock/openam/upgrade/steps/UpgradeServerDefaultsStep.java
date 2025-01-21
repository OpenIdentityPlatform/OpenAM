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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.upgrade.steps;

import static com.sun.identity.common.configuration.ServerConfiguration.*;
import static org.forgerock.openam.upgrade.UpgradeServices.*;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.ServerUpgrade;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;

/**
 * Detects changes made to default server properties and upgrades them if required.
 */
@UpgradeStepInfo(dependsOn = "*")
public class UpgradeServerDefaultsStep extends AbstractUpgradeStep {

    private static final String VALID_SERVER_CONFIG_PROPERTIES = "/validserverconfig.properties";
    private static final String NEW_ATTRS = "%NEW_ATTRS%";
    private static final String MOD_ATTRS = "%MOD_ATTRS%";
    private static final String DEL_ATTRS = "%DEL_ATTRS%";
    private Map<String, String> existingDefaults;
    private Map<String, String> addedAttrs;
    private Map<String, String> modifiedAttrs;
    private Set<String> deletedAttrs;

    @Inject
    public UpgradeServerDefaultsStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public boolean isApplicable() {
        return !addedAttrs.isEmpty() || !modifiedAttrs.isEmpty() || !deletedAttrs.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            existingDefaults = new HashMap(ServerConfiguration.getServerInstance(getAdminToken(),
                    ServerConfiguration.DEFAULT_SERVER_CONFIG));
            Map<String, String> newDefaults = ServerConfiguration.getNewServerDefaults(getAdminToken());

            addedAttrs = calculateAddedServerDefaults(newDefaults, existingDefaults);
            modifiedAttrs = calculateModifiedServerDefaults(newDefaults, existingDefaults);
            deletedAttrs = calculateDeletedServerDefaults(existingDefaults);
        } catch (Exception ex) {
            throw new UpgradeException(ex);
        }
    }

    /**
     * Finds newly added default server properties.
     *
     * @param newDefaults The default server properties defined in the new version.
     * @param existingDefaults The default server properties defined in the current version.
     * @return The newly added properties.
     */
    @VisibleForTesting
    protected Map<String, String> calculateAddedServerDefaults(Map<String, String> newDefaults,
            Map<String, String> existingDefaults) {
        Map<String, String> addedValues = new HashMap<String, String>();

        for (Map.Entry<String, String> newAttr : newDefaults.entrySet()) {
            if (!existingDefaults.containsKey(newAttr.getKey())) {
                addedValues.put(newAttr.getKey(), newAttr.getValue());
            }
        }

        return addedValues;
    }

    private Map<String, String> calculateModifiedServerDefaults(Map<String, String> newDefaults,
            Map<String, String> existingDefaults) throws UpgradeException {
        Set<String> attrsToUpgrade = ServerUpgrade.getAttrsToUpgrade();
        return calculateModifiedServerDefaults(newDefaults, existingDefaults, attrsToUpgrade);
    }

    /**
     * Only include in the list of modified attributes those that are listed in the serverupgrade.properites file;
     * otherwise existing properties that have been locally modified would be over-written.
     *
     * @param newDefaults The default server properties defined in the new version.
     * @param existingDefaults The default server properties defined in the current version.
     * @param attrToModify Attributes that should be checked for modification.
     * @return Modified key value pairs.
     */
    @VisibleForTesting
    protected Map<String, String> calculateModifiedServerDefaults(Map<String, String> newDefaults,
            Map<String, String> existingDefaults, Set<String> attrToModify) {
        Map<String, String> modifiedValues = new HashMap<String, String>();

        for (String attrName : attrToModify) {
            String newAttr = newDefaults.get(attrName);
            String existingAttr = existingDefaults.get(attrName);
            if (newAttr != null && existingAttr != null && !newAttr.equals(existingAttr)) {
                modifiedValues.put(attrName, newAttr);
            }
        }

        return modifiedValues;
    }

    private Set<String> calculateDeletedServerDefaults(Map<String, String> existingDefaults) throws IOException {
        Properties validProperties = new Properties();
        validProperties.load(getClass().getResourceAsStream(VALID_SERVER_CONFIG_PROPERTIES));
        Map<String, String> validServerProperties = new HashMap(validProperties);
        return calculateDeletedServerDefaults(existingDefaults, validServerProperties);
    }

    /**
     * Finds deleted attributes by comparing the currently defined properties against the new valid server properties.
     * By comparing against the valid server properties we can ensure that manually defined custom properties will not
     * be removed.
     *
     * @param existingDefaults The currently defined default server properties.
     * @param validServerProperties The valid server properties defined in validserverconfig.properties.
     * @return The attributenames that are no longer valid.
     */
    @VisibleForTesting
    protected Set<String> calculateDeletedServerDefaults(Map<String, String> existingDefaults,
            Map<String, String> validServerProperties) {
        Set<String> deletedValues = new HashSet<String>();

        for (String existingAttr : existingDefaults.keySet()) {
            int startBracket = existingAttr.indexOf('[');
            if (startBracket != -1) {
                existingAttr = existingAttr.substring(0, startBracket);
            }

            if (!validServerProperties.containsKey(existingAttr)) {
                deletedValues.add(existingAttr);
            }
        }

        return deletedValues;
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            UpgradeProgress.reportStart("upgrade.platformupdate");
            existingDefaults = new HashMap(ServerConfiguration.getServerInstance(getAdminToken(),
                    ServerConfiguration.DEFAULT_SERVER_CONFIG));

            // Refine the change sets
            addedAttrs = calculateAddedServerDefaults(addedAttrs, existingDefaults);
            modifiedAttrs = calculateModifiedServerDefaults(modifiedAttrs, existingDefaults);
            deletedAttrs = calculateDeletedServerDefaults(existingDefaults);

            Map<String, String> upgradedValues = new HashMap<>(existingDefaults);
            upgradedValues.putAll(addedAttrs);
            upgradedValues.putAll(modifiedAttrs);
            upgradedValues.keySet().removeAll(deletedAttrs);

            ServerConfiguration.upgradeServerInstance(getAdminToken(), DEFAULT_SERVER_CONFIG, DEFAULT_SERVER_ID,
                    upgradedValues);
            UpgradeProgress.reportEnd("upgrade.success");
        } catch (Exception ex) {
            UpgradeUtils.debug.error("Unable to upgrade server default properties", ex);
            throw new UpgradeException(ex);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        boolean reported = false;
        StringBuilder sb = new StringBuilder();
        sb.append(BUNDLE.getString("upgrade.defaults")).append(": ");
        if (!addedAttrs.isEmpty()) {
            sb.append(BUNDLE.getString("upgrade.new")).append(" (").append(addedAttrs.size()).append(')');
            reported = true;
        }
        if (!modifiedAttrs.isEmpty()) {
            if (reported) {
                sb.append(", ");
            }
            sb.append(BUNDLE.getString("upgrade.updated")).append(" (").append(modifiedAttrs.size()).append(')');
            reported = true;

        }
        if (!deletedAttrs.isEmpty()) {
            if (reported) {
                sb.append(", ");
            }
            sb.append(BUNDLE.getString("upgrade.deleted")).append(" (").append(deletedAttrs.size()).append(')');
        }
        sb.append(delimiter);
        return sb.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        
        if (!addedAttrs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> newAttr : addedAttrs.entrySet()) {
                sb.append(BULLET).append(BUNDLE.getString("upgrade.attrname")).append(": ").append(newAttr.getKey());
                sb.append(" : ").append(BUNDLE.getString("upgrade.value")).append(": ");
                sb.append(newAttr.getValue()).append(delimiter);
            }
            tags.put(NEW_ATTRS, sb.toString());
        } else {
            tags.put(NEW_ATTRS, BUNDLE.getString("upgrade.none"));
        }
        if (!modifiedAttrs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> modAttr : modifiedAttrs.entrySet()) {
                sb.append(BULLET).append(BUNDLE.getString("upgrade.attrname")).append(": ");
                sb.append(modAttr.getKey()).append(delimiter);
                sb.append(INDENT).append(BUNDLE.getString("upgrade.old")).append(' ');
                sb.append(BUNDLE.getString("upgrade.value")).append(": ");
                sb.append(existingDefaults.get(modAttr.getKey())).append(delimiter);
                sb.append(INDENT).append(BUNDLE.getString("upgrade.new")).append(' ');
                sb.append(BUNDLE.getString("upgrade.value")).append(": ");
                sb.append(modAttr.getValue()).append(delimiter);
            }
            tags.put(MOD_ATTRS, sb.toString());
        } else {
            tags.put(MOD_ATTRS, BUNDLE.getString("upgrade.none"));
        }
        if (!deletedAttrs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String deletedAttr : deletedAttrs) {
                sb.append(BULLET).append(BUNDLE.getString("upgrade.attrname")).append(": ").append(deletedAttr).append(delimiter);
            }
            tags.put(DEL_ATTRS, sb.toString());
        } else {
            tags.put(DEL_ATTRS, BUNDLE.getString("upgrade.none"));
        }
        return tagSwapReport(tags, "upgrade.defaultsreport");
    }
    
    /**
     * Gets the current server default values to see if any updates in previous upgrade steps need to be re-applied.
     * @return the key value pairs of attributes that will need to be re-applied
     * @throws UpgradeException
     */
    private Map<String, String> getUpdatedDefaults() throws UpgradeException {
        HashMap<String, String> modifiedValues = new HashMap<String, String>();
        try {
            Map<String, String> currentDefaults = new HashMap(ServerConfiguration.getServerInstance(getAdminToken(),
                    ServerConfiguration.DEFAULT_SERVER_CONFIG));
            String currentDefault = currentDefaults.get(CoreTokenConstants.CTS_STORE_HOSTNAME);
            String existingDefault = existingDefaults.get(CoreTokenConstants.CTS_STORE_HOSTNAME);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("currentDefault: " + currentDefault + "existingDefault: "
                        + existingDefault);
            }
            if (currentDefault != null && existingDefault != null && !existingDefault.equals(currentDefault)) {
                modifiedValues.put(CoreTokenConstants.CTS_STORE_HOSTNAME, currentDefault);
            }
        } catch (Exception ex) {
            DEBUG.error("An error occurred trying to get current configuration.", ex);
            throw new UpgradeException(ex);
        }
        return modifiedValues;
    }
    
}
