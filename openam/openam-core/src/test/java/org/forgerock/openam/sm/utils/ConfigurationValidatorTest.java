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
package org.forgerock.openam.sm.utils;

import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.exceptions.InvalidConfigurationException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.testng.AssertJUnit.fail;

public class ConfigurationValidatorTest {

    private ConnectionConfig mockConfig;
    private LDAPURL ldapurl;
    private ConfigurationValidator validator;

    @BeforeMethod
    public void setup() {
        mockConfig = mock(ConnectionConfig.class);
        ldapurl = LDAPURL.valueOf("badger", 1234);
        given(mockConfig.getLDAPURLs()).willReturn(new HashSet<LDAPURL>(Arrays.asList(ldapurl)));
        given(mockConfig.getBindDN()).willReturn("badger");
        given(mockConfig.getBindPassword()).willReturn("badger".toCharArray());
        given(mockConfig.getMaxConnections()).willReturn(10);

        validator = new ConfigurationValidator();
    }

    @Test
    public void shouldDemonstrateMockConfigIsValid() throws InvalidConfigurationException {
        validator.validate(mockConfig);
    }

    @Test
    public void shouldEnsureLDAPURLIsPresent() {
        given(mockConfig.getLDAPURLs()).willReturn(null);
        try {
            validator.validate(mockConfig);
            fail();
        } catch (InvalidConfigurationException e) {
        }
    }

    @Test
    public void shouldEnsurePortNumberIsInValid() {
        ldapurl = LDAPURL.valueOf("badger", -1);
        given(mockConfig.getLDAPURLs()).willReturn(new HashSet<LDAPURL>(Arrays.asList(ldapurl)));
        try {
            validator.validate(mockConfig);
            fail();
        } catch (InvalidConfigurationException e) {
        }
    }

    @Test
    public void shouldEnsureBindDNIsNotNull() {
        given(mockConfig.getBindDN()).willReturn(null);
        try {
            validator.validate(mockConfig);
            fail();
        } catch (InvalidConfigurationException e) {
        }
    }

    @Test
    public void shouldEnsureBindPasswordIsNotNull() {
        given(mockConfig.getBindPassword()).willReturn(null);
        try {
            validator.validate(mockConfig);
            fail();
        } catch (InvalidConfigurationException e) {
        }
    }
}