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

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FilterModeValueValidatorTest {

    private final FilterModeValueValidator validator = new FilterModeValueValidator();

    @DataProvider(name = "data")
    public Object[][] data() {
        return new Object[][] {
                {"[asdf]=ALL", true},
                {"[asdf]=SSO_ONLY", true},
                {"[asdf]=URL_POLICY", true},
                {"[asdf]=J2EE_POLICY", true},
                {"[asdf]=NONE", true},
                {"asdf=NONE", false},
                {"asdf=", false},
                {"[asdf]=", false},
                {"[asdf[]=NONE", false},
                {"[asdf]]=NONE", false},
                {"[asdf[asdf]]=NONE", false},
                {"=ALL", true},
                {"=NONE", true},
                {"=SSO_ONLY", true},
                {"=URL_POLICY", true},
                {"=J2EE_POLICY", true},
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

}
