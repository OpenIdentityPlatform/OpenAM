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
package org.forgerock.openam.audit.validation;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PositiveIntegerValidatorTest {

    private final PositiveIntegerValidator validator = new PositiveIntegerValidator();

    @DataProvider(name = "data")
    public Object[][] data() {
        return new Object[][] {
                {"1", true},
                {String.valueOf(Integer.MAX_VALUE), true},
                {String.valueOf(Integer.MAX_VALUE + 1), false}, //overflow check
                {"0", false},
                {"-1", false}
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


}
