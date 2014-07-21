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

package org.forgerock.maven.plugins.inject.content;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>Maven Mojo which can can inject a set of content files into a specific place in a destination file.</p>
 *
 * @since 12.0.0
 */
@Mojo(name = "inject-content", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class InjectContent extends AbstractMojo {

    static final String NO_OP_CONTENT_CONVERTER = "none";
    private static final String XML_CONTENT_CONVERTER = "xml";
    private static final Map<String, ContentConverter> CONTENT_CONVERTERS = new HashMap<String, ContentConverter>(){{
        put(NO_OP_CONTENT_CONVERTER, new NoOpContentConverter());
        put(XML_CONTENT_CONVERTER, new XmlContentConverter());
    }};

    private final IOFactory ioFactory;

    @Parameter(required = true)
    private List<Inject> injects;

    /**
     * Default constructor used by the Maven build process.
     */
    public InjectContent() {
        this.ioFactory = new IOFactory();
    }

    /**
     * Test constructor.
     *
     * @param ioFactory An instance of the {@code IOFactory}.
     * @param injects The inject configuration.
     */
    InjectContent(IOFactory ioFactory, List<Inject> injects) {
        this.ioFactory = ioFactory;
        this.injects = injects;
    }

    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException {

        try {
            for (Inject inject : injects) {
                injectContent(inject);
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Injects the configured content files into the destination file.
     *
     * @param injectContent The inject configuration.
     * @throws IOException If there is a problem reading or writing.
     * @throws MojoExecutionException If the {@code ContentConverter} could not be retrieved.
     */
    private void injectContent(Inject injectContent) throws IOException, MojoExecutionException {

        Map<Pattern, File> contents = getContentsMap(injectContent.getContents());

        File tmpFile = ioFactory.createTemporaryFile(injectContent.getDestinationFile());
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = ioFactory.newReader(injectContent.getDestinationFile());
            writer = ioFactory.newWriter(tmpFile);

            String line;
            while ((line = reader.readLine()) != null) {
                File content = doesLineMatchId(contents, line);
                if (content != null) {
                    ioFactory.writeContent(writer, content, getContentConverter(injectContent));
                } else {
                    ioFactory.writeLine(writer, line);
                }
            }

        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }

        ioFactory.moveTo(tmpFile, injectContent.getDestinationFile());
    }

    /**
     * Gets a content map of the content files and their content placeholder IDs.
     *
     * @param contents The contents configuration.
     * @return The contents map.
     */
    private Map<Pattern, File> getContentsMap(List<Content> contents) {
        Map<Pattern, File> contentsMap = new HashMap<Pattern, File>();
        for (Content content : contents) {
            contentsMap.put(Pattern.compile(".*\\Q${inject.content." + content.getId() + "}\\E.*"), content.getFile());
        }
        return contentsMap;
    }

    /**
     * Pattern matches the content placeholder IDs against the given line.
     *
     * @param contents The contents.
     * @param line The line.
     * @return The matching content file, or {@code null} if no content placeholder ID matched the line.
     */
    private File doesLineMatchId(Map<Pattern, File> contents, String line) {
        for (Pattern pattern : contents.keySet()) {
            if (pattern.matcher(line).matches()) {
                return contents.get(pattern);
            }
        }
        return null;
    }

    /**
     * Gets the {@code ContentConverter} configured for this inject configuration.
     *
     * @param injectContent The inject configuration.
     * @return The {@code ContentConverter}.
     * @throws MojoExecutionException If the configured content converter is invalid.
     */
    private ContentConverter getContentConverter(Inject injectContent) throws MojoExecutionException {

        String contentConverterName = injectContent.getContentConverter();
        if (contentConverterName != null && !contentConverterName.isEmpty()) {

            ContentConverter contentConverter = CONTENT_CONVERTERS.get(contentConverterName);

            if (contentConverter == null) {
                throw new MojoExecutionException("Unknown content converter, " + contentConverterName);
            }

            return contentConverter;
        }

        return CONTENT_CONVERTERS.get(NO_OP_CONTENT_CONVERTER);
    }
}
