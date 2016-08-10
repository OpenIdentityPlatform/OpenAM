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
package org.forgerock.openam.test.apidescriptor;

import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.TRANSLATION_KEY_PREFIX;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.assertj.core.api.Assertions;

/**
 * Utility methods for API annotation tests.
 *
 * @since 14.0.0
 */
final class ApiAssertions {

    private static final Map<String, Properties> resourceBundleCache = new HashMap<>();

    private ApiAssertions() {
        // Utils class only
    }

    static void assertI18nTitle(String title, Class<?> annotatedClass) {
        if (!title.startsWith(TRANSLATION_KEY_PREFIX)) {
            fail("Title \"" + title + "\" must start with \"" + TRANSLATION_KEY_PREFIX + "\"");
        }
        assertThatResourceBundleEntryExists(title, annotatedClass);
    }

    static void assertI18nDescription(String description, Class<?> annotatedClass) {
        if (!description.startsWith(TRANSLATION_KEY_PREFIX)) {
            fail("Description \"" + description + "\" must start with \"" + TRANSLATION_KEY_PREFIX + "\"");
        }
        assertThatResourceBundleEntryExists(description, annotatedClass);
    }

    private static void assertThatResourceBundleEntryExists(String i18nKey, Class<?> annotatedClass) {
        URI resource = getKeyAsUri(i18nKey);
        if (resource != null) {
            String resourceBundleName = "/" + resource.getSchemeSpecificPart() + ".properties";
            String propertyName = resource.getFragment();
            Properties properties = getResourceBundle(resourceBundleName, annotatedClass);
            Assertions.assertThat(properties)
                    .withFailMessage("No entry found for \"" + propertyName + "\" in " + resourceBundleName)
                    .containsKey(propertyName);
        }
    }

    private static Properties getResourceBundle(String resourceBundleName, Class<?> annotatedClass) {
        Properties properties;
        if (!resourceBundleCache.containsKey(resourceBundleName)) {
            properties = new Properties();
            try {
                properties.load(annotatedClass.getResourceAsStream(resourceBundleName));
                resourceBundleCache.put(resourceBundleName, properties);
            } catch (IOException e) {
                fail("No resource bundle found at " + resourceBundleName, e);
            }
        }
        return resourceBundleCache.get(resourceBundleName);
    }

    private static URI getKeyAsUri(String i18nKey) {
        try {
            return new URI(i18nKey);
        } catch (URISyntaxException e) {
            fail("Title or description I18N key is not a URI", e);
        }
        return null;
    }
}
