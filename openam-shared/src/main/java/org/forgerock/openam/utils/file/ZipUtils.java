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
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.utils.file;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;


import static java.util.Collections.singletonMap;

/**
 * Generate a zip from a folder.
 */
public final class ZipUtils {

    private ZipUtils() {
    }

    /**
     * Generate a zip
     *
     * Due to a bug in Java 7 corrected in Java 8 http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7156873
     * srcFolder and outputZip can't be URL encoded if you're using java 7.
     *
     * @param srcFolder source folder
     * @param outputZip zip folder
     * @return the list of files that were included in the archive.
     * @throws IOException if an error occurs creating the zip archive.
     * @throws URISyntaxException if an error occurs creating the zip archive.
     */
    public static List<String> generateZip(String srcFolder, String outputZip) throws IOException, URISyntaxException {

        final Path targetZip = Paths.get(outputZip);
        final Path sourceDir = Paths.get(srcFolder);
        final URI uri = new URI("jar", URLDecoder.decode(targetZip.toUri().toString(), "UTF-8"), null);
        final List<String> files = new ArrayList<>();

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, singletonMap("create", "true"))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // In case the target zip is being created in the folder being zipped (e.g. Fedlet), ignore it.
                    if (targetZip.equals(file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    Path target = zipfs.getPath(sourceDir.relativize(file).toString());

                    if (target.getParent() != null) {
                        Files.createDirectories(target.getParent());
                    }
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    files.add(file.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return files;
    }
}
