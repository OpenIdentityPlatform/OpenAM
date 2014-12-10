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
import org.testng.annotations.Test;

/**
 * This tests for wildcard matching for whitelisting. This is NOT the same
 * system as used by policy matching. Specifically:
 *
 * Wildcard is always single-level only (until the next slash), unless it's the last value, and
 * it's preceeded by the delimiter.
 *
 * A combination of delimiter-wildcard (e.g. /*) at the end of a URL's path will
 * continue to match indefinitely.
 *
 * When matching query parameters, use the single-level-only wildcard (default -*-) instead
 * of the standard wildcard to continue to enforce a precise number of levels prior to the
 * quesiton mark. See tests eighteen and nineteen for examples.
 *
 */
public class URLPatternMatcherTest {

    final URLPatternMatcher urlPatternMatcher = new URLPatternMatcher();

    private void performTestAndAssert(String url, boolean result, String pattern) throws MalformedURLException {
        boolean answer = urlPatternMatcher.match(url, Collections.singleton(pattern), true); //wildcard always enabled
        assertEquals(result, answer);
    }

    @DataProvider(name = "basic")
    public Object[][] testBasic() {
        return new Object[][]{
            {"https://www.google.com:443/hello/there/dance/for/me", true},
            {"https://www.google.com:443/hello/there", false},
            {"https://www.google.com:443/hello/there/", true}
        };
    }

