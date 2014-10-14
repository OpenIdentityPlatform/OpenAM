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

package org.forgerock.openam.oauth2;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.MapValueParser;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.jose.utils.KeystoreManager;
import org.forgerock.oauth2.core.OAuth2Constants;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.Set;

/**
 * Provides access to any OpenAM settings.
 *
 * @since 12.0.0
 */
public class OpenAMSettingsImpl implements OpenAMSettings {

    private final static String DEFAULT_KEYSTORE_FILE_PROP = "com.sun.identity.saml.xmlsig.keystore";
    private final static String DEFAULT_KEYSTORE_PASS_FILE_PROP = "com.sun.identity.saml.xmlsig.storepass";
    private final static String DEFAULT_KEYSTORE_TYPE_PROP = "com.sun.identity.saml.xmlsig.storetype";
    private final static String DEFAULT_PRIVATE_KEY_PASS_FILE_PROP  = "com.sun.identity.saml.xmlsig.keypass";

    private static final MapValueParser MAP_VALUE_PARSER = new MapValueParser();

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final String serviceName;
    private final String serviceVersion;

    /**
     * Constructs a new OpenAMSettingsImpl.
     *
     * @param serviceName The service name.
     * @param serviceVersion The service version.
     */
    @Inject
    public OpenAMSettingsImpl(@Assisted("serviceName") String serviceName,
            @Assisted("serviceVersion") String serviceVersion) {
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getSetting(String realm, String attributeName) throws SSOException, SMSException {
        final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        final ServiceConfigManager serviceConfigManager = new ServiceConfigManager(token, serviceName, serviceVersion);
        final ServiceConfig serviceConfig = serviceConfigManager.getOrganizationConfig(realm, null);

        final Map<String, Set<String>> attributes = serviceConfig.getAttributes();
        return attributes.get(attributeName);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringSetting(String realm, String attributeName) throws SSOException, SMSException {
        final Set<String> attribute = getSetting(realm, attributeName);
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        return attribute.iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    public Long getLongSetting(String realm, String attributeName) throws SSOException, SMSException {
        return Long.decode(getStringSetting(realm, attributeName));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean getBooleanSetting(String realm, String attributeName) throws SSOException, SMSException {
        return Boolean.valueOf(getStringSetting(realm, attributeName));
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getMapSetting(String realm, String attributeName) throws SSOException, SMSException {
        return MAP_VALUE_PARSER.parse(getSetting(realm, attributeName));
    }

    /**
     * {@inheritDoc}
     */
    public KeyPair getServerKeyPair(String realm) throws SMSException, SSOException {
        final String alias = getStringSetting(realm, OAuth2Constants.OAuth2ProviderService.KEYSTORE_ALIAS);

        //get keystore password from file
        final String kspfile = SystemPropertiesManager.get(DEFAULT_KEYSTORE_PASS_FILE_PROP);
        String keystorePass = null;
        if (kspfile != null) {
            try {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(kspfile)));
                    keystorePass = decodePassword(br.readLine());
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            } catch (IOException e) {
                logger.error("Unable to read keystore password file " + kspfile, e);
            }
        } else {
            logger.error("keystore password is null");
        }

        final String keypassfile = SystemPropertiesManager.get(DEFAULT_PRIVATE_KEY_PASS_FILE_PROP);
        String keypass = null;
        if (keypassfile != null) {
            try {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(keypassfile)));
                    keypass = decodePassword(br.readLine());
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            } catch (IOException e) {
                logger.error("Unable to read key password file " + keypassfile, e);
            }
        } else {
            logger.error("key password is null");
        }

        final KeystoreManager keystoreManager = new KeystoreManager(
                SystemPropertiesManager.get(DEFAULT_KEYSTORE_TYPE_PROP, "JKS"),
                SystemPropertiesManager.get(DEFAULT_KEYSTORE_FILE_PROP), keystorePass);

        final PrivateKey privateKey = keystoreManager.getPrivateKey(alias, keypass);
        final PublicKey publicKey = keystoreManager.getPublicKey(alias);
        return new KeyPair(publicKey, privateKey);
    }

    private String decodePassword(String password)  {
        final String decodedPassword = AccessController.doPrivileged(new DecodeAction(password));
        return decodedPassword == null ? password : decodedPassword;
    }

    /**
     * {@inheritDoc}
     */
    public String getSSOCookieName() {
        return SystemProperties.get("com.iplanet.am.cookie.name");
    }
}
