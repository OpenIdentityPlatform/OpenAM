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
 * Copyright 2015 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RealmUtilsTest {

    @DataProvider
    private Object[][] concatenateRealmPathData() {
        return new Object[][]{
                {null, null, "/"},
                {null, "subrealm", "/subrealm"},
                {null, "/subrealm", "/subrealm"},
                {"/", null, "/"},
                {"/realm", null, "/realm"},
                {"realm", null, "/realm"},
                {"/", "subrealm", "/subrealm"},
                {"/", "/subrealm", "/subrealm"},
                {"realm", "subrealm", "/realm/subrealm"},
                {"/realm", "subrealm", "/realm/subrealm"},
                {"realm", "/subrealm", "/realm/subrealm"},
        };
    }

    @Test(dataProvider = "concatenateRealmPathData")
    public void shouldConcatenateRealmPath(String parentRealm, String subRealm, String expectedRealm) {
        assertThat(RealmUtils.concatenateRealmPath(parentRealm, subRealm)).isEqualTo(expectedRealm);
    }

    @DataProvider
    private Object[][] sameOrSubRealmData() {
        return new Object[][]{
                {"/", "/", true},
                {"/", "/realm", true},
                {"/", "/realm/sub", true},
                {"/realm", "/realm", true},
                {"/realm", "/REALM", true},
                {"/realm", "/realm/", true},
                {"/realm/", "/realm", true},
                {"/realm", "/realm/sub", true},
                {"/realm", "/realm/sub/deeper", true},
                {"/realm", "/", false},
                {"/realm", "/other", false},
                {"/realm", "/realm-other", false},
                {"/realm", "/realmsub", false},
                {"/realm/sub", "/realm", false},
                {"/realm", null, false},
                {null, "/realm", false},
                {"", "/realm", false},
                {"/realm", "  ", false},
                {"/realm", "/realm/../other", false},
                {"/realm", "/realm/sub/../../other", false},
                {"/", "/realm/../../etc", false},
                {"/", "/..", false},
                {"/realm/..", "/realm", false},
                {"/realm", "/realm/..sub", true},
                {"/realm", "/realm/a..b", true},
                {"/realm", "realm/sub", true},
        };
    }

    @Test(dataProvider = "sameOrSubRealmData")
    public void shouldDetermineWhetherARealmIsTheSameOrASubRealm(String parentRealm, String realm, boolean expected) {
        assertThat(RealmUtils.isSameOrSubRealm(parentRealm, realm)).isEqualTo(expected);
    }

    @DataProvider
    private Object[][] parentPathSegmentData() {
        return new Object[][]{
                {null, false},
                {"", false},
                {"/", false},
                {"/realm", false},
                {"/realm/sub", false},
                {"/realm/..sub", false},
                {"/realm/sub..", false},
                {"/realm/a..b", false},
                {"/..", true},
                {"..", true},
                {"/realm/../other", true},
                {"/realm/ .. /other", true},
                {"/realm/sub/..", true},
        };
    }

    @Test(dataProvider = "parentPathSegmentData")
    public void shouldDetectAParentPathSegment(String realm, boolean expected) {
        assertThat(RealmUtils.containsParentPathSegment(realm)).isEqualTo(expected);
    }
}
