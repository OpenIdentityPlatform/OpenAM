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

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

import java.security.KeyPair;
import java.util.Map;
import java.util.Set;

/**
 * Provides access to any OpenAM settings.
 *
 * @since 12.0.0
 */
public interface OpenAMSettings {

    /**
     * Gets the raw {@code Set} of the values of the specified attribute name in the given realm.
     *
     * @param realm The realm.
     * @param attributeName The attribute name,.
     * @return The {@code Set} of values.
     * @throws SSOException If there is a problem getting the setting value.
     * @throws SMSException If there is a problem getting the setting value.
     */
    Set<String> getSetting(String realm, String attributeName) throws SSOException, SMSException;

    /**
     * Gets the raw {@code Set} of the values of the specified attribute name in the given realm and gets the first
     * value.
     *
     * @param realm The realm.
     * @param attributeName The attribute name,.
     * @return The {@code Set} of values.
     * @throws SSOException If there is a problem getting the setting value.
     * @throws SMSException If there is a problem getting the setting value.
     */
    String getStringSetting(String realm, String attributeName) throws SSOException, SMSException;

    /**
     * Gets the raw {@code Set} of the values of the specified attribute name in the given realm and gets the first
     * value and decodes it to a Long.
     *
     * @param realm The realm.
     * @param attributeName The attribute name,.
     * @return The {@code Set} of values.
     * @throws SSOException If there is a problem getting the setting value.
     * @throws SMSException If there is a problem getting the setting value.
     */
    Long getLongSetting(String realm, String attributeName) throws SSOException, SMSException;

    /**
     * Gets the raw {@code Set} of the values of the specified attribute name in the given realm and gets the first
     * value and parses it to a Boolean.
     *
     * @param realm The realm.
     * @param attributeName The attribute name,.
     * @return The {@code Set} of values.
     * @throws SSOException If there is a problem getting the setting value.
     * @throws SMSException If there is a problem getting the setting value.
     */
    Boolean getBooleanSetting(String realm, String attributeName) throws SSOException, SMSException;

    /**
     * Gets a setting from the given realm and parses it as into a Map from string keys to string values, according
     * to the {@link com.sun.identity.common.configuration.MapValueParser} format.
     *
     * @param realm the realm.
     * @param attributeName the attribute name.
     * @return The {@code Map} of values parsed from the attribute.
     * @throws SSOException If there is a problem getting the setting value.
     * @throws SMSException If there is a problem getting the setting value.
     */
    Map<String, String> getMapSetting(String realm, String attributeName) throws SSOException, SMSException;

    /**
     * Gets the key pair that OpenAM is configured to use for the specified realm.
     *
     * @param realm The realm.
     * @return The key pair.
     * @throws SSOException If there is a problem getting the setting value.
     * @throws SMSException If there is a problem getting the setting value.
     */
    KeyPair getServerKeyPair(String realm) throws SMSException, SSOException;

    /**
     * Gets the name of the SSO Cookie.
     *
     * @return The SSO Cookie name.
     */
    String getSSOCookieName();
}
