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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

import com.sun.identity.shared.debug.DebugConstants;

/**
 * Lays out one debug record: the prefix line, the message, and the stack trace if there is
 * one. A record is told from the next by its prefix line starting at column 0, so nothing
 * that is logged may start a line there: every line break inside the message and every
 * line of the stack trace (an exception's message is logged data too) continues the record
 * indented. A single-line message, the usual case, is written exactly as it always was, and
 * a multi-line dump stays readable, indented.
 * <p>
 * This is what keeps a value taken from a request - a user name, a RelayState, a SAML
 * attribute - from forging a record of its own, whether it reaches the debug file through
 * {@code Debug} directly or through the SLF4J binding.
 */
public final class DebugRecordFormat {

    /** What every continued line is indented with. */
    static final String CONTINUATION = "    ";

    private static final Pattern LINE_BREAK = Pattern.compile("\r\n|[\r\n  ]");

    private DebugRecordFormat() {
    }

    /**
     * @param prefix the record's prefix line (debug name, timestamp, thread, transaction)
     * @param msg the message; {@code null} is written as {@code null}
     * @param th the throwable whose stack trace follows the message, or {@code null}
     * @return the record, without a trailing line break
     */
    public static String format(String prefix, String msg, Throwable th) {
        StringBuilder buf = new StringBuilder(prefix);
        buf.append('\n');
        buf.append(continued(String.valueOf(msg), false));
        if (th != null) {
            StringWriter trace = new StringWriter(DebugConstants.MAX_BUFFER_SIZE_EXCEPTION);
            PrintWriter writer = new PrintWriter(trace);
            th.printStackTrace(writer);
            writer.flush();
            buf.append('\n');
            buf.append(continued(trace.toString(), true));
        }
        return buf.toString();
    }

    /**
     * {@code text} with every line break turned into a newline followed by the continuation
     * indent, the first line indented as well when {@code indentFirst}; trailing line breaks
     * are dropped.
     */
    private static String continued(String text, boolean indentFirst) {
        String[] lines = LINE_BREAK.split(text, -1);
        int last = lines.length;
        while (last > 1 && lines[last - 1].isEmpty()) {
            last--;
        }
        StringBuilder out = new StringBuilder(text.length() + last * CONTINUATION.length());
        for (int i = 0; i < last; i++) {
            if (i > 0) {
                out.append('\n');
            }
            if (i > 0 || indentFirst) {
                out.append(CONTINUATION);
            }
            out.append(lines[i]);
        }
        return out.toString();
    }
}
