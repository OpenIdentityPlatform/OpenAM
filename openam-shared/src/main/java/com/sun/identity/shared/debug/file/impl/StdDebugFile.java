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
 * Copyright 2014-2016 ForgeRock AS.
 */
package com.sun.identity.shared.debug.file.impl;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.shared.debug.file.DebugFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Debug file dedicated to std out
 */
public class StdDebugFile implements DebugFile {

    private static final StdDebugFile INSTANCE = new StdDebugFile();

    private PrintWriter stdoutWriter = new PrintWriter(System.out, true);

    private StdDebugFile() {
    }

    /**
     * Get std out debug file
     *
     * @return std debug file
     */
    public static StdDebugFile getInstance() {
        return INSTANCE;
    }

    @Override
    public void writeIt(String prefix, String msg, Throwable th) throws IOException {
        StringBuilder buf = new StringBuilder(prefix);
        buf.append('\n');
        buf.append(msg);
        if (th != null) {
            buf.append('\n');
            StringWriter stBuf = new StringWriter(DebugConstants.MAX_BUFFER_SIZE_EXCEPTION);
            PrintWriter stackStream = new PrintWriter(stBuf);
            th.printStackTrace(stackStream);
            stackStream.flush();
            buf.append(stBuf.toString());
        }
        stdoutWriter.println(buf.toString());
    }

    /**
     * Printing error directly into the stdout. A log header will be generated
     *
     * @param debugName debug name
     * @param message   the error message
     * @param ex        the exception (can be null)
     */
    public static void printError(String debugName, String message, Throwable ex) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss:SSS a zzz");
        String prefix = debugName + ":" + dateFormat.format(newDate()) + ": " + Thread.currentThread().toString() +
                "\n";

        System.err.println(prefix + message);
        if (ex != null) {
            ex.printStackTrace(System.err);
        }
    }

}
