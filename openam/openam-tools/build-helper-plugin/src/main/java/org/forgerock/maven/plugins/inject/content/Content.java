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

/**
 * Maven configuration object for modelling the content file and the ID placeholder in the destination file where the
 * content file will be injected.
 *
 * @since 12.0.0
 */
public class Content {

    private String id;
    private File file;

    /**
     * Gets the ID of the content placeholder.
     *
     * @return The content placeholder ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the content placeholder.
     *
     * @param id The content placeholder ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the content file.
     *
     * @return The content file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the content file.
     *
     * @param file The content file.
     */
    public void setFile(File file) {
        this.file = file;
    }
}
