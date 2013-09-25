/**
 * Copyright 2013 ForgeRock, AS.
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
package org.forgerock.openam.cts.utils;

import org.forgerock.openam.cts.TokenTestUtils;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.AttributeDescription;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.testng.annotations.Test;

import java.util.Calendar;

import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

/**
 * @author robert.wapshott@forgerock.com
 */
public class TokenAttributeConversionTest {
    @Test
    public void shouldProduceDNWithTokenId() {
        // Given
        String tokenId = "badger";
        LDAPConfig constants = mock(LDAPConfig.class);
        given(constants.getTokenStoreRootSuffix()).willReturn(DN.rootDN());
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        TokenAttributeConversion conversion = new TokenAttributeConversion(constants, dataConversion);

        // When
        DN dn = conversion.generateTokenDN(tokenId);

        // Then
        verify(constants).getTokenStoreRootSuffix();
        assertTrue(dn.toString().contains(tokenId));
    }

    @Test
    public void shouldNotStripObjectClassIfNotPresent() {
        // Given
        Entry entry = mock(Entry.class);
        given(entry.getAttribute(anyString())).willReturn(null);

        // When
        TokenAttributeConversion.stripObjectClass(entry);

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
        TokenAttributeConversion.stripObjectClass(entry);

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
        TokenAttributeConversion.addObjectClass(entry);

        // Then
        verify(entry, times(0)).addAttribute(anyString(), any());
    }

    @Test
    public void shouldAddObjectClass() {
        // Given
        Entry entry = mock(Entry.class);
        given(entry.getAttribute(anyString())).willReturn(null);

        // When
        TokenAttributeConversion.addObjectClass(entry);

        // Then
        verify(entry).addAttribute(anyString(), any());
    }

    @Test
    public void shouldConvertTokenToEntryAndBack() {
        // Given
        TokenAttributeConversion conversion = generateTokenAttributeConversion();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(LDAPDataConversionTest.BERLIN);
        calendar.setTimeInMillis(System.currentTimeMillis());

        Token token = new Token("badger", TokenType.SESSION);
        token.setAttribute(CoreTokenField.STRING_ONE, "Ferret");
        token.setAttribute(CoreTokenField.STRING_TWO, "Weasel");
        token.setAttribute(CoreTokenField.INTEGER_ONE, 1234);
        token.setAttribute(CoreTokenField.DATE_ONE, calendar);

        // When
        Entry entry = conversion.getEntry(token);
        Token result = conversion.tokenFromEntry(entry);

        // Then
        TokenTestUtils.compareTokens(token, result);
    }

    @Test
    public void shouldHandleEmptyStrings() {
        // Given
        Token token = new Token("id", TokenType.OAUTH);
        token.setAttribute(CoreTokenField.STRING_ONE, "");

        TokenAttributeConversion conversion = generateTokenAttributeConversion();

        // When
        Entry result = conversion.getEntry(token);

        // Then
        Attribute attribute = result.getAttribute(CoreTokenField.STRING_ONE.toString());
        String string = attribute.firstValue().toString();
        assertFalse(string.isEmpty());
    }

    @Test
    public void shouldUnderstandEmptyStrings() {
        // Given
        Entry entry = new LinkedHashMapEntry();
        entry.addAttribute(CoreTokenField.TOKEN_ID.toString(), "id");
        entry.addAttribute(CoreTokenField.TOKEN_TYPE.toString(), TokenType.OAUTH.toString());
        entry.addAttribute(CoreTokenField.STRING_ONE.toString(), TokenAttributeConversion.EMPTY);

        TokenAttributeConversion conversion = generateTokenAttributeConversion();

        // When
        Token result = conversion.tokenFromEntry(entry);

        // Then
        String string = result.getValue(CoreTokenField.STRING_ONE);
        assertTrue(string.isEmpty());
    }

    @Test
    public void shouldAllowPlusSignInDN() {
        // Given
        TokenAttributeConversion conversion = generateTokenAttributeConversion();
        // When / Then
        conversion.generateTokenDN("Badger+");
    }

    private TokenAttributeConversion generateTokenAttributeConversion() {
        LDAPConfig constants = new LDAPConfig("dn=rootDN");
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        return new TokenAttributeConversion(constants, dataConversion);
    }
}
