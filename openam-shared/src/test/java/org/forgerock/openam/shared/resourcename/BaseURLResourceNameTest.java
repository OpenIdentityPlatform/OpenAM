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

package org.forgerock.openam.shared.resourcename;

import com.sun.identity.shared.debug.Debug;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import static org.forgerock.openam.shared.resourcename.BaseURLResourceNameTest.ResourceMatch.*;

public class BaseURLResourceNameTest {

    private TestBaseURLResourceName resourceName = new TestBaseURLResourceName();

    @Test
    public void testWildcardMatchCompare() throws Exception {
        match(WILDCARD_MATCH, "http://example.com:80/fred/index.html", "http*://*example.com:*/fred/*", true);
        match(WILDCARD_MATCH, "http://www.example.com:80/fred/index.html", "http*://*example.com:*/fred/*", true);
        match(WILDCARD_MATCH, "http://www.google.com:80/asdf/hello/blah/wibble/asdf/blah",
                "http://www.google.com:80/*/blah/wibble/*/blah", true);
        match(WILDCARD_MATCH, "http://www.google.com.net", "http://www.google.com*", true);
        match(WILDCARD_MATCH, "http://www.google.com:80/", "http://www.google.com:*", true);
        match(WILDCARD_MATCH, "http://www.google.com.co.uk", "http://www.google.com*", true);
        match(WILDCARD_MATCH, "http://www.google.com.co.uk:80", "http://www.google.com*", true);
        match(WILDCARD_MATCH, "http://www.google.com.co.uk:80/", "http://www.google.com*", true);
        match(WILDCARD_MATCH, "http://www.google.com.co.uk:80/blah", "http://www.google.com*", true);
        match(WILDCARD_MATCH, "http://example.com/index.html", "http*://example.com/index.html", true);
        match(WILDCARD_MATCH, "http://www.google.com:80/blah?boo=bing", "http://www.google.com:80/*", true);
        match(WILDCARD_MATCH, "http://www.google.com:80/123/index.html", "http://*.com:80/123/index.html", true);
    }

    @Test
    public void testExactMatchCompare() throws Exception {
        match(EXACT_MATCH, "http://example.com:80/fred/index.html", "http://example.com:80/fred/index.html", true);
        match(EXACT_MATCH, "http://example.com:80/fred/index.html", "http://example.com:80/fred/index.html", false);
    }

    @Test
    public void testSuperResourceMatchCompare() throws Exception {
        match(SUPER_RESOURCE_MATCH, "http://example.com:80/fred/index.html", "http*://*example.com:*/fred", true);
        match(SUPER_RESOURCE_MATCH, "http://example.com:8080/foo/index.html", "http://example.com:8080/", true);
        match(SUPER_RESOURCE_MATCH, "http://example.com:8080/foo/index.html", "http://example.com:8080", true);
        match(SUPER_RESOURCE_MATCH, "http://example.com:8080/foo*", "http://example.com:8080/", true);
    }

    @Test
    public void testSubResourceMatchCompare() throws Exception {
        match(SUB_RESOURCE_MATCH, "http://example.com:80/fred", "http*://*example.com:*/fred/devil", true);
        match(SUB_RESOURCE_MATCH, "http://example.com:8080/", "http://example.com:8080/foo/index.html", true);
        match(SUB_RESOURCE_MATCH, "http://example.com:8080", "http://example.com:8080/foo/index.html", true);
        match(SUB_RESOURCE_MATCH, "http://example.com:8080/", "http://example.com:8080/foo*", true);
    }

    @Test
    public void testNoMatchCompare() throws Exception {
        match(NO_MATCH, "http://example.com/private/index.html", "http*://example.com:*/index.html", true);
        match(NO_MATCH, "http://example.com:80/private/index.html", "http*://example.com:*/index.html", true);
        match(NO_MATCH, "http://example.com:80/private/fred/index.html", "http*://*example.com:*/fred/*", true);
        match(NO_MATCH, "http://hello.world:80/hacked.example.com:80/index.html", "http://*.example.com:80/index.html", true);
        match(NO_MATCH, "https://example.com", "http://ex*mple.com", true);
        match(NO_MATCH, "http://example.com:80/foo/asdf/bar", "http://example.com:80/fred*/asdf/bar", true);
        match(NO_MATCH, "http://example.com/private/index.html", "http*://example.com:*/index.html", true);
    }

    private void match(ResourceMatch expected, String requestResource, String targetResource, boolean wildcard) {
        ResourceMatch result = resourceName.compare(requestResource, targetResource, wildcard);
        assertEquals(result, expected);
    }

    enum ResourceMatch {
        NO_MATCH, EXACT_MATCH, WILDCARD_MATCH, SUB_RESOURCE_MATCH, SUPER_RESOURCE_MATCH;
    }

    private static class TestBaseURLResourceName extends BaseURLResourceName<ResourceMatch, Exception> {

        TestBaseURLResourceName() {
            super(Debug.getInstance("test"), EXACT_MATCH, NO_MATCH, SUB_RESOURCE_MATCH, SUPER_RESOURCE_MATCH,
                    WILDCARD_MATCH);
        }

        @Override
        protected Exception constructResourceInvalidException(Object[] args) {
            return new Exception();
        }
    }
}