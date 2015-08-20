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

import java.io.File;
import java.io.IOException;

/**
 * Test the ZipUtils
 */
public class ZipUtilsTest {

    @Test(enabled = false)
    public void tryZippingAFolder() throws IOException {
        String testFolder =  File.separator + "zipUtils" + File.separator + "FakeFolder";
        testFolder = ZipUtilsTest.class.getResource(testFolder).getPath();
        String testFolderZipped =  testFolder + ".zip";

        ZipUtils.generateZip(testFolder, testFolderZipped);
        File f = new File(testFolderZipped);
        Assert.assertTrue(f.exists() && !f.isDirectory());
    }
}
