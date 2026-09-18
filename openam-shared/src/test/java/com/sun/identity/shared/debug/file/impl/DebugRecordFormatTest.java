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

import org.testng.annotations.Test;

public class DebugRecordFormatTest {

    private static final String PREFIX = "amAuth:09/18/2026 10:00:00:000 AM MSK: Thread[main,5,main]: TransactionId[x]";

    @Test
    public void aSingleLineMessageIsWrittenAsBefore() {
        assertThat(DebugRecordFormat.format(PREFIX, "login failed for user", null))
                .isEqualTo(PREFIX + "\nlogin failed for user");
    }

    /** A line break carried in a logged value cannot start a line that reads as a new record. */
    @Test
    public void aLineBreakInTheMessageContinuesTheRecordIndented() {
        String forged = "user\n" + PREFIX + "\nforged message";

        assertThat(DebugRecordFormat.format(PREFIX, forged, null))
                .isEqualTo(PREFIX + "\nuser\n    " + PREFIX + "\n    forged message");
    }

    @Test
    public void everyKindOfLineBreakIsAContinuation() {
        assertThat(DebugRecordFormat.format(PREFIX, "a\r\nb\rc d ef", null))
                .isEqualTo(PREFIX + "\na\n    b\n    c\n    d\n    e\n    f");
    }

    @Test
    public void aMultiLineDumpStaysReadable() {
        assertThat(DebugRecordFormat.format(PREFIX, "SAML response:\n<Response>\n  <Issuer/>\n</Response>", null))
                .isEqualTo(PREFIX + "\nSAML response:\n    <Response>\n      <Issuer/>\n    </Response>");
    }

    /** The exception's own message is logged data too: no line of the trace may start at column 0. */
    @Test
    public void everyLineOfTheStackTraceIsIndented() {
        String out = DebugRecordFormat.format(PREFIX, "failed", new IllegalStateException("boom\n" + PREFIX));

        String[] lines = out.split("\n");
        assertThat(lines[0]).isEqualTo(PREFIX);
        assertThat(lines[1]).isEqualTo("failed");
        assertThat(lines[2]).isEqualTo("    java.lang.IllegalStateException: boom");
        assertThat(lines[3]).isEqualTo("    " + PREFIX);
        assertThat(lines[4]).startsWith("    \tat ");
        for (int i = 2; i < lines.length; i++) {
            assertThat(lines[i]).as("line " + i).startsWith("    ");
        }
        assertThat(out).doesNotEndWith("\n");
    }

    @Test
    public void aNullMessageIsWrittenAsTheWordNull() {
        assertThat(DebugRecordFormat.format(PREFIX, null, null)).isEqualTo(PREFIX + "\nnull");
    }
}
