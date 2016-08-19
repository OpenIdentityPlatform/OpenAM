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
package org.forgerock.openam.utils;

import static org.assertj.core.api.Assertions.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RecoveryCodeGeneratorTest {

    static final Pattern ALPHANUMERIC = Pattern.compile("[0-9a-zA-Z-]+");
    static final Pattern HEX = Pattern.compile("[0-9a-f-]+");

    private RecoveryCodeGenerator recoveryCodeGenerator;

    @BeforeMethod
    public void theSetUP() { // you need this
       this.recoveryCodeGenerator = new RecoveryCodeGenerator(new SecureRandom());
    }


    @Test
    public void shouldGenerateAlphanumericRandomCode() throws CodeException {
        //given

        //when
        String[] codes = recoveryCodeGenerator.generateCodes(100, Alphabet.ALPHANUMERIC, false);

        //then
        for (String code : codes) {
            assertThat(ALPHANUMERIC.matcher(code).matches()).isTrue();
        }
    }

    @Test
    public void shouldGenerateHexRandomCode() throws CodeException {
        //given

        //when
        String[] codes = recoveryCodeGenerator.generateCodes(100, Alphabet.HEX_DIGITS, true);

        //then
        for (String code : codes) {
            assertThat(HEX.matcher(code).matches()).isTrue();
        }
    }

    @Test
    public void shouldGenerateDelimitedHexRandomCode() throws CodeException {
        //given

        //when
        String[] codes = recoveryCodeGenerator.generateDelimitedCodes(100, Alphabet.HEX_DIGITS, '-', true, 4, 4);

        //then
        for (String code : codes) {
            assertThat(HEX.matcher(code).matches()).isTrue();
            assertThat(code.charAt(4)).isEqualTo('-');
            assertThat(code.length()).isEqualTo(9);
        }
    }

    @Test
    public void shouldGenerateDelimitedHexRandomCodeWithSpecifics() throws CodeException {
        //given
        Map<Integer, Character> map = new HashMap<>();
        map.put(8, 'L');
        map.put(0, 'M');

        //when
        String[] codes =
                recoveryCodeGenerator.generateDelimitedCodesWithSpecifics(100, Alphabet.HEX_DIGITS, '-', map, true, 4, 4);

        //then
        for (String code : codes) {
            assertThat(HEX.matcher(code).matches());
            assertThat(code.charAt(4)).isEqualTo('-');
            assertThat(code.charAt(8)).isEqualTo('L');
            assertThat(code.charAt(0)).isEqualTo('M');
        }
    }

    @Test
    public void shouldGenerateDuplicateCodes() throws CodeException {
        //given
        Map<Integer, Character> map = new HashMap<>();
        map.put(0, 'L');

        //when
        String[] codes =
                recoveryCodeGenerator.generateDelimitedCodesWithSpecifics(2,  new TestAlphabet(), '-', map, true, 1);

        //then
        for (String code : codes) {
            assertThat(code).isEqualTo("L");
        }
    }

    @Test
    public void shouldGenerateDuplicateCodes2() throws CodeException {
        //given

        //when
        String[] codes =
                recoveryCodeGenerator.generateDelimitedCodes(2, new TestAlphabet(), '-', true, 1, 1);

        //then
        for (String code : codes) {
            assertThat(code).isEqualTo("A-A");
        }
    }

    @Test (expectedExceptions = CodeException.class)
    public void shouldErrorGeneratingDuplicateCodes() throws CodeException {
        //given
        Map<Integer, Character> map = new HashMap<>();
        map.put(0, 'L');

        //when
        recoveryCodeGenerator.generateDelimitedCodesWithSpecifics(2, Alphabet.HEX_DIGITS, '-', map, false, 1);

        //then
    }

    @Test (expectedExceptions = CodeException.class)
    public void shouldErrorGeneratingDuplicateCodes2() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateDelimitedCodes(2, new TestAlphabet(), '-', false, 1);

        //then
    }

    @Test (expectedExceptions = CodeException.class)
    public void shouldErrorGeneratingDuplicateCodes3() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateCodes(2, new TestAlphabet(), false);

        //then
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorRequestedSizeTooSmall() throws CodeException {
        //given
        Map<Integer, Character> map = new HashMap<>();
        map.put(0, 'L');

        //when
        recoveryCodeGenerator.generateDelimitedCodesWithSpecifics(0,  new TestAlphabet(), '-', map, true, 1);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorRequestedSizeTooSmall2() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateDelimitedCodes(0,  new TestAlphabet(), '-', true, 1);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorRequestedSizeTooSmall3() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateCodes(0,  new TestAlphabet(), true);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorRequestedSizeTooSmall4() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateCodes(0, new TestAlphabet(), 10, true);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorRequestedLengthTooSmall() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateCodes(10, new TestAlphabet(), 0, true);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorRequestedLengthTooSmall2() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateCode(new TestAlphabet(), 0);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldErrorNullAlphabet() throws CodeException {
        //given
        Map<Integer, Character> map = new HashMap<>();
        map.put(0, 'L');

        //when
        recoveryCodeGenerator.generateDelimitedCodesWithSpecifics(10, null, '-', map, true, 1);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldErrorNullAlphabet2() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateDelimitedCodes(10, null, '-', true, 1);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldErrorNullAlphabet3() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateCodes(10, null, true);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldErrorNullAlphabet4() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateCode(null, 10);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldErrorNullAlphabet5() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateDelimitedCode(null, '-', 1);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldErrorNullAlphabet6() throws CodeException {
        //given
        Map<Integer, Character> map = new HashMap<>();
        map.put(0, 'L');

        //when
        recoveryCodeGenerator.generateDelimitedCodeWithSpecifics(null, '-', map, 1);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorNullGroups() throws CodeException {
        //given
        Map<Integer, Character> map = new HashMap<>();
        map.put(0, 'L');

        //when
        recoveryCodeGenerator.generateDelimitedCodesWithSpecifics(10,  new TestAlphabet(),'-', map, true);
    }


    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorNullGroups2() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateDelimitedCodes(10,  new TestAlphabet(), '-', true);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorNullGroups3() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateDelimitedCode( new TestAlphabet(), '-');
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorGroupsUndefined() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateDelimitedCodes(10,  new TestAlphabet(), '-', true);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorGroupsUndefined2() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateDelimitedCode( new TestAlphabet(), '-');
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorGroupTooSmall() throws CodeException {
        //given
        Map<Integer, Character> map = new HashMap<>();
        map.put(0, 'L');

        //when
        recoveryCodeGenerator.generateDelimitedCodesWithSpecifics(10,  new TestAlphabet(), '-', map, true, 1, 0);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorGroupTooSmall2() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateDelimitedCodes(10,  new TestAlphabet(), '-', true, 1, 0);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorGroupTooSmall3() throws CodeException {
        //given

        //when
        recoveryCodeGenerator.generateDelimitedCode( new TestAlphabet(), '-', 1, 0);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorNullMap() throws CodeException {
        recoveryCodeGenerator.generateDelimitedCodesWithSpecifics(0,  new TestAlphabet(),'-', null, true, 1);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorMapKeyInvalid() throws CodeException {
        //given
        Map<Integer, Character> map = new HashMap<>();
        map.put(-1, 'L');

        //when
        recoveryCodeGenerator.generateDelimitedCodesWithSpecifics(0,  new TestAlphabet(), '-', map, true, 1);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldErrorMapValueInvalid() throws CodeException {
        //given
        Map<Integer, Character> map = new HashMap<>();
        map.put(-1, null);

        //when
        recoveryCodeGenerator.generateDelimitedCodesWithSpecifics(0, new TestAlphabet(), '-', map, true, 1);
    }

    private class TestAlphabet implements CodeGeneratorSource {

        @Override
        public String getChars() {
            return "A";
        }
    }
    
}


