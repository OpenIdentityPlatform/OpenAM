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
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.DNMapper;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.authentication.modules.oath.JsonConversionUtils;
import org.forgerock.openam.rest.devices.OathDeviceSettings;
import org.forgerock.openam.rest.devices.OathDevicesDao;
import org.forgerock.openam.rest.devices.services.OathServiceFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

/**
 * Migrates old OATH profile settings into a newly compacted Oath Device Profile stored as
 * a single attribute on a user's profile.
 *
 * Copies these values from the old realm configuration:
 *
 *  - iplanet-am-auth-oath-add-checksum
 *  - iplanet-am-auth-oath-truncation-offset
 *
 * Copies these values from their old place of storage on the user's profile:
 *
 *  - last login time (via iplanet-am-auth-oath-last-login-time-attribute-name)
 *  - counter value (via iplanet-am-auth-oath-hotp-counter-attribute)
 *  - secret key value (via iplanet-am-auth-oath-secret-key-attribute)
 *
 *  A previously-registered device will be given the name "Migrated User Device", and a
 *  random UUID (for referencing via REST), as well as 10 random recovery codes.
 *
 *  Will not upgrade users in realms where multiple OATH modules have existed as too
 *  great an ambiguity would exist. Users can delete and re-register their devices via
 *  the user dashboard if there are any issues with the upgrade process.
 *
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class TwoStepVerificationUpgradeStep extends AbstractUpgradeStep {

    private final OathServiceFactory oathServiceFactory;

    private ArrayList<String> skippedInRealm = new ArrayList<>();
    private Map<String, OathData> upgradeMap = new HashMap<>();
    private Map<String, Integer> numUpgradedMap = new HashMap<>();

    @Inject
    public TwoStepVerificationUpgradeStep(final PrivilegedAction<SSOToken> adminTokenAction,
                                          @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory,
                                          OathServiceFactory serviceFactory) {
        super(adminTokenAction, factory);
        this.oathServiceFactory = serviceFactory;
    }

    @Override
    public boolean isApplicable() {
        return !upgradeMap.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            for (String realm : getRealmNames()) {
                captureOathDataPerRealm(realm);
            }
        } catch (AMConfigurationException e) {
            DEBUG.error("An error occurred while trying to look for upgradable OATH auth modules", e);
            throw new UpgradeException("Unable to retrieve OATH auth modules", e);
        }
    }

    private void captureOathDataPerRealm(String realm) throws AMConfigurationException {
        OathData oathData = null;
        AMAuthenticationManager authManager = new AMAuthenticationManager(getAdminToken(), realm);
        Set<AMAuthenticationInstance> instances = authManager.getAuthenticationInstances();
        boolean inThisRealm = false;
        for (AMAuthenticationInstance instance : instances) {
            if ("OATH".equalsIgnoreCase(instance.getType())) {

                if (inThisRealm) {
                    skippedInRealm.add(realm);
                    DEBUG.message("Found second OATH Module called {}, in realm {}. Skipped realm.",
                            instance.getName(), realm);
                    return;
                }

                inThisRealm = true;

                DEBUG.message("Found OATH Module called {}, in realm {}", instance.getName(), realm);
                @SuppressWarnings("unchecked")
                Map<String, Set<String>> attributes = instance.getAttributeValues();

                oathData = getOathData(attributes);
            }
        }

        if (oathData != null && oathData.isUsable()) {
            upgradeMap.put(realm, oathData);
        } else {
            skippedInRealm.add(realm);
        }
    }

    private OathData getOathData(Map<String, Set<String>> attributes) {
        OathData oathData = new OathData();
        if (attributes.containsKey("iplanet-am-auth-oath-secret-key-attribute")) {
            String key = CollectionUtils.getFirstItem(attributes.get("iplanet-am-auth-oath-secret-key-attribute"),
                    null);
            if (StringUtils.isNotBlank(key)) {
                oathData.setSecretKeyAttribute(key);
            }
        }

        if (attributes.containsKey("iplanet-am-auth-oath-hotp-counter-attribute")) {
            String counter = CollectionUtils.getFirstItem(attributes.get("iplanet-am-auth-oath-hotp-counter-attribute"),
                    null);
            if (StringUtils.isNotBlank(counter)) {
                oathData.setCounterAttribute(counter);
            }
        }

        if (attributes.containsKey("iplanet-am-auth-oath-last-login-time-attribute-name")) {
            String loginTime =
                    CollectionUtils.getFirstItem(attributes.get("iplanet-am-auth-oath-last-login-time-attribute-name"),
                    null);
            if (StringUtils.isNotBlank(loginTime)) {
                oathData.setTimeAttribute(loginTime);
            }
        }

        if (attributes.containsKey("iplanet-am-auth-oath-truncation-offset")) {
            String truncation = CollectionUtils.getFirstItem(attributes.get("iplanet-am-auth-oath-truncation-offset"),
                    null);
            if (StringUtils.isNotBlank(truncation)) {
                oathData.setTruncationValue(Integer.valueOf(truncation));
            }
        }

        if (attributes.containsKey("iplanet-am-auth-oath-add-checksum")) {
            String checksum = CollectionUtils.getFirstItem(attributes.get("iplanet-am-auth-oath-add-checksum"),
                    null);
            if (StringUtils.isNotBlank(checksum)) {
                oathData.setChecksum(Boolean.parseBoolean(checksum));
            }
        }

        if (attributes.containsKey("iplanet-am-auth-oath-skippable-attr-name")) {
            String skippable = CollectionUtils.getFirstItem(attributes.get("iplanet-am-auth-oath-skippable-attr-name"),
                    null);
            if (StringUtils.isNotBlank(skippable)) {
                oathData.setSkippable(skippable);
            }
        }

        return oathData;
    }

    @Override
    public void perform() throws UpgradeException {

        for (Map.Entry<String, OathData> entry : upgradeMap.entrySet()) { //for each realm
            int numberUsersUpgraded = 0;
            String realm = entry.getKey();

            OathDevicesDao dao = new OathDevicesDao(oathServiceFactory);
            OathData attributes = entry.getValue();
            AMIdentityRepository amIdRepo = AuthD.getAuth().getAMIdentityRepository(DNMapper.orgNameToDN(realm));
            IdSearchControl idsc = new IdSearchControl();
            idsc.setAllReturnAttributes(true);
            Set<AMIdentity> results;
            idsc.setMaxResults(0);
            IdSearchResults searchResults;
            try {
                searchResults = amIdRepo.searchIdentities(IdType.USER, "*", idsc); //find all the things

                if (searchResults != null) {
                    results = searchResults.getSearchResults();
                    for (AMIdentity id : results) {

                        //we can only upgrade validly once...
                        if (dao.getDeviceProfiles(id.getName(), id.getRealm()).isEmpty() ) {

                            //gets the value from the attribute of counter
                            String counterVal =
                                    (String) CollectionUtils.getFirstItem(id.getAttribute(attributes.getCounter()), null);
                            //gets the value from the attribute of key
                            String keyVal =
                                    (String) CollectionUtils.getFirstItem(id.getAttribute(attributes.getKey()), null);
                            //gets the value from the attribute of time
                            String timeVal =
                                    (String) CollectionUtils.getFirstItem(id.getAttribute(attributes.getTime()), null);

                            //store the new values and save on the ID (assuming we have at least a shared key)
                            OathDeviceSettings settings = null;

                            if (keyVal != null) {
                                settings = new OathDeviceSettings();
                                settings.setSharedSecret(keyVal);

                                if (counterVal != null) {
                                    try {
                                        settings.setCounter(Integer.valueOf(counterVal));
                                    } catch (NumberFormatException e) {
                                        continue; //can't upgrade this one
                                    }
                                }

                                if (timeVal != null) {
                                    try {
                                        settings.setLastLogin(Long.valueOf(timeVal));
                                    } catch (NumberFormatException e) {
                                        continue; //can't upgrade this one
                                    }
                                }

                                settings.setDeviceName("Migrated User Device");
                                settings.setTruncationOffset(attributes.getTruncationValue());
                                settings.setChecksumDigit(attributes.getChecksum());
                                settings.setRecoveryCodes(OathDeviceSettings.generateRecoveryCodes(10));
                                settings.setUUID(UUID.randomUUID().toString());
                            }

                            if (settings != null) {
                                dao.saveDeviceProfiles(id.getName(), id.getRealm(),
                                        Collections.singletonList(JsonConversionUtils.toJsonValue(settings)));

                                //if there's a device we're saving, set their skippable as not-skippable
                                //otherwise leave it as not set
                                HashMap<String, Set<String>> attrMap = new HashMap<>();
                                attrMap.put(attributes.getSkippable(), Collections.singleton("2"));
                                id.setAttributes(attrMap);
                                id.store();

                                numberUsersUpgraded++;
                            }
                        }
                    }
                }
            } catch (IdRepoException | SSOException | IOException | InternalServerErrorException e) {
                DEBUG.error(e.getMessage());
                throw new UpgradeException("Unable to parse data for device migration.");
            }

            numUpgradedMap.put(realm, numberUsersUpgraded);

        }

    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (upgradeMap.size() > 0) {
            sb.append(MessageFormat.format(BUNDLE.getString("upgrade.oath.modules"), upgradeMap.size()));
            sb.append(delimiter);
            sb.append(MessageFormat.format(BUNDLE.getString("upgrade.oath.skipped"), skippedInRealm.size()));
            sb.append(delimiter);
        }
        return sb.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (upgradeMap.size() > 0) {
            sb.append(MessageFormat.format(BUNDLE.getString("upgrade.oath.modules"), upgradeMap.size()));
            sb.append(delimiter);
            for (Map.Entry<String, Integer> upgradeItem : numUpgradedMap.entrySet()) {
                sb.append(MessageFormat.format(BUNDLE.getString("upgrade.oath.realm"), upgradeItem.getKey()));
                sb.append(delimiter);
                sb.append(MessageFormat.format(BUNDLE.getString("upgrade.oath.users"), upgradeItem.getValue()));
                sb.append(delimiter);
            }
            sb.append(MessageFormat.format(BUNDLE.getString("upgrade.oath.skipped"), skippedInRealm.size()));
            sb.append(delimiter);
        }
        return sb.toString();
    }

    //OATH data for a realm - this step transitions these into the correct format to store in individual user's profiles
    private class OathData {

        private String key, counter, time;
        private int truncation = 0; //sensible defaults
        private boolean checksum = false;
        private String skippable = "oath2faEnabled";

        public void setSecretKeyAttribute(String key) { this.key = key; }
        public void setCounterAttribute(String counter) { this.counter = counter; }
        public void setTimeAttribute(String time) { this.time = time; }
        public void setTruncationValue(int truncation) { this.truncation = truncation; }
        public void setChecksum(boolean checksum) { this.checksum = checksum; }
        public void setSkippable(String skippable) { this.skippable = skippable; }

        public String getKey() { return key; }
        public String getCounter() { return counter; }
        public String getTime() { return time; }
        public int getTruncationValue() { return truncation; }
        public boolean getChecksum() { return checksum; }
        public String getSkippable() { return skippable; }

        public boolean isUsable() { return key != null && (counter != null || time != null); }
    }
}
