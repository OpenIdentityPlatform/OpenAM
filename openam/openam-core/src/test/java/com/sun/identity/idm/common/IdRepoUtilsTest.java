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
package com.sun.identity.idm.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import org.testng.annotations.Test;

public class IdRepoUtilsTest {

    @Test
    public void filteringPasswordWorksWithDefaultSettings() {
        Map<String, Set<String>> values = new HashMap<String, Set<String>>();
        values.put("userpassword2", asSet("test1"));
        values.put("auserpassword", asSet("test2"));
        values.put("userpassword", asSet("test3"));
        values.put("aunicodepwd", asSet("test4"));
        values.put("unicodepwd2", asSet("test5"));
        values.put("unicodepwd", asSet("test6"));
        Map<String, ?> result = IdRepoUtils.getAttrMapWithoutPasswordAttrs(values, null);
        assertThat(result.values()).containsOnly(asSet("test1"), asSet("test2"), asSet("test4"), asSet("test5"),
                "xxx...");
    }

    @Test
    public void filteringPasswordWorksCaseInsensitively() {
        Map<String, Set<String>> values = new HashMap<String, Set<String>>();
        values.put("userpassword", asSet("test1"));
        values.put("uSeRpAsSwOrD", asSet("test2"));
        values.put("userPassword", asSet("test3"));
        values.put("UnIcOdEpWd", asSet("test4"));
        values.put("unicodePwd", asSet("test5"));
        values.put("unicodepwd", asSet("test6"));
        Map<String, ?> result = IdRepoUtils.getAttrMapWithoutPasswordAttrs(values, null);
        assertThat(result.values()).containsOnly("xxx...");
    }

    @Test
    public void filteringPasswordWorksWithExtraPasswordParameters() {
        Map<String, Set<String>> values = new HashMap<String, Set<String>>();
        values.put("hello", asSet("test1"));
        values.put("world", asSet("test2"));
        values.put("userpassword", asSet("test3"));
        values.put("unicodepwd", asSet("test4"));
        values.put("hellp", asSet("test5"));
        values.put("worlt", asSet("test6"));
        Map<String, ?> result = IdRepoUtils.getAttrMapWithoutPasswordAttrs(values, asSet("HELLO", "WORLD"));
        assertThat(result.values()).containsOnly(asSet("test5"), asSet("test6"), "xxx...");
    }
}
