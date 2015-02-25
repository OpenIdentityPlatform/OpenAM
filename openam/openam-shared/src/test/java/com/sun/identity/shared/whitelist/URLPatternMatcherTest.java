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
package com.sun.identity.shared.whitelist;

import java.net.MalformedURLException;
import java.util.Collections;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.DataProvider;

public class URLPatternMatcherTest {

    final URLPatternMatcher urlPatternMatcher = new URLPatternMatcher();

    @DataProvider(name = "testdata")
    private void performTestAndAssert(String pattern, String url, boolean result) throws MalformedURLException {
        boolean answer = urlPatternMatcher.match(url, Collections.singleton(pattern), true); //wildcard always enabled
        assertEquals(result, answer);
    }

    @DataProvider(name = "testdata")
    public Object[][] testBasic() {
        final String PATTERN_1 = "https://www.google.com/*";
        final String PATTERN_2 = "http://www.good.com";
        final String PATTERN_3 = "http://www.good.com/hello.html";
        final String PATTERN_4 = "*";
        final String PATTERN_5 = "http://*";
        final String PATTERN_6 = "http://www.*:*";
        final String PATTERN_7 = "http://www.*/*";
        final String PATTERN_8 = "http://www.*:*/";
        final String PATTERN_9 = "http://www.*.good.com";
        final String PATTERN_10 = "http://www.*.good.com/";
        final String PATTERN_11 = "http://www.*.good.com/*";
        final String PATTERN_12 = "https://www.*.good.com:*/*";
        final String PATTERN_13 = "https://www.*.good.com:*/-*-/blah";
        final String PATTERN_14 = "*?*";
        final String PATTERN_15 = "http://*.good.com*/*?*";
        final String PATTERN_16 = "/abc*";
        final String PATTERN_17 = "http://www.good.com:80*";
        final String PATTERN_18 = "https://www.*.good.com:443/*";
        final String PATTERN_19 = "https://www.good.com:443/asdf*/blah";
        final String PATTERN_20 = "https://www.good.com:443/asdf-*-/blah";
        final String PATTERN_21 = "http://www.good.com:80/*/hello?*";
        final String PATTERN_22 = "http://www.good.com:80/-*-/-*-/-*-?*";
        final String PATTERN_23 = "http://www.good.com:80/*/*/*?*";

        return new Object[][]{

                //basic test one
            {PATTERN_1, "https://www.google.com:443/hello/there/dance/for/me", true},
            {PATTERN_1, "https://www.google.com:443/hello/there", true},
            {PATTERN_1, "https://www.google.com:443/hello/there/", true},
            {PATTERN_1, "https://www.google.com:443/", true},
            {PATTERN_1, "https://www.google.com:443", true},

                //basic test two
            {PATTERN_2, "http://www.good.com", true},
            {PATTERN_2, "http://www.good.com/", true},
            {PATTERN_2, "http://www.good.com:80", true},
            {PATTERN_2, "http://www.good.com:80/", true},
            {PATTERN_2, "http://www.good.com:80/hello/world", false},
            {PATTERN_2, "https://www.hello.good.com", false},
            {PATTERN_2, "https://www.hello.good.com:443", false},
            {PATTERN_2, "https://www.hello.good.com:443/hello/world", false},
            {PATTERN_2, "http://www.hello.good.com/hello/world", false},
            {PATTERN_2, "http://www.hello.good.com/hello/world?key=value", false},
            {PATTERN_2, "http://www.hello.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_2, "http://www.hello.good.com:80/hello/world", false},
            {PATTERN_2, "http://www.hello.good.com:80/hello/world?key=value", false},
            {PATTERN_2, "http://www.hello.good.com:80/hello/world?key=value&key2=value2", false},
            {PATTERN_2, "http://www.hello.bad.com/hello/world/.good.com:80", false},
            {PATTERN_2, "http://www.hello.bad.com/hello/world/.good.com:80/hello/world", false},
            {PATTERN_2, "http://www.hello.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_2, "http://www.hello.bad.com:80/hello/world/.good.com:80/hello/world", false},

                // tests basic matching with path
            {PATTERN_3, "http://www.good.com", false},
            {PATTERN_3, "http://www.good.com/", false},
            {PATTERN_3, "http://www.good.com:80", false},
            {PATTERN_3, "http://www.good.com:80/", false},
            {PATTERN_3, "http://www.good.com/hello.html", true},
            {PATTERN_3, "http://www.good.com/hello.html/", false},
            {PATTERN_3, "http://www.good.com:80/hello.html", true},
            {PATTERN_3, "http://www.good.com:80/hello.html/", false},
            {PATTERN_3, "http://www.good.com:80/hello/world", false},
            {PATTERN_3, "https://www.hello.good.com", false},
            {PATTERN_3, "https://www.hello.good.com:443", false},
            {PATTERN_3, "https://www.hello.good.com:443/hello/world", false},
            {PATTERN_3, "http://www.hello.good.com/hello/world", false},
            {PATTERN_3, "http://www.hello.good.com/hello/world?key=value", false},
            {PATTERN_3, "http://www.hello.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_3, "http://www.hello.good.com:80/hello/world", false},
            {PATTERN_3, "http://www.hello.good.com:80/hello/world?key=value", false},
            {PATTERN_3, "http://www.hello.good.com:80/hello/world?key=value&key2=value2", false},
            {PATTERN_3, "http://www.hello.bad.com:/hello/world/.good.com:80", false},
            {PATTERN_3, "http://www.hello.bad.com/hello/world/.good.com:80/hello/world", false},
            {PATTERN_3, "http://www.hello.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_3, "http://www.hello.bad.com:80/hello/world/.good.com:80/hello/world", false},

                // tests basic wildcards, should match everything except URLs with question marks in path
            {PATTERN_4, "http://www.good.com", true},
            {PATTERN_4, "http://www.good.com/", true},
            {PATTERN_4, "http://www.good.com:80", true},
            {PATTERN_4, "http://www.good.com:80/", true},
            {PATTERN_4, "http://www.good.com:80/hello/world", true},
            {PATTERN_4, "https://www.good.com", true},
            {PATTERN_4, "https://www.good.com/", true},
            {PATTERN_4, "https://www.good.com:443", true},
            {PATTERN_4, "https://www.good.com:443/", true},
            {PATTERN_4, "https://www.good.com:443/hello/world", true},
            {PATTERN_4, "http://www.good.com/hello/world", true},
            {PATTERN_4, "http://www.good.com/hello/world?key=value", false},
            {PATTERN_4, "http://www.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_4, "http://www.good.com:80/hello/world", true},
            {PATTERN_4, "http://www.good.com:80/hello/world?key=value", false},
            {PATTERN_4, "http://www.good.com:80/hello/world?key=value&key2=value2", false},
            {PATTERN_4, "http://www.bad.com:80/hello/world/.good.com:80", true},
            {PATTERN_4, "http://www.bad.com:80/hello/world/.good.com:80/hello/world", true},

                //testing wildcard in-location. Should only match domain, and default port (with or without trailing slash)
            {PATTERN_5, "http://www.good.com", true},
            {PATTERN_5, "http://www.good.com:80", true},
            {PATTERN_5, "http://www.good.com:80/hello/world", false},
            {PATTERN_5, "https://www.good.com", false},
            {PATTERN_5, "https://www.good.com:443", false},
            {PATTERN_5, "https://www.good.com:443/hello/world", false},
            {PATTERN_5, "http://www.good.com/hello/world", false},
            {PATTERN_5, "http://www.good.com/hello/world?key=value", false},
            {PATTERN_5, "http://www.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_5, "http://www.good.com:80/hello/world", false},
            {PATTERN_5, "http://www.good.com:80/hello/world?key=value", false},
            {PATTERN_5, "http://www.good.com:80/hello/world?key=value&key2=value2", false},
            {PATTERN_5, "http://www.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_5, "http://www.bad.com:80/hello/world/.good.com:80/hello/world", false},

                //as above, but allows the port to be anything other than 80
            {PATTERN_6, "http://www.good.com", true},
            {PATTERN_6, "http://www.good.com:80", true},
            {PATTERN_6, "http://www.good.com:80/hello/world", false},
            {PATTERN_6, "https://www.good.com", false},
            {PATTERN_6, "https://www.good.com:443", false},
            {PATTERN_6, "https://www.good.com:443/hello/world", false},
            {PATTERN_6, "http://www.good.com/hello/world", false},
            {PATTERN_6, "http://www.good.com/hello/world?key=value", false},
            {PATTERN_6, "http://www.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_6, "http://www.good.com:80/hello/world", false},
            {PATTERN_6, "http://www.good.com:80/hello/world?key=value", false},
            {PATTERN_6, "http://www.good.com:80/hello/world?key=value&key2=value2", false},
            {PATTERN_6, "http://www.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_6, "http://www.bad.com:80/hello/world/.good.com:80/hello/world", false},
            {PATTERN_6, "http://bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_6, "http://bad.com:80/hello/world/.good.com:80/hello/world", false},

                //default port, random host and random path of indeterminable length (excluding question mark)
            {PATTERN_7, "http://www.good.com", true},
            {PATTERN_7, "http://www.good.com:80", true},
            {PATTERN_7, "http://www.good.com:80/hello/world", true},
            {PATTERN_7, "https://www.good.com", false},
            {PATTERN_7, "https://www.good.com:443", false},
            {PATTERN_7, "https://www.good.com:443/hello/world", false},
            {PATTERN_7, "http://www.good.com/hello/world", true},
            {PATTERN_7, "http://www.good.com/hello/world?key=value", false},
            {PATTERN_7, "http://www.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_7, "http://www.good.com:80/hello/world", true},
            {PATTERN_7, "http://www.good.com:80/hello/world?key=value", false},
            {PATTERN_7, "http://www.good.com:80/hello/world?key=value&key2=value2", false},
            {PATTERN_7, "http://www.bad.com:80/hello/world", true},
            {PATTERN_7, "http://www.bad.com", true},
            {PATTERN_7, "http://www.bad.com:80", true},
            {PATTERN_7, "http://www.bad.com:80/hello/world/.good.com:80", true},
            {PATTERN_7, "http://www.bad.com:80/hello/world/.good.com:80/hello/world", true},

                //random domain, random port, no path but allowed to have optional trailing slash
            {PATTERN_8, "http://www.good.com", true},
            {PATTERN_8, "http://www.good.com/", true},
            {PATTERN_8, "http://www.good.com:80", true},
            {PATTERN_8, "http://www.good.com:80/", true},
            {PATTERN_8, "http://www.good.com:80/hello/world", false},
            {PATTERN_8, "https://www.good.com", false},
            {PATTERN_8, "https://www.good.com:443", false},
            {PATTERN_8, "https://www.good.com:443/hello/world", false},
            {PATTERN_8, "http://www.good.com/hello/world", false},
            {PATTERN_8, "http://www.good.com/hello/world?key=value", false},
            {PATTERN_8, "http://www.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_8, "http://www.good.com:80/hello/world", false},
            {PATTERN_8, "http://www.good.com:80/hello/world?key=value", false},
            {PATTERN_8, "http://www.good.com:80/hello/world?key=value&key2=value2", false},
            {PATTERN_8, "http://www.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_8, "http://www.bad.com:80/hello/world/.good.com:80/hello/world", false},

                //random subdomain (but only so long as it's actually a subdomain and not part of the path)
            {PATTERN_9, "http://www.good.com", false},
            {PATTERN_9, "http://www.good.com:80", false},
            {PATTERN_9, "http://www.good.com:80/hello/world", false},
            {PATTERN_9, "http://www.hello.good.com", true},
            {PATTERN_9, "http://www.hello.good.com/", true},
            {PATTERN_9, "http://www.hello.good.com:80", true},
            {PATTERN_9, "http://www.hello.good.com:80/", true},
            {PATTERN_9, "http://www.hello.good.com:80/hello/world", false},
            {PATTERN_9, "https://www.hello.good.com", false},
            {PATTERN_9, "https://www.hello.good.com:443", false},
            {PATTERN_9, "https://www.hello.good.com:443/hello/world", false},
            {PATTERN_9, "http://www.hello.good.com/hello/world", false},
            {PATTERN_9, "http://www.hello.good.com/hello/world?key=value", false},
            {PATTERN_9, "http://www.hello.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_9, "http://www.hello.good.com:80/hello/world", false},
            {PATTERN_9, "http://www.hello.good.com:80/hello/world?key=value", false},
            {PATTERN_9, "http://www.hello.good.com:80/hello/world?key=value&key2=value2", false},
            {PATTERN_9, "http://www.hello.bad.com:/hello/world/.good.com:80", false},
            {PATTERN_9, "http://www.hello.bad.com/hello/world/.good.com:80/hello/world", false},
            {PATTERN_9, "http://www.hello.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_9, "http://www.hello.bad.com:80/hello/world/.good.com:80/hello/world", false},

                //random subdomain with trailing slash, same as previous in different format
            {PATTERN_10, "http://www.hello.good.com", true},
            {PATTERN_10, "http://www.hello.good.com/", true},
            {PATTERN_10, "http://www.hello.good.com:80", true},
            {PATTERN_10, "http://www.hello.good.com:80/", true},
            {PATTERN_10, "http://www.hello.good.com:80/hello/world", false},
            {PATTERN_10, "https://www.hello.good.com", false},
            {PATTERN_10, "https://www.hello.good.com:443", false},
            {PATTERN_10, "https://www.hello.good.com:443/hello/world", false},
            {PATTERN_10, "http://www.hello.good.com/hello/world", false},
            {PATTERN_10, "http://www.hello.good.com/hello/world?key=value", false},
            {PATTERN_10, "http://www.hello.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_10, "http://www.hello.good.com:80/hello/world", false},
            {PATTERN_10, "http://www.hello.good.com:80/hello/world?key=value", false},
            {PATTERN_10, "http://www.hello.good.com:80/hello/world?key=value&key2=value2", false},
            {PATTERN_10, "http://www.hello.bad.com:/hello/world/.good.com:80", false},
            {PATTERN_10, "http://www.hello.bad.com/hello/world/.good.com:80/hello/world", false},
            {PATTERN_10, "http://www.hello.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_10, "http://www.hello.bad.com:80/hello/world/.good.com:80/hello/world", false},

                //random subdomain, default port, indeterminable length path, no question marks
            {PATTERN_11, "http://www.hello.good.com", true},
            {PATTERN_11, "http://www.hello.good.com:80", true},
            {PATTERN_11, "http://www.hello.good.com:80/hello/world", true},
            {PATTERN_11, "https://www.hello.good.com", false},
            {PATTERN_11, "https://www.hello.good.com:443", false},
            {PATTERN_11, "https://www.hello.good.com:443/hello/world", false},
            {PATTERN_11, "http://www.hello.good.com/hello/world", true},
            {PATTERN_11, "http://www.hello.good.com/hello/world?key=value", false},
            {PATTERN_11, "http://www.hello.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_11, "http://www.hello.good.com:80/hello/world", true},
            {PATTERN_11, "http://www.hello.good.com:80/hello/world?key=value", false},
            {PATTERN_11, "http://www.hello.good.com:80/hello/world?key=value&key2=value2", false},
            {PATTERN_11, "http://www.hello.bad.com:/hello/world/.good.com:80", false},
            {PATTERN_11, "http://www.hello.bad.com/hello/world/.good.com:80/hello/world", false},
            {PATTERN_11, "http://www.hello.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_11, "http://www.hello.bad.com:80/hello/world/.good.com:80/hello/world", false},

                //random subdomain, random port random path of indeterminable length, no question marks
            {PATTERN_12, "https://www.good.com", false},
            {PATTERN_12, "https://www.good.com/", false},
            {PATTERN_12, "https://www.good.com:443", false},
            {PATTERN_12, "https://www.good.com:443/", false},
            {PATTERN_12, "http://www.hello.good.com/path/", false},
            {PATTERN_12, "https://www.hello.good.com/path/", true},
            {PATTERN_12, "https://www.hello.good.com/", true},
            {PATTERN_12, "https://www.hello.good.com", true},
            {PATTERN_12, "https://www.good.com:443/path/", false},
            {PATTERN_12, "https://www.subdomain.good.com:443/path/", true},
            {PATTERN_12, "https://www.bad.com/.good.com:443/evil", false},
            {PATTERN_12, "https://www.hello.good.com:80/path/", true},
            {PATTERN_12, "https://www.bad.com:443/.good.com:443/evil", false},

                //testing single-level wildcards. After the (random) port, should have a single level before blah
            {PATTERN_13, "https://www.one.good.com", false},
            {PATTERN_13, "https://www.one.good.com:80", false},
            {PATTERN_13, "https://www.good.com/bad", false},
            {PATTERN_13, "https://www.bad.com/.good.com", false},
            {PATTERN_13, "https://www.one.good.com/hello/blah", true},
            {PATTERN_13, "https://www.one.good.com/hello/world/blah", false},
            {PATTERN_13, "https://www.one.good.com/hello/world/also/more/blah", false},

                //allows for anything which contains a question mark in it, regardless of scheme, host or port
            {PATTERN_14, "http://www.good.com", false},
            {PATTERN_14, "http://www.good.com:80", false},
            {PATTERN_14, "http://www.good.com:80/hello/world", false},
            {PATTERN_14, "https://www.good.com", false},
            {PATTERN_14, "https://www.good.com:443", false},
            {PATTERN_14, "https://www.good.com:443/hello/world", false},
            {PATTERN_14, "http://www.good.com/hello/world", false},
            {PATTERN_14, "http://www.good.com/hello/world?key=value", true},
            {PATTERN_14, "http://www.good.com/hello/world?key=value&key2=value2", true},
            {PATTERN_14, "http://www.good.com:80/hello/world", false},
            {PATTERN_14, "http://www.good.com:80/hello/world?", true},
            {PATTERN_14, "http://www.good.com:80/hello/world?key=value", true},
            {PATTERN_14, "http://www.good.com:80/hello/world?key=value&key2=value2", true},
            {PATTERN_14, "http://www.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_14, "http://www.bad.com:80/hello/world/.good.com:80/hello/world", false},

                //random subdomain, default port, any length path with question mark
            {PATTERN_15, "http://www.good.com", false},
            {PATTERN_15, "http://www.good.com:80", false},
            {PATTERN_15, "http://www.good.com:80/hello/world", false},
            {PATTERN_15, "https://www.good.com", false},
            {PATTERN_15, "https://www.good.com:443", false},
            {PATTERN_15, "https://www.good.com:443/hello/world", false},
            {PATTERN_15, "http://www.good.com/hello/world", false},
            {PATTERN_15, "http://www.good.com/hello/world?key=value", true},
            {PATTERN_15, "http://www.good.com/hello/world?key=value&key2=value2", true},
            {PATTERN_15, "http://www.good.com:80/hello/world", false},
            {PATTERN_15, "http://www.good.com:80/hello/world?key=value", true},
            {PATTERN_15, "http://www.good.com:80/hello/world?key=value&key2=value2", true},
            {PATTERN_15, "http://www.good.com:80/hello/good/bye/world?key=value&key2=value2", true},
            {PATTERN_15, "http://www.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_15, "http://www.bad.com:80/hello/world/.good.com:80/hello/world", false},

            {PATTERN_16, "http://www.good.com", false},
            {PATTERN_16, "http://www.good.com:80", false},
            {PATTERN_16, "http://www.good.com:80/hello/world", false},
            {PATTERN_16, "http://www.good.com:80/abc/world", true},
            {PATTERN_16, "http://www.good.com:80/abc", true},
            {PATTERN_16, "http://www.good.com/abc", true},
            {PATTERN_16, "https://www.good.com", false},
            {PATTERN_16, "https://www.good.com:443", false},
            {PATTERN_16, "http://www.good.com/hello/world", false},
            {PATTERN_16, "http://www.good.com/hello/world?key=value", false},
            {PATTERN_16, "http://www.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_16, "http://www.good.com:80/hello/world", false},
            {PATTERN_16, "http://www.good.com:80/hello/world?key=value", false},
            {PATTERN_16, "http://www.good.com:80/abc/world?key=value&key2=value2", false},
            {PATTERN_16, "http://www.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_16, "http://www.bad.com:80/hello/world/.good.com:80/hello/world", false},

                //wildcard after port
            {PATTERN_17, "http://www.good.com", true},
            {PATTERN_17, "http://www.good.com:80", true},
            {PATTERN_17, "http://www.good.com:808", true},
            {PATTERN_17, "http://www.good.com:808/", true},
            {PATTERN_17, "http://www.good.com:808/asdf", false},
            {PATTERN_17, "http://www.good.com:80/hello/world", false},
            {PATTERN_17, "http://www.good.com:80/abc/world", false},
            {PATTERN_17, "http://www.good.com:80/abc", false},
            {PATTERN_17, "http://www.good.com/abc", false},
            {PATTERN_17, "https://www.good.com", false},
            {PATTERN_17, "https://www.good.com:443", false},
            {PATTERN_17, "http://www.good.com/hello/world", false},
            {PATTERN_17, "http://www.good.com/hello/world?key=value", false},
            {PATTERN_17, "http://www.good.com/hello/world?key=value&key2=value2", false},
            {PATTERN_17, "http://www.good.com:80/hello/world?key=value", false},
            {PATTERN_17, "http://www.good.com:80/abc/world?key=value&key2=value2", false},
            {PATTERN_17, "http://www.bad.com:80/hello/world/.good.com:80", false},
            {PATTERN_17, "http://www.bad.com:80/hello/world/.good.com:80/hello/world", false},

                //any subdomain, any path
            {PATTERN_18, "https://www.good.com", false},
            {PATTERN_18, "https://www.good.com/", false},
            {PATTERN_18, "https://www.bad.good.com/", true},
            {PATTERN_18, "https://www.good.com:443", false},
            {PATTERN_18, "https://www.good.com:443/", false},
            {PATTERN_18, "http://www.hello.good.com/path/", false},
            {PATTERN_18, "https://www.hello.good.com/path/", true},
            {PATTERN_18, "https://www.good.com:443/path/", false},
            {PATTERN_18, "https://www.subdomain.good.com:443/path/", true},
            {PATTERN_18, "https://www.bad.com/.good.com:443/evil", false},
            {PATTERN_18, "https://www.hello.good.com:80/path/", false},
            {PATTERN_18, "https://www.bad.com:443/.good.com:443/evil", false},

                //continuation after "asdf"
            {PATTERN_19, "https://www.good.com", false},
            {PATTERN_19, "https://www.good.com/", false},
            {PATTERN_19, "https://www.bad.good.com/", false},
            {PATTERN_19, "https://www.good.com:443", false},
            {PATTERN_19, "https://www.good.com:443/", false},
            {PATTERN_19, "http://www.hello.good.com/path/", false},
            {PATTERN_19, "https://www.hello.good.com/path/", false},
            {PATTERN_19, "https://www.good.com:443/path/", false},
            {PATTERN_19, "https://www.subdomain.good.com:443/path/", false},
            {PATTERN_19, "https://www.bad.com/.good.com:443/evil", false},
            {PATTERN_19, "http://www.hello.good.com:80/asdf123/path/blah", false},
            {PATTERN_19, "https://www.good.com:443/asdf123/blah", true},
            {PATTERN_19, "https://www.good.com:443/asdf123/456/blah", true},
            {PATTERN_19, "https://www.bad.com:443/.good.com:443/evil", false},

                //continuation after "asdf" stops at first slash
            {PATTERN_20, "https://www.good.com:443/asdf123/blah", true},
            {PATTERN_20, "https://www.good.com:443/asdf123/456/blah", false},

                //any levels before hello, with query params
            {PATTERN_21, "http://www.good.com", false},
            {PATTERN_21, "http://www.good.com:80", false},
            {PATTERN_21, "http://www.good.com:80/hello/bob/hello/?bob=bob", false},
            {PATTERN_21, "http://www.good.com:80/hello/bob/hello?bob=bob", true},
            {PATTERN_21, "http://www.good.com:80/hello/hello?bob=bob", true},
            {PATTERN_21, "http://www.good.com:80/hello/bob?bob=bob", false},
            {PATTERN_21, "http://www.good.com:80/hello/world/", false},
            {PATTERN_21, "http://www.good.com:80/hello/world", false},
            {PATTERN_21, "http://www.good.com:80/hello/?bob=bob", false},

                //optional path-element before question mark, exactly two levels before that
            {PATTERN_22, "http://www.good.com", false},
            {PATTERN_22, "http://www.good.com:80", false},
            {PATTERN_22, "http://www.good.com:80/hello/bob/hello?bob=bob", true},
            {PATTERN_22, "http://www.good.com:80/hello/bob/hello/?bob=bob", true},
            {PATTERN_22, "http://www.good.com:80/hello/bob/hello/again?bob=bob", false},
            {PATTERN_22, "http://www.good.com:80/hello/bob/hello/again/and/again?bob=bob", false},
            {PATTERN_22, "http://www.good.com:80/hello/bob/?bob=bob", true},
            {PATTERN_22, "http://www.good.com:80/hello/bob?bob=bob", true},
            {PATTERN_22, "http://www.good.com:80/hello?bob=bob", false},
            {PATTERN_22, "http://www.good.com:80/hello/world/", false},
            {PATTERN_22, "http://www.good.com:80/hello/world", false},
            {PATTERN_22, "http://www.good.com:80/hello/?bob=bob", false},

                //optional path-element before question mark, two (or more) levels before that (but not fewer)
            {PATTERN_23, "http://www.good.com", false},
            {PATTERN_23, "http://www.good.com:80", false},
            {PATTERN_23, "http://www.good.com:80/hello/bob/hello?bob=bob", true},
            {PATTERN_23, "http://www.good.com:80/hello/bob/hello/?bob=bob", true},
            {PATTERN_23, "http://www.good.com:80/hello/bob/hello/again?bob=bob", true},
            {PATTERN_23, "http://www.good.com:80/hello/bob/hello/again/and/again?bob=bob", true},
            {PATTERN_23, "http://www.good.com:80/hello/bob/?bob=bob", true},
            {PATTERN_23, "http://www.good.com:80/hello/bob?bob=bob", false},
            {PATTERN_23, "http://www.good.com:80/hello?bob=bob", false},
            {PATTERN_23, "http://www.good.com:80/hello/world/", false},
            {PATTERN_23, "http://www.good.com:80/hello/world", false},
            {PATTERN_23, "http://www.good.com:80/hello/?bob=bob", false}
        };
    }
}
