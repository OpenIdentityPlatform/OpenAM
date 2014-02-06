/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.license;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loads required licenses from the classpath with well-known names.
 *
 * @see ClassLoader#getSystemResourceAsStream(String)
 * @since 12.0.0
 */
public class ClasspathLicenseLocator implements LicenseLocator {
    private static final String[] DEFAULT_LICENSE_FILES = {"license.txt"};
    private final List<String> licenseFiles;
    private final ClassLoader classLoader;
    private final Charset charset;

    /**
     * Constructs the license locator with the given license file names. Each file name given must exist on the
     * system classpath.
     *
     * @param classLoader the class loader to use to locate the license file.
     * @param charset the character set to use when loading the license files.
     * @param licenseFiles the license files to load.
     * @throws IllegalArgumentException if no license files are specified.
     * @throws NullPointerException if the class loader is null.
     */
    public ClasspathLicenseLocator(final ClassLoader classLoader, final Charset charset, final String... licenseFiles) {
        if (licenseFiles == null || licenseFiles.length == 0) {
            throw new IllegalArgumentException("No license files specified");
        }
        if (classLoader == null) {
            throw new NullPointerException("ClassLoader is null");
        }
        if (charset == null) {
            throw new NullPointerException("Charset is null");
        }

        this.licenseFiles = new ArrayList<String>(Arrays.asList(licenseFiles));
        this.classLoader = classLoader;
        this.charset = charset;
    }

    /**
     * Constructs the license locator with the default license file name (license.txt), the system classloader, and
     * the platform default character set.
     */
    public ClasspathLicenseLocator() {
        this(ClassLoader.getSystemClassLoader(), Charset.defaultCharset(), DEFAULT_LICENSE_FILES);
    }

    /**
     * {@inheritDoc}
     *
     * Loads any required licenses from the classpath.
     *
     * @return the set of required licenses
     * @throws MissingLicenseException if any of the required licenses cannot be found on the classpath or cannot be
     * loaded for any reason.
     */
    public LicenseSet getRequiredLicenses() {
        List<License> licenses = new ArrayList<License>(licenseFiles.size());
        for (String licenseFile : licenseFiles) {
            try {
                final String licenseText = readFully(classLoader.getResourceAsStream(licenseFile));
                licenses.add(new License(licenseText));
            } catch (IOException ex) {
                throw new MissingLicenseException(licenseFile);
            }
        }
        return new LicenseSet(licenses);
    }

    /**
     * Basic utility method to fully read a stream into memory. The reader will be closed afterwards.
     *
     * @param input the input stream to read.
     * @return the full contents of the file.
     * @throws IOException if an error occurs while reading the stream.
     */
    private String readFully(InputStream input) throws IOException {
        if (input == null) {
            throw new FileNotFoundException();
        }
        final BufferedReader in = new BufferedReader(new InputStreamReader(input, charset));
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb.toString();
        } finally {
            in.close();
        }
    }
}
