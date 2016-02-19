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
 */

package org.forgerock.openam.cts.utils;

import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Calendar;

import org.forgerock.openam.cts.TokenTestUtils;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.impl.CTSDataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.AttributeDescription;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.testng.annotations.Test;

public class LdapTokenAttributeConversionTest {
    @Test
    public void shouldProduceDNWithTokenId() {
        // Given
        String tokenId = "badger";
        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        given(config.getTokenStoreRootSuffix()).willReturn(DN.rootDN());
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);

        // When
        DN dn = conversion.generateTokenDN(tokenId);

        // Then
        verify(config).getTokenStoreRootSuffix();
        assertTrue(dn.toString().contains(tokenId));
    }

    @Test
    public void shouldNotStripObjectClassIfNotPresent() {
        // Given
        Entry entry = mock(Entry.class);
        given(entry.getAttribute(anyString())).willReturn(null);

        // When
        LdapTokenAttributeConversion.stripObjectClass(entry);

        // Then
        verify(entry, times(0)).removeAttribute(anyString(), any());
    }

    @Test
    public void shouldStripObjectClass() {
        // Given
        Entry entry = mock(Entry.class);
        Attribute attribute = mock(Attribute.class);
        given(entry.getAttribute(anyString())).willReturn(attribute);

        AttributeDescription description = AttributeDescription.valueOf("badger");
        given(attribute.getAttributeDescription()).willReturn(description);

        // When
        LdapTokenAttributeConversion.stripObjectClass(entry);

        // Then
        verify(entry).removeAttribute(description);
    }

    @Test
    public void shouldNotAddObjectClassIfPresent() {
        // Given
        Entry entry = mock(Entry.class);
        Attribute attribute = mock(Attribute.class);
        given(entry.getAttribute(anyString())).willReturn(attribute);

        // When
        LdapTokenAttributeConversion.addObjectClass(entry);

        // Then
        verify(entry, times(0)).addAttribute(anyString(), any());
    }

    @Test
    public void shouldAddObjectClass() {
        // Given
        Entry entry = mock(Entry.class);
        given(entry.getAttribute(anyString())).willReturn(null);

        // When
        LdapTokenAttributeConversion.addObjectClass(entry);

        // Then
        verify(entry).addAttribute(anyString(), any());
    }

    @Test
    public void shouldConvertTokenToEntryAndBack() {
        // Given
        LdapTokenAttributeConversion conversion = generateTokenAttributeConversion();

        Calendar calendar = getCalendarInstance();
        calendar.setTimeZone(LDAPDataConversionTest.BERLIN);
        calendar.setTimeInMillis(currentTimeMillis());

        Token token = new Token("badger", TokenType.SESSION);
        token.setAttribute(CoreTokenField.STRING_ONE, "Ferret");
        token.setAttribute(CoreTokenField.STRING_TWO, "Weasel");
        token.setAttribute(CoreTokenField.INTEGER_ONE, 1234);
        token.setAttribute(CoreTokenField.DATE_ONE, calendar);

        // When
        Entry entry = conversion.getEntry(token);
        Token result = conversion.tokenFromEntry(entry);

        // Then
        TokenTestUtils.assertTokenEquals(token, result);
    }

    @Test
    public void shouldHandleEmptyStrings() {
        // Given
        Token token = new Token("id", TokenType.OAUTH);
        token.setAttribute(CoreTokenField.STRING_ONE, "");

        LdapTokenAttributeConversion conversion = generateTokenAttributeConversion();

        // When
        Entry result = conversion.getEntry(token);

        // Then
        Attribute attribute = result.getAttribute(CoreTokenField.STRING_ONE.toString());
        assertNull(attribute);
    }

    @Test
    public void shouldUnderstandEmptyStrings() {
        // Given
        Entry entry = new LinkedHashMapEntry();
        entry.addAttribute(CoreTokenField.TOKEN_ID.toString(), "id");
        entry.addAttribute(CoreTokenField.TOKEN_TYPE.toString(), TokenType.OAUTH.toString());
        entry.addAttribute(CoreTokenField.STRING_ONE.toString(), LdapTokenAttributeConversion.EMPTY);

        LdapTokenAttributeConversion conversion = generateTokenAttributeConversion();

        // When
        Token result = conversion.tokenFromEntry(entry);

        // Then
        String string = result.getValue(CoreTokenField.STRING_ONE);
        assertTrue(string.isEmpty());
    }

    @Test
    public void shouldAllowPlusSignInDN() {
        // Given
        LdapTokenAttributeConversion conversion = generateTokenAttributeConversion();
        // When / Then
        conversion.generateTokenDN("Badger+");
    }

    private LdapTokenAttributeConversion generateTokenAttributeConversion() {
        LdapDataLayerConfiguration config = new CTSDataLayerConfiguration("ou=test-case");
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        return new LdapTokenAttributeConversion(dataConversion, config);
    }
}
