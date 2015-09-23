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
package com.iplanet.dpro.session;

import static org.assertj.core.api.Assertions.*;
import org.testng.annotations.Test;

import java.io.IOException;

public class LegacySessionIDExtensionsTest {
    @Test
    public void shouldDecodeKnownExtensionString() throws IOException {
        String knownExtensions = "AAJTSQACMDQAAlNLABQtMzIwMjM3MzE1Mzg1MjM4MTQ3MAACUzEAAjAx";
        LegacySessionIDExtensions result = new LegacySessionIDExtensions(knownExtensions);
        assertThat(result.getPrimaryID()).isEqualTo("01");
        assertThat(result.getSiteID()).isEqualTo("04");
    }
}