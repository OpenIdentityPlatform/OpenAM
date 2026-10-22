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
package org.forgerock.openam.authentication.modules.oauth2;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the URL host extraction used to name the identity provider on the OAuth2 logout page.
 */
public class OAuthUtilTest {

    @DataProvider(name = "urlsNamingAHost")
    public Object[][] urlsNamingAHost() {
        return new Object[][] {
            { "https://accounts.example.com/logout", "accounts.example.com" },
            { "https://accounts.example.com:8443/logout", "accounts.example.com" },
            { "https://accounts.example.com", "accounts.example.com" },
            { "https://user@accounts.example.com/logout", "accounts.example.com" },
            { "http://[::1]:8080/logout", "[::1]" },
            // java.net.URI will not parse an authority holding an underscore as server based, so getHost()
            // comes back null and the authority carries the name instead.
            { "https://openam_prod.example.com/logout", "openam_prod.example.com" },
            { "https://openam_prod.example.com:8443/logout", "openam_prod.example.com" },
        };
    }

    @Test(dataProvider = "urlsNamingAHost")
    public void shouldReturnTheHostNamedByTheUrl(String url, String expected) {
        assertEquals(OAuthUtil.getUrlHost(url), expected);
    }

    @DataProvider(name = "urlsNamingNoHost")
    public Object[][] urlsNamingNoHost() {
        return new Object[][] {
            { null },
            { "" },
            { "/openam/UI/Logout" },
            // The defect this replaces: indexOf("//") read the "//" of an empty path segment as the start of
            // an authority and named "x" as the identity provider.
            { "/openam/oauth2/logout//x" },
            { "openam/UI/Logout" },
            // Absolute, but names no authority at all.
            { "https:/elsewhere/x" },
        };
    }

    @Test(dataProvider = "urlsNamingNoHost")
    public void shouldReturnNullWhereTheUrlNamesNoHost(String url) {
        assertNull(OAuthUtil.getUrlHost(url));
    }

    @DataProvider(name = "urlsNamingAnUnreadableHost")
    public Object[][] urlsNamingAnUnreadableHost() {
        return new Object[][] {
            // Not a URI at all, so there is no authority to read. "https://" is one of these rather than an
            // empty authority: java.net.URI rejects it outright.
            { "not a uri at all ^" },
            { "https://" },
            { "https://attacker.example.com/a b" },
            // Userinfo is delimited by the LAST "@", so stripping to the first one would name
            // "trusted.example.com@evil_host.tld" - a trusted hostname the browser never resolves.
            { "https://x@trusted.example.com@evil_host.tld/" },
            { "https://user@openam_prod.example.com/logout" },
            // A port that is not a number, and an unbracketed IPv6 authority, are not host:port either.
            { "https://openam.example.com:80junk/x" },
            { "https://openam_prod.example.com:-1/x" },
            { "https://::1/x" },
            // Decoded, these spell "victim.com" with the dotless i, U+0131: a host the browser resolves to
            // xn--vctm-lzac.com, not to victim.com.
            { "https://v%C4%B1ct%C4%B1m.com/logout" },
            { "https://v\u0131ct\u0131m.com/logout" },
        };
    }

    @Test(dataProvider = "urlsNamingAnUnreadableHost")
    public void shouldReturnNullRatherThanGuessAtAnAuthorityItCanNotRead(String url) {
        // Where java.net.URI does not parse the authority, this reads it through the same
        // FailSafeRedirectUrlValidator.getBareAuthority the validator uses, so the two refuse the same shapes
        // rather than reading a host out of them. The caller names the host of the request instead.
        assertNull(OAuthUtil.getUrlHost(url));
    }
}
