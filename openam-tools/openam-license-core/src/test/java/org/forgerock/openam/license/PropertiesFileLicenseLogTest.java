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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.license;

import com.sun.identity.shared.DateUtils;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import static org.forgerock.openam.utils.Time.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for {@link PropertiesFileLicenseLog}.
 *
 * @since 12.0.0
 */
public class PropertiesFileLicenseLogTest {
    private static final Random RANDOM = new Random();
    /** A temporary directory to log into. */
    private File logDir;

    @BeforeMethod
    public void createTemporaryLogDir() throws IOException {
        // Attempt to create a temporary log directory (Files.createTempDirectory only available in Java 7)
        int tries = 0;
        do {
            if (tries++ > 3) {
                throw new SkipException("Unable to create temp dir");
            }
            logDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "licensetest" + RANDOM.nextInt());
        } while (!logDir.mkdir());

        System.out.println("tmpdir = " + logDir);
    }

    @AfterMethod
    public void deleteTemporaryLogDir() throws IOException {
        logDir.delete();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullLogDirectory() {
        new PropertiesFileLicenseLog(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectMissingLogDir() {
        new PropertiesFileLicenseLog(new File("does-not-exist"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectNonWriteableLogDir() {
        // Given
        if (!logDir.setWritable(false)) {
            throw new SkipException("Unable to make log dir non-writeable");
        }

        // Then
        new PropertiesFileLicenseLog(logDir);
    }

    @Test
    public void shouldCreateCorrectLogFile() {
        // Given
        String name = "test";
        String user = "testuser";
        License license = new License(name + ".txt", "...");
        LicenseLog log = new PropertiesFileLicenseLog(logDir);
        Date acceptedDate = newDate();

        // When
        log.logLicenseAccepted(license, user, acceptedDate);

        // Then
        File logFile = new File(logDir, name + ".log");
        assertTrue(logFile.exists());
    }

    @Test
    public void shouldLogCorrectAcceptanceDate() throws Exception {
        // Given
        String name = "test";
        String user = "testuser";
        License license = new License(name + ".txt", "...");
        LicenseLog log = new PropertiesFileLicenseLog(logDir);
        Date acceptedDate = newDate();

        // When
        log.logLicenseAccepted(license, user, acceptedDate);

        // Then
        Properties logged = loadLogFile(name);
        assertNotNull(logged.getProperty(user));
        Date loggedDate = DateUtils.stringToDate(logged.getProperty(user));
        // Only second precision in log:
        assertEquals(loggedDate.getTime() / 1000, acceptedDate.getTime() / 1000);
    }

    private Properties loadLogFile(String name) throws IOException {
        InputStream in = new FileInputStream(new File(logDir, name + ".log"));
        try {
            Properties props = new Properties();
            props.load(in);
            return props;
        } finally {
            in.close();
        }
    }
}
