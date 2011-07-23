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
 * $Id: ReaderWriterClear.java,v 1.2 2008/06/25 05:41:34 qcheng Exp $
 *
 */

package com.iplanet.services.comm.https;

import java.net.Socket;
import java.io.IOException;
import com.sun.identity.shared.debug.Debug;

public class ReaderWriterClear extends ReaderWriter {
    private static Debug debug = Debug.getInstance("amJSS");

    public ReaderWriterClear(ReaderWriterLock l, Socket fs, Socket ts) {
        super(l, fs, ts);
    }

    public void run() {
        int numBytes = 0;
        byte[] buffer = new byte[MAXBUFFERSIZE];

        try {
            while (go) {
                numBytes = in.read(buffer);
                if (numBytes > 0) {
                    out.write(buffer, 0, numBytes);
                    out.flush();
                } else if (numBytes == 0) {
                    debug.message(
                            "ReaderWriterClear: got a 0 length read");
                } else {
                    return;
                }
            }
        } catch (IOException e) {
            return;
        } catch (NullPointerException e) {
            // If this runnable is stopped, then either 'in' or 'out'
            // would have been set to null and hence the
            // NullPointerException. Gracefully return.
            return;
        } finally {
            stop();
            buffer = null;
            rwLock.notifyFinished(this);
            rwLock = null;
        }
    }
}
