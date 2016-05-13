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
*/
package org.forgerock.openam.core.rest.devices.services;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.openam.core.rest.devices.DeviceSerialisation;
import org.forgerock.openam.core.rest.devices.EncryptedJwtDeviceSerialisation;
import org.forgerock.openam.core.rest.devices.JsonDeviceSerialisation;
import org.forgerock.openam.shared.security.crypto.KeyStoreBuilder;
import org.forgerock.openam.shared.security.crypto.KeyStoreType;

/**
 * Abstract class providing configuration for the encryption of stored device information.
 */
public abstract class EncryptedDeviceStorage {

    final private Debug debug;

    protected Map<String, Set<String>> options;

    /**
     * Generates a new EncryptedDeviceStorage for a given service.
     *
     * @param configManager Version of the service this class needs to talk to via the {@link ServiceConfigManager}
     * @param realm Realm in which the service exists.
     * @param debugLocation Debug file handle.
     * @throws SMSException In case of exceptions communicating with the SMS.
     * @throws SSOException In case there was an issue retrieving an valid access token to get to the service.
     */
    protected EncryptedDeviceStorage(ServiceConfigManager configManager, String realm, String debugLocation)
            throws SMSException, SSOException {

        debug = Debug.getInstance(debugLocation);

        try {
            ServiceConfig scm = configManager.getOrganizationConfig(realm, null);
            options = scm.getAttributes();
        } catch (SMSException | SSOException e) {
            if (debug.errorEnabled()) {
                debug.error("Error connecting to SMS to retrieve config for EncryptedDeviceStorage.", e);
            }
            throw e;
        }
    }

    private KeyPair getEncryptionKeyPair(String keystoreFile, String keystorePass, String keystoreType,
                                         String keyPairAlias, String privateKeyPass) {
        try {
            final KeyStore keyStore = new KeyStoreBuilder()
                    .withKeyStoreFile(new File(CollectionHelper.getMapAttr(options, keystoreFile)))
                    .withPassword(CollectionHelper.getMapAttr(options, keystorePass))
                    .withKeyStoreType(KeyStoreType.valueOf(CollectionHelper.getMapAttr(options, keystoreType)))
                    .build();

            final Certificate cert = keyStore.getCertificate(
                    CollectionHelper.getMapAttr(options, keyPairAlias));
            final PublicKey publicKey = cert.getPublicKey();
            final PrivateKey privateKey = (PrivateKey) keyStore.getKey(
                    CollectionHelper.getMapAttr(options, keyPairAlias),
                    CollectionHelper.getMapAttr(options, privateKeyPass).toCharArray());

            return new KeyPair(publicKey, privateKey);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Invalid keystore location specified", e);
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            debug.error("EncryptedDeviceStorage.getEncryptionKeyPair(): Unable to load encryption key pair", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a strategy which will be used to serialise a device correctly by defining its encryption parameters
     * (if encrypted) or otherwise indicating that it should be simply JSON encoded.
     *
     * @param scheme The key of the data in the options map to use.
     * @param keystoreFile Location of the keystore file.
     * @param keystorePass Password to the keystore file.
     * @param keystoreType Type of the keystore.
     * @param keyPairAlias Name of the keypair.
     * @param privateKeyPass Password used to retrieve the private key from the keypair.
     * @return an appropriate {@link DeviceSerialisation}.
     */
    protected DeviceSerialisation getDeviceSerialisationStrategy(String scheme, String keystoreFile, String keystorePass,
                                                       String keystoreType, String keyPairAlias,
                                                       String privateKeyPass) {
        final SupportedEncryptionScheme encryptionScheme =
                SupportedEncryptionScheme.valueOf(CollectionHelper.getMapAttr(options, scheme,
                        SupportedEncryptionScheme.NONE.toString()));

        if (SupportedEncryptionScheme.NONE == encryptionScheme) {
            return new JsonDeviceSerialisation();
        } else {
            return new EncryptedJwtDeviceSerialisation(encryptionScheme.encryptionMethod,
                    encryptionScheme.jweAlgorithm,
                    getEncryptionKeyPair(keystoreFile, keystorePass, keystoreType, keyPairAlias, privateKeyPass));
        }
    }

    /**
     * Encryption schemes that are currently supported to use to secure stored data.
     */
    protected enum SupportedEncryptionScheme {
        NONE(null, null),
        RSAES_AES256CBC_HS512(EncryptionMethod.A256CBC_HS512, JweAlgorithm.RSAES_PKCS1_V1_5),
        RSAES_AES128CBC_HS256(EncryptionMethod.A128CBC_HS256, JweAlgorithm.RSAES_PKCS1_V1_5);

        private final EncryptionMethod encryptionMethod;
        private final JweAlgorithm jweAlgorithm;

        SupportedEncryptionScheme(final EncryptionMethod encryptionMethod, final JweAlgorithm jweAlgorithm) {
            this.encryptionMethod = encryptionMethod;
            this.jweAlgorithm = jweAlgorithm;
        }
    }

}
