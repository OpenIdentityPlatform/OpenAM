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
package com.sun.identity.shared.debug.file.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.testng.annotations.Test;

public class StdDebugFileTest {

    @Test
    public void writeItContinuesALineBreakInTheMessageIndented() throws Exception {
        StringWriter out = new StringWriter();

        new StdDebugFile(new PrintWriter(out, true)).writeIt("amAuth: TransactionId[x]", "user\nforged", null);

        assertThat(out.toString()).isEqualTo("amAuth: TransactionId[x]\nuser\n    forged" + System.lineSeparator());
    }

    @Test
    public void printErrorContinuesALineBreakInTheMessageIndented() {
        PrintStream saved = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err, true));
        try {
            StdDebugFile.printError("amAuth", "user\nforged", null);
        } finally {
            System.setErr(saved);
        }

        assertThat(err.toString())
                .startsWith("amAuth:")
                .contains("]\nuser\n    forged")
                .doesNotContain("\n\n");
    }
}
