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

import com.sun.identity.common.configuration.ServerConfiguration;
import static com.sun.identity.common.configuration.ServerConfiguration.DEFAULT_SERVER_CONFIG;
import static com.sun.identity.common.configuration.ServerConfiguration.DEFAULT_SERVER_ID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.upgrade.ServerUpgrade;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.UpgradeUtils;

/**
 * Detects changes made to default server properties and upgrades them if required.
 *
 * @author Peter Major
 */
@UpgradeStepInfo(dependsOn = "*")
public class UpgradeServerDefaultsStep extends AbstractUpgradeStep {

    private static final String NEW_ATTRS = "%NEW_ATTRS%";
    private static final String MOD_ATTRS = "%MOD_ATTRS%";
    private static final String DEL_ATTRS = "%DEL_ATTRS%";
    private Map<String, String> existingValues;
    private Map<String, String> addedAttrs;
    private Map<String, String> modifiedAttrs;
    private Set<String> deletedAttrs;
    private Map<String, String> upgradedValues;

    @Override
    public boolean isApplicable() {
        return !addedAttrs.isEmpty() || !modifiedAttrs.isEmpty() || !deletedAttrs.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            existingValues = new HashMap(ServerConfiguration.getServerInstance(getAdminToken(),
                    ServerConfiguration.DEFAULT_SERVER_CONFIG));
            Map<String, String> newValues = ServerConfiguration.getNewServerDefaults(getAdminToken());
            Set<String> attrsToUpgrade = ServerUpgrade.getAttrsToUpgrade();
            addedAttrs = calculateAddedServerDefaults(newValues, existingValues);
            modifiedAttrs = calculateModifiedServerDefaults(newValues, existingValues, attrsToUpgrade);
            deletedAttrs = calculateDeletedServerDefaults(newValues, existingValues);
            upgradedValues = new HashMap<String, String>(existingValues);

            for (Map.Entry<String, String> newAttr : addedAttrs.entrySet()) {
                upgradedValues.put(newAttr.getKey(), newAttr.getValue());
            }

            for (Map.Entry<String, String> modAttr : modifiedAttrs.entrySet()) {
                upgradedValues.put(modAttr.getKey(), modAttr.getValue());
            }

            for (String deletedAttr : deletedAttrs) {
                upgradedValues.remove(deletedAttr);
            }
        } catch (Exception ex) {
            throw new UpgradeException(ex);
        }
    }

    private static Map<String, String> calculateAddedServerDefaults(
            Map<String, String> newValues, Map<String, String> existingValues) {
        Map<String, String> addedValues = new HashMap<String, String>();

        for (Map.Entry<String, String> newAttr : newValues.entrySet()) {
            if (!(existingValues.containsKey(newAttr.getKey()))) {
                addedValues.put(newAttr.getKey(), newAttr.getValue());
            }
        }

        return addedValues;
    }

    /**
     * Only include in the list of modified attributes those that are listed in
     * the serverupgrade.properites file; otherwise existing properties that
     * have been locally modified will be over-written.
     *
     * @param newValues
     * @param existingValues
     * @return modified key value pairs
     */
    private static Map<String, String> calculateModifiedServerDefaults(
            Map<String, String> newValues, Map<String, String> existingValues, Set<String> attrToModify) {
        Map<String, String> modifiedValues = new HashMap<String, String>();

        for (Map.Entry<String, String> newAttr : newValues.entrySet()) {
            if (attrToModify.contains(newAttr.getKey())) {
                if (existingValues.containsKey(newAttr.getKey())) {
                    if (!(existingValues.get(newAttr.getKey()).equals(newAttr.getValue()))) {
                        modifiedValues.put(newAttr.getKey(), newAttr.getValue());
                    }
                }
            }
        }

        return modifiedValues;
    }

    private static Set<String> calculateDeletedServerDefaults(
            Map<String, String> newValues, Map<String, String> existingValues) {
        Set<String> deletedValues = new HashSet<String>();

        for (Map.Entry<String, String> existingAttr : existingValues.entrySet()) {
            if (!(newValues.containsKey(existingAttr.getKey()))) {
                deletedValues.add(existingAttr.getKey());
            }
        }

        return deletedValues;
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            UpgradeProgress.reportStart("upgrade.platformupdate");
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
                sb.append(existingValues.get(modAttr.getKey())).append(delimiter);
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
}
