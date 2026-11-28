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

import static org.testng.Assert.assertEquals;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.asciidoctor.jruby.internal.RubyObjectWrapper;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AntoraPageTitlesIT {
    private static final Map<String, Object> SITE_ATTRIBUTES = Map.of("env-site", "");
    private static final Options HEADER_OPTIONS = Options.builder().safe(SafeMode.SAFE)
            .attributes(SITE_ATTRIBUTES).parseHeaderOnly(true).build();

    @Test
    public void allGeneratedPagesHaveDocumentTitles() throws IOException {
        Path modules = generatedModules();
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

        List<String> untitledPages = new ArrayList<>();
        try (Asciidoctor asciidoctor = createSiteAsciidoctor(modules)) {
            for (Path page : pages) {
                File input = modules.resolve(page).toFile();
                if (!hasDocumentTitle(asciidoctor.loadFile(input, HEADER_OPTIONS))) {
                    untitledPages.add(page.toString());
                }
            }
        }
        assertTrue(untitledPages.isEmpty(),
                "Generated Antora pages without document titles:\n" + String.join("\n", untitledPages));
    }

    @DataProvider
    public Object[][] pagesWithPublishedHeadingIds() {
        return new Object[][] {
            { "getting-started/pages/chap-first-steps.adoc", "Get Started with OpenAM", "_get_started_with_openam" },
            { "how-to/pages/quick-start-guide.adoc", "Quick Start Guide", "_quick_start_guide" }
        };
    }

    @Test(dataProvider = "pagesWithPublishedHeadingIds")
    public void renderedPagePreservesPublishedHeadingId(String page, String title, String headingId) {
        Path modules = generatedModules();
        Options options = Options.builder().safe(SafeMode.SAFE).attributes(SITE_ATTRIBUTES).build();
        try (Asciidoctor asciidoctor = createSiteAsciidoctor(modules)) {
            Document document = asciidoctor.loadFile(modules.resolve(page).toFile(), options);
            assertEquals(document.getDoctitle(), title, page);
            long matches = Pattern.compile("\\sid=\"" + Pattern.quote(headingId) + "\"")
                    .matcher(document.convert()).results().count();
            assertEquals(matches, 1L, page + " must render one anchor for #" + headingId);
        }
    }

    @DataProvider
    public Object[][] documentTitlesWithOverrides() {
        return new Object[][] {
            { "= Heading\n:title: Browser Title", true },
            { ":title: Browser Title", false },
            { ":title: Browser Title\nifdef::title[]\n= Heading\nendif::[]", true },
            { ":title: Browser Title\nifndef::title[]\n= Heading\nendif::[]", false }
        };
    }

    @Test(dataProvider = "documentTitlesWithOverrides")
    public void recognizesDocumentTitlesWithOverrides(String header, boolean expected) {
        String source = header + "\n\nParagraph.\n";
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            assertEquals(hasDocumentTitle(asciidoctor.load(source, HEADER_OPTIONS)), expected);
        }
    }

    private static Path generatedModules() {
        return Path.of(System.getProperty("docBuildDirectory", "target/asciidoc"))
                .resolve("antora/modules").toAbsolutePath();
    }

    private static Asciidoctor createSiteAsciidoctor(Path modules) {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry().includeProcessor(
                new RootPartialIncludeProcessor(modules.resolve("ROOT/partials")));
        return asciidoctor;
    }

    private static boolean hasDocumentTitle(Document document) {
        // AsciidoctorJ's Document API does not expose the header predicate used by Antora.
        boolean hasHeader = ((RubyObjectWrapper) document).getBoolean("has_header?");
        String title = document.getDoctitle();
        return hasHeader && title != null && !title.isBlank();
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
