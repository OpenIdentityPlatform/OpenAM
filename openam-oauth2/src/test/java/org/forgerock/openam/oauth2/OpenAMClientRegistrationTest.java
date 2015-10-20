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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.oauth2;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.oauth2.core.OAuth2Constants.OAuth2Client.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.mockito.Mockito.*;

import com.sun.identity.idm.AMIdentity;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.PEMDecoder;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class OpenAMClientRegistrationTest {

    @Mock
    private AMIdentity amIdentity;
    private OpenAMClientRegistration clientRegistration;
    @Mock
    private OpenIdResolverService resolver;
    @Mock
    private OAuth2ProviderSettings providerSettings;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        ClientAuthenticationFailureFactory failureFactory = mock(ClientAuthenticationFailureFactory.class);
        clientRegistration = new OpenAMClientRegistration(amIdentity, new PEMDecoder(), resolver, providerSettings, failureFactory);
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
        List<String[]> splitStrings = clientRegistration.splitPipeDelimited(strings, "").get("");

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
                "[2]=scope2|fr|En Français",
                "[3]=scope3|en|Default English",
                "[4]=scope3|en_GB|British, innit",
                "[5]=scope3|en_US|American y'all",
                "[6]=scope4",
                "[7]=scope4|en|English only",
                "[8]=scope5|Default with overridden French exclusion",
                "[9]=scope5|fr|",
                "[10]=scope6|en|Included in English",
                "[11]=scope6|"
        ));

        when(providerSettings.getSupportedScopesWithTranslations()).thenReturn(asSet(
                "scope1",
                "scope2",
                "scope2|fr_FR|Pas en Français",
                "scope3",
                "scope4",
                "scope5",
                "scope6",
                "scope7",
                "scope8",
                "scope8|fr|Aussi en Français",
                "scope8|en|In English"
        ));

        // When
        Map<String, String> french = clientRegistration.getScopeDescriptions(Locale.FRANCE);
        Map<String, String> english = clientRegistration.getScopeDescriptions(Locale.UK);

        // Then
        assertThat(french).containsOnly(
                entry("scope1", "scope1"),
                entry("scope2", "En Français"),
                entry("scope3", "scope3"),
                entry("scope4", "scope4"),
                entry("scope7", "scope7"),
                entry("scope8", "Aussi en Français"));
        assertThat(english).containsOnly(
                entry("scope1", "scope1"),
                entry("scope2", "Default"),
                entry("scope3", "British, innit"),
                entry("scope4", "English only"),
                entry("scope5", "Default with overridden French exclusion"),
                entry("scope6", "Included in English"),
                entry("scope7", "scope7"),
                entry("scope8", "In English")
        );
    }

}