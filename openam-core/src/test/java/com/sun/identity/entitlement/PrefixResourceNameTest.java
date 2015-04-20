/*
 * Copyright 2014 ForgeRock AS.
 *
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
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Exercises the behaviour of {@link PrefixResourceName}.
 */
public class PrefixResourceNameTest {

    private ResourceName resourceName;

    @BeforeClass
    public void setUp() {
        resourceName = new PrefixResourceName();
        resourceName.initialize(new HashMap<String, String>());
    }

    @Test
    public void normaliseHandlesTrailingSlashes() throws EntitlementException {
        assertThat(resourceName.canonicalize("http://a.b.c:80///ab///cd///")).isEqualTo("http://a.b.c:80/ab/cd/");
        assertThat(resourceName.canonicalize("http://a.b.c:80///ab///cd")).isEqualTo("http://a.b.c:80/ab/cd");
        assertThat(resourceName.canonicalize("//a.b.c///ab///cd///")).isEqualTo("/a.b.c/ab/cd/");
        assertThat(resourceName.canonicalize("//a.b.c///ab///cd")).isEqualTo("/a.b.c/ab/cd");
    }

    @Test
    public void normalizationOnlyAffectsPath() throws Exception {
        assertThat(resourceName.canonicalize("http://a.b..c:80///ab/cd?index=hello/./world//../"))
                .isEqualTo("http://a.b..c:80/ab/cd?index=hello/./world//../");
        assertThat(resourceName.canonicalize("http://a.b.c:80/./ab//cd/../?index=hello/./world//../"))
                .isEqualTo("http://a.b.c:80/ab/?index=hello/./world//../");
        assertThat(resourceName.canonicalize("http://a.b.c:80/ab/../cd/.././ab///cd?index=hello/./world//../"))
                .isEqualTo("http://a.b.c:80/ab/cd?index=hello/./world//../");
    }

    @Test
    public void normalizationShouldRemovePathNavigationSegments() throws Exception {
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/../zz"))
                .isEqualTo("http://w.e.c:80/zz");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/../zz?qq"))
                .isEqualTo("http://w.e.c:80/zz?qq");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/./../zz"))
                .isEqualTo("http://w.e.c:80/zz");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/./../zz?qq"))
                .isEqualTo("http://w.e.c:80/zz?qq");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/bb/cc/../cc/../../bb/../../zz"))
                .isEqualTo("http://w.e.c:80/zz");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/bb/cc/../cc/../../bb/../../zz?qq"))
                .isEqualTo("http://w.e.c:80/zz?qq");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/bb/./../bb/.././bb/../../zz"))
                .isEqualTo("http://w.e.c:80/zz");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/bb/./../bb/.././bb/../../zz?qq"))
                .isEqualTo("http://w.e.c:80/zz?qq");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/bb/././../bb/././../././../././zz"))
                .isEqualTo("http://w.e.c:80/zz");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/bb/././../bb/././../././../././zz?qq"))
                .isEqualTo("http://w.e.c:80/zz?qq");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/../aa/ee.ff"))
                .isEqualTo("http://w.e.c:80/aa/ee.ff");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/../aa/ee.ff?qq"))
                .isEqualTo("http://w.e.c:80/aa/ee.ff?qq");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/./.././aa/ee.ff"))
                .isEqualTo("http://w.e.c:80/aa/ee.ff");
        assertThat(resourceName.canonicalize("http://w.e.c:80/aa/./.././aa/ee.ff?qq"))
                .isEqualTo("http://w.e.c:80/aa/ee.ff?qq");
    }

    @Test
    public void verifyNullMatchBehaviour() {
        assertThat(resourceName.compare(null, null, true)).isEqualTo(ResourceMatch.EXACT_MATCH);
        assertThat(resourceName.compare(null, "abc", true)).isEqualTo(ResourceMatch.NO_MATCH);
        assertThat(resourceName.compare("abc", null, true)).isEqualTo(ResourceMatch.NO_MATCH);
    }

    @Test
    public void verifyTrailingSlashMatchBehaviour() {
        assertThat(resourceName.compare("/abc/def", "/abc/def", true)).isEqualTo(ResourceMatch.EXACT_MATCH);
        assertThat(resourceName.compare("/abc/def", "/abc/def/", true)).isEqualTo(ResourceMatch.SUB_RESOURCE_MATCH);
        assertThat(resourceName.compare("/abc/def/", "/abc/def", true)).isEqualTo(ResourceMatch.SUPER_RESOURCE_MATCH);
    }

    @Test
    public void verifyMultiLevelWildcardMatchBehaviour() {
        assertThat(resourceName.compare("/abc/", "/abc/*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/def", "/abc/*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/def/", "/abc/*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/def/hij", "/abc/*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);

        assertThat(resourceName.compare("/abc", "/abc*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abcdef", "/abc*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/def", "/abc*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/def/", "/abc*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/def/hij", "/abc*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);

        assertThat(resourceName.compare("/ac", "/a*c", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc", "/a*c", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/a/c", "/a*c", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/ab/dc", "/a*c", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
    }

    @Test
    public void verifySingleLevelWildcardMatchBehaviour() {
        // Note: the single level wildcard is a lazy wildcard in that it does not
        // consume the forward slash marking the end of level its matching.
        assertThat(resourceName.compare("/abc/", "/abc/-*-", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/def", "/abc/-*-", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/def/", "/abc/-*-", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/def/hij", "/abc/-*-", true)).isEqualTo(ResourceMatch.SUPER_RESOURCE_MATCH);

        assertThat(resourceName.compare("/abc", "/abc-*-", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abcdef", "/abc-*-", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/def", "/abc-*-", true)).isEqualTo(ResourceMatch.SUPER_RESOURCE_MATCH);
        assertThat(resourceName.compare("/abc/def/", "/abc-*-", true)).isEqualTo(ResourceMatch.SUPER_RESOURCE_MATCH);
        assertThat(resourceName.compare("/abc/def/hij", "/abc-*-", true)).isEqualTo(ResourceMatch.SUPER_RESOURCE_MATCH);

        assertThat(resourceName.compare("/ac", "/a-*-c", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc", "/a-*-c", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/ac/", "/a-*-c", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc/d", "/a-*-c", true)).isEqualTo(ResourceMatch.SUPER_RESOURCE_MATCH);
        assertThat(resourceName.compare("/a/c", "/a-*-c", true)).isEqualTo(ResourceMatch.NO_MATCH);
        assertThat(resourceName.compare("/ab/dc", "/a-*-c", true)).isEqualTo(ResourceMatch.NO_MATCH);
    }

    @Test
    public void ignoreWildcardMatchBehaviour() {
        assertThat(resourceName.compare("/axyzc/", "/a-*-c/", false)).isEqualTo(ResourceMatch.NO_MATCH);
        assertThat(resourceName.compare("/a-*-c/", "/a-*-c/", false)).isEqualTo(ResourceMatch.EXACT_MATCH);
        assertThat(resourceName.compare("/ax/yc/", "/a*c/", false)).isEqualTo(ResourceMatch.NO_MATCH);
        assertThat(resourceName.compare("/a*c/", "/a*c/", false)).isEqualTo(ResourceMatch.EXACT_MATCH);
    }

    @Test
    public void verifyQueryStringMatchBehaviour() {
        assertThat(resourceName.compare("/abc?", "/abc?*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc?def", "/abc?*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abc?", "/abc*?*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
        assertThat(resourceName.compare("/abcdef?a=b", "/abc*?*", true)).isEqualTo(ResourceMatch.WILDCARD_MATCH);
    }

}
