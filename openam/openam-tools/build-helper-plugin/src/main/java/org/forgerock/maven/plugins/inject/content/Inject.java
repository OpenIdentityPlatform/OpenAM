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

import java.io.File;
import java.util.List;

/**
 * Maven configuration object for modelling the content files and the ID placeholders for destination file, where the
 * content files will be injected.
 *
 * @since 12.0.0
 */
public class Inject {

    private List<Content> contents;
    private File destinationFile;
    private String contentConverter = InjectContent.NO_OP_CONTENT_CONVERTER;

    /**
     * Gets the contents that will be injected into the destination file.
     *
     * @return The contents.
     */
    public List<Content> getContents() {
        return contents;
    }

    /**
     * Sets the contents that will be injected into the destination file.
     *
     * @param contents The contents.
     */
    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    /**
     * Gets the destination file.
     *
     * @return The destination file.
     */
    public File getDestinationFile() {
        return destinationFile;
    }

    /**
     * Sets the destination file.
     *
     * @param destinationFile The destination file.
     */
    public void setDestinationFile(File destinationFile) {
        this.destinationFile = destinationFile;
    }

    /**
     * <p>Gets the name of the {@code ContentConverter} to use when injecting content into the destination file.</p>
     *
     * @return The content converter name.
     */
    public String getContentConverter() {
        return contentConverter;
    }

    /**
     * <p>Sets the name of the {@code ContentConverter} to use when injecting content into the destination file.</p>
     *
     * <p>Optional, defaults to "none".</p>
     *
     * @param contentConverter The content converter name.
     */
    public void setContentConverter(String contentConverter) {
        this.contentConverter = contentConverter;
    }
}
