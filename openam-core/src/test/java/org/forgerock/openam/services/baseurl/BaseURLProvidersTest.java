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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.services.baseurl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.utils.OpenAMSettings;
import org.testng.annotations.Test;

/**
 * Tests which base URL providers are read as naming an origin of this server, the question the goto URL, RelayState
 * and wreply validators ask before they accept a redirect back to OpenAM itself.
 */
public class BaseURLProvidersTest {

    private static final String EXTENSION_TYPE = "base-url-extension-class";

    @Test
    public void shouldReadAFixedBaseUrlAsConfiguredRatherThanRequestDerived() {
        assertThat(BaseURLProviders.isRequestDerived(new FixedBaseURLProvider())).isFalse();
    }

    @Test
    public void shouldReadTheHeaderAndRequestProvidersAsRequestDerived() {
        assertThat(BaseURLProviders.isRequestDerived(new XForwardedHeadersBaseURLProvider())).isTrue();
        assertThat(BaseURLProviders.isRequestDerived(new ForwardedHeaderBaseURLProvider())).isTrue();
        assertThat(BaseURLProviders.isRequestDerived(new RequestValuesBaseURLProvider())).isTrue();
    }

    @Test
    public void shouldReadAnExtensionByWhatItWraps() throws Exception {
        // base-url-extension-class takes any BaseURLProvider, the header based ones included, and nothing in
        // amBaseURL.xml stops it. Deciding by the wrapper would let X-Forwarded-Host name an origin of this server.
        assertThat(BaseURLProviders.isRequestDerived(extensionWrapping(XForwardedHeadersBaseURLProvider.class)))
                .isTrue();
        assertThat(BaseURLProviders.isRequestDerived(extensionWrapping(ForwardedHeaderBaseURLProvider.class)))
                .isTrue();
        assertThat(BaseURLProviders.isRequestDerived(extensionWrapping(RequestValuesBaseURLProvider.class))).isTrue();
        // An extension class of its own is taken at its word: there is no telling from here what it reads.
        assertThat(BaseURLProviders.isRequestDerived(extensionWrapping(FixedOrigin.class))).isFalse();
    }

    @Test
    public void shouldTrustNothingThereIsNoProviderBehind() {
        // An extension that was never initialised has no delegate to judge by.
        assertThat(BaseURLProviders.isRequestDerived(new ExtensionBaseURLProvider())).isTrue();
        assertThat(BaseURLProviders.isRequestDerived(null)).isTrue();
    }

    @Test(timeOut = 10000)
    public void shouldGiveUpOnExtensionsWrappingOneAnotherRoundInACircle() {
        // Unwrapping follows the chain to the provider that decides, and a chain that never reaches one may not
        // spin the request thread: this runs inside goto, RelayState and wreply validation.
        Wrapper first = new Wrapper();
        Wrapper second = new Wrapper();
        first.wrapped = second;
        second.wrapped = first;

        assertThat(BaseURLProviders.isRequestDerived(first)).isTrue();
    }

    /**
     * An extension wrapper whose delegate the test sets, so a chain can be built without an injector behind it.
     */
    private static final class Wrapper extends ExtensionBaseURLProvider {

        private BaseURLProvider wrapped;

        @Override
        BaseURLProvider getDelegate() {
            return wrapped;
        }
    }

    private static ExtensionBaseURLProvider extensionWrapping(Class<? extends BaseURLProvider> extension)
            throws Exception {
        OpenAMSettings settings = mock(OpenAMSettings.class);
        when(settings.getStringSetting("realm", EXTENSION_TYPE)).thenReturn(extension.getName());
        ExtensionBaseURLProvider provider = new ExtensionBaseURLProvider();
        provider.init(settings, "realm");
        return provider;
    }

    /**
     * The shape of an extension a deployment writes to publish an origin of its own.
     */
    public static class FixedOrigin extends BaseURLProvider {

        @Override
        protected String getBaseURL(HttpServletRequest request) {
            return "https://sso.example.com";
        }

        @Override
        protected String getBaseURL(HttpContext context) {
            return "https://sso.example.com";
        }

        @Override
        void init(OpenAMSettings settings, String realm) {
        }
    }
}
