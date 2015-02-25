/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SetupWriter.java,v 1.2 2008/06/25 05:42:32 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.config;
import java.io.CharArrayWriter;
import java.io.Writer;
import java.io.IOException;

/**
 * <code>java.io.Writer</code> implementation to allow a thread to dump messages 
 * in a http response writer owned by another thread.
 * Currently used by AJAX spun threads to send progress log to be displayed
 * on the page tha invoked the AJAX calls.
 */
public class SetupWriter extends Writer {
    private Writer realWriter = null;
    private CharArrayWriter buf = new CharArrayWriter();
    private boolean flushflag = false;
    private volatile boolean closeflag = false;

    public SetupWriter(Writer wt) {
        realWriter = wt;
    }
    synchronized public void write(char[] b, int off, int len) throws IOException {
        buf.write(b, off, len);
        notify();
    }
    synchronized public void flush() throws IOException {
        flushflag = true;
        notify();
    }

    synchronized public void close() throws IOException {
        closeflag = true;
        notify();
    }

    synchronized public void realFlush() {
        try {
            while (true && !closeflag) {
                wait(600000);
                buf.writeTo(realWriter);
                buf.reset();
                if (flushflag) {
                    realWriter.flush();
                }
            }
        } catch(Exception ex) {
            System.out.println("SetupWriter:"+ex);
        }
     }
}
