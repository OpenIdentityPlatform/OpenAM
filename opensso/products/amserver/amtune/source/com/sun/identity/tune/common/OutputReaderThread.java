/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: OutputReaderThread.java,v 1.1 2008/07/02 18:45:44 kanduls Exp $
 */

package com.sun.identity.tune.common;

/**
 * Helper class for reading the process output stream.
 * 
 */
public class OutputReaderThread extends Thread {
    java.io.InputStream instr;
    StringBuffer buffer;
    boolean runEndedNormally;
    
    /**
     * Creates the instance of OutputReaderThread
     * @param instr
     */
    public OutputReaderThread(java.io.InputStream instr) {
        this.instr = instr;
        buffer = new StringBuffer();
    }

    public void run() {
        runEndedNormally = false;
        try {
            byte[] b = new byte[16];
            int nbread = 0;
            while ((nbread = instr.read(b)) != -1) {
                String s = new String(b, 0, nbread);
                buffer.append(s);
            }
            runEndedNormally = true;
        } catch (java.io.InterruptedIOException ie) {
        /// do nothing ... 
        } catch (java.io.IOException e) {
            if (e.getMessage().indexOf(
                    "A system call received an interrupt") == -1) {
            }
        }
    }

    public StringBuffer getBuffer() {
        return buffer;
    }
}
