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
 */

package com.sun.identity.common.configuration;

import java.util.Collections;
import java.util.HashSet;
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
    
    
}
