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
* Portions copyright 2025 3A Systems LLC.
*/
package org.forgerock.openam.core.rest.devices.services.push;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import java.util.Collections;
import jakarta.annotation.Nonnull;
import org.forgerock.openam.core.rest.devices.DeviceSerialisation;
import org.forgerock.openam.core.rest.devices.services.DeviceService;
import org.forgerock.openam.core.rest.devices.services.EncryptedDeviceStorage;
import org.forgerock.util.Reject;

/**
 * Implementation of the Push Service. Provides all necessary configuration information
 * at a realm-wide level to Push authentication modules underneath it.
 */
public class AuthenticatorPushService extends EncryptedDeviceStorage implements DeviceService {

    /** Name of this service for reference purposes. */
    public static final String SERVICE_NAME = "AuthenticatorPush";
    /** Version of this service. */
    public static final String SERVICE_VERSION = "1.0";

    private static final String DEBUG_LOCATION = "amAuthAuthenticatorPush";

    private static final String PUSH_ATTRIBUTE_NAME = "iplanet-am-auth-authenticator-push-attr-name";
    private static final String PUSH_ENCRYPTION_SCHEME =
            "openam-auth-authenticator-push-device-settings-encryption-scheme";
    private static final String PUSH_KEYSTORE_FILE =
            "openam-auth-authenticator-push-device-settings-encryption-keystore";
    private static final String PUSH_KEYSTORE_TYPE =
            "openam-auth-authenticator-push-device-settings-encryption-keystore-type";
    private static final String PUSH_KEYSTORE_PASSWORD =
            "openam-auth-authenticator-push-device-settings-encryption-keystore-password";
    private static final String PUSH_KEYSTORE_KEYPAIR_ALIAS =
            "openam-auth-authenticator-push-device-settings-encryption-keypair-alias";
    private static final String PUSH_KEYSTORE_PRIVATEKEY_PASSWORD =
            "openam-auth-authenticator-push-device-settings-encryption-privatekey-password";

    /**
     * Generates a new AuthenticatorPushStorage, used to encrypt and store device settings on a user's
     * profile under a config storage attribute provided by the service looked up on instance creation.
     *
     * @param serviceConfigManager Used to communicate with the config store.
     * @param realm The realm in which to look up the AuthenticatorPush service.
     * @throws SMSException If there were error communicating with the SMS.
     * @throws SSOException If there were invalid privileges to perform the requested operation.
     */
    public AuthenticatorPushService(ServiceConfigManager serviceConfigManager, String realm)
            throws SMSException, SSOException {
        super(serviceConfigManager, realm, DEBUG_LOCATION);
    }

    @Override
    public String getConfigStorageAttributeName() {
        return CollectionHelper.getMapAttr(options, PUSH_ATTRIBUTE_NAME);
    }

    @Override
    public DeviceSerialisation getDeviceSerialisationStrategy() {
        return getDeviceSerialisationStrategy(PUSH_ENCRYPTION_SCHEME, PUSH_KEYSTORE_FILE, PUSH_KEYSTORE_PASSWORD,
                PUSH_KEYSTORE_TYPE, PUSH_KEYSTORE_KEYPAIR_ALIAS, PUSH_KEYSTORE_PRIVATEKEY_PASSWORD);
    }

    /**
     * Remove all user devices in a single call.
     * @param id User ID from which to remove all devices.
     * @throws IdRepoException If there were error communicating with the user's profile.
     * @throws SSOException If there were invalid privileges to perform the requested operation.
     */
    public void removeAllUserDevices(@Nonnull AMIdentity id) throws IdRepoException, SSOException {
        Reject.ifNull(id);
        id.removeAttributes(Collections.singleton(getConfigStorageAttributeName()));
        id.store();
    }

}
