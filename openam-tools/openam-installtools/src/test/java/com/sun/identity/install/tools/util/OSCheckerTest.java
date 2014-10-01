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

package com.sun.identity.install.tools.util;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;
import static com.sun.identity.install.tools.util.OSChecker.*;

public class OSCheckerTest {

    private static final String OS_NAME = System.getProperty("os.name");
    private static final List<String> ARCHITECTURES = Arrays.asList(
            "i386", "i686", "x86", "x86_64", "amd64", "PowerPC", "ppc", "ppc64", "sparc");

    @Test
    public void testIsUnixOrWindows() {
        assertTrue(isUnix() || isWindows());
        assertFalse(isUnix() && isWindows());
    }

    @Test
    public void testLinuxVersionStringParsing() {

        String linuxVersionString = "2.6.32-279.14.1.el6.x86_64";
        parseVersion(linuxVersionString);
        assertEquals(getOsMajorVersion(), 2);
        assertEquals(getOsMinorVersion(), 6);
    }

    @Test
    public void testOSXVersionStringParsing() {
        String osXVersionString = "10.9.5";
        parseVersion(osXVersionString);
        assertEquals(getOsMajorVersion(), 10);
        assertEquals(getOsMinorVersion(), 9);
    }

    @Test
    public void testWindowsVersionStringParsing() {
        String windowsVersionString = "5.1";
        parseVersion(windowsVersionString);
        assertEquals(getOsMajorVersion(), 5);
        assertEquals(getOsMinorVersion(), 1);
    }

    @Test
    public void testNoMinorVersionStringParsing() {

        String noMinorString = "100";
        parseVersion(noMinorString);
        assertEquals(getOsMajorVersion(), 100);
        assertEquals(getOsMinorVersion(), 0);
    }

    @Test
    public void testInvalidVersionStringParsing() {

        // The exception output will end up in the TestCase debug log under the
        // target/test-cases directory as set in the POM.
        String exceptionString = "abc.10";
        parseVersion(exceptionString);
        assertEquals(getOsMajorVersion(), 0);
        assertEquals(getOsMinorVersion(), 0);
    }

    @Test
    public void testAtLeast() {
        parseVersion("1.2.3");
        assertTrue(atleast(OS_NAME, 1, 0));
        assertTrue(atleast(OS_NAME, 1, 2));
        assertFalse(atleast(OS_NAME, 1, 3));
        assertFalse(atleast(OS_NAME, 2, 1));
        assertFalse(atleast("iOS", 1, 0));
    }
    
    @Test
    public void testArchitecture() {
        int foundArch = 0;

        // non-exhaustive list, covers conceivable architectures that might be running unit tests
        for (String arch : ARCHITECTURES) {
            if (matchArch(arch)) {
                foundArch++;
            }
        }

        assertEquals(foundArch, 1, "Expected " + System.getProperty("os.arch") + " to be one of " + ARCHITECTURES);
    }
}
