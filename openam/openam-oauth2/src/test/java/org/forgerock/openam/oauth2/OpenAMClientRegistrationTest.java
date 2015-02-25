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

import com.sun.identity.idm.AMIdentity;
import org.forgerock.oauth2.core.PEMDecoder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.*;
import static org.fest.assertions.MapAssert.*;
import static org.forgerock.oauth2.core.OAuth2Constants.OAuth2Client.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.mockito.Mockito.*;

public class OpenAMClientRegistrationTest {

    private AMIdentity amIdentity;
    private OpenAMClientRegistration clientRegistration;

    @BeforeMethod
    public void setup() throws Exception {
        amIdentity = mock(AMIdentity.class);
        clientRegistration = new OpenAMClientRegistration(amIdentity, new PEMDecoder());
    }

    @Test
    public void testGetDisplayName() throws Exception {
        // Given
        when(amIdentity.getAttribute(NAME)).thenReturn(asSet("[0]=Name1", "[2]=en|Name2", "[1]=en_GB|Name3"));

        // When
        String name = clientRegistration.getDisplayName(Locale.UK);

        // Then
        assertThat(name).isEqualTo("Name3");
    }

    @Test
    public void testGetDisplayDescription() throws Exception {
        // Given
        when(amIdentity.getAttribute(DESCRIPTION)).thenReturn(asSet("[0]=Desc1", "[1]=en|Desc2", "[2]=en_GB|Desc3"));

        // When
        String desc = clientRegistration.getDisplayDescription(Locale.UK);

        // Then
        assertThat(desc).isEqualTo("Desc3");
    }

    @DataProvider(name = "languageStrings")
    public Object[][] languageStrings() {
        return new Object[][] {
                { asSet("1"), Locale.ENGLISH, "1" },
                { asSet("1", "en|2", "en_US|3", "en_GB|4"), Locale.ENGLISH, "2" },
                { asSet("1", "en|2", "en_US|3", "en_GB|4"), Locale.UK, "4" },
                { asSet("en_US|3"), new Locale("en", "US", "WIN"), "3" }
        };
    }

    @Test(dataProvider = "languageStrings")
    public void testFindLocaleSpecificString(Set<String> strings, Locale locale, String expected) throws Exception {
        // Given
        Set<String[]> splitStrings = clientRegistration.splitPipeDelimited(strings);

        // When
        String result = clientRegistration.findLocaleSpecificString(splitStrings, locale);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @DataProvider(name = "locales")
    public Object[][] localesData() {
        return new Object[][] {
                { new Locale("en"), asList("en") },
                { new Locale("de", "DE"), asList("de_DE", "de") },
                { new Locale("", "GB"), asList("_GB") },
                { new Locale("de", "", "POSIX"), asList("de__POSIX", "de") }
        };
    }

    @Test(dataProvider = "locales")
    public void testLanguageStrings(Locale locale, List<String> expected) throws Exception {
        assertThat(clientRegistration.languageStrings(locale)).isEqualTo(expected);
    }

    @Test
    public void testGetScopeDescriptions() throws Exception {
        // Given
        when(amIdentity.getAttribute(DEFAULT_SCOPES)).thenReturn(asSet(
                "[0]=scope1",
                "[1]=scope2|Default",
                "[2]=scope2|fr|En français",
                "[3]=scope3|en|Default English",
                "[4]=scope3|en_GB|British, innit",
                "[5]=scope3|en_US|American y'all",
                "[6]=scope4",
                "[7]=scope4|en|English only"
        ));

        // When
        Map<String, String> french = clientRegistration.getScopeDescriptions(Locale.FRANCE);
        Map<String, String> english = clientRegistration.getScopeDescriptions(Locale.UK);

        // Then
        assertThat(french).includes(entry("scope2", "En français"));
        assertThat(french.size()).isEqualTo(1);
        assertThat(english).includes(entry("scope2", "Default"), entry("scope3", "British, innit"), entry("scope4", "English only"));
        assertThat(english.size()).isEqualTo(3);
    }

}