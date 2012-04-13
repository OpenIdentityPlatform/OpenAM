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
 * $Id: JSSSocketFactory.java,v 1.3 2009/01/28 05:34:49 ww203982 Exp $
 *
 */
/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap;

import java.io.Serializable;
import java.net.Socket;
import java.net.InetAddress;

import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPSocketFactory;

import org.mozilla.jss.ssl.SSLSocket;

import com.iplanet.am.util.JSSInit;
import com.iplanet.services.comm.https.ApprovalCallback;
import com.sun.identity.shared.debug.Debug;

/**
 * Creates an SSL socket connection to a server, using the iPlanet JSS package.
 * This class implements the <CODE>LDAPSocketFactory</CODE>
 * interface.
 * <P>
 * <B>NOTE: This class is iPlanet internal and is distributed only with 
 * iPlanet products</B>.
 * <P>
 * By default, the factory is using "secmod.db", "key3.db" and "cert7.db"
 * databases in the current directory. If you need to override this default
 * setting, then you should call the static <CODE>initialize</CODE> method
 * before creating the first instance of <CODE>JSSSocketFactory</CODE>.
 * <P>
 * <PRE>
 *       ...
 *       JSSSocketFactory.initialize("certKeyDir");
 *       LDAPConnection ld = new LDAPConnection(new JSSSocketFactory());
 *       ...
 * </PRE>
 * @version iPlanet with JSS3.1
 * @see LDAPSocketFactory
 * @see com.sun.identity.shared.ldap.LDAPConnection#LDAPConnection(com.sun.identity.shared.ldap.LDAPSocketFactory)
 */

public class JSSSocketFactory implements Serializable,
                                         LDAPSocketFactory
{

    static final long serialVersionUID = -6926469178017736902L;

    private static Debug debug = Debug.getInstance("amJSS");

    static {
        JSSInit.initialize();
    }

    /**
     * Creates an SSL socket
     *
     * @param host Host name or IP address of SSL server
     * @param port Port numbers of SSL server
     * @return A socket for an encrypted session
     * @exception LDAPException on error creating socket
     */
    public Socket makeSocket(java.lang.String host, int port) 
    throws LDAPException
    {
        String method = "JSSSocketFactory.makeSocket ";
        SSLSocket socket = null;
        try {
            socket = new SSLSocket(
                         InetAddress.getByName(host), // address
                         port, // port
                         null, // localAddress
                         0,    // localPort
                         new ApprovalCallback(host), // certApprovalCallback
                         null  // clientCertSelectionCallback
            );
            socket.forceHandshake();
        } catch (Exception e) {
            debug.error(method + "Filed to create SSLSocket", e);
            throw new LDAPException("SSL connection to " + host +
                                    ":" + port + ", " + e.getMessage(),
                                    LDAPException.CONNECT_ERROR);
        }

        return (Socket)socket;
    }
}
