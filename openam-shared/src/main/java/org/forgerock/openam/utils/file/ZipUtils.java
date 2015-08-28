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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.utils.file;


import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;


import static java.util.Collections.singletonMap;

/**
 * Generate a zip from a folder
 */
public class ZipUtils {

    /**
     * Generate a zip
     * @param srcFolder source folder
     * @param outputZip zip folder
     * @throws IOException
     */
    public static void generateZip(String srcFolder, String outputZip) throws IOException {


        final Path targetZip = Paths.get(outputZip);
        final Path sourceDir = Paths.get(srcFolder);
        final URI uri = URI.create("jar:file:" + targetZip.toAbsolutePath());

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, singletonMap("create", "true"))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path target = zipfs.getPath(sourceDir.relativize(file).toString());

                    if (target.getParent() != null) {
                        Files.createDirectories(target.getParent());
                    }
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
