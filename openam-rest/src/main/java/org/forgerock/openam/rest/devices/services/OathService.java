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
package org.forgerock.openam.rest.devices.services;

import com.iplanet.sso.SSOException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.openam.rest.devices.DeviceSerialisation;
import org.forgerock.openam.rest.devices.EncryptedJwtDeviceSerialisation;
import org.forgerock.openam.rest.devices.JsonDeviceSerialisation;
import org.forgerock.openam.shared.security.crypto.KeyStoreBuilder;
import org.forgerock.openam.shared.security.crypto.KeyStoreType;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the OATH Service. Provides all necessary configuration information
 * at a realm-wide level to OATH authentication modules underneath it.
 */
public class OathService implements DeviceService {

    final static public String SERVICE_NAME = "OATH";
    final static public String SERVICE_VERSION = "1.0";

    final static private Debug debug = Debug.getInstance("amAuthOATH");

    public static final String OATH_ATTRIBUTE_NAME = "iplanet-am-auth-oath-attr-name";
    private static final String OATH_ENCRYPTION_SCHEME = "openam-auth-oath-device-settings-encryption-scheme";
    private static final String OATH_KEYSTORE_FILE = "openam-auth-oath-device-settings-encryption-keystore";
    private static final String OATH_KEYSTORE_TYPE = "openam-auth-oath-device-settings-encryption-keystore-type";
    private static final String OATH_KEYSTORE_PASSWORD =
            "openam-auth-oath-device-settings-encryption-keystore-password";
    private static final String OATH_KEYSTORE_KEYPAIR_ALIAS =
            "openam-auth-oath-device-settings-encryption-keypair-alias";
    private static final String OATH_KEYSTORE_PRIVATEKEY_PASSWORD =
            "openam-auth-oath-device-settings-encryption-privatekey-password";

    private Map<String, Set<String>> options;

    public OathService(String realm) throws SMSException, SSOException {
        try {
            ServiceConfigManager mgr = new ServiceConfigManager(
                    AccessController.doPrivileged(AdminTokenAction.getInstance()), SERVICE_NAME, SERVICE_VERSION);
            ServiceConfig scm = mgr.getOrganizationConfig(realm, null);
            options = scm.getAttributes();
        } catch (SMSException | SSOException e) {
            if (debug.errorEnabled()) {
                debug.error("Error connecting to SMS to retrieve config for OathService.", e);
            }
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConfigStorageAttributeName() {
        return CollectionHelper.getMapAttr(options, OATH_ATTRIBUTE_NAME);
    }

    @Override
    public DeviceSerialisation getDeviceSerialisationStrategy() {
        final SupportedOathEncryptionScheme encryptionScheme =
                SupportedOathEncryptionScheme.valueOf(CollectionHelper.getMapAttr(options, OATH_ENCRYPTION_SCHEME,
                SupportedOathEncryptionScheme.NONE.toString()));

        if (encryptionScheme == null || encryptionScheme == SupportedOathEncryptionScheme.NONE) {
            return new JsonDeviceSerialisation();
        } else {
            return new EncryptedJwtDeviceSerialisation(encryptionScheme.encryptionMethod,
                    encryptionScheme.jweAlgorithm, getEncryptionKeyPair());
        }
    }

    private KeyPair getEncryptionKeyPair() {
        try {
            final KeyStore keyStore = new KeyStoreBuilder()
                    .withKeyStoreFile(new File(CollectionHelper.getMapAttr(options, OATH_KEYSTORE_FILE)))
                    .withPassword(CollectionHelper.getMapAttr(options, OATH_KEYSTORE_PASSWORD))
                    .withKeyStoreType(KeyStoreType.valueOf(CollectionHelper.getMapAttr(options, OATH_KEYSTORE_TYPE)))
                    .build();

            final Certificate cert = keyStore.getCertificate(
                    CollectionHelper.getMapAttr(options, OATH_KEYSTORE_KEYPAIR_ALIAS));
            final PublicKey publicKey = cert.getPublicKey();
            final PrivateKey privateKey = (PrivateKey) keyStore.getKey(
                    CollectionHelper.getMapAttr(options, OATH_KEYSTORE_KEYPAIR_ALIAS),
                    CollectionHelper.getMapAttr(options, OATH_KEYSTORE_PRIVATEKEY_PASSWORD).toCharArray());

            return new KeyPair(publicKey, privateKey);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Invalid keystore location specified", e);
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            debug.error("OathService.getEncryptionKeyPair(): Unable to load encryption key pair", e);
            throw new IllegalStateException(e);
        }
    }

    private enum SupportedOathEncryptionScheme {
        NONE(null, null),
        RSAES_AES256CBC_HS512(EncryptionMethod.A256CBC_HS512, JweAlgorithm.RSAES_PKCS1_V1_5),
        RSAES_AES128CBC_HS256(EncryptionMethod.A128CBC_HS256, JweAlgorithm.RSAES_PKCS1_V1_5);

        private final EncryptionMethod encryptionMethod;
        private final JweAlgorithm jweAlgorithm;

        SupportedOathEncryptionScheme(final EncryptionMethod encryptionMethod, final JweAlgorithm jweAlgorithm) {
            this.encryptionMethod = encryptionMethod;
            this.jweAlgorithm = jweAlgorithm;
        }
    }
}
