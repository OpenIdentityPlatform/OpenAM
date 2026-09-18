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
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.junit.Before;
import org.junit.Test;
import org.openidentityplatform.openam.cassandra.embedded.Server;

/**
 * The embedded store keeps its data under the shared temporary directory by default;
 * the directory has to be the server account's alone.
 */
public class ServerStorageDirectoryTest {

    private Path base;

    @Before
    public void posixOnly() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        base = Files.createTempDirectory("embedded-cassandra-test");
    }

    @Test
    public void storageDirectoryIsCreatedForTheOwnerOnly() throws IOException {
        Path storage = base.resolve("embeddedCassandra");

        Server.privateDirectory(storage);

        assertTrue(Files.isDirectory(storage));
        assertEquals(EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE), Files.getPosixFilePermissions(storage));
    }

    @Test
    public void existingStorageDirectoryKeepsItsDataAndIsClosedToOthers() throws IOException {
        Path storage = Files.createDirectory(base.resolve("embeddedCassandra"));
        Files.write(storage.resolve("data"), "kept".getBytes(StandardCharsets.UTF_8));
        Files.setPosixFilePermissions(storage, EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
                java.nio.file.attribute.PosixFilePermission.OTHERS_READ,
                java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE));

        Server.privateDirectory(storage);

        assertEquals("kept", new String(Files.readAllBytes(storage.resolve("data")), StandardCharsets.UTF_8));
        assertEquals(EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE), Files.getPosixFilePermissions(storage));
    }
}
