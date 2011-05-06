/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IPSServerSocket.java,v 1.2 2008/06/25 05:41:27 qcheng Exp $
 *
 */

package com.iplanet.am.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class IPSServerSocket extends ServerSocket {
    public IPSServerSocket() throws IOException {
        this(0);
    }

    public IPSServerSocket(int port) throws IOException {
        super(port);
    }

    public IPSServerSocket(int port, int backlog) throws IOException {
        super(port, backlog);
    }

    public Socket accept() throws IOException {
        IPSSocket socket = new IPSSocket();
        implAccept(socket);
        return socket;
    }

    private class IPSSocket extends Socket {
        private BufferedInputStream in;

        public IPSSocket() throws IOException {
            super();
        }

        public InputStream getInputStream() throws IOException {
            if (in == null) {
                in = new BufferedInputStream(super.getInputStream());
            }
            return in;
        }
    }
}
