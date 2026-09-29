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
package org.forgerock.openam.core.rest.docs.api;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.EnumSet;

import org.testng.SkipException;
import org.testng.annotations.Test;

public class ApiDocsServiceTest {

    /**
     * The generated documentation sits in the shared temporary directory until it is served;
     * nothing but the server's own account may read it there.
     */
    @Test
    public void temporaryDocsAreReadableByTheOwnerOnly() throws IOException {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            throw new SkipException("POSIX permissions are not available on this file system");
        }
        File docs = ApiDocsService.privateTempFile("openam-api.", ".html");
        try {
            assertEquals(Files.getPosixFilePermissions(docs.toPath()), EnumSet.of(OWNER_READ, OWNER_WRITE));
        } finally {
            docs.delete();
        }
    }
}
