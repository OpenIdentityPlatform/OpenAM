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
 * Copyright 2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.docs.services;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class WarClassLoader extends URLClassLoader {

    private final Path webInfPath;
    private final Path classesPath;
    private final Path libPath;
    public WarClassLoader(String extractedPath) throws IOException {
        super(new URL[0]);
        this.webInfPath = Paths.get(extractedPath, "WEB-INF");
        this.classesPath = webInfPath.resolve("classes");
        this.libPath = webInfPath.resolve("lib");

        if(Files.exists(classesPath)) {
            addURL(classesPath.toUri().toURL());
        }

        if (Files.exists(libPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(libPath, "*.jar")) {
                for (Path jarPath : stream) {
                    addURL(jarPath.toUri().toURL());
                }
            }
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        try {
            Path resourcePath = classesPath.resolve(name);
            if (Files.exists(resourcePath)) {
                return Files.newInputStream(resourcePath);
            }

            if (Files.exists(libPath)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(libPath, "*.jar")) {
                    for (Path jarPath : stream) {
                        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                            JarEntry entry = jarFile.getJarEntry(name);
                            if (entry != null) {
                                // Need to read the entire stream since we're closing the JAR file
                                try (InputStream is = jarFile.getInputStream(entry)) {
                                    return new ByteArrayInputStream(IOUtils.toByteArray(is));
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.getResourceAsStream(name);
    }

    public Properties loadProperties(String propertiesPath) throws IOException {
        Properties props = new Properties();
        try (InputStream is = getResourceAsStream(propertiesPath)) {
            if (is != null) {
                props.load(is);
            } else {
                throw new FileNotFoundException("Properties file not found: " + propertiesPath);
            }
        }
        return props;
    }
}
