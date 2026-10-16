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
 * Portions Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.common.configuration;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DuplicateKeyMapValueValidatorTest {
    
    private DuplicateKeyMapValueValidator validator;
    
    private Set inputValues = null;
    
    
    @BeforeMethod
    public void setUp() throws Exception {
        validator = new DuplicateKeyMapValueValidator();
        inputValues = null;
    }

    @Test
    public void testValidateNullValue() {
        AssertJUnit.assertTrue("Null value valid", validator.validate(inputValues));    
    }
    
    @Test
    public void testValidateEmptyValue() {
        inputValues = Collections.EMPTY_SET;
        AssertJUnit.assertTrue("Empty value valid", validator.validate(inputValues)); 
    }
    
    @Test
    public void testValidateDistinctValue() {
        inputValues = new HashSet(3);
        inputValues.add("[cn]=userid");
        inputValues.add("[uid]=userid");
        inputValues.add("[email]=mail");
        AssertJUnit.assertTrue("Distinct values valid", validator.validate(inputValues));
    }    
    
    @Test
    public void testValidateDuplicateKeyValue() {
        inputValues = new HashSet(3);
        inputValues.add("[cn]=userid");
        inputValues.add("[cn]=X-Username2");
        inputValues.add("[email]=mail");
        AssertJUnit.assertTrue("Duplicate keys values valid", validator.validate(inputValues));        
    }
    
    @Test
    public void testValidateKeyWithoutValue() {
        inputValues = new HashSet(1);
        inputValues.add("[cn]=");
        AssertJUnit.assertTrue("Key without value is valid", validator.validate(inputValues));        
    }   
    
    @Test
    public void testValidateDefaultValue() {
        inputValues = new HashSet(1);
        inputValues.add("[]=");
        AssertJUnit.assertTrue("Default value is valid", validator.validate(inputValues));        
    }     
    
    @Test
    public void testValidateDefaultVariantValue() {
        inputValues = new HashSet(1);
        inputValues.add("[  ]=");
        AssertJUnit.assertTrue("Default variant value is valid", validator.validate(inputValues));        
    }        
    
    @Test
    public void testValidateKeyWithEqualSign() {
        inputValues = new HashSet(2);
        inputValues.add("[abc=123]==someValue");
        AssertJUnit.assertTrue("Default variant value is valid", validator.validate(inputValues));        
    }          
    
    @Test
    public void testValidateValueWithEqualSign() {
        inputValues = new HashSet(2);
        inputValues.add("[abc]==someValue=123");
        AssertJUnit.assertTrue("Default variant value is valid", validator.validate(inputValues));
    }

    /**
     * The loop used to overwrite its answer on every value rather than stop at the first one
     * that did not match, so the value read last decided the answer for the whole set - and
     * the order a set is read in is not the order it was written in. Unlike its two siblings,
     * which both break out of the loop as soon as the answer is <code>false</code>.
     */
    @Test
    public void testValidateRejectsAnInvalidValueWhicheverWayTheSetIsRead() {
        inputValues = new LinkedHashSet(2);
        inputValues.add("[a]=1");
        inputValues.add("[unterminated");
        AssertJUnit.assertFalse("A value that is not a map entry, read last, makes the set "
                + "invalid", validator.validate(inputValues));

        inputValues = new LinkedHashSet(2);
        inputValues.add("[unterminated");
        inputValues.add("[a]=1");
        AssertJUnit.assertFalse("A value that is not a map entry, read first, makes the set "
                + "invalid", validator.validate(inputValues));
    }

    /**
     * The blank value is skipped rather than answered for, so it must not clear an answer
     * that an earlier value already settled either.
     */
    @Test
    public void testValidateInvalidValueFollowedByABlankOne() {
        inputValues = new LinkedHashSet(2);
        inputValues.add("[unterminated");
        inputValues.add("   ");
        AssertJUnit.assertFalse("A blank value does not make an invalid set valid",
                validator.validate(inputValues));
    }

    /**
     * White space inside the key, which is what the two competing quantifiers in the shared
     * key expression exist to allow - the only thing wrapping them in an atomic group could
     * have narrowed.
     */
    @Test
    public void testValidateKeyWithWhitespace() {
        inputValues = new HashSet(2);
        inputValues.add("[a b]=v");
        AssertJUnit.assertTrue("A key with an interior space is valid",
                validator.validate(inputValues));

        inputValues = new HashSet(2);
        inputValues.add("[ a b ] = v ");
        AssertJUnit.assertTrue("A key padded with spaces is valid",
                validator.validate(inputValues));

        inputValues = new HashSet(2);
        inputValues.add("[  ]=ALL");
        AssertJUnit.assertFalse("A key of nothing but whitespace is not valid",
                validator.validate(inputValues));
    }

    /**
     * This validator compiles its own pattern out of the key expression
     * <code>MapValueValidator</code> shares, so the bound on matching a key that is never
     * closed has to hold here too.
     */
    @Test
    public void testValidateUnterminatedKeyIsBounded() {
        inputValues = new HashSet(2);
        inputValues.add("[" + "a".repeat(64000));

        long startedAt = System.nanoTime();
        boolean valid = validator.validate(inputValues);
        long elapsedMillis = (System.nanoTime() - startedAt) / 1000000L;

        AssertJUnit.assertFalse("An unterminated key is not a valid value", valid);
        AssertJUnit.assertTrue("Matching an unterminated key has to be bounded, it took "
                + elapsedMillis + " ms", elapsedMillis < 5000L);
    }


}
