/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: PriivlegeNameValidation.java,v 1.1 2009/11/25 18:09:50 veiming Exp $
 */
/*
 * Portions Copyrighted 2026 3A Systems, LLC
 */

package com.sun.identity.entitlement;

import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class PrivilegeNameValidationTest {

    @Test
    public void allLetters() throws Exception {
        if (!PrivilegeManager.isNameValid("test")) {
            throw new Exception(
                "PrivilegeNameValidationTest.allLetters test failed");
        }
    }

    @Test
    public void allNumeric() throws Exception {
        if (!PrivilegeManager.isNameValid("999")) {
            throw new Exception(
                "PrivilegeNameValidationTest.allNumeric test failed");
        }
    }

    @Test
    public void allAlphaNumeric() throws Exception {
        if (!PrivilegeManager.isNameValid("test123")) {
            throw new Exception(
                "PrivilegeNameValidationTest.allAlphaNumeric test failed");
        }
    }

    @Test
    public void withUnderscore() throws Exception {
        if (!PrivilegeManager.isNameValid("test_123")) {
            throw new Exception(
                "PrivilegeNameValidationTest.withUnderscore test failed");
        }
    }

    @Test
    public void withDash() throws Exception {
        if (!PrivilegeManager.isNameValid("test-12")) {
            throw new Exception(
                "PrivilegeNameValidationTest.withDash test failed");
        }
    }

    @Test
    public void withSpecialChar() throws Exception {
        if (PrivilegeManager.isNameValid("test^")) {
            throw new Exception(
                "PrivilegeNameValidationTest.withSpecialChar test failed");
        }
    }

}
