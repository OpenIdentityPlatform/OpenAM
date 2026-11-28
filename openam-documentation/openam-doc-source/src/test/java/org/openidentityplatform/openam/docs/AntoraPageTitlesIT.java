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
 * Copyright 2026 3A Systems, LLC.
 */
package org.openidentityplatform.openam.docs;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.testng.annotations.Test;

public class AntoraPageTitlesIT {
    @Test
    public void allGeneratedPagesHaveDocumentTitles() throws IOException {
        Path modules = Path.of(System.getProperty("docBuildDirectory", "target/asciidoc"))
                .resolve("antora/modules").toAbsolutePath();
        assertTrue(Files.isDirectory(modules), "Missing generated Antora modules: " + modules);

        List<Path> pages;
        try (Stream<Path> paths = Files.walk(modules)) {
            pages = paths.filter(Files::isRegularFile)
                    .map(modules::relativize)
                    .filter(path -> path.getNameCount() >= 3 && path.getName(1).toString().equals("pages"))
                    .filter(path -> path.getFileName().toString().endsWith(".adoc"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        assertFalse(pages.isEmpty(), "No generated Antora pages in " + modules);

        Options options = Options.builder().safe(SafeMode.SAFE)
                .attributes(Map.of("env-site", "")).parseHeaderOnly(true).build();
        Options withoutTitleOverride = Options.builder().safe(SafeMode.SAFE)
                .attributes(Map.of("env-site", "", "title!", "")).parseHeaderOnly(true).build();
        List<String> untitledPages = new ArrayList<>();
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            asciidoctor.javaExtensionRegistry().includeProcessor(
                    new RootPartialIncludeProcessor(modules.resolve("ROOT/partials")));
            for (Path page : pages) {
                File input = modules.resolve(page).toFile();
                Document document = asciidoctor.loadFile(input, options);
                // With :title:, getDoctitle can return a value even when there is no document header.
                if (!hasTitle(document) || (document.hasAttribute("title")
                        && !hasTitle(asciidoctor.loadFile(input, withoutTitleOverride)))) {
                    untitledPages.add(page.toString());
                }
            }
        }
        assertTrue(untitledPages.isEmpty(),
                "Generated Antora pages without document titles:\n" + String.join("\n", untitledPages));
    }

    private static boolean hasTitle(Document document) {
        String title = document.getDoctitle();
        return title != null && !title.isBlank();
    }

    private static final class RootPartialIncludeProcessor extends IncludeProcessor {
        private static final String PREFIX = "ROOT:partial$";
        private final Path partialsDirectory;

        private RootPartialIncludeProcessor(Path partialsDirectory) {
            this.partialsDirectory = partialsDirectory.normalize();
        }

        @Override
        public boolean handles(String target) {
            return target.startsWith(PREFIX);
        }

        @Override
        public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
            Path partial = partialsDirectory.resolve(target.substring(PREFIX.length())).normalize();
            if (!partial.startsWith(partialsDirectory)) {
                throw new IllegalArgumentException("Invalid Antora ROOT partial: " + target);
            }
            String path = partial.toString();
            try {
                reader.pushInclude(Files.readString(partial), path, path, 1, attributes);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read Antora partial " + partial, e);
            }
        }
    }
}
