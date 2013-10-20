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
 * Copyright 2013 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;


/**
 * Unit tests for Subtree wildcard policy evaluation
 *
 * @author Travis
 */
public class SubtreeWildcardTest {
    private PrefixResourceName comparator;

    @BeforeMethod
    public void initialize() {
        comparator = new PrefixResourceName();
    }

    @Test
    public void testEngine() {

        //check that a subresource match with a wildcard works where the last
        //token of the requested resource does not exist in the target resource
        verifySubtreeWildcardFunctionality("http://hello.abc.world:80/okay/yay",
                "http://hello.*.world:80/okay/*/tomorrow",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check that the newly implemented stuff doesn't affect wildcard matches
        verifySubtreeWildcardFunctionality("http://host.com:80",
                "http://*:80",
                ResourceMatch.WILDCARD_MATCH);

        //check that the newly implemented stuff doesn't affect wildcard matches
        verifySubtreeWildcardFunctionality("http://host.com:80/bobo",
                "http*://host.com:80/bobo",
                ResourceMatch.WILDCARD_MATCH);

        //check that the newly implemented stuff doesn't affect wildcard matches
        verifySubtreeWildcardFunctionality("http://host.com:80/dodo/bobo",
                "http*://host.com:80/dodo/*",
                ResourceMatch.WILDCARD_MATCH);

        //check that the newly implemented stuff doesn't affect wildcard matches
        verifySubtreeWildcardFunctionality("/example/of/something/uri/like",
                "/example/*/something/uri/like",
                ResourceMatch.WILDCARD_MATCH);

        //check wildcard string
        verifySubtreeWildcardFunctionality("/example/of/something/uri/like",
                "/example/*",
                ResourceMatch.WILDCARD_MATCH);

        //check multiple paths that have the same value
        verifySubtreeWildcardFunctionality("/example/of/something/uri/like/foo/like",
                "/example/*/something/uri/like/foo/like/bar/like",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check multiple paths that have the same value yet don't match
        verifySubtreeWildcardFunctionality("/example/of/something/uri/like/foo/like",
                "/example/*/something/uri/like/like/foo/bar/like",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check a subresource with a wildcard terminating character
        verifySubtreeWildcardFunctionality("/example/of/something/uri/like",
                "/example/*/something/uri/like/*",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check something other than a url
        verifySubtreeWildcardFunctionality("another/example/of/something/uri/like",
                "*/example/*/uri/like/with/subrealms",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //this is not a subresource
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:8080/sub/resource",
                "http://example.forgerock.com:8080",
                ResourceMatch.SUPER_RESOURCE_MATCH);

        //check wildcard in a partially fleshed out protocol
        verifySubtreeWildcardFunctionality("https://example.forgerock.com:443",
                "http*://*.forgerock.com:*/examples/index.html",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check wildcard in the middle of a port and at the end of the protocol
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:8080",
                "http*://*.forgerock.com:8*0/examples/index.html",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check wildcard in subdomain
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:80",
                "http://*.forgerock.com:80/examples/index.html",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check wildcard in protocol
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:8080",
                "*://example.forgerock.com:8080/stuff/index.html",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check wildcard in port
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:80",
                "http://example.forgerock.com:*/otherStuff/index.html",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check wildcard in everything
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:80",
                "*://*:*/MoreStuff/index.html",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check wildcard in path
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:80",
                "http://example.forgerock.com:80/*/index.html",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check wildcard in all the subdomains
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:80",
                "http://*.*.*:80/jsps/index.html",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check partial wildcards in every part
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:80",
                "h*://*ple.forge*.com:8*0/other*/index*",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check a non-match
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:80",
                "http://*.shouldnt.work.com:80/examples/index.html",
                ResourceMatch.SUB_RESOURCE_MATCH);

        //check another non-match
        verifySubtreeWildcardFunctionality("http://example.forgerock.com:80",
                "*://*.*.*:8080/jsps/index.html",
                ResourceMatch.SUB_RESOURCE_MATCH);
    }


    public void verifySubtreeWildcardFunctionality(String resource,
                                                   String target,
                                                   ResourceMatch rm) {

        assertThat(comparator.compare(resource, target, true)).isEqualTo(rm);
    }
}
