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
* Copyright 2015-2016 ForgeRock AS.
* Portions copyright 2025 3A Systems LLC.
*/
package org.forgerock.openam.core.rest.devices.services.oath;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import jakarta.annotation.Nonnull;
import org.forgerock.openam.core.rest.devices.DeviceSerialisation;
import org.forgerock.openam.core.rest.devices.services.DeviceService;
import org.forgerock.openam.core.rest.devices.services.EncryptedDeviceStorage;
import org.forgerock.util.Reject;

/**
 * Implementation of the OATH Service. Provides all necessary configuration information
 * at a realm-wide level to OATH authentication modules underneath it.
 */
public class AuthenticatorOathService extends EncryptedDeviceStorage implements DeviceService {

    /** Name of this service for reference purposes. */
    public static final String SERVICE_NAME = "AuthenticatorOATH";
    /** Version of this service. */
    public static final String SERVICE_VERSION = "1.0";

    /** Value is not set in config. */
    public static final int NOT_SET = 0;
    /** Value is set to allow skipping. */
    public static final int SKIPPABLE = 1;
    /** Value is set to not allow skipping. */
    public static final int NOT_SKIPPABLE = 2;

    private static final String DEBUG_LOCATION = "amAuthAuthenticatorOATH";

    private static final String OATH_ATTRIBUTE_NAME = "iplanet-am-auth-authenticator-oath-attr-name";
    private static final String OATH_ENCRYPTION_SCHEME =
            "openam-auth-authenticator-oath-device-settings-encryption-scheme";
    private static final String OATH_KEYSTORE_FILE =
            "openam-auth-authenticator-oath-device-settings-encryption-keystore";
    private static final String OATH_KEYSTORE_TYPE =
            "openam-auth-authenticator-oath-device-settings-encryption-keystore-type";
    private static final String OATH_KEYSTORE_PASSWORD =
            "openam-auth-authenticator-oath-device-settings-encryption-keystore-password";
    private static final String OATH_KEYSTORE_KEYPAIR_ALIAS =
            "openam-auth-authenticator-oath-device-settings-encryption-keypair-alias";
    private static final String OATH_KEYSTORE_PRIVATEKEY_PASSWORD =
            "openam-auth-authenticator-oath-device-settings-encryption-privatekey-password";
    private static final String OATH_SKIPPABLE_ATTRIBUTE_NAME =
            "iplanet-am-auth-authenticator-oath-skippable-name";

    /**
     * Basic constructor for the AuthenticatorOathService.
     *
     * @param configManager For communicating with the config datastore with listeners.
     * @param realm The realm in which this service instance exists.
     * @throws SMSException If we cannot talk to the config service.
     * @throws SSOException If we do not have correct permissions.
     */
    public AuthenticatorOathService(ServiceConfigManager configManager, String realm)
            throws SMSException, SSOException {
        super(configManager, realm, DEBUG_LOCATION);
    }

    @Override
    public String getConfigStorageAttributeName() {
        return CollectionHelper.getMapAttr(options, OATH_ATTRIBUTE_NAME);
    }

    @Override
    public DeviceSerialisation getDeviceSerialisationStrategy() {
        return getDeviceSerialisationStrategy(OATH_ENCRYPTION_SCHEME, OATH_KEYSTORE_FILE, OATH_KEYSTORE_PASSWORD,
                OATH_KEYSTORE_TYPE, OATH_KEYSTORE_KEYPAIR_ALIAS, OATH_KEYSTORE_PRIVATEKEY_PASSWORD);
    }

    /**
     * Returns the skippable attribute name for this service.
     *
     * @return The skippable attribute name.
     */
    public String getSkippableAttributeName() {
        return CollectionHelper.getMapAttr(options, OATH_SKIPPABLE_ATTRIBUTE_NAME);
    }

    /**
     * Sets the user's ability to skip an Authenticator OATH module (or any module configured to look at the
     * supplied attrName for its skippable value).
     *
     * @param id User's identity.
     * @param userSkipOath Whether or not to skip.
     * @throws IdRepoException If there were troubles talking to the IdRepo.
     * @throws SSOException If there were issues setting values on the provided ID.
     */
    public void setUserSkipOath(@Nonnull AMIdentity id, int userSkipOath)
            throws IdRepoException, SSOException {
        Reject.ifNull(id);
        final HashMap<String, Set<String>> attributesToWrite = new HashMap<>();
        attributesToWrite.put(getSkippableAttributeName(), Collections.singleton(String.valueOf(userSkipOath)));
        id.setAttributes(attributesToWrite);
        id.store();
    }

    /**
     * Removes all user's current devices of this type.
     *
     * @param id User's identity.
     * @throws IdRepoException If there were troubles talking to the IdRepo.
     * @throws SSOException If there were issues setting values on the provided ID.
     */
    public void removeAllUserDevices(@Nonnull AMIdentity id) throws IdRepoException, SSOException {
        Reject.ifNull(id);
        id.removeAttributes(Collections.singleton(getConfigStorageAttributeName()));
        id.store();
    }

}
