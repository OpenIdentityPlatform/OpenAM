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
*/
package com.sun.identity.common.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ListValueValidatorTest {

    private final ListValueValidator validator = new ListValueValidator();

    @DataProvider(name = "data")
    public Object[][] data() {
        return new Object[][] {
                {"[0]=ALL", true},
                {"[ 0]=ALL", true},
                {"[0 ]=ALL", true},
                {"[ 0 ]=ALL", true},
                {"[1]=ALL", true},
                {"[99]=ALL", true},
                {"[-1]=ALL", false},
                {"asdf=NONE", false},
                {"[asdf]=ALL", false},
                {"[asdf]=", false},
                {"=ALL", false},
                {"[]=ALL", false},
                {"[]=", false},
                {"[key_and_or_value_contains_=_sign] ==", false}
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
    public void checkSequentialSetOnlyContainsOneConfigPerApp() {
        //given
        Set<String> set = new HashSet<>();
        set.add("[0]=ALL");
        set.add("[1]=NONE");

        //when
        boolean result = validator.validate(set);

        //then
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void checkMissingNumberInSequentialSetAlsoSucceeds() {
        //given
        Set<String> set = new HashSet<>();
        set.add("[5]=");
        set.add("[3]=ALL");
        set.add("[1]=NONE");

        //when
        boolean result = validator.validate(set);

        //then
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void checkSetOnlyContainsOneConfigPerApp() {
        //given
        Set<String> set = new HashSet<>();
        set.add("[0]=ALL");
        set.add("[0]=NONE");

        //when
        boolean result = validator.validate(set);

        //then
        assertThat(result).isEqualTo(false);
    }

    @Test
    public void checkEmptySetPasses() {
        //given
        Set<String> set = new HashSet<>();

        //when
        boolean result = validator.validate(set);

        //then
        assertThat(result).isEqualTo(true);
    }

}
