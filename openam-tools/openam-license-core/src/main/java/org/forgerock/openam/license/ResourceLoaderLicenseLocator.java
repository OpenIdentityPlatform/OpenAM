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
 * Abstract base class for license locators that load the license from some resource location, such as classloaders and
 * servlet contexts.
 *
 * @since 12.0.0
 */
public abstract class ResourceLoaderLicenseLocator implements LicenseLocator {
    private final List<String> licenseFiles;
    private final Charset charset;

    /**
     * Constructs the license locator with the given license file names. Each file name given must exist on the
     * system classpath.
     *
     * @param charset the character set to use when loading the license files.
     * @param licenseFiles the license files to load.
     * @throws IllegalArgumentException if no license files are specified.
     * @throws NullPointerException if the class loader is null.
     */
    protected ResourceLoaderLicenseLocator(final Charset charset, final String... licenseFiles) {
        if (licenseFiles == null || licenseFiles.length == 0) {
            throw new IllegalArgumentException("No license files specified");
        }
        if (charset == null) {
            throw new NullPointerException("Charset is null");
        }

        this.licenseFiles = new ArrayList<String>(Arrays.asList(licenseFiles));
        this.charset = charset;
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
                final String licenseText = readFully(getResourceAsStream(licenseFile));
                licenses.add(new License(licenseFile, licenseText));
            } catch (IOException ex) {
                throw new MissingLicenseException(licenseFile);
            }
        }
        return new LicenseSet(licenses);
    }

    /**
     * Basic utility method to fully read a stream into memory. The input stream will be closed afterwards.
     *
     * @param input the input stream to read.
     * @return the full contents of the file.
     * @throws IOException if an error occurs while reading the stream.
     * @throws FileNotFoundException if the input is null.
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

    /**
     * Load the given resource as an input stream. Sub-classes should override this method to locate the actual resource.
     * Returns null if the resource cannot be found.
     *
     * @param resourceName the resource to load.
     * @return an input stream for the resource or null if not found.
     */
    protected abstract InputStream getResourceAsStream(String resourceName);
}
