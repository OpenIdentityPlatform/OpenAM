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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Test FileSizeUnit
 */
public class FileSizeUnitTest {

    @Test
    public void tryUnitsConversion() throws IOException {
        Assert.assertEquals(FileSizeUnit.GB.toMB(1), 1024);
        Assert.assertEquals(FileSizeUnit.GB.toKB(1), 1024 * 1024);
        Assert.assertEquals(FileSizeUnit.GB.toB(1), 1024 * 1024 * 1024);

        Assert.assertEquals(FileSizeUnit.MB.toGB(1024), 1);
        Assert.assertEquals(FileSizeUnit.MB.toKB(1), 1024);
        Assert.assertEquals(FileSizeUnit.MB.toB(1), 1024 * 1024);

        Assert.assertEquals(FileSizeUnit.KB.toGB(1024 * 1024), 1);
        Assert.assertEquals(FileSizeUnit.KB.toMB(1024), 1);
        Assert.assertEquals(FileSizeUnit.KB.toB(1), 1024);

        Assert.assertEquals(FileSizeUnit.B.toGB(1024 * 1024 * 1024), 1);
        Assert.assertEquals(FileSizeUnit.B.toMB(1024 * 1024), 1);
        Assert.assertEquals(FileSizeUnit.B.toKB(1024), 1);
    }
}
