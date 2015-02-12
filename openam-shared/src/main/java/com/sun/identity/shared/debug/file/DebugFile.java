/**
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
 * Copyright 2014-2015 ForgeRock AS.
 */
package com.sun.identity.shared.debug.file;

import java.io.IOException;


/**
 * <p>
 * The implementation of this interface as well as the
 * <code>com.sun.identity.shared.debug.file.DebugFileProvider</code> interface together
 * provide the necessary functionality to replace or enhance the Debug File service.
 * <p/>
 * Because of specific features that are only related to the log file and not the debugger,
 * the debugger and the log file management can't be manage in a same class.
 * For example, two debugger could write on the same log file with the merge mapping.
 * They could have a different debug level but should have the same log rotation.
 * This interface manages the log rotation, and the debugger the log level.
 * <p/>
 * <p/>
 * Even if this interface was dedicated to debugger, it could be used for managing a
 * different kind of log file (example : access, statistics, etc.).
 * <p/>
 * Every features that are related to the file should be implemented by classes that
 * implement this interface.
 * </p>
 */
public interface DebugFile {

    /**
     * Write message into file
     *
     * @param prefix Message prefix
     * @param msg    Message to be recorded.
     * @param th     the optional <code>java.lang.Throwable</code> which if
     *               present will be used to record the stack trace.
     * @throws IOException
     */
    public void writeIt(String prefix, String msg, Throwable th) throws IOException;

}