    // tests basic matching, including trailing slash
    @Test(dataProvider = "basic")
    public void testBasicRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "https://www.google.com/*/*/*";
        performTestAndAssert(url, result, pattern);
    }


    @DataProvider(name = "basicOne")
    public Object[][] testBasicOne() {
        return new Object[][]{
            {"http://www.good.com", true},
            {"http://www.good.com/", true},
            {"http://www.good.com:80", true},
            {"http://www.good.com:80/", true},
            {"http://www.good.com:80/hello/world", false},
            {"https://www.hello.good.com", false},
            {"https://www.hello.good.com:443", false},
            {"https://www.hello.good.com:443/hello/world", false},
            {"http://www.hello.good.com/hello/world", false},
            {"http://www.hello.good.com/hello/world?key=value", false},
            {"http://www.hello.good.com/hello/world?key=value&key2=value2", false},
            {"http://www.hello.good.com:80/hello/world", false},
            {"http://www.hello.good.com:80/hello/world?key=value", false},
            {"http://www.hello.good.com:80/hello/world?key=value&key2=value2", false},
            {"http://www.hello.bad.com/hello/world/.good.com:80", false},
            {"http://www.hello.bad.com/hello/world/.good.com:80/hello/world", false},
            {"http://www.hello.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.hello.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    // tests basic matching, without trailing slash
    @Test(dataProvider = "basicOne")
    public void testBasicRunnerOne(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.good.com";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "basicTwo")
    public Object[][] testBasicTwo() {
        return new Object[][]{
            {"http://www.good.com", false},
            {"http://www.good.com/", false},
            {"http://www.good.com:80", false},
            {"http://www.good.com:80/", false},

            {"http://www.good.com/hello.html", true},
            {"http://www.good.com/hello.html/", false},
            {"http://www.good.com:80/hello.html", true},
            {"http://www.good.com:80/hello.html/", false},
            {"http://www.good.com:80/hello/world", false},

            {"https://www.hello.good.com", false},
            {"https://www.hello.good.com:443", false},
            {"https://www.hello.good.com:443/hello/world", false},

            {"http://www.hello.good.com/hello/world", false},
            {"http://www.hello.good.com/hello/world?key=value", false},
            {"http://www.hello.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.hello.good.com:80/hello/world", false},
            {"http://www.hello.good.com:80/hello/world?key=value", false},
            {"http://www.hello.good.com:80/hello/world?key=value&key2=value2", false},

            {"http://www.hello.bad.com:/hello/world/.good.com:80", false},
            {"http://www.hello.bad.com/hello/world/.good.com:80/hello/world", false},

            {"http://www.hello.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.hello.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    // tests basic matching with path
    @Test(dataProvider = "basicTwo")
    public void testBasicTwoRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.good.com/hello.html";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testOne")
    public Object[][] testOne() {
        return new Object[][] {
            {"http://www.good.com", true},
            {"http://www.good.com/", true},
            {"http://www.good.com:80", true},
            {"http://www.good.com:80/", true},
            {"http://www.good.com:80/hello/world", true},

            {"https://www.good.com", true},
            {"https://www.good.com/", true},
            {"https://www.good.com:443", true},
            {"https://www.good.com:443/", true},

            {"https://www.good.com:443/hello/world", true},

            {"http://www.good.com/hello/world", true},
            {"http://www.good.com/hello/world?key=value", false},
            {"http://www.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.good.com:80/hello/world", true},
            {"http://www.good.com:80/hello/world?key=value", false},
            {"http://www.good.com:80/hello/world?key=value&key2=value2", false},

            {"http://www.bad.com:80/hello/world/.good.com:80", true},
            {"http://www.bad.com:80/hello/world/.good.com:80/hello/world", true}
        };
    }

    // tests basic wildcards, should match everything except URLs with question marks in path
    @Test(dataProvider = "testOne")
    public void testOneRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testTwo")
    public Object[][] testTwo() {
        return new Object[][] {
            {"http://www.good.com", true},
            {"http://www.good.com:80", true},
            {"http://www.good.com:80/hello/world", false},

            {"https://www.good.com", false},
            {"https://www.good.com:443", false},
            {"https://www.good.com:443/hello/world", false},

            {"http://www.good.com/hello/world", false},
            {"http://www.good.com/hello/world?key=value", false},
            {"http://www.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.good.com:80/hello/world", false},
            {"http://www.good.com:80/hello/world?key=value", false},
            {"http://www.good.com:80/hello/world?key=value&key2=value2", false},

            {"http://www.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    //testing wildcard in-location. Should only match domain, and default port (with or without trailing slash)
    @Test(dataProvider = "testTwo")
    public void testTwoRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testThree")
    public Object[][] testThree() {
        return new Object[][] {
            {"http://www.good.com", true},
            {"http://www.good.com:80", true},
            {"http://www.good.com:80/hello/world", false},

            {"https://www.good.com", false},
            {"https://www.good.com:443", false},
            {"https://www.good.com:443/hello/world", false},

            {"http://www.good.com/hello/world", false},
            {"http://www.good.com/hello/world?key=value", false},
            {"http://www.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.good.com:80/hello/world", false},
            {"http://www.good.com:80/hello/world?key=value", false},
            {"http://www.good.com:80/hello/world?key=value&key2=value2", false},

            {"http://www.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.bad.com:80/hello/world/.good.com:80/hello/world", false},

            {"http://bad.com:80/hello/world/.good.com:80", false},
            {"http://bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    //as above, but allows the port to be anything other than 80
    @Test(dataProvider = "testThree")
    public void testThreeRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.*:*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testFour")
    public Object[][] testFour() {
        return new Object[][] {
            {"http://www.good.com", true},
            {"http://www.good.com:80", true},
            {"http://www.good.com:80/hello/world", true},

            {"https://www.good.com", false},
            {"https://www.good.com:443", false},
            {"https://www.good.com:443/hello/world", false},

            {"http://www.good.com/hello/world", true},
            {"http://www.good.com/hello/world?key=value", false},
            {"http://www.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.good.com:80/hello/world", true},
            {"http://www.good.com:80/hello/world?key=value", false},
            {"http://www.good.com:80/hello/world?key=value&key2=value2", false},
            {"http://www.bad.com:80/hello/world", true},
            {"http://www.bad.com", true},
            {"http://www.bad.com:80", true},

            {"http://www.bad.com:80/hello/world/.good.com:80", true},
            {"http://www.bad.com:80/hello/world/.good.com:80/hello/world", true}
        };
    }

    //default port, random host and random path of indeterminable length (excluding question mark)
    @Test(dataProvider = "testFour")
    public void testFourRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.*/*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testFive")
    public Object[][] testFive() {
        return new Object[][] {
            {"http://www.good.com", true},
            {"http://www.good.com/", true},
            {"http://www.good.com:80", true},
            {"http://www.good.com:80/", true},
            {"http://www.good.com:80/hello/world", false},

            {"https://www.good.com", false},
            {"https://www.good.com:443", false},
            {"https://www.good.com:443/hello/world", false},

            {"http://www.good.com/hello/world", false},
            {"http://www.good.com/hello/world?key=value", false},
            {"http://www.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.good.com:80/hello/world", false},
            {"http://www.good.com:80/hello/world?key=value", false},
            {"http://www.good.com:80/hello/world?key=value&key2=value2", false},

            {"http://www.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    //random domain, random port, no path but allowed to have optional trailing slash (see testThree)
    @Test(dataProvider = "testFive")
    public void testFiveRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.*:*/";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testSix")
    public Object[][] testSix() {
        return new Object[][] {
            {"http://www.good.com", false},
            {"http://www.good.com:80", false},
            {"http://www.good.com:80/hello/world", false},

            {"http://www.hello.good.com", true},
            {"http://www.hello.good.com/", true},
            {"http://www.hello.good.com:80", true},
            {"http://www.hello.good.com:80/", true},
            {"http://www.hello.good.com:80/hello/world", false},

            {"https://www.hello.good.com", false},
            {"https://www.hello.good.com:443", false},
            {"https://www.hello.good.com:443/hello/world", false},

            {"http://www.hello.good.com/hello/world", false},
            {"http://www.hello.good.com/hello/world?key=value", false},
            {"http://www.hello.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.hello.good.com:80/hello/world", false},
            {"http://www.hello.good.com:80/hello/world?key=value", false},
            {"http://www.hello.good.com:80/hello/world?key=value&key2=value2", false},

            {"http://www.hello.bad.com:/hello/world/.good.com:80", false},
            {"http://www.hello.bad.com/hello/world/.good.com:80/hello/world", false},

            {"http://www.hello.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.hello.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    //random subdomain, but only so long as it's actually a subdomain and not part of the path
    @Test(dataProvider = "testSix")
    public void testSixRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.*.good.com";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testSeven")
    public Object[][] testSeven() {
        return new Object[][] {
            {"http://www.hello.good.com", true},
            {"http://www.hello.good.com/", true},
            {"http://www.hello.good.com:80", true},
            {"http://www.hello.good.com:80/", true},
            {"http://www.hello.good.com:80/hello/world", false},

            {"https://www.hello.good.com", false},
            {"https://www.hello.good.com:443", false},
            {"https://www.hello.good.com:443/hello/world", false},

            {"http://www.hello.good.com/hello/world", false},
            {"http://www.hello.good.com/hello/world?key=value", false},
            {"http://www.hello.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.hello.good.com:80/hello/world", false},
            {"http://www.hello.good.com:80/hello/world?key=value", false},
            {"http://www.hello.good.com:80/hello/world?key=value&key2=value2", false},

            {"http://www.hello.bad.com:/hello/world/.good.com:80", false},
            {"http://www.hello.bad.com/hello/world/.good.com:80/hello/world", false},

            {"http://www.hello.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.hello.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    //random subdomain with trailing slash, same as testSix in different format
    @Test(dataProvider = "testSeven")
    public void testSevenRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.*.good.com/";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testEight")
    public Object[][] testEight() {
        return new Object[][] {
            {"http://www.hello.good.com", true},
            {"http://www.hello.good.com:80", true},
            {"http://www.hello.good.com:80/hello/world", true},

            {"https://www.hello.good.com", false},
            {"https://www.hello.good.com:443", false},
            {"https://www.hello.good.com:443/hello/world", false},

            {"http://www.hello.good.com/hello/world", true},
            {"http://www.hello.good.com/hello/world?key=value", false},
            {"http://www.hello.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.hello.good.com:80/hello/world", true},
            {"http://www.hello.good.com:80/hello/world?key=value", false},
            {"http://www.hello.good.com:80/hello/world?key=value&key2=value2", false},

            {"http://www.hello.bad.com:/hello/world/.good.com:80", false},
            {"http://www.hello.bad.com/hello/world/.good.com:80/hello/world", false},

            {"http://www.hello.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.hello.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    //random subdomain, default port, indeterminable length path, no question marks
    @Test(dataProvider = "testEight")
    public void testEightRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.*.good.com/*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testNine")
    public Object[][] testNine() {
        return new Object[][] {
            {"https://www.good.com", false},
            {"https://www.good.com/", false},
            {"https://www.good.com:443", false},
            {"https://www.good.com:443/", false},
            {"http://www.hello.good.com/path/", false},
            {"https://www.hello.good.com/path/", true},
            {"https://www.hello.good.com/", true},
            {"https://www.hello.good.com", true},
            {"https://www.good.com:443/path/", false},
            {"https://www.subdomain.good.com:443/path/", true},
            {"https://www.bad.com/.good.com:443/evil", false},
            {"https://www.hello.good.com:80/path/", true},
            {"https://www.bad.com:443/.good.com:443/evil", false}
        };
    }

    //random subdomain, random port random path of indeterminable length, no question marks
    @Test(dataProvider = "testNine")
    public void testNineRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "https://www.*.good.com:*/*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testTen")
    public Object[][] testTen() {
        return new Object[][] {
            {"https://www.one.good.com", false},
            {"https://www.one.good.com:80", false},
            {"https://www.good.com/bad", false},
            {"https://www.bad.com/.good.com", false},
            {"https://www.one.good.com/hello/blah", true},
            {"https://www.one.good.com/hello/world/blah", false},
            {"https://www.one.good.com/hello/world/also/more/blah", false}
        };
    }

    //testing single-level wildcards. After the (random) port, should have a single level before blah
    @Test(dataProvider = "testTen")
    public void testTenRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "https://www.*.good.com:*/*/blah";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testEleven")
    public Object[][] testEleven() {
        return new Object[][] {
            {"http://www.good.com", false},
            {"http://www.good.com:80", false},
            {"http://www.good.com:80/hello/world", false},

            {"https://www.good.com", false},
            {"https://www.good.com:443", false},
            {"https://www.good.com:443/hello/world", false},

            {"http://www.good.com/hello/world", false},
            {"http://www.good.com/hello/world?key=value", true},
            {"http://www.good.com/hello/world?key=value&key2=value2", true},

            {"http://www.good.com:80/hello/world", false},
            {"http://www.good.com:80/hello/world?", true},
            {"http://www.good.com:80/hello/world?key=value", true},
            {"http://www.good.com:80/hello/world?key=value&key2=value2", true},

            {"http://www.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    //allows for anything which contains a question mark in it, regardless of scheme, host or port
    @Test(dataProvider = "testEleven")
    public void testElevenRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "*?*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testTwelve")
    public Object[][] testTwelve() {
        return new Object[][] {
            {"http://www.good.com", false},
            {"http://www.good.com:80", false},
            {"http://www.good.com:80/hello/world", false},

            {"https://www.good.com", false},
            {"https://www.good.com:443", false},
            {"https://www.good.com:443/hello/world", false},

            {"http://www.good.com/hello/world", false},
            {"http://www.good.com/hello/world?key=value", true},
            {"http://www.good.com/hello/world?key=value&key2=value2", true},

            {"http://www.good.com:80/hello/world", false},
            {"http://www.good.com:80/hello/world?key=value", true},
            {"http://www.good.com:80/hello/world?key=value&key2=value2", true},
            {"http://www.good.com:80/hello/good/bye/world?key=value&key2=value2", true},

            {"http://www.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    //random subdomain, default port, any length path with question mark
    @Test(dataProvider = "testTwelve")
    public void testTwelveRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://*.good.com*/*?*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testThirteen")
    public Object[][] testThirteen() {
        return new Object[][] {
            {"http://www.good.com", false},
            {"http://www.good.com:80", false},
            {"http://www.good.com:80/hello/world", false},
            {"http://www.good.com:80/abc/world", true},
            {"http://www.good.com:80/abc", true},
            {"http://www.good.com/abc", true},

            {"https://www.good.com", false},
            {"https://www.good.com:443", false},

            {"http://www.good.com/hello/world", false},
            {"http://www.good.com/hello/world?key=value", false},
            {"http://www.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.good.com:80/hello/world", false},
            {"http://www.good.com:80/hello/world?key=value", false},
            {"http://www.good.com:80/abc/world?key=value&key2=value2", false},

            {"http://www.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    @Test(dataProvider = "testThirteen")
    public void testThirteenRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "/abc*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testFourteen")
    public Object[][] testFourteen() throws MalformedURLException {
        return new Object[][] {
            {"http://www.good.com", true},
            {"http://www.good.com:80", true},
            {"http://www.good.com:808", true},
            {"http://www.good.com:808/", true},
            {"http://www.good.com:808/asdf", false},
            {"http://www.good.com:80/hello/world", false},
            {"http://www.good.com:80/abc/world", false},
            {"http://www.good.com:80/abc", false},
            {"http://www.good.com/abc", false},

            {"https://www.good.com", false},
            {"https://www.good.com:443", false},

            {"http://www.good.com/hello/world", false},
            {"http://www.good.com/hello/world?key=value", false},
            {"http://www.good.com/hello/world?key=value&key2=value2", false},

            {"http://www.good.com:80/hello/world?key=value", false},
            {"http://www.good.com:80/abc/world?key=value&key2=value2", false},

            {"http://www.bad.com:80/hello/world/.good.com:80", false},
            {"http://www.bad.com:80/hello/world/.good.com:80/hello/world", false}
        };
    }

    //wildcard after port
    @Test(dataProvider = "testFourteen")
    public void testFourteenRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.good.com:80*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testFifteen")
      public Object[][] testFifteen() {
        return new Object[][] {
                {"https://www.good.com", false},
                {"https://www.good.com/", false},
                {"https://www.bad.good.com/", true},
                {"https://www.good.com:443", false},
                {"https://www.good.com:443/", false},
                {"http://www.hello.good.com/path/", false},
                {"https://www.hello.good.com/path/", true},
                {"https://www.good.com:443/path/", false},
                {"https://www.subdomain.good.com:443/path/", true},
                {"https://www.bad.com/.good.com:443/evil", false},
                {"https://www.hello.good.com:80/path/", false},
                {"https://www.bad.com:443/.good.com:443/evil", false}
        };
    }

    //any subdomain, any path
    @Test(dataProvider = "testFifteen")
    public void testFifteenRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "https://www.*.good.com:443/*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testSixteen")
    public Object[][] testSixteen() {
        return new Object[][] {
                {"https://www.good.com", false},
                {"https://www.good.com/", false},
                {"https://www.bad.good.com/", false},
                {"https://www.good.com:443", false},
                {"https://www.good.com:443/", false},
                {"http://www.hello.good.com/path/", false},
                {"https://www.hello.good.com/path/", false},
                {"https://www.good.com:443/path/", false},
                {"https://www.subdomain.good.com:443/path/", false},
                {"https://www.bad.com/.good.com:443/evil", false},
                {"http://www.hello.good.com:80/asdf123/path/blah", false},
                {"https://www.good.com:443/asdf123/blah", true},
                {"https://www.good.com:443/asdf123/456/blah", false},
                {"https://www.bad.com:443/.good.com:443/evil", false}
        };
    }

    //continuation after "asdf", but no additional path elements
    @Test(dataProvider = "testSixteen")
    public void testSixteenRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "https://www.good.com:443/asdf*/blah";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testSeventeen")
    public Object[][] testSeventeen() {
        return new Object[][] {
                {"http://www.good.com", false},
                {"http://www.good.com:80", false},
                {"http://www.good.com:80/hello/bob/hello/?bob=bob", false},
                {"http://www.good.com:80/hello/bob/hello?bob=bob", true},
                {"http://www.good.com:80/hello/bob?bob=bob", false},
                {"http://www.good.com:80/hello/world/", false},
                {"http://www.good.com:80/hello/world", false},
                {"http://www.good.com:80/hello/?bob=bob", false}
        };
    }

    //precisely two levels after port and before question mark
    @Test(dataProvider = "testSeventeen")
    public void testSeventeenRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.good.com:80/*/*/hello?*";
        performTestAndAssert(url, result, pattern);
    }

    //how to use special (single-level) matchers when also using a question mark:

    @DataProvider(name = "testEighteen")
    public Object[][] testEighteen() {
        return new Object[][] {
                {"http://www.good.com", false},
                {"http://www.good.com:80", false},
                {"http://www.good.com:80/hello/bob/hello?bob=bob", true},
                {"http://www.good.com:80/hello/bob/hello/?bob=bob", true},
                {"http://www.good.com:80/hello/bob/hello/again?bob=bob", false},
                {"http://www.good.com:80/hello/bob/hello/again/and/again?bob=bob", false},
                {"http://www.good.com:80/hello/bob/?bob=bob", true},
                {"http://www.good.com:80/hello/bob?bob=bob", true},
                {"http://www.good.com:80/hello?bob=bob", false},
                {"http://www.good.com:80/hello/world/", false},
                {"http://www.good.com:80/hello/world", false},
                {"http://www.good.com:80/hello/?bob=bob", false}
        };
    }

    //optional path-element before question mark, exactly two levels before that
    @Test(dataProvider = "testEighteen")
    public void testEighteenRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.good.com:80/-*-/-*-/-*-?*";
        performTestAndAssert(url, result, pattern);
    }

    @DataProvider(name = "testNineteen")
    public Object[][] testNineteen() {
        return new Object[][] {
                {"http://www.good.com", false},
                {"http://www.good.com:80", false},
                {"http://www.good.com:80/hello/bob/hello?bob=bob", true},
                {"http://www.good.com:80/hello/bob/hello/?bob=bob", true},
                {"http://www.good.com:80/hello/bob/hello/again?bob=bob", true},
                {"http://www.good.com:80/hello/bob/hello/again/and/again?bob=bob", true}, //seems odd - see testEighteen
                {"http://www.good.com:80/hello/bob/?bob=bob", true},
                {"http://www.good.com:80/hello/bob?bob=bob", false},
                {"http://www.good.com:80/hello?bob=bob", false},
                {"http://www.good.com:80/hello/world/", false},
                {"http://www.good.com:80/hello/world", false},
                {"http://www.good.com:80/hello/?bob=bob", false}
        };
    }

    //optional path-element before question mark, two (or more) levels before that (but not fewer)
    @Test(dataProvider = "testNineteen")
    public void testNineteenRunner(String url, boolean result) throws MalformedURLException {
        final String pattern = "http://www.good.com:80/*/*/*?*";
        performTestAndAssert(url, result, pattern);
    }

}
