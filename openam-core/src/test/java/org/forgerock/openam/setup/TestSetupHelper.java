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

package org.forgerock.openam.setup;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Test helper for setting up tests with pre-zipped directories and removing directories on test
 * tear down.
 */
public class TestSetupHelper {

    /**
     * Extracts the {@literal zipFile} to the {@literal destinationDirectory}.
     *
     * @param zipFile The zip file.
     * @param destinationDirectory The extract directory.
     * @throws IOException If the zip file could not be extracted.
     */
    public static void extractZip(File zipFile, final Path destinationDirectory) throws IOException {
        if (Files.notExists(destinationDirectory)) {
            Files.createDirectories(destinationDirectory);
        }

        try (java.nio.file.FileSystem zipFileSystem = FileSystems.newFileSystem(zipFile.toPath(), null)) {
            Path root = zipFileSystem.getPath("/");
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Path destinationFile = Paths.get(destinationDirectory.toString(), file.toString());
                    Files.copy(file, destinationFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                        throws IOException {
                    Path directoryToCreate = Paths.get(destinationDirectory.toString(), directory.toString());
                    if (Files.notExists(directoryToCreate)) {
                        Files.createDirectory(directoryToCreate);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Deletes the given {@literal directory} from the file system.
     *
     * @param directory The directory to delete.
     * @throws IOException If the directory could not be deleted.
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw e;
                }
            }
        });
    }
}
