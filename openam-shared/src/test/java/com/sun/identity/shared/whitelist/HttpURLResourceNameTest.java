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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

public class HttpURLResourceNameTest {

    public HttpURLResourceName resourceName = new HttpURLResourceName();

    @Test
    public void checkLevels() {
        String pattern = "http://www.google.com:80/blah/*";
        runner("http://www.google.com:80/blah/one/moo", pattern, true);
        runner("http://www.google.com:80/blah/one/two/moo", pattern, true);
    }

    @Test
    public void checkDoubleInternal() {
        String pattern = "http://www.google.com:80/blah/*/*/moo";
        runner("http://www.google.com:80/blah/one/moo", pattern, false);
        runner("http://www.google.com:80/blah/one/two/moo", pattern, true);
    }

    @Test
    public void checkSingleLevelInternal() {
        String pattern = "http://www.google.com:80/blah/-*-/moo";
        runner("http://www.google.com:80/blah/one/moo", pattern, true);
        runner("http://www.google.com:80/blah/one/two/moo", pattern, false);
    }

    @Test
    public void checkInternal() {
        String pattern = "http://www.google.com:80/blah/*/moo";
        runner("http://www.google.com:80/blah/one/moo", pattern, true);
        runner("http://www.google.com:80/blah/one/two/moo", pattern, true);
    }

    private void runner(String url, String pattern, boolean expected) {
        ResourceMatch result = resourceName.compare(url, pattern, true);

        if (result.equals(ResourceMatch.WILDCARD_MATCH) || result.equals(ResourceMatch.EXACT_MATCH) ||
                result.equals(ResourceMatch.SUPER_RESOURCE_MATCH)) {
            assertTrue(expected);
        } else {
            assertFalse(expected);
        }
    }

}
