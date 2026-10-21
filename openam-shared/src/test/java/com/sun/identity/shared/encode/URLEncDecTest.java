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
package com.sun.identity.shared.encode;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link URLEncDec#appendQueryParameter(String, String, String)}.
 */
public class URLEncDecTest {

    @DataProvider(name = "urlsToPutAParameterOn")
    public Object[][] urlsToPutAParameterOn() {
        return new Object[][] {
            // The URL carries no query string yet.
            { "/openam/console", "whr", "sub", "/openam/console?whr=sub" },
            // It already carries one, and the parameter joins it.
            { "/openam/console?a=b", "whr", "sub", "/openam/console?a=b&whr=sub" },
            // The parameter stays in front of the fragment, where the server can see it.
            { "/openam/XUI/#login", "whr", "sub", "/openam/XUI/?whr=sub#login" },
            { "/openam/XUI/?a=b#login", "whr", "sub", "/openam/XUI/?a=b&whr=sub#login" },
            // The value is encoded, so that it is read back whole.
            { "/openam/console", "goto", "/a b&c=d", "/openam/console?goto=%2Fa+b%26c%3Dd" },
            // An occurrence the caller put there first is dropped: getParameter would answer with it.
            { "/openam/console?whr=evil", "whr", "sub", "/openam/console?whr=sub" },
            { "/openam/console?a=b&whr=evil", "whr", "sub", "/openam/console?a=b&whr=sub" },
            { "/openam/console?whr=evil&a=b", "whr", "sub", "/openam/console?a=b&whr=sub" },
            { "/openam/console?whr=evil&whr=worse", "whr", "sub", "/openam/console?whr=sub" },
            { "/openam/console?whr", "whr", "sub", "/openam/console?whr=sub" },
            // Including an occurrence that only the container would decode back to the name.
            { "/openam/console?%77hr=evil", "whr", "sub", "/openam/console?whr=sub" },
            // A name that merely starts the same is left alone.
            { "/openam/console?whrx=keep", "whr", "sub", "/openam/console?whrx=keep&whr=sub" },
            // A trailing question mark is a query string with nothing in it.
            { "/openam/console?", "whr", "sub", "/openam/console?whr=sub" },
        };
    }

    @Test(dataProvider = "urlsToPutAParameterOn")
    public void shouldPutTheParameterOnTheUrl(String url, String name, String value, String expected) {
        assertThat(URLEncDec.appendQueryParameter(url, name, value)).isEqualTo(expected);
    }

    @Test
    public void shouldAnswerNullForNoUrl() {
        assertThat(URLEncDec.appendQueryParameter(null, "whr", "sub")).isNull();
    }

    @Test
    public void shouldLeaveTheUrlAsItStandsForNoParameterName() {
        // The null url is answered rather than thrown at, so a name that is not there is answered as well
        // instead of escaping as a 500 from whichever page is composing the URL.
        assertThat(URLEncDec.appendQueryParameter("/openam/console", null, "sub")).isEqualTo("/openam/console");
        assertThat(URLEncDec.appendQueryParameter("/openam/console", "", "sub")).isEqualTo("/openam/console");
    }

    @Test
    public void shouldWriteAMissingValueAsAnEmptyOne() {
        // A parameter carrying nothing is spelled name= on a query string, which is what a caller handing over
        // a value it does not have means.
        assertThat(URLEncDec.appendQueryParameter("/openam/console", "whr", null))
                .isEqualTo("/openam/console?whr=");
        assertThat(URLEncDec.appendQueryParameter("/openam/console?whr=old#f", "whr", null))
                .isEqualTo("/openam/console?whr=#f");
    }

    @Test
    public void shouldKeepAnUndecodableParameterName() {
        // A name that cannot be decoded is not the one being put on the URL either, so it survives.
        assertThat(URLEncDec.appendQueryParameter("/openam/console?%zz=keep", "whr", "sub"))
                .isEqualTo("/openam/console?%zz=keep&whr=sub");
    }
}
