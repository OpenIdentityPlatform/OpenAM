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
* Copyright 2016 ForgeRock AS.
* Portions Copyright 2026 3A Systems, LLC.
*/
package com.sun.identity.common.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class GlobalMapValueValidatorTest {

    private final GlobalMapValueValidator validator = new GlobalMapValueValidator();

    @DataProvider(name = "data")
    public Object[][] data() {
        return new Object[][] {
                {"[asdf]=ALL", true},
                {"asdf=NONE", true},
                {"asdf=", true},
                {"[asdf]=", true},
                {"[asdf[]=NONE", false},
                {"[asdf]]=NONE", false},
                {"[asdf[asdf]]=NONE", false},
                {"=ALL", true},
                {"[]=", true},
                {"[key_and_or_value_contains_=_sign] ==", true},
                // white space inside the key, which is what the key expression's two competing
                // quantifiers exist to allow
                {"[a b]=v", true},
                {"[ a b ] = v ", true},
                {"[a ]=v", true},
                {"[ a]=v", true},
                // and a key of nothing but whitespace, which the first of them keeps out
                {"[  ]=ALL", false}
        };
    }

    @Test(dataProvider = "data")
    public void checkCorrectness(String name, boolean expected) {
        //given

        //when
        boolean result = validator.validate(Collections.singleton(name));

        //then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void checkSetOnlyContainsOneConfigPerApp() {
        //given
        Set<String> set = new HashSet<>();
        set.add("[asdf]=ALL");
        set.add("[asdf]=NONE");

        //when
        boolean result = validator.validate(set);

        //then
        assertThat(result).isEqualTo(false);
    }

    @Test
    public void checkSetOnlyContainsOneGlobalConfig() {
        //given
        Set<String> set = new HashSet<>();
        set.add("=ALL");
        set.add("=NONE");

        //when
        boolean result = validator.validate(set);

        //then
        assertThat(result).isEqualTo(false);
    }

    @Test
    public void checkEmptySetFails() {
        //given
        Set<String> set = new HashSet<>();

        //when
        boolean result = validator.validate(set);

        //then
        assertThat(result).isEqualTo(false);
    }

    /**
     * This validator folds the key expression <code>MapValueValidator</code> shares into an
     * alternation of its own, so the bound on matching a key that is never closed has to hold
     * through that pattern too.
     */
    @Test
    public void boundsTheCostOfAnUnterminatedKey() {
        //given
        String value = "[" + "a".repeat(64000);

        //when
        long startedAt = System.nanoTime();
        boolean result = validator.validate(Collections.singleton(value));
        long elapsedMillis = (System.nanoTime() - startedAt) / 1000000L;

        //then
        assertThat(result).isFalse();
        assertThat(elapsedMillis).as("matching an unterminated key has to be bounded")
                .isLessThan(5000L);
    }

}
