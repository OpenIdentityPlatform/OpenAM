/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ReaderWriter.java,v 1.2 2008/06/25 05:41:34 qcheng Exp $
 *
 */

package com.iplanet.services.comm.https;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.sun.identity.shared.debug.Debug;

public abstract class ReaderWriter implements Runnable {
    protected ReaderWriterLock rwLock;

    protected DataInputStream in;
    protected DataOutputStream out;

    final protected int MAXBUFFERSIZE = 8*1024;

    protected boolean sent = false;

    protected volatile boolean go = true;
    private static Debug debug = Debug.getInstance("amJSS");

    public ReaderWriter(ReaderWriterLock l, Socket fs, Socket ts) {
        rwLock = l;

        //  create data input and output streams
        try {
            in = new DataInputStream(fs.getInputStream());
            out = new DataOutputStream(ts.getOutputStream());
        }
        catch (IOException e) {
            in = null;
            out = null;
            debug.error("ReaderWriter: Cannot construct ReaderWriter ", e);
        }
    }

    public abstract void run();

    void clean() {
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
            } finally {
                in = null;
            }
        }

        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
            } finally {
                out = null;
            }
        }
    }

    public void netletstop() {
        go = false;
        clean();
    }

    public void stop() {
        go = false;
        clean();
    }

    public boolean sentDataFlag() {
        return(sent);
    }

    public void clearDataFlag() {
        sent = false;
    }

    public boolean isAlive() {
        return(go);
    }
}
